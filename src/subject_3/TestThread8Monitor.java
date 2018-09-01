package subject_3;

/**
 * @Author: yu_zh
 * @DateTime: 2018/09/01 18:09
 */
public class TestThread8Monitor {
    public static void main(String[] args) {
        Number number = new Number();
        Number number2 = new Number();

        new Thread(new Runnable() {
            @Override
            public void run() {
                number.one();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                number2.two();
            }
        }).start();
    }
}

class Number {

    public static synchronized void one() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("one");
    }

    public synchronized void two() {
        System.out.println("two");
    }

    public synchronized void three() {
        System.out.println("three");
    }
}