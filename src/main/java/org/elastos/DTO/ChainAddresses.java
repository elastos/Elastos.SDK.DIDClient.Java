/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.elastos.DTO;


import org.elastos.entity.ChainType;

import java.util.List;

public class ChainAddresses {
    ChainType type = null;
    List<String> AddressList = null;

    public ChainType getType() {
        return type;
    }

    public void setType(ChainType type) {
        this.type = type;
    }

    public List<String> getAddressList() {
        return AddressList;
    }

    public void setAddressList(List<String> addressList) {
        AddressList = addressList;
    }
}
