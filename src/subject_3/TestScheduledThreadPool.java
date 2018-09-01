package subject_3;

import java.util.Random;
import java.util.concurrent.*;

/**
 * @Author: yu_zh
 * @DateTime: 2018/09/01 20:19
 */
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
