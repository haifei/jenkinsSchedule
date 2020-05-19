/*
 *  The MIT License
 *
 *  Copyright 2010 Sony Ericsson Mobile Communications. All rights reservered.
 *  Copyright 2012 Sony Mobile Communications AB. All rights reservered.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.sonyericsson.rebuild;

import com.google.common.base.Joiner;
import com.jenkins.jplugin.dependency.trigger.JobDependencyBuildTrigger;
import hudson.Extension;
import hudson.model.*;

import javax.servlet.ServletException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hudson.matrix.MatrixRun;
import hudson.triggers.Trigger;
import hudson.util.RunList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rebuild RootAction implementation class. This class will basically reschedule
 * the build with existing parameters.
 *
 * @author Shemeer S;
 */
public class RebuildAction implements Action {

    private static final String SVN_TAG_PARAM_CLASS = "hudson.scm.listtagsparameter.ListSubversionTagsParameterValue";

    private static final Logger logger = LoggerFactory.getLogger(RebuildAction.class);

    /*
     * All the below transient variables are declared only for backward
     * compatibility of the rebuild plugin.
     */
    private transient String rebuildurl = "rebuild";
    private transient String parameters = "rebuildParam";
    private transient String p = "parameter";
    private transient Run<?, ?> build;
    private transient ParametersDefinitionProperty pdp;
    private static final String PARAMETERIZED_URL = "parameterized";

    private static final String CONTEXT = "time_hour";

    /**
     * Rebuild Descriptor.
     */
    @Extension
    public static final RebuildDescriptor DESCRIPTOR = new RebuildDescriptor();

    /**
     * RebuildAction constructor.
     */
    public RebuildAction() {
    }

    /**
     * Getter method for pdp.
     *
     * @return pdp.
     */
    public ParametersDefinitionProperty getPdp() {
        return pdp;
    }

    /**
     * Getter method for build.
     *
     * @return build.
     */
    public Run<?, ?> getBuild() {
        return build;
    }

    /**
     * Getter method for p.
     *
     * @return p.
     */
    public String getP() {
        return p;
    }

    /**
     * Getter method for parameters.
     *
     * @return parameters.
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * Getter method for rebuildurl.
     *
     * @return rebuildurl.
     */
    public String getRebuildurl() {
        return rebuildurl;
    }

    /**
     * True if the password fields should be pre-filled.
     *
     * @return True if the password fields should be pre-filled.
     */
    public boolean isRememberPasswordEnabled() {
        return DESCRIPTOR.getRebuildConfiguration().isRememberPasswordEnabled();
    }

    /**
     * Method will return current project.
     *
     * @return currentProject.
     */
    public Job getProject() {
        if (build != null) {
            return build.getParent();
        }

        Job currentProject = null;
        StaplerRequest request = Stapler.getCurrentRequest();
        if (request != null) {
            currentProject = request.findAncestorObject(Job.class);
        }

        return currentProject;
    }

