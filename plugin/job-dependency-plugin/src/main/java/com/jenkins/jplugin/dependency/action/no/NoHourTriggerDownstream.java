package com.jenkins.jplugin.dependency.action.no;

import com.jenkins.jplugin.dependency.action.AbstractTriggerDownstream;
import com.jenkins.jplugin.dependency.constant.Constants;
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
public class NoHourTriggerDownstream extends AbstractTriggerDownstream {

    private static final Logger logger = LoggerFactory.getLogger(NoHourTriggerDownstream.class);

    private TriggerConditionInfo conditionInfo;

    public NoHourTriggerDownstream(Job job,
                                   AbstractProject downstream,
                                   AbstractBuild upstreamBuild,
                                   RunList<Run> runlist,
                                   Date tokenDate,
                                   JobDependencyProperty jobProperty,
                                   TriggerConditionInfo conditionInfo) {
        super(job, downstream,upstreamBuild, runlist, tokenDate, jobProperty);
        this.conditionInfo = conditionInfo;
    }

    @Override
    public boolean check() {
        String date = conditionInfo.getDateList().get(0);
        if (date.contains(Constants.DATE_RELY_ON_ONESELF)) {
            date = dateAppendToContext(date);
            return checkTokenHourCondition(Utils.stringConvertInteger(date), true);
        }

        if (date.contains(Constants.DATE_OF_CALCULATION_CONTEXT)) {
            date = dateAppendToContext(date);
            return checkTokenHourCondition(Utils.stringConvertInteger(date), false);
        }

        return checkHourCondition(Utils.stringConvertInteger(date));
    }
}
