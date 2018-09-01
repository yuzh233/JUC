>  Java并发编程包 java.util.concurrent 的学习笔记

<!-- TOC -->

- [简介](#简介)
- [volatile 关键字-内存可见性](#volatile-关键字-内存可见性)
- [原子变量-CAS算法](#原子变量-cas算法)
    - [原子变量](#原子变量)
    - [CAS算法](#cas算法)
- [ConcurrentHashMap 锁分段机制](#concurrenthashmap-锁分段机制)
- [CountDownLatch 闭锁](#countdownlatch-闭锁)
- [实现 Callable 接口](#实现-callable-接口)
- [Lock 同步锁](#lock-同步锁)
- [Condition 控制线程通信](#condition-控制线程通信)
- [线程按序交替](#线程按序交替)
- [ReadWriteLock 读写锁](#readwritelock-读写锁)
- [线程八锁](#线程八锁)
- [线程池](#线程池)
- [线程调度](#线程调度)
- [ForkJoinPool 分支/合并框架 工作窃取](#forkjoinpool-分支合并框架-工作窃取)

<!-- /TOC -->

# 简介
 在 Java 5.0 提供了 java.util.concurrent （简称JUC ）包，在此包中增加了在并发编程中很常用的实用工具类，用于定义类似于线程的自定义子系统，包括线程池、异步 IO 和轻量级任务框架。提供可调的、灵活的线程池。还提供了设计用于多线程上下文中的 Collection 实现等。

# volatile 关键字-内存可见性
> 内存可见性（Memory Visibility）是指当某个线程正在使用对象状态而另一个线程在同时修改该状态，需要确保当一个线程修改了对象状态后，其他线程能够看到发生的状态变化。

通过一个用例验证可见性：子线程修改共享资源的状态（写操作），主线程访问共享资源（读操作），如果为true打印一段符号并退出循环。
```java
public class TestVolatile {
    public static void main(String[] args) {
        // 线程1
        ThreadDemo td = new ThreadDemo();
        new Thread(td).start();

        // 主线程循环的访问共享资源
        while(true){
            if(td.isFlag()){
                System.out.println("------------------");
                break;
            }
        }
    }
}

class ThreadDemo implements Runnable {
    private boolean flag = false; // 共享资源

    @Override
    public void run() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        flag = true;
        System.out.println("flag=" + isFlag());
    }
}
```
运行结果：
![Alt text](/img/1.jpg)

可以看到，子线程将资源状态改为true之后，而主线程一直处在循环状态，并未打印一段符号，说明此时共享资源状态还是false，而子线程明明将状态改为true来着的，这是什么原因呢？

>计算机在执行程序时，每条指令都是在CPU中执行的，而执行指令过程中，势必涉及到数据的读取和写入。由于程序运行过程中的临时数据是存放在主存（物理内存）当中的，这时就存在一个问题，由于CPU执行速度很快，而从内存读取数据和向内存写入数据的过程跟CPU执行指令的速度比起来要慢的多，因此如果任何时候对数据的操作都要通过和内存的交互来进行，会大大降低指令执行的速度。因此在CPU里面就有了高速缓存。
当程序在运行过程中，会将运算需要的数据从主存复制一份到CPU的高速缓存当中，那么CPU进行计算时就可以直接从它的高速缓存读取数据和向其中写入数据，当运算结束之后，再将高速缓存中的数据刷新到主存当中。

程序运行时，JVM为每一个线程分配一个独立的缓存来提高效率。共享资源存放在主存（堆内存、物理内存）中，“每个线程操作共享资源时，先从主存中读取共享资源放入自己的缓存区，在自己的缓存区中对资源进行修改，然后将修改更新到主存中去。” 从用例可以看出，子线程睡了2s，所以主线程先执行，获得了资源状态为false。然后子线程将状态改为true并将修改更新到主存中，但是主线程一直处在循环状态，这是因为while的运行调用了底层的代码处理非常之快，快到都没有来得及更新主存中的共享资源数据。

现在知道是因为子线程更新缓存中的值到主存去了而另外一个线程没有来得及刷新缓存来更新主存中的数据，所以导致的缓存一致性问题。通常称这种被多个线程访问的变量为共享变量。如果一个变量在多个CPU中都存在缓存（一般在多线程编程时才会出现），那么就可能存在缓存不一致的问题。

> Java内存模型规定所有的变量都是存在主存当中（类似于前面说的物理内存），每个线程都有自己的工作内存（类似于前面的高速缓存）。线程对变量的所有操作都必须在工作内存中进行，而不能直接对主存进行操作。并且每个线程不能访问其他线程的工作内存。

**子线程对主存进行了修改，主线程没有立即看到，所以出现了概念中的可见性问题。** 解决方案：

方式一：使用synchronized和Lock（后面学）保证可见性。它能保证同一时刻只有一个线程获取锁然后执行同步代码，并且在释放锁之前会将对变量的修改刷新到主存当中。
```java
while (true) {
    synchronized (td) {
        if (td.isFlag()) {
            System.out.println("------------------");
            break;
        }
    }
}
```
![Alt text](/img/2.jpg)

每次访问加锁数据都会从主存中刷新数据，所以主线程正常退出。

但是由于 synchronized 关键字会极低的影响运行效率，所以还有一种方式：使用 `volatile` 关键字保证可见性。当一个共享变量被volatile修饰时，它会保证修改的值会立即被更新到主存，当有其他线程需要读取时，它会去内存中读取新值。

volatile 和 synchronized 的区别：

- volatile 不保证互斥性。一个线程对共享资源访问另外一个线程也能访问，不具备互斥性。

- volatile 不保证原子性。见下一节

- synchronized 保证原子性是因为对共享资源加锁，每次只能一个线程访问，一个线程在同步代码块中执行操作之后另一个线程才会进入。


引用：[Java并发编程：volatile关键字解析](https://www.cnblogs.com/dolphin0520/p/3920373.html)

#  原子变量-CAS算法
首先明白 `i++` 操作是不具备原子性的，`i++`分为三个步骤：首先线程从主存中获取变量i的值，然后执行 i + 1，再将 i+1 赋值给i，然后写入到线程自己的工作内存，最后写入到主存。

假如线程1获取了volatile修饰的共享变量i还未来得及操作，但此时被线程2抢夺了时间片进行了 +1 操作并写入到主存，此时主存的 i 的值已被更改。

由于线程1只是对共享变量进行读取操作并没有对变量进行修改，所以不会导致线程2的工作内存中缓存变量i的缓存无效，所以线程2会直接去主存读取i的值。然后线程1对线程2修改之前的i进行+1之后写入到主存，此次的线程2更新的值被线程1给覆盖掉了。

## 原子变量
原子变量是保证了变量具有原子性的特征，对该变量的操作要么全部成功，要么全部失败。原子变量通过以下两点保证变量的原子性。

- 使用 volatile 修饰符保证变量的内存可见性
- 使用 CAS 算法通过内存值和预估值的比对实现原子性

java.util.concurrent 包中常见原子变量及API：

|类|对应类型|
|-----|-----|
|AtomicBoolean|boolean|
|AtomicInteger|int|
|AtomicLong|long| 
|AtomicReference|参数化类型 T|
|AtomicIntegerArray|int[]|
|AtomicLongArray|long[]|
|AtomicMarkableReference|Pair<V>|
|AtomicReferenceArray|T[]|
|AtomicStampedReference|Pair<V>|

核心方法：boolean compareAndSet(expectedValue, updateValue)

## CAS算法

CAS (Compare-And-Swap) 是一种硬件对并发的支持，针对多处理器操作而设计的处理器中的一种特殊指令，用于管理对共享数据的并访问。

CAS 是一种无锁的非阻塞算法的实现。

CAS 包含了 3 个操作数：

- 需要读写的内存值 V
- 进行比较的值 A
- 拟写入的新值 B
- 当且仅当 V 的值等于 A 时，CAS 通过原子方式用新值 B 来更新 V 的值，否则不会执行任何操作。

重现i++操作：初始化时 i=0, 线程2从主存中获取到共享变量i，内存值V=0；此时线程1抢夺到了时间片也从主存中获取到共享变量i，内存值V=0；并且进行+1操作，+1时CAS算法再次读取主存的值发现i没变，所以比较值A=0，对比V == A为true 则以B=1更新V，写入到线程1的工作内存，再写入到主存。

此时主存中的值i=1，但是线程2在线程1修改之前就读取到了 i 的值，V=0，然后进行+1操作，CAS算法再次从主存获取i值发现已经变化了：A=1，此时 V == A 为false，则取消更新。

#  ConcurrentHashMap 锁分段机制
> Java 5.0 在 java.util.concurrent 包中提供了多种并发容器类来改进同步容器的性能。ConcurrentHashMap 同步容器类是Java 5 增加的一个线程安全的哈希表。对与多线程的操作，介于 HashMap 与 Hashtable 之间。内部采用“锁分段”机制替代 Hashtable 的独占锁。进而提高性能。
 
多线程环境下，使用 HashMap 是线程不安全的。为了保证线程安全，我们可以通过使用Collectinos集合工具类中的synchronized方法来将非线程安全的容器类转为对应线程安全的容器类。其内部就是给每个方法加了一把synchronized锁。另外一种方式就是使用 HashMap 的线程安全类 Hashtable。

Hashtable 采用了表锁机制，任何一个线程对容器访问其他线程会进入阻塞或轮询状态。由于Hashtable 为每个方法使用了同一把锁（每一个被synchronized修饰的方法都是使用的java内置锁，锁的是方法所属对象本身），一个线程获得了锁进入某个同步方法，其他线程都不能进入被该锁修饰的其他同步方法，除非锁被释放抢到了锁。在并发编程中，极低的降低了效率。

另外 Hashtable 这种表锁机制（每个同步方法使用同一把锁）在并发的复合操作中会产生异常。例如：在迭代操作中，线程1调用了同步的 hashNext() 方法发现有下一个元素准备调用同步的 next() 方法获取元素时，时间片被线程2抢夺过去将某个元素修改了，此时会抛出并发异常ConcurrentModificationException。多个同步方法组成的操作不是同步操作了。

为了解决这种问题于是有了锁分段机制，这个机制将容器中的数据分为一段一段存储，为每一段加一把锁，不同的锁之间互不干扰，当一个线程占用其中一段数据的时候另一个线程可以访问另一段的数据。以此提高了并发效率。

以下是常用并发容器类对应的未加同步的容器类：

|并发容器类|非同步容器类|
|-----|-----|
|ConcurrentHashMap|HashMap|
|ConcurrentSkipListMap|TreeMap|
|ConcurrentSkipListSet|TreeSet|
|CopyOnWriteArrayList|ArrayList|
|CopyOnWriteArraySet|Set|

当希望许多线程访问一个容器类的时候，ConcurrentHashMap 通常优于同步的 HashMap。ConcurrentSkipListMap 通常优于同步的TreeMap。当期望的读数和遍历远远大于列表的更新数时，CopyOnWriteArrayList 优于同步的 ArrayList。

#  CountDownLatch 闭锁
CountDownLatch 一个同步辅助类，在完成一组正在其他线程中执行的操作之前，它允许一个或多个线程一直等待。

闭锁可以延迟线程的进度直到其到达终止状态，闭锁可以用来确保某些活动直到其他活动都完成才继续执行：

- 确保某个计算在其需要的所有资源都被初始化之后才继续执行;
- 确保某个服务在其依赖的所有其他服务都已经启动之后才启动;
- 等待直到某个操作所有参与者都准备就绪再继续执行

一个简单的用例，统计所有线程执行某个同步操作的总计时间。主线程需要在其他线程执行完毕之前等待，直至闭锁计数器为0解除等待状态。
```java
public class TestCountDownLatch {
    public static void main(String[] args) {
        final CountDownLatch latch = new CountDownLatch(50); // 创建一个闭锁对象
        LatchDemo ld = new LatchDemo(latch);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            new Thread(ld).start(); // 每一个线程执行一次运算
        }
        try {
            latch.await(); // 主线程进入等待，直至闭锁计算器为0，解除等待状态。
        } catch (InterruptedException e) {
        }
        long end = System.currentTimeMillis();
        System.out.println("耗费时间为：" + (end - start)); // 其他线程都执行完毕统计所有线程执行的总时间
    }
}

class LatchDemo implements Runnable {
    private CountDownLatch latch; // 维护一个闭锁对象

    public LatchDemo(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < 50000; i++) {
                if (i % 2 == 0) {
                    System.out.println(i);
                }
            }
        } finally {
            latch.countDown(); // 每个线程执行完毕计数器-1
        }
    }
}
```

#  实现 Callable 接口
创建线程的第三种方式：实现 Callable 接口，这是一个带泛型的接口，实现这个接口的线程可以返回泛型参数的值并可以抛出异常。

```java
public class TestCallable {
    public static void main(String[] args) {
        ThreadDemo td = new ThreadDemo(); // 线程对象
        FutureTask<Integer> result = new FutureTask<>(td); // 用于接收线程结果的对象
        new Thread(result).start(); // 启动
        try {
            Integer sum = result.get(); //FutureTask没有获取到结果之前，线程进入阻塞状态，因此也可用于闭锁。
            System.out.println(sum);
            System.out.println("------------------------------------");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}

class ThreadDemo implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        int sum = 0;
        for (int i = 0; i <= 100000; i++) {
            sum += i;
        }
        return sum;
    }
}
```

#  Lock 同步锁
> 在 Java 5.0 之前，协调共享对象的访问时可以使用的机制只有 synchronized 和 volatile 。Java 5.0 后增加了一些新的机制，但并不是一种替代内置锁的方法，而是当内置锁不适用时，作为一种可选择的高级功能。ReentrantLock 实现了 Lock 接口，并提供了与synchronized 相同的互斥性和内存可见性。但相较于synchronized 提供了更高的处理锁的灵活性。

解决多线程安全问题，加锁的第二种方式。相对于 synchronized 上锁使用 lock 可更具有灵活性，并且是显式锁，上锁和释放锁需要手动完成。需要注意的是当同步代码出现异常必须保证 unlock 操作一定会执行，否则由于没释放锁其他线程会一直处于阻塞状态。

```java
public class TestLock {
    public static void main(String[] args) {
        Ticket ticket = new Ticket();
        new Thread(ticket, "1号窗口").start();
        new Thread(ticket, "2号窗口").start();
        new Thread(ticket, "3号窗口").start();
    }
}

class Ticket implements Runnable {
    private int tick = 100;
    private Lock lock = new ReentrantLock();

    @Override
    public void run() {
        while (true) {
            lock.lock(); //上锁
            try {
                if (tick > 0) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                    System.out.println(Thread.currentThread().getName() + " 完成售票，余票为：" + --tick);
                }
            } finally {
                lock.unlock(); //释放锁
            }
        }
    }
}
```

#  Condition 控制线程通信
首先回顾一下生产者消费者模型，直接上代码：
```java
public class TestProducerAndConsumer {
    public static void main(String[] args) {
        Clerk clerk = new Clerk();

        Producer producer = new Producer();
        producer.setClerk(clerk);
        Consumer consumer = new Consumer();
        consumer.setClerk(clerk);

        new Thread(producer,"生产者A：").start();
        new Thread(consumer,"消费者B：").start();
    }
}

// 店员（维护共享资源——产品，生产和消费）
@Data
class Clerk {
    private int product;

    public synchronized void production(){
        if (product >= 10) {
            System.out.println("已满！");
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        } else {
            System.out.println(Thread.currentThread().getName()+"生产了：" + ++product);
            this.notifyAll();
        }
    }

    public synchronized void consumption(){
        if (product <= 0) {
            System.out.println("缺货！");
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        } else {
            System.out.println(Thread.currentThread().getName()+"消费了：" + product--);
            this.notifyAll();
        }
    }
}

// 生产者
@Data
class Producer implements Runnable{
    private Clerk clerk;

    @Override
    public void run() {
        for (int i = 0; i < 20; i++){
            clerk.production();
        }
    }
}

// 消费者
@Data
class Consumer implements Runnable{
    private Clerk clerk;

    @Override
    public void run() {
        for (int i = 0; i < 20; i++){
            clerk.consumption();
        }
    }
}
```
运行结果部分截图：
![Alt text](/img/3.jpg)

以上结果看似交替有序的执行，但是存在一个潜在的问题。当我们将仓库容量设为1，库存有了一个产品就满了，生产者挂起，通知消费者运行。

    if (product >= 1) {
        System.out.println("已满！");
        try {
            this.wait();
        } catch (InterruptedException e) {
        }
    } else {
        System.out.println(Thread.currentThread().getName()+"生产了：" + ++product);
        this.notifyAll();
    }

再让生产者每次生产之前睡一秒：

    for (int i = 0; i < 20; i++){
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        clerk.production();
    }

看看运行情况... 发现程序没有正常停止

![Alt text](/img/4.jpg)

为什么会出现这种情况呢？首先：生产者每次生产之前都会睡上一秒，这就意味着时间片总是被消费者先抢去了，每次都是消费者没货了进入阻塞状态之后生产者才慢吞吞的抢到了时间片进行生产。

再看一次消费者的代码：

    public synchronized void consumption() { 
        if (product <= 0) {
            System.out.println("缺货！");
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        } else {
            System.out.println(Thread.currentThread().getName() + "消费了：" + product--);
            this.notifyAll();
        }
    }

注意：这个 `else` 是问题的关键点。

- 生产者消费者同时开抢，由于生产者0要睡上一会，于是毫不意外的被消费者0抢到时间片，进入`缺货分支`里面陷入了`等待状态`，此时生产者0 睡完1秒生产了一个产品，唤醒所有线程。消费者0由于已经在消费的同步代码块里面所以继续执行退出了本次循环，该消费者0没有消费产品。

- 接着，生产者1和消费者1抢时间片，生产者每次都要睡觉怎么抢的过消费者，于是消费者1抢到了，发现有货！通知所有线程，消费者2又抢到了时间，发现没货，此时生产1才“被迫”拿到时间片生产一个产品，通知所有线程。由于生产者每次都要等待所以每次都被消费者抢到时间，消费者2执行完毕。消费者2没有消费就结束。

- 生产者2与消费者3开抢，消费者3胜。有货！消费完毕通知所有线程，消费者4又抢到了，没货。此时生产者2又“被迫”生产了一个，通知所有线程...

- 好吧，消费者5抢到了，消费完美滋滋。消费者6又来了，缺货！生产者3“被迫”生产......

- 每次都是消费者抢到CPU时间片执行权，而生产者每次都是在消费者没货时等待状态下才有机会执行，而由于存在else分支导致有些消费者并没有消费产品就结束了。

- 最后的结果是消费者早早的遍历完20次，而生产者迟迟没有遍历完20次，慢慢吞吞的...

- **最关键的：** 由于消费者早就消费完了，生产者生产了一个唤醒所有，只有自己被唤醒没人和它抢，就又进入了生产者方法，发现库存满了！于是乎，等待其他线程唤醒自己，然而没有其他线程了，就没人唤醒自己了...

解决办法：去掉else分支

    public synchronized void production() { 
        if (product >= 1) {
            System.out.println("已满！");
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        System.out.println(Thread.currentThread().getName() + "生产了：" + ++product);
        this.notifyAll();
    }

    public synchronized void consumption() { 
        if (product <= 0) {
            System.out.println("缺货！");
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        System.out.println(Thread.currentThread().getName() + "消费了：" + product--);
        this.notifyAll();
    }

这样保证了每次消费者都能消费到产品，然而只有一个消费者和一个生产者的情况下是没问题的，当分别有两个呢？

![Alt text](/img/5.jpg)

乱套了... 这个稍微想想很好理解，比如刚开始第一个消费者线程抢到了时间进入缺货分支陷入等待，于是生产者生产了一个，开始抢夺时间，假如此时被第二个消费者线程抢到时间进入了消费代码块，但是第一个消费线程已经进入了它就会继续执行完毕，于是乎两个消费者线程消费了同一份产品。

这就是 **虚假唤醒** ，那么怎么解决呢？真是令人头大...

其实也很简单，将生产消费的同步代码块中由 if 改为 while就行了。

    public synchronized void production() {
        while (product >= 1) {
            System.out.println("已满！");
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        System.out.println(Thread.currentThread().getName() + "生产了：" + ++product);
        this.notifyAll();
    }

    public synchronized void consumption() {
        while (product <= 0) {
            System.out.println("缺货！");
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        System.out.println(Thread.currentThread().getName() + "消费了：" + product--);
        this.notifyAll();
    }

一次等待之后，当要继续执行下面的代码，再次判断一次是否有货，有才消费，没有就继续等待。

至此，线程间通信的潜在问题及虚假唤醒问题完美解决了。

接下来就是通过使用Lock锁取代Synchronized锁，而Condiction则是对线程通信进行控制的条件变量的对象。

> Condition 接口描述了可能会与锁有关联的条件变量。这些变量在用法上与使用 Object.wait 访问的隐式监视器类似，但提供了更强大的功能。需要特别指出的是，单个 Lock 可能与多个 Condition 对象关联。为了避免兼容性问题，Condition 方法的名称与对应的 Object 版本中的不同。在 Condition 对象中，与 wait、notify和notifyAll 方法对应的分别是await、signal 和 signalAll。Condition 实例实质上被绑定到一个锁上。要为特定 Lock 实例获得Condition 实例，请使用其 newCondition() 方法。
```java
class Clerk1 {
    private int product;
    private Lock lock = new ReentrantLock(); // 加 Lock 锁
    Condition condition = lock.newCondition(); // 通过 lock 获得锁关联变量对象

    public void production() {
        lock.lock();
        try {
            while (product >= 1) {
                System.out.println("已满！");
                try {
                    condition.await();
                } catch (InterruptedException e) {
                }
            }
            System.out.println(Thread.currentThread().getName() + "生产了：" + ++product);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void consumption() {
        lock.lock();
        try {
            while (product <= 0) {
                System.out.println("缺货！");
                try {
                    condition.await();
                } catch (InterruptedException e) {
                }
            }
            System.out.println(Thread.currentThread().getName() + "消费了：" + product--);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
```

#  线程按序交替
 编写一个程序，开启 3 个线程，这三个线程的 ID 分别为A、B、C，每个线程将自己的 ID 在屏幕上打印 10 遍，要求输出的结果必须按顺序显示。如：ABCABCABC…… 依次递归
```java
public class TestABCAlternate {
    public static void main(String[] args) {
        ABCAlternate alternate = new ABCAlternate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    alternate.printA();
                }
            }
        },"A").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    alternate.printB();
                }
            }
        },"B").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    alternate.printC();
                }
            }
        },"C").start();
    }
}

class ABCAlternate {
    private int num = 1;
    Lock lock = new ReentrantLock();
    Condition con1 = lock.newCondition();
    Condition con2 = lock.newCondition();
    Condition con3 = lock.newCondition();

    public void printA() {
        lock.lock();
        try {
            if (num != 1) {
                try {
                    con1.await();
                } catch (InterruptedException e) {
                }
            }
            System.out.print(Thread.currentThread().getName());
            num = 2;
            con2.signal();
        } finally {
            lock.unlock();
        }
    }

    public void printB() {
        lock.lock();
        try {
            if (num != 2) {
                try {
                    con2.await();
                } catch (InterruptedException e) {
                }
            }
            System.out.print(Thread.currentThread().getName());
            num = 3;
            con3.signal();
        } finally {
            lock.unlock();
        }
    }

    public void printC() {
        lock.lock();
        try {
            if (num != 3) {
                try {
                    con3.await();
                } catch (InterruptedException e) {
                }
            }
            System.out.print(Thread.currentThread().getName());
            num = 1;
            con1.signal();
        } finally {
            lock.unlock();
        }
    }
}
```

#  ReadWriteLock 读写锁
ReadWriteLock 维护了一对相关的锁，一个用于只读操作，另一个用于写入操作。只要没有 writer，读取锁可以由多个 reader 线程同时保持。写入锁是独占的。

ReadWriteLock 读取操作通常不会改变共享资源，但执行写入操作时，必须独占方式来获取锁。对于读取操作占多数的数据结构。 ReadWriteLock 能提供比独占锁更高的并发性。而对于只读的数据结构，其中包含的不变性可以完全不需要考虑加锁操作

用例：
```java
public class TestReadWriterLock {
    public static void main(String[] args) {
        ReadWriterLock rwLock = new ReadWriterLock();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    rwLock.read();
                }
            }
        }, "ReadLock:").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                rwLock.writer(233);
            }
        }, "WriterLock:").start();
    }
}

class ReadWriterLock {
    private int resource;
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void read() {
        try {
            rwLock.readLock().lock();
            System.out.println(Thread.currentThread().getName() + resource);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void writer(int n) {
        try {
            rwLock.writeLock().lock();
            resource = n;
            System.out.println(Thread.currentThread().getName() + resource);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```
运行结果部分截图：![Alt text](/img/6.jpg)

#  线程八锁
线程八锁，实际上是多线程编程中经常遇到的八种情况。通过八种场景学习总结线程锁的特性。

场景一：两个普通同步方法，两个线程，是同一把锁（this锁），存在互斥关系。
```java
public class TestThread8Monitor {
    public static void main(String[] args) {
        Number number = new Number();

        new Thread(new Runnable() {
            @Override
            public void run() {
                number.one();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                number.two();
            }
        }).start();
    }
}

class Number {

    public synchronized void one() {
        System.out.println("one");
    }

    public synchronized void two() {
        System.out.println("two");
    }
}
```
运行结果：

    one
    two


场景二：让同步方法 one() 睡3秒，观察结果。**同一把锁，存在互斥关系，一个同步方法没有释放锁其他所有同步方法阻塞。**

    public synchronized void one() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("one");
    }

运行结果：

    one
    two

场景三：新增一个同步方法 three()，三个同步方法，三个线程，一个线程对象，竞争打印。

    public synchronized void one() {
        System.out.println("one");
    }

    public synchronized void two() {
        System.out.println("two");
    }

    public synchronized void three() {
        System.out.println("three");
    }

运行结果：

    one
    three
    two

场景四：新增一个 Number 对象。两个线程对象，两个同步方法。**不同锁之间（两个this不是同一个this）的同步方法不存在互斥。**

    public static void main(String[] args) {
        Number number = new Number();
        Number number2 = new Number();

        new Thread(new Runnable() {
            @Override
            public void run() {
                number.one();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                number2.two();
            }
        }).start();
    }

两个同步方法：

    public synchronized void one() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("one");
    }

    public synchronized void two() {
        System.out.println("two");
    }

运行结果：

    two
    one

场景五：同一个线程对象，一个静态同步方法，一个非静态同步方法。**不同锁之间的同步方法不存在互斥。**

    public static synchronized void one() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("one");
    }

    public synchronized void two() {
        System.out.println("two");
    }

运行结果：

    two
    one

场景六：同一个线程对象，两个静态同步方法，是相同的锁（Class锁），存在互斥。

    public static synchronized void one() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("one");
    }

    public static synchronized void two() {
        System.out.println("two");
    }

  运行结果：

    one 
    two

场景七：新增一个线程对象。两个线程对象，两个静态同步方法。**同一把锁存在互斥性。**

    public static void main(String[] args) {
        Number number = new Number();
        Number number2 = new Number();

        new Thread(new Runnable() {
            @Override
            public void run() {
                number.one();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                number2.two();
            }
        }).start();
    }

静态同步方法： 

    public static synchronized void one() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("one");
    }

    public static synchronized void two() {
        System.out.println("two");
    }

运行结果：

    one 
    two

场景八：新增一个线程对象。两个线程对象，一个静态同步方法，一个非静态同步方法。**非同一把锁不存在竞争**

    public static synchronized void one() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("one");
    }

    public synchronized void two() {
        System.out.println("two");
    }

运行结果：

    two 
    one 

结论：

- 静态同步方法的锁是 Class 锁，非静态同步方法的锁是 this 锁，不同的对象是不同的 this 锁。
- 某一个时刻内，只能有一个线程持有锁，无论几个方法。相同的锁，一个线程获得锁，其他线程都得等待。

#  线程池
创建线程的第四种方式，线程池：提供了一个线程队列，队列中保存着所有等待状态的线程。避免了创建与销毁额外开销，提高了响应的速度。

线程池的体系结构：

    java.util.concurrent.Executor : 负责线程的使用与调度的根接口
  		|--**ExecutorService 子接口: 线程池的主要接口
  			|--ThreadPoolExecutor 线程池的实现类
  			|--ScheduledExecutorService 子接口：负责线程的调度
  				|--ScheduledThreadPoolExecutor ：继承ThreadPoolExecutor，实现ScheduledExecutorService

ThreadPoolExecutor和ScheduledThreadPoolExecutor可以创建连接池对象，但是使用工厂获得对象是最好的方式。工具类 Executors 的常用API及描述：

|方法|描述|
|-----|-----|
|ExecutorService newFixedThreadPool() | 创建固定大小的线程池|
|ExecutorService newCachedThreadPool() | 缓存线程池，线程池的数量不固定，可以根据需求自动的更改数量，可以自动进行线程回收|
|ExecutorService newSingleThreadExecutor() | 创建单个线程池。线程池中只有一个线程|
|ScheduledExecutorService newScheduledThreadPool() | 创建固定大小的线程，可以延迟或定时的执行任务|

用例说明：
```java
public class TestThreadPool {
    public static void main(String[] args) throws Exception {
        //创建线程池
        ExecutorService pool = Executors.newFixedThreadPool(5);
        ThreadPoolDemo tpd = new ThreadPoolDemo();
        //5个线程执行10个任务，某些线程会被回收
        for (int i = 0; i < 10; i++) {
            pool.submit(tpd); // 可传 Callable 和 Runnable接口作为任务对象
        }
        //关闭线程池
        pool.shutdown();

        /** 以 Callable 作为任务 **/
        List<Future<Integer>> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Future<Integer> future = pool.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int sum = 0;
                    for (int i = 0; i <= 100; i++) {
                        sum += i;
                    }
                    return sum;
                }
            });
            list.add(future);
        }
        pool.shutdown();
        for (Future<Integer> future : list) {
            System.out.println(future.get());
        }
    }
}

class ThreadPoolDemo implements Runnable {
    private int i = 0;

    @Override
    public void run() {
        while (i <= 100) {
            System.out.println(Thread.currentThread().getName() + " : " + i++);
        }
    }
}
```

#  线程调度
一个 ExecutorService，可安排在给定的延迟后运行或定期执行的命令。
```java
public class TestScheduledThreadPool {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 线程池的任务调度
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(5);
        // 十个任务
        for (int i = 0; i < 5; i++) {
            /**
             * 参数一：任务
             * 参数二：延迟时间
             * 参数一：时间单位
             */
            ScheduledFuture<Integer> future = pool.schedule(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int num = new Random().nextInt(100);
                    return num;
                }
            }, 1, TimeUnit.SECONDS);
            System.out.println(future.get());
        }
        // 关闭线程池
        pool.shutdown();
    }
}

```

#  ForkJoinPool 分支/合并框架 工作窃取
Fork/Join 框架：就是在必要的情况下，将一个大任务，进行拆分(fork)成若干个小任务（拆到不可再拆时），再将一个个的小任务运算的结果进行 join 汇总。

前面学过 [归并排序](https://github.com/yuzh233/Algorithms#%E5%BD%92%E5%B9%B6%E6%8E%92%E5%BA%8F)，实际上这种思想和归并排序是一样的。

![Alt text](/img/7.jpg)

采用 “工作窃取”模式（work-stealing）：当执行新的任务时它可以将其拆分分成更小的任务执行，并将小任务加到线程队列中，然后再从一个随机线程的队列中偷一个并把它放在自己的队列中。

相对于一般的线程池实现，fork/join框架的优势体现在对其中包含的任务的处理方式上.在一般的线程池中，如果一个线程正在执行的任务由于某些原因无法继续运行，那么该线程会处于等待状态。而在fork/join框架实现中，如果某个子问题由于等待另外一个子问题的完成而无法继续运行。那么处理该子问题的线程会主动寻找其他尚未运行的子问题来执行.这种方式减少了线程的等待时间，提高了性能。

模拟拆分：
```java
public class TestForkJoinPool {
    public static void main(String[] args) {
        // 创建fork/join对象
        ForkJoinPool pool = new ForkJoinPool();
        // 创建任务对象
        ForkJoinTask<Long> task = new ForkJoinSumCalculate(0L, 100000000L);
        // 执行任务
        Long sum = pool.invoke(task);
        System.out.println(sum);
    }
}

// 创建自己的fork/join任务，计算从start开始end个数的和。需要继承递归任务类。
class ForkJoinSumCalculate extends RecursiveTask<Long> {
    private long start;
    private long end;

    /**
     * 拆分临界值：定义子任务（子线程队列）到多大时不再拆分，开始计算每个子任务的值。
     * - 如果临界值 = 任务总大小，就不会拆分，直接循环计算所有值。
     * - 如果临界值 = 1L，就拆到每个子线程队列大小为1时停止拆分
     */
    private static final long THURSHOLD = 10000L;

    public ForkJoinSumCalculate(long start, long end) {
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        long len = end - start;
        if (len <= THURSHOLD) {
            long sum = 0L;
            for (long i = start; i <= end; i++) {
                sum += i;
            }
            return sum;
        } else {
            long mid = (end + start) / 2;
            ForkJoinSumCalculate left = new ForkJoinSumCalculate(start, mid);
            left.fork();
            ForkJoinSumCalculate right = new ForkJoinSumCalculate(mid + 1, end);
            right.fork();
            return left.join() + right.join();
        }
    }
}
```