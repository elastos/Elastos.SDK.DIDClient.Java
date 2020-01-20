/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.conf;

/**
 *
 * patternmap
 *
 * 11/23/18
 *
 */
public interface EthConfiguration {
    String ONE_ETH = "1000000000000000000";
    String CROSS_CHAIN_FEE = "100000000000000";//0.0001
    String ETH_CHAIN_ROUND = "10000000000";//0.0000001
    String SAME_CHAIN_GAS_LIMIT = "21000";//GAS_FEE=0.00021
    String CROSS_CHAIN_GAS_LIMIT = "3000000";//GAS_FEE=CROSS_CHAIN_GAS_LIMIT*GAS_PRICE_BASIC=0.03
    String ELA_MAIN_CHAIN_ADDRESS = "XVbCTM7vqM1qHKsABSFH4xKN1qbp7ijpWf";
    String ELA_MAIN_TESTCHAIN_ADDRESS = "XWCiyXM1bQyGTawoaYKx9PjRkMUGGocWub";
    String ETH_SIDE_CHAIN_CONTRACT_ADDRESS = "0xC445f9487bF570fF508eA9Ac320b59730e81e503";
    String ETH_SIDE_TESTCHAIN_CONTRACT_ADDRESS = "0x491bC043672B9286fA02FA7e0d6A3E5A0384A31A";
}
