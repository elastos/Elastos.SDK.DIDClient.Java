package sample.com.upChain;

import com.alibaba.fastjson.JSON;
import org.elastos.service.ElaDidService;
import org.elastos.util.HttpUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class UpChainSample {

    //UserChange: BLOCK CHAIN AGENT service url
    private final String ELA_BLOCK_AGENT_URL = "https://api-wallet-did-testnet.elastos.org";    //测试DID链上链服务
    //UserChange: DID EXPLORER service url
    private final String ELA_EXPLORER_URL = "https://api-wallet-did-testnet.elastos.org";    //测试DID链浏览器服务

    //UserChange: your did info
    private String didPrivateKey = "02D0125FB262E3E7A7723394C3D8ADB86F68192329A4AE56A75F46B949566552";
    private String didPublicKey = "0245E44ACDF97CD8B676C064B373B4FDD246456F3B391FC21BAF515A4E49FD5E24";
    private String did = "ipHvyXxv9jkAjTDv8N9iSsiNtrgi1K4p1n";

    //UserChange: get from elastos baas platform. You can set null, if there is no need for some BLOCK CHAIN AGENT service
    private String acc_id = null;
    private String acc_secret = null;

    ElaDidService didService;

    public UpChainSample() {
        didService = new ElaDidService();
    }

    public String packDidRawData(String key, String value) {
        String rawData = didService.packDidProperty(didPrivateKey, key, value);
        if (null == rawData) {
            System.out.println("Err didService.packDidProperty failed.");
            return null;
        }
        System.out.println("DidService.packDidProperty rawData:" + rawData);
        return rawData;
    }

    public String putDataToElaChain(String rawData) {
        String ret = didService.upChainByAgent(ELA_BLOCK_AGENT_URL, null, null, rawData);
        return ret;
    }

    public String getDataFromElaChain(String key) {
        String response = HttpUtil.get(ELA_EXPLORER_URL + "/api/1/didexplorer/did/" + did + "/property?key=" + key, null);
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

        String upChainDataKey = "Apps/E9AC59878569C187DF42B602A8FCBC2F439CB8769C71D7A4ABE913ECCBE8FEA26DF7457BFCC478CC81A92780584990DEEF7776E2E33B604F1DE3FF62308E2121/vername";
        String upChainDataValue = "1.0.227";

        String rawData = sample.packDidRawData(upChainDataKey, upChainDataValue);

        sample.putDataToElaChain(rawData);

       //wait 3 minutes for info add on chain!!
       TimeUnit.MINUTES.sleep(4);

       String value = sample.getDataFromElaChain(upChainDataKey);
    }
}
