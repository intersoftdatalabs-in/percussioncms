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
package com.percussion.pso.demandpreview.servlet;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.pso.demandpreview.service.DemandPublisherService;
import com.percussion.pso.demandpreview.service.ItemTemplateService;
import com.percussion.pso.demandpreview.service.LinkBuilderService;
import com.percussion.pso.demandpreview.service.SiteEditionHolder;
import com.percussion.pso.demandpreview.service.SiteEditionLookUpService;
import com.percussion.pso.utils.IPSOItemSummaryFinder;
import com.percussion.pso.utils.PSOItemSummaryFinderWrapper;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeoutException;

/**
 * @author DavidBenua
 *
 */
public class DemandPreviewController extends ParameterizableViewController
		implements Controller {

	private static final Logger log = LogManager.getLogger(DemandPreviewController.class);
	
	private String errorViewName = "error";  
	private DemandPublisherService demandPublisherService = null; 
	private ItemTemplateService itemTemplateService = null; 
	private LinkBuilderService linkBuilderService = null;
	private SiteEditionLookUpService siteEditionLookUpService = null; 
	private IPSOItemSummaryFinder isFinder = null; 
	
	private IPSGuidManager gmgr = null; 
	
	public void init() throws Exception
	{
		if(gmgr == null)
		{
			gmgr = PSGuidManagerLocator.getGuidMgr(); 
		}
		if(isFinder == null)
		{
			isFinder = new PSOItemSummaryFinderWrapper(); 
		}
	}
	
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String emsg; 
		ModelAndView mav = super.handleRequestInternal(request, response);
		try {
			String contentId = request.getParameter(IPSHtmlParameters.SYS_CONTENTID);
			Validate.notEmpty(contentId);
			String folderId = request.getParameter(IPSHtmlParameters.SYS_FOLDERID);
			Validate.notEmpty(folderId); 
			String siteId = request.getParameter(IPSHtmlParameters.SYS_SITEID);
			Validate.notEmpty(siteId);
			if(log.isDebugEnabled())
			   log.debug("Publishing for preview id:{}, folder:{}, and site: {}", contentId, folderId, siteId );
		   	String redirectTo = doPublishForPreview(contentId, folderId, siteId);
		   	log.debug("redirecting to:{}", redirectTo);
		   	mav.addObject("redirectTo", redirectTo); 
		   	
		} catch (Exception e){
			emsg = e.getMessage(); 
			log.error("Exception {}", PSExceptionUtils.getMessageForLog(e));
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));

			mav.addObject("errorMessage", emsg); 
			mav.setViewName(errorViewName); 
		}
		
		return mav; 
	}

	
	protected String doPublishForPreview(String contentId, String folderId, String siteId) 
	   throws PSAssemblyException, TimeoutException, PSException
	{
	    String redirectTo = null; 
	    PSLocator loc = isFinder.getCurrentOrEditLocator(contentId); 
	    
	    IPSGuid contentGUID = gmgr.makeGuid(loc);
	    log.debug("Content item is {}", contentGUID);
	    IPSGuid folderGUID = gmgr.makeGuid(new PSLocator(folderId, "0"));
	   
	    SiteEditionHolder siteEditionHolder = siteEditionLookUpService.LookUpSiteEdition(siteId);
	    Validate.notNull(siteEditionHolder.getSite()); 
	    demandPublisherService.publishAndWait(siteEditionHolder.getEdition(), contentGUID, folderGUID); 
       
	    IPSAssemblyTemplate template = itemTemplateService.findTemplate(siteEditionHolder.getSite(), contentGUID);
	    Validate.notNull(template); 
	    log.debug("using assembly context {}", siteEditionHolder.getContext().getName());
	    redirectTo = linkBuilderService.buildLinkUrl(siteEditionHolder.getSite(), template,
	            contentGUID, folderGUID, siteEditionHolder.getContext(),siteEditionHolder.getContextURLRootVar()); 
	    log.debug("redirect address: {}", redirectTo);
		return redirectTo; 
	}

	public String getErrorViewName() {
		return errorViewName;
	}


	public void setErrorViewName(String errorViewName) {
		this.errorViewName = errorViewName;
	}


	public DemandPublisherService getDemandPublisherService() {
		return demandPublisherService;
	}


	public void setDemandPublisherService(
			DemandPublisherService demandPublisherService) {
		this.demandPublisherService = demandPublisherService;
	}


	public ItemTemplateService getItemTemplateService() {
		return itemTemplateService;
	}


	public void setItemTemplateService(ItemTemplateService itemTemplateService) {
		this.itemTemplateService = itemTemplateService;
	}


	public LinkBuilderService getLinkBuilderService() {
		return linkBuilderService;
	}


	public void setLinkBuilderService(LinkBuilderService linkBuilderService) {
		this.linkBuilderService = linkBuilderService;
	}


	public SiteEditionLookUpService getSiteEditionLookUpService() {
		return siteEditionLookUpService;
	}


	public void setSiteEditionLookUpService(
			SiteEditionLookUpService siteEditionLookUpService) {
		this.siteEditionLookUpService = siteEditionLookUpService;
	}

	protected void setGmgr(IPSGuidManager gmgr) {
		this.gmgr = gmgr;
	}

	protected void setIsFinder(IPSOItemSummaryFinder isFinder) {
		this.isFinder = isFinder;
	}
	
	
	

}
