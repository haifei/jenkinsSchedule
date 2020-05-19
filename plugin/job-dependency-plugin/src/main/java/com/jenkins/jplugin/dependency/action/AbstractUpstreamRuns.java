package com.jenkins.jplugin.dependency.action;

import com.jenkins.jplugin.dependency.pojo.JobDependencyProperty;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.RunList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 2020-04-12 add by wanghf
 */
public class AbstractUpstreamRuns extends AbstractTriggerDownstream {

    private static final Logger logger = LoggerFactory.getLogger(AbstractUpstreamRuns.class);

    public static final Integer DEFAULT_DATE = 0;

    private List<Run> runs = new ArrayList<>();

    public AbstractUpstreamRuns(Job job,
                                RunList<Run> runlist,
                                Date tokenDate,
                                JobDependencyProperty jobProperty) {
        super(job, null, null, runlist, tokenDate, jobProperty);
    }

    @Override
    protected boolean matchToken(Run run, String time_hour) {
        if (null != run.getResult()
                && jobProperty.getThreshold().isMet(run.getResult())) {
            logger.info(String.format("当前任务:%s time_hour:%s 的构建是存在的,而且状态为成功", job.getName(), time_hour));
            //符合条件的run存起来
            runs.add(run);
            return true;
        } else {
            logger.warn(String.format("当前任务:%s time_hour:%s 的构建是存在的,但是状态为失败,无法触发下游任务:%s",
                    job.getName(), time_hour, downstream.getName()));
            return false;
        }
    }

    public List<Run> runs() {
        return runs;
    }
}
