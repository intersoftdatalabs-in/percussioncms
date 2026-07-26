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

// REFACTORED: CP-JAVA11
package com.percussion.integrations.siteimprove.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.percussion.integrations.siteimprove.data.PSSiteImproveCredentials;
import com.percussion.integrations.siteimprove.data.PSSiteImproveSiteConfigurations;
import com.percussion.metadata.data.PSMetadata;
import com.percussion.metadata.data.PSMetadataList;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.services.integrations.siteimprove.PSSiteImproveProviderService;
import com.percussion.services.publisher.IPSEditionTaskDef;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** REST endpoints for our integration with Siteimprove. */
@Path("/siteimprove")
@Lazy
@PSSiteManageBean("siteImproveService")
public class PSSiteimprove {

  private static final PSSiteImproveProviderService providerService =
      new PSSiteImproveProviderService();
  private static final IPSSiteManager sitemgr = PSSiteManagerLocator.getSiteManager();
  private IPSMetadataService metadataService;
  private static final Logger logger = LogManager.getLogger(PSSiteimprove.class);

  private static final String SITEIMPROVE_CREDENTIALS_BASE_KEY = "perc.siteimprove.credentials.";
  private static final String SITEIMPROVE_CONFIGURATION_BASE_KEY = "perc.siteimprove.site.";
  private static final String EXT_NAME =
      "Java/global/percussion/task/perc_PSSiteimproveEditionTask";

  // Generic client-facing error messages used to avoid leaking internal exception details
  // (CWE-209 / CodeQL java/error-message-exposure). The detailed exception is always logged
  // server-side before this generic message is returned.
  private static final String GENERIC_STORE_CREDENTIALS_ERROR =
      "Failed to store Siteimprove credentials";

  private static final String GENERIC_SAVE_CONFIG_ERROR =
      "Failed to save Siteimprove configuration settings";

  /**
   * Auto wires the metadata service for us to use.
   *
   * @param service auto wired service
   */
  @Autowired
  public PSSiteimprove(IPSMetadataService service) {
    this.metadataService = service;
  }

