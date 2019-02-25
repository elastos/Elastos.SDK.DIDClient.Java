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
import org.eclipse.jetty.util.StringUtil;
import org.elastos.DTO.DidAuthRequest;
import org.elastos.POJO.DidEntity;
import org.elastos.conf.DidConfiguration;
import org.elastos.conf.RetCodeConfiguration;
import org.elastos.ela.ECKey;
import org.elastos.ela.Ela;
import org.elastos.ela.SignTool;
import org.elastos.ela.Util;
import org.elastos.entity.ChainType;
import org.elastos.entity.Errors;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.service.ela.DidBackendService;
import org.elastos.service.ela.ElaTransaction;
import org.elastos.util.ela.ElaKit;
import org.elastos.util.ela.ElaSignTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElaDidServiceImp implements ElaDidService {
    private static final String CHARSET = "UTF-8";

    private static Logger logger = LoggerFactory.getLogger(ElaDidServiceImp.class);

    @Override
    public void setElaNodeUrl(String nodeUrl) {
        DidBackendService.setDidPreFix(nodeUrl);
    }

    @Override
    public ReturnMsgEntity createDid() {
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

        return new ReturnMsgEntity().setResult(JSON.toJSONString(result)).setStatus(RetCodeConfiguration.SUCC);
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
    public ReturnMsgEntity signDidMessage(String didPrivateKey, String msg) {
        if (StringUtils.isAnyBlank(didPrivateKey,
                msg)) {
            return new ReturnMsgEntity().setResult("Err: signDidMessage Parameter invalid.").setStatus(RetCodeConfiguration.BAD_REQUEST);
        }

        Map<String, Object> signDid;
        try {
            signDid = this.sign(didPrivateKey, msg);
        } catch (Exception e) {
            return new ReturnMsgEntity().setResult("Err: signDidMessage failed").setStatus(RetCodeConfiguration.PROCESS_ERROR);
        }
        logger.debug("signDidMessage signDid:{}", JSON.toJSONString(signDid));
        String sig = (String) signDid.get("sig");
        return new ReturnMsgEntity().setResult(sig).setStatus(RetCodeConfiguration.SUCC);
    }

    @Override
    public ReturnMsgEntity verifyDidMessage(String didPublicKey, String sig, String msg) {
        if (StringUtils.isAnyBlank(didPublicKey,
                msg,
                sig)) {
            return new ReturnMsgEntity().setResult("Err: verifyDidMessage Parameter invalid.").setStatus(RetCodeConfiguration.BAD_REQUEST);
        }

        try {
            String hexmsg = DatatypeConverter.printHexBinary(msg.getBytes(CHARSET));
            boolean ret = this.verify(didPublicKey, sig, hexmsg);
            return new ReturnMsgEntity().setResult(ret).setStatus(RetCodeConfiguration.SUCC);
        } catch (Exception e) {
            return new ReturnMsgEntity().setResult("Err: verifyDidMessage failed").setStatus(RetCodeConfiguration.PROCESS_ERROR);
        }
    }

    @Override
    public ReturnMsgEntity getDidPublicKey(String didPrivateKey) {
        if (StringUtils.isBlank(didPrivateKey)) {
            return new ReturnMsgEntity().setResult("Err: getDidPublicKey Parameter invalid.").setStatus(RetCodeConfiguration.BAD_REQUEST);
        }

        String didPublicKey = Ela.getPublicFromPrivate(didPrivateKey);
        System.out.println("getDidPublicKey:" + didPublicKey);
        return new ReturnMsgEntity().setResult(didPublicKey).setStatus(RetCodeConfiguration.SUCC);
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
            return null;
        } else {
            return raw;
        }
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
            return null;
        } else {
            return raw;
        }
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

        String msg =  new String(DatatypeConverter.parseHexBinary(hexMsg));
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
}
