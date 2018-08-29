package subject_1;

import java.util.concurrent.atomic.*;

/**
 * @Author: yu_zh
 * @DateTime: 2018/08/29 22:44
 */
public class TestAtomicDemo {

    public static void main(String[] args) {
        AtomicDemo ad = new AtomicDemo();

        for (int i = 0; i < 10; i++) {
            new Thread(ad).start();
        }
    }

}

class AtomicDemo implements Runnable {

//	private volatile int serialNumber = 0;
    private AtomicInteger serialNumber = new AtomicInteger(0);

    @Override
    public void run() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        System.out.println(getSerialNumber());
    }

    public int getSerialNumber() {
        return serialNumber.getAndIncrement();
    }

}