  /**
   * Retrieve all site improve tokens for the CM1 instance.
   *
   * @return 200 with the tokens or 500 if it failed to retrieve.
   */
  @GET
  @Path("/token")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<PSMetadata> retrieveAllSiteImproveTokens() {
    try {
      return new PSMetadataList(metadataService.findByPrefix(SITEIMPROVE_CREDENTIALS_BASE_KEY));
    } catch (IPSGenericDao.LoadException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Retrieve the tokens for a given site.
   *
   * @param siteName The name of the site we want to retrieve the credentials for.
   * @return 200 with the credentials or 500 if failed to retrieve.
   */
  @GET
  @Path("/token/{siteName}")
  @Produces(MediaType.APPLICATION_JSON)
  public PSMetadata retrieveSiteImproveTokens(@PathParam("siteName") String siteName) {
    try {
      var credentialsKey = SITEIMPROVE_CREDENTIALS_BASE_KEY + siteName;
      return metadataService.find(credentialsKey);
    } catch (IPSGenericDao.LoadException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Called when Siteimprove is enabled for a site using the Dashboard gadget. We do not need a
   * sitename because the token returned is account specific, not site specific. The token will be
   * applied to all sites that enable Siteimprove. Also called if the refresh token method is called
   * from the 'advanced' button within the gadget.
   *
   * @return 200 if successful with a json response with {"token":"token"} or 500 if error
   */
  @GET
  @Path("/getnewtoken")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSiteImproveTokenForAccount() {
    var token = providerService.getNewSiteImproveToken();
    if (!token.isEmpty()) {
      return Response.status(200).entity("{\"token\":\"" + token + "\"}").build();
    }
    return Response.serverError().entity("Unable to get Siteimprove token.").build();
  }

  /**
   * Updates the Siteimprove token within the PSMetadata object for the site.
   *
   * @param credentials The Sitename, email address and apikey of the siteimprove account.
   * @return 200 if valid. 401 if invalid and 500 if we ran into an exception.
   */
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/token")
  public Response storeSiteImproveToken(PSSiteImproveCredentials credentials) {
    try {
      var credentialsToValidate = new HashMap<String, String>();
      credentialsToValidate.put("token", credentials.getToken());
      credentialsToValidate.put("sitename", credentials.getSiteName());
      credentialsToValidate.put("siteProtocol", credentials.getSiteProtocol().orElse(""));
      credentialsToValidate.put("defaultDocument", credentials.getDefaultDocument().orElse(""));
      credentialsToValidate.put("canonicalDist", credentials.getCanonicalDist().orElse(""));
      var validCredentials = providerService.validateCredentials(credentialsToValidate);

      if (Boolean.TRUE.equals(validCredentials)) {
        var jsonMap = new LinkedHashMap<String, Object>(credentialsToValidate);
        addSiteImproveTaskToPreExistingEditions(credentials.getSiteName());
        var metadataKey = SITEIMPROVE_CREDENTIALS_BASE_KEY + credentials.getSiteName();
        metadataService.save(
            new PSMetadata(metadataKey, new ObjectMapper().writeValueAsString(jsonMap)));
        // CMS-8189: Upgraded jquery unable to parse if response is empty string. Advisable to
        // return "204 - No content" to avoid jquery parser error for json.
        return Response.noContent().build();
      }
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("Failed to validate credentials against siteImprove")
          .build();
    } catch (Exception e) {
      logger.error("Failed to store Siteimprove credentials. Exception is: {}", e.getMessage(), e);
      return Response.serverError().entity(GENERIC_STORE_CREDENTIALS_ERROR).build();
    }
  }

  /**
   * Store or update a site's configuration settings with Siteimprove.
   *
   * @return 200 or server error.
   */
  @PUT
  @Path("publish/config")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public Response storeSiteImproveConfiguration(PSSiteImproveSiteConfigurations publishSettings) {
    try {
      // IF invalid settings
      if (!validateConfiguration(publishSettings)) {
        throw new Exception("Settings may not be null and a valid site name must be provided.");
      }
      var siteConfigKey = SITEIMPROVE_CONFIGURATION_BASE_KEY + publishSettings.getSiteName();
      var publishSettingsJSON = new LinkedHashMap<String, Object>();
      publishSettingsJSON.put("doProduction", publishSettings.getDoProduction().orElse(false));
      publishSettingsJSON.put("doStaging", publishSettings.getDoStaging().orElse(false));
      publishSettingsJSON.put(
          "doAssetsScanExclude", publishSettings.getDoAssetsScanExclude().orElse(false));
      publishSettingsJSON.put("doPreview", publishSettings.getDoPreview().orElse(false));
      publishSettingsJSON.put(
          "isSiteImproveEnabled", publishSettings.getIsSiteImproveEnabled().orElse(false));
      metadataService.save(
          new PSMetadata(
              siteConfigKey, new ObjectMapper().writeValueAsString(publishSettingsJSON)));
      // CMS-8189: Upgraded jquery unable to parse if response is empty string. Advisable to return
      // "204 - No content" to avoid jquery parser error for json.
      return Response.noContent().build();
    } catch (Exception e) {
      logger.error(
          "Failed to save configuration settings for {}. Exception is {}",
          publishSettings.getSiteName(),
          e.getMessage(),
          e);
      return Response.serverError().entity(GENERIC_SAVE_CONFIG_ERROR).build();
    }
  }

  /**
   * Remove the Siteimprove configuration for a given site.
   *
   * @param siteName The site we want to remove the configuration information for.
   */
  @DELETE
  @Path("delete/config/{siteName}")
  public void deleteSiteImproveConfiguration(@PathParam("siteName") String siteName) {
    try {
      metadataService.delete(SITEIMPROVE_CONFIGURATION_BASE_KEY + siteName);
      metadataService.delete(SITEIMPROVE_CREDENTIALS_BASE_KEY + siteName);
      removeSiteImproveTaskFromEditions(siteName);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Retrieve Siteimprove configuration for the given site.
   *
   * @param siteName The site we want to retrieve the configuration information from.
   * @return The Siteimprove configuration settings for the associated site.
   */
  @GET
  @Path("publish/config/{siteName}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSMetadata retrieveSiteImproveConfiguration(@PathParam("siteName") String siteName) {
    try {
      var siteConfigKey = SITEIMPROVE_CONFIGURATION_BASE_KEY + siteName;
      return metadataService.find(siteConfigKey);
    } catch (IPSGenericDao.LoadException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Retrieve all Siteimprove configuration for every site in the system.
   *
   * @return Siteimprove configurations.
   */
  @GET
  @Path("publish/config")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public Collection<PSMetadata> retrieveAllSiteImproveConfiguration() {
    try {
      return metadataService.findByPrefix(SITEIMPROVE_CONFIGURATION_BASE_KEY);
    } catch (IPSGenericDao.LoadException e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Validate that the configuration has no nulls or an empty string for the site name.
   *
   * @param configuration The configuration to validate.
   * @return True if siteName is not null or empty, and that the configuration settings are not
   *     null.
   */
  private boolean validateConfiguration(PSSiteImproveSiteConfigurations configuration) {
    var validSiteName =
        configuration.getSiteName() != null && !configuration.getSiteName().isEmpty();
    return validSiteName
        && configuration.getDoPreview().isPresent()
        && configuration.getDoStaging().isPresent()
        && configuration.getDoAssetsScanExclude().isPresent()
        && configuration.getDoProduction().isPresent();
  }

  /**
   * For the given site, see that all editions for it, add the Siteimprove post edition task if it
   * is not there.
   *
   * @param siteName The site whose editions we are adding the new post edition task to.
   */
  private void addSiteImproveTaskToPreExistingEditions(String siteName) {
    var pubService = PSPublisherServiceLocator.getPublisherService();
    var site = sitemgr.findSite(siteName);
    var editions = pubService.findAllEditionsBySite(site.getGUID());
    for (var edition : editions) {
      var tasks = pubService.loadEditionTasks(edition.getGUID());
      if (!hasSiteimproveTask(tasks)) {
        // Add the Siteimprove publication cache post edition task on the editions if they don't
        // have that already
        logger.info(
            "Adding the {} post editiontask on the edition that doesn't have it already", EXT_NAME);
        var siteImprovePostEditionTask = pubService.createEditionTask();
        siteImprovePostEditionTask.setContinueOnFailure(true);
        siteImprovePostEditionTask.setEditionId(edition.getGUID());
        siteImprovePostEditionTask.setSequence(3);
        siteImprovePostEditionTask.setExtensionName(EXT_NAME);
        pubService.saveEditionTask(siteImprovePostEditionTask);
        logger.info("Finished adding {} post edition task on the edition", EXT_NAME);
      }
    }
  }

  /**
   * For the given site, see that all editions for it, remove the Siteimprove post edition task if
   * it exists.
   *
   * @param siteName The site from whose editions we are removing the post edition task.
   */
  private void removeSiteImproveTaskFromEditions(String siteName) throws Exception {
    var pubService = PSPublisherServiceLocator.getPublisherService();
    var site = sitemgr.findSite(siteName);
    var editions = pubService.findAllEditionsBySite(site.getGUID());
    for (var edition : editions) {
      var tasks = pubService.loadEditionTasks(edition.getGUID());
      if (hasSiteimproveTask(tasks)) {
        for (var task : tasks) {
          if (EXT_NAME.equals(task.getExtensionName())) {
            logger.info(
                "Removing the {} post editiontask from the edition that have it.", EXT_NAME);
            pubService.deleteEditionTask(task);
            logger.info("Finished removing {} post edition task on the edition", EXT_NAME);
          }
        }
      }
    }
  }

  /**
   * Verify that the Siteimprove post edition task does not already exist, before we add it.
   *
   * @param tasks See if any of the tasks are a Siteimprove task, if so return true so we don't add
   *     it a second time.
   * @return true if there is a pre-existing Siteimprove task, false otherwise.
   */
  private boolean hasSiteimproveTask(Collection<IPSEditionTaskDef> tasks) {
    return tasks.stream().anyMatch(task -> EXT_NAME.equals(task.getExtensionName()));
  }
}
