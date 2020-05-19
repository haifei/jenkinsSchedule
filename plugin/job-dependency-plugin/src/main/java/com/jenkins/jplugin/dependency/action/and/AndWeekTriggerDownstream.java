package com.jenkins.jplugin.dependency.action.and;

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
public class AndWeekTriggerDownstream extends AbstractTriggerDownstream {

    private static final Logger logger = LoggerFactory.getLogger(AndWeekTriggerDownstream.class);

    private TriggerConditionInfo conditionInfo;

    public AndWeekTriggerDownstream(Job job,
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
        for (String week : conditionInfo.getDateList()) {
            if (!checkWeekCondition(Utils.stringConvertInteger(week))) {
                return false;
            }
        }
        return true;
    }
}
