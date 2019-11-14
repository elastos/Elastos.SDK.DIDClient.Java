/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.constant;

/**
 * patternmap
 * <p>
 * 11/8/18
 */
public interface RetCode {
    long SUCC = 0;
    long BAD_REQUEST = 1000;
    long BAD_REQUEST_PARAMETER = 1001;
    long NOT_FOUND = 1002;
    long INTERNAL_ERROR = 1003;
    long INTERNAL_EXCEPTION = 1004;
    long NETWORK_FAILED = 1005;
    long RESPONSE_ERROR = 1006;
}
