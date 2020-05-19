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
public class AndMonthTriggerDownstream extends AbstractTriggerDownstream {

    private static final Logger logger = LoggerFactory.getLogger(AndMonthTriggerDownstream.class);

    private TriggerConditionInfo conditionInfo;

    public AndMonthTriggerDownstream(Job job,
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
        for (String month : conditionInfo.getDateList()) {
            if (month.contains(Constants.DATE_RELY_ON_ONESELF)) {
                month = dateAppendToContext(month);
                if (!checkTokenMonthCondition(Utils.stringConvertInteger(month), true)) {
                    return false;
                }
            } else if (month.contains(Constants.DATE_OF_CALCULATION_CONTEXT)) {
                month = dateAppendToContext(month);
                if (!checkTokenMonthCondition(Utils.stringConvertInteger(month), false)) {
                    return false;
                }
            } else if (!checkMonthCondition(Utils.stringConvertInteger(month))) {
                return false;
            }
        }
        return true;
    }
}
