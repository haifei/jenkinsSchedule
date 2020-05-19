package com.jenkins.jplugin.dependency.pojo;

import com.jenkins.jplugin.dependency.enums.DateType;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 2020-04-12 add by wanghf
 */
public class OnlyBuildOnceForDependencyJob {

    //这里应该用小写啊,为了避免现在修改这里造成老的数据无法获取,所以这里就不改了,知道这点问题.
    public boolean OnlyBuildOnce;

    private boolean triggerByUser;

    private DateType dateType;

    @DataBoundConstructor
    public OnlyBuildOnceForDependencyJob(boolean OnlyBuildOnce,
                                         boolean triggerByUser,
                                         DateType dateType) {
        this.OnlyBuildOnce = OnlyBuildOnce;
        this.triggerByUser = triggerByUser;
        this.dateType = dateType;
    }

    public boolean isTriggerByUser() {
        return triggerByUser;
    }

    public void setTriggerByUser(boolean triggerByUser) {
        this.triggerByUser = triggerByUser;
    }

    public boolean isOnlyBuildOnce() {
        return OnlyBuildOnce;
    }

    public void setOnlyBuildOnce(boolean onlyBuildOnce) {
        this.OnlyBuildOnce = onlyBuildOnce;
    }

    public DateType getDateType() {
        return dateType;
    }

    public void setDateType(DateType dateType) {
        this.dateType = dateType;
    }
}
