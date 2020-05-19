package org.jenkinsci.plugins.dependencyqueue;

import hudson.Extension;
import hudson.model.*;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class BlockWhilePriorityQueuedProperty extends JobProperty<AbstractProject<?, ?>> {
    
    //default to true for backward compatibility
    private String priority = "3";
    @DataBoundConstructor
    public BlockWhilePriorityQueuedProperty(String priority) {
        this.priority = priority;
    }

    public String getPriority() {
        return priority;
    }
    
    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public JobProperty<?> newInstance(
                final StaplerRequest req,
                final JSONObject formData
        ) throws FormException {

            final JSONObject watcherData = formData.getJSONObject("taskPriority");
            if (watcherData.isNullObject()) return null;

            final String priority = watcherData.getString( "priority" );
            if (priority == null || priority.isEmpty()) return null;

            return new BlockWhilePriorityQueuedProperty(priority);
        }

        @Override
        public String getDisplayName() {
            return "Task Priority";
        }
    }
}
