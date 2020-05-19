package jenkins.util.login;

import com.google.common.collect.Maps;
import net.sf.json.JSONObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;

/**
 * 2020-04-21
 * Created by wanghf
 */
public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
    private static final String AUTH_URL = "";
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     *  @desc 获取用户详细信息, 访问 公司内部用户中心或自定义实现
     */

    public static Response postUserDetail(String userName) {
        Map<String, String> params = Maps.newHashMap();
        return post(JSONObject.fromObject(params).toString());
    }


    /**
     *  访问 用户中心 ,
     *
     * @param userName
     * @param passwd
     * @return
     */
    public static Response postUserAuth(String userName, String passwd) throws IOException {
        Map<String, String> params = Maps.newHashMap();
        return post(JSONObject.fromObject(params).toString());
    }

    private static Response post(String json) {

        RequestBody body = RequestBody.create(MEDIA_TYPE, json);

        Request request = new Request
                .Builder()
                .post(body)
                .url(AUTH_URL)
                .build();
        try {
            OkHttpClient client = new OkHttpClient();
            return client.newCall(request).execute();
        } catch (SocketTimeoutException e) {
            logger.error(String.format("url:[%s] request  error.", request.url()), e);
        } catch (IOException e) {
            logger.error(String.format("url:[%s] request error.", request.url()), e);
        }

        return null;
    }

    public static String getResponseText(Response response) {
        String responseText = null;
        try {
            if (response != null && response.code() == 200) {
                responseText = response.body().string();
            } else {
                logger.error(String.format("Call Interface fail.response:%s", response));
            }
        } catch (IOException e) {
            logger.error(String.format("Call  Interface error.response:%s", e));
        }
        return responseText;
    }


}
