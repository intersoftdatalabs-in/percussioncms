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
package com.percussion.pso.demandpreview.service.impl;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.pso.demandpreview.exception.SiteLookUpException;
import com.percussion.pso.demandpreview.service.ItemTemplateService;
import com.percussion.pso.jexl.IPSOObjectFinder;
import com.percussion.pso.jexl.PSOObjectFinder;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSAssemblyTemplate.OutputFormat;
import com.percussion.services.assembly.IPSTemplateService;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the default page template for a given content item (by content type) 
 * on a specified site.  
 * 
 * @author DavidBenua
 *
 */
public class DefaultPageTemplateBean implements ItemTemplateService {

	private static final Logger log = LogManager.getLogger(DefaultPageTemplateBean.class);
	
	private IPSTemplateService tempSvc = null; 
	private IPSOObjectFinder objFinder = null; 
	
	public void init() throws Exception
	{
		if(tempSvc == null)
		{
			tempSvc = PSAssemblyServiceLocator.getAssemblyService(); 
		}
		if(objFinder == null)
		{
			objFinder = new PSOObjectFinder(); 
		}
	}
	/**
	 * Finds the first default page (or binary) template for the content type of the indicated content item that is 
	 * available on the site. 
	 * @param site the site
	 * @param contentId the content item. 
	 * @return the default template.  Never <code>null</code>. 
	 * @throws SiteLookUpException when the template cannot be located or a service error occurs.  
	 */
	public IPSAssemblyTemplate findTemplate(IPSSite site, IPSGuid contentId)
			throws SiteLookUpException {
		String emsg; 
		Validate.notNull(site);
		try {
			PSComponentSummary summ = objFinder.getComponentSummary(contentId);
			Set<IPSGuid> siteTemplates = getSiteTemplates(site); 
			List<IPSAssemblyTemplate> allTemps = tempSvc.findTemplatesByContentType(summ.getContentTypeGUID()); 
			
			for(IPSAssemblyTemplate template : allTemps)
			{
				log.trace("examining template " + template.getName()); 
				if(template.getPublishWhen() == IPSAssemblyTemplate.PublishWhen.Default)
				{					
					OutputFormat oformat = template.getOutputFormat(); 
					if(oformat == OutputFormat.Page || oformat == OutputFormat.Binary)
					{
						if(siteTemplates.contains(template.getGUID()))
						{  //we found it!
							log.trace("found template"); 
							return template; 
						}
						else
						{
							log.trace("template not in site");
						}
					}
					else
					{
						log.trace("template not a page or binary");
					}
				}
				else
				{
					log.trace("template not default"); 
				}
			}
			
			emsg = "no default page template found for type " + summ.getContentTypeId() + " and site " + site.getName();
			log.error(emsg); 
			throw new SiteLookUpException(emsg); 
			
			
		} catch (Exception e) {
			emsg = "Unexpected Exception " + e.getMessage();  
			log.error(emsg , e); 
			throw new SiteLookUpException(emsg,e);
		}
	}

		
	protected Set<IPSGuid> getSiteTemplates(IPSSite site)
	{
		Set<IPSGuid> results = new HashSet<IPSGuid>();
        for(IPSAssemblyTemplate tp : site.getAssociatedTemplates())		
        {
        	results.add(tp.getGUID()); 
        }
		
		return results; 
	}
	
	
	/**
	 * @param tempSvc the tempSvc to set
	 */
	protected void setTempSvc(IPSTemplateService tempSvc) {
		this.tempSvc = tempSvc;
	}
	protected void setObjFinder(IPSOObjectFinder objFinder) {
		this.objFinder = objFinder;
	}
}
