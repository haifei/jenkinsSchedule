package com.jenkins.jplugin.dependency.pojo;

/**
 * 2020-04-12 add by wanghf
 */
public class RunForUpstreamJobInfo {

    String jobName;

    String buildId;

    public RunForUpstreamJobInfo(String jobName, String buildId) {
        this.jobName = jobName;
        this.buildId = buildId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }
}
