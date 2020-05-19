package com.jenkins.jplugin.dependency.action.none;

import com.jenkins.jplugin.dependency.action.AbstractUpstreamRuns;
import com.jenkins.jplugin.dependency.constant.Constants;
import com.jenkins.jplugin.dependency.pojo.JobDependencyProperty;
import com.jenkins.jplugin.dependency.pojo.TriggerConditionInfo;
import com.jenkins.jplugin.dependency.utils.Utils;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.RunList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * 2020-04-12 add by wanghf
 */
public class DayUpstreamRuns extends AbstractUpstreamRuns {

    private static final Logger logger = LoggerFactory.getLogger(DayUpstreamRuns.class);

    private TriggerConditionInfo conditionInfo;

    public DayUpstreamRuns(Job job,
                           RunList<Run> runlist,
                           Date tokenDate,
                           JobDependencyProperty jobProperty,
                           TriggerConditionInfo conditionInfo) {
        super(job, runlist, tokenDate, jobProperty);
        this.conditionInfo = conditionInfo;
    }

    @Override
    public List<Run> obtain() {
        for (String day : conditionInfo.getDateList()) {
            if (day.contains(Constants.DATE_RELY_ON_ONESELF)) {
                day = dateAppendToContext(day);
                checkTokenDayCondition(Utils.stringConvertInteger(day), true);
            } else if (day.contains(Constants.DATE_OF_CALCULATION_CONTEXT)) {
                day = dateAppendToContext(day);
                checkTokenDayCondition(Utils.stringConvertInteger(day), false);
            } else if (Constants.DAY_OF_LAST_MONTH.equals(day)) {
                checkLastDayCondition();
            } else if (Constants.DAY_OF_ANY_MONTH.equals(day)) {
                checkAnyDayCondition();
            } else {
                checkDayCondition(Utils.stringConvertInteger(day));
            }
        }
        return runs();
    }
}
