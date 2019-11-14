package org.elastos.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.elastos.POJO.ElaChainType;
import org.elastos.POJO.KeyPair;
import org.elastos.POJO.Credentials;
import org.elastos.constant.RetCode;
import org.elastos.ela.Ela;
import org.elastos.entity.MnemonicType;
import org.elastos.exception.ElaDidServiceException;
import org.elastos.service.ela.BackendService;
import org.elastos.service.ela.ElaTransaction;
import org.elastos.util.RetResult;
import org.elastos.util.ela.ElaHdSupport;
import org.web3j.crypto.CipherException;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ElaService implements ElaTransferService {
    private BackendService backendService = new BackendService();
    private ElaChainType chainType;
    private static final int SLEEP_DURATION = 2;
    private static final int ATTEMPTS = 15;

    public ElaService(ElaChainType elaChainType, String inNodeUrl, boolean isTest) {
        if ((elaChainType == ElaChainType.ELA_CHAIN) || (elaChainType == ElaChainType.DID_CHAIN)) {
            chainType = elaChainType;
        } else {
            throw new ElaDidServiceException("ElaService chain type only support ELA_CHAIN and DID_CHAIN");
        }

        if (null == inNodeUrl) {
            throw new ElaDidServiceException("ElaService node url must not be null");
        }

        backendService.setTestNet(isTest);
        backendService.setPrefix(inNodeUrl);
    }

    @Override
    public String createMnemonic() {
        String mnemonic = ElaHdSupport.generateMnemonic(MnemonicType.ENGLISH);
        return mnemonic;
    }

    @Override
    public RetResult<Credentials> geneCredentials(String mnemonic, int index) {
        String ret;
        try {
            ret = ElaHdSupport.generate(mnemonic, index);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | CipherException e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err geneCredentials generate exception:" + e.getMessage());
        }
        JSONObject data = JSON.parseObject(ret);

        KeyPair keyPair = new KeyPair();

        String privateKey = data.getString("privateKey");
        keyPair.setPrivateKey(privateKey);
        keyPair.setPublicKey(data.getString("publicKey"));
        Credentials credentials = new Credentials();
        credentials.setKeyPair(keyPair);
        credentials.setAddress(Ela.getAddressFromPrivate(privateKey));
        return RetResult.retOk(credentials);
    }

    @Override
    public RetResult<String> transfer(Credentials credentials, String dstAddress, double value) {
        ElaTransaction elaTransaction = new ElaTransaction(backendService, chainType, chainType);
        RetResult retSender = elaTransaction.addSender(credentials.getAddress(), credentials.getKeyPair().getPrivateKey());
        if (retSender.getCode() != RetCode.SUCC) {
            return RetResult.retErr(retSender.getCode(), "Err: createTransaction " + retSender.getMsg());
        }

        elaTransaction.addReceiver(dstAddress, value);

        try {
            RetResult<String> ret = elaTransaction.transfer();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err: transfer exception:" + e.getMessage());
        }
    }

    @Override
    public RetResult<String> transferToMainChain(Credentials credentials, String dstMainChainAddress, double value) {
        if (ElaChainType.ELA_CHAIN == chainType) {
            return RetResult.retErr(RetCode.BAD_REQUEST, "Not support in ela chain.");
        }
        ElaTransaction elaTransaction = new ElaTransaction(backendService, chainType, ElaChainType.ELA_CHAIN);
        RetResult retSender = elaTransaction.addSender(credentials.getAddress(), credentials.getKeyPair().getPrivateKey());
        if (retSender.getCode() != RetCode.SUCC) {
            return RetResult.retErr(retSender.getCode(), "Err: createTransactionToMainChain " + retSender.getMsg());
        }

        elaTransaction.addReceiver(dstMainChainAddress, value);
        try {
            RetResult<String> ret = elaTransaction.transfer();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err: transferToMainChain exception:" + e.getMessage());
        }
    }

    @Override
    public RetResult<String> transferToSideChain(Credentials credentials, ElaChainType dstChainType, String dstAddress, double value) {
        if (ElaChainType.DID_CHAIN == chainType) {
            return RetResult.retErr(RetCode.BAD_REQUEST, "Not support in did chain.");
        }

        ElaTransaction elaTransaction = new ElaTransaction(backendService, ElaChainType.ELA_CHAIN, dstChainType);
        RetResult retSender = elaTransaction.addSender(credentials.getAddress(), credentials.getKeyPair().getPrivateKey());
        if (retSender.getCode() != RetCode.SUCC) {
            return RetResult.retErr(retSender.getCode(), "Err: createTransactionToSideChain " + retSender.getMsg());
        }

        elaTransaction.addReceiver(dstAddress, value);
        try {
            RetResult<String> ret = elaTransaction.transfer();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err: transferToSideChain exception:" + e.getMessage());
        }
    }

    @Override
    public RetResult<Double> estimateTransactionFee(String srcAddress, ElaChainType dstChainType, String dstAddress, double value) {
        Double fee = ElaTransaction.countFee(chainType, dstChainType);
        if (null != fee) {
            return RetResult.retOk(fee);
        } else {
            return RetResult.retErr(RetCode.BAD_REQUEST_PARAMETER, "not support this chan type");
        }
    }

    @Override
    public RetResult<String> waitForTransactionReceipt(String transactionHash) {

        for (int i = 0; i < ATTEMPTS; i++) {
            try {
                TimeUnit.MINUTES.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
            }
            Map<String, Object> ret = this.backendService.getTransaction(transactionHash);
            if (null != ret) {
                return RetResult.retOk(transactionHash);
            }
        }

        return RetResult.retErr(RetCode.NOT_FOUND, "Transaction hash not generated after " + ATTEMPTS + " attempts");
    }

    @Override
    public RetResult<Double> getBalance(String address) {
        return backendService.getBalance(address);
    }

    public RetResult<String> transferEla(List<String> srcWalletPrivateKeys, Map<String, Double> dstAddrAndEla) {

        ElaTransaction transaction = new ElaTransaction(backendService, chainType, chainType);
        for (String key : srcWalletPrivateKeys) {
            String sendAddr = Ela.getAddressFromPrivate(key);
            RetResult retSender = transaction.addSender(sendAddr, key);
            if (retSender.getCode() != RetCode.SUCC) {
                return RetResult.retErr(retSender.getCode(), "Err: transferEla " + retSender.getMsg());
            }
        }

        for (Map.Entry<String, Double> entry : dstAddrAndEla.entrySet()) {
            transaction.addReceiver(entry.getKey(), entry.getValue());
        }

        RetResult<String> ret;
        try {
            ret = transaction.transfer();
        } catch (Exception e) {
            e.printStackTrace();
            ret = RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err: transferEla exception:" + e.getMessage());
        }

        return ret;
    }

    @Override
    public Credentials geneCredentialsByPrivateKey(String privateKey) {
        String publicKey = Ela.getPublicFromPrivate(privateKey);
        String address = Ela.getAddressFromPrivate(privateKey);

        KeyPair keyPair = new KeyPair();
        keyPair.setPrivateKey(privateKey);
        keyPair.setPublicKey(publicKey);
        Credentials credentials = new Credentials();
        credentials.setKeyPair(keyPair);
        credentials.setAddress(address);
        return credentials;
    }
}
