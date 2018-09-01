package subject_3;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: yu_zh
 * @DateTime: 2018/09/01 17:17
 */
public class ABCAlternate {
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
