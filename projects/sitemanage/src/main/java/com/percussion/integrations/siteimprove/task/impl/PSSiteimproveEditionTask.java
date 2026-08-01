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
package com.percussion.integrations.siteimprove.task.impl;

import com.google.common.collect.Iterators;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.integrations.siteimprove.data.PSSiteImproveSiteConfigurations;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.pagemanagement.assembler.impl.PSAssemblyConfig;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.rx.publisher.IPSEditionTask;
import com.percussion.rx.publisher.IPSEditionTaskStatusCallback;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.integrations.IPSIntegrationProviderService;
import com.percussion.services.integrations.siteimprove.PSSiteImproveProviderService;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPubItemStatus;
import com.percussion.services.publisher.IPSSiteItem;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.databind.json.JsonMapper;

/**
 * A post publish edition task that runs if a Siteimprove configuration is found.
 *
 * <p>The task alerts Siteimprove that pages have been updated, or to crawl the whole site.
 */
public class PSSiteimproveEditionTask implements IPSEditionTask {

  private static final String SITEIMPROVE_CONFIGURATION_BASE_KEY = "perc.siteimprove.site.";
  private static final String SITEIMPROVE_CREDENTIALS_BASE_KEY = "perc.siteimprove.credentials.";
  private static final String TOKEN = "token";
  private static final String SITE_NAME = "sitename";
  private static final String DO_PRODUCTION = "doProduction";
  private static final String DO_STAGING = "doStaging";
  private static final String DO_ASSETS_SCAN_EXCLUDE = "doAssetsScanExclude";
  private static final String DO_PREVIEW = "doPreview";
  private static final String IS_SITEIMPROVE_ENABLED = "isSiteImproveEnabled";
  private static final String HTTPS = "https";
  private static final String HTTP = "http";
  private final ConcurrentHashMap<Long, String> templateDetails = new ConcurrentHashMap<>();
  private static final IPSIntegrationProviderService siteimproveService =
      new PSSiteImproveProviderService();
  private static final Logger logger = LogManager.getLogger(PSSiteimproveEditionTask.class);
  private IPSMetadataService metadataService;
  private IPSPubServerService pubServerService;

