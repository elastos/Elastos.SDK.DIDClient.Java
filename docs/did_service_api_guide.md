# Elastos.SDK.DIDClient.API
==============

## Usage

DID service API export from interface ElaDidService.
Use it like:

```Java
import org.elastos.service.*;
//And new an object with its implimentation
ElaDidService didService = new ElaDidServiceImp();
```

## setElaNodeUrl
**void setElaNodeUrl(String url);**
* Set elastos chain node url.

## setBlockAgentUrl
**void setBlockAgentUrl(String url);**
* Set Block agent service url.

## setDidExplorerUrl
**void setDidExplorerUrl(String url);**
* Set DID explorer url.

## createDid
**String createDid();**
* Create a did, return a did private key, a public key and a did.

```Java
        String ret = didService.createDid();
        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("DID");
        didPublicKey = (String) data.get("DidPublicKey");
```

## destroyDid
**ReturnMsgEntity destroyDid(String payWalletPrivateKey, String didPrivateKey);**
* Deprecated a did from did chain. If you call this function, you will not get any data of this did. the parameter payWalletPrivateKey is used to pay the record fee, didPrivateKey is used to sign and verify did data.

```Java
    ReturnMsgEntity ret = didService.destroyDid(payPrivateKey, didPrivateKey);
    long status = ret.getStatus();
    if (status != RetCodeConfiguration.SUCC) {
        System.out.println("Err didService.destroyDid failed. result:" + JSON.toJSONString(ret.getResult()));
        return;
    }
    String txId = (String) ret.getResult();
```

## signDidMessage
**String signDidMessage(String didPrivateKey, String msg);**
* Sign msg with did private key.

```java
    String sig = didService.signDidMessage(didPrivateKey, didPropertyKey);
    if (null == sig) {
        System.out.println("Err didService.signDidMessage failed.");
        return;
    }
```

## verifyDidMessage
**boolean verifyDidMessage(String didPublicKey, String sig, String msg);**
* Verify the sig with did public key.

```Java
    boolean isVerify = didService.verifyDidMessage(didPublicKey, sig, didPropertyKey);
    if (!isVerify) {
        System.out.println("Err didService.verifyDidMessage not right. result:");
        return;
    }
```

## getDidPublicKey
**String getDidPublicKey(String didPrivateKey);**
* Get did public key from did private key.

```Java
    String ret = didService.getDidPublicKey(didPrivateKey);
    if (null == pubKey) {
        System.out.println("Err didService.getDidPublicKey failed. result:");
        return;
    }
```

## getDid
**String getDid(String didPrivateKey);**
* Get did from  did private key.

```Java
    String did = didService.getDid(didPrivateKey);
    if (null == did) {
        System.out.println("Err didService.getDidFromPublicKey failed.";
        return;
    }
```

## getDidFromPublicKey
**String getDidFromPublicKey(String publicKey);**
* Get did from  did private key.

```Java
    String did = didService.getDidFromPublicKey(didPubKey);
    if (null == did) {
        System.out.println("Err didService.getDidFromPublicKey failed.";
        return;
    }
```
## packDidRawData
**String packDidRawData(String didPrivateKey, String propertyKey, String propertyData);**
* Pack a raw data which can be send to [did chain service](https://github.com/elastos/Elastos.ORG.DID.Service/tree/did_chain_service) to record.

```Java
    String ret = didService.packDidRawData(didPrivateKey, didPropertyKey, didPropertyValue);
    if (null == rawData) {
        System.out.println("Err didService.packDidRawData failed.");
        return;
    }
    System.out.println("DidService.packDidRawData rawData:" + rawData);
```

## upChainByBlockAgent
**String upChainByBlockAgent(String accessId, String accessSecret, String rawData);**
* Send raw data which be gene by packDidRawData to did chain.
* The accessId and accessSecret get form elastos service platform.
```Java
    String txid = didService.upChainByBlockAgent(accessId, accessSecret, rawData);
    if (null == txid) {
        System.out.println("Err didService.upChainByBlockAgent failed.");
        return;
    }
    System.out.println("DidService.upChainByBlockAgent txid:" + txid);
```

## setDidProperty
**ReturnMsgEntity setDidProperty(String payWalletPrivateKey, String didPrivateKey, String propertyKey, String propertyData);**
* Put propertyKey and propertyData on did chain. payWalletPrivateKey is used to pay the record fee, didPrivateKey is used to sign and verify did data.

```Java
    ReturnMsgEntity ret = didService.setDidProperty(payPrivateKey, didPrivateKey, didPropertyKey, didPropertyValue);
    long status = ret.getStatus();
    if (status != RetCodeConfiguration.SUCC) {
        System.out.println("Err didService.setDidProperty failed. result:" + JSON.toJSONString(ret.getResult()));
        return;
    }
    String txId = (String) ret.getResult();
```

## getDidPropertyByTxId
**ReturnMsgEntity getDidPropertyByTxId(String did, String propertyKey, String txId);**
* Get property value from did chain of a designated transaction (txId).

```Java
    ReturnMsgEntity ret = didService.getDidPropertyByTxId(did, didPropertyKey, txId);
    long status = ret.getStatus();
    if (status != RetCodeConfiguration.SUCC) {
        System.out.println("Err didService.getDidPropertyByTxId failed. result:" + JSON.toJSONString(ret.getResult()));
        return;
    }
    String propertyJson = (String) ret.getResult();
    String property = JSONObject.parseObject(propertyJson).getString(didPropertyKey);
```

## delDidProperty
**ReturnMsgEntity delDidProperty(String payWalletPrivateKey, String didPrivateKey, String propertyKey);**
* Delete a did property. payWalletPrivateKey is used to pay the record fee, didPrivateKey is used to sign and verify did data.

```Java
    ReturnMsgEntity ret = didService.delDidProperty(payPrivateKey, didPrivateKey, didPropertyKey);
    long status = ret.getStatus();
    if (status != RetCodeConfiguration.SUCC) {
        System.out.println("Err didService.delDidProperty failed. result:" + JSON.toJSONString(ret.getResult()));
        return;
    }

    String txId = (String) ret.getResult();
```
## bindUserDid
**String bindUserDid(String serverDidPrivateKey, String userId, String userDid);**
* Bind userId and userDid and pack raw data for up to DID chain.

```Java
    String rawData = didService.bindUserDid(String serverDidPrivateKey, String userId, String userDid);
    if (null == rawData) {
        System.out.println("Err didService.bindUserDid failed.");
        return;
    }
```

## getUserDid
**String getUserDid(String serverDid, String userId);**
* Get userDId from DID chain.

```Java
    String userDid = didService.getUserDid(String serverDid, String userId);
    if (null == userDid) {
        System.out.println("Err didService.getUserDid failed.");
        return;
    }
```
