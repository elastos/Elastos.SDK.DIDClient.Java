package sample.com.upChain;

import com.alibaba.fastjson.JSON;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.elastos.service.ElaDidService;
import org.elastos.service.ElaDidServiceImp;
import sample.com.util.HttpUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class UpChainSample {

    //用户替换为所在链的对应节点信息
    private final String ELA_NODE_URL = "http://54.64.220.165:21334";
    //用户替换为 BLOCK AGENT 上链服务的URL
    private final String ELA_BLOCK_AGENT_URL = "http://localhost:8093";
    //用户替换为DID EXPLORER 服务的URL
    private final String ELA_EXPLORER_URL = "http://localhost:8091";

    //用户替换为自己的did信息
    private String didPrivateKey = "";
    private String didPublicKey = "";
    private String did = "";

    //用户替换为自己从亦来云服务平台上获取的access key信息
    private String acc_id = "unCZRceA8o7dbny";
    private String acc_secret = "qtvb4PlRVGLYYYQxyLIo3OgyKI7kUL";

    ElaDidService didService;

    public UpChainSample() {
        didService = new ElaDidServiceImp();
        didService.setElaNodeUrl(ELA_NODE_URL);
    }

    public void createDid() throws Exception {
        String ret = didService.createDid();
        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("DID");
        didPublicKey = (String) data.get("DidPublicKey");
    }

    public String packDidRawData(String key, String value) {
        String rawData = didService.packDidRawData(didPrivateKey, key, value);
        if (null == rawData) {
            System.out.println("Err didService.packDidRawData failed.");
            return null;
        }
        System.out.println("DidService.packDidRawData rawData:" + rawData);
        return rawData;
    }

    public String putDataToElaChain(String rawData) {

        Map<String, String> header = new HashMap<>();
        header.put("X-Elastos-Agent-Auth", createAuthHeaderValue());
        String response = HttpUtil.post(ELA_BLOCK_AGENT_URL + "/api/1/blockagent/upchain/data", rawData, header);
        if (null == response) {
            System.out.println("Err: putDataToElaChain post failed");
            return null;
        }

        Map<String, Object> msg = (Map<String, Object>) JSON.parse(response);
        if ((int) msg.get("status") == 200) {
            return (String) msg.get("result");
        } else {
            System.out.println("Err: block agent failed" + msg.get("result"));
            return null;
        }
    }

    public String createAuthHeaderValue() {
        long time = new Date().getTime();
        String strTime = String.valueOf(time);
        SimpleHash hash = new SimpleHash("md5", acc_secret, strTime);
        String auth = hash.toHex();
        Map<String, String> map = new HashMap<>();
        map.put("id", acc_id);
        map.put("time", String.valueOf(time));
        map.put("auth", auth);
        String X_Elastos_Agent_Auth_value = JSON.toJSONString(map);
        return X_Elastos_Agent_Auth_value;
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

        sample.createDid();

        String upChainDataKey = "My_notebooks";
        List<String> didProperty = new ArrayList<>();
        didProperty.add("Dell中文");
        didProperty.add("Mac中文");
        didProperty.add("Thinkpad中文");
        String upChainDataValue = JSON.toJSONString(didProperty);

        String rawData = sample.packDidRawData(upChainDataKey, upChainDataValue);

        sample.putDataToElaChain(rawData);

        //wait 3 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        String value = sample.getDataFromElaChain(upChainDataKey);
    }
}