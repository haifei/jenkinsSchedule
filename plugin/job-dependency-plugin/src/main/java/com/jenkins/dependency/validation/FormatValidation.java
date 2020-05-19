package com.jenkins.dependency.validation;

import com.jenkins.jplugin.dependency.exception.FormValidationException;
import com.jenkins.jplugin.dependency.exception.JobDependencyRuntimeException;
import com.jenkins.jplugin.dependency.pojo.JobParam;
import com.jenkins.jplugin.dependency.utils.Utils;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 2020-04-12 add by wanghf
 */
public class FormatValidation {

    private static final Logger logger = LoggerFactory.getLogger(FormatValidation.class);

    /**
     * 长度为2或者3
     *
     * @param strs
     * @return
     */
    public static JobParam generateJobParam(String[] strs) {
        String name = strs[0];
        String param = null;

        if (strs.length == 3) {
            param = strs[2];
        } else {
            param = strs[1];
        }
        return new JobParam(name, param);
    }

    /**
     * 判断任务是否存在
     *
     * @param jobName
     * @return
     */
    public static FormValidation jobIsExistedInJenkins(String jobName) {
        if (!Utils.hasValueForList(Jenkins.getInstance().getJobNames(), jobName)) {
            return FormValidation.error(String.format("任务:%s不存在.", jobName));
        }
        return FormValidation.ok();
    }

    /**
     * 在Job中查找参数是否存在
     *
     * @param job
     * @param parameter
     * @return
     */
    public static boolean getParameterByJob(Job job, String parameter) {
        ParametersDefinitionProperty paramProperty = (ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class);
        if (Utils.isEmpty(paramProperty)) {
            throw new JobDependencyRuntimeException(String.format("当前任务:%s不存在参数.", job.getName()));
        }
        for (String param : paramProperty.getParameterDefinitionNames()) {
            if (parameter.equals(param)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param projects
     * @param name
     * @return
     */
    public static AbstractProject getProjectByName(List<AbstractProject> projects, String name) {
        for (AbstractProject project : projects) {
            if (name.equals(project.getName())) {
                return project;
            }
        }
        return null;
    }

    /**
     * @param value
     * @return
     * @throws FormValidationException
     */
    public static FormValidation vaildJobAndParamExistedInJenkins(String value) throws FormValidationException {
        for (String param : value.split(",")) {
            String[] jobNameParam = param.split("\\.");
            if (jobNameParam.length == 2
                    || jobNameParam.length == 3) {

                JobParam jobParam = generateJobParam(jobNameParam);

                FormValidation validation = jobIsExistedInJenkins(jobParam.getName());
                if (validation.kind != FormValidation.Kind.OK) {
                    return validation;
                }

                boolean paramIsExisted = getParameterByJob(getJobByName(jobParam.getName()), jobParam.getParam());
                if (!paramIsExisted) {
                    return FormValidation.error(String.format("上游任务:%s没有这个参数:%s", jobParam.getName(), jobParam.getParam()));
                }
            }
        }
        return FormValidation.ok();
    }

    /**
     * 在Jenkin中获取对应的Job
     *
     * @param name
     * @return
     * @throws FormValidationException
     */
    public static Job getJobByName(String name) throws FormValidationException {
        List<Project> projects = Jenkins.getInstance().getProjects();
        for (Project project : projects) {
            if (name.equals(project.getName())) {
                return project;
            }
        }
        throw new FormValidationException(String.format("任务:%s不存在.", name));
    }

    public static boolean paramIsExistedInJobs(List<AbstractProject> jobs, String param) {

        for (Job job : jobs) {
            boolean paramValue = getParameterByJob(job, param.trim());
            if (paramValue) {
                return true;
            }
        }
        return false;
    }
}