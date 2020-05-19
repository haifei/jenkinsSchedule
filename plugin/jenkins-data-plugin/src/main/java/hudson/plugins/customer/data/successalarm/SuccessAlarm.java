package hudson.plugins.customer.data.successalarm;

/**
 * @author wanghf
 * @desc 成功发送消息
 */

import com.google.common.base.Joiner;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.customer.data.alarmsdkv2.AlarmV2;
import hudson.plugins.customer.data.alarm.AbstractAlarm;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class SuccessAlarm extends AbstractAlarm {

    private enum ValidationResult {
        DO_NOTHING, DO_TRIGGER
    }

    @DataBoundConstructor
    public SuccessAlarm(boolean triggerEmail, boolean triggerSms, boolean triggerDingDing) {
        super();
        this.triggerSms = triggerSms;
        this.triggerDingDing = triggerDingDing;
    }

    protected ValidationResult validWithPreviousResults(AbstractBuild<?, ?> build) {
        return build.getResult() == Result.SUCCESS ? ValidationResult.DO_TRIGGER : ValidationResult.DO_NOTHING;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("[task success plugin] perform...");
        ValidationResult validationResult = validWithPreviousResults(build);
        configParameter(build, listener);

        if (validationResult != ValidationResult.DO_NOTHING) {
            if (validationResult == ValidationResult.DO_TRIGGER) {
                triggerSms(build, listener);
                triggerDingDing(build, listener);
            }
        }
        clearGlobalObject();
        return true;
    }

    @Override
    public SuccessAlarm.DescriptorImpl getDescriptor() {
        Jenkins j = Jenkins.getInstance();
        return (j != null) ? j.getDescriptorByType(SuccessAlarm.DescriptorImpl.class) : null;
    }

    /**
     * @desc 短信发送
     */
    @Override
    public boolean triggerSms(AbstractBuild<?, ?> build, BuildListener listener) {
        if (!triggerSms) {
            return true;
        }

        listener.getLogger().println(String.format("Triggering [%s] SMS Notification -----------------------",
                Joiner.on(",").join(contactUsers)));

        Map<String, Object> contentMap = new HashMap<>();
        String content = String.format("[job任务调度][%s]执行成功, 负责人[%s], Context[%s], 启动时间[%s], 请检查!",
                taskName, owner, time_hour, buildTime(build));

        contentMap.put("content", content);
        contentMap.put("mobiles", Joiner.on(",").join(contactUserPhones));

        new AlarmV2(Joiner.on(",").join(contactUsers), contentMap).sendSms();

        return true;
    }


    @Override
    public boolean triggerDingDing(AbstractBuild<?, ?> build, BuildListener listener) {
        if (!triggerDingDing) {
            return true;
        }

        listener.getLogger().println(String.format("Triggering [%s] DingDing Notification -----------------------",
                Joiner.on(",").join(contactUsers)));

        Map<String, Object> contentMap = new HashMap<>();
        String content = String.format("[job任务调度][%s]执行成功, 负责人[%s], Context[%s], 启动时间[%s], 请检查!",
                taskName, owner, time_hour, buildTime(build));


        contentMap.put("content", content);
        contentMap.put("mobiles", Joiner.on(",").join(contactUserPhones));

        new AlarmV2(Joiner.on(",").join(contactUsers), contentMap).sendDingDing();

        return true;
    }


    @Extension(ordinal = 1003)
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "成功提示";
        }

    }

    private String buildTime(AbstractBuild<?, ?> build) {
        long startTime = build.getStartTimeInMillis();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义格式，不显示毫秒
        Timestamp timestamp = new Timestamp(startTime);
        return df.format(timestamp);
    }

}
