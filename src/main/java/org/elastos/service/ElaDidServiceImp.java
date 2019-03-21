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
import org.elastos.DTO.DidAuthRequest;
import org.elastos.POJO.DidEntity;
import org.elastos.conf.DependServiceConfiguration;
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

public class ElaDidServiceImp implements ElaDidService {
    private static final String CHARSET = "UTF-8";

    private static Logger logger = LoggerFactory.getLogger(ElaDidServiceImp.class);

    private String blockAgentUrl = DependServiceConfiguration.BLOCKAGENT_URL_PRIFIX;

    private String didExplorerUrl = DependServiceConfiguration.DID_EXPLORER_URL_PRIFIX;


    @Override
    public void setElaNodeUrl(String nodeUrl) {
        DidBackendService.setDidPreFix(nodeUrl);
    }

    @Override
    public void setBlockAgentUrl(String url) {
        blockAgentUrl = url;
    }

    @Override
    public void setDidExplorerUrl(String url) {
        didExplorerUrl = url;
    }

    @Override
    public String createDidMnemonic() {
        String mnemonic = ElaHdSupport.generateMnemonic(MnemonicType.ENGLISH);
        return mnemonic;
    }

    @Override
    public String createDidByMnemonic(String mnemonic) {
        String ret;
        try {
            ret = ElaHdSupport.generate(mnemonic, 0);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | CipherException e) {
            logger.error("Err: signDidMessage Parameter invalid.");
            e.printStackTrace();
            return null;
        }

//        return "{\"privateKey\":\"" + privateKey + "\",\"publicKey\":\"" + publicKey + "\",\"publicAddress\":\"" + publicAddr + "\"}";
        Map data = JSON.parseObject(ret, Map.class);
        String privateKey = (String) data.get("privateKey");
        String publicKey = (String) data.get("publicKey");
        String did = Ela.getIdentityIDFromPrivate(privateKey);

        Map<String, String> result = new HashMap<>();
        result.put("DidPrivateKey", privateKey);
        result.put("DID", did);
        result.put("DidPublicKey", publicKey);

        return JSON.toJSONString(result);
    }

    @Override
    public String createDid() {
        String didPrivateKey = Ela.getPrivateKey();
        System.out.println(didPrivateKey);
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);
        System.out.println(did);
        String didPublicKey = Ela.getPublicFromPrivate(didPrivateKey);
        System.out.println(didPublicKey);

        Map<String, String> result = new HashMap<>();
        result.put("DidPrivateKey", didPrivateKey);
        result.put("DID", did);
        result.put("DidPublicKey", didPublicKey);

