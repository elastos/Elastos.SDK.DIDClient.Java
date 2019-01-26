package org.elastos.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import junit.framework.TestCase;
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
    String payPrivateKey = "17f9885d36ce7c646cd1d613708e9b375f81b81309fbdfbd922d0cd72faadb1b";
    String payPublicKey = "035ADEF4A1566BD30B2A89327ECC3DE9B876F9624AEBEDDA7725A24816125CE261";
    String payPublicAddr = "EJqsNp9qSWkX7wkkKeKnqeubok6FxuA9un";
    String didPrivateKey = "";
    String didPublicKey = "";
    String did = "";
    ElaDidService didService = new ElaDidServiceImp();
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
    public void testDidCteate() throws Exception {
        ReturnMsgEntity ret = didService.createDid();
        long status = ret.getStatus();
        assertEquals("Err didService.createDid failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        Map data = JSON.parseObject((String) ret.getResult(), Map.class);
        didPrivateKey = (String) data.get("DidPrivateKey");
        did = (String) data.get("DID");
        didPublicKey = (String) data.get("DidPublicKey");
    }

    @Test
    public void testSignAndVertifyDidMessage() throws Exception {
        ReturnMsgEntity ret = didService.signDidMessage(didPrivateKey, didPropertyKey);
        long status = ret.getStatus();
        assertEquals("Err didService.signDidMessage failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);
        String sig = (String) ret.getResult();

        ret = didService.verifyDidMessage(didPublicKey, sig, didPropertyKey);
        status = ret.getStatus();
        assertEquals("Err didService.verifyDidMessage failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        Boolean isVertify = (Boolean) ret.getResult();
        assertTrue("Err didService.verifyDidMessage not right. result:" + JSON.toJSONString(ret.getResult()), isVertify);

        ret = didService.verifyDidMessage(didPublicKey, sig, "just for test string");
        status = ret.getStatus();
        assertEquals("Err didService.verifyDidMessage failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        isVertify = (Boolean) ret.getResult();
        assertTrue("Err didService.verifyDidMessage not right. result:" + JSON.toJSONString(ret.getResult()), !isVertify);

    }

    @Test
    public void testGetPublicKey() throws Exception {
        ReturnMsgEntity ret = didService.getDidPublicKey(didPrivateKey);
        long status = ret.getStatus();
        assertEquals("Err didService.getDidPublicKey failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);
        String pubKey = (String) ret.getResult();
        assertEquals("Err didService.getDidPublicKey not right. result:" + JSON.toJSONString(ret.getResult()), pubKey, didPublicKey);
    }

    @Test
    public void testGetDid() throws Exception {
        ReturnMsgEntity ret = didService.getDid(didPrivateKey);
        long status = ret.getStatus();
        assertEquals("Err didService.getDid failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);
        String id = (String) ret.getResult();
        assertEquals("Err didService.getDid not right. result:" + JSON.toJSONString(ret.getResult()), id, did);
    }

    @Test
    public void testPackDidRawData() throws Exception {
        String ret = didService.packDidRawData(didPrivateKey, didPropertyKey, didPropertyValue);
        assertNotNull("Err didService.packDidRawData failed.", ret);
    }

    @Test
    public void testPackDidRawData2() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("test1_key", "test1_value");
        properties.put("test2_key", "test2_value");
        String ret = didService.packDidRawData(didPrivateKey, properties);
        assertNotNull("Err didService.packDidRawData failed. ", ret);
    }

    @Test
    public void testDidPropertyBasicSetAndGet() throws Exception {

        ReturnMsgEntity ret = didService.setDidProperty(payPrivateKey, didPrivateKey, didPropertyKey, didPropertyValue);
        long status = ret.getStatus();
        assertEquals("Err didService.setDidProperty failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        String txId = (String) ret.getResult();
        ret = didService.getDidPropertyByTxId(did, didPropertyKey, txId);
        status = ret.getStatus();
        assertEquals("Err didService.getDidPropertyByTxId failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);
        String propertyJson = (String) ret.getResult();
        String property = JSONObject.parseObject(propertyJson).getString(didPropertyKey);
        assertEquals("Err didService.getDidPropertyByTxId failed. result:" + propertyJson, property, didPropertyValue);
    }

    @Test
    public void testDidPropertiesBasicSetAndGet() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("test1_key", "test1_value");
        properties.put("test2_key", "test2_value");
        ReturnMsgEntity ret = didService.setDidProperties(payPrivateKey, didPrivateKey,properties);
        long status = ret.getStatus();
        assertEquals("Err didService.setDidProperty failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        //wait 4 minutes for info add on chain!!
//        TimeUnit.MINUTES.sleep(4);

        String txId = (String) ret.getResult();
        ret = didService.getDidPropertyByTxId(did, "test2_key", txId);
        status = ret.getStatus();
        assertEquals("Err didService.getDidPropertyByTxId failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);
        String propertyJson = (String) ret.getResult();
        String property = JSONObject.parseObject(propertyJson).getString("test2_key");
        assertEquals("Err didService.getDidPropertyByTxId failed. result:" + propertyJson, property, "test2_value");
    }


    @Test
    public void testDidPropertyDelete() throws Exception {
        ReturnMsgEntity ret = didService.delDidProperty(payPrivateKey, didPrivateKey, didPropertyKey);
        long status = ret.getStatus();
        assertEquals("Err didService.delDidProperty failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        String txId = (String) ret.getResult();
        ret = didService.getDidPropertyByTxId(did, didPropertyKey, txId);
        status = ret.getStatus();
        assertEquals("Err after del did property, didService.getDidPropertyByTxId should not get info. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.NOT_FOUND);

    }

    @Test
    public void testDidDelete() throws Exception {
        ReturnMsgEntity ret = didService.destroyDid(payPrivateKey, didPrivateKey);
        long status = ret.getStatus();
        assertEquals("Err didService.delDidProperty failed. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.SUCC);

        //wait 4 minutes for info add on chain!!
        TimeUnit.MINUTES.sleep(4);

        String txId = (String) ret.getResult();
        ret = didService.getDidPropertyByTxId(did, didPropertyKey, txId);
        status = ret.getStatus();
        assertEquals("Err after del did property, didService.getDidPropertyByTxId should not get info. result:" + JSON.toJSONString(ret.getResult()), status, RetCodeConfiguration.NOT_FOUND);
    }
}