/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.service.ela;

import com.alibaba.fastjson.JSON;
import org.elastos.conf.RetCodeConfiguration;
import org.elastos.entity.ChainType;
import org.elastos.entity.Errors;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.exception.ApiRequestDataException;
import org.elastos.util.HttpKit;
import org.elastos.util.ela.ElaKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DidBackendService {
    private static Logger logger = LoggerFactory.getLogger(DidBackendService.class);
    private String didPrefix = null;

    private final String getUtosByAddr = "/api/v1/asset/utxos/";
    private final String transaction = "/api/v1/transaction";

//    {
//        boolean ret = OutSideConfig.readOutSide();
//        if (ret) {
//            didPrefix = OutSideConfig.getObject("node.prefix");
//        }
//    }

    public void setDidPrefix(String didPrefix) {
        this.didPrefix = didPrefix;
    }

    public String getDidPrefix() {
        if (null == didPrefix) {
            String msg = "There is no ela chain node url, please call DidBackendService::setDidPrefix first";
            System.out.println(msg);
            throw new NullPointerException(msg);
        }
        return didPrefix;
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

    public List<Map> getUtxoListByAddr(String address) {

        checkAddr(address);

        ReturnMsgEntity msgEntity = elaReqChainData(ReqMethod.GET, getDidPrefix() + getUtosByAddr, address);
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

    public ReturnMsgEntity sendRawTransaction(String rawTx) {

        Map<String, String> entity = new HashMap<>();
        entity.put("method", "sendrawtransaction");
        entity.put("data", rawTx);

        String jsonEntity = JSON.toJSONString(entity);
        System.out.println("tx send data:" + jsonEntity);
        ReturnMsgEntity msgEntity = elaReqChainData(ReqMethod.POST, getDidPrefix() + transaction, jsonEntity);
        return msgEntity;
    }

    public Map<String, Object> getTransaction(String txId, ChainType type) {
        ReturnMsgEntity msgEntity = elaReqChainData(ReqMethod.GET, getDidPrefix() + transaction + "/", txId);

        if (msgEntity.getStatus() == RetCodeConfiguration.SUCC) {
            return (Map<String, Object>) msgEntity.getResult();
        } else {
            return null;
        }
    }

    private ReturnMsgEntity elaReqChainData(ReqMethod method, String url, String data) {
        String response;

        if (ReqMethod.GET == method) {
            String str = url;
            if (null != data) {
                str += data;
            }
            response = HttpKit.get(str);
        } else {
            response = HttpKit.post(url, data);
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
}
