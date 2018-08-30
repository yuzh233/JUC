package subject_2;

import lombok.Data;

/**
 * @Author: yu_zh
 * @DateTime: 2018/08/30 15:55
 * <p>
 * 生产者消费者模型及相关问题
 */
public class TestProducerAndConsumer {
    public static void main(String[] args) {
        Clerk clerk = new Clerk();

        Producer producer = new Producer();
        producer.setClerk(clerk);
        Consumer consumer = new Consumer();
        consumer.setClerk(clerk);

        new Thread(producer, "生产者A：").start();
        new Thread(consumer, "消费者B：").start();
        new Thread(producer, "生产者C：").start();
        new Thread(consumer, "消费者D：").start();
    }
}

// 店员（维护共享资源——产品，生产和消费）
@Data
class Clerk {
    private int product;

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
}

// 生产者
@Data
class Producer implements Runnable {
    private Clerk clerk;

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
class Consumer implements Runnable {
    private Clerk clerk;

    @Override
    public void run() {
        for (int i = 0; i < 20; i++) {
            System.out.println("消费者 " + i);
            clerk.consumption();
        }
    }
}

