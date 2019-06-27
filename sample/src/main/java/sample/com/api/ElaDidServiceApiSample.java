package sample.com.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.elastos.conf.RetCodeConfiguration;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.service.ElaDidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ElaDidServiceApiSample {
    private static Logger logger = LoggerFactory.getLogger(ElaDidServiceApiSample.class);

    String didNodeUrl = "http://54.64.220.165:21334";
    String payPrivateKey = "17f9885d36ce7c646cd1d613708e9b375f81b81309fbdfbd922d0cd72faadb1b";
    String payPublicKey = "035ADEF4A1566BD30B2A89327ECC3DE9B876F9624AEBEDDA7725A24816125CE261";
    String payPublicAddr = "EJqsNp9qSWkX7wkkKeKnqeubok6FxuA9un";
    String didPrivateKey = "";
    String didPublicKey = "";
    String did = "";
    ElaDidService didService = new ElaDidService();
    String didPropertyKey;
    String didPropertyValue;

    public ElaDidServiceApiSample() {
        didPropertyKey = "中文my_notebooks";
        List<String> didProperty = new ArrayList<>();
        didProperty.add("Dell中文");
        didProperty.add("中学Mac");
        didProperty.add("Thinkpad");
        didPropertyValue = JSON.toJSONString(didProperty);
    }

    public void createDid() throws Exception {
        String memo = didService.createMnemonic();
        String ret = didService.createDid(memo, 0);
        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("Did");
        didPublicKey = (String) data.get("DidPublicKey");
    }

    public void signAndVerifyDidMessage() throws Exception {
        String sig = didService.signMessage(didPrivateKey, didPropertyKey);
        if (null == sig) {
            System.out.println("Err didService.signDidMessage failed.");
            return;
        }

        boolean isVerify = didService.verifyMessage(didPublicKey, sig, didPropertyKey);
        if (!isVerify) {
            System.out.println("Err didService.verifyDidMessage not right. result:");
            return;
        }
    }

    public void getPublicKey() throws Exception {
        String pubKey = didService.getDidPublicKey(didPrivateKey);
        if (null == pubKey) {
            System.out.println("Err didService.getDidPublicKey failed. result:");
            return;
        }

        System.out.println("DidService.getDidPublicKey pubKey:" + pubKey);
    }

    public void getDid() throws Exception {
        String did1 = didService.getDidFromPrivateKey(didPrivateKey);
        if (null == did1) {
            System.out.println("Err didService.getDidFromPrivateKey failed. result:");
            return;
        }
        String did2 = didService.getDidFromPublicKey(didPublicKey);
        if (null == did2) {
            System.out.println("Err didService.getDidFromPublicKey failed. result:");
            return;
        }
        if (!did1.equals(did2)) {
            System.out.println("Err didService.getDid failed. not equal!");

        }
    }

    public void setAndGetDidProperty() throws Exception {
        String rawData = didService.packDidProperty(didPrivateKey, didPropertyKey, didPropertyValue);
        if (null == rawData) {
            System.out.println("Err didService.packDidProperty failed.");
            return;
        }
        ReturnMsgEntity ret = didService.upChainByWallet(didNodeUrl, payPrivateKey, rawData);
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.upChainByWallet failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }

        String txId = (String) ret.getResult();

        //wait 4 minutes for info add on chain!!
//        TimeUnit.MINUTES.sleep(4);

        ret = didService.getDidPropertyByTxid(didNodeUrl, did, didPropertyKey, txId);
        status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.getDidPropertyByTxid failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }
        String propertyJson = (String) ret.getResult();
        String property = JSONObject.parseObject(propertyJson).getString(didPropertyKey);
        System.out.println("DidService.getDidPropertyByTxid property:" + property);
    }

    public void deleteDidProperty() throws Exception {
        String rawData = didService.packDelDidProperty(didPrivateKey, didPropertyKey);
        if (null == rawData) {
            System.out.println("Err didService.packDelDidProperty failed.");
            return;
        }
        ReturnMsgEntity ret = didService.upChainByWallet(didNodeUrl, payPrivateKey, rawData);
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.packDelDidProperty failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }

        String txId = (String) ret.getResult();
        //wait 4 minutes for info add on chain!!
//        TimeUnit.MINUTES.sleep(4);

        ret = didService.getDidPropertyByTxid(didNodeUrl, did, didPropertyKey, txId);
        status = ret.getStatus();
        System.out.println("Status:" + status + " result:" + ret.getResult());
    }

    public void deleteDid() throws Exception {
        String rawData = didService.packDestroyDid(didPrivateKey);
        if (null == rawData) {
            System.out.println("Err didService.packDestroyDid failed.");
            return;
        }
        ReturnMsgEntity ret = didService.upChainByWallet(didNodeUrl, payPrivateKey, rawData);
        long status = ret.getStatus();
        if (status != RetCodeConfiguration.SUCC) {
            System.out.println("Err didService.packDestroyDid failed. result:" + JSON.toJSONString(ret.getResult()));
            return;
        }
        String txId = (String) ret.getResult();

        //wait 4 minutes for info add on chain!!
//        TimeUnit.MINUTES.sleep(4);

        ret = didService.getDidPropertyByTxid(didNodeUrl, did, didPropertyKey, txId);
        status = ret.getStatus();
        System.out.println("Status:" + status + " result:" + ret.getResult());
    }

    public static void main(String[] args) throws Exception {
        ElaDidServiceApiSample sample = new ElaDidServiceApiSample();
        sample.createDid();
        sample.getPublicKey();
        sample.getDid();
        sample.signAndVerifyDidMessage();
        sample.setAndGetDidProperty();
        sample.deleteDidProperty();
        sample.deleteDid();
    }
}
