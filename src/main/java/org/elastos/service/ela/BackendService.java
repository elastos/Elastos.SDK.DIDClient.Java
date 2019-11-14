/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.service.ela;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.elastos.constant.RetCode;
import org.elastos.entity.Errors;
import org.elastos.exception.ApiRequestDataException;
import org.elastos.util.HttpUtil;
import org.elastos.util.RetResult;
import org.elastos.util.ela.ElaKit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackendService {
    private String prefix = null;
    private boolean testNet = false;

    private final static String getUtosByAddr = "/api/v1/asset/utxos/";
    private final static String transaction = "/api/v1/transaction";
    private final static String  balance= "/api/v1/asset/balances";

//    {
//        boolean ret = OutSideConfig.readOutSide();
//        if (ret) {
//            prefix = OutSideConfig.getObject("node.prefix");
//        }
//    }

    public void setPrefix(String inPrefix) {
        this.prefix = inPrefix;
    }

    public void setTestNet(boolean isTestNet) {
       testNet = isTestNet;
    }

    public boolean isTestNet() {
        return testNet;
    }

    public String getPrefix() {
        if (null == prefix) {
            String msg = "There is no ela chain node url, please call BackendService::setPrefix first";
            System.out.println(msg);
            throw new NullPointerException(msg);
        }
        return prefix;
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
    private void checkAddr(String address) {
        if (!ElaKit.checkAddress(address)) {
            throw new ApiRequestDataException(Errors.ELA_ADDRESS_INVALID.val() + ":" + address);
        }
    }

    public RetResult<List<Map>> getUtxoListByAddr(String address) {

        checkAddr(address);

        RetResult msgEntity = elaReqChainData(ReqMethod.GET, getPrefix() + getUtosByAddr, address);
        if (msgEntity.getCode() == RetCode.SUCC) {
            try {
                List<Map> data = (List<Map>) msgEntity.getData();
                List<Map> utxoList = (List<Map>) data.get(0).get("Utxo");

                return RetResult.retOk(utxoList);
            } catch (Exception ex) {
                ex.printStackTrace();
                return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, ex.getMessage());
            }
        } else {
            return msgEntity;
        }
    }

    public RetResult<String> sendRawTransaction(String rawTx) {

        Map<String, String> entity = new HashMap<>();
        entity.put("method", "sendrawtransaction");
        entity.put("data", rawTx);

        String jsonEntity = JSON.toJSONString(entity);
        System.out.println("tx send data:" + jsonEntity);
        RetResult<String> msgEntity = elaReqChainData(ReqMethod.POST, getPrefix() + transaction, jsonEntity);
        return msgEntity;
    }

    public RetResult<Double> getBalance(String address) {
         RetResult<Double> ret = elaReqChainData(ReqMethod.GET,getPrefix() + balance + "/" + address, null);
         return ret;
    }

    public Map<String, Object> getTransaction(String txId) {
        RetResult msgEntity = elaReqChainData(ReqMethod.GET, getPrefix() + transaction + "/", txId);

        if (msgEntity.getCode() == RetCode.SUCC) {
            return (Map<String, Object>) msgEntity.getData();
        } else {
            return null;
        }
    }

    private <T> RetResult<T> elaReqChainData(ReqMethod method, String url, String data) {
        String response;

        if (ReqMethod.GET == method) {
            String str = url;
            if (null != data) {
                str += data;
            }
            response = HttpUtil.get(str, null);
        } else {
            response = HttpUtil.post(url, data, null);
        }

        if (null == response) {
            return  RetResult.retErr(RetCode.NETWORK_FAILED, "http failed");
        }

        JSONObject msg =  JSON.parseObject(response);
        int err = msg.getInteger("Error");
        if (err == 0) {
            return RetResult.retOk((T)msg.get("Result"));
        } else {
            return RetResult.retErr(RetCode.RESPONSE_ERROR, "Error:"+err);
        }
    }
}
