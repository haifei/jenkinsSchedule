package hudson.plugins.customer.data.failurealarm;

/**
 * @author wanghf
 * @desc 失败告警
 */

import com.google.common.base.Joiner;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.customer.data.alarm.AbstractAlarm;
import hudson.plugins.customer.data.alarmsdkv2.AlarmV2;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class FailureAlarm extends AbstractAlarm {
    private static final Logger logger = LoggerFactory.getLogger(FailureAlarm.class);


    private enum ValidationResult {
        DO_NOTHING, DO_TRIGGER
    }

    @DataBoundConstructor
    public FailureAlarm(boolean triggerEmail, boolean triggerSms, boolean triggerDingDing) {
        super();
        this.triggerEmail = triggerEmail;
        this.triggerSms = triggerSms;
        this.triggerDingDing = triggerDingDing;
    }

    protected ValidationResult validWithPreviousResults(AbstractBuild<?, ?> build) {
        return build.getResult() == Result.FAILURE ? ValidationResult.DO_TRIGGER : ValidationResult.DO_NOTHING;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("[task failure plugin] perform...");
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
    public void afterUpdateContactUsers(HashSet<String> contactUsers) {
        //添加运维人员
/*        try{
            String responseContent=getOndutyUser();
            if(StringUtils.isNotEmpty(responseContent)){
                JSONObject jsonData = JSON.parseObject(responseContent);
                String ondutyUsers=jsonData.getString("data");
                List<String> ondutyUserList= Lists.newArrayList(ondutyUsers.split(","));
                Set<String> contactUsersCopy=new HashSet<>();
                contactUsersCopy.addAll(contactUsers);
                Set<String> contacts = Sets.union(Sets.newHashSet(ondutyUserList), contactUsersCopy);
                contactUsers.clear();
                contactUsers.addAll(contacts);
            }
        }catch (Exception e){
            logger.error("===获取值班用户失败=====");
            logger.error(e.getMessage());
        }*/
    }


    @Override
    public FailureAlarm.DescriptorImpl getDescriptor() {
        Jenkins j = Jenkins.getInstance();
        return (j != null) ? j.getDescriptorByType(FailureAlarm.DescriptorImpl.class) : null;
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
        String content = String.format("[job任务调度][%s]执行失败, 负责人[%s], Context[%s], 启动时间[%s], 请检查!",
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
        String content = String.format("[job任务调度][%s]执行失败, 负责人[%s], Context[%s], 启动时间[%s], 请检查!",
                taskName, owner, time_hour, buildTime(build));

        contentMap.put("content", content);
        contentMap.put("mobiles", Joiner.on(",").join(contactUserPhones));

        new AlarmV2(Joiner.on(",").join(contactUsers), contentMap).sendDingDing();

        return true;
    }

    @Extension(ordinal = 1001)
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "失败告警";
        }

    }

    private String buildTime(AbstractBuild<?, ?> build) {
        long startTime = build.getStartTimeInMillis();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义格式，不显示毫秒
        Timestamp timestamp = new Timestamp(startTime);
        return df.format(timestamp);
    }

}
