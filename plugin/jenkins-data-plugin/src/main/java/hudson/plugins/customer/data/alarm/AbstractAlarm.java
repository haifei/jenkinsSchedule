package hudson.plugins.customer.data.alarm;

import hudson.model.*;
import hudson.tasks.Notifier;

import java.util.HashSet;

/**
 * 2020-04-12 add by wanghf
 */
public abstract class AbstractAlarm extends Notifier {

    public boolean triggerEmail;
    public boolean triggerSms;
    public boolean triggerDingDing;


//    public ConfParser conf = null;

    public String owner;
    public String contacts;
    /*接收通知的所有用户*/
    public HashSet<String> contactUsers;
    public HashSet<String> owners;
    public HashSet<String> contactUserPhones; /*联系人手机号*/
    public String rootUrl;
    public String taskName;
    public String time_hour;

    private ParameterValue getParameterValue(AbstractBuild<?, ?> build, String parameter) {
        ParametersAction action = build.getAction(ParametersAction.class);
        ParameterValue value = null;
        if (null != action) {
            value = action.getParameter(parameter);
        }

        return value;
    }

    protected void configParameter(AbstractBuild<?, ?> build, BuildListener listener) {
        AbstractProject<?, ?> project = build.getProject();

        owner = project.getOwnerName();
        contacts = project.getContacts();
        rootUrl = project.getUrl();
        taskName = project.getName();
        owners = new HashSet<String>();
        if (null != owner && !owner.trim().isEmpty()) {
            for (String u : owner.split(",")) {
                if (!u.isEmpty()) {
                    owners.add(u);
                }
            }
        }

        contactUsers = new HashSet<>();
        contactUserPhones = new HashSet<>();
        contactUsers.addAll(owners);
        if (null != contacts && !contacts.trim().isEmpty()) {
            for (String u : contacts.split(",")) {
                if (!u.isEmpty()) {
                    contactUsers.add(u);
                }
            }
        }

        afterUpdateContactUsers(contactUsers);

        for (String u : contactUsers) {
            User user = User.getById(u, false);
            // 如果 调度系统中没有, 则访问 用户中心用户信息
            if (user == null) {
                user = remoteGetUesrInfoByLDAP(u);
            }
            if (user == null) {
                continue;
            }
            contactUserPhones.add(user.getPhone());
        }

        time_hour = "";
        ParameterValue pv = getParameterValue(build, "time_hour");
        if (null != pv) {
            time_hour = String.valueOf(pv.getValue());
        }
    }


    public void clearGlobalObject() {

        this.owner = null;
        this.contacts = null;
        this.contactUsers = null;
        this.owners = null;
        this.contactUserPhones = null;
        this.rootUrl = null;
        this.taskName = null;
        this.time_hour = null;
    }

    public void afterUpdateContactUsers(HashSet<String> contactUsers) {
        //啥都不做
    }

    /**
     * @param userName ,  登录的用户Id
     * @dec  这里可以通过 访问用户中心 获取用户具体信息
     */
    protected User remoteGetUesrInfoByLDAP(String userName) {
        return new User(userName, userName, "12345678912",
                userName+"@jenkins.com", "001", "数据中心", "001", "大数据开发工程师", "001");

    }

    /**
     * @desc 短信发送
     */
    public abstract boolean triggerSms(AbstractBuild<?, ?> build, BuildListener listener);

    public abstract boolean triggerDingDing(AbstractBuild<?, ?> build, BuildListener listener);
}
