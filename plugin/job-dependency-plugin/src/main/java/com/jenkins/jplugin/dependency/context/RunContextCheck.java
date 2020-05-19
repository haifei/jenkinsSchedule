package com.jenkins.jplugin.dependency.context;

import com.jenkins.jplugin.dependency.constant.Constants;
import com.jenkins.jplugin.dependency.trigger.JobDependencyBuildTrigger;
import hudson.AbortException;
import hudson.model.*;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * 2020-04-12 add by wanghf
 */
public class RunContextCheck {

    private static final Logger LOGGER = Logger.getLogger(RunContextCheck.class.getName());

    private Run r;

    public RunContextCheck(Run r) {
        this.r = r;
    }

    /**
     * 判断 JobDependencyBuildTrigger 任务是否存在context,并且context符合规范
     */
    public void checkJobDependencyContext() throws AbortException {
        JobDependencyBuildTrigger t = ParameterizedJobMixIn.getTrigger(r.getParent(), JobDependencyBuildTrigger.class);
        ParametersAction triggeredJobAction = r.getAction(ParametersAction.class);

        Jenkins jenkins = Jenkins.getInstance();
        List<AbstractProject> downstream = jenkins.getDependencyGraph().getDownstream((AbstractProject) r.getParent());
        //判断任务是否有依赖
        if (t != null
                || (null != downstream
                && downstream.size() != 0
                && projectsHasJobDependencyBuildTrigger(downstream))) {
            if (null == triggeredJobAction
                    || null == triggeredJobAction.getParameter(Constants.TOKEN)) {
                LOGGER.warning("Current project params can't be empty and token:time_hour is needed.");
                throw new AbortException("当前任务的参数不能为空,而且必须存在参数:time_hour");
            }

            //校验time_hour格式
            String _time_hour = (String) (triggeredJobAction.getParameter(Constants.TOKEN).getValue());
            Matcher matcher = Constants.pattern.matcher(_time_hour);
            if (!matcher.matches()) {
                LOGGER.warning("Current project token:time_hour is unvaliable.");
                throw new AbortException(String.format("当前任务的参数:time_hour:%s的格式不合法,合法的格式:yyyy/MM/dd/HH", _time_hour));
            }
        }
    }

    /**
     * 判断下游列表中是否存在 JobDependencyBuildTrigger
     *
     * @param projects
     *
     */
    private boolean projectsHasJobDependencyBuildTrigger(List<AbstractProject> projects) {
        for (AbstractProject project : projects) {
            JobDependencyBuildTrigger trigger = ParameterizedJobMixIn.getTrigger(project, JobDependencyBuildTrigger.class);
            if (null != trigger) {
                return true;
            }
        }
        return false;
    }
}
