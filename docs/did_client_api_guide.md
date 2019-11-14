# Elastos.SDK.DIDClient.API
==============

## Usage

DID service API export from interface ElaDidService.
Use it like:

```Java
import org.elastos.service.*;
//And new an object with its implimentation
ElaDidService didService = new ElaDidService(String didNodeUrl, boolean isTestNet);
```

## createMnemonic
**String ElaDidService::createMnemonic();**
* Create mnemonic to generate did

## createDid
**String ElaDidService::createDid(String mnemonic, int index);**
* Create a did, return a did private key, a public key and a did.

```Java
String memo = ElaDidService.createMnemonic();
String ret = ElaDidService.createDid(memo, 0);
Map data = JSON.parseObject(ret, Map.class);
didPrivateKey = (String) data.get("DidPrivateKey");
did = (String) data.get("Did");
didPublicKey = (String) data.get("DidPublicKey");
```

## getDidPublicKey
**String ElaDidService::getDidPublicKey(String didPrivateKey);**
* Get did public key from did private key.

```Java
String ret = ElaDidService.getDidPublicKey(didPrivateKey);
if (null == pubKey) {
    System.out.println("Err didService.getDidPublicKey failed. retResult:");
    return;
}
```

## getDidFromPrivateKey
**String ElaDidService::getDidFromPrivateKey(String didPrivateKey);**
* Get did from did private key.

```Java
String did = ElaDidService.getDidFromPrivateKey(didPrivateKey);
if (null == did) {
    System.out.println("Err didService.getDidFromPrivateKey failed.";
    return;
}
```

## getDidFromPublicKey
**String ElaDidService::getDidFromPublicKey(String publicKey);**
* Get did from  did public key.

```Java
String did = ElaDidService.getDidFromPublicKey(didPubKey);
if (null == did) {
    System.out.println("Err didService.getDidFromPublicKey failed.";
    return;
}
```
## packDidProperty
**String ElaDidService::packDidProperty(String didPrivateKey, String propertyKey, String propertyData);**
* Pack did property to a raw data which can be up to chain.

```Java
String rawData = ElaDidService.packDidProperty(didPrivateKey, didPropertyKey, didPropertyValue);
if (null == rawData) {
    System.out.println("Err didService.packDidProperty failed.");
    return;
}
System.out.println("DidService.packDidProperty rawData:" + rawData);
```

## packDidProperties
**String ElaDidService::packDidProperties(String didPrivateKey, Map<String, String> propertiesMap);**
* Pack did properties(Map(property_key->property_value)) to a raw data which can be up to chain.

```Java
String rawData = ElaDidService.packDidProperties(didPrivateKey, propertiesMap);
if (null == rawData) {
    System.out.println("Err didService.packDidProperties failed.");
    return;
}
System.out.println("DidService.packDidProperties rawData:" + rawData);
```

## packDelDidProperty
**ReturnMsgEntity ElaDidService::packDelDidProperty(String didPrivateKey, String propertyKey);**
* Delete a did property. payWalletPrivateKey is used to pay the record fee, didPrivateKey is used to sign and verify did data.

```Java
String rawData = ElaDidService.packDelDidProperty(didPrivateKey, didPropertyKey);
if (null == rawData) {
    System.out.println("Err didService.packDelDidProperty failed.");
    return;
}
```
## packDestroyDid
**String ElaDidService::packDestroyDid(String didPrivateKey);**
* Deprecated a did from did chain. If you send the raw data which is packed by this function on chain, you will not get any data of this did.

```Java
String rawData = ElaDidService.packDestroyDid(didPrivateKey);
if (null == rawData) {
    System.out.println("Err didService.packDestroyDid failed.");
    return;
}
```

## upChainByWallet
**String ElaDidService::upChainByWallet(String nodeUrl, String payWalletPrivateKey, String rawData);**
* Send raw data to did chain which is pointed by nodeUrl.

```Java
ReturnMsgEntity ret = didService.upChainByWallet(payPrivateKey, rawData);
long status = ret.getStatus();
if (status != RetCodeConfiguration.SUCC) {
    System.out.println("Err didService.upChainByWallet failed. retResult:" + JSON.toJSONString(ret.getResult()));
    return;
}
String txId = (String) ret.getResult();
```

## upChainByAgent
**String ElaDidService::upChainByAgent(String agentUrl, String accessId, String accessSecret, String rawData);**
* Send raw data to did chain by block chain agent service which is pointed by agentUrl.
* The accessId and accessSecret get from elastos baas platform. You can set null, if there is no need for some BLOCK CHAIN AGENT service.
```Java
String txid = ElaDidService.upChainByAgent(ELA_BLOCK_AGENT_URL, null, null, rawData);
if (null == txid) {
    System.out.println("Err didService.upChainByAgent failed.");
    return;
}
System.out.println("DidService.upChainByAgent txid:" + txid);
```

## getDidPropertyByTxid
**ReturnMsgEntity ElaDidService::getDidPropertyByTxid(String nodeUrl, String did, String propertyKey, String txId);**
* Get property value from did chain by transaction id (txId).

```Java
ReturnMsgEntity ret = didService.getDidPropertyByTxid(did, didPropertyKey, txId);
long status = ret.getStatus();
if (status != RetCodeConfiguration.SUCC) {
    System.out.println("Err didService.getDidPropertyByTxid failed. retResult:" + JSON.toJSONString(ret.getResult()));
    return;
}
String propertyJson = (String) ret.getResult();
String property = JSONObject.parseObject(propertyJson).getString(didPropertyKey);
System.out.println("DidService.getDidPropertyByTxid property:" + property)
```

## signMessage
**String ElaDidService::signMessage(String didPrivateKey, String msg);**
* Sign msg with did private key.

```java
String sig = ElaDidService.signMessage(didPrivateKey, didPropertyKey);
if (null == sig) {
    System.out.println("Err didService.signDidMessage failed.");
    return;
}
```

## verifyMessage
**boolean ElaDidService::verifyMessage(String didPublicKey, String sig, String msg);**
* Verify the signature with did public key.

```Java
boolean isVerify = ElaDidService.verifyMessage(didPublicKey, sig, didPropertyKey);
if (!isVerify) {
    System.out.println("Err didService.verifyDidMessage not right. retResult:");
    return;
}
```

