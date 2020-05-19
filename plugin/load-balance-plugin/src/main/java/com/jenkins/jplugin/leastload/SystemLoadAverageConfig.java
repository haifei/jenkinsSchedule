package com.jenkins.jplugin.leastload;

import hudson.Extension;
import jenkins.YesNoMaybe;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;

/**
 * 2020-04-12 add by wanghf
 */
@Extension(dynamicLoadable = YesNoMaybe.YES)
public class SystemLoadAverageConfig extends GlobalConfiguration {

    private static final String SYSTEM_LOAD_AVERAGE_THRESHOLD = "32";

    public static SystemLoadAverageConfig get() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            SystemLoadAverageConfig config = jenkins.getDescriptorByType(SystemLoadAverageConfig.class);
            if (config != null) {
                return config;
            }
        }
        return null;
    }

    public SystemLoadAverageConfig() {
        load();
    }

    @CheckForNull
    private String averageThreshold;

    public String getAverageThreshold() {
        return averageThreshold != null ? averageThreshold : SYSTEM_LOAD_AVERAGE_THRESHOLD;
    }

    public void setAverageThreshold(String averageThreshold) {
        this.averageThreshold = averageThreshold;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

}
