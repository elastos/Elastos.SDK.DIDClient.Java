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

## createDid
**ReturnMsgEntity createDid();**
* Create a did, return a did private key, a public key and a did.

```Java
    ReturnMsgEntity ret = didService.createDid();
    long status = ret.getStatus();
    if( status != RetCodeConfiguration.SUCC){
        System.out.println("Err didService.createDid failed. result:" + JSON.toJSONString(ret.getResult()));
        return;
    }
    Map data = JSON.parseObject((String) ret.getResult(), Map.class);
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
**ReturnMsgEntity signDidMessage(String didPrivateKey, String msg);**
* Sign msg with did private key.

```java
    ReturnMsgEntity ret = didService.signDidMessage(didPrivateKey, msg);
    long status = ret.getStatus();
    if( status != RetCodeConfiguration.SUCC){
        System.out.println("Err didService.signDidMessage failed. result:" + JSON.toJSONString(ret.getResult()));
        return;
    }
    String sig = (String) ret.getResult();
```

## verifyDidMessage
**ReturnMsgEntity verifyDidMessage(String didPublicKey, String sig, String msg);**
* Verify the sig with did public key.

```Java
    ReturnMsgEntity ret = didService.verifyDidMessage(didPublicKey, sig, msg);
    long status = ret.getStatus();
    if (status != RetCodeConfiguration.SUCC) {
        System.out.println("Err didService.verifyDidMessage failed. result:" + JSON.toJSONString(ret.getResult()));
        return;
    }

    Boolean isVerify = (Boolean) ret.getResult();
```

## getDidPublicKey
**ReturnMsgEntity getDidPublicKey(String didPrivateKey);**
* Get did public key from did private key.

```Java
    ReturnMsgEntity ret = didService.getDidPublicKey(didPrivateKey);
    long status = ret.getStatus();
    if (status != RetCodeConfiguration.SUCC) {
        System.out.println("Err didService.getDidPublicKey failed. result:" + JSON.toJSONString(ret.getResult()));
        return;
    }

    String pubKey = (String) ret.getResult();
```

## getDid
**ReturnMsgEntity getDid(String didPrivateKey);**
* Get did from  did private key.

```Java
    ReturnMsgEntity ret = didService.getDid(didPrivateKey);
    long status = ret.getStatus();
    if (status != RetCodeConfiguration.SUCC) {
        System.out.println("Err didService.getDid failed. result:" + JSON.toJSONString(ret.getResult()));
        return;
    }
    String id = (String) ret.getResult();
```

## packDidRawData
**ReturnMsgEntity packDidRawData(String didPrivateKey, String propertyKey, String propertyData);**
* Pack a raw data which can be send to [did chain service](https://github.com/elastos/Elastos.ORG.DID.Service/tree/did_chain_service) to record.

```Java
    ReturnMsgEntity ret = didService.packDidRawData(didPrivateKey, didPropertyKey, didPropertyValue);
    long status = ret.getStatus();
    if (status != RetCodeConfiguration.SUCC) {
        System.out.println("Err didService.packDidRawData failed. result:" + JSON.toJSONString(ret.getResult()));
        return;
    }
    String rawData = (String) ret.getResult();
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