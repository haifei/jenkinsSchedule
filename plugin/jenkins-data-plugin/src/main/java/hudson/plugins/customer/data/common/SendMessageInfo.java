package hudson.plugins.customer.data.common;

/**
 * Created by wanghf on 2018/9/29.
 */
public enum SendMessageInfo {
    /**
     * 短信发送
     */
    SMS("sms", "520680", "/tnotice/sms/sendAlarm"),
    DINGDING("dingding", "520680", "/tnotice/dingtalk/send");

    private String name;
    private String url;
    private String appId;

    SendMessageInfo(String name, String appId, String url) {
        this.name = name;
        this.url = url;
        this.appId = appId;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAppId() {
        return appId;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
