package subject_3;

/**
 * @Author: yu_zh
 * @DateTime: 2018/09/01 16:58
 */
public class TestABCAlternate {
    public static void main(String[] args) {
        ABCAlternate alternate = new ABCAlternate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    alternate.printA();
                }
            }
        }, "A").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    alternate.printB();
                }
            }
        }, "B").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    alternate.printC();
                }
            }
        }, "C").start();
    }
}
