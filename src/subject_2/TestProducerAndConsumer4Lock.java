package subject_2;

import lombok.Data;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: yu_zh
 * @DateTime: 2018/08/30 21:00
 */
public class TestProducerAndConsumer4Lock {
    public static void main(String[] args) {
        Clerk1 clerk = new Clerk1();
        Producer1 producer = new Producer1();
        producer.setClerk(clerk);
        Consumer1 consumer = new Consumer1();
        consumer.setClerk(clerk);
        new Thread(producer, "生产者A：").start();
        new Thread(consumer, "消费者B：").start();
        new Thread(producer, "生产者C：").start();
        new Thread(consumer, "消费者D：").start();
    }
}

@Data
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

// 生产者
@Data
class Producer1 implements Runnable {
    private Clerk1 clerk;

    @Override
    public void run() {
        for (int i = 0; i < 20; i++) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            System.out.println("生产者 " + i);
            clerk.production();
        }
    }
}

// 消费者
@Data
class Consumer1 implements Runnable {
    private Clerk1 clerk;

    @Override
    public void run() {
        for (int i = 0; i < 20; i++) {
            System.out.println("消费者 " + i);
            clerk.consumption();
        }
    }
}