package hudson.plugins.swarm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main1 {
    public static void main(String[] args) throws InterruptedException {
        Worker2 work = new Worker2();
        work.start();
       /* BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            if(Boolean.parseBoolean(br.readLine())) {
                work.interrupt();        //中断线程
                System.out.println("线程是否中断？" + work.isInterrupted());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/

       Thread.sleep(10000);
        work.interrupt();
        System.out.println("线程是否中断？" + work.isInterrupted());

    }
}

class Worker2 extends Thread {
    @Override
    public void run() {
        int i = 0;
        while(!isInterrupted()) {        //注意这里isInterrupted()方法的使用
            System.out.println("number: " + i++);
        }
    }
}