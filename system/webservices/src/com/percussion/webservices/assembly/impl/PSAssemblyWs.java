/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.webservices.assembly.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSExceptionUtils;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.util.PSBaseBean;
import com.percussion.webservices.assembly.IPSAssemblyWs;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * The public assembly webservice implementations.
 */
@PSBaseBean("sys_assemblyWs")
public class PSAssemblyWs extends PSAssemblyBaseWs implements IPSAssemblyWs
{
   // @see IPSAssemblyWs#loadAssemblyTemplates(String, String)
   public List<PSAssemblyTemplateWs> loadAssemblyTemplates(String name,
      String contentType)
   {
      IPSAssemblyService service = 
         PSAssemblyServiceLocator.getAssemblyService();

      if (StringUtils.isBlank(name))
         name = "*";
      name = StringUtils.replaceChars(name, '*', '%');

      if (StringUtils.isBlank(contentType))
         contentType = "*";
      contentType = StringUtils.replaceChars(contentType, '*', '%');

      List<IPSAssemblyTemplate> rval;
      try
      {
         rval = service.findTemplates(name, contentType, null, null, null,
               null, null);
      }
      catch (PSAssemblyException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         throw new RuntimeException(e);
      }
      
      return getTemplateWs(rval);
   }

   // @see IPSAssemblyWs#loadSlots(String)
   public List<IPSTemplateSlot> loadSlots(String name)
   {
      IPSAssemblyService service = PSAssemblyServiceLocator
            .getAssemblyService();

      if (StringUtils.isBlank(name))
         name = "*";
      name = StringUtils.replaceChars(name, '*', '%');

      List<IPSTemplateSlot> rval = service.findSlotsByName(name);
      return rval;
   }

   private static final Logger log = LogManager.getLogger(IPSConstants.WEBSERVICES_LOG);

}
