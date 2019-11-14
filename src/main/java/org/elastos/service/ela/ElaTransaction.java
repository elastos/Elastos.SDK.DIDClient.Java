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
import org.elastos.constant.RetCode;
import org.elastos.entity.Errors;
import org.elastos.exception.ApiRequestDataException;
import org.elastos.exception.ElaDidServiceException;
import org.elastos.util.RetResult;
import org.elastos.util.ela.ElaKit;

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
        }

        RetResult<List<Map>> getUtxo() {
            RetResult<List<Map>> utxoRet = backendService.getUtxoListByAddr(address);
            if (utxoRet.getCode() == RetCode.SUCC) {
                utxoList = utxoRet.getData();
            }
            return utxoRet;
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

    private List<Sender> senderList = new ArrayList<>();
    private List<Receiver> receiverList = new ArrayList<>();
    private String memo = "";
    private Double totalValue = 0.0;
    private ElaChainType srcChainType = null;
    private ElaChainType dstChainType = null;
    private BackendService backendService;

    public static Double countFee(ElaChainType srcChainType, ElaChainType dstChainType) {
        //The ela chain can transfer to all side chain, but side chain can only transfer to ela chain.
        if (srcChainType != ElaChainType.ELA_CHAIN) {
            if ((dstChainType != srcChainType) && (dstChainType != ElaChainType.ELA_CHAIN)) {
                return null;
            }
        }

        if (srcChainType == dstChainType) {
            return BasicConfiguration.FEE;
        } else {
            return BasicConfiguration.CROSS_CHAIN_FEE * 2 * BasicConfiguration.ONE_ELA;
        }
    }

    public ElaTransaction(BackendService backendService, ElaChainType srcChain, ElaChainType dstChain) {
        if (ElaChainType.ETH_CHAIN == srcChainType) {
            throw new ElaDidServiceException("Err:Not support transfer source chain:" + srcChainType.toString());
        }
        srcChainType = srcChain;
        dstChainType = dstChain;
        this.backendService = backendService;
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

    public double getTotalValue() {
        return totalValue;
    }

    public RetResult addSender(String addr, String priKey) {
        Sender sender = new Sender(addr, priKey);
        RetResult ret = sender.getUtxo();
        if (ret.getCode() == RetCode.SUCC) {
            this.senderList.add(sender);
        }
        return ret;
    }

    public void addReceiver(String addr, double value) {
        this.receiverList.add(new Receiver(addr, value));
        totalValue += value;
    }


    public RetResult<String> transfer() throws ApiRequestDataException {
        if (receiverList.isEmpty()
                || senderList.isEmpty()
                || StringUtils.isBlank(backendService.getPrefix())
                || (null == srcChainType)
                || (null == dstChainType)) {
            return null;
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
        System.out.println("rawTx:" + rawTx + ", txHash :" + txHash);

        RetResult<String> ret = backendService.sendRawTransaction(rawTx);
        return ret;
    }

    private boolean isSameChainProc(ElaChainType srcChain, ElaChainType dstChain) {
        if (srcChain == dstChain) {
            return true;
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
    private String createRawTx() throws ApiRequestDataException {
        if (isSameChainProc(srcChainType, dstChainType)) {
            return createSameChainRawTx();
        } else {
            return createCrossChainRawTx();
        }

    }

    private String createCrossChainRawTx() throws ApiRequestDataException {
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
        System.out.println("createCrossChainRawTx sending:" + par.toString());
        String rawTx = SingleSignTransaction.genCrossChainRawTransaction(par);
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
                        >= Math.round((totalValue + (BasicConfiguration.CROSS_CHAIN_FEE * 2)) * BasicConfiguration.ONE_ELA)) {
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
        } else if ((ElaChainType.ELA_CHAIN == srcChain)
                && (ElaChainType.ETH_CHAIN == dstChain)) {
            if (backendService.isTestNet()) {
                return EthConfiguration.ELA_MAIN_TESTCHAIN_ADDRESS;
            } else {
                return EthConfiguration.ELA_MAIN_CHAIN_ADDRESS;
            }
        } else {
            throw new ApiRequestDataException("Err getBrokerAddr not support such transfer type");
        }
    }

    private void dealCrossChainTxOutputs(Map<String, Object> tx, double spendMoney) {
        List utxoOutputsArray = new ArrayList<>();
        Map<String, Object> brokerOutputs = new HashMap<>();
        String brokerAddr = getBrokerAddr(srcChainType, dstChainType);
        brokerOutputs.put("address", brokerAddr);
        brokerOutputs.put("amount", Math.round((totalValue + BasicConfiguration.CROSS_CHAIN_FEE) * BasicConfiguration.ONE_ELA));
        utxoOutputsArray.add(brokerOutputs);

        double leftMoney = (spendMoney - ((BasicConfiguration.CROSS_CHAIN_FEE * 2) + totalValue));
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

    private String createSameChainRawTx() throws ApiRequestDataException {
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
        System.out.println("createSameChainRawTx sending:" + par.toString());
        String rawTx = ElaKit.genRawTransaction(par);
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
                        >= Math.round((totalValue + BasicConfiguration.FEE) * BasicConfiguration.ONE_ELA)) {
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

        double leftMoney = (spendMoney - (BasicConfiguration.FEE + totalValue));
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
