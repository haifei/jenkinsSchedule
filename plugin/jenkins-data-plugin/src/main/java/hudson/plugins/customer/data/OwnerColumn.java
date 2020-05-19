package hudson.plugins.customer.data;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.views.ListViewColumn;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public class OwnerColumn extends ListViewColumn {
    public String getOwnerName(Job job) {
        return job.getOwnerName();
    }

    @Extension
    public static final Descriptor<ListViewColumn> DESCRIPTOR = new DescriptorImpl();

    private static class DescriptorImpl extends Descriptor<ListViewColumn> {
        @Override
        public OwnerColumn newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new OwnerColumn();
        }

        @Override
        public String getDisplayName() {
            return "Owner Name";
        }
    }
}