package com.jenkins.jplugin.gleaning.model;

/**
 * 2020-04-21  add by wanghf
 */
public class FailureTask extends Task{

    private String lastBuildResult;

    public String getLastBuildResult() {
        return lastBuildResult;
    }

    public void setLastBuildResult(String lastBuildResult) {
        this.lastBuildResult = lastBuildResult;
    }

}
