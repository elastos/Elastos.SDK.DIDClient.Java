package org.elastos.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Http请求
 */
public class HttpUtil {

    private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    private static class TrustAnyTrustManager implements X509TrustManager {
        private TrustAnyTrustManager() {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }

    public static CloseableHttpClient createHttpClient() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] tm = new TrustManager[]{new HttpUtil.TrustAnyTrustManager()};
        sslContext.init(null, tm, new SecureRandom());
        SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.2", "TLSv1", "TLSv1.1", "SSLv3"},
                null,
                NoopHostnameVerifier.INSTANCE);

        BasicHttpClientConnectionManager connManager;
        connManager = new BasicHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", sslConnectionFactory)
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
     *
     * @param url
     * @return
     */
    public static String get(String url, Map<String, String> headers) {
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
            logger.info("get URL：" + url + ";code：" + code);
            if (code == HttpStatus.SC_OK) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * key-value格式的参数
     *
     * @param url
     * @param params
     * @return
     */
    public static String post(String url, Map<String, Object> params, Map<String, String> headers) {
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
            logger.info("post URL：" + url + ";code：" + code);
            if (code == HttpStatus.SC_OK) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 请求json格式的参数
     *
     * @param url
     * @param params
     * @return
     */
    public static String post(String url, String params, Map<String, String> headers) {
        try {
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
            response = httpclient.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            logger.info("post URL：" + url + ";code：" + code);
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

    public static String upload(String url, String uploadName, File uploadFile, Map<String, Object> params, Map<String, String> headers) {
        try {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(200000).setSocketTimeout(200000000).build();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig);
            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            //multipartEntityBuilder.setCharset(Charset.forName("UTF-8"));

            if (null != headers) {
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    httpPost.setHeader(h.getKey(), h.getValue());
                }
            }

            if (null != params) {
                for (Map.Entry<String, Object> p : params.entrySet()) {
                    String key = p.getKey();
                    String value = String.valueOf(p.getValue());
                    multipartEntityBuilder.addTextBody(key, value);
                }
            }

            multipartEntityBuilder.addBinaryBody(uploadName, uploadFile);

            HttpEntity httpEntity = multipartEntityBuilder.build();
            httpPost.setEntity(httpEntity);

            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity responseEntity = httpResponse.getEntity();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(responseEntity.getContent(), StandardCharsets.UTF_8));
                StringBuffer sb = new StringBuffer("");
                String line = "";
                String NL = System.getProperty("line.separator");
                while ((line = in.readLine()) != null) {
                    sb.append(line + NL);
                }
                in.close();
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String download(String url, Map<String, String> headers, File targetFile) {
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
            logger.info("get URL：" + url + ";code：" + code);
            if (code == HttpStatus.SC_OK) {
                try {
                    response.getEntity().writeTo(new FileOutputStream(targetFile)); // 写入文件
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

