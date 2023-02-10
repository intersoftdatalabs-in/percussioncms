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
package com.percussion.cms.objectstore;

import com.percussion.cms.PSCmsException;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Catalogs all server security providers by querying the
 * ../sys_components/getSecurityProviders.xml app.
 */
public class PSSecurityProviderCataloger
{
   /**
    * Constructor meant to be used in the context of an applet. This may not work
    * in other contexts since there is no way of supplying credentials for logging
    * in.
    * @param urlBase the document or code base for the applet.
    * @throws PSCmsException if request to server to get the data fails for
    * any reason.
    */
   public PSSecurityProviderCataloger(URL urlBase)
      throws PSCmsException
   {
      try
      {
         URL url = new URL(urlBase, RESOURCE);
         Document doc = PSXmlDocumentBuilder.createXmlDocument(
            url.openStream(), false);
         fromXml(doc.getDocumentElement());
      }
      catch(Exception e)
      {
         throw new PSCmsException(1000, PSExceptionUtils.getMessageForLog(e));
      }
   }

   /**
    * Extracts <code>PSSecurityProviderInstanceSummary</code> Nodes
    * from the xml, creates the instances and adds them to
    * the provider list.
    *
    * @param src the element representing the list of provider summaries.
    * May be <code>null</code>.
    *
    * Expects Xml document based on the following DTD:
    *
    * <code><pre>
    *
    *  &lt;!ELEMENT SecurityProviders (PSXSecurityProviderInstanceSummary*)&gt;
    *  &lt;!ELEMENT PSXSecurityProviderInstanceSummary (name)&gt;
    *   &lt;!ATTLIST
    *    typeName  CDATA       #REQUIRED
    *    typeId     CDATA       #REQUIRED
    *   &gt;
    *
    *  &lt;!ELEMENT name       (#PCDATA)&gt;
    *
    * </pre></code>
    */
   public void fromXml(Element src) throws PSUnknownNodeTypeException
   {
      if(null == src)
         return;

      Element elem = null;
      NodeList nodes =
         src.getElementsByTagName(PSSecurityProviderInstanceSummary.XML_ELEMENT_ROOT);
      for(int i = 0; i < nodes.getLength(); i++)
      {
         elem = (Element)nodes.item(i);
         m_providers.add(new PSSecurityProviderInstanceSummary(elem));
      }

   }


   /**
    * Return iterator of providers.
    * @return iterator. Never <code>null</code>.
    */
   public Iterator getProviders()
   {
      return m_providers.iterator();
   }

   /**
    * Return provider by id
    * @param id id of provider to be found
    * @return provider if found, else <code>null</code>.
    */
   public PSSecurityProviderInstanceSummary getProviderById(int id)
   {
      PSSecurityProviderInstanceSummary provider = null;
      Iterator it = getProviders();
      while(it.hasNext())
      {
         provider = (PSSecurityProviderInstanceSummary)it.next();
         if(provider.getTypeId() == id)
            return provider;
      }
      return null;
   }

   /**
    * Return provider by name.
    * @param name name of provider to be found
    * @return provider if found, else <code>null</code>.
    */
   public PSSecurityProviderInstanceSummary getProviderByName(String name)
   {
      PSSecurityProviderInstanceSummary provider = null;
      Iterator it = getProviders();
      while(it.hasNext())
      {
         provider = (PSSecurityProviderInstanceSummary)it.next();
         if(provider.getTypeName().equals(name))
            return provider;
      }
      return null;
   }


   /**
    * This is a list of lists of strings. The parent list contains provider type
    * names, the sublist contains a lists of provider type instance names.
    */
   private List m_providers = new ArrayList();

   /**
    * The security providers XML root element node.
    */
   static public final String XML_ELEM_ROOT = "SecurityProviders";


   /**
    * The application resource that fetches the security providers.
    */
   private static final String RESOURCE =
      "../sys_components/getSecurityProviders.xml";
}
