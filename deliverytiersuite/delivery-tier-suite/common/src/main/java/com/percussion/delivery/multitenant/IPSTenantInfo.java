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

import java.util.Date;

/**
 * Contains information about the tenant, including license and usage counts.
 *
 * @author natechadwick
 */
public interface IPSTenantInfo {

  /**
   * Sets the tenant ID or license number associated with this tenant.
   *
   * @param id the tenant id, never <code>null</code>.
   */
  public void setTenantId(String id);

  /**
   * Returns the tenant ID.
   *
   * @return the tenant id, may be <code>null</code>.
   */
  public String getTenantId();

  /**
   * Returns the total number of API calls made by this tenant to the service.
   *
   * @return the cumulative API call count.
   */
  public long getAPIUsage();

  /**
   * Adds the specified value to the tenant's current API usage counter.
   *
   * @param value a number representing API calls made to the service.
   */
  public void addAPIUsage(long value);

  /** Clears the tenant API usage counter. */
  public void clearAPIUsage();

  /**
   * Sets the date and time that API usage counting was reset to zero.
   *
   * @param start the date and time of the last reset.
   */
  public void setAPIUsageStart(Date start);

  /**
   * Returns the date and time that usage counting was started for this tenant.
   *
   * @return the API usage start date, never <code>null</code>.
   */
  public Date getAPIUsageStart();

  /**
   * Returns the date and time that the license was last authorized.
   *
   * @return the last authorization check date, may be <code>null</code>.
   */
  public Date getLastAuthorizationCheckDate();

  /**
   * Sets the date and time that the license was last authorized.
   *
   * @param date the new authorization check date.
   */
  public void setLastAuthorizationCheckDate(Date date);

  /**
   * Returns the current license status for the tenant.
   *
   * @return the license status, may be <code>null</code>.
   */
  public PSLicenseStatus getLicenseStatus();

  /**
   * Sets the current license status for the tenant.
   *
   * @param status the license status, may be <code>null</code>.
   */
  public void setLicenseStatus(PSLicenseStatus status);
}
