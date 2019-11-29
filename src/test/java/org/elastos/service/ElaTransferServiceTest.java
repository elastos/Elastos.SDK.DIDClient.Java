package org.elastos.service;

import org.elastos.POJO.Credentials;
import org.elastos.POJO.ElaChainType;
import org.elastos.constant.RetCode;
import org.elastos.util.RetResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ElaTransferServiceTest {
    private ElaTransferService elaService;
    private ElaTransferService didService;
    private ElaTransferService ethService;

    private String elaUrl = "http://54.64.220.165:21334";
    private String didUrl = "http://54.64.220.165:21604";
    private String ethUrl = "http://rpc.elaeth.io:8545";

    private String elaPrivateKey = "FEDF4265E4B459074754C4A09420A278C7316959A48EA964263E86DECECEF232";
    private String elaPublicAddr = "EZdDnKBRnV8o77gjr1M3mWBLZqLA3WBjB7";

    private String didPrivateKey = "17f9885d36ce7c646cd1d613708e9b375f81b81309fbdfbd922d0cd72faadb1b";
    private String didPublicAddr = "EJqsNp9qSWkX7wkkKeKnqeubok6FxuA9un";

    private String ethPrivateKey = "3e7bd30c5dd15e50e31c3d59a0db08d54c38bb4349406ce52149e732dcbe3914";
    private String ethPublicAddr = "0xb3597A4Ed6aA224dF9741322805F9C8BDC6Ab9A4";

    private String elaMemo = "lamp laugh fatal victory tonight utility spray curve lazy ticket vessel happy";
    private String didMemo = "gentle total soda metal reform bind indicate into egg calm prize chronic";
    private String ethMemo = "gas step decrease weekend vivid burst donate describe mention suspect shoe idea";

    @Before
    public void setUp() throws Exception {
        elaService = ElaTransferService.getInstance(ElaChainType.ELA_CHAIN, elaUrl, true);
        didService = ElaTransferService.getInstance(ElaChainType.DID_CHAIN, didUrl, true);
        ethService = ElaTransferService.getInstance(ElaChainType.ETH_CHAIN, ethUrl, true);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getInstance() throws Exception {
    }

    @Test
    public void createMnemonic() throws Exception {
        for(int i=0; i < 10; i++){
            String inelaMemo = elaService.createMnemonic();
            System.out.println("ela memo:" + inelaMemo);
            String indidMemo = didService.createMnemonic();
            System.out.println("did memo:" + indidMemo);
            String inethMemo = ethService.createMnemonic();
            System.out.println("eth memo:" + inethMemo);
        }
    }

    @Test
    public void geneCredentials() throws Exception {
        for(int i=0; i < 10; i++){

            RetResult<Credentials> elaResult = elaService.geneCredentials(elaMemo, i);
            RetResult<Credentials> didResult = didService.geneCredentials(didMemo, i);
            RetResult<Credentials> ethResult = ethService.geneCredentials(ethMemo, i);

            assertEquals("ela credentials", elaResult.getCode().longValue(), RetCode.SUCC);
            assertEquals("did credentials", didResult.getCode().longValue(), RetCode.SUCC);
            assertEquals("eth credentials", ethResult.getCode().longValue(), RetCode.SUCC);

            System.out.println("ela pri key:" + elaResult.getData().getKeyPair().getPrivateKey());
            System.out.println("did pri key:" + didResult.getData().getKeyPair().getPrivateKey());
            System.out.println("eth pri key:" + ethResult.getData().getKeyPair().getPrivateKey());

            System.out.println("ela address:" + elaResult.getData().getAddress());
            System.out.println("did address:" + didResult.getData().getAddress());
            System.out.println("eth address:" + ethResult.getData().getAddress());
        }
    }

    @Test
    public void transfer() throws Exception {
//        Credentials srcElaCre = elaService.geneCredentialsByPrivateKey(elaPrivateKey);
//        RetResult<Credentials> elaResult = elaService.geneCredentials(elaMemo, 0);
//        assertEquals("ela credentials", elaResult.getCode().longValue(), RetCode.SUCC);
//        Credentials dstElaCre = elaResult.getData();
//        RetResult<Double> elaFee =elaService.estimateTransactionFee(srcElaCre.getAddress(), ElaChainType.ELA_CHAIN, dstElaCre.getAddress(), 0.1);
//        assertEquals("ela fee:" + elaFee.getMsg(), elaFee.getCode().longValue(), RetCode.SUCC);
//        System.out.println("ela transfer fee:" + elaFee.getData());
//        RetResult<String> elaTxid = elaService.transfer(srcElaCre, dstElaCre.getAddress(), 0.1);
//        assertEquals("ela txid:" + elaTxid.getMsg(), elaTxid.getCode().longValue(), RetCode.SUCC);
//        elaTxid = elaService.waitForTransactionReceipt(elaTxid.getData());
//        assertEquals("ela txid:" + elaTxid.getMsg(), elaTxid.getCode().longValue(), RetCode.SUCC);
//        RetResult<Double> elaBalance = elaService.getBalance(dstElaCre.getAddress());
//        assertEquals("ela balance:" + elaBalance.getMsg(), elaBalance.getCode().longValue(), RetCode.SUCC);
//        System.out.println("ela balance:" + elaBalance.getData());
//
//
//        Credentials srcDidCre = didService.geneCredentialsByPrivateKey(didPrivateKey);
//        RetResult<Credentials> didResult = didService.geneCredentials(didMemo, 0);
//        assertEquals("did credentials", didResult.getCode().longValue(), RetCode.SUCC);
//        Credentials dstDidCre = didResult.getData();
//        RetResult<Double> didFee =didService.estimateTransactionFee(srcDidCre.getAddress(), ElaChainType.DID_CHAIN, dstDidCre.getAddress(), 0.1);
//        assertEquals("did fee:" + didFee.getMsg(), didFee.getCode().longValue(), RetCode.SUCC);
//        System.out.println("did transfer fee:" + didFee.getData());
//        RetResult<String> didTxid = didService.transfer(srcDidCre, dstDidCre.getAddress(), 0.1);
//        assertEquals("did txid:" + didTxid.getMsg(), didTxid.getCode().longValue(), RetCode.SUCC);
//        didTxid = didService.waitForTransactionReceipt(didTxid.getData());
//        assertEquals("did txid:" + didTxid.getMsg(), didTxid.getCode().longValue(), RetCode.SUCC);
//        RetResult<Double> didBalance = elaService.getBalance(dstDidCre.getAddress());
//        assertEquals("did balance:" + didBalance.getMsg(), didBalance.getCode().longValue(), RetCode.SUCC);
//        System.out.println("did balance:" + didBalance.getData());


        Credentials srcEthCre = ethService.geneCredentialsByPrivateKey(ethPrivateKey);
        RetResult<Credentials> ethResult = ethService.geneCredentials(ethMemo, 0);
        assertEquals("eth credentials", ethResult.getCode().longValue(), RetCode.SUCC);
        Credentials dstEthCre = ethResult.getData();
        RetResult<Double> ethFee =ethService.estimateTransactionFee(srcEthCre.getAddress(), ElaChainType.ETH_CHAIN, dstEthCre.getAddress(), 0.1);
        assertEquals("eth fee:" + ethFee.getMsg(), ethFee.getCode().longValue(), RetCode.SUCC);
        System.out.println("eth transfer fee:" + ethFee.getData());
        RetResult<String> ethTxid = ethService.transfer(srcEthCre, dstEthCre.getAddress(), 0.1);
        assertEquals("eth txid:" + ethTxid.getMsg(), ethTxid.getCode().longValue(), RetCode.SUCC);
        ethTxid = ethService.waitForTransactionReceipt(ethTxid.getData());
        assertEquals("eth txid:" + ethTxid.getMsg(), ethTxid.getCode().longValue(), RetCode.SUCC);
        RetResult<Double> ethBalance = ethService.getBalance(dstEthCre.getAddress());
        assertEquals("eth balance:" + ethBalance.getMsg(), ethBalance.getCode().longValue(), RetCode.SUCC);
        System.out.println("eth balance:" + ethBalance.getData());
    }

    @Test
    public void transferToSideChain() throws Exception {
        Credentials srcElaCre = elaService.geneCredentialsByPrivateKey(elaPrivateKey);

        Credentials dstEthCre = ethService.geneCredentialsByPrivateKey(ethPrivateKey);
        RetResult<Double> ethFee =elaService.estimateTransactionFee(srcElaCre.getAddress(), ElaChainType.ETH_CHAIN, dstEthCre.getAddress(), 1.0);
        System.out.println("eth transfer fee:" + ethFee.getData());
        assertEquals("eth fee:" + ethFee.getMsg(), ethFee.getCode().longValue(), RetCode.SUCC);
        RetResult<String> ethTxid = elaService.transferToSideChain(srcElaCre, ElaChainType.ETH_CHAIN, dstEthCre.getAddress(), 1.0);
        assertEquals("eth txid:" + ethTxid.getMsg(), ethTxid.getCode().longValue(), RetCode.SUCC);
        ethTxid = elaService.waitForTransactionReceipt(ethTxid.getData());
        assertEquals("eth txid:" + ethTxid.getMsg(), ethTxid.getCode().longValue(), RetCode.SUCC);
        RetResult<Double> ethBalance = ethService.getBalance(dstEthCre.getAddress());
        assertEquals("eth balance:" + ethBalance.getMsg(), ethBalance.getCode().longValue(), RetCode.SUCC);
        System.out.println("eth balance:" + ethBalance.getData());

        Credentials dstDidCre = didService.geneCredentialsByPrivateKey(didPrivateKey);
        RetResult<Double> didFee =elaService.estimateTransactionFee(srcElaCre.getAddress(), ElaChainType.DID_CHAIN, dstDidCre.getAddress(), 0.05);
        System.out.println("did transfer fee:" + didFee.getData());
        assertEquals("did fee:" + didFee.getMsg(), didFee.getCode().longValue(), RetCode.SUCC);
        RetResult<String> didTxid = elaService.transferToSideChain(srcElaCre, ElaChainType.DID_CHAIN, dstDidCre.getAddress(), 0.05);
        assertEquals("did txid:" + didTxid.getMsg(), didTxid.getCode().longValue(), RetCode.SUCC);
        didTxid = elaService.waitForTransactionReceipt(didTxid.getData());
        assertEquals("did txid:" + didTxid.getMsg(), didTxid.getCode().longValue(), RetCode.SUCC);
        RetResult<Double> didBalance = didService.getBalance(dstDidCre.getAddress());
        assertEquals("did balance:" + didBalance.getMsg(), didBalance.getCode().longValue(), RetCode.SUCC);
        System.out.println("did balance:" + didBalance.getData());

    }

    @Test
    public void transferToMainChain() throws Exception {
        Credentials dstElaCre = elaService.geneCredentialsByPrivateKey(elaPrivateKey);

        RetResult<Credentials> didResult = didService.geneCredentials(didMemo, 0);
        assertEquals("did credentials", didResult.getCode().longValue(), RetCode.SUCC);
        Credentials srcDidCre = didResult.getData();
        RetResult<Double> didFee =didService.estimateTransactionFee(srcDidCre.getAddress(), ElaChainType.ELA_CHAIN, dstElaCre.getAddress(), 0.02);
        assertEquals("did fee:" + didFee.getMsg(), didFee.getCode().longValue(), RetCode.SUCC);
        System.out.println("did transfer fee:" + didFee.getData());
        RetResult<String> didTxid = didService.transferToMainChain(srcDidCre, dstElaCre.getAddress(), 0.02);
        assertEquals("did txid:" + didTxid.getMsg(), didTxid.getCode().longValue(), RetCode.SUCC);
        didTxid = didService.waitForTransactionReceipt(didTxid.getData());
        assertEquals("did txid:" + didTxid.getMsg(), didTxid.getCode().longValue(), RetCode.SUCC);



        RetResult<Credentials> ethResult = ethService.geneCredentials(ethMemo, 0);
        assertEquals("eth credentials", ethResult.getCode().longValue(), RetCode.SUCC);
        Credentials srcEthCre = ethResult.getData();
        RetResult<Double> ethFee =ethService.estimateTransactionFee(srcEthCre.getAddress(), ElaChainType.ELA_CHAIN, dstElaCre.getAddress(), 0.1);
        assertEquals("eth fee:" + ethFee.getMsg(), ethFee.getCode().longValue(), RetCode.SUCC);
        System.out.println("eth transfer fee:" + ethFee.getData());
        RetResult<String> ethTxid = ethService.transferToMainChain(srcEthCre, dstElaCre.getAddress(), 0.1);
        assertEquals("eth txid:" + ethTxid.getMsg(), ethTxid.getCode().longValue(), RetCode.SUCC);
        ethTxid = ethService.waitForTransactionReceipt(ethTxid.getData());
        assertEquals("eth txid:" + ethTxid.getMsg(), ethTxid.getCode().longValue(), RetCode.SUCC);



        RetResult<Double> elaBalance = elaService.getBalance(dstElaCre.getAddress());
        assertEquals("ela balance:" + elaBalance.getMsg(), elaBalance.getCode().longValue(), RetCode.SUCC);
        System.out.println("ela balance:" + elaBalance.getData());
    }

    @Test
    public void getBalance() throws Exception {
//        Credentials dstDidCre = didService.geneCredentialsByPrivateKey(didPrivateKey);
//        didService.getBalance(dstDidCre.getAddress());

//        RetResult<Double> retRest = ethService.getBalance("0xb3597A4Ed6aA224dF9741322805F9C8BDC6Ab9A4");
        RetResult<Double> retRest = ethService.getBalance("0x8a01678ae58e1839e02546538484bad7ef53f8ee");
        if (retRest.getCode() == RetCode.SUCC) {
            System.out.println("rest:" + retRest.getData());
        } else {
            System.out.println("Err getBalance msg:" + retRest.getMsg());
        }
    }

    @Test
    public void transferEla() throws Exception {
        Credentials srcElaCre = elaService.geneCredentialsByPrivateKey(elaPrivateKey);
        String dstAddr = "ETyT1LGW6ZqKUDzstEwCCc5xpK3JLCY3XJ";
        RetResult<String> elaTxid = elaService.transfer(srcElaCre, dstAddr, 0.3);
        assertEquals("ela txid:" + elaTxid.getMsg(), elaTxid.getCode().longValue(), RetCode.SUCC);
        System.out.println("ela txid:" + elaTxid.getData());
    }

    @Test
    public void transferEth() throws Exception {
        Credentials srcEthCre = ethService.geneCredentialsByPrivateKey(ethPrivateKey);
        String dstAddr = "0xd2c9ec9731422ef02a10a3e612a8aa96c7e3db8b";
        RetResult<String> elaTxid = ethService.transfer(srcEthCre, dstAddr, 0.3);
        assertEquals("ela txid:" + elaTxid.getMsg(), elaTxid.getCode().longValue(), RetCode.SUCC);
        System.out.println("ela txid:" + elaTxid.getData());
    }
}