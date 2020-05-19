package hudson.model;

import hudson.Extension;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author wanghf
 * @date 2020/4/18
 * @desc
 */
public class JobFailoverProperty extends JobProperty<AbstractProject<?, ?>> {
    /**
     * 任务失败重试次数
     */
    private String failoverTimes = "3";

    /**
     * 任务失败 间隔尝试时间，单位秒
     */
    private String failoverDelayTime = "10";


    @DataBoundConstructor
    public JobFailoverProperty(String failoverTimes, String failoverDelayTime) {
        this.failoverTimes = failoverTimes;
        this.failoverDelayTime = failoverDelayTime;
    }

    public String getFailoverTimes() {
        return failoverTimes;
    }


    public String getFailoverDelayTime() {
        return failoverDelayTime;
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public JobProperty<?> newInstance(
                final StaplerRequest req,
                final JSONObject formData
        ) throws FormException {

            final JSONObject watcherData = formData.getJSONObject("taskFailover");
            if (watcherData.isNullObject()) {
                return null;
            }

            final String taskFailoverTimes = watcherData.getString("jobFailoverTimes");
            final String taskFailoverDelayTime = watcherData.getString("jobFailoverDelayTime");
            if (taskFailoverTimes == null || taskFailoverTimes.isEmpty() ||
                    taskFailoverDelayTime == null || taskFailoverDelayTime.isEmpty()) {
                return null;
            }
            return new JobFailoverProperty(taskFailoverTimes, taskFailoverDelayTime);
        }

        @Override
        public String getDisplayName() {
            return "Job Failover";
        }
    }

}
