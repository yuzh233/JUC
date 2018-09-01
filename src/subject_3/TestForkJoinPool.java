package subject_3;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * @Author: yu_zh
 * @DateTime: 2018/09/01 21:53
 * <p>
 * 使用Fork/join框架解决问题
 */
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