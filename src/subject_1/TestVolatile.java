package subject_1;

/**
 * @Author: yu_zh
 * @DateTime: 2018/08/29 18:39
 */
public class TestVolatile {
    public static void main(String[] args) {
        // 线程1
        ThreadDemo td = new ThreadDemo();
        new Thread(td).start();

        // 主线程循环的访问共享资源
        while (true) {
            if (td.isFlag()) {
                System.out.println("------------------");
                break;
            }
        }

    }
}

class ThreadDemo implements Runnable {
    private volatile boolean flag = false; // 共享资源

    @Override
    public void run() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        flag = true;
        System.out.println("flag=" + isFlag());
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}

