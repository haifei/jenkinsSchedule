package hudson.plugins.swarm;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * 2020-04-12 add by wanghf
 */
public class SystemLoadAverageThread extends Thread {

    private SwarmClient client;
    private Candidate target;

    public SystemLoadAverageThread(SwarmClient client,
                                   Candidate target) {
        this.client = client;
        this.target = target;
    }

    @Override
    public void run() {
        while(!isInterrupted()) {
            final OperatingSystemMXBean opsysMXbean = ManagementFactory
                    .getOperatingSystemMXBean();
             String average = String.format("%.2f", opsysMXbean.getSystemLoadAverage());
            try {
                client.postSystemLoadAverage(average,target);
                Thread.sleep(5000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RetryException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
