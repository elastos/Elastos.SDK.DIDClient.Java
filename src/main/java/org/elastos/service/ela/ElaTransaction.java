/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.service.ela;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.StringUtil;
import org.elastos.POJO.ElaChainType;
import org.elastos.api.SingleSignTransaction;
import org.elastos.conf.BasicConfiguration;
import org.elastos.conf.DidConfiguration;
import org.elastos.conf.EthConfiguration;
import org.elastos.entity.ChainType;
import org.elastos.entity.Errors;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.exception.ApiRequestDataException;
import org.elastos.util.ela.ElaKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElaTransaction {
    private class Sender {
        String address;
        String privateKey;
        List<Map> utxoList;

        Sender(String addr, String priKey) {
            address = addr;
            privateKey = priKey;
            utxoList = didBackendService.getUtxoListByAddr(address);
            if (utxoList == null) {
                utxoList = new ArrayList<>();
            }
        }
    }

    private class Receiver {
        String address;
        double fee;

        Receiver(String addr, double cost) {
            address = addr;
            fee = cost;
        }
    }

    private static Logger logger = LoggerFactory.getLogger(ElaTransaction.class);
    private DidBackendService didBackendService = new DidBackendService();
    private List<Sender> senderList = new ArrayList<>();
    private List<Receiver> receiverList = new ArrayList<>();
    private String memo = "";
    private Double totalFee = 0.0;
    private ChainType chainType = null;
    private ElaChainType srcChainType = null;
    private ElaChainType dstChainType = null;

    public void setChainInfo(String nodeUrl, ElaChainType srcChain, ElaChainType dstChain) {
        didBackendService.setDidPrefix(nodeUrl);
        srcChainType = srcChain;
        dstChainType = dstChain;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getSenderList() {
        return JSON.toJSONString(senderList);
    }

    public String getReceiverList() {
        return JSON.toJSONString(receiverList);
    }

    public double getTotalFee() {
        return totalFee;
    }

    public void addSender(String addr, String priKey) {
        this.senderList.add(new Sender(addr, priKey));
    }

    public void addReceiver(String addr, double fee) {
        this.receiverList.add(new Receiver(addr, fee));
        totalFee += fee;
    }

    public ReturnMsgEntity transfer() throws Exception {

        if (receiverList.isEmpty()
                || senderList.isEmpty()
                || StringUtils.isBlank(didBackendService.getDidPrefix())
                || (null == srcChainType)
                || (null == dstChainType)) {
            throw new RuntimeException("Not enough transaction parameter");
        }

        String txJson = createRawTx();
        JSONObject ob = JSONObject.fromObject(txJson);
        Object txData = ob.get("Result");
        if ((txData instanceof Map) == false) {

            throw new ApiRequestDataException("Not valid Data to create raw tx");
        }
        Map<String, Object> tx = (Map<String, Object>) txData;
        String rawTx = (String) tx.get("rawTx");
        String txHash = (String) tx.get("txHash");
        logger.info("rawTx:" + rawTx + ", txHash :" + txHash);

        return didBackendService.sendRawTransaction(rawTx);
    }

    private boolean isSupportSameChain(ElaChainType srcChain, ElaChainType dstChain) {
        if (srcChain == dstChain) {
            if((srcChain == ElaChainType.ETH_CHAIN)
                ||(srcChain == ElaChainType.ETH_TESTCHAIN)){
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * send a transaction to blockchain.
     *
     * @return
     * @throws Exception
     */
    private String createRawTx() throws Exception {

        if (isSupportSameChain(srcChainType, dstChainType)) {
            return createSameChainRawTx();
        } else {
            return createCrossChainRawTx();
        }

    }

    private String createCrossChainRawTx() {
        Map<String, Object> tx = new HashMap<>();
        Double spendMoney = dealCrossChainTxInputs(tx);
        if (null == spendMoney) {
            //All utxo have not enough money to pay tx.
            throw new ApiRequestDataException(Errors.NOT_ENOUGH_UTXO.val());
        }
        dealCrossChainTxOutputs(tx, spendMoney);

        List txList = new ArrayList<>();
        txList.add(tx);
        Map<String, Object> paraListMap = new HashMap<>();
        paraListMap.put("Transactions", txList);
        JSONObject par = new JSONObject();
        par.accumulateAll(paraListMap);
        logger.info("sending : " + par.toString());
        System.out.println("createCrossChainRawTx sending:" + par.toString());
        String rawTx = SingleSignTransaction.genCrossChainRawTransaction(par);
        logger.info("receiving : " + rawTx);
        System.out.println("createCrossChainRawTx receiving:" + rawTx);

        return rawTx;
    }

    private Double dealCrossChainTxInputs(Map<String, Object> tx) {
        List utxoInputsArray = new ArrayList<>();
        List privsArray = new ArrayList<>();
        double spendMoney = 0.0;
        for (Sender sender : senderList) {
            for (Map<String, Object> utxo : sender.utxoList) {
                Map<String, Object> utxoInputsDetail = new HashMap<>();
                utxoInputsDetail.put("txid", utxo.get("Txid"));
                utxoInputsDetail.put("index", utxo.get("Index"));
                utxoInputsDetail.put("address", sender.address);
                utxoInputsArray.add(utxoInputsDetail);

                Map<String, Object> privM = new HashMap<>();
                privM.put("privateKey", sender.privateKey);
                privsArray.add(privM);

                spendMoney += Double.valueOf(utxo.get("Value") + "");

                //If there is enough money to deal the tx, we done.
                if (Math.round(spendMoney * BasicConfiguration.ONE_ELA)
                        >= Math.round((totalFee + (BasicConfiguration.CROSS_CHAIN_FEE * 2)) * BasicConfiguration.ONE_ELA)) {
                    tx.put("UTXOInputs", utxoInputsArray);
                    tx.put("PrivateKeySign", privsArray);
                    return spendMoney;
                }
            }
        }

        //There is not enough money to deal the tx.
        return null;
    }

    String getBrokerAddr(ElaChainType srcChain, ElaChainType dstChain) {
        if ((ElaChainType.ELA_CHAIN == srcChain)
                && (ElaChainType.DID_CHAIN == dstChain)) {
            return DidConfiguration.ELA_MAIN_CHAIN_ADDRESS;
        } else if ((ElaChainType.DID_CHAIN == srcChain)
                && (ElaChainType.ELA_CHAIN == dstChain)) {
            return DidConfiguration.DID_SIDE_CHAIN_BURN_ADDRESS;
        } else if ((ElaChainType.ELA_TESTCHAIN == srcChain)
                && (ElaChainType.DID_TESTCHAIN == dstChain)) {
            return DidConfiguration.ELA_MAIN_CHAIN_ADDRESS;
        } else if ((ElaChainType.DID_TESTCHAIN == srcChain)
                && (ElaChainType.ELA_TESTCHAIN == dstChain)) {
            return DidConfiguration.DID_SIDE_CHAIN_BURN_ADDRESS;
        } else if ((ElaChainType.ELA_CHAIN == srcChain)
                && (ElaChainType.ETH_CHAIN == dstChain)) {
            return EthConfiguration.ELA_MAIN_CHAIN_ADDRESS;
        } else if ((ElaChainType.ETH_CHAIN == srcChain)
                && (ElaChainType.ELA_CHAIN == dstChain)) {
            return EthConfiguration.ETH_SIDE_CHAIN_BURN_ADDRESS;
        } else if ((ElaChainType.ELA_TESTCHAIN == srcChain)
                && (ElaChainType.ETH_TESTCHAIN == dstChain)) {
            return EthConfiguration.ELA_MAIN_TESTCHAIN_ADDRESS;
        } else if ((ElaChainType.ETH_TESTCHAIN == srcChain)
                && (ElaChainType.ELA_TESTCHAIN == dstChain)) {
            return EthConfiguration.ETH_SIDE_TESTCHAIN_BURN_ADDRESS;
        } else {
            throw new ApiRequestDataException("no such transfer type");
        }
    }

    private void dealCrossChainTxOutputs(Map<String, Object> tx, double spendMoney) {
        List utxoOutputsArray = new ArrayList<>();
        Map<String, Object> brokerOutputs = new HashMap<>();
        String brokerAddr = getBrokerAddr(srcChainType, dstChainType);
        brokerOutputs.put("address", brokerAddr);
        brokerOutputs.put("amount", Math.round((totalFee + BasicConfiguration.CROSS_CHAIN_FEE) * BasicConfiguration.ONE_ELA));
        utxoOutputsArray.add(brokerOutputs);

        double leftMoney = (spendMoney - ((BasicConfiguration.CROSS_CHAIN_FEE * 2) + totalFee));
        String changeAddr = senderList.get(0).address;
        Map<String, Object> utxoOutputsDetail = new HashMap<>();
        utxoOutputsDetail.put("address", changeAddr);
        utxoOutputsDetail.put("amount", Math.round(leftMoney * BasicConfiguration.ONE_ELA));
        utxoOutputsArray.add(utxoOutputsDetail);

        List crossOutputsArray = new ArrayList<>();
        for (Receiver receiver : receiverList) {
            utxoOutputsDetail = new HashMap<>();
            utxoOutputsDetail.put("address", receiver.address);
            utxoOutputsDetail.put("amount", Math.round(receiver.fee * BasicConfiguration.ONE_ELA));
            crossOutputsArray.add(utxoOutputsDetail);
        }
        tx.put("CrossChainAsset", crossOutputsArray);
        tx.put("Outputs", utxoOutputsArray);
    }

    private String createSameChainRawTx() {
        Map<String, Object> tx = new HashMap<>();
        Double spendMoney = dealSameChainTxInputs(tx);
        if (null == spendMoney) {
            //All utxo have not enough money to pay tx.
            throw new ApiRequestDataException(Errors.NOT_ENOUGH_UTXO.val());
        }
        dealSameChainTxOutputs(tx, spendMoney);


        List txList = new ArrayList<>();
        txList.add(tx);
        Map<String, Object> paraListMap = new HashMap<>();
        paraListMap.put("Transactions", txList);
        JSONObject par = new JSONObject();
        par.accumulateAll(paraListMap);
        logger.info("sending : " + par);
        System.out.println("createSameChainRawTx sending:" + par.toString());
        String rawTx = ElaKit.genRawTransaction(par);
        logger.info("receiving : " + rawTx);
        System.out.println("createSameChainRawTx receiving:" + rawTx);

        return rawTx;
    }

    private Double dealSameChainTxInputs(Map<String, Object> tx) {
        List utxoInputsArray = new ArrayList<>();
        double spendMoney = 0.0;
        for (Sender sender : senderList) {
            for (Map<String, Object> utxo : sender.utxoList) {
                Map<String, Object> utxoInputsDetail = new HashMap<>();
                utxoInputsDetail.put("txid", utxo.get("Txid"));
                utxoInputsDetail.put("index", utxo.get("Index"));
                utxoInputsDetail.put("address", sender.address);
                utxoInputsDetail.put("privateKey", sender.privateKey);
                utxoInputsArray.add(utxoInputsDetail);


                spendMoney += Double.valueOf(utxo.get("Value") + "");
                //If there is enough money to deal the tx, we done.
                if (Math.round(spendMoney * BasicConfiguration.ONE_ELA)
                        >= Math.round((totalFee + BasicConfiguration.FEE) * BasicConfiguration.ONE_ELA)) {
                    if (StringUtil.isNotBlank(memo)) {
                        tx.put("Memo", memo);
                    }
                    tx.put("UTXOInputs", utxoInputsArray);
                    return spendMoney;
                }
            }
        }

        //There is not enough money to deal the tx.
        return null;
    }

    private void dealSameChainTxOutputs(Map<String, Object> tx, double spendMoney) {
        List utxoOutputsArray = new ArrayList<>();

        double leftMoney = (spendMoney - (BasicConfiguration.FEE + totalFee));
        String changeAddr = senderList.get(0).address;
        Map<String, Object> utxoOutputsDetail = new HashMap<>();
        utxoOutputsDetail.put("address", changeAddr);
        utxoOutputsDetail.put("amount", Math.round(leftMoney * BasicConfiguration.ONE_ELA));
        utxoOutputsArray.add(utxoOutputsDetail);

        for (Receiver receiver : receiverList) {
            utxoOutputsDetail = new HashMap<>();
            utxoOutputsDetail.put("address", receiver.address);
            utxoOutputsDetail.put("amount", Math.round(receiver.fee * BasicConfiguration.ONE_ELA));
            utxoOutputsArray.add(utxoOutputsDetail);
        }
        tx.put("Outputs", utxoOutputsArray);
    }


}
