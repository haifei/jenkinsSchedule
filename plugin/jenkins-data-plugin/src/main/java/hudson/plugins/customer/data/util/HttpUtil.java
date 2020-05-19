package hudson.plugins.customer.data.util;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * 2020-04-12 add by wanghf
 */
public class HttpUtil {
    private static final Logger logger = Logger
            .getLogger(HttpUtil.class);

    private static final AsyncHttpClient httpClient = new AsyncHttpClient(
            new AsyncHttpClientConfig.Builder().setMaxConnections(100).build());

    /**
     * 发送get请求,没有header
     *
     * @param baseUrl
     * @param paramMap
     * @return
     * @throws IllegalArgumentException
     */
    public static String get(String baseUrl, Map<String, List<String>> paramMap) {
        return get(baseUrl, null, paramMap);
    }

    /**
     * 发送get请求,没有请求参数
     *
     * @param headers
     * @param baseUrl
     * @return
     */
    public static String get(Map<String, String> headers, String baseUrl) {
        return get(baseUrl, headers, null);
    }

    public static String get(String baseUrl, Map<String, String> headers, Map<String, List<String>> paramMap) {
        try {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = httpClient.prepareGet(baseUrl);

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            } else {
                requestBuilder.addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json;charset=UTF-8");
            }

            Future<Response> response = requestBuilder.setQueryParams(paramMap)
                    .execute(new AsyncCompletionHandler<Response>() {
                        @Override
                        public Response onCompleted(Response response) throws Exception {
                            return response;
                        }
                    });
            if (response.get().getStatusCode() != 200) {
                logger.error(String.format("Request error.url:[%s] error:[%s]", baseUrl, response.get().getResponseBody()));
                return null;
            }
            return response.get().getResponseBody();
        } catch (Exception e) {
            logger.error(String.format("Send get request:[%s] message:[%s] exception:[%s]", baseUrl, paramMap, e.getMessage(), e));
            return null;
        }
    }


    public static int postMessage(String baseUrl,String json) throws IOException {
        logger.info(" msg: " + json);
        HttpResponse response = post(baseUrl, json);
        int statusCode = response.getStatusLine().getStatusCode();
        logger.info(String.format(" post status code = %s", statusCode));
        return statusCode;
    }


    public static HttpResponse post(String baseUrl, String json) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(baseUrl);
        StringEntity entity = new StringEntity(json, Charset.forName("UTF-8"));
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json; charset=utf-8");
        try {
            CloseableHttpResponse response = client.execute(httpPost);
            return response;
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


