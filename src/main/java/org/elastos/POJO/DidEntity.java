/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.elastos.POJO;

import java.util.List;

public class DidEntity {

    public static final String DID_TAG = "DID Property";

    public enum DidStatus{
        Normal,
        Deprecated
    }

    public static class DidProperty {
        String Key;
        String Value;
        DidStatus Status = DidStatus.Normal;

        public String getKey() {
            return Key;
        }

        public void setKey(String key) {
            this.Key = key;
        }

        public String getValue() {
            return Value;
        }

        public void setValue(String value) {
            this.Value = value;
        }

        public DidStatus getStatus() {
            return Status;
        }

        public void setStatus(DidStatus status) {
            this.Status = status;
        }
    }

    String Tag = DidEntity.DID_TAG;
    String Did;
    DidStatus Status = DidStatus.Normal;
    String Ver = "1.0";
    List<DidProperty> properties;

    public String getDid() {
        return Did;
    }

    public void setDid(String did) {
        this.Did = did;
    }

    public String getTag() {
        return Tag;
    }

    public DidStatus getStatus() {
        return Status;
    }

    public void setStatus(DidStatus status) {
        this.Status = status;
    }

    public String getVer() {
        return Ver;
    }

    public void setVer(String ver) {
        this.Ver = ver;
    }

    public List<DidProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<DidProperty> properties) {
        this.properties = properties;
    }

}