    @Override
    public String getIconFileName() {
        if (isRebuildAvailable()) {
            return "images/24x24/rebuild.png";
        } else {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        if (isRebuildAvailable()) {
            return "重新构建";
        } else {
            return null;
        }
    }

    @Override
    public String getUrlName() {
        if (isRebuildAvailable()) {
            return "rebuild";
        } else {
            return null;
        }
    }

    /**
     * Handles the rebuild request and redirects to parameterized
     * and non parameterized build when needed.
     *
     * @param request  StaplerRequest the request.
     * @param response StaplerResponse the response handler.
     * @throws IOException          in case of Stapler issues
     * @throws ServletException     if something unfortunate happens.
     * @throws InterruptedException if something unfortunate happens.
     */
    public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException, ServletException, InterruptedException {

        Run currentBuild = request.findAncestorObject(Run.class);

        if (currentBuild != null) {
            ParametersAction paramAction = currentBuild.getAction(ParametersAction.class);
            if (paramAction != null) {
                RebuildSettings settings = (RebuildSettings) getProject().getProperty(RebuildSettings.class);
                if (settings != null && settings.getAutoRebuild()) {
                    parameterizedRebuild(currentBuild, response);
                } else {
                    response.sendRedirect(PARAMETERIZED_URL);
                }
            } else {
                nonParameterizedRebuild(currentBuild, response);
            }
        }
    }

    private String joinDownstreamProjects(Set<AbstractProject> allDownstreamProjects) {
        List<String> projectNames = new ArrayList<String>();
        for (AbstractProject project : allDownstreamProjects) {
            projectNames.add(project.getName());
        }
        return Joiner.on(",").join(projectNames);
    }

    /**
     * 获取当前构建的context
     *
     * @param currentBuild
     * @return
     */
    private String context(Run currentBuild) {
        if (null != currentBuild) {
            ParametersAction action = currentBuild.getAction(ParametersAction.class);
            if (null != action) {
                StringParameterValue parameter = (StringParameterValue) action.getParameter(CONTEXT);
                if (null != parameter) {
                    return parameter.getValue().toString();
                } else {
                    logger.error(String.format("Current project:[%s] build error,because of no context.",
                            currentBuild.getParent().getName()));
                }
            }
        }
        return null;
    }

    /**
     * 获取所有下游的列表
     *
     * @param job
     * @param projects
     * @return
     */
    private void downstreamProjects(Job job, Set<AbstractProject> projects) {
        List<AbstractProject> downstreamProjects = ((AbstractProject) job).getDownstreamProjects();
        if (downstreamProjects != null
                && downstreamProjects.size() != 0) {

            for (AbstractProject project : downstreamProjects){
                Trigger trigger = project.getTrigger(JobDependencyBuildTrigger.class);
                if (trigger != null &&
                        trigger instanceof JobDependencyBuildTrigger) {
                    JobDependencyBuildTrigger jobDependencyBuildTrigger = (JobDependencyBuildTrigger) trigger;
                    boolean onlyOnceBuild = jobDependencyBuildTrigger.getOnlyBuildOnce().isOnlyBuildOnce();
                    if (!onlyOnceBuild) {
                        projects.add(project);
                        downstreamProjects(project, projects);
                    }
                }
            }
        }
    }

    /**
     * 重置context的project的状态为 Result.ABORT
     *
     * @param project
     * @param expectContext
     */
    private void setBuildStatus(AbstractProject project, String expectContext) {

        //获取任务的trigger，并且在置灰时，判断任务是否是周期内执行一次，如果只能一次，则不置灰
        /*Trigger trigger = project.getTrigger(JobDependencyBuildTrigger.class);

        if (trigger != null &&
                trigger instanceof JobDependencyBuildTrigger) {
            JobDependencyBuildTrigger jobDependencyBuildTrigger = (JobDependencyBuildTrigger) trigger;
            boolean onlyOnceBuild = jobDependencyBuildTrigger.getOnlyBuildOnce().isOnlyBuildOnce();
            if (onlyOnceBuild) {
                logger.info(String.format("Current project:[%s] context:[%s] onlyOnceBuild:[%s] ,so no reset gray,and skip.",
                        project.getName(), expectContext, onlyOnceBuild));
                return;
            }
        }*/

        RunList<Run> runlist = project.getNewBuilds();
        if (runlist != null
                && runlist.size() != 0) {
            for (Run run : runlist) {
                ParametersAction action = run.getAction(ParametersAction.class);
                if (null != action) {
                    ParameterValue parameter = action.getParameter(CONTEXT);
                    try {
                        if (null != parameter
                                && null != parameter.getValue()
                                && parameter.getValue() instanceof String
                                && expectContext.equals((String) parameter.getValue())) {
                            logger.warn(String.format("Current project:[%s] status:[%s] -> [%s],because of rerun project.",
                                    project.getName(), run.getResult().toString(), Result.ABORTED.toString()));
                            if (!run.isBuilding()) {
                                run.setWorseResult(Result.ABORTED);
                            }
                            break;
                        }
                    } catch (Exception e) {
                        logger.error(String.format("Reset project:[%s] error.", project.getName()), e);
                    }
                }
            }
        }
    }

    /**
     * Handles the rebuild request with parameter.
     *
     * @param currentBuild the build.
     * @param response     StaplerResponse the response handler.
     * @throws IOException in case of Stapler issues
     */
    public void parameterizedRebuild(Run currentBuild, StaplerResponse response) throws IOException {
        Job project = getProject();
        if (project == null) {
            return;
        }
        project.checkPermission(Item.BUILD);
        if (isRebuildAvailable()) {

            List<Action> actions = copyBuildCausesAndAddUserCause(currentBuild);
            ParametersAction action = currentBuild.getAction(ParametersAction.class);
            actions.add(action);

            Hudson.getInstance().getQueue().schedule((Queue.Task) build.getParent(), 0, actions);
            response.sendRedirect("../../");
        }
    }

    /**
     * Call this method while rebuilding
     * non parameterized build.     .
     *
     * @param currentBuild current build.
     * @param response     current response object.
     * @throws ServletException     if something unfortunate happens.
     * @throws IOException          if something unfortunate happens.
     * @throws InterruptedException if something unfortunate happens.
     */
    public void nonParameterizedRebuild(Run currentBuild, StaplerResponse
            response) throws ServletException, IOException, InterruptedException {
        getProject().checkPermission(Item.BUILD);

        List<Action> actions = constructRebuildCause(build, null);
        Hudson.getInstance().getQueue().schedule((Queue.Task) currentBuild.getParent(), 0, actions);
        response.sendRedirect("../../");
    }

    /**
     * Saves the form to the configuration and disk.
     *
     * @param req StaplerRequest
     * @param rsp StaplerResponse
     * @throws ServletException     if something unfortunate happens.
     * @throws IOException          if something unfortunate happens.
     * @throws InterruptedException if something unfortunate happens.
     */
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException, InterruptedException {
        Job project = getProject();
        if (project == null) {
            return;
        }
        project.checkPermission(Item.BUILD);
        if (isRebuildAvailable()) {
            if (!req.getMethod().equals("POST")) {
                // show the parameter entry form.
                req.getView(this, "index.jelly").forward(req, rsp);
                return;
            }
            build = req.findAncestorObject(Run.class);
            ParametersDefinitionProperty paramDefProp = build.getParent().getProperty(
                    ParametersDefinitionProperty.class);
            List<ParameterValue> values = new ArrayList<ParameterValue>();
            ParametersAction paramAction = build.getAction(ParametersAction.class);
            JSONObject formData = req.getSubmittedForm();
            if (!formData.isEmpty()) {
                JSONArray a = JSONArray.fromObject(formData.get("parameter"));
                String expectContext = "";
                for (Object o : a) {
                    JSONObject jo = (JSONObject) o;
                    String name = jo.getString("name");
                    ParameterValue parameterValue = getParameterValue(paramDefProp, name, paramAction, req, jo);

                    //是否存在context,取值添加到expectContext中
                    if (CONTEXT.equals(name)) {
                        if (parameterValue.getValue() instanceof String) {
                            expectContext = (String) parameterValue.getValue();
                        }
                    }

                    //始终添加到数据组中,用于触发任务
                    if (parameterValue != null) {
                        values.add(parameterValue);
                    }
                }

                //设置所有的下游任务状态为ABORT
                if (!"".equals(expectContext))
                    setAllDownStreamProjectAbortStatusAboutContext(build, expectContext);
            }

            List<Action> actions = constructRebuildCause(build, new ParametersAction(values));
            Hudson.getInstance().getQueue().schedule((Queue.Task) build.getParent(), 0, actions);

            rsp.sendRedirect("../../");
        }
    }

    /**
     * 对所有的下游在context的构建为成功的置为终止状态
     *
     * @param build
     * @param expectContext
     */
    private void setAllDownStreamProjectAbortStatusAboutContext(Run<?, ?> build, String expectContext) {
        //获取当前任务的所有下游任务
        Set<AbstractProject> allDownstreamProjects = new HashSet<AbstractProject>();
        downstreamProjects(getProject(), allDownstreamProjects);
        logger.info(String.format("Current project:[%s] Context:[%s] BuildId:[%s] all downstream project:[%s]",
                getProject().getName(),
                expectContext,
                build.getNumber() + 1,
                joinDownstreamProjects(allDownstreamProjects)));

        if (allDownstreamProjects != null
                && allDownstreamProjects.size() != 0) {

            for (AbstractProject project : allDownstreamProjects) {
                //搜索下游任务的最新的100次构建,如果存在当前context,则进行置灰操作
                setBuildStatus(project, expectContext);
            }
        }
    }

    /**
     * Extracts the build causes and adds or replaces the {@link hudson.model.Cause.UserIdCause}. The result is a
     * list of all build causes from the original build (might be an empty list), plus a
     * {@link hudson.model.Cause.UserIdCause} for the user who started the rebuild.
     *
     * @param fromBuild the build to copy the causes from.
     * @return list with all original causes and a {@link hudson.model.Cause.UserIdCause}.
     */
    private List<Action> copyBuildCausesAndAddUserCause(Run<?, ?> fromBuild) {
        List currentBuildCauses = fromBuild.getCauses();

        List<Action> actions = new ArrayList<Action>(currentBuildCauses.size());
        boolean hasUserCause = false;
        for (Object buildCause : currentBuildCauses) {
            if (buildCause instanceof Cause.UserIdCause) {
                hasUserCause = true;
                actions.add(new CauseAction(new Cause.UserIdCause()));
            }
            //注释掉此行代码解决输入参数与执行参数不一致的问题
            /*else {
                actions.add(new CauseAction((Cause) buildCause));
            }*/
        }
        if (!hasUserCause) {
            actions.add(new CauseAction(new Cause.UserIdCause()));
        }

        return actions;
    }

    /**
     * Method for checking whether current build is sub job(MatrixRun) of Matrix
     * build.
     *
     * @return boolean
     */
    public boolean isMatrixRun() {
        StaplerRequest request = Stapler.getCurrentRequest();
        if (request != null) {
            build = request.findAncestorObject(Run.class);
            if (build != null && build instanceof MatrixRun) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method for checking,whether the rebuild functionality would be available
     * for build.
     *
     * @return boolean
     */
    public boolean isRebuildAvailable() {
        Job project = getProject();
        return project != null
                && project.hasPermission(Item.BUILD)
                && project.isBuildable()
                && project instanceof Queue.Task
                && !isMatrixRun()
                && !isRebuildDisbaled();

    }

    private boolean isRebuildDisbaled() {
        RebuildSettings settings = (RebuildSettings) getProject().getProperty(RebuildSettings.class);

        if (settings != null && settings.getRebuildDisabled()) {
            return true;
        }
        return false;
    }

    /**
     * Method for getting the ParameterValue instance from ParameterDefinition
     * or ParamterAction.
     *
     * @param paramDefProp  ParametersDefinitionProperty
     * @param parameterName Name of the Parameter.
     * @param paramAction   ParametersAction
     * @param req           StaplerRequest
     * @param jo            JSONObject
     * @return ParameterValue instance of subclass of ParameterValue
     */
    public ParameterValue getParameterValue(ParametersDefinitionProperty paramDefProp,
                                            String parameterName, ParametersAction paramAction, StaplerRequest req, JSONObject jo) {
        ParameterDefinition paramDef;
        // this is normal case when user try to rebuild a parameterized job.
        if (paramDefProp != null) {
            paramDef = paramDefProp.getParameterDefinition(parameterName);
            if (paramDef != null) {
                // The copy artifact plugin throws an exception when using createValue(req, jo)
                // If the parameter comes from the copy artifact plugin, then use the single argument createValue
                if (jo.toString().contains("BuildSelector") || jo.toString().contains("WorkspaceSelector")) {
                    SimpleParameterDefinition parameterDefinition =
                            (SimpleParameterDefinition) paramDefProp.getParameterDefinition(parameterName);
                    return parameterDefinition.createValue(jo.getString("value"));
                }
                return paramDef.createValue(req, jo);
            }
        }
        /*
         * when user try to rebuild a build that was invoked by
         * parameterized trigger plugin in that case ParameterDefinition
         * is null for that parametername that is paased by parameterize
         * trigger plugin,so for handling that scenario, we need to
         * create an instance of that specific ParameterValue with
         * passed parameter value by form.
         *
         * In contrast to all other parameterActions, ListSubversionTagsParameterValue uses "tag" instead of "value"
         */
        if (jo.containsKey("value")) {
            return cloneParameter(paramAction.getParameter(parameterName), jo.getString("value"));
        } else {
            return cloneParameter(paramAction.getParameter(parameterName), jo.getString("tag"));
        }
    }

    /**
     * Method for replacing the old parametervalue with new parameter value
     *
     * @param oldValue ParameterValue
     * @param newValue The value that is submitted by user using form.
     * @return ParameterValue
     */
    private ParameterValue cloneParameter(ParameterValue oldValue, String newValue) {
        if (oldValue instanceof StringParameterValue) {
            return new StringParameterValue(oldValue.getName(), newValue, oldValue.getDescription());
        } else if (oldValue instanceof BooleanParameterValue) {
            return new BooleanParameterValue(oldValue.getName(), Boolean.valueOf(newValue),
                    oldValue.getDescription());
        } else if (oldValue instanceof RunParameterValue) {
            return new RunParameterValue(oldValue.getName(), newValue, oldValue.getDescription());
        } else if (oldValue instanceof PasswordParameterValue) {
            return new PasswordParameterValue(oldValue.getName(), newValue,
                    oldValue.getDescription());
        } else if (oldValue.getClass().getName().equals(SVN_TAG_PARAM_CLASS)) {
            /**
             * getClass().getName() to avoid dependency on svn plugin.
             */
            return new StringParameterValue(oldValue.getName(), newValue, oldValue.getDescription());
        }
        throw new IllegalArgumentException("Unrecognized parameter type: " + oldValue.getClass());
    }

    /**
     * Method for constructing Rebuild cause.
     *
     * @param up          AbsstractBuild
     * @param paramAction ParametersAction.
     * @return actions List<Action>
     */
    private List<Action> constructRebuildCause(Run up, ParametersAction paramAction) {
        List<Action> actions = copyBuildCausesAndAddUserCause(up);
        actions.add(new CauseAction(new RebuildCause(up)));
        if (paramAction != null) {
            actions.add(paramAction);
        }
        return actions;
    }

    /**
     * @param value the parameter value to show to rebuild.
     * @return page for the parameter value, or null if no suitable option found.
     */
    public RebuildParameterPage getRebuildParameterPage(ParameterValue value) {
        for (RebuildParameterProvider provider : RebuildParameterProvider.all()) {
            RebuildParameterPage page = provider.getRebuildPage(value);
            if (page != null) {
                return page;
            }
        }

        // Check if we have a branched Jelly in the plugin.
        if (getClass().getResource(String.format("/%s/%s.jelly", getClass().getCanonicalName().replace('.', '/'), value.getClass().getSimpleName())) != null) {
            // No provider available, use an existing view provided by rebuild plugin.
            return new RebuildParameterPage(
                    getClass(),
                    String.format("%s.jelly", value.getClass().getSimpleName())
            );

        }
        // Else we return that we haven't found anything.
        // So Jelly fallback could occur.
        return null;
    }
}
