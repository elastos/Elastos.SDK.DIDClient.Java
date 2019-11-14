package sample.com.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.elastos.constant.RetCode;
import org.elastos.service.ElaDidService;
import org.elastos.util.RetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class ElaDidServiceApiSample {
    private static Logger logger = LoggerFactory.getLogger(ElaDidServiceApiSample.class);
    String didNodeUrl = "http://54.64.220.165:21334";
    String payPrivateKey = "17f9885d36ce7c646cd1d613708e9b375f81b81309fbdfbd922d0cd72faadb1b";
    String payPublicKey = "035ADEF4A1566BD30B2A89327ECC3DE9B876F9624AEBEDDA7725A24816125CE261";
    String payPublicAddr = "EJqsNp9qSWkX7wkkKeKnqeubok6FxuA9un";
    String didPrivateKey = "";
    String didPublicKey = "";
    String did = "";
    ElaDidService didService = new ElaDidService(didNodeUrl, true);
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
        String memo = ElaDidService.createMnemonic();
        String ret = ElaDidService.createDid(memo, 0);
        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("Did");
        didPublicKey = (String) data.get("DidPublicKey");
    }

    public void signAndVerifyDidMessage() throws Exception {
        String msg = "This is a msg to be signature and verify";
        String sig = ElaDidService.signMessage(didPrivateKey, msg);
        if (null == sig) {
            System.out.println("Err didService.signDidMessage failed.");
            return;
        }

        boolean isVerify = ElaDidService.verifyMessage(didPublicKey, sig, msg);
        if (!isVerify) {
            System.out.println("Err didService.verifyDidMessage not right. result:");
            return;
        }
    }

    public void getPublicKey() throws Exception {
        String pubKey = ElaDidService.getDidPublicKey(didPrivateKey);
        if (null == pubKey) {
            System.out.println("Err didService.getDidPublicKey failed. result:");
            return;
        }

        System.out.println("DidService.getDidPublicKey pubKey:" + pubKey);
    }

    public void getDid() throws Exception {
        String did1 = ElaDidService.getDidFromPrivateKey(didPrivateKey);
        if (null == did1) {
            System.out.println("Err didService.getDidFromPrivateKey failed. result:");
            return;
        }
        String did2 = ElaDidService.getDidFromPublicKey(didPublicKey);
        if (null == did2) {
            System.out.println("Err didService.getDidFromPublicKey failed. result:");
            return;
        }
        if (!did1.equals(did2)) {
            System.out.println("Err didService.getDid failed. not equal!");

        }
    }

    public void setAndGetDidProperty() throws Exception {
        String rawData = ElaDidService.packDidProperty(didPrivateKey, didPropertyKey, didPropertyValue);
        if (null == rawData) {
            System.out.println("Err didService.packDidProperty failed.");
            return;
        }
        RetResult<String> ret = didService.upChainByWallet(payPrivateKey, rawData);
        long status = ret.getCode();
        if (status != RetCode.SUCC) {
            System.out.println("Err didService.upChainByWallet failed. result:" + JSON.toJSONString(ret.getData()));
            return;
        }

        String txId = ret.getData();

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        ret = didService.getDidPropertyByTxid(did, didPropertyKey, txId);
        status = ret.getCode();
        if (status != RetCode.SUCC) {
            System.out.println("Err didService.getDidPropertyByTxid failed. result:" + JSON.toJSONString(ret.getData()));
            return;
        }
        String propertyJson = ret.getData();
        String property = JSONObject.parseObject(propertyJson).getString(didPropertyKey);
        System.out.println("DidService.getDidPropertyByTxid property:" + property);
    }

    public void deleteDidProperty() throws Exception {
        String rawData = ElaDidService.packDelDidProperty(didPrivateKey, didPropertyKey);
        if (null == rawData) {
            System.out.println("Err didService.packDelDidProperty failed.");
            return;
        }
        RetResult<String> ret = didService.upChainByWallet(payPrivateKey, rawData);
        long status = ret.getCode();
        if (status != RetCode.SUCC) {
            System.out.println("Err didService.packDelDidProperty failed. result:" + JSON.toJSONString(ret.getData()));
            return;
        }

        String txId = ret.getData();
        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        ret = didService.getDidPropertyByTxid(did, didPropertyKey, txId);
        status = ret.getCode();
        System.out.println("Status:" + status + " result:" + ret.getCode());
    }

    public void deleteDid() throws Exception {
        String rawData = ElaDidService.packDestroyDid(didPrivateKey);
        if (null == rawData) {
            System.out.println("Err didService.packDestroyDid failed.");
            return;
        }
        RetResult<String> ret = didService.upChainByWallet(payPrivateKey, rawData);
        long status = ret.getCode();
        if (status != RetCode.SUCC) {
            System.out.println("Err didService.packDestroyDid failed. result:" + JSON.toJSONString(ret.getData()));
            return;
        }
        String txId = ret.getData();

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        ret = didService.getDidPropertyByTxid(did, didPropertyKey, txId);
        status = ret.getCode();
        System.out.println("Status:" + status + " result:" + ret.getData());
    }

    public void getDidProperty() throws Exception {
        RetResult<String> ret = didService.getDidPropertyByTxid("iYBNS46VsgLGgBmYptChzAMNhQtyAviYvj", "test2_key", "07f04f1af782b74247dcab71a55e58209dc5e5aac13a46b8aa3a761d45ce2e47");
        long status = ret.getCode();
        if (status != RetCode.SUCC) {
            System.out.println("Err didService.getDidPropertyByTxid failed. result:" + JSON.toJSONString(ret.getData()));
            return;
        }
        System.out.println("DidService.getDidPropertyByTxid property:" + ret.getData());
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
        sample.getDidProperty();
    }
}
