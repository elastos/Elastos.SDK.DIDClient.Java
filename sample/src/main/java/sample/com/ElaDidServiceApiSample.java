package sample.com;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.elastos.conf.RetCodeConfiguration;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.service.ElaDidService;
import org.elastos.service.ElaDidServiceImp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class ElaDidServiceApiSample {
    String payPrivateKey = "17f9885d36ce7c646cd1d613708e9b375f81b81309fbdfbd922d0cd72faadb1b";
    String payPublicKey = "035ADEF4A1566BD30B2A89327ECC3DE9B876F9624AEBEDDA7725A24816125CE261";
    String payPublicAddr = "EJqsNp9qSWkX7wkkKeKnqeubok6FxuA9un";
    String didPrivateKey = "";
    String didPublicKey = "";
    String did = "";
    ElaDidService didService = new ElaDidServiceImp();
    String didPropertyKey;
    String didPropertyValue;

    public ElaDidServiceApiSample() {
        didPropertyKey = "my_notebooks";
        List<String> didProperty = new ArrayList<>();
        didProperty.add("Dell");
        didProperty.add("Mac");
        didProperty.add("Thinkpad");
        didPropertyValue = JSON.toJSONString(didProperty);

    }

    public void createDid() throws Exception {
        ReturnMsgEntity ret = didService.createDid();
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.createDid failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }

        Map data = JSON.parseObject((String) ret.getResult(), Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("DID");
        didPublicKey = (String) data.get("DidPublicKey");
    }

    public void signAndVerifyDidMessage() throws Exception {
        ReturnMsgEntity ret = didService.signDidMessage(didPrivateKey, didPropertyKey);
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.signDidMessage failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }
        String sig = (String) ret.getResult();

        ret = didService.verifyDidMessage(didPublicKey, sig, didPropertyKey);
        status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.verifyDidMessage failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }

        Boolean isVerify = (Boolean) ret.getResult();
        if (!isVerify) {
            System.out.println("Err didService.verifyDidMessage not right. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }
    }

    public void getPublicKey() throws Exception {
        ReturnMsgEntity ret = didService.getDidPublicKey(didPrivateKey);
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.getDidPublicKey failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }

        String pubKey = (String) ret.getResult();
        System.out.println("DidService.getDidPublicKey pubKey:" + pubKey);
    }

    public void getDid() throws Exception {
        ReturnMsgEntity ret = didService.getDid(didPrivateKey);
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.getDid failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }
        String id = (String) ret.getResult();
        System.out.println("DidService.getDidPublicKey id:" + id);
    }

    public void packDidRawData() throws Exception {
        ReturnMsgEntity ret = didService.packDidRawData(didPrivateKey, didPropertyKey, didPropertyValue);
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.packDidRawData failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }
        String rawData = (String) ret.getResult();
        System.out.println("DidService.packDidRawData rawData:" + rawData);
    }

    public void setAndGetDidProperty() throws Exception {
        ReturnMsgEntity ret = didService.setDidProperty(payPrivateKey, didPrivateKey, didPropertyKey, didPropertyValue);
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.setDidProperty failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }

        String txId = (String) ret.getResult();

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        ret = didService.getDidPropertyByTxId(did, didPropertyKey, txId);
        status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.getDidPropertyByTxId failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }
        String propertyJson = (String) ret.getResult();
        String property = JSONObject.parseObject(propertyJson).getString(didPropertyKey);
        System.out.println("Err didService.getDidPropertyByTxId property:" + property);
    }

    public void deleteDidProperty() throws Exception {
        ReturnMsgEntity ret = didService.delDidProperty(payPrivateKey, didPrivateKey, didPropertyKey);
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.delDidProperty failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }

        String txId = (String) ret.getResult();
        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        ret = didService.getDidPropertyByTxId(did, didPropertyKey, txId);
        status = ret.getStatus();
        System.out.println("Status:" + status + " result:" + ret.getResult());
    }

    public void deleteDid() throws Exception {
        ReturnMsgEntity ret = didService.destroyDid(payPrivateKey, didPrivateKey);
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.destroyDid failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }
        String txId = (String) ret.getResult();

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        ret = didService.getDidPropertyByTxId(did, didPropertyKey, txId);
        status = ret.getStatus();
        System.out.println("Status:" + status + " result:" + ret.getResult());
    }


    public final static void main(String[] args) throws Exception {
        ElaDidServiceApiSample sample = new ElaDidServiceApiSample();
        sample.createDid();
        sample.getPublicKey();
        sample.getDid();
        sample.signAndVerifyDidMessage();
        sample.packDidRawData();
        sample.setAndGetDidProperty();
        sample.deleteDidProperty();
        sample.deleteDid();
    }

}
