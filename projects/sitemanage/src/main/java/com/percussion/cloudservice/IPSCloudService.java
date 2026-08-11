// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.cloudservice;

import com.percussion.cloudservice.data.PSCloudLicenseType;
import com.percussion.cloudservice.data.PSCloudServiceInfo;
import com.percussion.cloudservice.data.PSCloudServicePageData;
import com.percussion.share.service.exception.PSDataServiceException;

/** Service interface for Percussion CMS cloud services. */
public interface IPSCloudService {

  /**
   * Determines if cloud services are active (licensed).
   *
   * @return true if services are active; false otherwise.
   */
  boolean isActive();

  /**
   * Determines if the given license type is active.
   *
   * @param licenseType the license type to check.
   * @return true if service is active; false otherwise.
   */
  boolean isActive(PSCloudLicenseType licenseType);

  /**
   * Gets a map of license types to isActive.
   *
   * @return string in JSON format.
   */
  String getActiveState();

  /**
   * Gets the cloud services info (client identity, UI provider, etc.).
   *
   * @return cloud services info.
   */
  PSCloudServiceInfo getInfo() throws PSCloudServiceException;

  /**
   * Gets the cloud services info (client identity, UI provider, etc.) for the given license type.
   *
   * @param licenseType the license type.
   * @return cloud services info.
   */
  PSCloudServiceInfo getInfo(PSCloudLicenseType licenseType) throws PSCloudServiceException;

  /**
   * Gets the page data for the given page.
   *
   * @param pageId the page ID.
   * @return page data.
   */
  PSCloudServicePageData getPageData(String pageId) throws PSCloudServiceException;

  /**
   * Gets the page data for the given page using the given license type.
   *
   * @param licenseType the license type.
   * @param pageId the page ID.
   * @return page data.
   */
  PSCloudServicePageData getPageData(PSCloudLicenseType licenseType, String pageId);

  /**
   * Saves the page data.
   *
   * @param pageData the page data to save.
   */
  void savePageData(PSCloudServicePageData pageData);

  /** Runtime exception thrown when an error occurs in this service. */
  class PSCloudServiceException extends PSDataServiceException {
    private static final long serialVersionUID = 1L;

    public PSCloudServiceException() {
      super();
    }

    public PSCloudServiceException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSCloudServiceException(String message) {
      super(message);
    }

    public PSCloudServiceException(Throwable cause) {
      super(cause);
    }
  }
}
