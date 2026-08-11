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
package com.percussion.pso.demandpreview.servlet;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSException;
import com.percussion.pso.demandpreview.service.DemandPublisherService;
import com.percussion.pso.demandpreview.service.ItemTemplateService;
import com.percussion.pso.demandpreview.service.LinkBuilderService;
import com.percussion.pso.demandpreview.service.SiteEditionHolder;
import com.percussion.pso.demandpreview.service.SiteEditionLookUpService;
import com.percussion.pso.utils.IPSOItemSummaryFinder;
import com.percussion.pso.utils.PSOItemSummaryFinderWrapper;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Spring MVC controller that triggers an on-demand preview publish of a single content item for the
 * requesting user, returning the published preview URL via the configured view.
 *
 * @author DavidBenua
 */
public class DemandPreviewController extends ParameterizableViewController implements Controller {

  /**
   * Creates a new DemandPreviewController.
   */
  public DemandPreviewController() {
    // default
  }

  private static final Logger log = LogManager.getLogger(DemandPreviewController.class);

  private String errorViewName = "error";
  private DemandPublisherService demandPublisherService = null;
  private ItemTemplateService itemTemplateService = null;
  private LinkBuilderService linkBuilderService = null;
  private SiteEditionLookUpService siteEditionLookUpService = null;
  private IPSOItemSummaryFinder isFinder = null;

  private IPSGuidManager gmgr = null;

  /**
   * Initializes service dependencies if they have not been injected.
   *
   * @throws Exception if initialization fails
   */
  public void init() throws Exception {
    if (gmgr == null) {
      gmgr = PSGuidManagerLocator.getGuidMgr();
    }
    if (isFinder == null) {
      isFinder = new PSOItemSummaryFinderWrapper();
    }
  }
  /**
   * handleRequestInternal operation.
   * @param request the request
   * @param response the response
   * @return the result
   * @throws Exception if an error occurs
   */

  @Override
  protected ModelAndView handleRequestInternal(
      HttpServletRequest request, HttpServletResponse response) throws Exception {
    String emsg;
    ModelAndView mav = super.handleRequestInternal(request, response);
    try {
      String contentId = request.getParameter(IPSHtmlParameters.SYS_CONTENTID);
      Validate.notEmpty(contentId);
      String folderId = request.getParameter(IPSHtmlParameters.SYS_FOLDERID);
      Validate.notEmpty(folderId);
      String siteId = request.getParameter(IPSHtmlParameters.SYS_SITEID);
      Validate.notEmpty(siteId);
      if (log.isDebugEnabled())
        log.debug(
            "Publishing for preview id:{}, folder:{}, and site: {}", contentId, folderId, siteId);
      String redirectTo = doPublishForPreview(contentId, folderId, siteId);
      log.debug("redirecting to:{}", redirectTo);
      mav.addObject("redirectTo", redirectTo);

    } catch (Exception e) {
      emsg = e.getMessage();
      log.error("Exception {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));

      mav.addObject("errorMessage", emsg);
      mav.setViewName(errorViewName);
    }

    return mav;
  }

  /**
   * Publishes the item for on-demand preview and returns the preview URL.
   *
   * @param contentId the content id to publish
   * @param folderId the folder id context
   * @param siteId the site id for edition lookup
   * @return the preview redirect URL
   * @throws PSAssemblyException if template assembly fails
   * @throws TimeoutException if the publish wait times out
   * @throws PSException if content lookup fails
   */
  protected String doPublishForPreview(String contentId, String folderId, String siteId)
      throws PSAssemblyException, TimeoutException, PSException {
    String redirectTo = null;
    PSLocator loc = isFinder.getCurrentOrEditLocator(contentId);

    IPSGuid contentGUID = gmgr.makeGuid(loc);
    log.debug("Content item is {}", contentGUID);
    IPSGuid folderGUID = gmgr.makeGuid(new PSLocator(folderId, "0"));

    SiteEditionHolder siteEditionHolder = siteEditionLookUpService.LookUpSiteEdition(siteId);
    Validate.notNull(siteEditionHolder.getSite());
    demandPublisherService.publishAndWait(siteEditionHolder.getEdition(), contentGUID, folderGUID);

    IPSAssemblyTemplate template =
        itemTemplateService.findTemplate(siteEditionHolder.getSite(), contentGUID);
    Validate.notNull(template);
    log.debug("using assembly context {}", siteEditionHolder.getContext().getName());
    redirectTo =
        linkBuilderService.buildLinkUrl(
            siteEditionHolder.getSite(),
            template,
            contentGUID,
            folderGUID,
            siteEditionHolder.getContext(),
            siteEditionHolder.getContextURLRootVar());
    log.debug("redirect address: {}", redirectTo);
    return redirectTo;
  }

  /**
   * Returns the ErrorViewName.
   *
   * @return the value
   */
  public String getErrorViewName() {
    return errorViewName;
  }

  /**
   * Sets the ErrorViewName.
   *
   * @param errorViewName the errorViewName
   */
  public void setErrorViewName(String errorViewName) {
    this.errorViewName = errorViewName;
  }

  /**
   * Returns the DemandPublisherService.
   *
   * @return the value
   */
  public DemandPublisherService getDemandPublisherService() {
    return demandPublisherService;
  }

  /**
   * Sets the DemandPublisherService.
   *
   * @param demandPublisherService the demandPublisherService
   */
  public void setDemandPublisherService(DemandPublisherService demandPublisherService) {
    this.demandPublisherService = demandPublisherService;
  }

  /**
   * Returns the ItemTemplateService.
   *
   * @return the value
   */
  public ItemTemplateService getItemTemplateService() {
    return itemTemplateService;
  }

  /**
   * Sets the ItemTemplateService.
   *
   * @param itemTemplateService the itemTemplateService
   */
  public void setItemTemplateService(ItemTemplateService itemTemplateService) {
    this.itemTemplateService = itemTemplateService;
  }

  /**
   * Returns the LinkBuilderService.
   *
   * @return the value
   */
  public LinkBuilderService getLinkBuilderService() {
    return linkBuilderService;
  }

  /**
   * Sets the LinkBuilderService.
   *
   * @param linkBuilderService the linkBuilderService
   */
  public void setLinkBuilderService(LinkBuilderService linkBuilderService) {
    this.linkBuilderService = linkBuilderService;
  }

  /**
   * Returns the SiteEditionLookUpService.
   *
   * @return the value
   */
  public SiteEditionLookUpService getSiteEditionLookUpService() {
    return siteEditionLookUpService;
  }

  /**
   * Sets the SiteEditionLookUpService.
   *
   * @param siteEditionLookUpService the siteEditionLookUpService
   */
  public void setSiteEditionLookUpService(SiteEditionLookUpService siteEditionLookUpService) {
    this.siteEditionLookUpService = siteEditionLookUpService;
  }

  /**
   * Sets the Gmgr.
   *
   * @param gmgr the gmgr
   */
  protected void setGmgr(IPSGuidManager gmgr) {
    this.gmgr = gmgr;
  }

  /**
   * Sets the IsFinder.
   *
   * @param isFinder the isFinder
   */
  protected void setIsFinder(IPSOItemSummaryFinder isFinder) {
    this.isFinder = isFinder;
  }
}
