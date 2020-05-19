package hudson.plugins.customer.data.remoteapinotifiy;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.customer.data.util.HttpUtil;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 成功的作业被手动触发后，短信和邮件通知下游依赖任务的owner
 * Created by wanghf on 2018/09/22.
 */
public class RemoteAPINotifiy extends Notifier {
    private static final Logger logger = LoggerFactory.getLogger(RemoteAPINotifiy.class);
    private static final Map<String, String> logPatternMap = Maps.newHashMap(ImmutableMap.of("numRows", "stats:\\s+\\[numFiles=\\d+,\\s+numRows=([\\d]+),\\s+totalSize=\\d+,\\s+rawDataSize=\\d+"));
    private static final String patterNumber = "/^\\d+$/";


    private enum ValidationResult {
        DO_NOTHING, DO_TRIGGER
    }


    public String callbackUrl;
    public String time_hour;

    @Override
    public DescriptorImpl getDescriptor() {
        Jenkins j = Jenkins.getInstance();
        return (j != null) ? j.getDescriptorByType(DescriptorImpl.class) : null;
    }


    @DataBoundConstructor
    public RemoteAPINotifiy(String callbackUrl) {
        super();
        this.callbackUrl = callbackUrl;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        if (StringUtils.isEmpty(callbackUrl)) {
            return true;
        }
        listener.getLogger().println(String.format("回调函数:  [%s]", callbackUrl));
        System.out.println(String.format("============== 回调函数:  [%s]", callbackUrl));

        Map<String, Object> result = Maps.newHashMap();
        result.put("taskName", build.getProject().getFullName());
        result.put("taskStatus", build.getResult().toString());

        time_hour = "";
        ParameterValue pv = getParameterValue(build, "time_hour");
        if (null != pv) {
            time_hour = String.valueOf(pv.getValue());
        }
        result.put("timeHour", time_hour);

        Map<String, Object> stringParamters = getStringParameters(build, listener);
        if (stringParamters != null && stringParamters.size() > 0) {
            result.put("paramters", stringParamters);
        }


        try {
            Gson gson = new Gson();
            String jsonData = gson.toJson(NotifiyMessage.success(result));
            int code = HttpUtil.postMessage(callbackUrl, jsonData);
            listener.getLogger().println(String.format("回调函数返回的code: ") + code);
        } catch (IOException e) {
            listener.getLogger().println(String.format("回调函数请求异常"));
        }


        return true;
    }

    private Map<String, Object> getStringParameters(AbstractBuild<?, ?> build, BuildListener listener) {
        ParametersAction action = build.getAction(ParametersAction.class);
        List<ParameterValue> allParameters = action.getAllParameters();
        Map<String, Object> parameters = Maps.newHashMap();
        for (ParameterValue parameter : allParameters) {
            if (parameter.getName().equals("time_hour")) {
                continue;
            }
            parameters.put(parameter.getName(), parameter.getValue());
            //通过正则匹配运行日志,获取执行数据的数据量
            if (logPatternMap.keySet().contains(parameter.getName())) {
                try {
                    String patternStr = logPatternMap.get(parameter.getName());
                    BufferedReader logReader = new BufferedReader(new InputStreamReader(build.getLogInputStream()));
                    String line = null;
                    StringBuilder sb = new StringBuilder();
                    while ((line = logReader.readLine()) != null) {
                        sb.append(line);
                    }
                    String log = sb.toString();
                    sb = null;
                    Matcher matcher = Pattern.compile(patternStr).matcher(log);
                    while (matcher.find()) {
                        parameters.put(parameter.getName(), matcher.group(1));
                    }
                } catch (Exception e) {
                    listener.getLogger().println(String.format("回调函数请求异常, 匹配logPatternMap[%s]出错,错误原因:%s", parameter.getName(), e.getMessage()));
                }

            }
        }
        return parameters;
    }

    private ParameterValue getParameterValue(AbstractBuild<?, ?> build, String parameter) {
        ParametersAction action = build.getAction(ParametersAction.class);
        ParameterValue value = null;
        if (null != action) {
            value = action.getParameter(parameter);
        }

        return value;
    }

    /*执行状态*/
    private ValidationResult validWithBuildSuccess(AbstractBuild<?, ?> build) {
        if (build.getResult().equals(Result.SUCCESS) || build.getResult().equals(Result.FAILURE)) {
            return ValidationResult.DO_TRIGGER;
        } else {
            return ValidationResult.DO_NOTHING;
        }
    }

    @Extension(ordinal = 1001)
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
            return "远程API回调";
        }

    }

}
