package com.jenkins.jplugin.dependency.action.no;

import com.jenkins.jplugin.dependency.action.AbstractTriggerDownstream;
import com.jenkins.jplugin.dependency.pojo.JobDependencyProperty;
import com.jenkins.jplugin.dependency.pojo.TriggerConditionInfo;
import com.jenkins.jplugin.dependency.utils.Utils;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.RunList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * 2020-04-12 add by wanghf
 */
public class NoWeekTriggerDownstream extends AbstractTriggerDownstream {

    private static final Logger logger = LoggerFactory.getLogger(NoWeekTriggerDownstream.class);

    private TriggerConditionInfo conditionInfo;

    public NoWeekTriggerDownstream(Job job,
                                   AbstractProject downstream,
                                   AbstractBuild upstreamBuild,
                                   RunList<Run> runlist,
                                   Date tokenDate,
                                   JobDependencyProperty jobProperty,
                                   TriggerConditionInfo conditionInfo) {
        super(job, downstream, upstreamBuild, runlist, tokenDate, jobProperty);
        this.conditionInfo = conditionInfo;
    }

    @Override
    public boolean check() {

        String date = conditionInfo.getDateList().get(0);
        return checkWeekCondition(Utils.stringConvertInteger(date));
    }
}
