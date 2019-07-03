package org.elastos.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import junit.framework.TestCase;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.RetCodeConfiguration;
import org.elastos.ela.Ela;
import org.elastos.entity.ReturnMsgEntity;
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
//    String payPrivateKey = "FEDF4265E4B459074754C4A09420A278C7316959A48EA964263E86DECECEF232";
//    String payPublicKey = "033CAE6DF91E6A77313601FEF25BAB55B0A4362E399377B0B87661AE5A2CE95A81";
//    String payPublicAddr = "EZdDnKBRnV8o77gjr1M3mWBLZqLA3WBjB7";
    String didPrivateKey = "";
    String didPublicKey = "";
    String did = "";
    ElaDidService didService = new ElaDidService();
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
        String Mnemonic = didService.createMnemonic();
        assertNotNull(Mnemonic);

        String ret = didService.createDid(Mnemonic, 0);
        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("Did");
        didPublicKey = (String) data.get("DidPublicKey");
    }

    @Test
    public void testMnemonicDidCteate2() throws Exception {
        String Mnemonic = "abandon people pact bargain blush rack entire dirt resist damage joke fold";

        String ret = didService.createDid(Mnemonic, 0);
        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("Did");
        didPublicKey = (String) data.get("DidPublicKey");
    }


    @Test
    public void testDidCteate() throws Exception {
        String memo = didService.createMnemonic();
        String ret = didService.createDid(memo, 0);
        assertNotNull(ret);

        Map data = JSON.parseObject(ret, Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("Did");
        didPublicKey = (String) data.get("DidPublicKey");
    }

    @Test
    public void testSignAndVertifyDidMessage() throws Exception {
        String sig = didService.signMessage(didPrivateKey, didPropertyKey);
        assertNotNull(sig);

        boolean ret = didService.verifyMessage(didPublicKey, sig, didPropertyKey);
        assertTrue(ret);

        ret = didService.verifyMessage(didPublicKey, sig, "just for test string");
        assertTrue(!ret);
    }

    @Test
    public void testGetPublicKey() throws Exception {
        String pubKey = didService.getDidPublicKey(didPrivateKey);
        assertEquals("Err didService.getDidPublicKey not right", pubKey, didPublicKey);
    }

    @Test
    public void testGetDid() throws Exception {
        String did1 = didService.getDidFromPrivateKey(didPrivateKey);
        assertNotNull(did1);
        String did2 = didService.getDidFromPrivateKey(didPublicKey);
        assertNotNull(did2);
        assertEquals(did1, did2);
    }

    @Test
    public void testPackDidRawData() throws Exception {
        String ret = didService.packDidProperty(didPrivateKey, didPropertyKey, didPropertyValue);
        assertNotNull("Err didService.packDidProperty failed.", ret);
    }

    @Test
    public void testPackDidRawData2() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("test1_key", "test1_value");
        properties.put("test2_key", "test2_value");
        String ret = didService.packDidProperties(didPrivateKey, properties);
        assertNotNull("Err didService.packDidProperty failed. ", ret);
    }

    @Test
    public void testDidPropertyBasicSetAndGet() throws Exception {

        String rawData = didService.packDidProperty(didPrivateKey, didPropertyKey, didPropertyValue);
        assertNotNull("Err didService.setDidProperty failed.", rawData);
        ReturnMsgEntity ret = didService.upChainByWallet(didNodeUrl, payPrivateKey, rawData);
        long status = ret.getStatus();
        assertEquals("Err didService.upChainByWallet failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        //wait 3 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(3);

        String txId = (String) ret.getResult();
        ret = didService.getDidPropertyByTxid(didNodeUrl, did, didPropertyKey, txId);
        status = ret.getStatus();
        assertEquals("Err didService.getDidPropertyByTxid failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);
        String propertyJson = (String) ret.getResult();
        String property = JSONObject.parseObject(propertyJson).getString(didPropertyKey);
        assertEquals("Err didService.getDidPropertyByTxid failed. result:" + propertyJson, property, didPropertyValue);
    }

    @Test
    public void testDidPropertiesBasicSetAndGet() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("test1_key", "test1_value");
        properties.put("test2_key", "test2_value");
        String rawData = didService.packDidProperties(didPrivateKey, properties);
        assertNotNull("Err didService.setDidProperties failed.", rawData);
        ReturnMsgEntity ret = didService.upChainByWallet(didNodeUrl, payPrivateKey, rawData);
        long status = ret.getStatus();
        assertEquals("Err didService.upChainByWallet failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        //wait 3 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(3);

        String txId = (String) ret.getResult();
        ret = didService.getDidPropertyByTxid(didNodeUrl, did, "test2_key", txId);
        status = ret.getStatus();
        assertEquals("Err didService.getDidPropertyByTxid failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);
        String propertyJson = (String) ret.getResult();
        String property = JSONObject.parseObject(propertyJson).getString("test2_key");
        assertEquals("Err didService.getDidPropertyByTxid failed. result:" + propertyJson, property, "test2_value");
    }


    @Test
    public void testDidPropertyDelete() throws Exception {
        String rawData = didService.packDelDidProperty(didPrivateKey, didPropertyKey);
        assertNotNull("Err didService.setDidProperties failed.", rawData);
        ReturnMsgEntity ret = didService.upChainByWallet(didNodeUrl, payPrivateKey, rawData);
        long status = ret.getStatus();
        assertEquals("Err didService.packDelDidProperty failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        String txId = (String) ret.getResult();
        ret = didService.getDidPropertyByTxid(didNodeUrl, did, didPropertyKey, txId);
        status = ret.getStatus();
        assertEquals("Err after del did property, didService.getDidPropertyByTxid should not get info. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.NOT_FOUND);
    }

    @Test
    public void testDidDelete() throws Exception {
        String rawData = didService.packDestroyDid(didPrivateKey);
        assertNotNull("Err didService.setDidProperties failed.", rawData);
        ReturnMsgEntity ret = didService.upChainByWallet(didNodeUrl, payPrivateKey, rawData);
        long status = ret.getStatus();
        assertEquals("Err didService.packDelDidProperty failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        String txId = (String) ret.getResult();
        ret = didService.getDidPropertyByTxid(didNodeUrl, did, didPropertyKey, txId);
        status = ret.getStatus();
        assertEquals("Err after del did property, didService.getDidPropertyByTxid should not get info. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.NOT_FOUND);
    }

    @Test
    public void testTransferEla() throws Exception {

        List<String> payPrivateKeys = new ArrayList<>();
        payPrivateKeys.add(payPrivateKey);
        Map<String, Double> dstAddrAndEla = new HashMap<>();
//        dstAddrAndEla.put("EJqsNp9qSWkX7wkkKeKnqeubok6FxuA9un", 0.01);
        dstAddrAndEla.put("EZdDnKBRnV8o77gjr1M3mWBLZqLA3WBjB7", 0.01);
        ReturnMsgEntity ret = didService.transferEla(didNodeUrl, ElaChainType.DID_CHAIN, payPrivateKeys, ElaChainType.DID_CHAIN, dstAddrAndEla);
        long status = ret.getStatus();
        assertEquals("Err didService.transferEla failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);
    }
}