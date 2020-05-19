package com.jenkins.jplugin.dependency.action.or;

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
public class OrDayTriggerDownstream extends AbstractTriggerDownstream {

    private static final Logger logger = LoggerFactory.getLogger(OrDayTriggerDownstream.class);

    private TriggerConditionInfo conditionInfo;

    public OrDayTriggerDownstream(Job job,
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
        for (String day : conditionInfo.getDateList()) {
            if (Constants.DAY_OF_LAST_MONTH.equals(day)) {
                if (checkLastDayCondition()) {
                    return true;
                }
            } else if (Constants.DAY_OF_ANY_MONTH.equals(day)) {
                if (checkAnyDayCondition()) {
                    return true;
                }
            } else if (day.contains(Constants.DATE_OF_CALCULATION_CONTEXT)) {
                day = dateAppendToContext(day);
                if (checkTokenDayCondition(Utils.stringConvertInteger(day), true)) {
                    return true;
                }
            } else if (day.contains(Constants.DATE_RELY_ON_ONESELF)) {
                day = dateAppendToContext(day);
                if (checkTokenDayCondition(Utils.stringConvertInteger(day), false)) {
                    return true;
                }
            } else if (checkDayCondition(Utils.stringConvertInteger(day))) {
                return true;
            }
        }
        return false;
    }
}
