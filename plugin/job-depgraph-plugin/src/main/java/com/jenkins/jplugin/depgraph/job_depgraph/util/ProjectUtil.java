package com.jenkins.jplugin.depgraph.job_depgraph.util;

import com.jenkins.jplugin.depgraph.job_depgraph.model.context.BuildInfo;
import com.jenkins.jplugin.depgraph.job_depgraph.model.context.JOB_STATE;
import hudson.model.*;
import hudson.util.RunList;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 2020-04-12 add by wanghf
 */
public class ProjectUtil {
    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static AbstractProject getTopProject(AbstractProject project) {
        AbstractProject topProject = project;
        List<AbstractProject> upProjects = project.getUpstreamProjects();
        if (upProjects != null && upProjects.size() != 0) {
            topProject = upProjects.get(0);
            return ProjectUtil.getTopProject(topProject);
        } else {
            return project;
        }
    }

    public static JOB_STATE getJobState(AbstractProject project) {
        if (project.isBuilding()) {
            return JOB_STATE.BUILDING;
        }
        if (ProjectUtil.hasBuildingInUpstream(project)) {
            return JOB_STATE.PEDDING;
        }
        String lastRunState = getLastRunState(project);
        if (lastRunState == null) {
            return JOB_STATE.NOT_BUILT;
        }

        AbstractProject rootProject = ProjectUtil.getTopProject(project);
        if (rootProject != null) {
            String lastRootContext = ProjectUtil.getLastBuildContext(rootProject);
            if (lastRootContext == null) {
                return JOB_STATE.NOT_BUILT;
            }
            String lastCurrContext = ProjectUtil.getLastBuildContext(project);
            if (lastCurrContext == null || !lastCurrContext.equals(lastRootContext)) {
                return JOB_STATE.NOT_BUILT;
            }
            if (ProjectUtil.hasFailureInUpstream(project, lastRootContext)) {
                return JOB_STATE.NOT_BUILT;
            }
        }
        return JOB_STATE.valueOf(lastRunState);
    }

    public static String getLastRunState(AbstractProject project) {
        AbstractBuild lastBuild = project.getLastBuild();
        if (lastBuild != null) {
            return lastBuild.getResult().toString();
        }
        return null;
    }

    // TODO: 2017/5/2 小心循环依赖问题
    public static boolean hasBuildingInUpstream(AbstractProject project) {
        List<AbstractProject> upstreamProjects = project.getUpstreamProjects();
        if (upstreamProjects != null && upstreamProjects.size() != 0) {
            for (AbstractProject pro : upstreamProjects) {
                if (pro.isBuilding()) {
                    return true;
                }
                return hasBuildingInUpstream(pro);
            }
        }
        return false;
    }

    public static boolean hasFailureInUpstream(AbstractProject project, String context) {
        List<AbstractProject> upstreamProjects = project.getUpstreamProjects();
        if (upstreamProjects != null && upstreamProjects.size() != 0) {
            for (AbstractProject pro : upstreamProjects) {
                String lastBuildContext = getLastBuildContext(pro);
                String lastRunState = ProjectUtil.getLastRunState(pro);
                if (lastBuildContext != null
                        && lastRunState != null
                        && lastBuildContext.equals(context)
                        && lastRunState.equals(JOB_STATE.FAILURE.toString())) {
                    return true;
                }
                return hasFailureInUpstream(pro, context);
            }
        }
        return false;
    }

    public static String getLastBuildContext(AbstractProject project) {
        AbstractBuild lastBuild = project.getLastBuild();
        return ProjectUtil.getContextFromRun(lastBuild);
    }

    public static String getContextFromRun(Run run) {
        if (run != null) {
            ParametersAction action = run.getAction(ParametersAction.class);
            if (action != null) {
                ParameterValue contextVal = action.getParameter("time_hour");
                if (contextVal != null) {
                    return contextVal.getValue().toString();
                }
            }
        }
        return null;
    }

    public static BuildInfo genBuildInfoWithStatus(AbstractProject project, JOB_STATE state) {
        BuildInfo buildInfo = new BuildInfo();
        buildInfo.setJobName(project.getName());
        buildInfo.setJobCreator(project.getOwnerName());
        buildInfo.setJobStatus(state.toString());
        return buildInfo;
    }

    public static BuildInfo genRealBuildInfo(AbstractProject project, String context, Run run) {
        BuildInfo buildInfo = new BuildInfo();
        buildInfo.setContext(context);
        buildInfo.setJobCreator(project.getOwnerName());
        buildInfo.setJobLastInstanceID(run.getId());
        buildInfo.setJobName(project.getName());
        buildInfo.setJobRunEndTime(run.hasntStartedYet() ? -1 : (run.isBuilding() ? transTimespans(System.currentTimeMillis() - run.getStartTimeInMillis()) : transTimespans(run.getDuration())));
        buildInfo.setJobRunStartTime(sdf.format(run.getTime()));
        String status;
        if (run.isBuilding() && run.getResult() == null) {
            status = JOB_STATE.BUILDING.toString();
        } else {
            status = run.getResult().toString();
        }
        buildInfo.setJobStatus(status);
        CauseAction action = run.getAction(CauseAction.class);
        if (action != null) {
            buildInfo.setJobTrigger(action.getCauses().get(0).getShortDescription());
        } else {
            buildInfo.setJobTrigger("Unkown");
        }
        return buildInfo;
    }

    public static Run findLastBuildByContext(AbstractProject project, String context) {
        RunList<Run> runs = project.getBuilds();
        if (runs == null || runs.isEmpty()) {
            return null;
        }
        Run lastRun = null;
        Long timestamp = 0L;
        for (Run run : runs) {
            if (context.equals(ProjectUtil.getContextFromRun(run))) {
                if (timestamp < run.getTimeInMillis()) {
                    timestamp = run.getTimeInMillis();
                    lastRun = run;
                }
            }
        }
        return lastRun;
    }

    public static long transTimespans(long t) {
        return t / 1000;
    }

    public static BuildInfo getLastBuildInfo(AbstractProject project, String context) {
        Run run;
        if (context != null && !context.isEmpty()) {
            RunList<Run> runs = project.getBuilds();
            if (runs == null || runs.isEmpty()) {
                return genBuildInfoWithStatus(project, JOB_STATE.NOT_BUILT);
            }
            run = findLastBuildByContext(project, context);
            if (run == null) {
                if (ProjectUtil.hasBuildingInUpstream(project)) {
                    return genBuildInfoWithStatus(project, JOB_STATE.PEDDING);
                }
                return genBuildInfoWithStatus(project, JOB_STATE.NOT_BUILT);
            }
        } else {
            run = project.getLastBuild();
            if (run == null) {
                return genBuildInfoWithStatus(project, JOB_STATE.NOT_BUILT);
            }
        }
        String cont = ProjectUtil.getContextFromRun(run);
        if (cont != null) {
            return genRealBuildInfo(project, cont, run);
        }
        return genBuildInfoWithStatus(project, JOB_STATE.NOT_BUILT);
    }
}
