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
public class HourUpstreamRuns extends AbstractUpstreamRuns {

    private static final Logger logger = LoggerFactory.getLogger(HourUpstreamRuns.class);

    private TriggerConditionInfo conditionInfo;

    public HourUpstreamRuns(Job job,
                            RunList<Run> runlist,
                            Date tokenDate,
                            JobDependencyProperty jobProperty,
                            TriggerConditionInfo conditionInfo) {
        super(job, runlist, tokenDate, jobProperty);
        this.conditionInfo = conditionInfo;
    }

    @Override
    public List<Run> obtain() {
        for (String hour : conditionInfo.getDateList()) {

            if (hour.contains(Constants.DATE_RELY_ON_ONESELF)) {
                hour = dateAppendToContext(hour);
                checkTokenHourCondition(Utils.stringConvertInteger(hour), true);
            } else if (hour.contains(Constants.DATE_OF_CALCULATION_CONTEXT)) {
                hour = dateAppendToContext(hour);
                checkTokenHourCondition(Utils.stringConvertInteger(hour), false);
            } else {
                checkHourCondition(Utils.stringConvertInteger(hour));
            }
        }
        return runs();
    }
}
