package hudson.plugins.customer.data.alarmsdkv2;


import com.google.gson.Gson;
import hudson.plugins.customer.data.common.SendMessageInfo;
import hudson.plugins.customer.data.util.HttpUtil;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Created by wangf on 2020-04-23
 */
public class AlarmV2 implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AlarmV2.class);

    protected String users;
    protected Map<String, Object> message;

    /**
     * 消息发送平台
     */
    protected String url;


    public AlarmV2(String users, Map<String, Object> message) {
        this.users = users;
        this.message = message;
    }


    /**
     * @desc 需要用户自定实现发送 短信告警的逻辑
     */
    public void sendSms() {
        //初始化url
        //url=
        //调用消息平台 发送短信告警
        //trigger(SendMessageInfo.SMS);
    }


    public void sendDingDing() {
        //初始化url
        //url=
        //调用消息平台 发送短信告警
        //trigger(SendMessageInfo.DINGDING);
    }


    private void trigger(SendMessageInfo sendMessageInfo) {
        boolean flag = false;
        int num = 3;
        while (!flag && num > 0) {
            try {
                flag = send(users, message, sendMessageInfo.toString());
            } catch (Exception e) {
                logger.info(String.format("发送给[%s]的%s失败, %s", users, sendMessageInfo.toString(), e.getMessage()));
            }

            if (!flag) {
                num -= 1;
                logger.info(String.format("发送给[%s]的%s失败, sleep 5s 重试...", users, sendMessageInfo.toString()));
            } else {
                break;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                logger.info(String.format("Thread sleep 5s Error: %s", users, e1.getMessage()));
                e1.printStackTrace();
            }

        }
    }

    private boolean send(String user, Map<String, Object> message, String type) throws IOException {

        if (null == user || user.trim().length() < 1) {
            logger.info(String.format("用户[%s]为空!", user));
            return true;
        }


        Gson gson = new Gson();
        String json = gson.toJson(message);
        logger.info(type + " msg: " + json);
        HttpResponse response = HttpUtil.post(url, json);
        int statusCode = response.getStatusLine().getStatusCode();
        logger.info(String.format("send to [%s] %s, post status code = %s", user, type, statusCode));
        return statusCode == 200;
    }

}
