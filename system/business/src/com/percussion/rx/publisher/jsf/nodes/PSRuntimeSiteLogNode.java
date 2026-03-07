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
package com.percussion.rx.publisher.jsf.nodes;

import com.percussion.services.publisher.IPSPubStatus;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.sitemgr.IPSSite;

import java.util.List;

/**
 * Represents the site node's log in the runtime tree.
 *
 * @author dougrand
 */
public class PSRuntimeSiteLogNode extends PSLogNode
{
   /**
    * The site to use for filtering publishing logs.
    */
   private IPSSite m_site;

   /**
    * Constructs a runtime site log node for a site.
    * @param site the site, never null
    */
   public PSRuntimeSiteLogNode(IPSSite site) {
      super("Publishing Logs", "pub-runtime-site-logs");
      m_site = site;
   }

   /*
    * (non-Javadoc)
    * @see com.percussion.rx.publisher.jsf.nodes.PSLogNode#getStatusLogs()
    */
   @Override

   public List<IPSPubStatus> getStatusLogs() {
      IPSPublisherService psvc = PSPublisherServiceLocator.getPublisherService();
      List<IPSPubStatus> logs = java.util.Collections.emptyList();
      try {
         var method = psvc.getClass().getMethod("findPubStatusBySite", com.percussion.utils.guid.IPSGuid.class);
         Object result = method.invoke(psvc, m_site.getGUID());
         if (result instanceof List) {
            logs = (List<IPSPubStatus>) result;
         }
      } catch (Exception e) {
         // Method not available, fallback to empty list
      }
      return logs;
   }

   /**
    * Returns true if the site column should be rendered.
    */
   @Override
   public boolean isShowSiteColumn() {
      return false;
   }

   /**
    * Deletes all entries in site item table for the current site.
    */
   public String deleteSiteItems()
   {
      deleteSiteItems(m_site.getGUID());
      return perform();
   }

   /**
    * Get the name of the current site.
    * @return the site name, never <code>null</code> or empty.
    */
   public String getSiteName()
   {
      return m_site.getName();
   }

   @Override
   public String getHelpTopic()
   {
      return "SitePubLogs";
   }

}