  @Override
  public void init(IPSExtensionDef def, File codeRoot) {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Override
  public void perform(
      IPSEdition edition,
      IPSSite site,
      Date startTime,
      Date endTime,
      long jobId,
      long duration,
      boolean success,
      Map<String, String> params,
      IPSEditionTaskStatusCallback status)
      throws Exception {
    logger.info("Starting Siteimprove post edition task.");

    var credentialsMetadata =
        metadataService.find(SITEIMPROVE_CREDENTIALS_BASE_KEY + site.getName());
    var siteConfiguration =
        metadataService.find(SITEIMPROVE_CONFIGURATION_BASE_KEY + site.getName());

    if (credentialsMetadata == null || siteConfiguration == null) {
      logger.debug(
          "Did not find Siteimprove credentials or configurations for: {}. Exiting Siteimprove post"
              + " edition task.",
          site.getName());
      return;
    }

    var credentials = obtainToken(credentialsMetadata.getData());
    var siteConfigurations = obtainSiteConfiguration(siteConfiguration.getData());

    var publishServerInfo =
        pubServerService.getPubServer(
            site.getSiteId().toString(), Long.toString(edition.getPubServerId().longValue()));

    var jobPages = status.getIterableJobStatus();
    var pageCount = Iterators.size(jobPages.iterator());
    var siteBaseUrl = getBaseURL(site);
    handleProductionOrStagingEnabled(
        credentials,
        siteConfigurations,
        publishServerInfo,
        jobPages,
        pageCount,
        siteBaseUrl,
        edition);
  }

  private void handleProductionOrStagingEnabled(
      Map<String, String> credentials,
      PSSiteImproveSiteConfigurations siteConfigurations,
      PSPublishServerInfo publishServerInfo,
      Iterable<IPSPubItemStatus> jobPages,
      int pageCount,
      String siteBaseUrl,
      IPSEdition edition)
      throws Exception {
    if (isProductionEnabled(siteConfigurations, publishServerInfo)
        || isStagingEnabled(siteConfigurations, publishServerInfo)) {
      alertSiteImproveToNewPublishes(
          credentials, jobPages, siteBaseUrl, edition, siteConfigurations);
      logger.info("Submitted {} URL(s) to Siteimprove.", pageCount);
      logger.info("Ending Siteimprove post edition task.");
    } else {
      logger.info(
          "No production or staging settings are configured for Siteimprove, publishing no URLs to"
              + " Siteimprove.");
    }
  }

  private String getBaseURL(IPSSite site) {
    var siteBaseUrl = site.getBaseUrl();
    // If the user doesn't have canonical URLs set, we set to http as default.
    if (site.getSiteProtocol() == null) site.setSiteProtocol(HTTP);
    // Retrieve Siteimprove site id.
    if (!siteBaseUrl.contains(HTTPS) && HTTPS.equals(site.getSiteProtocol())) {
      siteBaseUrl = convertHTTPtoHTTPS(siteBaseUrl);
    }
    return siteBaseUrl;
  }

  /**
   * Alert Siteimprove that we have published new pages for them to spider, or do a full crawl of
   * the site.
   */
  private void alertSiteImproveToNewPublishes(
      Map<String, String> credentials,
      Iterable<IPSPubItemStatus> jobPages,
      String siteBaseUrl,
      IPSEdition edition,
      PSSiteImproveSiteConfigurations siteConfigurations)
      throws Exception {
    if (edition.getDisplayTitle().contains("_FULL")) {
      siteimproveService.updateSiteInfo(siteBaseUrl, credentials);
    } else if (isAssetsScanExcludeEnabled(siteConfigurations)) {
      // This condition checks whether we have to exclude assets from scanning
      for (var jobPage : jobPages) {
        // This condition excludes assets from scanning and scans only pages.
        if (isTemplateMatch(jobPage) && !jobPage.getLocation().startsWith("/Assets")) {
          alertSiteImproveUpdatePageInfo(credentials, jobPage, siteBaseUrl);
        }
      }
    } else {
      // Individual page checks
      for (var jobPage : jobPages) {
        alertSiteImproveUpdatePageInfo(credentials, jobPage, siteBaseUrl);
      }
    }
  }

  /** Determine if we are a production publish and that production is enabled. */
  private boolean isProductionEnabled(
      PSSiteImproveSiteConfigurations siteConfigurations, PSPublishServerInfo publishServerInfo) {
    var enabled =
        PSPubServer.PRODUCTION.equalsIgnoreCase(publishServerInfo.getServerType())
            && Boolean.TRUE.equals(siteConfigurations.getDoProduction());
    if (enabled) {
      logger.debug(
          "Production configuration is enabled for this site for Siteimprove, alerting Siteimprove"
              + " to update indices.");
    }
    return enabled;
  }

  /** Determine if we are a staging publish and that staging is enabled. */
  private boolean isStagingEnabled(
      PSSiteImproveSiteConfigurations siteConfigurations, PSPublishServerInfo publishServerInfo) {
    var enabled =
        PSPubServer.STAGING.equalsIgnoreCase(publishServerInfo.getServerType())
            && Boolean.TRUE.equals(siteConfigurations.getDoStaging());
    if (enabled) {
      logger.debug(
          "Staging configuration is enabled for this site for Siteimprove, alerting Siteimprove to"
              + " update indices.");
    }
    return enabled;
  }

  /** Determine if assets scan exclude is enabled. */
  private boolean isAssetsScanExcludeEnabled(PSSiteImproveSiteConfigurations siteConfigurations) {
    var enabled = Boolean.TRUE.equals(siteConfigurations.getDoAssetsScanExclude());
    if (enabled) {
      logger.debug(
          "Assets scan exclude is enabled for this site for Siteimprove, alerting Siteimprove to"
              + " update indices.");
    }
    return enabled;
  }

  /** Convert a URL that is http to https. */
  private String convertHTTPtoHTTPS(String siteBaseUrl) {
    return StringUtils.replace(siteBaseUrl, HTTP, HTTPS);
  }

  /** Obtain the token and site name from a metadata JSON. */
  private Map<String, String> obtainToken(String credentialsData) throws Exception {
    var mapper = JsonMapper.builder().build();
    var credentialsJSON = mapper.readValue(credentialsData, java.util.LinkedHashMap.class);
    var credentials = new HashMap<String, String>();
    if (!credentialsJSON.containsKey(SITE_NAME)) {
      var message = "The credentials were missing the associated site name.";
      logger.error(message);
      throw new Exception(message);
    }
    if (!credentialsJSON.containsKey(TOKEN)) {
      var message = "The credentials were missing the apikey.";
      logger.error(message);
      throw new Exception(message);
    }
    credentials.put(SITE_NAME, (String) credentialsJSON.get(SITE_NAME));
    credentials.put(TOKEN, (String) credentialsJSON.get(TOKEN));
    credentials.put("siteProtocol", (String) credentialsJSON.getOrDefault("siteProtocol", ""));
    credentials.put(
        "defaultDocument", (String) credentialsJSON.getOrDefault("defaultDocument", ""));
    credentials.put("canonicalDist", (String) credentialsJSON.getOrDefault("canonicalDist", ""));
    return credentials;
  }

  /** Parse metadata JSON for our Siteimprove configuration settings. */
  private PSSiteImproveSiteConfigurations obtainSiteConfiguration(String siteConfigurationData)
      throws Exception {
    var mapper = JsonMapper.builder().build();
    var siteConfigurationJson =
        mapper.readValue(siteConfigurationData, java.util.LinkedHashMap.class);
    if (!siteConfigurationJson.containsKey(DO_PRODUCTION)) {
      var message = "Siteimprove configuration details were missing the production setting";
      logger.error(message);
      throw new Exception(message);
    }
    if (!siteConfigurationJson.containsKey(DO_STAGING)) {
      var message = "Siteimprove configuration details were missing the staging setting";
      logger.error(message);
      throw new Exception(message);
    }
    if (!siteConfigurationJson.containsKey(DO_ASSETS_SCAN_EXCLUDE)) {
      var message =
          "Siteimprove configuration details were missing the assets scan exclude setting";
      logger.error(message);
      throw new Exception(message);
    }
    if (!siteConfigurationJson.containsKey(DO_PREVIEW)) {
      var message = "Siteimprove configuration details were missing the preview setting";
      logger.error(message);
      throw new Exception(message);
    }
    if (!siteConfigurationJson.containsKey(IS_SITEIMPROVE_ENABLED)) {
      var message =
          "Siteimprove configuration details were missing the Siteimprove enabled setting";
      logger.error(message);
      throw new Exception(message);
    }
    var siteConfiguration = new PSSiteImproveSiteConfigurations();
    siteConfiguration.setDoProduction((Boolean) siteConfigurationJson.get(DO_PRODUCTION));
    siteConfiguration.setDoStaging((Boolean) siteConfigurationJson.get(DO_STAGING));
    siteConfiguration.setDoAssetsScanExclude(
        (Boolean) siteConfigurationJson.get(DO_ASSETS_SCAN_EXCLUDE));
    siteConfiguration.setDoPreview((Boolean) siteConfigurationJson.get(DO_PREVIEW));
    siteConfiguration.setIsSiteImproveEnabled(
        (Boolean) siteConfigurationJson.get(IS_SITEIMPROVE_ENABLED));
    return siteConfiguration;
  }

  /** Determine if the template name of the job page is matching with the standard template name. */
  private boolean isTemplateMatch(IPSPubItemStatus jobPage) throws PSAssemblyException {
    var isEnabled = false;
    var templateId = jobPage.getTemplateId();
    var templateName = "";
    if (!templateDetails.isEmpty() && templateDetails.containsKey(templateId)) {
      templateName = templateDetails.get(templateId);
    } else {
      var assembly = PSAssemblyServiceLocator.getAssemblyService();
      var guid = PSGuidManagerLocator.getGuidMgr().makeGuid(templateId, PSTypeEnum.TEMPLATE);
      var template = assembly.loadUnmodifiableTemplate(guid);
      templateName = template.getName();
      templateDetails.put(templateId, templateName);
    }
    if (PSAssemblyConfig.PERC_RESOURCE_ASSEMBLY_TEMPLATE.equals(templateName)) {
      isEnabled = true;
    }
    return isEnabled;
  }

  /**
   * Alert Siteimprove that we have published new pages for them to spider, or do a full crawl of
   * the site.
   */
  private void alertSiteImproveUpdatePageInfo(
      Map<String, String> credentials, IPSPubItemStatus jobPage, String siteBaseUrl)
      throws Exception {
    // Remove last forward slash
    var url = StringUtils.removeEnd(siteBaseUrl, "/") + jobPage.getLocation();
    if (IPSSiteItem.Status.SUCCESS.equals(jobPage.getStatus())) {
      siteimproveService.updatePageInfo(siteBaseUrl, url, credentials);
    } else {
      logger.debug(
          "Did not submit page:{} to Siteimprove because of page's status of {}",
          jobPage.getContentId(),
          jobPage.getStatus());
    }
  }

  @Override
  public TaskType getType() {
    return TaskType.POSTEDITION;
  }

  public IPSPubServerService getPubServerService() {
    return pubServerService;
  }

  public void setPubServerService(IPSPubServerService pubServerService) {
    this.pubServerService = pubServerService;
  }

  public IPSMetadataService getMetadataService() {
    return metadataService;
  }

  public void setMetadataService(IPSMetadataService metadataService) {
    this.metadataService = metadataService;
  }
}
