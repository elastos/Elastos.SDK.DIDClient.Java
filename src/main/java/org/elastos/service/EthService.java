package org.elastos.service;

import org.elastos.POJO.ElaChainType;
import org.elastos.POJO.Credentials;
import org.elastos.POJO.KeyPair;
import org.elastos.conf.EthConfiguration;
import org.elastos.constant.RetCode;
import org.elastos.util.RetResult;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Bip44WalletUtils;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import static java.lang.Math.pow;
import static org.elastos.conf.EthConfiguration.ETH_SIDE_CHAIN_CONTRACT_ADDRESS;
import static org.elastos.conf.EthConfiguration.ETH_SIDE_TESTCHAIN_CONTRACT_ADDRESS;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class EthService implements ElaTransferService {
    private Admin web3j;
    private static final int SLEEP_DURATION = 15000;
    private static final int ATTEMPTS = 40;
    //    private static final String CROSS_CHAIN_GAS_LIMIT = "3000000";
    //    private static final String CROSS_CHAIN_FEE = "100000000000000";
    private String contractAddress = ETH_SIDE_CHAIN_CONTRACT_ADDRESS;
    //Total cross chain fee is CROSS_CHAIN_GAS_LIMIT * gas_price + CROSS_CHAIN_FEE


    public EthService(String url, boolean testNet) {
        web3j = Admin.build(new HttpService(url));
        if (testNet) {
            contractAddress = ETH_SIDE_TESTCHAIN_CONTRACT_ADDRESS;
        } else {
            contractAddress = ETH_SIDE_CHAIN_CONTRACT_ADDRESS;
        }
    }


    @Override
    public String createMnemonic() {
        byte[] initialEntropy = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(initialEntropy);
        String mnemonic = MnemonicUtils.generateMnemonic(initialEntropy);
        return mnemonic;
    }

    @Override
    public RetResult<Credentials> geneCredentials(String mnemonic, int index) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "" + index);
        Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(seed);
        Bip32ECKeyPair bip44Keypair = Bip44WalletUtils.generateBip44KeyPair(masterKeypair);
        org.web3j.crypto.Credentials ethCredentials = org.web3j.crypto.Credentials.create(bip44Keypair);

        KeyPair keyPair = new KeyPair();
        keyPair.setPrivateKey(ethCredentials.getEcKeyPair().getPrivateKey().toString(16));
        keyPair.setPublicKey(ethCredentials.getEcKeyPair().getPublicKey().toString(16));

        Credentials transCredentials = new Credentials();
        transCredentials.setKeyPair(keyPair);
        transCredentials.setAddress(ethCredentials.getAddress());
        return RetResult.retOk(transCredentials);
    }


    @Override
    public RetResult<String> transfer(Credentials credentials, String dstAddress, double value) {
        org.web3j.crypto.Credentials ethCredential = this.toEthCredential(credentials);
        try {
            TransactionReceipt transactionReceipt = Transfer.sendFunds(
                    web3j, ethCredential, dstAddress,
                    BigDecimal.valueOf(value), Convert.Unit.ETHER).send();

            String hash = transactionReceipt.getTransactionHash();
            return RetResult.retOk(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err transfer exception:" + e.getMessage());
        }
    }

    @Override
    public RetResult<String> transferToSideChain(Credentials credentials, ElaChainType dstChainType, String dstAddress, double value) {
            return RetResult.retErr(RetCode.BAD_REQUEST, "Not support in eth chain.");
    }

    @Override
    public RetResult<String> transferToMainChain(Credentials credentials, String dstAddress, double withdrawAmount) {

        BigInteger value = Convert.toWei(BigDecimal.valueOf(withdrawAmount), Convert.Unit.ETHER).toBigInteger();
        Function function = new Function(
                "receivePayload",
                Arrays.asList(new Utf8String(dstAddress), new Uint256(value), new Uint256(new BigInteger(EthConfiguration.CROSS_CHAIN_FEE))),
                Collections.emptyList());
        String encodedFunction = FunctionEncoder.encode(function);

        org.web3j.crypto.Credentials credential = toEthCredential(credentials);

        BigInteger nonce;
        try {
            nonce = this.getNonce(credential.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err getNonce exception:" + e.getMessage());
        }

        BigInteger gasPrice;
        try {
            gasPrice = this.getGasPrice();
        } catch (IOException e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err getGasPrice exception:" + e.getMessage());
        }

        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice,
                new BigInteger(EthConfiguration.CROSS_CHAIN_GAS_LIMIT), contractAddress, value, encodedFunction);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credential);
        String hexValue = Numeric.toHexString(signedMessage);

        try {
            EthSendTransaction ethSendTransaction =
                    web3j.ethSendRawTransaction(hexValue).sendAsync().get();
            String transactionHash = ethSendTransaction.getTransactionHash();
            if (null == transactionHash) {
                return RetResult.retErr(RetCode.RESPONSE_ERROR, "Err in web3j.ethSendRawTransaction code:" + ethSendTransaction.getError().getCode() + " msg:" + ethSendTransaction.getError().getMessage());
            }

            return  RetResult.retOk(transactionHash);
        } catch (Exception e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err transferToMainChain exception:" + e.getMessage());
        }
    }

    @Override
    public RetResult<Double> estimateTransactionFee(String srcAddress, ElaChainType dstChainType, String dstAddress, double amount) {
        if((dstChainType != ElaChainType.ELA_CHAIN) && (dstChainType != ElaChainType.ETH_CHAIN)){
            return RetResult.retErr(RetCode.BAD_REQUEST, "Not support this chain type");
        }

        BigInteger value = Convert.toWei(BigDecimal.valueOf(amount), Convert.Unit.ETHER).toBigInteger();

        BigInteger nonce;
        try {
            nonce = this.getNonce(srcAddress);
        } catch (Exception e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err getNonce exception:" + e.getMessage());
        }

        BigInteger gasPrice = null;
        try {
            gasPrice = this.getGasPrice();
        } catch (IOException e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err getGasPrice exception:" + e.getMessage());
        }

        org.web3j.protocol.core.methods.request.Transaction transaction;
        if (dstChainType == ElaChainType.ETH_CHAIN) {
            transaction = org.web3j.protocol.core.methods.request.Transaction.createEtherTransaction(
                    srcAddress, nonce, gasPrice, new BigInteger(EthConfiguration.SAME_CHAIN_GAS_LIMIT), dstAddress, value);
        } else {
            Function function = new Function(
                    "receivePayload",
                    Arrays.asList(new Utf8String(dstAddress), new Uint256(value), new Uint256(new BigInteger(EthConfiguration.CROSS_CHAIN_FEE))),
                    Collections.emptyList());
            String encodedFunction = FunctionEncoder.encode(function);

            transaction = org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction(
                    srcAddress, nonce, gasPrice, new BigInteger(EthConfiguration.CROSS_CHAIN_GAS_LIMIT), contractAddress, value, encodedFunction);
        }

        try {
            EthEstimateGas ethEstimateGas  =  web3j.ethEstimateGas(transaction).send();
            BigInteger feeInt = ethEstimateGas.getAmountUsed();
            BigDecimal fee = Convert.fromWei(feeInt.toString(), Convert.Unit.ETHER);
            return RetResult.retOk(fee.doubleValue());
        } catch (IOException e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err ethEstimateGas exception:" + e.getMessage());
        }
    }

    @Override
    public RetResult<String> waitForTransactionReceipt(String transactionHash) {

        Optional<TransactionReceipt> transactionReceiptOptional =
                null;
        try {
            transactionReceiptOptional = getTransactionReceipt(transactionHash, SLEEP_DURATION, ATTEMPTS);
        } catch (Exception e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.NOT_FOUND, "waitForTransactionReceipt exception:" + e.getMessage());
        }

        if (!transactionReceiptOptional.isPresent()) {
            return RetResult.retErr(RetCode.NOT_FOUND, "Transaction hash not generated after " + ATTEMPTS + " attempts");
        } else {
            TransactionReceipt transactionReceipt  = transactionReceiptOptional.get();
            return RetResult.retOk(transactionReceipt.getTransactionHash());
        }
    }

    @Override
    public RetResult<String> getTransactionReceipt(String txid) {
        Optional<TransactionReceipt> receiptOptional =
                null;
        try {
            receiptOptional = sendTransactionReceiptRequest(txid);
        } catch (Exception e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.NOT_FOUND, "Exception: " + e.getMessage());
        }
        if (receiptOptional.isPresent()) {
            return RetResult.retOk(txid);
        } else {
            return RetResult.retErr(RetCode.NOT_FOUND, "Transaction hash not generated after " + ATTEMPTS + " attempts");
        }
    }

    @Override
    public RetResult<Double> getBalance(String address) {
        try {
            EthGetBalance ethGetBalance =
                    web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                            .send();
            BigInteger r = ethGetBalance.getBalance();
            if (null != r) {
                Double d = pow(10, 18);
                Double ret = r.doubleValue() / d;
                return RetResult.retOk(ret);
            } else {
                return RetResult.retErr(RetCode.RESPONSE_ERROR, "Err ethGetBalance failed" + ethGetBalance.getError());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return RetResult.retErr(RetCode.INTERNAL_EXCEPTION, "Err ethGetBalance exception:" + e.getMessage());
        }
    }

    private Optional<TransactionReceipt> getTransactionReceipt(
            String transactionHash, int sleepDuration, int attempts) throws Exception {

        Optional<TransactionReceipt> receiptOptional =
                sendTransactionReceiptRequest(transactionHash);
        for (int i = 0; i < attempts; i++) {
            if (!receiptOptional.isPresent()) {
                Thread.sleep(sleepDuration);
                receiptOptional = sendTransactionReceiptRequest(transactionHash);
            } else {
                break;
            }
        }

        return receiptOptional;
    }

    public EthTransaction getTransaction(String transactionHash) {
        try {
            EthTransaction transaction = web3j.ethGetTransactionByHash(transactionHash).send();
            return transaction;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public BigInteger getGasPrice() throws IOException{
        EthGasPrice ethGasPrice;
        ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice();
        return gasPrice;
    }

    public BigInteger getNonce(String address) throws Exception {
        EthGetTransactionCount ethGetTransactionCount =
                web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send();

        return ethGetTransactionCount.getTransactionCount();
    }

    private Optional<TransactionReceipt> sendTransactionReceiptRequest(String transactionHash)
            throws Exception {
        EthGetTransactionReceipt transactionReceipt =
                web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get();

        return transactionReceipt.getTransactionReceipt();
    }

    private org.web3j.crypto.Credentials toEthCredential(Credentials credentials) {
        BigInteger privateKeyInBT = new BigInteger(credentials.getKeyPair().getPrivateKey(), 16);
        BigInteger publicKeyInBT = new BigInteger(credentials.getKeyPair().getPublicKey(), 16);

        ECKeyPair aPair = new ECKeyPair(privateKeyInBT, publicKeyInBT);
        org.web3j.crypto.Credentials aCredential = org.web3j.crypto.Credentials.create(aPair);

        return aCredential;
    }

    public Credentials geneCredentialsByPrivateKey(String privateKeyInHex) {
        BigInteger privateKeyInBT = new BigInteger(privateKeyInHex, 16);

        ECKeyPair aPair = ECKeyPair.create(privateKeyInBT);
        org.web3j.crypto.Credentials aCredential = org.web3j.crypto.Credentials.create(aPair);

        KeyPair keyPair = new KeyPair();
        keyPair.setPrivateKey(aCredential.getEcKeyPair().getPrivateKey().toString(16));
        keyPair.setPublicKey(aCredential.getEcKeyPair().getPublicKey().toString(16));

        Credentials credentials = new Credentials();
        credentials.setKeyPair(keyPair);
        credentials.setAddress(aCredential.getAddress());
        return credentials;
    }


}

