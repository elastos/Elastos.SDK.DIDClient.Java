/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.elastos.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.eclipse.jetty.util.StringUtil;
import org.elastos.POJO.DidEntity;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.DidConfiguration;
import org.elastos.conf.RetCodeConfiguration;
import org.elastos.ela.ECKey;
import org.elastos.ela.Ela;
import org.elastos.ela.SignTool;
import org.elastos.ela.Util;
import org.elastos.entity.ChainType;
import org.elastos.entity.Errors;
import org.elastos.entity.MnemonicType;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.service.ela.DidBackendService;
import org.elastos.service.ela.ElaTransaction;
import org.elastos.util.HttpUtil;
import org.elastos.util.ela.ElaHdSupport;
import org.elastos.util.ela.ElaKit;
import org.elastos.util.ela.ElaSignTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.CipherException;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public class ElaDidService {
    private static final String CHARSET = "UTF-8";

    private static Logger logger = LoggerFactory.getLogger(ElaDidService.class);

    /**
     * Create mnemonic to generate did
     *
     * @return mnemonic
     */
    public String createMnemonic() {
        String mnemonic = ElaHdSupport.generateMnemonic(MnemonicType.ENGLISH);
        return mnemonic;
    }

    /**
     * @param mnemonic
     * @param index
     * @return Json string of did data: DidPrivateKey, DidPublicKey, Did
     */
    public String createDid(String mnemonic, int index) {
        String ret;
        try {
            ret = ElaHdSupport.generate(mnemonic, index);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | CipherException e) {
            logger.error("Err: signDidMessage Parameter invalid.");
            e.printStackTrace();
            return null;
        }

        Map data = JSON.parseObject(ret, Map.class);
        String privateKey = (String) data.get("privateKey");
        String publicKey = (String) data.get("publicKey");
        String did = Ela.getIdentityIDFromPrivate(privateKey);

        Map<String, String> result = new HashMap<>();
        result.put("DidPrivateKey", privateKey);
        result.put("Did", did);
        result.put("DidPublicKey", publicKey);

        return JSON.toJSONString(result);
    }

    /**
     * Get a did signature of a message
     *
     * @param didPrivateKey
     * @param msg
     * @return signature
     */
    public String signMessage(String didPrivateKey, String msg) {
        if (StringUtils.isAnyBlank(didPrivateKey,
                msg)) {
            logger.error("Err: signDidMessage Parameter invalid.");
            return null;
        }

        Map<String, Object> signDid;
        try {
            signDid = this.sign(didPrivateKey, msg);
        } catch (Exception e) {
            logger.error("Err: signDidMessage failed.");
            return null;

        }
        logger.debug("signDidMessage signDid:{}", JSON.toJSONString(signDid));
        String sig = (String) signDid.get("sig");
        return sig;
    }

    /**
     * Verify a did signature
     *
     * @param didPublicKey
     * @param sig
     * @param msg
     * @return bool
     */
    public boolean verifyMessage(String didPublicKey, String sig, String msg) {
        if (StringUtils.isAnyBlank(didPublicKey,
                msg,
                sig)) {
            logger.error("Err: verifyMessage parameter invalid");
            return false;
        }

        try {
            String hexmsg = DatatypeConverter.printHexBinary(msg.getBytes(CHARSET));
            boolean ret = this.verify(didPublicKey, sig, hexmsg);
            return ret;
        } catch (Exception e) {
            logger.error("Err: verifyDidMessage failed.");
            return false;
        }
    }

    /**
     * Get did public key by did private key
     *
     * @param didPrivateKey
     * @return didPublicKey
     */
    public String getDidPublicKey(String didPrivateKey) {
        if (StringUtils.isBlank(didPrivateKey)) {
            logger.error("Err: getDidPublicKey Parameter invalid.");
            return null;
        }

        String didPublicKey = Ela.getPublicFromPrivate(didPrivateKey);
        System.out.println("getDidPublicKey:" + didPublicKey);
        return didPublicKey;
    }

    /**
     * Get did
     *
     * @param didPrivateKey
     * @return did
     */
    public String getDidFromPrivateKey(String didPrivateKey) {
        if (StringUtils.isBlank(didPrivateKey)) {
            return null;
        }
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);
        System.out.println("getDid:" + did);
        return did;
    }

    /**
     * Get did
     *
     * @param publicKey
     * @return did
     */
    public String getDidFromPublicKey(String publicKey) {
        if (StringUtils.isBlank(publicKey)) {
            return null;
        }
        String did = ElaKit.getIdentityFromPublicKey(publicKey);
        return did;
    }

    /**
     * Make did property to raw data for up chain
     *
     * @param didPrivateKey
     * @param propertyKey
     * @param propertyValue
     * @return raw data for up chain
     */
    public String packDidProperty(String didPrivateKey, String propertyKey, String propertyValue) {
        if (StringUtils.isAnyBlank(
                didPrivateKey,
                propertyKey,
                propertyValue)) {
            logger.error("Err: packDidProperty parameter invalid");
            return null;
        }

        DidEntity.DidProperty property = new DidEntity.DidProperty();
        property.setKey(propertyKey);
        property.setValue(propertyValue);
        List<DidEntity.DidProperty> properties = new ArrayList<>();
        properties.add(property);
        DidEntity didEntity = new DidEntity();
        didEntity.setProperties(properties);
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);
        didEntity.setDid(did);
        String raw = packDidEntity(didPrivateKey, didEntity);
        if (null == raw) {
            logger.error("Err: packDidProperty packDidEntity failed");
            System.out.println("Err: packDidProperty packDidEntity failed");
        }
        return raw;
    }


    /**
     * Make did properties to raw data for up chain
     *
     * @param didPrivateKey
     * @param propertiesMap
     * @return raw data for up chain
     */
    public String packDidProperties(String didPrivateKey, Map<String, String> propertiesMap) {
        if (StringUtils.isBlank(didPrivateKey)
                || (null == propertiesMap)
                || (propertiesMap.isEmpty())) {
            logger.error("Err: packDidProperty parameter invalid");
            System.out.println("Err: packDidProperty parameter invalid");
            return null;
        }

        List<DidEntity.DidProperty> properties = new ArrayList<>();
        propertiesMapToList(propertiesMap, properties);
        DidEntity didEntity = new DidEntity();
        didEntity.setProperties(properties);
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);
        didEntity.setDid(did);
        String raw = packDidEntity(didPrivateKey, didEntity);
        if (null == raw) {
            logger.error("Err: packDidProperty packDidEntity failed");
            System.out.println("Err: packDidProperty packDidEntity failed");
        }
        return raw;
    }

    /**
     * Generate a raw data for delete did property
     *
     * @param didPrivateKey
     * @param propertyKey
     * @return
     */
    public String packDelDidProperty(String didPrivateKey, String propertyKey) {
        if (StringUtils.isAnyBlank(
                didPrivateKey,
                propertyKey)) {
            logger.error("Err: packDelDidProperty parameter invalid");
            return null;
        }

        //Create rawData
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);

        DidEntity.DidProperty property = new DidEntity.DidProperty();
        property.setKey(propertyKey);
        property.setStatus(DidEntity.DidStatus.Deprecated);

        DidEntity didEntity = new DidEntity();
        List<DidEntity.DidProperty> properties = new ArrayList<>();
        properties.add(property);
        didEntity.setProperties(properties);
        didEntity.setDid(did);
        String rawData = packDidEntity(didPrivateKey, didEntity);
        return rawData;
    }

    /**
     * Generate a up chain raw data for deprecate did
     *
     * @param didPrivateKey
     * @return raw date
     */
    public String packDestroyDid(String didPrivateKey) {
        if (StringUtils.isBlank(didPrivateKey)) {
            return null;
        }

        //Create rawData
        DidEntity didEntity = new DidEntity();
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);
        didEntity.setDid(did);
        didEntity.setStatus(DidEntity.DidStatus.Deprecated);
        String rawData = packDidEntity(didPrivateKey, didEntity);
        return rawData;
    }

    /**
     * Send raw data to did chain by block chain agent service
     *
     * @param agentUrl
     * @param accessId     If there is no auth agent, it is null
     * @param accessSecret If there is no auth agent, it is null
     * @param rawData
     * @return txid
     */
    public String upChainByAgent(String agentUrl, String accessId, String accessSecret, String rawData) {
        Map<String, String> header = new HashMap<>();
        if (!StringUtils.isAnyBlank(accessId, accessSecret)) {
            header.put("X-Elastos-Agent-Auth", createAuthHeader(accessId, accessSecret));
        }
        String response = HttpUtil.post(agentUrl + "/api/1/blockagent/upchain/data", rawData, header);
        if (null == response) {
            System.out.println("Err: putDataToElaChain post failed");
            return null;
        }

        Map<String, Object> msg = (Map<String, Object>) JSON.parse(response);
        if ((int) msg.get("status") == 200) {
            return (String) msg.get("result");
        } else {
            System.out.println("Err: block agent failed" + msg.get("result"));
            return null;
        }
    }

    private String createAuthHeader(String acc_id, String acc_secret) {
        long time = new Date().getTime();
        String strTime = String.valueOf(time);
        SimpleHash hash = new SimpleHash("md5", acc_secret, strTime, 1);
        String auth = hash.toHex();
        Map<String, String> map = new HashMap<>();
        map.put("id", acc_id);
        map.put("time", String.valueOf(time));
        map.put("auth", auth);
        String X_Elastos_Agent_Auth_value = JSON.toJSONString(map);
        return X_Elastos_Agent_Auth_value;
    }


    private void propertiesMapToList(Map<String, String> propertiesMap, List<DidEntity.DidProperty> properties) {
        propertiesMap.forEach((k, v) -> {
            DidEntity.DidProperty property = new DidEntity.DidProperty();
            property.setKey(k);
            property.setValue(v);
            properties.add(property);
        });
    }

    private String packDidEntity(String didPrivateKey, DidEntity didEntity) {
        String msg = (JSON.toJSONString(didEntity));
        System.out.print("packDidEntity did entity:" + msg);
        Map<String, Object> signDid;
        try {
            signDid = sign(didPrivateKey, msg);
        } catch (Exception e) {
            return null;
        }
        String rawData = JSON.toJSONString(signDid);
        logger.debug("rawData:{}", rawData);
        return rawData;
    }

    /**
     * Sent raw data to did chain use wallet to pay
     *
     * @param nodeUrl             Did chain node url
     * @param payWalletPrivateKey
     * @param rawData
     * @return
     */
    public ReturnMsgEntity upChainByWallet(String nodeUrl, String payWalletPrivateKey, String rawData) {
        //Pack data to tx for record.
        ElaTransaction transaction = new ElaTransaction();
        transaction.setChainInfo(nodeUrl, ElaChainType.DID_CHAIN, ElaChainType.DID_CHAIN);
        transaction.setMemo(rawData);
        String sendAddr = Ela.getAddressFromPrivate(payWalletPrivateKey);
        transaction.addSender(sendAddr, payWalletPrivateKey);
        //Transfer ela to sender itself. the only record payment is miner FEE.
        transaction.addReceiver(sendAddr, DidConfiguration.FEE);
        try {
            ReturnMsgEntity ret = transaction.transfer();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return new ReturnMsgEntity().setResult("Err: sentRawDataToChain transfer failed").setStatus(RetCodeConfiguration.PROCESS_ERROR);
        }
    }

    /**
     * Get did property by txid
     *
     * @param nodeUrl     Did chain node url
     * @param did
     * @param propertyKey
     * @param txId
     * @return
     */
    public ReturnMsgEntity getDidPropertyByTxid(String nodeUrl, String did, String propertyKey, String txId) {
        DidBackendService didBackendService = new DidBackendService();
        didBackendService.setDidPrefix(nodeUrl);

        if (StringUtils.isAnyBlank(did, propertyKey, txId)) {
            return new ReturnMsgEntity().setResult("Err: getDidPropertyByTxid Parameter invalid.").setStatus(RetCodeConfiguration.BAD_REQUEST);
        }

        Map<String, Object> tx = didBackendService.getTransaction(txId, ChainType.DID_SIDECHAIN);
        if (null == tx) {
            return new ReturnMsgEntity().setResult(Errors.DID_NO_SUCH_INFO.val()).setStatus(RetCodeConfiguration.NOT_FOUND);
        }

        try {
            String property = getDidPropertyFromTx(did, propertyKey, tx);
            if (null != property) {
                JSONObject ret = new JSONObject();
                ret.put(propertyKey, property);
                return new ReturnMsgEntity().setResult(ret.toJSONString()).setStatus(RetCodeConfiguration.SUCC);
            } else {
                return new ReturnMsgEntity().setResult(Errors.DID_NO_SUCH_INFO.val()).setStatus(RetCodeConfiguration.NOT_FOUND);
            }
        } catch (Exception ex) {
            logger.warn(ex.getMessage());
            ex.printStackTrace();
            return new ReturnMsgEntity().setResult(Errors.DID_NO_SUCH_INFO.val()).setStatus(RetCodeConfiguration.NOT_FOUND);
        }
    }

    private String getDidPropertyFromTx(String did, String propertyKey, Map<String, Object> tx) {
        List<Map> attrList = (List) tx.get("attributes");
        String hexData = (String) attrList.get(0).get("data");
        Map rawData = (Map) JSON.parse(new String(DatatypeConverter.parseHexBinary(hexData)));

        String hexMsg = (String) rawData.get("msg");
        String pub = (String) rawData.get("pub");
        String sig = (String) rawData.get("sig");
        boolean verifyRet = verify(pub, sig, hexMsg);
        if (!verifyRet) {
            return null;
        }

        String msg = new String(DatatypeConverter.parseHexBinary(hexMsg));
        DidEntity didEntity = JSON.parseObject(msg, DidEntity.class);
        String tag = didEntity.getTag();
        if (StringUtil.isBlank(tag) || (0 != DidEntity.DID_TAG.compareTo(tag))) {
            return null;
        }

        DidEntity.DidStatus status = didEntity.getStatus();
        if ((null == status) || (DidEntity.DidStatus.Deprecated == status)) {
            return null;
        }

        String msgDid = ElaKit.getIdentityFromPublicKey(pub);
        if (StringUtil.isBlank(msgDid) || (0 != did.compareTo(msgDid))) {
            return null;
        }


        List<DidEntity.DidProperty> msgProperties = didEntity.getProperties();
        if ((null == msgProperties) || msgProperties.isEmpty()) {
            return null;
        }

        for (DidEntity.DidProperty property : msgProperties) {
            if (0 == property.getKey().compareTo(propertyKey)) {
                if (DidEntity.DidStatus.Deprecated == property.getStatus()) {
                    return null;
                } else {
                    return property.getValue();
                }
            }
        }

        return null;
    }

    public ReturnMsgEntity transferEla(String nodeUrl, ElaChainType srcChainType, List<String> srcWalletPrivateKeys, ElaChainType dstChainType, Map<String, Double> dstAddrAndEla) {
        ElaTransaction transaction = new ElaTransaction();
        transaction.setChainInfo(nodeUrl, srcChainType, dstChainType);
        for (String key : srcWalletPrivateKeys) {
            String sendAddr = Ela.getAddressFromPrivate(key);
            transaction.addSender(sendAddr, key);
        }

        for (Map.Entry<String, Double> entry : dstAddrAndEla.entrySet()) {
            transaction.addReceiver(entry.getKey(), entry.getValue());
        }

        ReturnMsgEntity ret;
        try {
            ret = transaction.transfer();
        } catch (Exception e) {
            e.printStackTrace();
            ret = new ReturnMsgEntity().setResult("Err: transferEla transfer failed"+e.getMessage()).setStatus(RetCodeConfiguration.PROCESS_ERROR);
        }

        return ret;
    }

    private Map<String, Object> sign(String privateKey, String msg) throws Exception {
        ECKey ec = ECKey.fromPrivate(DatatypeConverter.parseHexBinary(privateKey));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.write(msg.getBytes(CHARSET));

        byte[] signature = SignTool.doSign(baos.toByteArray(), DatatypeConverter.parseHexBinary(privateKey));
        byte[] code = new byte[33];
        System.arraycopy(Util.CreateSingleSignatureRedeemScript(ec.getPubBytes(), 1), 1, code, 0, code.length);

        Map<String, Object> result = new HashMap<>();
        result.put("msg", DatatypeConverter.printHexBinary(msg.getBytes(CHARSET)));
        result.put("pub", DatatypeConverter.printHexBinary(code));
        result.put("sig", DatatypeConverter.printHexBinary(signature));
        return result;
    }

    private boolean verify(String hexPub, String hexSig, String hexMsg) {
        byte[] msg = DatatypeConverter.parseHexBinary(hexMsg);
        byte[] sig = DatatypeConverter.parseHexBinary(hexSig);
        byte[] pub = DatatypeConverter.parseHexBinary(hexPub);
        boolean isVerify = ElaSignTool.verify(msg, sig, pub);
        return isVerify;
    }

}
