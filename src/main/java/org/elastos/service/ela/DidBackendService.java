/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.service.ela;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.elastos.conf.RetCodeConfiguration;
import org.elastos.entity.ChainType;
import org.elastos.entity.Errors;
import org.elastos.entity.RawTxEntity;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.exception.ApiRequestDataException;
import org.elastos.util.HttpKit;
import org.elastos.util.ela.ElaKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class DidBackendService {
    private static Logger logger = LoggerFactory.getLogger(DidBackendService.class);
    private static String didPreFix;

    private static final String getUtosByAddr = "/api/1/didexplorer/asset/utxos";
    private static final String transaction = "/api/1/didexplorer/transaction";

    static {
        boolean ret = OutSideConfig.readOutSide();
        if (ret) {
            didPreFix = OutSideConfig.getObject("node.prefix");
        } else {
            didPreFix = "http://localhost:8091";
        }
    }

    public enum ReqMethod {
        GET,
        POST
    }

    /**
     * check address
     *
     * @param address
     */
    private static void checkAddr(String address) {
        if (!ElaKit.checkAddress(address)) {
            throw new ApiRequestDataException(Errors.ELA_ADDRESS_INVALID.val() + ":" + address);
        }
    }

    public static List<Map> getUtxoListByAddr(String address, ChainType type) {

        checkAddr(address);

        ReturnMsgEntity msgEntity = elaReqChainData(ReqMethod.GET, didPreFix + getUtosByAddr + "/" + type.toString() + "/" + address, null);
        if (msgEntity.getStatus() == RetCodeConfiguration.SUCC) {
            try {
                List<Map> data = (List<Map>) msgEntity.getResult();
                List<Map> utxoList = (List<Map>) data.get(0).get("Utxo");
                return utxoList;
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.warn(" address has no utxo yet .");
                return null;
            }
        } else {
            return null;
        }
    }

    public static ReturnMsgEntity sendRawTransaction(String rawTx, ChainType type) {

        RawTxEntity entity = new RawTxEntity();
        entity.setData(rawTx);
        entity.setType(type);
        entity.setType(ChainType.MAIN_CHAIN);

        String jsonEntity = JSON.toJSONString(entity);
        System.out.println("tx send data:" + jsonEntity);
        ReturnMsgEntity msgEntity = elaReqChainData(ReqMethod.POST, didPreFix + transaction + "/" + type.toString(), jsonEntity);
        return msgEntity;
    }

    public static Map<String, Object> getTransaction(String txId, ChainType type) {
        ReturnMsgEntity msgEntity = elaReqChainData(ReqMethod.GET, didPreFix + transaction + "/" + type.toString() + "/" + txId, null);

        if (msgEntity.getStatus() == RetCodeConfiguration.SUCC) {
            return (Map<String, Object>) msgEntity.getResult();
        } else {
            return null;
        }
    }

    private static ReturnMsgEntity elaReqChainData(ReqMethod method, String url, String data) {
        String response;
        if (ReqMethod.GET == method) {
            String str = url;
            if (null != data) {
                str += "?" + data;
            }
            response = HttpKit.get(str);
        } else {
//            response = HttpKit.post("http://localhost:21334/api/v1/transaction", data);
            try {
                response = DidBackendService.requestOnce(url, data);
            } catch (Exception e) {
                e.printStackTrace();
                return new ReturnMsgEntity().setResult("Err: post data to server").setStatus(RetCodeConfiguration.INTERNAL_ERROR);
            }
        }

        Map<String, Object> msg = (Map<String, Object>) JSON.parse(response);
        long status;
        if ((int) msg.get("Error") == 0) {
            status = RetCodeConfiguration.SUCC;
        } else {
            status = RetCodeConfiguration.PROCESS_ERROR;
        }
        return new ReturnMsgEntity().setResult(msg.get("Result")).setStatus(status);
    }

    private static String requestOnce(String url, String data) throws Exception {
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

        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connManager)
                .build();

        HttpPost httpPost = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(20000).setConnectTimeout(20000).build();
        httpPost.setConfig(requestConfig);

        StringEntity postEntity = new StringEntity(data, "UTF-8");
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(postEntity);

        HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity httpEntity = httpResponse.getEntity();
        return EntityUtils.toString(httpEntity, "UTF-8");

    }
}
