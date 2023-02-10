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
package com.percussion.cms.objectstore.client;

import com.percussion.cms.IPSCmsErrors;
import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.IPSCataloger;
import com.percussion.cms.objectstore.PSRelationshipInfoSet;
import com.percussion.design.objectstore.PSSearchConfig;
import com.percussion.error.PSException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.util.IPSRemoteRequester;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Cataloger used on the client side by the clients which cannot use designer
 * connection e.g cataloging client calls from the applet.
 */
public class PSRemoteCataloger implements IPSCataloger
{
	private static final Logger log = LogManager.getLogger(PSRemoteCataloger.class);
   /**
    * Ctor.
    * @param requester never <code>null</code>.
    */
   public PSRemoteCataloger(IPSRemoteRequester requester)
   {
      if (requester==null)
         throw new IllegalArgumentException("requester may not be null");

      m_requester = requester;
   }

   // see interface
   public Element getCEFieldXml(int controlFlags) throws PSCmsException
   {
      return getCEFieldXml(controlFlags, null);
   }
   
   //see the interface.
   @SuppressWarnings("unused")
   public Element getCEFieldXml(int controlFlags, Set<String> fields)
      throws PSCmsException
   {
      final Map<String, Object> params = new HashMap<>();
      if ((controlFlags & FLAG_INCLUDE_HIDDEN) > 0)
         params.put(IPSHtmlParameters.SYS_INCLUDEHIDDENFIELDS, "");
      if ((controlFlags & FLAG_INCLUDE_RESULTONLY) > 0)
         params.put(IPSHtmlParameters.SYS_INCLUDERESULTONLYFIELDS, "");
      if ((controlFlags & FLAG_RESTRICT_TOUSERCOMMUNITY) > 0)
         params.put(IPSHtmlParameters.SYS_RESTRICTFIELDSTOUSERCOMMUNITY, "");
      if ((controlFlags & FLAG_USER_SEARCH) > 0)
         params.put(IPSHtmlParameters.SYS_USERSEARCH, "");
      if ((controlFlags & FLAG_CTYPE_EXCLUDE_HIDDENFROMMENU) > 0)
         params.put(IPSHtmlParameters.SYS_CTYPESHIDEFROMMENU, "");
      if ((controlFlags & FLAG_EXCLUDE_CHOICES) > 0)
         params.put(IPSHtmlParameters.SYS_EXCLUDE_CHOICES, "");
      
      if (fields != null && !fields.isEmpty())
      {
         params.put(IPSHtmlParameters.SYS_CE_FIELD_NAME, 
            new ArrayList<String>(fields));
      }
    
      try{
    	  Document doc = getCatalogDocument(GET_CE_FIELDS, params);
    	  if(doc != null)
    		  return doc.getDocumentElement();   	   
      }catch(Throwable t){
         log.error("An unexpected exception occurred while cataloging fields", t);
         log.error(t.getMessage());
         log.debug(t.getMessage(), t);
    	  throw new PSCmsException(IPSCmsErrors.CONTENT_TYPE_CANNOT_BE_OPENED);
      }
    return null;
   }

   // see interface
   public PSSearchConfig getSearchConfig() throws PSCmsException
   {
      Document doc = getCatalogDocument(GET_SEARCH_CONFIG, null);
      Element resultEl = doc.getDocumentElement();
      
      try
      {
         return new PSSearchConfig(resultEl, null, null);
      }
      catch (PSException e)
      {
         throw new PSCmsException(e);
      }
   }

   /**
    * See {@link IPSCataloger#getRelationshipInfoSet() interface}
    */
   public PSRelationshipInfoSet getRelationshipInfoSet()
      throws PSCmsException
   {
      Document doc = getCatalogDocument(GET_RELATE_INFO_SET, null);
      Element resultEl = doc.getDocumentElement();
      String expectedNodeName = PSRelationshipInfoSet.XML_NODE_NAME;

      if (! resultEl.getNodeName().equalsIgnoreCase(expectedNodeName))
      {
         String unknownDoc = PSXmlDocumentBuilder.toString(resultEl);
         String[] args = {GET_RELATE_INFO_SET, unknownDoc};
         throw new PSCmsException(IPSCmsErrors.UNEXPECTED_CATALOG_ERROR, args);
      }

      try
      {
         return new PSRelationshipInfoSet(resultEl);
      }
      catch (PSException e)
      {
         throw new PSCmsException(e);
      }
   }

   /**
    * Returns a reference to the IPSRemoteRequester.
    * @return reference to IPSRemoteRequester, never <code>null</code>.
    */
   public IPSRemoteRequester getRemoteRequester()
   {
      return m_requester;
   }

   /**
    * Get the catalogged document from the specified path/
    *
    * @param path The path that is used to get the catalogged document, assume
    *    not <code>null</code>.
    *
    * @param params Any html parameters that need to be included with the
    *    request. May be <code>null</code> or empty.
    *
    * @return The document that is received from the server, never
    *    <code>null</code>.
    *
    * @throws PSCmsException if an error occurs.
    */
   private Document getCatalogDocument(String path, Map params)
      throws PSCmsException
   {
      Document doc = null;
      try
      {
         if (null == params)
            params = new HashMap<>();
         doc = m_requester.getDocument(path, params);
      }
      catch (Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         String[] args = {path, e.toString()};
         throw new PSCmsException(IPSCmsErrors.UNEXPECTED_CATALOG_ERROR, args);
      }

      return doc;
   }

   /**
    * The object which knows to communicate with the remote server. Initialized
    * by the ctor, never <code>null</code> after that.
    */
   private IPSRemoteRequester m_requester = null;

   private static final String CATALOGER_HANDLER = "sys_ceFieldsCataloger/";

   private static final String GET_CE_FIELDS =
      CATALOGER_HANDLER + "ContentEditorFields";

   private static final String GET_RELATE_INFO_SET =
      CATALOGER_HANDLER + "RelationshipInfoSet";
   
   private static final String GET_SEARCH_CONFIG =
      CATALOGER_HANDLER + "SearchConfig";
   
}
