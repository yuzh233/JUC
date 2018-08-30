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


#  Lock 同步锁
#  Condition 控制线程通信
#  线程按序交替
#  ReadWriteLock 读写锁
#  线程八锁
#  线程池
#  线程调度
#  ForkJoinPool 分支/合并框架 工作窃取
