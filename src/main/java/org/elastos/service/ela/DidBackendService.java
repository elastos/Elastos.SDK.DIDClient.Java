/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.service.ela;

import com.alibaba.fastjson.JSON;
import org.elastos.conf.DependServiceConfiguration;
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
    private static String didPreFix = DependServiceConfiguration.NODE_URL_PRIFIX;

    private static final String getUtosByAddr = "/api/v1/asset/utxos/";
    private static final String transaction = "/api/v1/transaction";

    static {
        boolean ret = OutSideConfig.readOutSide();
        if (ret) {
            didPreFix = OutSideConfig.getObject("node.prefix");
        }
    }

    public static void setDidPreFix(String didPreFix) {
        DidBackendService.didPreFix = didPreFix;
    }

    public static String getDidPreFix() {
        if (null == didPreFix) {
            String msg = "There is no ela chain node url, please set it with API setElaNodeUrl or configuration file in \"./conf/ela.did.properties\".";
            System.out.println(msg);
            throw new NullPointerException(msg);
        }
        return didPreFix;
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

        ReturnMsgEntity msgEntity = elaReqChainData(ReqMethod.GET, getDidPreFix()+ getUtosByAddr, address);
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
        ReturnMsgEntity msgEntity = elaReqChainData(ReqMethod.POST, getDidPreFix()+ transaction, jsonEntity);
        return msgEntity;
    }

    public static Map<String, Object> getTransaction(String txId, ChainType type) {
        ReturnMsgEntity msgEntity = elaReqChainData(ReqMethod.GET, getDidPreFix()+ transaction+"/", txId);

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
