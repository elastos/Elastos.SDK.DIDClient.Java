package org.elastos.service;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class EthTransfer {
    private Admin web3j;
    private static final int SLEEP_DURATION = 15000;
    private static final int ATTEMPTS = 40;
    private static final String GAS_LIMIT = "3000000";
    private static final String GAS_PRICE = "20000000000";
    private static final String CROSS_CHAIN_FEE= "100000000000000";
    //Total cross chain fee is GAS_LIMIT * GAS_PRICE + CROSS_CHAIN_FEE

    public EthTransfer(String url) {
        web3j = Admin.build(new HttpService(url));
    }

    public String transfer(String privateKey, String dstAddress, double value) {

        Credentials credential = this.geneCredential(privateKey);
        try {
            TransactionReceipt transactionReceipt = Transfer.sendFunds(
                    web3j, credential, dstAddress,
                    BigDecimal.valueOf(value), Convert.Unit.ETHER).send();

            return transactionReceipt.getTransactionHash();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String withdrawEla(String priKey, String srcAddress, String dstAddress, String contractAddress, BigDecimal withdrawAmount) {
        BigInteger value = Convert.toWei(withdrawAmount, Convert.Unit.ETHER).toBigInteger();
        Function function = new Function(
                "receivePayload",
                Arrays.asList(new Utf8String(dstAddress), new Uint256(value), new Uint256(new BigInteger(CROSS_CHAIN_FEE))),
                Collections.emptyList());


        String encodedFunction = FunctionEncoder.encode(function);
        BigInteger nonce;
        try {
            nonce = this.getNonce(srcAddress);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, new BigInteger(GAS_PRICE),
                new BigInteger(GAS_LIMIT), contractAddress, value, encodedFunction);

        Credentials credentials = geneCredential(priKey);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        try {
            EthSendTransaction ethSendTransaction =
                    web3j.ethSendRawTransaction(hexValue).sendAsync().get();
            String transactionHash = ethSendTransaction.getTransactionHash();
            TransactionReceipt transactionReceipt = waitForTransactionReceipt(transactionHash);

            String hash = transactionReceipt.getTransactionHash();
            return hash;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public BigInteger getNonce(String address) throws Exception {
        EthGetTransactionCount ethGetTransactionCount =
                web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
                        .sendAsync()
                        .get();

        return ethGetTransactionCount.getTransactionCount();
    }

    public BigInteger getBalence(String address){

        try {
            EthGetBalance ethGetBalance =
                    web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                            .send();
            return ethGetBalance.getBalance();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public EthTransaction getTransaction(String transactionHash){
        try {
            EthTransaction transaction = web3j.ethGetTransactionByHash(transactionHash).send();
            return transaction;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private TransactionReceipt waitForTransactionReceipt(String transactionHash) throws Exception {

        Optional<TransactionReceipt> transactionReceiptOptional =
                getTransactionReceipt(transactionHash, SLEEP_DURATION, ATTEMPTS);

        if (!transactionReceiptOptional.isPresent()) {
            throw new Exception("Transaction receipt not generated after " + ATTEMPTS + " attempts");
        }

        return transactionReceiptOptional.get();
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

    private Optional<TransactionReceipt> sendTransactionReceiptRequest(String transactionHash)
            throws Exception {
        EthGetTransactionReceipt transactionReceipt =
                web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get();

        return transactionReceipt.getTransactionReceipt();
    }

    private Credentials geneCredential(String privateKeyInHex) {
        BigInteger privateKeyInBT = new BigInteger(privateKeyInHex, 16);

        ECKeyPair aPair = ECKeyPair.create(privateKeyInBT);
        Credentials aCredential = Credentials.create(aPair);

        return aCredential;
    }

}

