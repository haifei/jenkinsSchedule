package hudson.plugins.customer.data.rebuild;


import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.customer.data.common.Constants;
import hudson.plugins.customer.data.util.ProjectDependence;
import hudson.plugins.customer.data.alarmsdkv2.AlarmV2;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 2020-04-12 add by wanghf
 * 成功的作业被手动触发后，短信和邮件通知下游依赖任务的owner
 */

public class RebuildNotifiy extends Notifier {
    private static final Logger logger = LoggerFactory.getLogger(RebuildNotifiy.class);

    private enum ValidationResult {
        DO_NOTHING, DO_TRIGGER
    }

    public boolean triggerDownstream;
    public String time_hour;

    @Override
    public DescriptorImpl getDescriptor() {
        Jenkins j = Jenkins.getInstance();
        return (j != null) ? j.getDescriptorByType(DescriptorImpl.class) : null;
    }


    @DataBoundConstructor
    public RebuildNotifiy(boolean triggerDownstream) {
        super();
        this.triggerDownstream = triggerDownstream;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("[task rebuild plugin] perform...");
        boolean isStartByUser = false;
        String byUser = "";
        try {
            List<Action> actions = build.getActions();
            for (Action action : actions) {
                if (action instanceof CauseAction) {
                    CauseAction ca = (CauseAction) action;
                    List<Cause> causes = ca.getCauses();
                    for (Cause cause : causes) {
                        if (cause instanceof Cause.UserIdCause) {
                            isStartByUser = true;
                            byUser = ((Cause.UserIdCause) cause).getUserId();
                        }
                    }
                }
            }

            if (Constants.AdminUser.equals(byUser)) {
                listener.getLogger().println("由admin用户手动启动，不进行通知！");
                return true;
            }

            if (!triggerDownstream) {
                listener.getLogger().println("手动执行通知开关没有开启，不进行通知！");
                return true;
            }

            // 不是被用户触发的, 不告警
            if (!isStartByUser) {
                listener.getLogger().println("该作业不是由用户启动的，不进行手动执行通知！");
                return true;
            }

            ProjectDependence pd = new ProjectDependence();
            Set<String> owners = new LinkedHashSet<>();
            owners.add(build.getParent().getOwnerName());

            pd.getDownstreamOwnersForBuild(build, listener);

            listener.getLogger().println(String.format("该作业是由用户[ %s ]启动的. 接收用户：[ %s ]",
                    byUser, StringUtils.join(owners, ",")));

            if (owners.size() < 1) {
                listener.getLogger().println("接收用户为空，请检查本作业及下游作业的own_name配置是否为空");
                return true;
            }

            time_hour = "";
            ParameterValue pv = getParameterValue(build, "time_hour");
            if (null != pv) {
                time_hour = String.valueOf(pv.getValue());
            }

            //用户触发, 并且构建成功, 通知下游
            String taskName = build.getProject().getName();
            if (validWithBuildSuccess(build).equals(ValidationResult.DO_TRIGGER)) {
                for (String owner : owners) {
                    triggerAlarm(build, listener, byUser, owner, taskName);
                }
            } else {
                listener.getLogger().println("该作业执行失败，不进行通知！");
            }
        } catch (Exception e) {
            logger.error("执行异常,skip...", e);
        }
        return true;
    }

    private ParameterValue getParameterValue(AbstractBuild<?, ?> build, String parameter) {
        ParametersAction action = build.getAction(ParametersAction.class);
        ParameterValue value = null;
        if (null != action) {
            value = action.getParameter(parameter);
        }


        return value;
    }

    private boolean triggerAlarm(AbstractBuild<?, ?> build, BuildListener listener, String triggerUser, String taskOwner, String taskName) {
        long startTime = build.getStartTimeInMillis();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义格式，不显示毫秒
        Timestamp timestamp = new Timestamp(startTime);
        String start = df.format(timestamp);

        String buildOwner = build.getParent().getOwnerName();

        String content = String.format("任务[%s],负责人[%s],被[%s]手动执行成功,Context[%s],启动时间%s!",
                taskName, buildOwner, triggerUser, time_hour, start);

        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("content", content);
        contentMap.put("mobiles", User.get(triggerUser).getPhone());

        //发送钉钉
        new AlarmV2(triggerUser, contentMap).sendDingDing();

        return true;
    }

    /*执行状态*/
    private ValidationResult validWithBuildSuccess(AbstractBuild<?, ?> build) {
        if (build.getResult().equals(Result.SUCCESS)) {
            return ValidationResult.DO_TRIGGER;
        } else {
            return ValidationResult.DO_NOTHING;
        }
    }

    @Extension(ordinal = 1000)
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        /*
         * (non-Javadoc)
         *
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return "手动执行告警";
        }

    }

}
