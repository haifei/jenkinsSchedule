package com.jenkins.jplugin.dependency.context;

import com.jenkins.jplugin.dependency.constant.Constants;
import com.jenkins.jplugin.dependency.exception.JobDependencyException;
import com.jenkins.jplugin.dependency.factory.TriggerDownstreamFactory;
import com.jenkins.jplugin.dependency.factory.UpstreamRunsFactory;
import com.jenkins.jplugin.dependency.pojo.JobDependencyProperty;
import com.jenkins.jplugin.dependency.pojo.TriggerConditionInfo;
import com.jenkins.jplugin.dependency.utils.DateUtils;
import com.jenkins.jplugin.dependency.utils.Utils;
import hudson.model.*;
import hudson.util.RunList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 2020-04-12 add by wanghf
 */
public class JobBuildHistory {

    private static final Logger logger = LoggerFactory.getLogger(JobBuildHistory.class);

    private Job job;
    private AbstractProject downstream;
    private TriggerConditionInfo conditionInfo;
    private JobDependencyProperty jobProperty;
    private ParameterValue token;
    private TaskListener listener;
    private RunList<Run> runlist;
    private Date tokenDate;
    private List<Run> runs;
    private AbstractBuild upstreamBuild;


    public JobBuildHistory(Job job,
                           AbstractProject downstream,
                           AbstractBuild upstreamBuild,
                           TriggerConditionInfo conditionInfo,
                           JobDependencyProperty jobProperty,
                           ParameterValue token,
                           TaskListener listener) throws JobDependencyException {
        this(job, downstream, upstreamBuild, conditionInfo, jobProperty, token);
        this.listener = listener;
    }

    public JobBuildHistory(Job job,
                           TriggerConditionInfo conditionInfo,
                           JobDependencyProperty jobProperty,
                           ParameterValue token) throws JobDependencyException {
        this(job, null, null, conditionInfo, jobProperty, token);
    }

    public JobBuildHistory(Job job,
                           AbstractProject downstream,
                           AbstractBuild upstreamBuild,
                           TriggerConditionInfo conditionInfo,
                           JobDependencyProperty jobProperty,
                           ParameterValue token) throws JobDependencyException {
        runs = new ArrayList<Run>();
        this.job = job;
        this.downstream = downstream;
        this.upstreamBuild = upstreamBuild;
        this.conditionInfo = conditionInfo;
        this.jobProperty = jobProperty;
        this.token = token;
        //如果含有%$，则获取当前任务的下游的构建历史
        if (Utils.containValueInCollection(conditionInfo.getDateList(), Constants.DATE_RELY_ON_ONESELF)) {
            runlist = downstream.getNewBuilds();
        } else {
            runlist = job.getNewBuilds();
        }
        tokenDate = DateUtils.parseDate(String.valueOf(this.token.getValue()));
    }


    /**
     * 检查job的构建记录中是否存在符合条件的Run
     * @return boolean
     * @throws  JobDependencyException
     */
    public boolean check() throws JobDependencyException {
        return TriggerDownstreamFactory.getInstance(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo).check();
    }

    /**
     * 检查job的构建记录中是否存在符合条件的Run,并且返回
     *
     * @return  List
     */
    public List<Run> getRuns() throws JobDependencyException {
        return UpstreamRunsFactory.getInstance(job, runlist, tokenDate, jobProperty, conditionInfo).obtain();
    }

}
