package com.jenkins.jplugin.gleaning.model;

/**
 * 2020-04-21  add by wanghf
 */
public class CronTask extends Task {

    private String calContext;
    private String calBuildTime;

    public String getCalContext() {
        return calContext;
    }

    public void setCalContext(String calContext) {
        this.calContext = calContext;
    }

    public String getCalBuildTime() {
        return calBuildTime;
    }

    public void setCalBuildTime(String calBuildTime) {
        this.calBuildTime = calBuildTime;
    }

}
