package com.jenkins.jplugin.dependency.action.and;

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
public class AndHourTriggerDownstream extends AbstractTriggerDownstream {

    private static final Logger logger = LoggerFactory.getLogger(AndHourTriggerDownstream.class);

    private TriggerConditionInfo conditionInfo;

    public AndHourTriggerDownstream(Job job,
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
        for (String hour : conditionInfo.getDateList()) {
            if (hour.contains(Constants.DATE_RELY_ON_ONESELF)) {
                hour = dateAppendToContext(hour);
                if (!checkTokenHourCondition(Utils.stringConvertInteger(hour), true)) {
                    return false;
                }
            } else if (hour.contains(Constants.DATE_OF_CALCULATION_CONTEXT)) {
                hour = dateAppendToContext(hour);
                if (!checkTokenHourCondition(Utils.stringConvertInteger(hour), false)) {
                    return false;
                }
            } else if (!checkHourCondition(Utils.stringConvertInteger(hour))) {
                return false;
            }
        }
        return true;
    }
}
