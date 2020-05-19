package com.jenkins.jplugin.dependency.context;

import com.google.common.base.Joiner;
import com.jenkins.jplugin.dependency.constant.Constants;
import com.jenkins.jplugin.dependency.exception.FormValidationException;
import com.jenkins.jplugin.dependency.exception.JobDependencyException;
import com.jenkins.jplugin.dependency.exception.JobDependencyRuntimeException;
import com.jenkins.jplugin.dependency.pojo.JobDependencyProperty;
import com.jenkins.jplugin.dependency.pojo.RunForUpstreamJobInfo;
import com.jenkins.jplugin.dependency.pojo.TriggerConditionInfo;
import com.jenkins.jplugin.dependency.utils.Utils;
import hudson.model.*;
import hudson.util.RunList;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.jenkins.jplugin.dependency.context.UpstreamTriggerRun.ParameterValueInfo.getParameterValue;

/**
 * 2020-04-12 add by wanghf
 */
public class UpstreamTriggerRun {

    //当前构建的Run
    private Run run;

    //上游任务的JobName和BuildId
    private RunForUpstreamJobInfo runForUpstreamJobInfo;

    //上游任务的Condition对象
    private JobDependencyProperty[] jobDependencyProperties;

    //上游任务的job列表
    private List<Job> upstreamJobs;

    //上游传下来的context
    private ParameterValue tokenValue = null;

    public UpstreamTriggerRun(Run run,
                              JobDependencyProperty[] jobDependencyProperties,
                              List<Job> upstreamJobs) {
        this.run = run;
        this.jobDependencyProperties = jobDependencyProperties;
        this.upstreamJobs = upstreamJobs;

        //获取上游job信息
        this.runForUpstreamJobInfo = getRunForUpstreamJobInfo(run);

        //获取上游传下来的context
        tokenValue = getParameterValue(triggerUpstreamRun(), Constants.TOKEN);
    }

    /**
     * 获取上游任务的 Run,针对触发下游的任务
     *
     *
     */
    public Run triggerUpstreamRun() {
        for (Job job : upstreamJobs) {
            if (runForUpstreamJobInfo.getJobName().equals(job.getName())) {

                Run run = job.getBuild(runForUpstreamJobInfo.getBuildId());
                if (null == run)
                    throw new JobDependencyRuntimeException(String.format("获取不到上游的构建任务.上游任务:%s 构建id:%s",
                            runForUpstreamJobInfo.getJobName(), runForUpstreamJobInfo.getBuildId()));
                return run;
            }
        }
        List<String> upstreamJobNames = new ArrayList<>();
        for (Job job : upstreamJobs)
            upstreamJobNames.add(job.getName());
        throw new JobDependencyRuntimeException(String.format("获取不到上游的构建任务.当前任务:%s 上游的构建任务列表:%s", run.getParent().getName(),
                Joiner.on(",").join(upstreamJobNames)));
    }

    /**
     * 获取上游任务的 Run 列表,针对非触发下游的任务
     *
     */
    public List<Run> NotTriggerUpstreamRun() throws JobDependencyException {

        List<Run> allRuns = getAllUpstreamRuns();
        allRuns.remove(triggerUpstreamRun());

        return allRuns;
    }

    /**
     * 获取所有上游的Run,包括condition中的所有的Run,这里的Run是指JobDependencyProperty对应的Run.
     *
     * @return
     */
    private List<Run> getAllUpstreamRuns() throws JobDependencyException {

        List<Run> runs = new ArrayList<>();

        for (JobDependencyProperty dependencyProperty : jobDependencyProperties) {
            if (StringUtils.isEmpty(dependencyProperty.getTriggerCondition())) {
                Run run = getRunByContext(jobByName(dependencyProperty.getUpstreamJobName()), (String) tokenValue.getValue());
                runs.add(run);
            } else {
                TriggerConditionInfo conditionInfo = new TriggerConditionParser(dependencyProperty).parse();
                List<Run> runsOfconditionJob = new JobBuildHistory(jobByName(dependencyProperty.getUpstreamJobName()),
                        conditionInfo,
                        dependencyProperty,
                        tokenValue).getRuns();
                runs.addAll(runsOfconditionJob);
            }
        }
        return runs;
    }

