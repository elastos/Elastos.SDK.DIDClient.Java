/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.elastos.DTO;

import org.elastos.entity.ChainType;

import java.util.List;

public class ChainAddrUtxos {
    ChainType type = null;
    String Address = null;
    List<String> Utxo = null;

    public ChainType getType() {
        return type;
    }

    public void setType(ChainType type) {
        this.type = type;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public List<String> getUtxo() {
        return Utxo;
    }

    public void setUtxo(List<String> utxo) {
        Utxo = utxo;
    }
}
