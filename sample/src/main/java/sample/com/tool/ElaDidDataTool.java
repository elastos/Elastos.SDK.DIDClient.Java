package sample.com.tool;

import com.alibaba.fastjson.JSON;
import org.elastos.service.ElaDidService;
import org.elastos.service.ElaDidServiceImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class ElaDidDataTool {

    String appName = "";
    String appId = "";
    String didMnemonic = "";
    String didPrivateKey = "";
    String didPublicKey = "";
    String did = "";

    ElaDidService didService = new ElaDidServiceImp();

    public void createAppInfoAndDid(String inAppName) throws Exception {
        didMnemonic = didService.createDidMnemonic();

        String ret = didService.createDidByMnemonic(didMnemonic);
        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("DID");
        didPublicKey = (String) data.get("DidPublicKey");

        appName = inAppName;
        appId = didService.signDidMessage(didPrivateKey, appName);
    }

    public void showData(){
        System.out.println("appName:" + appName);
        System.out.println("appId:"+appId);
        System.out.println("didMnemonic:" + didMnemonic);
        System.out.println("didPrivateKey:"+didPrivateKey);
        System.out.println("didPublicKey:"+didPublicKey);
        System.out.println("did:"+did);
    }

    public static void main(String[] args) throws Exception {
        List<String> params = new ArrayList<>();
        params.addAll(Arrays.asList(args));

        if (params.isEmpty()) {
            System.out.println("Should add app name in parameter");
        }

        ElaDidDataTool tool = new ElaDidDataTool();
        tool.createAppInfoAndDid(params.get(0));
        tool.showData();
    }
}
