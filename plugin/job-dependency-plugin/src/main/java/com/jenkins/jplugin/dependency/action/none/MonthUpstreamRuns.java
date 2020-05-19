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
public class MonthUpstreamRuns extends AbstractUpstreamRuns {
    private static final Logger logger = LoggerFactory.getLogger(MonthUpstreamRuns.class);

    private TriggerConditionInfo conditionInfo;

    public MonthUpstreamRuns(Job job,
                             RunList<Run> runlist,
                             Date tokenDate,
                             JobDependencyProperty jobProperty,
                             TriggerConditionInfo conditionInfo) {
        super(job, runlist, tokenDate, jobProperty);
        this.conditionInfo = conditionInfo;
    }

    @Override
    public List<Run> obtain() {
        for (String month : conditionInfo.getDateList()) {
            if (month.contains(Constants.DATE_RELY_ON_ONESELF)) {
                month = dateAppendToContext(month);
                checkTokenMonthCondition(Utils.stringConvertInteger(month), true);
            } else if (month.contains(Constants.DATE_OF_CALCULATION_CONTEXT)) {
                month = dateAppendToContext(month);
                checkTokenMonthCondition(Utils.stringConvertInteger(month), false);
            } else {
                checkMonthCondition(Utils.stringConvertInteger(month));
            }
        }
        return runs();
    }
}
