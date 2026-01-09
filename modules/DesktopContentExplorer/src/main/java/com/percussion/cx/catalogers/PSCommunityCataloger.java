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

package com.percussion.cx.catalogers;

import com.percussion.cms.PSCmsException;
import com.percussion.cx.error.IPSContentExplorerErrors;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.util.PSXMLDomUtil;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Catalogs all server communities by querying the ../sys_cmpCommunities/communities.xml app. */
public class PSCommunityCataloger {
  /**
   * Default constructor. Does nothing. Must be followed by call to fromXml() method. This is useful
   * only to build an object in the fly means the state information might not come from the Rhythmyx
   * server.
   */
  public PSCommunityCataloger() {}

   }

   /**
    * Constructor meant to be used in the context of an applet. This may not work
    * in other contexts since there is no way of supplying credentials for logging
    * in.
    * @param urlBase the document or code base for the applet.
    * @throws PSCmsException if request to server to get the data fails for
    * any reason.
    */
   public PSCommunityCataloger(URL urlBase)
      throws PSCmsException
   {
      m_collCommunities.clear();
      try
      {
         URL url = new URL(urlBase, "sys_cmpCommunities/communities.xml");
         Document doc = PSXmlDocumentBuilder.createXmlDocument(
            url.openStream(), false);
         fromXml(doc.getDocumentElement());
      }
      catch(Exception e)
      {
         throw new PSCmsException(
            IPSContentExplorerErrors.CATALOG_ERROR,
            e.getMessage());
      }
   }

   /*
    * Implementation of the interface method.
    */
   public Object clone()
   {
      PSCommunityCataloger clone = null;
      try
      {
         clone = (PSCommunityCataloger)super.clone();

         Collection clonedComm = new ArrayList();

         Iterator it = m_collCommunities.iterator();
         while(it.hasNext())
            clonedComm.add(((Community)it.next()).clone());

         clone.m_collCommunities = clonedComm;

      }
      catch(CloneNotSupportedException e)
      {
         //????
      }
      return clone;
   }

    public boolean equals(Object object) {
        if (this == object) return true;

        if (!(object instanceof PSCommunityCataloger)) return false;

        PSCommunityCataloger that = (PSCommunityCataloger) object;

        return new org.apache.commons.lang3.builder.EqualsBuilder()
                .appendSuper(super.equals(object))
                .append(m_collCommunities, that.m_collCommunities)
                .isEquals();
    }
  }

    public int hashCode() {
        return new org.apache.commons.lang3.builder.HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(m_collCommunities)
                .toHashCode();
