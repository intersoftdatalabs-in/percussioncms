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
package com.percussion.cas;

import com.percussion.data.PSInternalRequestCallException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSRequestPreProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.IPSInternalRequest;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequestValidationException;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.util.PSXMLDomUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This exit will turn the sys_template values into the appropriate
 * sys_variantid by doing a lookup. This will only occur if sys_variantid 
 * does not exist but sys_template does. 
 * 
 * This exit requires the existance of App resource 
 * sys_ceSupport/variantlist.xml.
 */
public class PSSysTemplateToSysVariantid implements IPSRequestPreProcessor
{
  
   /* 
    * @see com.percussion.extension.IPSRequestPreProcessor#preProcessRequest(
    * java.lang.Object[], com.percussion.server.IPSRequestContext)
    */
   @SuppressWarnings("unused")
   public void preProcessRequest(Object[] params, IPSRequestContext request) 
      throws PSAuthorizationException, PSRequestValidationException,
      PSParameterMismatchException, PSExtensionProcessingException
   {
      String variantid = 
         request.getParameter(IPSHtmlParameters.SYS_VARIANTID);
      String template = request.getParameter(IPSHtmlParameters.SYS_TEMPLATE);
      if(StringUtils.isBlank(variantid) && StringUtils.isNotBlank(template))
      {
         Map<String, String> variantMap = getVariantMap(request);
         variantid = variantMap.get(template);
         if(StringUtils.isNotBlank(variantid))
            request.appendParameter(IPSHtmlParameters.SYS_VARIANTID, variantid);
      }
   }
   
   /**
    * Queries the variant list resource and creates a map
    * of template name/ variantids.
    * @param request the request associated with this exit call.
    * @return never <code>null</code>, may be empty.
    */
   private Map<String, String> getVariantMap(IPSRequestContext request)
      throws PSExtensionProcessingException
   {
      Map<String, String> results = new HashMap<>();
      IPSInternalRequest ir = 
         request.getInternalRequest(VARIANT_LIST_RESOURCE);
      try
      {
         Document doc = ir.getResultDoc();
         Element root = doc.getDocumentElement();
         NodeList nl = root.getElementsByTagName(XML_ELEM_VARIANTLIST);
         if(nl.getLength() == 1)
         {
            Element vlist = (Element)nl.item(0);
            NodeList variants = vlist.getElementsByTagName(XML_ELEM_VARIANT);
            int len = variants.getLength();
            for(int i = 0; i < len ; i++)
            {
               Element var = (Element)variants.item(i);
               String templatename = null;
               String variantid = var.getAttribute(XML_ATTR_VARIANTID);
               Node displayname = PSXMLDomUtil.findFirstNamedChildNode(
                  var, XML_ELEM_DISPLAYNAME);
               if(displayname != null)
                  templatename = PSXMLDomUtil.getElementData(displayname);
               if(StringUtils.isNotBlank(templatename) && 
                  StringUtils.isNotBlank(variantid))
                  results.put(templatename, variantid);
            }
         }
      }
      catch (PSInternalRequestCallException e)
      {
         throw new PSExtensionProcessingException(getClass().getName(), e);         
      }
      return results;
   }
   
   /* 
    * @see com.percussion.extension.IPSExtension#init(
    * com.percussion.extension.IPSExtensionDef, java.io.File)
    */
   @SuppressWarnings("unused")
   public void init( IPSExtensionDef def, File codeRoot) 
      throws PSExtensionException
   {
      // no-op      
   }
   
   /**
    * Variant list lookup resource
    */
   private static final String VARIANT_LIST_RESOURCE = 
      "sys_ceSupport/variantlist.xml";
   
   // Various XML constants
   private static final String XML_ATTR_VARIANTID = "variantId";
   private static final String XML_ELEM_VARIANTLIST = "VariantList";
   private static final String XML_ELEM_VARIANT = "Variant";
   private static final String XML_ELEM_DISPLAYNAME= "DisplayName";

}
