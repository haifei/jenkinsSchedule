package com.jenkins.jplugin.depgraph.job_depgraph.model.context;

/**
 * 2020-04-12 add by wanghf
 */
public class BuildInfo {
    private String jobName;
    private String jobRunStartTime;
    private long jobRunEndTime;
    private String streamFlag = "-";
    private String context;
    private String jobTrigger;
    private String jobLastInstanceID;
    private String jobCreator;
    private String jobStatus;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobRunStartTime() {
        return jobRunStartTime;
    }

    public void setJobRunStartTime(String jobRunStartTime) {
        this.jobRunStartTime = jobRunStartTime;
    }

    public long getJobRunEndTime() {
        return jobRunEndTime;
    }

    public void setJobRunEndTime(long jobRunEndTime) {
        this.jobRunEndTime = jobRunEndTime;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getJobTrigger() {
        return jobTrigger;
    }

    public void setJobTrigger(String jobTrigger) {
        this.jobTrigger = jobTrigger;
    }

    public String getJobLastInstanceID() {
        return jobLastInstanceID;
    }

    public void setJobLastInstanceID(String jobLastInstanceID) {
        this.jobLastInstanceID = jobLastInstanceID;
    }

    public String getJobCreator() {
        return jobCreator;
    }

    public void setJobCreator(String jobCreator) {
        this.jobCreator = jobCreator;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getStreamFlag() {
        return streamFlag;
    }

    public void setStreamFlag(String streamFlag) {
        this.streamFlag = streamFlag;
    }
}
