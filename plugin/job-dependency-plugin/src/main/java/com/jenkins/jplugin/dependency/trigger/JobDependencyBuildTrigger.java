package com.jenkins.jplugin.dependency.trigger;

import antlr.ANTLRException;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.jenkins.jplugin.dependency.constant.Constants;
import com.jenkins.jplugin.dependency.context.JobBuildHistory;
import com.jenkins.jplugin.dependency.context.RunContextCheck;
import com.jenkins.jplugin.dependency.context.TriggerConditionParser;
import com.jenkins.jplugin.dependency.context.UpstreamTriggerRun;
import com.jenkins.jplugin.dependency.enums.DateType;
import com.jenkins.jplugin.dependency.enums.ResultCondition;
import com.jenkins.jplugin.dependency.exception.FormValidationException;
import com.jenkins.jplugin.dependency.exception.JobDependencyException;
import com.jenkins.jplugin.dependency.exception.JobDependencyRuntimeException;
import com.jenkins.jplugin.dependency.pojo.*;
import com.jenkins.jplugin.dependency.utils.DateUtils;
import com.jenkins.jplugin.dependency.utils.Utils;
import com.jenkins.dependency.validation.FormatValidation;
import hudson.*;
import hudson.console.ModelHyperlinkNote;
import hudson.model.*;
import hudson.model.DependencyGraph.Dependency;
import hudson.model.Queue;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.RunListener;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.util.RunList;
import jenkins.model.DependencyDeclarer;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.security.QueueItemAuthenticatorConfiguration;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Similar to {# ReverseBuildTrigger} it triggers job on downstream, but checks for all the upstream jobs are build and are stable
 *
 * @author lonkar.yogeshr@gmail.com
 *         Mar 1, 2016
 */
@SuppressWarnings("rawtypes")
public final class JobDependencyBuildTrigger extends Trigger<Job> implements DependencyDeclarer {

    private static final Logger LOGGER = Logger.getLogger(JobDependencyBuildTrigger.class.getName());
    private static final Map<Job, Collection<JobDependencyBuildTrigger>> upstream2Trigger = new WeakHashMap<Job, Collection<JobDependencyBuildTrigger>>();

    private String upstreamProjects;

    private JobDependencyProperty[] jobProperties;
    private OnlyBuildOnceForDependencyJob onlyBuildOnce;
    private UpstreamJobParams jobParams;

    public JobDependencyBuildTrigger(OnlyBuildOnceForDependencyJob onlyBuildOnce,
                                     UpstreamJobParams jobParams,
                                     JobDependencyProperty... jobProperties) throws ANTLRException {
        this.jobProperties = jobProperties;
        this.jobParams = jobParams;
        this.onlyBuildOnce = onlyBuildOnce;
    }

    public JobDependencyBuildTrigger(OnlyBuildOnceForDependencyJob onlyBuildOnce,
                                     UpstreamJobParams jobParams,
                                     List<JobDependencyProperty> jobProperties) throws ANTLRException {
        this(onlyBuildOnce, jobParams,
                (JobDependencyProperty[]) jobProperties.toArray(new JobDependencyProperty[jobProperties.size()]));
    }

    public JobDependencyProperty[] getJobProperties() {
        return jobProperties;
    }

    public OnlyBuildOnceForDependencyJob getOnlyBuildOnce() {
        return onlyBuildOnce;
    }

    public UpstreamJobParams getJobParams() {
        return jobParams;
    }

    private boolean shouldTrigger(Run upstreamBuild, TaskListener listener) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return false;
        }
        // This checks Item.READ also on parent folders; note we are checking as
        // the upstream auth currently:
        boolean downstreamVisible = jenkins.getItemByFullName(job.getFullName()) == job;
        Authentication originalAuth = Jenkins.getAuthentication();
        Job upstream = upstreamBuild.getParent();
        Authentication auth = Tasks.getAuthenticationOf((Queue.Task) job);
        if (auth.equals(ACL.SYSTEM) && !QueueItemAuthenticatorConfiguration.get().getAuthenticators().isEmpty()) {
            auth = Jenkins.ANONYMOUS; // cf. BuildTrigger
        }
        SecurityContext orig = ACL.impersonate(auth);
        try {
            if (jenkins.getItemByFullName(upstream.getFullName()) != upstream) {
                if (downstreamVisible) {
                    listener.getLogger()
                            .println(
                                    "Running as "
                                            + (auth.getName() + " cannot even see " + upstream.getFullName()
                                            + " for trigger from " + job.getFullName()));
                } else {
                    LOGGER.log(Level.WARNING,
                            "Running as {0} cannot even see {1} for trigger from {2} (but cannot tell {3} that)",
                            new Object[]{auth.getName(), upstream, job, originalAuth.getName()});
                }
                return false;
            }
            // No need to check Item.BUILD on downstream, because the downstream
            // projectâ€™s configurer has asked for this.
        } finally {
            SecurityContextHolder.setContext(orig);
        }
        Result result = upstreamBuild.getResult();

        boolean resultStatus = result != null && jobDependencyPropertybyName(upstream.getName()).getThreshold().isMet(result);
        if (!resultStatus) {
            listener.getLogger().println(String.format("当前任务:%s构建状态:%s,无法触发下游.", upstreamBuild.getParent().getName(),
                    upstreamBuild.getResult().toString()));
        }

        return resultStatus;
    }

    /**
     * @author lonkar.yogeshr@gmail.com
     *         Mar 1, 2016
     */
    public static class FanInDependency extends Dependency {

        private String description;

        public FanInDependency(AbstractProject upstream, AbstractProject downstream, String description) {
            super(upstream, downstream);
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;

            final Dependency that = (Dependency) obj;
            return getUpstreamProject() == that.getUpstreamProject()
                    || this.getDownstreamProject() == that.getDownstreamProject();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + this.getUpstreamProject().hashCode();
            hash = 23 * hash + this.getDownstreamProject().hashCode();
            return hash;
        }

        @Override
        public String toString() {
            return super.toString() + "[" + getUpstreamProject() + "->" + getDownstreamProject() + "]";
        }

    }

    /**
     * 分别对每次上游的最新构建与上游的所有任务的所有的构建(注意是下游的上游,包括当前任务,此处比较难理解)进行比较
     *
     * @param upstreamBuild
     * @param listener
     */
    private boolean UpstreamIsBuildByToken(AbstractBuild upstreamBuild,
                                           AbstractProject downstream,
                                           TaskListener listener,
                                           ArrayList<Job> upsteamProjects) {

        ParameterValue context = getParameterValue(upstreamBuild, Constants.TOKEN);
        if (context == null) {
            return false;
        }
        for (Job upstream : upsteamProjects) {

            /**
             * 如果不存在trigger condition参数,直接按照time_hour的最新的build进行即可
             */
            if (StringUtils.isEmpty(jobDependencyPropertybyName(upstream.getName()).getTriggerCondition())) {

                RunList<Run> runList = upstream.getNewBuilds();
                int i = 0;

                for (Run run : runList) {
                    if (context.equals(getParameterValue(run, Constants.TOKEN))) {
                        if (null != run.getResult()
                                && jobDependencyPropertybyName(upstream.getName()).getThreshold().isMet(run.getResult())) {
                            LOGGER.info(String.format("任务:%s time_hour:%s 最新的构建是成功的.", upstream.getName(),
                                    context.getValue()));
                            break;
                        } else {
                            LOGGER.warning(String.format("任务:%s time_hour:%s 最新的构建是失败的,无法触发下游任务.",
                                    upstream.getName(),
                                    context.getValue()));
                            return false;
                        }
                    } else {
                        i++;
                    }
                }
                //如果没有发现符合条件的构建返回false
                if (i == runList.size()) {
                    return false;
                }
            }
            /**
             * 需要进行trigger condition的判断
             */
            else {
                try {
                    JobDependencyProperty jobProperty = jobDependencyPropertybyName(upstream.getName());
                    TriggerConditionInfo conditionInfo = new TriggerConditionParser(jobProperty, listener).parse();
                    JobBuildHistory jobBuildHistory = new JobBuildHistory(upstream, downstream, upstreamBuild, conditionInfo, jobDependencyPropertybyName(upstream.getName()), context, listener);
                    boolean checkStatus = jobBuildHistory.check();
                    jobBuildHistory = null;
                    if (checkStatus) {
                        LOGGER.info(String.format("上游任务:%s context:%s condition:%s最新的构建是成功的,可以尝试触发下游任务:%s.", upstream.getName(),
                                context.getValue(), jobProperty.getTriggerCondition(), downstream.getName()));
                        continue;
                    } else {
                        LOGGER.warning(String.format("上游任务:%s context:%s condition:%s最新的构建状态不是成功状态,无法触发下游任务:%s", upstream.getName(),
                                context.getValue(), jobProperty.getTriggerCondition(), downstream.getName()));
                        listener.getLogger().println(String.format("上游任务:%s context:%s condition:%s最新的构建状态不是成功状态,无法触发下游任务:%s", upstream.getName(),
                                context.getValue(), jobProperty.getTriggerCondition(), downstream.getName()));
                        return false;
                    }
                } catch (JobDependencyException e) {
                    listener.getLogger().print(e.getMessage());
                    //.TODO 需要验证.
                    ((StreamBuildListener) listener).finished(Result.ABORTED);
                    throw new JobDependencyRuntimeException(e.getCause());
                }
            }
        }
        return true;
    }

    /**
     * 周期内触发,根据是否人为操作来确定怎么个周期内执行法
     *
     * @param downstream
     * @param upstreamBuild
     * @param listener
     */
    private boolean triggerInWhile(AbstractProject downstream, AbstractBuild upstreamBuild, TaskListener listener) {

        //不需要做是否只在一段时间内做触发一次检查,直接返回true
        if (!onlyBuildOnce.isOnlyBuildOnce()) {
            return true;
        }

        //在用户触发情况下,周期内依然支持多次执行
        if (onlyBuildOnce.isTriggerByUser()) {
            return userTriggerJob(downstream, upstreamBuild, listener);
        } else {
            return onlyBuildOnce(downstream, upstreamBuild, listener);
        }
    }

    /**
     * 人为操作,可以允许在周期内多次执行
     *
     * @param downstream
     * @param upstreamBuild
     * @param listener
     */
    private boolean userTriggerJob(AbstractProject downstream, AbstractBuild upstreamBuild, TaskListener listener) {

        if (!triggerByUser(upstreamBuild)) {
            return onlyBuildOnce(downstream, upstreamBuild, listener);
        } else {
            return true;
        }
    }

    private boolean triggerByUser(AbstractBuild upstreamBuild) {
        List<Action> actions = upstreamBuild.getActions();
        for (Action action : actions) {
            if (action instanceof CauseAction) {
                CauseAction ca = (CauseAction) action;
                for (Cause cause : ca.getCauses()) {
                    if (cause instanceof Cause.UserIdCause) {
                        return true;
                    } else if (cause instanceof Cause.UpstreamCause) {
                        AbstractBuild build = (AbstractBuild) ((Cause.UpstreamCause) cause).getUpstreamRun();
                        return triggerByUser(build);
                    } else {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private boolean onlyBuildOnce(AbstractProject downstream, AbstractBuild upstreamBuild, TaskListener listener) {

        RunList<Run> runList = downstream.getNewBuilds();
        ParameterValue context = getParameterValue(upstreamBuild, Constants.TOKEN);
        if (context == null) {
            return false;
        }
        try {
            switch (onlyBuildOnce.getDateType()) {
                case HOUR:
                    for (Run run : runList) {
                        ParameterValue value = getParameterValue(run, Constants.TOKEN);
                        if (value != null
                                && value.getValue().equals(context.getValue())
                                && (Result.SUCCESS == run.getResult() ||
                                run.isBuilding())) {
                            listener.getLogger().println(String.format("无法触发下游,因为下游任务:%s只能在:%s周期内触发一次", downstream.getName(), DateType.HOUR.name()));
                            return false;
                        }
                    }
                    return true;
                case DAY:
                    for (Run run : runList) {
                        ParameterValue value = getParameterValue(run, Constants.TOKEN);
                        if (value != null
                                && value.getValue().toString().contains(DateUtils.getStringOfDay(DateUtils.parseDate((String) context.getValue())))
                                && (Result.SUCCESS == run.getResult() ||
                                run.isBuilding())) {
                            listener.getLogger().println(String.format("无法触发下游,因为下游任务:%s只能在:%s周期内触发一次", downstream.getName(), DateType.DAY.name()));
                            return false;
                        }
                    }
                    return true;
                case WEEK:
                    for (Run run : runList) {
                        ParameterValue value = getParameterValue(run, Constants.TOKEN);
                        if (value != null
                                && DateUtils.getStringOfWeek(DateUtils.parseDate((String) value.getValue())).
                                equals(DateUtils.getStringOfWeek(DateUtils.parseDate((String) context.getValue())))
                                && (Result.SUCCESS == run.getResult() ||
                                run.isBuilding())) {
                            listener.getLogger().println(String.format("无法触发下游,因为下游任务:%s只能在:%s周期内触发一次", downstream.getName(), DateType.WEEK.name()));
                            return false;
                        }
                    }
                    return true;
                case MONTH:
                    for (Run run : runList) {
                        ParameterValue value = getParameterValue(run, Constants.TOKEN);
                        if (value != null
                                && value.getValue().toString().contains(DateUtils.getStringOfMonth(DateUtils.parseDate((String) context.getValue())))
                                && (Result.SUCCESS == run.getResult() ||
                                run.isBuilding())) {
                            listener.getLogger().println(String.format("无法触发下游,因为下游任务:%s只能在:%s周期内触发一次", downstream.getName(), DateType.MONTH.name()));
                            return false;
                        }
                    }
                    return true;
                default:
                    LOGGER.info(String.format("没有匹配上相应的时间类型"));
                    return true;
            }
        } catch (JobDependencyException e) {
            listener.getLogger().print(e.getMessage());
            return false;
        }
    }

    private ParameterValue getParameterValue(Run run, String parameter) {
        ParametersAction action = run.getAction(ParametersAction.class);
        ParameterValue value = null;
        if (null != action)
            value = action.getParameter(parameter);

        return value;
    }

    @Override
    public void buildDependencyGraph(final AbstractProject downstream, DependencyGraph graph) {

        /**
         *  upstreamBuild   当前执行的作业
         *  downstream    当前执行的作业的下游作业(可能有多个下有作业)
         *
         */
        for (AbstractProject upstream : Items.fromNameList(downstream.getParent(), upstreamProjects, AbstractProject.class)) {

            graph.addDependency(new FanInDependency(upstream, downstream, "") {
                @Override
                public boolean shouldTriggerBuild(AbstractBuild upstreamBuild, TaskListener listener, List<Action> actions) {
                    //解决上游同时执行完,触发多次下游的问题,加一下同步锁
                    synchronized (downstream.getName()) {
                        //判断是否单次触发,如果是则不触发下游
                        ParameterizedJobMixIn.SingleBuildInvisibleAction singleBuildAction = upstreamBuild.getAction(ParameterizedJobMixIn.SingleBuildInvisibleAction.class);
                        if (null != singleBuildAction) {
                            listener.getLogger().print("单次触发,不触发下游");
                            return false;
                        }

                        ArrayList<Job> upsteamProjects = new ArrayList<Job>();

                        for (Job upstream : Items.fromNameList(downstream.getParent(), upstreamProjects, Job.class)) {
                            upsteamProjects.add(upstream);
                        }

                        /**
                         * 修复在任务尝试触发下游任务时,因为触发条件报错而最终状态为成功,导致无法排查问题的情况
                         * 处理为:
                         * 只要触发条件存在异常,则任务标记为失败,这样可以方便后续的处理
                         */
                        try {
                            // 解决上游同时执行完,触发多次下游的问题
                            Thread.sleep(1000);

                            return shouldTrigger(upstreamBuild, listener)
                                    && triggerInWhile(downstream, upstreamBuild, listener)
                                    && UpstreamIsBuildByToken(upstreamBuild, downstream, listener, upsteamProjects);
                        } catch (Exception e) {
                            listener.getLogger().println(String.format("触发下游任务:[%s]失败,原因:%s",
                                    downstream.getName(),
                                    e.getMessage()));
                            e.printStackTrace();

                            //发送通知
                            sendNotice(upstreamBuild, downstream);
//                        upstreamBuild.setWorseResult(Result.FAILURE);
                            return false;
                        }
                    }
                }
            });
        }
    }

    @Override
    public void start(Job project, boolean newInstance) {
        super.start(project, newInstance);
        SecurityContext orig = ACL.impersonate(ACL.SYSTEM);

        try {
            for (Job upstream : Items.fromNameList(project.getParent(), upstreamProjects, Job.class)) {
                if (upstream instanceof AbstractProject && project instanceof AbstractProject) {
                    continue; // handled specially
                }
                synchronized (upstream2Trigger) {
                    Collection<JobDependencyBuildTrigger> triggers = upstream2Trigger.get(upstream);
                    if (triggers == null) {
                        triggers = new LinkedList<JobDependencyBuildTrigger>();
                        upstream2Trigger.put(upstream, triggers);
                    }
                    triggers.remove(this);
                    triggers.add(this);
                }
            }
        } finally {
            SecurityContextHolder.setContext(orig);
        }
    }

    @Override
    public void stop() {
        super.stop();
        synchronized (upstream2Trigger) {
            for (Collection<JobDependencyBuildTrigger> triggers : upstream2Trigger.values()) {
                triggers.remove(this);
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends TriggerDescriptor {

        @Override
        public String getDisplayName() {
            return "Job-Dependency";
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job && item instanceof ParameterizedJobMixIn.ParameterizedJob;
        }

        public AutoCompletionCandidates doAutoCompleteUpstreamJobs(@QueryParameter String value,
                                                                   @AncestorInPath Item self, @AncestorInPath ItemGroup container) {
            return AutoCompletionCandidates.ofJobNames(Job.class, value, self, container);
        }

        public FormValidation doCheckUpstreamJobParams(@AncestorInPath Job project, @QueryParameter String value) {

            if (Utils.isEmpty(value) || !value.contains(Constants.TOKEN)) {
                return FormValidation.error(String.format("参数不能为空,且必须存在time_hour参数!"));
            }

            Matcher matcher = Constants.upstreamJobParamFormatPattern.matcher(value);
            if (!matcher.matches()) {
                return FormValidation.error(String.format("参数格式错误.当前的参数为:%s.支持的格式:A 或者 A,B 或者 A.param,B 或者 A.date.param,B", value));
            }

            List<AbstractProject> upstreamLists = Jenkins.getInstance().getDependencyGraph().getUpstream((AbstractProject) project);
            if (Utils.isEmpty(upstreamLists)) {
                try {
                    FormValidation validation = FormatValidation.vaildJobAndParamExistedInJenkins(value);
                    if (validation.kind != FormValidation.Kind.OK) {
                        return validation;
                    }
                } catch (FormValidationException e) {
                    return FormValidation.error(e.getMessage());
                }
            } else {
                for (String param : value.split(",")) {
                    String[] jobNameParam = param.split("\\.");
                    if (jobNameParam.length == 2
                            || jobNameParam.length == 3) {

                        JobParam jobParam = FormatValidation.generateJobParam(jobNameParam);

                        AbstractProject upstreamProject = FormatValidation.getProjectByName(upstreamLists, jobParam.getName());
                        if (null == upstreamProject) {
                            FormValidation validation = FormatValidation.jobIsExistedInJenkins(jobParam.getName());
                            if (validation.kind != FormValidation.Kind.OK) {
                                return validation;
                            } else {
                                try {
                                    upstreamProject = (AbstractProject) FormatValidation.getJobByName(jobParam.getName());
                                } catch (FormValidationException e) {
                                    return FormValidation.error(e.getMessage());
                                }
                            }
                        }

                        boolean paramValue = FormatValidation.getParameterByJob(upstreamProject, jobParam.getParam().trim());
                        if (!paramValue) {
                            return FormValidation.error(String.format("上游任务:%s没有这个参数:%s", upstreamProject.getName(),
                                    jobParam.getParam()));
                        }
                    } else {
                        boolean paramIsExistedInJos = FormatValidation.paramIsExistedInJobs(upstreamLists, param.trim());
                        if (!paramIsExistedInJos) {
                            return FormValidation.error(String.format("上游列表:%s中不存在参数:%s",
                                    Joiner.on(",").join(upstreamProjectNames(upstreamLists)), param));
                        }
                    }
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTriggerCondition(@QueryParameter String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.ok();
            }
            JobDependencyProperty jobDependencyProperty = new JobDependencyProperty("checkName", value, ResultCondition.SUCCESS);
            TriggerConditionParser conditionParser = new TriggerConditionParser(jobDependencyProperty);

            try {
                conditionParser.checkBeforeParse();
                conditionParser.checkAfterParse();
            } catch (JobDependencyException e) {
                return FormValidation.error(e.getMessage());
            } catch (FormValidationException e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok();
        }


        private List<String> upstreamProjectNames(List<AbstractProject> upstreamProjectNames) {
            List<String> names = new ArrayList<>();
            for (Job project : upstreamProjectNames) {
                names.add(project.getName());
            }
            return names;
        }

        public JobDependencyBuildTrigger newInstance(StaplerRequest req)
                throws FormException {
            List<JobDependencyProperty> jobprops = req.bindParametersToList(
                    JobDependencyProperty.class, "job-dependency.jobproperty.");

            OnlyBuildOnceForDependencyJob onlyBuildOnce = req.bindParameters(OnlyBuildOnceForDependencyJob.class,
                    "job-dependency.onlyBuildOnce.");

            UpstreamJobParams upstreamJobParams = req.bindParameters(UpstreamJobParams.class, "job-dependency.upstreamJob.");

            JobDependencyBuildTrigger ft;
            try {
                ft = new JobDependencyBuildTrigger(onlyBuildOnce, upstreamJobParams, jobprops);
                ft.upstreamProjects = ft.upstreamProjectLists();
                return ft;
            } catch (ANTLRException e) {
                e.printStackTrace();
            }
            throw new JobDependencyRuntimeException("Initialise job dependency instance error.");
        }

    }

    /**
     * 虽然各任务的参数个数不同,但是默认各参数的值都是一致的(从上游传下来的)
     */
    @Extension
    public static final class RunListenerImpl extends RunListener<Run> {

        @Override
        public void onStarted(Run r, TaskListener listener) {

            List<Job> upstreamPros = new ArrayList<>();
            JobDependencyBuildTrigger t = ParameterizedJobMixIn.getTrigger(r.getParent(), JobDependencyBuildTrigger.class);

            ParametersAction triggeredJobAction = r.getAction(ParametersAction.class);
            //此处必须是Map,不能是List,因为有默认值,否则后续的值无法进行覆盖,只能追加
            Map<String, ParameterValue> values = new HashMap<String, ParameterValue>();

            if (null != t) {

                //修复重启后 upsteamProjects 为空导致time_hour无法传下去.
                for (Job upstream : Items.fromNameList(r.getParent().getParent(), t.upstreamProjects, Job.class))
                    upstreamPros.add(upstream);

                try {
                    //添加默认值
                    for (ParameterValue _value : triggeredJobAction.getParameters()) {
                        if (null != _value.getValue()
                                && _value.getValue() instanceof String) {
                            String param = ((StringParameterValue) _value).getName();
                            values.put(param, _value);
                        }
                    }
                    UpstreamTriggerRun upstreamTriggerRun = new UpstreamTriggerRun(r, t.jobProperties, upstreamPros);
                    t.appendUpstreamRunParamaterValueToCurrentRun(upstreamTriggerRun, values);
                } catch (ArrayIndexOutOfBoundsException e) {
                    //skip ...说明是直接点击的带有上游的任务
                    LOGGER.info(String.format("当前任务:%s为手动触发,不需要获取上游信息", r.getParent().getName()));
                } catch (JobDependencyException e) {
                    LOGGER.log(Level.WARNING, e.getMessage());
                } catch (FormValidationException e) {
                    //1.参数中的上游name和上游name对不上,即找不到对应的Run
                    LOGGER.log(Level.WARNING, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                r.replaceAction(new ParametersAction(values.values().toArray(new ParameterValue[values.size()])));
            }
            //判断time_hour参数是否符合条件
            DescribableList buildWrappers = ((Project) r.getParent()).getBuildWrappersList();
            if (null == buildWrappers.get(StringBuildWrapper.class)) {
                buildWrappers.add(new StringBuildWrapper());
            }
        }

        @Override
        public void onCompleted(Run r, TaskListener listener) {
            Collection<JobDependencyBuildTrigger> triggers;
            synchronized (this) {
                Collection<JobDependencyBuildTrigger> _triggers = upstream2Trigger.get(r.getParent());
                if (_triggers == null || _triggers.isEmpty()) {
                    return;
                }
                triggers = new ArrayList<JobDependencyBuildTrigger>(_triggers);
            }
            for (final JobDependencyBuildTrigger trigger : triggers) {
                if (trigger.shouldTrigger(r, listener)) {
                    if (!trigger.job.isBuildable()) {
                        listener.getLogger().println(
                                hudson.tasks.Messages.BuildTrigger_Disabled(ModelHyperlinkNote.encodeTo(trigger.job)));
                        continue;
                    }
                    String name = ModelHyperlinkNote.encodeTo(trigger.job) + " #" + trigger.job.getNextBuildNumber();
                    if (ParameterizedJobMixIn.scheduleBuild2(trigger.job, -1, new CauseAction(new Cause.UpstreamCause(r))) != null) {
                        listener.getLogger().println(hudson.tasks.Messages.BuildTrigger_Triggering(name));
                    } else {
                        listener.getLogger().println(hudson.tasks.Messages.BuildTrigger_InQueue(name));
                    }
                }
            }
        }
    }

    public static class StringBuildWrapper extends BuildWrapper {

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
            new RunContextCheck(build).checkJobDependencyContext();
            return new Environment() {
            };
        }

        @Extension
        public static class StringBuildWrapperDescriptor extends BuildWrapperDescriptor {

            /**
             * 可以控制是否在页面显示
             */
            @Override
            public boolean isApplicable(AbstractProject<?, ?> ap) {
                return false;
            }
        }
    }

    @Extension
    public static class ItemListenerImpl extends ItemListener {
        @Override
        public void onLocationChanged(Item item, String oldFullName, String newFullName) {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                return;
            }
            for (Job<?, ?> p : jenkins.getAllItems(Job.class)) {
                JobDependencyBuildTrigger t = ParameterizedJobMixIn.getTrigger(p, JobDependencyBuildTrigger.class);
                if (t != null) {
                    Map<String, String> oldNewParamMap = null;
                    String revised = Items.computeRelativeNamesAfterRenaming(oldFullName, newFullName, t.upstreamProjects,
                            p.getParent());
                    if (!revised.equals(t.upstreamProjects)) {
                        try {
                            oldNewParamMap = Utils.getReviewdMap(t.upstreamProjects.split(Constants.MULTI_JOB_PARAM_JOINER),
                                    revised.split(Constants.MULTI_JOB_PARAM_JOINER));
                            String reviewdValue = t.jobParams.revisedJobParams(oldNewParamMap);

                            //修改上游列表
                            t.upstreamProjects = revised;

                            //修改上游参数列表
                            t.jobParams.setUpstreamJobParams(reviewdValue);

                            //修改 JobDependencyProperty 对象
                            for (Map.Entry<String, String> entry : oldNewParamMap.entrySet()) {
                                for (JobDependencyProperty property : t.jobProperties) {
                                    if (entry.getKey().equals(property.getUpstreamJobName())) {
                                        property.setUpstreamJobName(entry.getValue());
                                        break;
                                    }
                                }
                            }
                            p.save();
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Failed to persist project setting during rename from " + oldFullName
                                    + " to " + newFullName, e);
                        } catch (JobDependencyException e) {
                            LOGGER.log(Level.WARNING, e.getMessage(), e);
                        }
                    }
                }
            }
        }

        @Override
        public void onDeleted(Item item) {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                return;
            }
            for (Job<?, ?> p : jenkins.getAllItems(Job.class)) {

                JobDependencyBuildTrigger t = ParameterizedJobMixIn.getTrigger(p, JobDependencyBuildTrigger.class);
                if (t != null) {

                    ArrayList<String> upstreamJobNames = new ArrayList<>();
                    upstreamJobNames.addAll(Arrays.asList(t.upstreamProjects.split(Constants.MULTI_JOB_PARAM_JOINER)));
                    if (upstreamJobNames.contains(item.getName())) {
                        try {

                            if (upstreamJobNames.size() == 1) {
                                TriggerDescriptor triggerDescriptor = (TriggerDescriptor) jenkins.getDescriptorOrDie(JobDependencyBuildTrigger.class);
                                ((Project) p).removeTrigger(triggerDescriptor);
                                p.save();
                                LOGGER.info(String.format("Job:[%s] jobdependencytrigger only contain one job,and delete the trigger.", p.getName()));
                                return;
                            }

                            //修改参数
                            t.jobParams.setUpstreamJobParams(t.jobParams.deleteMatchJobParam(item.getName()));

                            //修改上游参数列表
                            upstreamJobNames.remove(item.getName());
                            t.upstreamProjects = Joiner.on(Constants.MULTI_JOB_PARAM_JOINER).join(upstreamJobNames);

                            //修改 JobDependencyProperty 对象
                            JobDependencyProperty deletedJobDependencyProperty = null;
                            for (JobDependencyProperty property : t.jobProperties) {
                                if (item.getName().equals(property.getUpstreamJobName())) {
                                    deletedJobDependencyProperty = property;
                                    break;
                                }
                            }
                            List<JobDependencyProperty> jobDependencyProperties = new ArrayList<>();
                            jobDependencyProperties.addAll(Arrays.asList(t.jobProperties));
                            jobDependencyProperties.remove(deletedJobDependencyProperty);
                            t.jobProperties = jobDependencyProperties.toArray(new JobDependencyProperty[0]);
                            LOGGER.info(String.format("Job:[%s] jobdependencytrigger delete job:[%s]'s dependency relationship.", p.getName(), item.getName()));
                            p.save();
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Failed to persist project setting during delete job: " + item.getName(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取上游列表,以","分隔
     */
    private String upstreamProjectLists() {
        List<String> projectNames = new ArrayList<>();
        for (JobDependencyProperty jobProperty : jobProperties)
            projectNames.add(jobProperty.getUpstreamJobName());
        return Joiner.on(",").join(projectNames);
    }

    /**
     * 根据name获取JobDependencyProperty对象
     *
     * @param name
     */
    private JobDependencyProperty jobDependencyPropertybyName(String name) {
        Preconditions.checkArgument(!StringUtils.isEmpty(name), "Upstream name can't empty.");
        for (JobDependencyProperty jobProperty : jobProperties) {
            if (name.equals(jobProperty.getUpstreamJobName().trim())) {
                return jobProperty;
            }
        }
        throw new JobDependencyRuntimeException(String.format("无法获取上游任务:[%s]", name));
    }

    /**
     * 获取上游构建任务的参数,用于后面的替换
     *
     * @param upstreamTriggerRun
     * @param values
     * @throws JobDependencyException
     */
    private void appendUpstreamRunParamaterValueToCurrentRun(UpstreamTriggerRun upstreamTriggerRun,
                                                             Map<String, ParameterValue> values)
            throws JobDependencyException, FormValidationException {
        for (String param : jobParams.getUpstreamJobParams().split(",")) {

            if (StringUtils.isEmpty(param)
                    || StringUtils.isBlank(param)) {
                LOGGER.info(String.format("参数为空,跳过....%s", param));
                continue;
            }

            param = param.trim();

            if (param.contains(".")) {
                String[] paramAboutJobName = param.split("\\.");
                if (paramAboutJobName.length == 2) {
                    String name = paramAboutJobName[0].trim();
                    String _param = paramAboutJobName[1].trim();

                    Run run = upstreamTriggerRun.getRunsByName(name).get(0);
                    ParameterValue value = UpstreamTriggerRun.ParameterValueInfo.getParameterValue(run, _param);
                    if (null != value)
                        values.put(_param, value);
                } else if (paramAboutJobName.length == 3) {
                    String name = paramAboutJobName[0].trim();
                    String dateType = paramAboutJobName[1].trim();
                    String _param = paramAboutJobName[2].trim();

                    Run run = upstreamTriggerRun.getUpstreamRunByNameAndDateType(name, dateType);
                    ParameterValue value = UpstreamTriggerRun.ParameterValueInfo.getParameterValue(run, _param);
                    if (null != value)
                        values.put(_param, value);
                } else {
                    throw new JobDependencyRuntimeException(String.format("格式不符合规范,请检查.支持的格式:A.B | A.B.C"));
                }
            } else {
                ParameterValue value = upstreamTriggerRun.getUpstreamParamValueByParam(param.trim());
                values.put(param, value);
            }
        }
    }

    /**
     * 对触发失败的任务增加发送通知的功能
     *
     * @param upstreamBuild
     * @param downstreamProject
     */
    private void sendNotice(AbstractBuild upstreamBuild, AbstractProject downstreamProject) {
/*

        FalconServiceImpl falconService = new FalconServiceImpl();

        String downstreamProjectOwner = downstreamProject.getOwnerName();
        String upstreamProjectOwner = upstreamBuild.getProject().getOwnerName();

        //值班人、任务owner
        Set<String> dutyUsers = falconService.getMainDutyUser();
        dutyUsers.add(downstreamProjectOwner);
        dutyUsers.add(upstreamProjectOwner);
        String ownerAndDutyUser = Joiner.on(",").join(dutyUsers);

        //值班人、任务owner、admin
        Set<String> adminDutyUsers = falconService.getAdminDutyUser();
        adminDutyUsers.addAll(dutyUsers);
        String ownerAndDutyUserAndAdmin = Joiner.on(",").join(adminDutyUsers);

        //获取context
        ParameterValue context = getParameterValue(upstreamBuild, Constants.TOKEN);
        String time_hour = "";
        if (context != null) {
            time_hour = (String) context.getValue();
        }

        Map<String, String> contentMap = new HashMap<>();
        String content = String.format("[job任务调度] [%s]触发[%s]失败! 负责人分别为:[%s,%s], Context[%s], 启动时间[%s], 请检查!",
                upstreamBuild.getProject().getName(), downstreamProject.getName(), upstreamProjectOwner, downstreamProjectOwner, time_hour, buildTime(upstreamBuild));


        User user = User.get(ownerAndDutyUser);
        contentMap.put("content", content);

        //发送短信
        contentMap.put("to", user.getPhone());
        new AlarmV2(ownerAndDutyUser, contentMap).sendSms();
        //发送钉钉
        contentMap.put("to", user.getEmplid());
        new AlarmV2(ownerAndDutyUser, contentMap).sendDingDing();

        //发送邮件
        contentMap.put("title", "[job任务调度] 任务失败");
        contentMap.put("to", ownerAndDutyUser + "@ziroom.com");
        new AlarmV2(ownerAndDutyUser, contentMap).sendEmail();

*/

    }

    private String buildTime(AbstractBuild<?, ?> build) {
        long startTime = build.getStartTimeInMillis();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义格式，不显示毫秒
        Timestamp timestamp = new Timestamp(startTime);
        return df.format(timestamp);
    }

}