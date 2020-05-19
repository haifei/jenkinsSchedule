package hudson.plugins.customer.data.timeoutalarm;

import com.google.common.base.Joiner;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.customer.data.util.TimeUtils;
import hudson.plugins.customer.data.alarmsdkv2.AlarmV2;
import hudson.plugins.customer.data.alarm.AbstractAlarm;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 2020-04-12 add by wanghf
 */
public class TimeoutAlarm extends AbstractAlarm {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutAlarm.class);

    private Double duration = 0d;
    private String startTime = "0";

    @DataBoundConstructor
    public TimeoutAlarm(boolean triggerEmail, boolean triggerSms, boolean triggerDingDing) {
        this.triggerEmail = triggerEmail;
        this.triggerSms = triggerSms;
        this.triggerDingDing = triggerDingDing;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public TimeoutDescriptorImpl getDescriptor() {
        Jenkins j = Jenkins.getInstance();
        return (j != null) ? j.getDescriptorByType(TimeoutDescriptorImpl.class) : null;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("[Timeout Alart Plugin] perform.");
        configParameter(build, listener);

        try {
            duration = Double.parseDouble(String.valueOf(System.currentTimeMillis() - build.getStartTimeInMillis()));
        } catch (Exception e) {
            //skip ...
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        startTime = sdf.format(build.getStartTimeInMillis());


        if (isTrigger(duration.longValue() / 1000)) {

            triggerSms(build, listener);
            triggerDingDing(build, listener);
        }
        clearGlobalObject();
        return true;
    }

    private boolean isTrigger(long currentDuration) {
        int i = 1;
        long totalDuration = 0;//秒
        Job job = (Job) Jenkins.getInstance().getItemByFullName(taskName);
        RunList<FreeStyleBuild> list = job.getBuilds();

        for (FreeStyleBuild build : (RunList<FreeStyleBuild>) job.getBuilds()) {
            if (i > 16)//取最近15天 完成作业的持续时间
                break;
            if (build.getResult() != null && build.getResult().toString().equals("SUCCESS")) {
                totalDuration = totalDuration + build.getDuration() / 1000;
                i++;
            }
        }

        long averageDuration = totalDuration + 20 * 60; //平均值+20 分钟 作为阈值

        return currentDuration >= averageDuration;  //当前时间大于阈值触发 超时告警
    }


    @Override
    public boolean triggerSms(AbstractBuild<?, ?> build, BuildListener listener) {
        if (!triggerSms) {
            return true;
        }

        listener.getLogger().println(String.format("Triggering [%s] SMS Notification -----------------------",
                Joiner.on(",").join(contactUsers)));
        Map<String, Object> contentMap = new HashMap<>();
        String content = String.format("[超时告警] [%s] 运行超时 , 开始时间[%s] , 持续时间[%s] , 负责人[%s] , Context[%s] , 请检查!!!",
                taskName, startTime, TimeUtils.parse(duration), owner, time_hour);

        contentMap.put("content", content);
        contentMap.put("mobiles", Joiner.on(",").join(contactUserPhones));

        logger.info(String.format("Send Message.Context:%s", content));
        new AlarmV2(Joiner.on(",").join(contactUsers), contentMap).sendSms();
        return true;
    }


    @Override
    public boolean triggerDingDing(AbstractBuild<?, ?> build, BuildListener listener) {
        if (!triggerDingDing) {
            return true;
        }

        listener.getLogger().println(String.format("Triggering [%s] DINGDING Notification -----------------------",
                Joiner.on(",").join(contactUsers)));
        Map<String, Object> contentMap = new HashMap<>();
        String content = String.format("[超时告警] [%s] 运行超时 , 开始时间[%s] , 持续时间[%s] , 负责人[%s] , Context[%s] , 请检查!!!",
                taskName, startTime, TimeUtils.parse(duration), owner, time_hour);
        contentMap.put("content", content);
        contentMap.put("mobiles", Joiner.on(",").join(contactUserPhones));

        logger.info(String.format("Send Message.Context:%s", content));
        new AlarmV2(Joiner.on(",").join(contactUsers), contentMap).sendDingDing();
        return true;
    }


    @Extension(ordinal = 1002)
    public static final class TimeoutDescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Timeout Alert";
        }

    }
}
