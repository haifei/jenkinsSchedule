package hudson.model;

import hudson.views.ListViewColumn;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author wanghf
 * @date 2020/4/18
 * @desc
 */
public class JobFailoverTimesShowColumn extends ListViewColumn {

    public String getJobFailoverTimes(Job job) {
        String jobFailoverTimes = "3";
        JobProperty pro = job.getProperty(JobFailoverProperty.class);
        if (pro != null) {
            JobFailoverProperty jfp = (JobFailoverProperty) pro;
            jobFailoverTimes = jfp.getFailoverTimes();
        }

        return jobFailoverTimes;
    }


    private static class DescriptorImpl extends Descriptor<ListViewColumn> {
        @Override
        public ListViewColumn newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new JobFailoverTimesShowColumn();
        }

        @Override
        public String getDisplayName() {
            return "重试次数";
        }
    }

}
