package com.jenkins.jplugin.dependency.factory;

import com.jenkins.jplugin.dependency.action.AbstractTriggerDownstream;
import com.jenkins.jplugin.dependency.action.and.AndDayTriggerDownstream;
import com.jenkins.jplugin.dependency.action.and.AndHourTriggerDownstream;
import com.jenkins.jplugin.dependency.action.and.AndMonthTriggerDownstream;
import com.jenkins.jplugin.dependency.action.and.AndWeekTriggerDownstream;
import com.jenkins.jplugin.dependency.action.no.NoDayTriggerDownstream;
import com.jenkins.jplugin.dependency.action.no.NoHourTriggerDownstream;
import com.jenkins.jplugin.dependency.action.no.NoMonthTriggerDownstream;
import com.jenkins.jplugin.dependency.action.no.NoWeekTriggerDownstream;
import com.jenkins.jplugin.dependency.action.or.OrDayTriggerDownstream;
import com.jenkins.jplugin.dependency.action.or.OrHourTriggerDownstream;
import com.jenkins.jplugin.dependency.action.or.OrMonthTriggerDownstream;
import com.jenkins.jplugin.dependency.action.or.OrWeekTriggerDownstream;
import com.jenkins.jplugin.dependency.exception.JobDependencyException;
import com.jenkins.jplugin.dependency.pojo.JobDependencyProperty;
import com.jenkins.jplugin.dependency.pojo.TriggerConditionInfo;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.RunList;

import java.util.Date;

/**
 * 2020-04-12 add by wanghf
 */
public class TriggerDownstreamFactory {

    public static AbstractTriggerDownstream getInstance(Job job,
                                                        AbstractProject downstream,
                                                        AbstractBuild upstreamBuild,
                                                        RunList<Run> runlist,
                                                        Date tokenDate,
                                                        JobDependencyProperty jobProperty,
                                                        TriggerConditionInfo conditionInfo) throws JobDependencyException {

        AbstractTriggerDownstream triggerDownstream = null;

        switch (conditionInfo.getLogicSymbol()) {
            case OR:
                switch (conditionInfo.getDateType()) {
                    case HOUR:
                        triggerDownstream = new OrHourTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    case DAY:
                        triggerDownstream = new OrDayTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    case WEEK:
                        triggerDownstream = new OrWeekTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    case MONTH:
                        triggerDownstream = new OrMonthTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    default:
                        throw new JobDependencyException("无法获取AbstractTriggerDownstream对象.");
                }
                return triggerDownstream;
            case AND:
                switch (conditionInfo.getDateType()) {
                    case HOUR:
                        triggerDownstream = new AndHourTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    case DAY:
                        triggerDownstream = new AndDayTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    case WEEK:
                        triggerDownstream = new AndWeekTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    case MONTH:
                        triggerDownstream = new AndMonthTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    default:
                        throw new JobDependencyException("无法获取AbstractTriggerDownstream对象.");
                }
                return triggerDownstream;
            case NO:
                switch (conditionInfo.getDateType()) {
                    case HOUR:
                        triggerDownstream = new NoHourTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    case DAY:
                        triggerDownstream = new NoDayTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    case WEEK:
                        triggerDownstream = new NoWeekTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    case MONTH:
                        triggerDownstream = new NoMonthTriggerDownstream(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty, conditionInfo);
                        break;
                    default:
                        throw new JobDependencyException("无法获取AbstractTriggerDownstream对象.");
                }
                return triggerDownstream;
            default:
                throw new JobDependencyException("无法获取AbstractTriggerDownstream对象.");
        }
    }
}
