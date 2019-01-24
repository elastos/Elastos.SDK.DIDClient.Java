/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.service.ela;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
import org.eclipse.jetty.util.StringUtil;
import org.elastos.api.SingleSignTransaction;
import org.elastos.conf.BasicConfiguration;
import org.elastos.conf.DidConfiguration;
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
        ChainType chainType;

        Sender(String addr, String priKey, ChainType type) {
            address = addr;
            privateKey = priKey;
            chainType = type;
            utxoList = DidBackendService.getUtxoListByAddr(address, type);
            if (utxoList == null) {
                throw new ApiRequestDataException(Errors.NOT_ENOUGH_UTXO.val());
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
    private List<Sender> senderList = new ArrayList<>();
    private List<Receiver> receiverList = new ArrayList<>();
    private String memo = null;
    private ChainType chainType = null;
    private Double totalFee = null;

    public ElaTransaction(ChainType type, String memoInput) {
        chainType = type;
        memo = memoInput;
        totalFee = 0.0;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public ChainType getChainType() {
        return chainType;
    }

    public void setChainType(ChainType chainType) {
        this.chainType = chainType;
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
        this.senderList.add(new Sender(addr, priKey, chainType));
    }

    public void addReceiver(String addr, double fee) {
        this.receiverList.add(new Receiver(addr, fee));
        totalFee += fee;
    }

    public ReturnMsgEntity transfer() throws Exception {

        if (receiverList.isEmpty()
                || senderList.isEmpty()
                || null == memo
                || null == chainType
                || null == totalFee) {
            throw new RuntimeException("Not enough transaction parameter");
        }

        String txJson = createRawTx();
        JSONObject ob = JSONObject.fromObject(txJson);
        Object txData = ob.get("Result");
//        Object txData = ((Map<String, Object>) JSON.parse(txJson)).get("Result");
        if ((txData instanceof Map) == false) {

            throw new ApiRequestDataException("Not valid Data to create raw tx");
        }
        Map<String, Object> tx = (Map<String, Object>) txData;
        String rawTx = (String) tx.get("rawTx");
        String txHash = (String) tx.get("txHash");
        logger.info("rawTx:" + rawTx + ", txHash :" + txHash);

        return DidBackendService.sendRawTransaction(rawTx, chainType);
    }

    /**
     * send a transaction to blockchain.
     *
     * @return
     * @throws Exception
     */
    private String createRawTx() throws Exception {

        if ((chainType == ChainType.MAIN_DID_CROSS_CHAIN) || (chainType == ChainType.DID_MAIN_CROSS_CHAIN)) {
            return createCrossChainRawTx();
        } else if ((chainType == ChainType.MAIN_CHAIN) || (chainType == ChainType.DID_SIDECHAIN)) {
            return createSameChainRawTx();
        } else {
            throw new ApiRequestDataException("no such transfer type");
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
        System.out.println("createCrossChainRawTx sending:"+par.toString());
        String rawTx = SingleSignTransaction.genCrossChainRawTransaction(par);
        logger.info("receiving : " + rawTx);
        System.out.println("createCrossChainRawTx receiving:"+rawTx);

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

    private void dealCrossChainTxOutputs(Map<String, Object> tx, double spendMoney) {
        List utxoOutputsArray = new ArrayList<>();
        Map<String, Object> brokerOutputs = new HashMap<>();
        if (chainType == ChainType.MAIN_DID_CROSS_CHAIN) {
            brokerOutputs.put("address", DidConfiguration.MAIN_CHAIN_ADDRESS);
        } else if (chainType == ChainType.DID_MAIN_CROSS_CHAIN) {
            brokerOutputs.put("address", DidConfiguration.BURN_ADDRESS);
        } else {
            throw new ApiRequestDataException("no such transfer type");
        }
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
        System.out.println("createSameChainRawTx sending:"+par.toString());
        String rawTx = ElaKit.genRawTransaction(par);
        logger.info("receiving : " + rawTx);
        System.out.println("createSameChainRawTx receiving:"+rawTx);

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
