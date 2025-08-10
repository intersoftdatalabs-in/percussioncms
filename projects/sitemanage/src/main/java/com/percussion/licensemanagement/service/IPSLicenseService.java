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

package com.percussion.licensemanagement.service;

import com.percussion.licensemanagement.data.PSModuleLicense;
import com.percussion.licensemanagement.data.PSModuleLicenses;
import com.percussion.licensemanagement.error.PSLicenseServiceException;
import com.percussion.share.data.PSNoContent;
import com.percussion.share.service.IPSSystemProperties;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service interface for managing module licenses.
 *
 * <p>Sunny Sal says: "License management so smooth, even your modules will want to be legal!"
 */
public interface IPSLicenseService {

  // Netsuite response constants for getLicenseStatus
  String NETSUITE_STATUS_SUCCESS = "SUCCESS";
  String NETSUITE_STATUS_UNEXPECTED_ERROR = "UNEXPECTED_ERROR";
  String NETSUITE_STATUS_NO_ACCOUNT_EXISTS = "NO_ACCOUNT_EXISTS";
  String NETSUITE_STATUS_REGISTERED = "NOT_ACTIVE";
  String NETSUITE_STATUS_SUSPENDED = "SUSPENDED";
  String NETSUITE_STATUS_EXCEEDED_QUOTA = "EXCEEDED_QUOTA";

  // Custom constant to represent Suspended, need refresh state
  String CUSTOM_STATUS_SUSPENDED_REFRESH = "SUSPENDED_REFRESH";
  String CUSTOM_STATUS_ACTIVE_OVERLIMIT = "SUCCESS_OVERLIMIT";
  String CLOUD_LICENSES_URL_PROPNAME = "CLOUD_LICENSES_URL";

  // License Status constants
  String LICENSE_STATUS_ACTIVE = "Active";
  String LICENSE_STATUS_ACTIVE_OVERLIMIT = "Active, Overlimit";
  String LICENSE_STATUS_INACTIVE = "Inactive";
  String LICENSE_STATUS_REGISTERED = "Inactive, Registered";
  String LICENSE_STATUS_SUSPENDED_REFRESH = "Suspended, Refresh Required";
  String LICENSE_STATUS_SUSPENDED = "Suspended";
  String MODULE_LICENSE_TYPE_PAGE_OPTIMIZER = "PAGE_OPTIMIZER";
  String MODULE_LICENSE_TYPE_REDIRECT = "REDIRECT";

  // Module Licenses constants
  String MODULE_LICENSE_METADATA_KEY = "perc.license.module.license.data";

  /**
   * Saves a module license.
   *
   * @param moduleLicense the module license to save, not null
   * @return PSNoContent indicating operation result
   * @throws PSLicenseServiceException if saving fails
   */
  @POST
  @Path("/module/save")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  PSNoContent saveModuleLicense(PSModuleLicense moduleLicense) throws PSLicenseServiceException;

  /**
   * Deletes a module license.
   *
   * @param moduleLicense the module license to delete, not null
   * @return PSNoContent indicating operation result
   * @throws PSLicenseServiceException if deletion fails
   */
  @DELETE
  @Path("/module/delete")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  PSNoContent deleteModuleLicense(PSModuleLicense moduleLicense) throws PSLicenseServiceException;

  /**
   * Finds a module license by name.
   *
   * @param name the module name, not blank
   * @return the module license
   */
  @GET
  @Path("/module/{name}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  PSModuleLicense findModuleLicense(@PathParam("name") String name);

  /**
   * Finds all module licenses.
   *
   * @return all module licenses
   */
  @GET
  @Path("/module/all")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  PSModuleLicenses findAllModuleLicenses();

  /**
   * Sets the system properties for this service.
   *
   * @param systemProps the system properties
   */
  @Autowired
  void setSystemProps(IPSSystemProperties systemProps);

  /**
   * Gets the system properties for this service.
   *
   * @return the system properties
   */
  IPSSystemProperties getSystemProps();
}
