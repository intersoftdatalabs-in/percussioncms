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
package com.percussion.webservices.transformation.converter;

import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;
import com.percussion.webservices.common.Reference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtilsBean;

/**
 * Converts objects between the classes
 * <code>com.percussion.webservices.assembly.data.PSAssemblyTemplateWs</code> and
 * <code>com.percussion.webservices.assembly.data.PSAssemblyTemplate</code>.
 */
public class PSAssemblyTemplateWsConverter extends PSConverter
{
   /* (non-Javadoc)
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSAssemblyTemplateWsConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
      m_templateConverter = new PSAssemblyTemplateConverter(beanUtils);
   }

   private PSAssemblyTemplateConverter m_templateConverter;

   /* (non-Javadoc)
    * @see PSConverter#convert(Class, Object)
    */
   @Override
   public Object convert(Class type, Object value)
   {
      // client to server
      if (value instanceof
            com.percussion.webservices.assembly.data.PSAssemblyTemplate)
      {
         IPSAssemblyTemplate template =
            (IPSAssemblyTemplate) m_templateConverter.convert(
                  PSAssemblyTemplate.class, value);

         com.percussion.webservices.assembly.data.PSAssemblyTemplate orig =
            (com.percussion.webservices.assembly.data.PSAssemblyTemplate) value;

         // convert sites (generated wrapper)
         Map<IPSGuid,String> sites = new HashMap<>();
         com.percussion.webservices.assembly.data.PSAssemblyTemplate.Sites origSitesWrapper = orig.getSites();
         if (origSitesWrapper != null && origSitesWrapper.getSite() != null)
         {
            for (com.percussion.webservices.common.Reference origSite : origSitesWrapper.getSite())
            {
               PSDesignGuid siteid = new PSDesignGuid(PSTypeEnum.SITE,
                     origSite.getId());
               sites.put(siteid, origSite.getName());
            }
         }

         return new PSAssemblyTemplateWs(template, sites);
      }
      else // server to client
      {
         PSAssemblyTemplateWs orig = (PSAssemblyTemplateWs) value;
         Object result = m_templateConverter.convert(type, orig.getTemplate());

         com.percussion.webservices.assembly.data.PSAssemblyTemplate dest =
            (com.percussion.webservices.assembly.data.PSAssemblyTemplate) result;

         // convert sites
         com.percussion.webservices.assembly.data.PSAssemblyTemplate.Sites sitesWrapper = new com.percussion.webservices.assembly.data.PSAssemblyTemplate.Sites();
         for (Map.Entry<IPSGuid,String> s : orig.getSites().entrySet())
         {
            PSDesignGuid id = new PSDesignGuid(s.getKey());
            com.percussion.webservices.common.Reference site = new com.percussion.webservices.common.Reference();
            site.setId(id.getValue());
            site.setName(s.getValue());
            sitesWrapper.getSite().add(site);
         }
         dest.setSites(sitesWrapper);

         return dest;
      }
   }
}
