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
// REFACTORED: CP-JAVA11
package com.percussion.delivery.multitenant;

import javax.servlet.ServletRequest;

/**
 * Handles authorization of a tenant ID.
 * Implementations should ensure the tenant is active, attached to a customer account,
 * and has not exceeded its request quota.
 */
public interface IPSTenantAuthorization {

    /**
     * Authorizes the tenant ID from a request, ensuring it is valid, active, and within quota.
     *
     * @param tenantid the tenant ID string, must not be {@code null} or empty
     * @param apiCalls number of API calls made
     * @param req the servlet request
     * @return the appropriate license status, never {@code null}
     */
    PSLicenseStatus authorize(String tenantid, long apiCalls, ServletRequest req);

    /**
     * Authorization status codes.
     */
    enum Status {
        UNEXPECTED_ERROR, // Validation failed due to a system error
        EXCEEDED_QUOTA,   // User has exceeded quota
        NO_ACCOUNT_EXISTS,// No license matching that number
        NOT_ACTIVE,       // License is valid but not activated
        SUCCESS,          // License is active and valid
        SUSPENDED         // License has been suspended by Percussion
    }
}
