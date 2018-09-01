package subject_3;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author: yu_zh
 * @DateTime: 2018/09/01 17:44
 */
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