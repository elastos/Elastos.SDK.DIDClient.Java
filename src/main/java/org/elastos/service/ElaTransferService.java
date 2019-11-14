package org.elastos.service;

import org.elastos.POJO.Credentials;
import org.elastos.POJO.ElaChainType;
import org.elastos.util.RetResult;

public interface ElaTransferService {
    static ElaTransferService getInstance(ElaChainType type, String nodeUrl, boolean isTestChain) {
        switch (type) {
            case ETH_CHAIN:
                return new EthService(nodeUrl, isTestChain);
            case ELA_CHAIN:
            case DID_CHAIN:
                return new ElaService(type, nodeUrl, isTestChain);
            default:
                return null;
        }
    }

    String createMnemonic();

    RetResult<Credentials> geneCredentials(String mnemonic, int index);

    RetResult<String> transfer(Credentials credentials, String dstAddress, double value);

    //Only side chain support
    RetResult<String> transferToMainChain(Credentials credentials, String dstMainChainAddress, double value);

    //Only main chain support
    RetResult<String> transferToSideChain(Credentials credentials, ElaChainType dstChainType, String dstAddress, double value);

    RetResult<Double> estimateTransactionFee(String srcAddress, ElaChainType dstChainType, String dstAddress, double value);

    RetResult<String> waitForTransactionReceipt(String txid);

    RetResult<Double> getBalance(String address);

    Credentials geneCredentialsByPrivateKey(String privateKey);
}
