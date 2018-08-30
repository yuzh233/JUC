package subject_2;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @Author: yu_zh
 * @DateTime: 2018/08/30 14:37
 */
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