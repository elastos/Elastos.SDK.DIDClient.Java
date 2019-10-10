package org.elastos.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class EthTransferTest {
//    String url = "https://rpc.elaeth.io";
    String url = "http://rpc.elaeth.io:8545";
    String priKey = "3e7bd30c5dd15e50e31c3d59a0db08d54c38bb4349406ce52149e732dcbe3914";
    String srcAddr = "0xb3597A4Ed6aA224dF9741322805F9C8BDC6Ab9A4";
    String dstAddr = "EZdDnKBRnV8o77gjr1M3mWBLZqLA3WBjB7";
    String contractAddress = "0x491bC043672B9286fA02FA7e0d6A3E5A0384A31A";
EthTransfer ethTransfer;
    @Before
    public void setUp() throws Exception {
        ethTransfer = new EthTransfer(url);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void transfer() throws Exception {
        ethTransfer.transfer(priKey, dstAddr, 0.1);

    }

    @Test
    public void withdrawEla() throws Exception {
        ethTransfer.withdrawEla(priKey, srcAddr, dstAddr, contractAddress, new BigDecimal(1));

    }

}