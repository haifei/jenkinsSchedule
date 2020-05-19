package com.jenkins.jplugin.dependency.action.none;

import com.jenkins.jplugin.dependency.action.AbstractUpstreamRuns;
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
public class WeekUpstreamRuns extends AbstractUpstreamRuns {
    private static final Logger logger = LoggerFactory.getLogger(WeekUpstreamRuns.class);

    private TriggerConditionInfo conditionInfo;

    public WeekUpstreamRuns(Job job,
                            RunList<Run> runlist,
                            Date tokenDate,
                            JobDependencyProperty jobProperty,
                            TriggerConditionInfo conditionInfo) {
        super(job, runlist, tokenDate, jobProperty);
        this.conditionInfo = conditionInfo;
    }

    @Override
    public List<Run> obtain() {
        for (String week : conditionInfo.getDateList()) {
            checkWeekCondition(Utils.stringConvertInteger(week));
        }
        return runs();
    }
}
