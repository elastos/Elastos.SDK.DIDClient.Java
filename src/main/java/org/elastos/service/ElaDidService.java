/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.service;

import org.elastos.entity.ReturnMsgEntity;

import java.util.Map;

public interface ElaDidService {
    //创建DID
    //已实现
    ReturnMsgEntity createDid();

    //注销DID，该操作不可撤销。在did上有特殊属性来验证状态。 必须要注意，已经注销的did不能操作以下任何服务功能
    //已实现
    ReturnMsgEntity destroyDid(String payWalletPrivateKey, String didPrivateKey);

    //使用DID签名数据
    //已实现
    ReturnMsgEntity signDidMessage(String didPrivateKey, String msg);

    //验证DID签名
    //已实现
    ReturnMsgEntity verifyDidMessage(String didPublicKey, String sig, String msg);

    //获取DID公钥
    //已实现
    ReturnMsgEntity getDidPublicKey(String didPrivateKey);

    //获取DID
    //已实现
    ReturnMsgEntity getDid(String didPrivateKey);

    //将DID属性数据打包成上链数据
    ReturnMsgEntity packDidRawData(String didPrivateKey, String propertyKey, String propertyData);

    //将多个DID属性数据打包成上链数据
    ReturnMsgEntity packDidRawData(String didPrivateKey, Map<String, String> properties);

    //已实现
    ReturnMsgEntity setDidProperty(String payWalletPrivateKey, String didPrivateKey, String propertyKey, String propertyData);

    //已实现
    ReturnMsgEntity setDidProperties(String payWalletPrivateKey, String didPrivateKey, Map<String, String> properties);

//    ReturnMsgEntity getDidProperty(String did, String propertyKey);

    //已实现
    ReturnMsgEntity getDidPropertyByTxId(String did, String propertyKey, String txId);

    //已实现
    ReturnMsgEntity delDidProperty(String payWalletPrivateKey, String didPrivateKey, String propertyKey);

    // 绑定用户名与第三方登陆
//    ReturnMsgEntity bindUserCountToDid(String payWalletPrivateKey, String didPrivateKey, String userDid, String userId);

//    ReturnMsgEntity sendDidAuthRequest(String didPrivateKey, Long randomNum, Long serialNum);

//    ReturnMsgEntity sendDidAuthResponse(String didPrivateKey, DidAuthRequest request);

//    ReturnMsgEntity ResDidPonseAndRequestAuth(String didPrivateKey, Long randomNum, DidAuthRequest request);

    //数据管理
//    ReturnMsgEntity saveCertificationToDid(String payWalletPrivateKey, String saverDidPrivateKey, String ownerDidPublicKey, String certification);

//    ReturnMsgEntity getCertificationFromDid(String did, String signature, String certificationKey);

//    ReturnMsgEntity certificateDidProperty(String payWalletPrivateKey,  String didPrivateKey, String propertyKey);

}
