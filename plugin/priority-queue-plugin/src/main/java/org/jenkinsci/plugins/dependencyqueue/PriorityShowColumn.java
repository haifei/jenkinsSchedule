package org.jenkinsci.plugins.dependencyqueue;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.views.ListViewColumn;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;

/**
 * 2020-04-12 add by wanghf
 */
public class PriorityShowColumn extends ListViewColumn {
    private static final Logger LOGGER = Logger.getLogger(PriorityShowColumn.class.getName());
    public String getJobPriority(Job job) {
        String priorityDefault = "3";
        JobProperty pro = job.getProperty("org.jenkinsci.plugins.dependencyqueue.BlockWhilePriorityQueuedProperty");
        if(pro != null){
//            try {
//                pro.getClass().getClassLoader().loadClass("org.jenkinsci.plugins.dependencyqueue.BlockWhilePriorityQueuedProperty");
//            } catch (ClassNotFoundException e) {
//                LOGGER.warning(e.getMessage());
//            }
//            LOGGER.info("pros classLoader: " + pro.getClass().getClassLoader());
//            LOGGER.info("BlockWhilePriorityQueuedProperty classLoader: "+BlockWhilePriorityQueuedProperty.class.getClassLoader());
            org.jenkinsci.plugins.dependencyqueue.BlockWhilePriorityQueuedProperty bp = (org.jenkinsci.plugins.dependencyqueue.BlockWhilePriorityQueuedProperty) pro;
            priorityDefault = bp.getPriority();
        }
        return priorityDefault;
    }

    @Extension(ordinal = 50)
    public static final Descriptor<ListViewColumn> DESCRIPTOR = new DescriptorImpl();

    private static class DescriptorImpl extends Descriptor<ListViewColumn> {
        @Override
        public ListViewColumn newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new PriorityShowColumn();
        }

        @Override
        public String getDisplayName() {
            return "优先级";
        }
    }
}
