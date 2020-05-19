package com.jenkins.jplugin.dependency.pojo;

//import com.sun.istack.internal.NotNull;
import com.jenkins.jplugin.dependency.enums.ResultCondition;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 2020-04-12 add by wanghf
 */
public final class JobDependencyProperty {

    private String upstreamJobName;

    private String triggerCondition;

    private ResultCondition threshold;

    @DataBoundConstructor
    public JobDependencyProperty(String upstreamJobName, String triggerCondition, ResultCondition threshold) {
        this.upstreamJobName = upstreamJobName;
        this.triggerCondition = triggerCondition;
        this.threshold = threshold;
    }

    public String getUpstreamJobName() {
        return upstreamJobName;
    }

    public void setUpstreamJobName(String upstreamJobName) {
        this.upstreamJobName = upstreamJobName;
    }

    public String getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(String triggerCondition) {
        this.triggerCondition = triggerCondition;
    }

    public ResultCondition getThreshold() {
        return threshold;
    }

    public void setThreshold(ResultCondition threshold) {
        this.threshold = threshold;
    }
}
