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
package com.percussion.pubserver;

import com.percussion.error.PSException;
import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pubserver.IPSPubServer;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSPubInfo;
import com.percussion.utils.guid.IPSGuid;
import java.util.List;
import java.util.Map;

/**
 * The pubserver service is responsible for exposing the publish server information.
 *
 * @author leonardohildt
 * @author ignacioerro
 */
public interface IPSPubServerService {
  String DEFAULT_DTS = IPSPubServer.DEFAULT_DTS;

  /**
   * Loads the server information based on the site and server IDs.
   *
   * @param siteId the site ID
   * @param serverId the server ID
   * @return a {@link PSPublishServerInfo} object, never {@code null}
   * @throws PSPubServerServiceException if an error occurs
   */
  PSPublishServerInfo getPubServer(String siteId, String serverId)
      throws PSPubServerServiceException;

  /**
   * Retrieves a list of all publish servers for a site.
   *
   * @param name the site name or ID
   * @return a list of {@link PSPublishServerInfo}, never {@code null}
   * @throws PSPubServerServiceException if an error occurs
   */
  List<PSPublishServerInfo> getPubServerList(String name) throws PSPubServerServiceException;

  /**
   * Creates a new publish server with the provided name.
   *
   * @param siteId the site ID, never empty or {@code null}
   * @param serverName the name of the publish server to be created
   * @param pubServerInfo the {@link PSPublishServerInfo} object containing the server information
   * @return a {@link PSPublishServerInfo} object, never {@code null}
   * @throws PSPubServerServiceException if the supplied object is invalid
   */
  PSPublishServerInfo createPubServer(
      String siteId, String serverName, PSPublishServerInfo pubServerInfo)
      throws PSPubServerServiceException, PSNotFoundException, PSValidationException;

  /**
   * Updates a publish server with the provided name.
   *
   * @param siteId the site ID, never empty or {@code null}
   * @param serverId the ID of the publish server to be updated
   * @param pubServerInfo the {@link PSPublishServerInfo} object containing the server information
   * @return a {@link PSPublishServerInfo} object, never {@code null}
   * @throws PSPubServerServiceException if the supplied object is invalid
   */
  PSPublishServerInfo updatePubServer(
      String siteId, String serverId, PSPublishServerInfo pubServerInfo)
      throws PSPubServerServiceException, PSDataServiceException, PSNotFoundException;

  /**
   * Deletes a publish server for a site.
   *
   * @param siteId the site ID
   * @param serverId the server ID
   * @return a list of updated {@link PSPublishServerInfo} objects
   * @throws PSPubServerServiceException if an error occurs
   */
  List<PSPublishServerInfo> deleteServer(String siteId, String serverId)
      throws PSPubServerServiceException, PSDataServiceException, PSNotFoundException;

  /**
   * Deletes all publish servers that belong to the specified site.
   *
   * @param siteId the ID of the site, never {@code null}
   */
  void deletePubServersBySite(IPSGuid siteId);

  /**
   * Stops publishing for the given job ID.
   *
   * @param jobId the job ID
   * @throws PSPubServerServiceException if an error occurs
   */
  void stopPublishing(String jobId) throws PSPubServerServiceException;

  /**
   * Gets information about available drivers.
   *
   * @return a map of driver names to availability, never {@code null}
   */
  Map<String, Boolean> getAvailableDrivers();

  /**
   * Determines if the default publish server for a site is modified.
   *
   * @param siteId the site ID
   * @return {@code true} if the default server was modified by the user
   */
  Boolean isDefaultServerModified(String siteId);

  /**
   * Returns the default folder location for a new server.
   *
   * @param siteId the site ID
   * @param publishType the type of publication server
   * @param driver the driver for the new server
   * @param serverType the server type
   * @return the path for the default publishing location
   */
  String getDefaultFolderLocation(
      String siteId, String publishType, String driver, String serverType);

  /**
   * Returns the default publish server defined for the site.
   *
   * @param siteId the site ID
   * @return a {@link PSPubServer} object, never {@code null}
   */
  PSPubServer getDefaultPubServer(IPSGuid siteId) throws PSNotFoundException;

  /**
   * Returns the staging publish server defined for the site.
   *
   * @param siteId the site GUID, never {@code null}
   * @return a {@link PSPubServer} object, may be {@code null} if not created
   */
  PSPubServer getStagingPubServer(IPSGuid siteId) throws PSNotFoundException;

  /**
   * Creates a new server with default settings based on the site name.
   *
   * @param site the associated site
   * @param serverName the name for the new publishing server
   * @return the {@link PSPubServer} object, may be {@code null} if it cannot be created
   * @throws PSPubServerServiceException if an error occurs
   */
  PSPubServer createDefaultPubServer(IPSSite site, String serverName)
      throws PSPubServerServiceException;

  /**
   * Updates folder root after renaming a site.
   *
   * @param site never {@code null}
   * @param root the root with the new name, never {@code null}
   * @param oldName the old site name, may be {@code null}
   * @return {@code true} if the folder location was changed for any pub server
   */
  boolean updateDefaultFolderLocation(IPSSite site, String root, String oldName);

  /** Exception thrown when an error is encountered in the publish service. */
  class PSPubServerServiceException extends PSException {
    private static final long serialVersionUID = 1L;

    public PSPubServerServiceException() {
      super();
    }

    public PSPubServerServiceException(String message) {
      super(message);
    }

    public PSPubServerServiceException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSPubServerServiceException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Checks the configuration of a publish server.
   *
   * @param pubServerInfo the server info
   * @param site the site
   * @return {@code true} if the configuration is valid
   */
  boolean checkPubServerConfig(PSPublishServerInfo pubServerInfo, IPSSite site);

  /**
   * Returns S3 publishing info if the default pubserver for the supplied site is Amazon S3.
   *
   * @param siteId must not be {@code null}
   * @return {@link PSPubInfo} of Amazon S3 pub server, may be {@code null}
   * @throws PSPubServerServiceException if an error occurs
   */
  PSPubInfo getS3PubInfo(IPSGuid siteId) throws PSPubServerServiceException, PSNotFoundException;

  /**
   * Finds the pub server for the supplied server ID, returns null if server doesn't exist.
   *
   * @param serverId the server ID
   * @return pub server, may be {@code null} if not found
   * @throws PSPubServerServiceException if an error occurs
   */
  PSPubServer findPubServer(long serverId) throws PSPubServerServiceException;

  /**
   * Finds the pub server for the supplied GUID, returns null if not found.
   *
   * @param guid a valid pub server GUID
   * @return the matching pub server or null if not found
   * @throws PSPubServerServiceException if an error occurs
   */
  PSPubServer findPubServer(IPSGuid guid) throws PSPubServerServiceException;

  /**
   * Gets the default admin URL for a site.
   *
   * @param siteName the site name
   * @return the admin URL
   * @throws PSPubServerServiceException if an error occurs
   * @throws PSNotFoundException if the site is not found
   */
  String getDefaultAdminURL(String siteName)
      throws PSPubServerServiceException, PSNotFoundException;
}
