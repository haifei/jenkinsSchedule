package hudson.model;

import hudson.views.ListViewColumn;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author wanghf
 * @date 2020/4/18
 * @desc
 */
public class JobFailoverDelayTimeShowColumn extends ListViewColumn {

    public String getJobFailoverDelayTime(Job job) {
        String jobFailoverDelayTime = "10";
        JobProperty pro = job.getProperty(JobFailoverProperty.class);
        if (pro != null) {
            JobFailoverProperty jfp = (JobFailoverProperty) pro;
            jobFailoverDelayTime = jfp.getFailoverDelayTime();
        }

        return jobFailoverDelayTime;
    }


    private static class DescriptorImpl extends Descriptor<ListViewColumn> {
        @Override
        public ListViewColumn newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new JobFailoverDelayTimeShowColumn();
        }

        @Override
        public String getDisplayName() {
            return "重试间隔时间";
        }
    }
}
