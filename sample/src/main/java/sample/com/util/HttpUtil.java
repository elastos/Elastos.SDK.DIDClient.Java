package sample.com.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Http请求
 *
 */
public class HttpUtil {

    private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    public static CloseableHttpClient createHttpClient(){
        BasicHttpClientConnectionManager connManager;
        connManager = new BasicHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", SSLConnectionSocketFactory.getSocketFactory())
                        .build(),
                null,
                null,
                null
        );

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connManager)
                .build();
        return httpClient;
    }
    /**
     * get请求
     * @param url
     * @return
     */
    public static String get(String url, Map<String, String>headers) {
        try {
            CloseableHttpClient client = HttpUtil.createHttpClient();
            HttpGet httpGet = new HttpGet(url);

            if (null != headers) {
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    httpGet.setHeader(h.getKey(), h.getValue());
                }
            }

            HttpResponse response = client.execute(httpGet);

            int code = response.getStatusLine().getStatusCode();
            logger.info("get URL：" + url + ";code："+ code);
            if (code == HttpStatus.SC_OK) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return result;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * key-value格式的参数
     * @param url
     * @param params
     * @return
     */
    public static String post(String url, Map<String,Object> params, Map<String, String> headers){
        BufferedReader in = null;
        try {
            HttpClient client = HttpUtil.createHttpClient();
            HttpPost httpPost = new HttpPost(url);

            if (null != headers) {
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    httpPost.setHeader(h.getKey(), h.getValue());
                }
            }

            List<NameValuePair> kv = new ArrayList<>();
            for (Map.Entry<String, Object> p : params.entrySet()) {
                String key = p.getKey();
                String value = String.valueOf(p.getValue());
                kv.add(new BasicNameValuePair(key, value));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(kv, StandardCharsets.UTF_8));

            HttpResponse response = client.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            logger.info("post URL：" + url + ";code："+ code);
            if(code == HttpStatus.SC_OK) {
                in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuffer sb = new StringBuffer("");
                String line = "";
                String NL = System.getProperty("line.separator");
                while ((line = in.readLine()) != null) {
                    sb.append(line + NL);
                }
                in.close();
                return sb.toString();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 请求json格式的参数
     * @param url
     * @param params
     * @return
     */
    public static String post(String url, String params, Map<String, String> headers){
        CloseableHttpClient httpclient = HttpUtil.createHttpClient();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-Type", "application/json");
        if (null != headers) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                httpPost.setHeader(h.getKey(), h.getValue());
            }
        }
        StringEntity entity = new StringEntity(params, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            logger.info("post URL：" + url + ";code："+ code);
            if (code == HttpStatus.SC_OK) {
                HttpEntity responseEntity = response.getEntity();
                String jsonString = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                return jsonString;
            }
            if (response != null) {
                response.close();
            }
            httpclient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

