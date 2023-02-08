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
package com.percussion.pso.preview;

import com.percussion.pso.utils.SimplifyParameters;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author DavidBenua
 *
 */
public class MultiSiteResolutionController extends ParameterizableViewController
      implements
         Controller
{
   
   private static final Logger log = LogManager.getLogger(MultiSiteResolutionController.class);
   
   private SiteFolderFinder siteFolderFinder; 
   private UrlBuilder urlBuilder; 
   
   private static IPSGuidManager gmgr = null;
   private static IPSAssemblyService asm = null; 
   
   /**
    * 
    */
   public MultiSiteResolutionController()
   {
     
   }
   
   private static void initServices()
   {
      if(asm == null)
      {
         gmgr = PSGuidManagerLocator.getGuidMgr(); 
         asm = PSAssemblyServiceLocator.getAssemblyService(); 
      }
   }
   /**
    * @see org.springframework.web.servlet.mvc.AbstractController#handleRequestInternal(HttpServletRequest, HttpServletResponse)
    */
   @Override
   @SuppressWarnings({ "unchecked", "rawtypes" })
   protected ModelAndView handleRequestInternal(HttpServletRequest request,
         HttpServletResponse response) throws Exception
   {
      String emsg; 
      ModelAndView mav = super.handleRequestInternal(request, response);
      
      String templateid = request.getParameter(IPSHtmlParameters.SYS_TEMPLATE);
      if(StringUtils.isBlank(templateid))
      {
         templateid = request.getParameter(IPSHtmlParameters.SYS_VARIANTID);
         if(StringUtils.isBlank(templateid))
         {
            emsg = "template or variant has not been specified";
            log.error(emsg); 
            throw new RuntimeException(emsg);
         }
      }
      String contentid = request.getParameter(IPSHtmlParameters.SYS_CONTENTID); 
      String folderid = request.getParameter(IPSHtmlParameters.SYS_FOLDERID); 
      String siteid = request.getParameter(IPSHtmlParameters.SYS_SITEID); 
      
      if(StringUtils.isBlank(contentid))
      {
         emsg = "content id cannot be blank"; 
         log.error(emsg); 
         throw new RuntimeException(emsg); 
      }
      Map urlParams = SimplifyParameters.simplifyMapStringStringArray(request.getParameterMap());
      List<PreviewLocation> previews = new ArrayList<>();
      List<SiteFolderLocation> locations = siteFolderFinder.findSiteFolderLocations(contentid, folderid, siteid);
      IPSAssemblyTemplate template = findTemplate(templateid); 
      
      
      for(SiteFolderLocation loc : locations)
      {
         PreviewLocation prev = new PreviewLocation(); 
         prev.setSiteName(loc.getSiteName()); 
         prev.setPath(loc.getFolderPath()); 
         String url = urlBuilder.buildUrl(template, urlParams, loc, false);
         prev.setUrl(url); 
         previews.add(prev); 
      }
      
      Collections.sort(previews);
      mav.addObject("previews", previews); 
      return mav; 
   }
   
   
   protected IPSAssemblyTemplate findTemplate(String templateid) throws PSAssemblyException
   {
      initServices(); 
      long tid = Long.parseLong(templateid);
      IPSGuid tguid = gmgr.makeGuid(tid,PSTypeEnum.TEMPLATE); 
      IPSAssemblyTemplate template = asm.loadTemplate(tguid, false); 
      log.debug("loaded template {} for id {}", template.getName(), templateid);
      return template; 
   }

   /**
    * @return the siteFolderFinder
    */
   public SiteFolderFinder getSiteFolderFinder()
   {
      return siteFolderFinder;
   }

   /**
    * @param siteFolderFinder the siteFolderFinder to set
    */
   public void setSiteFolderFinder(SiteFolderFinder siteFolderFinder)
   {
      this.siteFolderFinder = siteFolderFinder;
   }

   /**
    * @return the gmgr
    */
   public static IPSGuidManager getGmgr()
   {
      return gmgr;
   }

   /**
    * @param gmgr the gmgr to set
    */
   public static void setGmgr(IPSGuidManager gmgr)
   {
      MultiSiteResolutionController.gmgr = gmgr;
   }

   /**
    * @return the asm
    */
   public static IPSAssemblyService getAsm()
   {
      return asm;
   }

   /**
    * @param asm the asm to set
    */
   public static void setAsm(IPSAssemblyService asm)
   {
      MultiSiteResolutionController.asm = asm;
   }

   /**
    * @return the urlBuilder
    */
   public UrlBuilder getUrlBuilder()
   {
      return urlBuilder;
   }

   /**
    * @param urlBuilder the urlBuilder to set
    */
   public void setUrlBuilder(UrlBuilder urlBuilder)
   {
      this.urlBuilder = urlBuilder;
   }
   
}