    private Job jobByName(String name) {
        for (Job job : upstreamJobs) {
            if (name.equals(job.getName())) {
                return job;
            }
        }
        throw new JobDependencyRuntimeException(String.format("Job 不存在.name:%s", name));
    }

    /**
     * 先从触发的上游任务尝试获取值,如果没有的话,再从非触发的上游任务中获取值
     *
     * @param param
     * @throws JobDependencyException
     */
    public ParameterValue getUpstreamParamValueByParam(String param) throws JobDependencyException {

        ParameterValue value = null;

        Run triggerRun = triggerUpstreamRun();
        ParameterValue triggerRunValue = getParameterValue(triggerRun, param);

        if (null != triggerRunValue) {
            value = triggerRunValue;
        } else {
            for (Run notTriggerRun : NotTriggerUpstreamRun()) {
                ParameterValue notTriggerRunValue = getParameterValue(notTriggerRun, param);
                if (null != notTriggerRunValue) {
                    value = notTriggerRunValue;
                    break;
                }
            }
        }
        return value;
    }

    public Run getUpstreamRunByNameAndDateType(String name, String dateType) throws FormValidationException, JobDependencyException {
        for (Run run : getAllUpstreamRuns()) {
            if (name.equals(run.getParent().getName())) {
                TriggerConditionInfo parser = new TriggerConditionParser(jobDependencyPropertyByName(name)).parse();
                String date = new DateTypeValue(getParameterValue(run, Constants.TOKEN)).dateType(parser.getDateType());
                //可能存在01等情况
                try {
                    if (Integer.parseInt(dateType) ==
                            Integer.parseInt(date)) {
                        return run;
                    }
                } catch (NumberFormatException e) {
                    throw new FormValidationException(String.format("日期转化有问题.任务名:%s 参数日期:%s context日期:%s",
                            name, dateType, date), e);
                }
            }
        }
        throw new JobDependencyRuntimeException(String.format("找不到任务:%s date:%s", name, dateType));
    }

    private JobDependencyProperty jobDependencyPropertyByName(String name) throws FormValidationException {
        for (JobDependencyProperty property : jobDependencyProperties) {
            if (name.equals(property.getUpstreamJobName())) {
                return property;
            }
        }
        throw new FormValidationException(String.format("没有配置 name:%s 的上游信息.", name));
    }

    /**
     * 在Job中获取某一个Run,根据context的值
     *
     * @param job
     * @param context
     * @return
     */
    private Run getRunByContext(Job job, String context) {
        for (Run run : (RunList<Run>) job.getNewBuilds()) {
            StringParameterValue parameterValue = (StringParameterValue) getParameterValue(run, Constants.TOKEN);
            if (context.equals(parameterValue.getValue())) {
                return run;
            }
        }
        throw new JobDependencyRuntimeException(String.format("找不到Job对应的构建记录.Job Name:%s context:%s", job.getName(), context));
    }

    /**
     * 根据name获取所有Run中的对应的Run,主要针对含有condition这种情况,可能存在多个Run
     *
     * @param name
     * @return
     * @throws JobDependencyException
     */
    public List<Run> getRunsByName(String name) throws JobDependencyException, FormValidationException {
        List<Run> runs = new ArrayList<>();
        for (Run run : getAllUpstreamRuns()) {
            if (name.equals(run.getParent().getName())) {
                runs.add(run);
            }
        }
        if (runs.size() == 0) {
            throw new FormValidationException(String.format("在上游列表中找不到任务:%s的Run.", name));
        }
        return runs;
    }

    /**
     * 获取上游的jobname和buildid
     *
     * @param r
     * @return
     */
    private RunForUpstreamJobInfo getRunForUpstreamJobInfo(Run r) {
        String[] upstreamRunInfo = r.getAction(CauseAction.class).getCauses().get(0).getShortDescription().split(" ");
        String buildId = Utils.trimCommo(upstreamRunInfo[upstreamRunInfo.length - 1]);
        RunForUpstreamJobInfo jobInfo = new RunForUpstreamJobInfo(upstreamRunInfo[4].substring(1, upstreamRunInfo[4].length() - 1), buildId);
        return jobInfo;
    }

    public static class ParameterValueInfo {

        public static ParameterValue getParameterValue(Run run, String parameter) {
            ParametersAction action = run.getAction(ParametersAction.class);
            ParameterValue value = null;
            if (null != action)
                value = action.getParameter(parameter);
            return value;
        }
    }

}