        return JSON.toJSONString(result);
    }

    @Override
    public ReturnMsgEntity destroyDid(String payWalletPrivateKey, String didPrivateKey) {
        if (StringUtils.isAnyBlank(payWalletPrivateKey,
                didPrivateKey)) {
            return new ReturnMsgEntity().setResult("Err: SetDidProperty Parameter invalid.").setStatus(RetCodeConfiguration.BAD_REQUEST);
        }

        //Create rawMemo
        DidEntity didEntity = new DidEntity();
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);
        didEntity.setDid(did);
        didEntity.setStatus(DidEntity.DidStatus.Deprecated);
        return setDidEntity(payWalletPrivateKey, didPrivateKey, didEntity);
    }

    @Override
    public String signDidMessage(String didPrivateKey, String msg) {
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

    @Override
    public boolean verifyDidMessage(String didPublicKey, String sig, String msg) {
        if (StringUtils.isAnyBlank(didPublicKey,
                msg,
                sig)) {
            logger.error("Err: verifyDidMessage Parameter invalid.");
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

    @Override
    public String getDidPublicKey(String didPrivateKey) {
        if (StringUtils.isBlank(didPrivateKey)) {
            logger.error("Err: getDidPublicKey Parameter invalid.");
            return null;
        }

        String didPublicKey = Ela.getPublicFromPrivate(didPrivateKey);
        System.out.println("getDidPublicKey:" + didPublicKey);
        return didPublicKey;
    }

    @Override
    public String getDid(String didPrivateKey) {
        if (StringUtils.isBlank(didPrivateKey)) {
            return null;
        }
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);
        System.out.println("getDid:" + did);
        return did;
    }

    @Override
    public String getDidFromPublicKey(String publicKey) {
        if (StringUtils.isBlank(publicKey)) {
            return null;
        }
        String did = ElaKit.getIdentityFromPublicKey(publicKey);
        return did;
    }

    @Override
    public String packDidRawData(String didPrivateKey, String propertyKey, String propertyValue) {
        if (StringUtils.isAnyBlank(
                didPrivateKey,
                propertyKey,
                propertyValue)) {
            logger.error("Err: packDidRawData parameter invalid");
            System.out.println("Err: packDidRawData parameter invalid");
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
        String raw = packRawDid(didPrivateKey, didEntity);
        if (null == raw) {
            logger.error("Err: packDidRawData packRawDid failed");
            System.out.println("Err: packDidRawData packRawDid failed");
        }
        return raw;
    }

    @Override
    public String packDidRawData(String didPrivateKey, Map<String, String> propertiesMap) {
        if (StringUtils.isBlank(didPrivateKey)
                || (null == propertiesMap)
                || (propertiesMap.isEmpty())) {
            logger.error("Err: packDidRawData parameter invalid");
            System.out.println("Err: packDidRawData parameter invalid");
            return null;
        }

        List<DidEntity.DidProperty> properties = new ArrayList<>();
        propertiesMapToList(propertiesMap, properties);
        DidEntity didEntity = new DidEntity();
        didEntity.setProperties(properties);
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);
        didEntity.setDid(did);
        String raw = packRawDid(didPrivateKey, didEntity);
        if (null == raw) {
            logger.error("Err: packDidRawData packRawDid failed");
            System.out.println("Err: packDidRawData packRawDid failed");
        }
        return raw;
    }

    @Override
    public String upChainByBlockAgent(String accessId, String accessSecret, String rawData) {
        Map<String, String> header = new HashMap<>();
        header.put("X-Elastos-Agent-Auth", createAuthHeaderValue(accessId, accessSecret));
        String response = HttpUtil.post(blockAgentUrl + "/api/1/blockagent/upchain/data", rawData, header);
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

    private String createAuthHeaderValue(String acc_id, String acc_secret) {
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

    @Override
    public ReturnMsgEntity setDidProperty(String payWalletPrivateKey, String didPrivateKey, String propertyKey, String propertyValue) {

        if (StringUtils.isAnyBlank(payWalletPrivateKey,
                didPrivateKey,
                propertyKey,
                propertyValue)) {

            return new ReturnMsgEntity().setResult("Err: SetDidProperty Parameter invalid.").setStatus(RetCodeConfiguration.BAD_REQUEST);
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
        return setDidEntity(payWalletPrivateKey, didPrivateKey, didEntity);

    }

    @Override
    public ReturnMsgEntity setDidProperties(String payWalletPrivateKey, String didPrivateKey, Map<String, String> propertiesMap) {
        if (StringUtils.isAnyBlank(payWalletPrivateKey, didPrivateKey)
                || (null == propertiesMap)
                || (propertiesMap.isEmpty())) {

            return new ReturnMsgEntity().setResult("Err: setDidProperties Parameter invalid.").setStatus(RetCodeConfiguration.BAD_REQUEST);
        }

        List<DidEntity.DidProperty> properties = new ArrayList<>();
        propertiesMapToList(propertiesMap, properties);
        DidEntity didEntity = new DidEntity();
        didEntity.setProperties(properties);
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);
        didEntity.setDid(did);
        return setDidEntity(payWalletPrivateKey, didPrivateKey, didEntity);
    }

    private String packRawDid(String didPrivateKey, DidEntity didEntity) {
        String msg = (JSON.toJSONString(didEntity));
        System.out.print("packRawDid did entity:" + msg);
        Map<String, Object> signDid;
        try {
            signDid = sign(didPrivateKey, msg);
        } catch (Exception e) {
            return null;
        }
        String rawMemo = JSON.toJSONString(signDid);
        logger.debug("rawMemo:{}", rawMemo);
        return rawMemo;
    }

    private ReturnMsgEntity setDidEntity(String payWalletPrivateKey, String didPrivateKey, DidEntity didEntity) {

        String rawMemo = packRawDid(didPrivateKey, didEntity);
        if (null == rawMemo) {
            return new ReturnMsgEntity().setResult("Err: setDidEntity packRawDid failed").setStatus(RetCodeConfiguration.PROCESS_ERROR);
        }

        //Pack data to tx for record.
        ElaTransaction transaction = new ElaTransaction(ChainType.DID_SIDECHAIN, rawMemo);

        String sendAddr = Ela.getAddressFromPrivate(payWalletPrivateKey);
        transaction.addSender(sendAddr, payWalletPrivateKey);
        //Transfer ela to sender itself. the only record payment is miner FEE.
        transaction.addReceiver(sendAddr, DidConfiguration.FEE);
        try {
            ReturnMsgEntity ret = transaction.transfer();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return new ReturnMsgEntity().setResult("Err: SetDidProperty transfer failed").setStatus(RetCodeConfiguration.PROCESS_ERROR);
        }
    }

    //    @Override
    public ReturnMsgEntity getDidProperty(String did, String propertyKey) {

        //获取did信息操作要注意已经注销的did任何获取操作无效。
        //did属性如果删除，返回空值。
        return null;
    }

    @Override
    public ReturnMsgEntity getDidPropertyByTxId(String did, String propertyKey, String txId) {

        if (StringUtils.isAnyBlank(did, propertyKey, txId)) {
            return new ReturnMsgEntity().setResult("Err: getDidPropertyByTxId Parameter invalid.").setStatus(RetCodeConfiguration.BAD_REQUEST);
        }

        Map<String, Object> tx = DidBackendService.getTransaction(txId, ChainType.DID_SIDECHAIN);
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
        Map rawMemo = (Map) JSON.parse(new String(DatatypeConverter.parseHexBinary(hexData)));

        String hexMsg = (String) rawMemo.get("msg");
        String pub = (String) rawMemo.get("pub");
        String sig = (String) rawMemo.get("sig");
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

    @Override
    public ReturnMsgEntity delDidProperty(String payWalletPrivateKey, String didPrivateKey, String propertyKey) {
        if (StringUtils.isAnyBlank(payWalletPrivateKey,
                didPrivateKey,
                propertyKey)) {

            return new ReturnMsgEntity().setResult("Err: SetDidProperty Parameter invalid.").setStatus(RetCodeConfiguration.BAD_REQUEST);
        }

        //Create rawMemo
        String did = Ela.getIdentityIDFromPrivate(didPrivateKey);

        DidEntity.DidProperty property = new DidEntity.DidProperty();
        property.setKey(propertyKey);
        property.setStatus(DidEntity.DidStatus.Deprecated);

        DidEntity didEntity = new DidEntity();
        List<DidEntity.DidProperty> properties = new ArrayList<>();
        properties.add(property);
        didEntity.setProperties(properties);
        didEntity.setDid(did);
        return setDidEntity(payWalletPrivateKey, didPrivateKey, didEntity);
    }

    //    @Override
    public ReturnMsgEntity bindUserCountToDid(String payWalletPrivateKey, String didPrivateKey, String userDid, String userId) {
        return null;
    }

    //    @Override
    public ReturnMsgEntity sendDidAuthRequest(String didPrivateKey, Long randomNum, Long serialNum) {
        return null;
    }

    //    @Override
    public ReturnMsgEntity sendDidAuthResponse(String didPrivateKey, DidAuthRequest request) {
        return null;
    }

    //    @Override
    public ReturnMsgEntity ResDidPonseAndRequestAuth(String didPrivateKey, Long randomNum, DidAuthRequest request) {
        return null;
    }

    //    @Override
    public ReturnMsgEntity saveCertificationToDid(String payWalletPrivateKey, String saverDidPrivateKey, String ownerDidPublicKey, String certification) {
        return null;
    }

    //    @Override
    public ReturnMsgEntity getCertificationFromDid(String did, String signature, String certificationPath) {
        return null;
    }

    //    @Override
    public ReturnMsgEntity certificateDidProperty(String payWalletPrivateKey, String didPrivateKey, String propertyKey) {
        return null;
    }


    public ReturnMsgEntity transferEla(List<String> srcPrivateKeys, Map<String, Double> dstAddrAndEla, ChainType chainType) {


        ElaTransaction transaction = new ElaTransaction(chainType, "");


        for (String key : srcPrivateKeys) {
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
            ret = new ReturnMsgEntity().setResult("Err: transferEla transfer failed").setStatus(RetCodeConfiguration.PROCESS_ERROR);
        }

        return ret;
    }

    /**
     * using privateKey sign data
     *
     * @param
     * @return
     * @throws Exception
     */
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

    /**
     * verify if message is signed by a public key
     *
     * @param
     * @return
     */
    private boolean verify(String hexPub, String hexSig, String hexMsg) {
        byte[] msg = DatatypeConverter.parseHexBinary(hexMsg);
        byte[] sig = DatatypeConverter.parseHexBinary(hexSig);
        byte[] pub = DatatypeConverter.parseHexBinary(hexPub);
        boolean isVerify = ElaSignTool.verify(msg, sig, pub);
        return isVerify;
    }

    @Override
    public String bindUserDid(String serverDidPrivateKey, String userId, String userDid) {
        if (StringUtils.isAnyBlank(serverDidPrivateKey, userDid, userId)) {
            logger.error("Err: bindUserDid Parameter invalid.");
            System.out.println("Err: bindUserDid Parameter invalid.");
        }
        String key = "user/" + userId + "/DID";
        return this.packDidRawData(serverDidPrivateKey, key, userDid);
    }


    @Override
    public String getUserDid(String serverDid, String userId) {
        String key = "user/" + userId + "/DID";
        String response = HttpUtil.get(didExplorerUrl + "/api/1/didexplorer/did/" + serverDid + "/property?key=" + key, null);
        Map<String, Object> msg = (Map<String, Object>) JSON.parse(response);
        if ((int) msg.get("status") == 200) {
            String result = (String) msg.get("result");
            if ((null != result) && (!result.isEmpty())) {
                List<Map<String, String>> properties = (List<Map<String, String>>) JSON.parse(result);
                return properties.get(0).get("value");
            } else {
                return null;
            }
        } else {
            System.out.println("Err: getDataFromElaChain failed" + msg.get("result"));
            return null;
        }
    }

}
