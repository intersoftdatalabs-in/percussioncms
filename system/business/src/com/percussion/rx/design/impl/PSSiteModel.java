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
package com.percussion.rx.design.impl;

import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSEditionContentList;
import com.percussion.services.publisher.IPSEditionTaskDef;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PSSiteModel extends PSDesignModel
{
   @Override
   public Object load(IPSGuid guid)
   {
      return loadSite(guid, true);
   }
   
   @Override
   public Object loadModifiable(IPSGuid guid)
   {
      return loadSite(guid, false);
   }
   
   /**
    * Loads the readonly or modifiable site from the site manager for
    * the supplied guid based on the readonly flag.
    * 
    * @param guid Must not be <code>null</code> and must be a site guid.
    * @param readonly Flag to indicate whether to load a readonly or modifiable
    * site.
    * @return Object site object never <code>null</code>, throws
    * {@link RuntimeException} in case of an error.
    */
   private Object loadSite(IPSGuid guid, boolean readonly)
   {
      if (guid == null || !isValidGuid(guid))
         throw new IllegalArgumentException("guid is not valid for this model");
      IPSSite obj = null;
      try
      {
         if (readonly)
         {
            obj = getSiteMgr().loadSite(guid);
         }
         else
         {
            obj = getSiteMgr().loadSiteModifiable(guid);
         }
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      if (obj == null)
      {
         String msg = "Failed to get the design object for guid {0}";
         Object[] args = { guid.toString() };
         throw new RuntimeException(MessageFormat.format(msg, args));
      }
      return obj;
   }
   
   /*
    * //see base interface method for details
    */
   @Override
   public List<IPSGuid> findAllIds()
   {
      Map<PSPair<IPSGuid, String>, Collection<IPSGuid>> assoc = getSiteMgr()
            .findSiteTemplatesAssociations();
      List<IPSGuid> ids = new ArrayList<>();
      for (PSPair<IPSGuid, String> k : assoc.keySet())
      {
         ids.add(k.getFirst());
      }
      return ids;
   }
   
   /*
    * //see base interface method for details
    */
   @Override
   public Collection<String> findAllNames()
   {
      Map<PSPair<IPSGuid, String>, Collection<IPSGuid>> assoc = getSiteMgr()
            .findSiteTemplatesAssociations();
      List<String> names = new ArrayList<>();
      for (PSPair<IPSGuid, String> k : assoc.keySet())
      {
         names.add(k.getSecond());
      }
      return names;
   }
   
   /**
    * Gets the Site Manager service.
    * 
    * @return the Site Manager service, never <code>null</code>.
    */
   private IPSSiteManager getSiteMgr()
   {
      if (m_siteMgr == null)
         m_siteMgr = (IPSSiteManager) getService();
      
      return m_siteMgr;
   }

   @Override
   public void delete(IPSGuid guid) throws PSNotFoundException {
      IPSSiteManager smgr = PSSiteManagerLocator.getSiteManager();
      IPSPublisherService psvc = PSPublisherServiceLocator
            .getPublisherService();
      IPSSite site = smgr.loadSite(guid);
      List<IPSEdition> editions = psvc.findAllEditionsBySite(site.getGUID());
      for (IPSEdition edition : editions)
      {
         List<IPSEditionTaskDef> tasks = psvc.loadEditionTasks(edition
               .getGUID());
         for (IPSEditionTaskDef task : tasks)
         {
            psvc.deleteEditionTask(task);
         }
         List<IPSEditionContentList> eclists = psvc
               .loadEditionContentLists(edition.getGUID());
         for (IPSEditionContentList list : eclists)
         {
            psvc.deleteEditionContentList(list);
         }
         psvc.deleteEdition(edition);
      }
      smgr.deleteSite(site);
   }
   
   
   /**
    * The cached Site Manager Service. Default to <code>null</code> if has not
    * been initialized.
    */
   IPSSiteManager m_siteMgr = null;
}
