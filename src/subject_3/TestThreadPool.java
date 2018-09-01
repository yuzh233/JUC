package subject_3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @Author: yu_zh
 * @DateTime: 2018/09/01 19:31
 */
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