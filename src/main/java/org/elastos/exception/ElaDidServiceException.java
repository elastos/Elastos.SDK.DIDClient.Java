/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.elastos.exception;

public class ElaDidServiceException extends RuntimeException {
    public ElaDidServiceException(String message) {
        super(message);
    }

    public ElaDidServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
