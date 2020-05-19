package com.jenkins.jplugin.dependency.action;

import com.jenkins.jplugin.dependency.constant.Constants;
import com.jenkins.jplugin.dependency.exception.JobDependencyRuntimeException;
import com.jenkins.jplugin.dependency.pojo.JobDependencyProperty;
import com.jenkins.jplugin.dependency.utils.DateUtils;
import com.jenkins.jplugin.dependency.utils.Utils;
import hudson.model.*;
import hudson.util.RunList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 2020-04-12 add by wanghf
 */
public abstract class AbstractTriggerDownstream implements TriggerDownstreamI {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTriggerDownstream.class);

    private SimpleDateFormat sdf;

    protected Job job;
    protected AbstractProject downstream;
    protected RunList<Run> runlist;
    protected Date tokenDate;
    protected JobDependencyProperty jobProperty;
    protected AbstractBuild upstreamBuild;

    public AbstractTriggerDownstream(Job job,
                                     AbstractProject downstream,
                                     AbstractBuild upstreamBuild,
                                     RunList<Run> runlist,
                                     Date tokenDate,
                                     JobDependencyProperty jobProperty) {
        this.job = job;
        this.downstream = downstream;
        this.upstreamBuild = upstreamBuild;
        this.runlist = runlist;
        this.tokenDate = tokenDate;
        this.jobProperty = jobProperty;
    }

    protected boolean checkHourCondition(int hour, boolean ownself) {
        sdf = new SimpleDateFormat("yyyy/MM/dd/HH");
        String time_hour = sdf.format(DateUtils.getSpecifyHourOfDay(tokenDate, hour));

        return matchTokenFromBuildHistory(time_hour, ownself);
    }

    protected boolean checkHourCondition(int hour) {
        sdf = new SimpleDateFormat("yyyy/MM/dd/HH");
        String time_hour = sdf.format(DateUtils.getSpecifyHourOfDay(tokenDate, hour));

        return matchTokenFromBuildHistory(time_hour, false);
    }

    protected boolean checkTokenHourCondition(int hour, boolean ownself) {
        sdf = new SimpleDateFormat("yyyy/MM/dd/HH");
        String time_hour = sdf.format(DateUtils.getTokenHourOfDay(tokenDate, hour));

        return matchTokenFromBuildHistory(time_hour, ownself);
    }

    protected boolean checkTokenDayCondition(int day, boolean ownself) {
        sdf = new SimpleDateFormat("yyyy/MM/dd");
        String day_hour = sdf.format(DateUtils.getTokenDayOfMonth(tokenDate, day));

        return matchTokenFromBuildHistory(day_hour, sdf, ownself);
    }

    protected boolean checkTokenMonthCondition(int month, boolean ownself) {
        sdf = new SimpleDateFormat("yyyy/MM");
        String month_hour = sdf.format(DateUtils.getTokenMonthOfYear(tokenDate, month));

        return matchTokenFromBuildHistory(month_hour, sdf, ownself);
    }

    protected boolean checkDayCondition(int day) {
        sdf = new SimpleDateFormat("yyyy/MM/dd");
        String day_hour = sdf.format(DateUtils.getSpecifyDayOfMonth(tokenDate, day));

        return matchTokenFromBuildHistory(day_hour, sdf, false);
    }

    protected boolean checkLastDayCondition() {
        sdf = new SimpleDateFormat("yyyy/MM/dd");
        String day_hour = sdf.format(DateUtils.getLastDayOfMonth(tokenDate));

        return matchTokenFromBuildHistory(day_hour, sdf, false);
    }

    protected boolean checkAnyDayCondition() {
        sdf = new SimpleDateFormat("yyyy/MM/dd");
        String day_hour = sdf.format(tokenDate);

        return matchTokenFromBuildHistory(day_hour, sdf, false);
    }

    protected boolean checkWeekCondition(int week) {
        sdf = new SimpleDateFormat("yyyy/MM/dd");
        String week_hour = sdf.format(DateUtils.getSpecifyDayOfWeek(tokenDate, week));

        return matchTokenFromBuildHistory(week_hour, sdf, false);
    }

    protected boolean checkMonthCondition(int month, boolean ownself) {
        sdf = new SimpleDateFormat("yyyy/MM");
        String month_hour = sdf.format(DateUtils.getSpecifyMonthOfYear(tokenDate, month));

        return matchTokenFromBuildHistory(month_hour, sdf, ownself);
    }

    protected boolean checkMonthCondition(int month) {
        sdf = new SimpleDateFormat("yyyy/MM");
        String month_hour = sdf.format(DateUtils.getSpecifyMonthOfYear(tokenDate, month));

        return matchTokenFromBuildHistory(month_hour, sdf, false);
    }

    protected boolean matchTokenFromBuildHistory(String time_hour, SimpleDateFormat sdf, boolean ownself) {

        ParametersAction action;
        ParameterValue parameter;

        String downupstreamName = downstream == null ? "none" : downstream.getName();

        if (ownself) {
            if (runlist.size() == 0) {
                logger.info(String.format("自依赖任务,第一次构建不需要从过去构建记录查找.自依赖任务:%s 上游任务:%s", downstream.getName(), job.getName()));
                return true;
            }
        }

        for (Run run : runlist) {
            action = run.getAction(ParametersAction.class);
            if (null != action) {
                parameter = action.getParameter(Constants.TOKEN);

                try {
                    if (null != parameter
                            && null != parameter.getValue()
                            && parameter.getValue() instanceof String
                            && time_hour.equals(sdf.format(DateUtils.parseDate((String) parameter.getValue())))) {
                        return matchToken(run, time_hour);
                    } else {
                        if (upstreamBuild != null
                                && downstream != null) {
                            logger.warn(String.format("当前任务名:%s buildid:%s parameter:%s 当前任务构建参数:%s 当前任务构建context:%s 期望的任务:%s 期望的任务构建id:%s 期望的context:%s 期望的tokenDate:%s 应被触发的下游:%s",
                                    run.getParent().getName(),
                                    run.getId(),
                                    parameter,
                                    (String) parameter.getValue(),
                                    sdf.format(DateUtils.parseDate((String) parameter.getValue())),
                                    upstreamBuild.getParent().getName(),
                                    upstreamBuild.getId(),
                                    time_hour,
                                    tokenDate,
                                    downstream.getName()));
                        } else {
                            logger.warn(String.format("当前任务名:%s buildid:%s parameter:%s 当前任务构建参数:%s 当前任务构建context:%s 期望的context:%s 期望的tokenDate:%s",
                                    run.getParent().getName(),
                                    run.getId(),
                                    parameter,
                                    (String) parameter.getValue(),
                                    sdf.format(DateUtils.parseDate((String) parameter.getValue())),
                                    time_hour,
                                    tokenDate));
                        }
                    }
                } catch (Exception e) {
                    logger.warn(String.format("time_hour不符合条件.time_hour:%s 当前任务名:%s build_id:%s 应被触发下游任务:%s",
                            parameter.getValue(), run.getParent().getName(), run.getId(), downupstreamName));
                }
            }
        }
        logger.warn(String.format("当前任务:%s time_hour:%s runlist:%s 的构建是不存在的,无法触发下游任务:%s", job.getName(), time_hour, runlist.size(), downupstreamName));
        //清空 runlist
        runlist = null;
        return false;
    }

    protected boolean matchTokenFromBuildHistory(String time_hour, boolean ownself) {

        ParametersAction action;
        ParameterValue parameter;

        String downupstreamName = downstream == null ? "none" : downstream.getName();

        if (ownself) {
            if (runlist.size() == 0) {
                logger.info(String.format("自依赖任务,第一次构建不需要从过去构建记录查找,直接执行即可.自依赖任务:%s 上游任务:%s", downstream.getName(), job.getName()));
                return true;
            }
        }

        for (Run run : runlist) {
            action = run.getAction(ParametersAction.class);
            if (null != action) {
                parameter = action.getParameter(Constants.TOKEN);
                try {
                    if (null != parameter
                            && null != parameter.getValue()
                            && parameter.getValue() instanceof String
                            && time_hour.equals((String) parameter.getValue())) {
                        return matchToken(run, time_hour);
                    } else {
                        if (upstreamBuild != null
                                && downstream != null) {
                            logger.warn(String.format("当前任务名:%s buildid:%s parameter:%s 当前任务构建参数:%s 当前任务构建context:%s 期望的任务:%s 期望的任务构建id:%s 期望的context:%s 期望的tokenDate:%s 应被触发的下游:%s",
                                    run.getParent().getName(),
                                    run.getId(),
                                    parameter,
                                    (String) parameter.getValue(),
                                    sdf.format(DateUtils.parseDate((String) parameter.getValue())),
                                    upstreamBuild.getParent().getName(),
                                    upstreamBuild.getId(),
                                    time_hour,
                                    tokenDate,
                                    downstream.getName()));
                        } else {
                            logger.warn(String.format("当前任务名:%s buildid:%s parameter:%s 当前任务构建参数:%s 当前任务构建context:%s 期望的context:%s 期望的tokenDate:%s",
                                    run.getParent().getName(),
                                    run.getId(),
                                    parameter,
                                    (String) parameter.getValue(),
                                    sdf.format(DateUtils.parseDate((String) parameter.getValue())),
                                    time_hour,
                                    tokenDate));
                        }
                    }
                } catch (Exception e) {
                    logger.warn(String.format("time_hour不符合条件.time_hour:%s 当前任务名:%s build_id:%s 应被触发下游任务:%s",
                            parameter.getValue(), run.getParent().getName(), run.getId(), downupstreamName));
                }
            }
        }

        logger.warn(String.format("当前任务:%s time_hour:%s 的构建是不存在的,无法触发下游任务:%s", job.getName(), time_hour, downupstreamName));
        //清空 runlist
        runlist = null;
        return false;
    }

    /**
     * .TODO 可能存在不是同一个事务的问题
     *
     * @param run
     * @param time_hour
     * @return
     */

    protected boolean matchToken(Run run, String time_hour) {
        if (null != run.getResult()
                && jobProperty.getThreshold().isMet(run.getResult())) {
            logger.info(String.format("当前任务:%s time_hour:%s 的构建是存在的,而且状态为成功", job.getName(), time_hour));
            //符合条件的run存起来
            return true;
        } else {
            logger.warn(String.format("当前任务:%s time_hour:%s 的构建是存在的,但是状态为失败,无法触发下游任务:%s",
                    job.getName(), time_hour, downstream.getName()));
            return false;
        }
    }

    /**
     * 针对 *$ 和 %$ 进行解析,并得到对应的数据,例如针对 %$-1 或者 *$+1 进行操作.
     *
     * @param str
     * @return
     */
    protected String dateAppendToContext(String str) {
        String result = "0";
        try {
            if (str.contains("+")) {
                String[] strs = str.split("\\+");
                result = strs[1].trim();
            } else if (str.contains("-")) {
                String[] strs = str.split("-");
                result = Utils.getStrings(new String[]{"-", strs[1].trim()});
            }
        } catch (Exception e) {
            logger.error(String.format("获取时间错误,请检查condition格式.condition:%s", str));
            throw new JobDependencyRuntimeException(String.format("获取时间错误,请检查condition格式.condition:%s", str));
        }
        return result;
    }

    @Override
    public boolean check() {
        return false;
    }

    @Override
    public List<Run> obtain() {
        return null;
    }
}
