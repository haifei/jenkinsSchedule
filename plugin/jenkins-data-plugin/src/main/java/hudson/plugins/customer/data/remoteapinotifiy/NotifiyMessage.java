package hudson.plugins.customer.data.remoteapinotifiy;

import com.alibaba.fastjson.JSON;

/**
 * @author wanghf on 2019/6/12
 * @desc
 */
public class NotifiyMessage<T> {
    /**
     * 默认成功编号
     */
    private static final String CODE_SUCCESS = "0";
    /**
     * 默认成功信息
     */
    private static final String MESSAGE_SUCCESS = "success";
    /**
     * 默认错误编号
     */
    private static final String CODE_ERROR = "-1";
    /**
     * 默认错误信息
     */
    private static final String MESSAGE_ERROR = "error";


    private String code;

    private String message;


    private T data;

    public NotifiyMessage() {
    }

    public NotifiyMessage(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> NotifiyMessage success(T value) {
        return new NotifiyMessage<>(CODE_SUCCESS, MESSAGE_SUCCESS, value);
    }

    public static <T> NotifiyMessage<T> success() {
        return new NotifiyMessage<>(CODE_SUCCESS, MESSAGE_SUCCESS, null);
    }

    public static <T> NotifiyMessage<T> error(String errorCode, T data) {
        return new NotifiyMessage<>(errorCode, MESSAGE_ERROR, data);
    }

    public static <T> NotifiyMessage<T> error(String errorCode, String errorMsg) {
        return new NotifiyMessage<>(errorCode, errorMsg, null);
    }

    public static <T> NotifiyMessage<T> error(String errorMsg) {
        return new NotifiyMessage<>(CODE_ERROR, errorMsg, null);
    }


    public String buildJsonStrong() {
        return JSON.toJSONString(this);
    }
}
