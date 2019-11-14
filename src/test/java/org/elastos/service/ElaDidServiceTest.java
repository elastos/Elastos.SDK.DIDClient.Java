package org.elastos.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import junit.framework.TestCase;
import org.elastos.POJO.ElaChainType;
import org.elastos.constant.RetCode;
import org.elastos.ela.Ela;
import org.elastos.util.RetResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ElaDidServiceTest extends TestCase {
    String didNodeUrl = "http://54.64.220.165:21334";

    String payPrivateKey = "17f9885d36ce7c646cd1d613708e9b375f81b81309fbdfbd922d0cd72faadb1b";
    String payPublicKey = "035ADEF4A1566BD30B2A89327ECC3DE9B876F9624AEBEDDA7725A24816125CE261";
    String payPublicAddr = "EJqsNp9qSWkX7wkkKeKnqeubok6FxuA9un";

    String didPrivateKey = "";
    String didPublicKey = "";
    String did = "";
    ElaDidService didService;
    String didPropertyKey;
    String didPropertyValue;

    private void createNewDid() {
        didPrivateKey = Ela.getPrivateKey();
        System.out.println(didPrivateKey);
        did = Ela.getIdentityIDFromPrivate(didPrivateKey);
        System.out.println(did);
        didPublicKey = Ela.getPublicFromPrivate(didPrivateKey);
        System.out.println(didPublicKey);
    }

    @Before
    public void setUp() throws Exception {
        didService = new ElaDidService(didNodeUrl, true);

        createNewDid();

        didPropertyKey = "my_notebooks";
        List<String> didProperty = new ArrayList<>();
        didProperty.add("Dell");
        didProperty.add("Mac");
        didProperty.add("Thinkpad");
        didPropertyValue = JSON.toJSONString(didProperty);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testMnemonicDidCteate() throws Exception {
        String Mnemonic = ElaDidService.createMnemonic();
        assertNotNull(Mnemonic);

        String ret = ElaDidService.createDid(Mnemonic, 0);
        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("Did");
        didPublicKey = (String) data.get("DidPublicKey");
    }

    @Test
    public void testMnemonicDidCteate2() throws Exception {
        String Mnemonic = "abandon people pact bargain blush rack entire dirt resist damage joke fold";

        String ret = ElaDidService.createDid(Mnemonic, 0);
        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("Did");
        didPublicKey = (String) data.get("DidPublicKey");
    }


    @Test
    public void testDidCteate() throws Exception {
        String memo = ElaDidService.createMnemonic();
        String ret = ElaDidService.createDid(memo, 0);
        assertNotNull(ret);

        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("Did");
        didPublicKey = (String) data.get("DidPublicKey");
    }

    @Test
    public void testSignAndVertifyDidMessage() throws Exception {
        String sig = ElaDidService.signMessage(didPrivateKey, didPropertyKey);
        assertNotNull(sig);

        boolean ret = ElaDidService.verifyMessage(didPublicKey, sig, didPropertyKey);
        assertTrue(ret);

        ret = ElaDidService.verifyMessage(didPublicKey, sig, "just for test string");
        assertTrue(!ret);
    }

    @Test
    public void testGetPublicKey() throws Exception {
        String pubKey = ElaDidService.getDidPublicKey(didPrivateKey);
        assertEquals("Err didService.getDidPublicKey not right", pubKey, didPublicKey);
    }

    @Test
    public void testGetDid() throws Exception {
        String did1 = ElaDidService.getDidFromPrivateKey(didPrivateKey);
        assertNotNull(did1);
        String did2 = ElaDidService.getDidFromPublicKey(didPublicKey);
        assertNotNull(did2);
        assertEquals(did1, did2);
    }

    @Test
    public void testPackDidRawData() throws Exception {
        String ret = ElaDidService.packDidProperty(didPrivateKey, didPropertyKey, didPropertyValue);
        assertNotNull("Err didService.packDidProperty failed.", ret);
    }

    @Test
    public void testPackDidRawData2() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("test1_key", "test1_value");
        properties.put("test2_key", "test2_value");
        String ret = ElaDidService.packDidProperties(didPrivateKey, properties);
        assertNotNull("Err didService.packDidProperty failed. ", ret);
    }

    @Test
    public void testDidPropertyBasicSetAndGet() throws Exception {

        String rawData = ElaDidService.packDidProperty(didPrivateKey, didPropertyKey, didPropertyValue);
        assertNotNull("Err didService.setDidProperty failed.", rawData);
        RetResult ret = didService.upChainByWallet(payPrivateKey, rawData);
        long status = ret.getCode();
        assertEquals("Err didService.upChainByWallet failed. result:" + JSON.toJSONString(ret.getMsg()), status, RetCode.SUCC);

        //wait 3 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(3);

        String txId = (String) ret.getData();
        ret = didService.getDidPropertyByTxid(did, didPropertyKey, txId);
        status = ret.getCode();
        assertEquals("Err didService.getDidPropertyByTxid failed. result:" + JSON.toJSONString(ret.getMsg()), status, RetCode.SUCC);
        String propertyJson = (String) ret.getData();
        String property = JSONObject.parseObject(propertyJson).getString(didPropertyKey);
        assertEquals("Err didService.getDidPropertyByTxid failed. result:" + propertyJson, property, didPropertyValue);
    }

    @Test
    public void testDidPropertiesBasicSetAndGet() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("test1_key", "test1_value");
        properties.put("test2_key", "test2_value");
        String rawData = ElaDidService.packDidProperties(didPrivateKey, properties);
        assertNotNull("Err didService.setDidProperties failed.", rawData);
        RetResult ret = didService.upChainByWallet(payPrivateKey, rawData);
        long status = ret.getCode();
        assertEquals("Err didService.upChainByWallet failed. result:" + JSON.toJSONString(ret.getMsg()), status, RetCode.SUCC);

        //wait 3 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(3);

        String txId = (String) ret.getData();
        ret = didService.getDidPropertyByTxid(did, "test2_key", txId);
        status = ret.getCode();
        assertEquals("Err didService.getDidPropertyByTxid failed. result:" + JSON.toJSONString(ret.getMsg()), status, RetCode.SUCC);
        String propertyJson = (String) ret.getData();
        String property = JSONObject.parseObject(propertyJson).getString("test2_key");
        assertEquals("Err didService.getDidPropertyByTxid failed. result:" + propertyJson, property, "test2_value");
    }


    @Test
    public void testDidPropertyDelete() throws Exception {
        String rawData = ElaDidService.packDelDidProperty(didPrivateKey, didPropertyKey);
        assertNotNull("Err didService.setDidProperties failed.", rawData);
        RetResult ret = didService.upChainByWallet(payPrivateKey, rawData);
        long status = ret.getCode();
        assertEquals("Err didService.packDelDidProperty failed. result:" + JSON.toJSONString(ret.getMsg()), status, RetCode.SUCC);

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        String txId = (String) ret.getData();
        ret = didService.getDidPropertyByTxid(did, didPropertyKey, txId);
        status = ret.getCode();
        assertEquals("Err after del did property, didService.getDidPropertyByTxid should not get info. result:" + JSON.toJSONString(ret.getMsg()), status, RetCode.NOT_FOUND);
    }

    @Test
    public void testDidDelete() throws Exception {
        String rawData = ElaDidService.packDestroyDid(didPrivateKey);
        assertNotNull("Err didService.setDidProperties failed.", rawData);
        RetResult<String> ret = didService.upChainByWallet(payPrivateKey, rawData);
        long status = ret.getCode();
        assertEquals("Err didService.packDelDidProperty failed. result:" + JSON.toJSONString(ret.getMsg()), status, RetCode.SUCC);

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        String txId = ret.getData();
        ret = didService.getDidPropertyByTxid(did, didPropertyKey, txId);
        status = ret.getCode();
        assertEquals("Err after del did property, didService.getDidPropertyByTxid should not get info. result:" + JSON.toJSONString(ret.getMsg()), status, RetCode.NOT_FOUND);
    }
}