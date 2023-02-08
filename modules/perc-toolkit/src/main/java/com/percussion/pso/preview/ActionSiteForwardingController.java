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
/*
 * com.percussion.pso.preview ActionSiteForwardingController.java
 *  
 * @author DavidBenua
 *
 */
package com.percussion.pso.preview;

import com.percussion.pso.utils.PSOMutableUrl;
import com.percussion.pso.utils.SimplifyParameters;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.util.IPSHtmlParameters;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Controller for miscellaneous actions such as Compare.  These actions to not require
 * a location, just a site id.  
 *
 * @author DavidBenua
 *
 */
public class ActionSiteForwardingController extends AbstractMenuController
      implements
         Controller
{

   private static final Logger log = LogManager.getLogger(ActionSiteForwardingController.class);
   
   private String baseUrl = null; 
   
   private String contentIdParam = IPSHtmlParameters.SYS_CONTENTID; 
   
   private String revisionParam = IPSHtmlParameters.SYS_REVISION; 
   
   private String folderIdParam = IPSHtmlParameters.SYS_FOLDERID; 
   
   private String siteIdParam = IPSHtmlParameters.SYS_SITEID; 
   /**
    * @see AbstractMenuController#handleRequestInternal(HttpServletRequest, HttpServletResponse)
    */
   @Override
   @SuppressWarnings("unchecked")
   protected ModelAndView handleRequestInternal(HttpServletRequest request,
         HttpServletResponse response) throws Exception
   {
      ModelAndView mav = super.handleRequestInternal(request, response);
      
      Locale locale = request.getLocale(); 
      
      Map<String, String[]> pmap = request.getParameterMap();
      Map<String, String> simpleMap = SimplifyParameters.simplifyMapStringStringArray(pmap);
      log.debug("Simple map is {}", simpleMap);
     
      if(simpleMap.containsKey(IPSHtmlParameters.SYS_SITEID))
      { //there is already a site id, we can just use it.  
         return redirectTo(baseUrl, simpleMap); 
      }
      
      Set<IPSSite> sites = findSitesForItem(simpleMap); 
      if(sites.size() == 0)
      {
         log.error("item not found on any site"); 
         return redirectTo(baseUrl, simpleMap); 
      }
      if(sites.size() == 1)
      {
         IPSSite site = (IPSSite) sites.toArray()[0];
         log.debug("found single site {}", site.getName());
         simpleMap.put(IPSHtmlParameters.SYS_SITEID, 
               String.valueOf(site.getSiteId()));
         return redirectTo(baseUrl, simpleMap); 
      }
      PSOMutableUrl url = new PSOMutableUrl(baseUrl); 
      url.setParamList(new HashMap<String, Object>(simpleMap)); //need to do this for type conversion
      List<PreviewLocation> locations = new ArrayList<PreviewLocation>();
      for(IPSSite site : sites)
      {
         PreviewLocation location = new PreviewLocation();
         location.setSiteName(site.getName());
         log.debug("adding site {}", site.getName());
         url.setParam(IPSHtmlParameters.SYS_SITEID,String.valueOf(site.getSiteId()));
         location.setUrl(url.toString()); 
         locations.add(location); 
      }
      mav.addObject("previews", locations);
      log.debug("locations: {}", locations);
      return mav; 
   }
   
   /**
    * Finds the sites for an item.  The sites are based on the site folder locations for the item. 
    * @param pmap the HTML parameters that define the item. 
    * @return the sites that this item appears on. 
    * @throws Exception
    */
   protected Set<IPSSite> findSitesForItem(Map<String,String> pmap) throws Exception
   {
      String contentid = StringUtils.defaultString(pmap.get(contentIdParam)); 
      String revision = StringUtils.defaultString(pmap.get(revisionParam)); 
      String folderid = StringUtils.defaultString(pmap.get(folderIdParam));
      String siteid = StringUtils.defaultString(pmap.get(siteIdParam));

      List<SiteFolderLocation> locations = this.siteFolderFinder.findSiteFolderLocations(contentid, folderid, siteid);
      SiteFolderLocation loc; 
      log.debug("there are {} locations",locations.size());
      Set<IPSSite> sites = findSitesFromLocations(locations);
      log.debug("there are {} sites",sites.size());
      return sites; 
   }
  
   /**
    * Redirect the user's browser to a specific url. 
    * @param url the base url. 
    * @param pmap the HTML parameters to pass with the url. 
    * @return the Redirect view for the url and parameters. 
    */
   protected ModelAndView redirectTo(String url, Map<String,String> pmap)
   {
      View redirect = new RedirectView(url, false);
      return new ModelAndView(redirect, pmap); 
   }
   /**
    * Gets the base url. 
    * @return the baseUrl
    */
   public String getBaseUrl()
   {
      return baseUrl;
   }
   /**
    * Sets the base url.
    * @param baseUrl the baseUrl to set
    */
   public void setBaseUrl(String baseUrl)
   {
      this.baseUrl = baseUrl;
   }

   /**
    * Gets the content id parameter name. 
    * @return the contentIdParam
    */
   public String getContentIdParam()
   {
      return contentIdParam;
   }

   /**
    * Sets the content id parameter name. 
    * @param contentIdParam the contentIdParam to set
    */
   public void setContentIdParam(String contentIdParam)
   {
      this.contentIdParam = contentIdParam;
   }

   /**
    * Gets the revision parameter name.
    * @return the revisionParam
    */
   public String getRevisionParam()
   {
      return revisionParam;
   }

   /**
    * Sets the revision parameter name.
    * @param revisionParam the revisionParam to set
    */
   public void setRevisionParam(String revisionParam)
   {
      this.revisionParam = revisionParam;
   }

   /**
    * Gets the folder id parameter name.
    * @return the folderIdParam
    */
   public String getFolderIdParam()
   {
      return folderIdParam;
   }

   /**
    * Sets the folder id parameter.
    * @param folderIdParam the folderIdParam to set
    */
   public void setFolderIdParam(String folderIdParam)
   {
      this.folderIdParam = folderIdParam;
   }

   /**
    * Gets the site id parameter name. 
    * @return the siteIdParam
    */
   public String getSiteIdParam()
   {
      return siteIdParam;
   }

   /**
    * Sets the site id parameter name. 
    * @param siteIdParam the siteIdParam to set
    */
   public void setSiteIdParam(String siteIdParam)
   {
      this.siteIdParam = siteIdParam;
   }
}
