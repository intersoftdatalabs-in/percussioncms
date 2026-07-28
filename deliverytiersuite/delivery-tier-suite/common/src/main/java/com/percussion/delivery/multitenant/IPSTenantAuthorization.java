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
package com.percussion.delivery.multitenant;

import jakarta.servlet.ServletRequest;

/**
 * Handle authorization of a tenant id.
 *
 * @author erikserating
 */
public interface IPSTenantAuthorization {

  /**
   * Authorize the tenant id from a request to make sure it is an existing tenant id attached to a
   * customer account and is active, and that the request quota has not been exceeded.
   *
   * @param tenantid the tenant id string, cannot be <code>null</code> or empty.
   * @param apiCalls the number of API calls being charged against the tenant for this request.
   * @param req the current servlet request, may be <code>null</code>.
   * @return the authorization status, never <code>null</code>.
   */
  public PSLicenseStatus authorize(String tenantid, long apiCalls, ServletRequest req);

  /** Authorization status codes. */
  public enum Status {
    /** Validation failed due to a system error - client behavior will be different than a failure. */
    UNEXPECTED_ERROR,
    /** User has exceeded quota. */
    EXCEEDED_QUOTA,
    /** There is no license matching that number. */
    NO_ACCOUNT_EXISTS,
    /** The license is valid but not activated. */
    NOT_ACTIVE,
    /** The licence is active and valid. */
    SUCCESS,
    /** The license has been suspended by Percussion. */
    SUSPENDED
  }
}
