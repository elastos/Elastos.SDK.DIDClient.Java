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
import org.elastos.POJO.*;
import org.elastos.conf.BasicConfiguration;
import org.elastos.constant.RetCode;
import org.elastos.ela.ECKey;
import org.elastos.ela.Ela;
import org.elastos.ela.SignTool;
import org.elastos.ela.Util;
import org.elastos.entity.Errors;
import org.elastos.entity.MnemonicType;
import org.elastos.exception.ElaDidServiceException;
import org.elastos.service.ela.BackendService;
import org.elastos.service.ela.ElaTransaction;
import org.elastos.util.HttpUtil;
import org.elastos.util.RetResult;
import org.elastos.util.ela.ElaHdSupport;
import org.elastos.util.ela.ElaKit;
import org.elastos.util.ela.ElaSignTool;
import org.web3j.crypto.CipherException;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public class ElaDidService {
    private static final String CHARSET = "UTF-8";
    private BackendService backendService = new BackendService();

    public ElaDidService(String inNodeUrl, boolean isTestNet) {
        if (null == inNodeUrl) {
            throw new ElaDidServiceException("ElaDidService node url must not be null");
        }
        backendService.setPrefix(inNodeUrl);
        backendService.setTestNet(isTestNet);
    }

    /**
     * Create mnemonic to generate did
     *
     * @return mnemonic
     */
    public static String createMnemonic() {
        String mnemonic = ElaHdSupport.generateMnemonic(MnemonicType.ENGLISH);
        return mnemonic;
    }

    /**
     * @param mnemonic
     * @param index
     * @return Json string of did data: DidPrivateKey, DidPublicKey, Did
     */
    public static String createDid(String mnemonic, int index) throws InvalidKeySpecException, NoSuchAlgorithmException, CipherException {
        String ret = ElaHdSupport.generate(mnemonic, index);

        JSONObject data = JSON.parseObject(ret);
        String privateKey = data.getString("privateKey");
        String publicKey = data.getString("publicKey");
        String did = Ela.getIdentityIDFromPrivate(privateKey);

        Map<String, String> result = new HashMap<>();
        result.put("DidPrivateKey", privateKey);
        result.put("Did", did);
        result.put("DidPublicKey", publicKey);

        return JSON.toJSONString(result);
    }

    /**
     * @param mnemonic
     * @param index
     * @return DidCredentials
     */
    public static DidCredentials geneDidCredentials(String mnemonic, int index) throws InvalidKeySpecException, NoSuchAlgorithmException, CipherException {
        String ret = ElaHdSupport.generate(mnemonic, index);
        JSONObject data = JSON.parseObject(ret);

        KeyPair keyPair = new KeyPair();

        String privateKey = data.getString("privateKey");
        keyPair.setPrivateKey(privateKey);
        keyPair.setPublicKey(data.getString("publicKey"));
        DidCredentials didCredentials = new DidCredentials();
        didCredentials.setKeyPair(keyPair);
        didCredentials.setDid(Ela.getIdentityIDFromPrivate(privateKey));
        didCredentials.setAddress(Ela.getAddressFromPrivate(privateKey));
        return didCredentials;
    }

    /**
     * Get a did signature of a message
     *
     * @param didPrivateKey
     * @param msg
     * @return signature
     */
    public static String signMessage(String didPrivateKey, String msg) throws Exception {
        if (StringUtils.isAnyBlank(didPrivateKey,
                msg)) {
            System.out.println("Err: signDidMessage Parameter invalid.");
            return null;
        }

        Map<String, Object> signDid = sign(didPrivateKey, msg);
        System.out.println("signDidMessage signDid:" + JSON.toJSONString(signDid));
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
    public static boolean verifyMessage(String didPublicKey, String sig, String msg) {
        if (StringUtils.isAnyBlank(didPublicKey,
                msg,
                sig)) {
            System.out.println("Err: verifyMessage parameter invalid");
            return false;
        }

        try {
            String hexmsg = DatatypeConverter.printHexBinary(msg.getBytes(CHARSET));
            boolean ret = verify(didPublicKey, sig, hexmsg);
            return ret;
        } catch (Exception e) {
            System.out.println("Err: verifyDidMessage failed.");
            return false;
        }
    }

    /**
     * Get did public key by did private key
     *
     * @param didPrivateKey
     * @return didPublicKey
     */
    public static String getDidPublicKey(String didPrivateKey) {
        if (StringUtils.isBlank(didPrivateKey)) {
            System.out.println("Err: getDidPublicKey Parameter invalid.");
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
    public static String getDidFromPrivateKey(String didPrivateKey) {
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
    public static String getDidFromPublicKey(String publicKey) {
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
    public static String packDidProperty(String didPrivateKey, String propertyKey, String propertyValue) {
        if (StringUtils.isAnyBlank(
                didPrivateKey,
                propertyKey,
                propertyValue)) {
            System.out.println("Err: packDidProperty parameter invalid");
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
    public static String packDidProperties(String didPrivateKey, Map<String, String> propertiesMap) {
        if (StringUtils.isBlank(didPrivateKey)
                || (null == propertiesMap)
                || (propertiesMap.isEmpty())) {
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
    public static String packDelDidProperty(String didPrivateKey, String propertyKey) {
        if (StringUtils.isAnyBlank(
                didPrivateKey,
                propertyKey)) {
            System.out.println("Err: packDelDidProperty parameter invalid");
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
     * @return raw data
     */
    public static String packDestroyDid(String didPrivateKey) {
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
    public static String upChainByAgent(String agentUrl, String accessId, String accessSecret, String rawData) {
        Map<String, String> header = new HashMap<>();
        if (!StringUtils.isAnyBlank(accessId, accessSecret)) {
            header.put("X-Elastos-Agent-Auth", createAuthHeader(accessId, accessSecret));
        }
        String response = HttpUtil.post(agentUrl + "/api/1/blockagent/upchain/data", rawData, header);
        if (null == response) {
            System.out.println("Err: putDataToElaChain post failed");
            return null;
        }

        JSONObject msg = JSON.parseObject(response);
        if (msg.getIntValue("status") == 200) {
            return msg.getString("result");
        } else {
            System.out.println("Err: block agent failed" + msg.getString("result"));
            return null;
        }
    }

    private static String createAuthHeader(String acc_id, String acc_secret) {
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

    private static  void propertiesMapToList(Map<String, String> propertiesMap, List<DidEntity.DidProperty> properties) {
        propertiesMap.forEach((k, v) -> {
            DidEntity.DidProperty property = new DidEntity.DidProperty();
            property.setKey(k);
            property.setValue(v);
            properties.add(property);
        });
    }

    private static String packDidEntity(String didPrivateKey, DidEntity didEntity) {
        String msg = (JSON.toJSONString(didEntity));
        System.out.print("packDidEntity did entity:" + msg);
        Map<String, Object> signDid;
        try {
            signDid = sign(didPrivateKey, msg);
        } catch (Exception e) {
            return null;
        }
        String rawData = JSON.toJSONString(signDid);
        return rawData;
    }

    public RetResult<String> upChainByWallet(String payWalletPrivateKey, String rawDidData){
        return upChainByWallet(payWalletPrivateKey, rawDidData, null);
    }
    /**
     * Sent raw data to did chain use wallet to pay
     *
     * @param payWalletPrivateKey
     * @param rawDidData
     * @return
     */
    public RetResult<String> upChainByWallet(String payWalletPrivateKey, String rawDidData, String dstAddress) {
        //Pack data to tx for record.
        ElaTransaction transaction = new ElaTransaction(backendService, ElaChainType.DID_CHAIN, ElaChainType.DID_CHAIN);
        transaction.setMemo(rawDidData);
        String sendAddr = Ela.getAddressFromPrivate(payWalletPrivateKey);
        RetResult retSender = transaction.addSender(sendAddr, payWalletPrivateKey);
        if (retSender.getCode() != RetCode.SUCC) {
            return RetResult.retErr(retSender.getCode(), "Err: sentRawDataToChain " + retSender.getMsg());
        }

        if (null == dstAddress) {
            //Transfer ela to sender itself. the only record payment is miner FEE.
            transaction.addReceiver(sendAddr, BasicConfiguration.FEE);
        } else {
            transaction.addReceiver(dstAddress, BasicConfiguration.FEE);
        }
        try {
            RetResult<String> ret = transaction.transfer();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err: sentRawDataToChain exception:" + e.getMessage());
        }
    }

    /**
     * Get did property by txid
     *
     * @param did
     * @param propertyKey
     * @param txId
     * @return RetResult
     */
    public RetResult<String> getDidPropertyByTxid(String did, String propertyKey, String txId) {
        if (StringUtils.isAnyBlank(did, propertyKey, txId)) {
            return RetResult.retErr(RetCode.BAD_REQUEST_PARAMETER, "Err: getDidPropertyByTxid Parameter invalid.");
        }

        Map<String, Object> tx = backendService.getTransaction(txId);
        if (null == tx) {
            return RetResult.retErr(RetCode.NOT_FOUND, Errors.DID_NO_SUCH_INFO.val());
        }

        try {
            String property = getDidPropertyFromTx(did, propertyKey, tx);
            if (null != property) {
                JSONObject ret = new JSONObject();
                ret.put(propertyKey, property);
                return RetResult.retOk(ret.toJSONString());
            } else {
                return RetResult.retErr(RetCode.NOT_FOUND, Errors.DID_NO_SUCH_INFO.val());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return RetResult.retErr(RetCode.NOT_FOUND, "getDidPropertyByTxid exception:" + ex.getMessage());
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

    private static Map<String, Object> sign(String privateKey, String msg) throws Exception {
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

    private static boolean verify(String hexPub, String hexSig, String hexMsg) {
        byte[] msg = DatatypeConverter.parseHexBinary(hexMsg);
        byte[] sig = DatatypeConverter.parseHexBinary(hexSig);
        byte[] pub = DatatypeConverter.parseHexBinary(hexPub);
        boolean isVerify = ElaSignTool.verify(msg, sig, pub);
        return isVerify;
    }

}
