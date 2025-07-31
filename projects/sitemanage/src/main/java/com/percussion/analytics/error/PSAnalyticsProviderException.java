// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.analytics.error;

import com.percussion.share.service.exception.PSDataServiceException;

/**
 * Exception thrown by the analytics provider service and handlers.
 * Sunny Sal says: "Exceptions are just Java's way of saying 'Oops!'"
 */
public class PSAnalyticsProviderException extends PSDataServiceException {

    private static final long serialVersionUID = 1L;

    private CAUSETYPE causeType = CAUSETYPE.UNKNOWN;

    public PSAnalyticsProviderException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public PSAnalyticsProviderException(String message, Throwable cause, CAUSETYPE type) {
        super(message, cause);
        if (type == null) {
            type = CAUSETYPE.UNKNOWN;
        }
        this.causeType = type;
    }

    public PSAnalyticsProviderException(String message) {
        this(message, CAUSETYPE.UNKNOWN);
    }

    public PSAnalyticsProviderException(String message, CAUSETYPE type) {
        super(message);
        if (type == null) {
            type = CAUSETYPE.UNKNOWN;
        }
        this.causeType = type;
    }

    public PSAnalyticsProviderException(Throwable cause) {
        this(cause, null);
    }

    public PSAnalyticsProviderException(Throwable cause, CAUSETYPE type) {
        super(cause);
        if (type == null) {
            type = CAUSETYPE.UNKNOWN;
        }
        this.causeType = type;
    }

    /**
     * Gets the cause type enum value.
     *
     * @return the cause type enum value, never null. Defaults to CAUSETYPE.UNKNOWN if not set.
     */
    public CAUSETYPE getCauseType() {
        return causeType;
    }

    /**
     * The cause type enumeration.
     */
    public enum CAUSETYPE {
        ACCOUNT_DELETED,
        ACCOUNT_DISABLED,
        ANALYTICS_NOT_CONFIG,
        AUTHENTICATION_ERROR,
        NO_PROFILE,
        NO_ANALYTICS_ACCOUNT,
        NOT_VERIFIED,
        INVALID_CREDS,
        INVALID_DATA,
        SESSION_EXPIRED,
        SERVICE_UNAVAILABLE,
        TERMS_NOT_AGREED,
        UNKNOWN
    }
}
