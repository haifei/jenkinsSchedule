package hudson.plugins.customer.data;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.views.ListViewColumn;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;


public class LastBuildTimeColumn extends ListViewColumn {

    private final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public String getLastBuildTime(Job job) {
        Run lastBuild = job.getLastBuild();
        Date lastBuildTime = null;
        if (lastBuild != null) {
            lastBuildTime = lastBuild.getTime();
        }
        String result = "";
        if (lastBuildTime != null) {
            result = (new SimpleDateFormat(DATE_FORMAT)).format(lastBuildTime);
        }
        return result;
    }

    @Extension
    public static final Descriptor<ListViewColumn> DESCRIPTOR = new DescriptorImpl();

    private static class DescriptorImpl extends Descriptor<ListViewColumn> {
        @Override
        public ListViewColumn newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new LastBuildTimeColumn();
        }

        @Override
        public String getDisplayName() {
            return "Last Build Time";
        }
    }
}
