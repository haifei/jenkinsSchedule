package com.jenkins.jplugin.dependency.factory;

import com.jenkins.jplugin.dependency.action.AbstractUpstreamRuns;
import com.jenkins.jplugin.dependency.action.none.DayUpstreamRuns;
import com.jenkins.jplugin.dependency.action.none.HourUpstreamRuns;
import com.jenkins.jplugin.dependency.action.none.MonthUpstreamRuns;
import com.jenkins.jplugin.dependency.action.none.WeekUpstreamRuns;
import com.jenkins.jplugin.dependency.exception.JobDependencyException;
import com.jenkins.jplugin.dependency.pojo.JobDependencyProperty;
import com.jenkins.jplugin.dependency.pojo.TriggerConditionInfo;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.RunList;

import java.util.Date;

public class UpstreamRunsFactory {

    public static AbstractUpstreamRuns getInstance(Job job,
                                                   RunList<Run> runlist,
                                                   Date tokenDate,
                                                   JobDependencyProperty jobProperty,
                                                   TriggerConditionInfo conditionInfo) throws JobDependencyException {

        AbstractUpstreamRuns upstreamRuns = null;

        switch (conditionInfo.getDateType()) {
            case HOUR:
                upstreamRuns = new HourUpstreamRuns(job, runlist, tokenDate, jobProperty, conditionInfo);
                break;
            case DAY:
                upstreamRuns = new DayUpstreamRuns(job, runlist, tokenDate, jobProperty, conditionInfo);
                break;
            case WEEK:
                upstreamRuns = new WeekUpstreamRuns(job, runlist, tokenDate, jobProperty, conditionInfo);
                break;
            case MONTH:
                upstreamRuns = new MonthUpstreamRuns(job, runlist, tokenDate, jobProperty, conditionInfo);
                break;
            default:
                throw new JobDependencyException("无法获取AbstractUpstreamRuns对象.");
        }
        return upstreamRuns;
    }

}
