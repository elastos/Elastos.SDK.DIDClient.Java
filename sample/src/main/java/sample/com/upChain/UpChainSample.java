package sample.com.upChain;

import com.alibaba.fastjson.JSON;
import org.elastos.ela.Ela;
import org.elastos.service.ElaDidService;
import org.elastos.util.HttpUtil;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class UpChainSample {

    //UserChange: Elastos did chain service url
//    private final String ELA_DID_SERVICE_URL = "https://api-wallet-did-testnet.elastos.org";    //ELASTOS DID链 测试网服务 URL
    private final String ELA_DID_SERVICE_URL = "https://api-wallet-did.elastos.org";    //ELASTOS DID链 测试网服务 URL

    //UserChange: your did info
    private String didPrivateKey = "3C7BD426718F9534EC77E9183C06AA8DEE0B538866C9D66A6049A87C7B2415CD";
    private String didPublicKey = "02C6A4571B21AFE6AD62E127CF25C0955C97817060E5DBC704D15D03B358CD67D8";
    private String did = "iY2UzLhBPCxxTs9BfwHyGQWQiHo7u4FECM";

    //UserChange: get from elastos baas platform. You can set null, if there is no need for some BLOCK CHAIN AGENT service
    private String acc_id = null;
    private String acc_secret = null;

    public String packDidRawData(String key, String value) {
        String rawData = ElaDidService.packDidProperty(didPrivateKey, key, value);
        if (null == rawData) {
            System.out.println("Err didService.packDidProperty failed.");
            return null;
        }
        System.out.println("DidService.packDidProperty rawData:" + rawData);
        return rawData;
    }

    public String putDataToElaChain(String rawData) {
        String ret = ElaDidService.upChainByAgent(ELA_DID_SERVICE_URL, null, null, rawData);
        return ret;
    }

    public String getDataFromElaChain(String key) {
        String url = ELA_DID_SERVICE_URL + "/api/1/didexplorer/did/" + did + "/property?key=";
        try {
            url += java.net.URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String response = HttpUtil.get(url, null);
        if (null == response) {
            System.out.print("Err: no response.");
            return null;
        }
        Map<String, Object> msg = (Map<String, Object>) JSON.parse(response);
        if ((int) msg.get("status") == 200) {
            return (String) msg.get("result");
        } else {
            System.out.println("Err: getDataFromElaChain failed" + msg.get("result"));
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        UpChainSample sample = new UpChainSample();

        String upChainDataKey = "PublicKey";
        String upChainDataValue = "02C6A4571B21AFE6AD62E127CF25C0955C97817060E5DBC704D15D03B358CD67D8";

        String rawData = sample.packDidRawData(upChainDataKey, upChainDataValue);

        sample.putDataToElaChain(rawData);

       //wait 3 minutes for info add on chain!!
       TimeUnit.MINUTES.sleep(4);

       String value = sample.getDataFromElaChain(upChainDataKey);
    }
}
