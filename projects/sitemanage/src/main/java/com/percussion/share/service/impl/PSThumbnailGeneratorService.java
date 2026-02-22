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

package com.percussion.share.service.impl;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.server.PSRequest;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.share.service.impl.PSThumbnailRunner.Function;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.request.PSRequestInfo;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service for generating thumbnails for templates and pages. Sunny Sal says: "Thumbnails so sharp,
 * even your boss will be impressed!"
 */
@PSSiteManageBean("thumbnailGeneratorService")
public class PSThumbnailGeneratorService {

  private final IPSSiteTemplateService siteTemplateService;
  private final IPSTemplateService templateService;
  private final IPSPageService pageService;
  private IPSSystemProperties systemProps;
  private boolean isServerStarted = false;

  @Autowired
  public PSThumbnailGeneratorService(
      IPSSiteTemplateService siteTemplateService,
      IPSTemplateService templateService,
      IPSPageService pageService,
      IPSNotificationService notificationService) {
    this.siteTemplateService = siteTemplateService;
    this.templateService = templateService;
    this.pageService = pageService;

    // old template/page-specific events were removed; use TEMPLATE_CHANGED
    notificationService.addListener(
        EventType.TEMPLATE_CHANGED,
        event -> generateTemplateThumbnail((String) event.getTarget(), true));
    // page-specific thumbnail support is not wired to notification events any more
    notificationService.addListener(
        EventType.CORE_SERVER_INITIALIZED, event -> isServerStarted = true);
    // deletion of a template is treated as a change
    notificationService.addListener(
        EventType.TEMPLATE_CHANGED,
        event ->
            delete(
                (String) event.getTarget(), PSThumbnailRunner.Function.DELETE_TEMPLATE_THUMBNAIL));
    notificationService.addListener(
        EventType.CORE_SERVER_SHUTDOWN, event -> PSThumbnailRunner.shutdown());
  }

  /**
   * Set the system properties on this service. This service will always use the values provided by
   * the most recently set instance of the properties.
   *
   * @param systemProps the system properties
   */
  public void setSystemProps(IPSSystemProperties systemProps) {
    this.systemProps = systemProps;
    Integer i = null;
    if (isServerStarted) {
      try {
        i = Integer.parseInt(systemProps.getProperty("thumbnailWorkerLimit"));
      } catch (Exception e) {
        i = -1;
      }
    }
    PSThumbnailRunner.setActiveWorkerLimit(i);
  }

  /**
   * Gets the system properties used by this service.
   *
   * @return The properties
   */
  public IPSSystemProperties getSystemProps() {
    return systemProps;
  }

  private void delete(String id, Function function) {
    doRun(id, function, null, null);
  }

  private void generateTemplateThumbnail(String templateId, boolean waitForCompletion) {
    doRun(templateId, PSThumbnailRunner.Function.GENERATE_TEMPLATE_THUMBNAIL, null, null);
  }



  private void doRun(
      String id, PSThumbnailRunner.Function function, PSPage page, PSTemplate template) {
    final Map<String, Object> requestInfoMap = PSRequestInfo.copyRequestInfoMap();
    var request = (PSRequest) requestInfoMap.get(PSRequestInfo.KEY_PSREQUEST);
    requestInfoMap.put(PSRequestInfo.KEY_PSREQUEST, request.cloneRequest());
    var runner =
        new PSThumbnailRunner(
            siteTemplateService, templateService, pageService, true, requestInfoMap);
    PSThumbnailRunner.scheduleThumbnailJob(id, function);
    var thumbnailRunner = new Thread(runner);
    thumbnailRunner.setDaemon(true);
    thumbnailRunner.start();
  }
}
