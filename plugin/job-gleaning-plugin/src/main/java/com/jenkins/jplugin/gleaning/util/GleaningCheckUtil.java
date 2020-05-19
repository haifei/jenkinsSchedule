package com.jenkins.jplugin.gleaning.util;

/**
 * 2020-04-21  add by wanghf
 */

import antlr.ANTLRException;
import com.google.common.collect.Sets;
import com.jenkins.jplugin.gleaning.model.*;
import hudson.model.*;
import hudson.scheduler.CronTab;
import hudson.scheduler.Hash;
import hudson.triggers.TimerTrigger;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.CyclicGraphDetector;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public abstract class GleaningCheckUtil {
    private final static Logger LOGGER = Logger.getLogger(Logger.class.getName());
    public final static String CONTEXT_KEY = "time_hour";
    public final static String TIMETRIGGER_KEY = "Started by timer";
    public final static String UPTRIGGER_PREFIX = "Started by upstream project";
    public final static String CONTEXT_FORMAT = "yyyy/MM/dd/HH";
    public final static String HASH_SEED = "stuff";
    public final static int CRON_LIMIT = 1; // 只看昨天未执行的
    public final static int FAILURE_HOUR_LIMIT = 8;
    public final static int EXECUTOR_THREAD_LIMIT = 3;
    public final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public final static Set adminUser= Sets.newHashSet("admin","dp");

    private final static ExecutorService pool = Executors.newFixedThreadPool(3);


    public static void execTasks(List<BuildParameter> parameters) {
        if (parameters != null && parameters.size() > 0) {
            pool.execute(new BuildTask(parameters));
        }
    }

    public static void execTasks(BuildParameter parameter) {
        doBuild(parameter.getProject(), parameter.getContext(), parameter.getType());
    }

    public static String getContextFromRun(Run run) {
        try {
            if (run != null) {
                ParametersAction action = run.getAction(ParametersAction.class);
                if (action != null) {
                    ParameterValue contextVal = action.getParameter(CONTEXT_KEY);
                    if (contextVal != null) {
                        return contextVal.getValue().toString();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("======获取作业:[%s], 执行时间 time_hour 失败！", run.getParent().getName()));
        }

        return null;
    }

    public static Map<Long, String> getLossContext(String cron, Date endDate) {
        Map<Long, String> times = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat(CONTEXT_FORMAT);

        try {
            if (StringUtils.isBlank(cron)) {
                return null;
            }
            CronTab x = new CronTab(cron, Hash.from(HASH_SEED));
            Calendar c = setCurrentZeroTime();
            Date curr = x.ceil(c).getTime();  //当前开始执行日期

            int limitCount = 1;
            while (endDate.before(curr) && limitCount <= CRON_LIMIT && curr.before(new Date())) {
                Date t = new Date(curr.getTime());
                Calendar td = new GregorianCalendar();
                td.setTimeInMillis(t.getTime());
                td.set(Calendar.DAY_OF_MONTH, td.get(Calendar.DAY_OF_MONTH) - 1);
                //c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) + 1);
                curr = x.ceil(td).getTime();
                times.put(t.getTime(), sdf.format(curr.getTime()));
                limitCount++;
            }
        } catch (ANTLRException e) {
            LOGGER.info("===error:" + e.getMessage());
        }

        return times;
    }

    private static Calendar setCurrentZeroTime() {
        Calendar c = new GregorianCalendar();
        c.setTime(new Date());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        return c;
    }

    /**
     * Author wanghf
     * Description 获取 未执行的任务
     * 1. 定时未被处罚
     * 2. 依赖已正在执行， 但被重启冲掉的任务
     * Date 2019/1/23 上午11:18
     */
    public static List<CronTask> getMissCronTasks(Project project, Trigger trigger) {
            //TimerTrigger timerTrigger = (TimerTrigger) trigger;
            Run run = project.getLastBuild();
            if (run == null || project.isDisabled()) {
                return null;
            }
            CauseAction action = run.getAction(CauseAction.class);
            if (action != null) {
                String triggerInfo = action.getCauses().get(0).getShortDescription();
                if (!(triggerInfo.startsWith(UPTRIGGER_PREFIX) ||
                        triggerInfo.equals(TIMETRIGGER_KEY))) {
                    return null;
                }
            }

            List<CronTask> tasks = new ArrayList<>();
            String context = getContextFromRun(run);
            if (context != null && !context.isEmpty()) {
                //上一个任务, 时间应该是昨天
                Calendar cal = new GregorianCalendar();
                cal.setTimeInMillis(run.getStartTimeInMillis());
                //取昨天未执行的任务
                Map<Long, String> missContextMap=null;
                if(trigger instanceof  TimerTrigger){
                    missContextMap = getLossContext(trigger.getSpec(), cal.getTime());
                }else if((run.getNumber()+1)<project.getNextBuildNumber()){
                    missContextMap= getLossContext("01 0 * * *", cal.getTime());
                }

                if (missContextMap == null || missContextMap.size() <= 0) {
                    return null;
                }
                for (Map.Entry<Long, String> entry : missContextMap.entrySet()) {
                    CronTask task = new CronTask();
                    task.setName(project.getName());
                    task.setOwner(project.getOwnerName());
                    task.setCalBuildTime(sdf.format(new Date(entry.getKey())));
                    task.setCalContext(entry.getValue());
                    task.setLastBuildTime(sdf.format(new Date(run.getStartTimeInMillis())));
                    task.setLastContext(context);
                    task.setIsChecked("false");
                    tasks.add(task);
                }
                return tasks;
            }

        return null;
    }

    public static FailureTask getFailureTask(Project project) {
        Run run = project.getLastBuild();
        if (run == null || run.isBuilding() || project.isInQueue()) {
            return null;
        }
        Result result = run.getResult();
        if (result == null) {
            return null;
        }

        // 1. 判断上次构建的任务是否是 定时任务 或者 依赖触发
        CauseAction causeAction = run.getAction(CauseAction.class);
        if (causeAction != null) {
            String triggerInfo = causeAction.getCauses().get(0).getShortDescription();
            //剔除手动 执行 失败的任务
            if (!(triggerInfo.startsWith(UPTRIGGER_PREFIX) ||
                    triggerInfo.equals(TIMETRIGGER_KEY))) {
                return null;
            }
        }
        String context = getContextFromRun(run);
        //  2. 依据作业定义的触发时间看是否 已经被触发了
        if (isNoneDocurrentTask(project, run)) {
            return null;
        }

        if (result.equals(Result.FAILURE) || result.equals(Result.ABORTED)) {

            FailureTask task = new FailureTask();
            task.setLastBuildResult(run.getResult().toString());
            task.setIsChecked("false");
            task.setLastContext(context);
            task.setOwner(project.getOwnerName());
            task.setLastBuildTime(sdf.format(new Date(run.getStartTimeInMillis())));
            task.setName(project.getName());
            return task;
        }
        return null;
    }


    private static boolean isNoneDocurrentTask(Project project, Run run) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String startTime = sdf.format(run.getStartTimeInMillis());
        if (StringUtils.isEmpty(startTime)) {
            return true;
        }
        String currentDate = sdf.format(new Date());
        if (!currentDate.equals(startTime)) {
            return true;
        }
        return false;
    }

    public static void doBuildFailure(Project project) {
        Run run = project.getLastBuild();
        if (run == null || run.isBuilding() || project.isInQueue()) {
            return;
        }
        String context = getContextFromRun(run);
        Result result = run.getResult();
        if (result == null) {
            return;
        }
        if (result.equals(Result.FAILURE) && StringUtils.isNotEmpty(context)) {
            ParametersAction parametersAction = run.getAction(ParametersAction.class);
            CauseAction causeAction = run.getAction(CauseAction.class);
            if (parametersAction == null) {
                return;
            }
            if (causeAction != null) {
                String triggerInfo = causeAction.getCauses().get(0).getShortDescription();
                if (!(triggerInfo.startsWith(UPTRIGGER_PREFIX) ||
                        triggerInfo.equals(TIMETRIGGER_KEY))) {
                    return;
                }
            }
            Jenkins.getInstance().getQueue().schedule2(project, 0, parametersAction, causeAction);
        }
    }

    public static void doBuildWithContext(Project project, String context) {
        Run run = project.getLastBuild();
        if (run != null) {
            ParametersAction action = run.getAction(ParametersAction.class);
            List<ParameterValue> values = action.getAllParameters();
            List<ParameterValue> curParameters;
            if (values != null) {
                curParameters = new ArrayList<>(values);
            } else {
                curParameters = new ArrayList<>();
            }
//            while (project.is)
            for (Iterator<ParameterValue> itr = curParameters.iterator(); itr.hasNext(); ) {
                ParameterValue a = itr.next();
                if (a.getName().equals(CONTEXT_KEY)) {
                    itr.remove();
                }
            }
            StringParameterValue contextVal = new StringParameterValue(CONTEXT_KEY, context);
            curParameters.add(contextVal);
            ParametersAction parametersAction = new ParametersAction(curParameters);
            Jenkins.getInstance().getQueue().schedule2(project, 0, parametersAction, new CauseAction(new TimerTrigger.TimerTriggerCause()));
        }
    }

    public static void doBuild(Project project, String context, String type) {
//        try {
//            if (project.getHasCustomQuietPeriod()) {
//                Thread.sleep(project.getQuietPeriod() * 1000 + 3000);
//            } else {
//                Thread.sleep(10 * 1000);
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        if (type.equals(TaskTypeEnum.CRON_TASK.toString())) {
            doBuildWithContext(project, context);
        } else if (type.equals(TaskTypeEnum.FAILURE_TASK.toString())) {
            doBuildFailure(project);
        }
    }

    /**
     * Author wanghf
     * Description //   判断剔除任务
     * 1. 禁用的任务,
     * 2. 登录者 不是 project 的创建者  , 也不是项目联系人
     * Date 2019/1/22 下午5:14
     */
    public static boolean excludeProject(Project project) {
        try {
            String userId = User.current().getId();

            return !adminUser.contains(userId) && (
                    project.isDisabled() || (
                            !userId.equals(project.getOwnerName()) && !project.getContacts().contains(userId))
            );
        } catch (Exception e) {
            LOGGER.info(String.format("======= 获取剔除任务出错, 作业:%s , 原因:", project.getName()));
            return false;
        }
    }

    /**
     * author: wanghf
     * Description 获取 定时的任务
     * Date 2019/1/22 下午3:07
     */
    public static List<CronTask> getAllCronTasks() {
        List<CronTask> cronTasks = new ArrayList<>();
        Jenkins jenkins = Jenkins.getInstance();
        List<Project> projects = jenkins.getProjects();
        int id = 1;
        for (Project project : projects) {

            if (excludeProject(project)) {
                continue;
            }

            Map<TriggerDescriptor, Trigger> triggerMap = project.getTriggers();
            for (Map.Entry<TriggerDescriptor, Trigger> entry : triggerMap.entrySet()) {
                Trigger trigger = entry.getValue();
                List<CronTask> missTasks = getMissCronTasks(project, trigger);
                if (missTasks != null) {
                    for (CronTask task : missTasks) {
                        task.setId(id);
                        cronTasks.add(task);
                        id++;
                    }
                }
                break;
            }

        }
        return cronTasks;
    }

    /**
     * Author wanghf
     * Description 获取当日 未执行、失败的作业
     * Date 2019/1/22 下午3:06
     */
    public static List<FailureTask> getAllExceptionTasks() {
        List<FailureTask> failureTasks = new ArrayList<>();
        Jenkins jenkins = Jenkins.getInstance();
        List<Project> projects = jenkins.getProjects();

        int i = 1;
        for (Project project : projects) {
            try {
                if (excludeProject(project)) {
                    continue;
                }

                FailureTask task = getFailureTask(project);
                if (task != null) {
                    task.setId(i);
                    failureTasks.add(task);
                    i++;
                }
            } catch (Exception e) {
                LOGGER.info(String.format("======= 获取 失败任务报错, 作业:%s , 原因:", project.getName(), e.getMessage()));
            }
        }


        return failureTasks;
    }

    public static Project getProjectByName(String name) {
        Jenkins jenkins = Jenkins.getInstance();
        List<Project> projects = jenkins.getProjects();
        for (Project project : projects) {
            if (project.getName().equals(name)) {
                return project;
            }
        }
        return null;
    }

    public static boolean filterFailureTask(Task task) {
        Project project = getProjectByName(task.getName());
        if (project == null || project.isBuilding() || project.isInQueue()) {
            return false;
        }
        return true;
    }

    public static boolean filterCronTask(CronTask task) {
        Project project = getProjectByName(task.getName());
        if (project == null) {
            return false;
        }
        Date lastBuildTime;
        try {
            lastBuildTime = sdf.parse(task.getLastBuildTime());
        } catch (ParseException e) {
            return true;
        }
        RunList<Run> runList = project.getBuilds();
        for (Run run : runList) {
            if (run.getStartTimeInMillis() > lastBuildTime.getTime()) {
                String context = getContextFromRun(run);
                if (context != null && context.equals(task.getCalContext())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<DagTask> checkDAGCyclic() {
        DependencyGraph g = Jenkins.getInstance().getDependencyGraph();
        List<AbstractProject<?, ?>> sorted = g.getTopologicallySorted();
        GleaningCyclicGraphDetector detector = new GleaningCyclicGraphDetector();
        Map<String, AbstractProject> projectMap = new HashMap<>();
        for (AbstractProject project : sorted) {
            List<AbstractProject> upProjects = project.getUpstreamProjects();
            if (upProjects != null && !project.isDisabled()) {
                for (AbstractProject upProject : upProjects) {
                    detector.addRelation(upProject.getName(), project.getName());
                }
                projectMap.put(project.getName(), project);
            }
        }
        List<DagTask> tasks = new ArrayList<>();
        try {
            detector.checkContainCycle();
        } catch (CyclicGraphDetector.CycleDetectedException e) {
            for (int i = 0; i < e.cycle.size(); i++) {
                AbstractProject project = projectMap.get((String) e.cycle.get(i));
                DagTask task = new DagTask();
                task.setId(i + 1);
                task.setName(project.getName());
                if (project.getLastBuild() != null) {
                    task.setLastBuildTime(sdf.format(new Date(project.getLastBuild().getStartTimeInMillis())));
                    task.setLastContext(getContextFromRun(project.getLastBuild()));
                } else {
                    task.setLastBuildTime("-");
                    task.setLastContext("-");
                }
                task.setOwner(project.getOwnerName());
                tasks.add(task);
            }
        }
        return tasks;
    }

    public static boolean containDAGCyclic() {
        DependencyGraph g = Jenkins.getInstance().getDependencyGraph();
        List<AbstractProject<?, ?>> sorted = g.getTopologicallySorted();
        GleaningCyclicGraphDetector detector = new GleaningCyclicGraphDetector();
        for (AbstractProject project : sorted) {
            List<AbstractProject> upProjects = project.getUpstreamProjects();
            if (upProjects != null && !project.isDisabled()) {
                for (AbstractProject upProject : upProjects) {
                    detector.addRelation(upProject.getName(), project.getName());
                }
            }
        }
        try {
            detector.checkContainCycle();
        } catch (CyclicGraphDetector.CycleDetectedException e) {
            return true;
        }
        return false;
    }
}

