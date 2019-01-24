/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.conf;

/**
 * patternmap
 * <p>
 * 11/8/18
 */
public interface RetCodeConfiguration {
    public static final long SUCC = 200;
    public static final long BAD_REQUEST = 400;
    public static final long NOT_FOUND = 404;
    public static final long INTERNAL_ERROR = 500;
    public static final long PROCESS_ERROR = 10001;
}
