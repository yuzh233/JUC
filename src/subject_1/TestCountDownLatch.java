package subject_1;

import java.util.concurrent.CountDownLatch;

/**
 * @Author: yu_zh
 * @DateTime: 2018/08/30 14:11
 */
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