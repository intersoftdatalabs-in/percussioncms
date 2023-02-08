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

package com.percussion.extensions.cx;

import com.percussion.cms.handlers.PSConditionalCloneHandler;
import com.percussion.cms.handlers.PSRelationshipCommandHandler;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.server.IPSInternalRequest;
import com.percussion.server.IPSRequestContext;
import com.percussion.util.IPSHtmlParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;

/**
 * This extension translates given array of items into supplied locale. It
 * needs corresponding array of revisions. Gets the internal request for the 
 * content type url with required parameters for the translation and then 
 * makes an internal request to execute the url. Builds the result document 
 * with the status of translations for each content item.
 */
public class PSCreateTranslations extends PSDefaultExtension
      implements IPSResultDocumentProcessor
{

   private static final Logger log = LogManager.getLogger(PSCreateTranslations.class);

   /**
    * Required by the interface. This exit never modifies the stylesheet.
    * @see IPSResultDocumentProcessor#canModifyStyleSheet()
    */
   public boolean canModifyStyleSheet()
   {
      return false;
   }

   /*
    * implementation of the method in the interface IPSRequestPreProcessor
    */
   public Document processResultDocument(Object[] params,
         IPSRequestContext request,
         Document doc)
         throws PSParameterMismatchException, PSExtensionProcessingException
   {
      Object[] contentIdArray = request.getParameterList(
         IPSHtmlParameters.SYS_CONTENTID);
      if(contentIdArray == null || contentIdArray.length < 1)
      {
         throw new IllegalArgumentException(
            "At least one content id is required for translation");
      }

      Object[] revisionArray = request.getParameterList(
         IPSHtmlParameters.SYS_REVISION);
      if(revisionArray == null || revisionArray.length < 1)
      {
         throw new IllegalArgumentException(
            "At least one revision is required for translation");
      }

      if(contentIdArray.length != revisionArray.length)
      {
         throw new IllegalArgumentException(
            "Number of content ids and revisions does not match");
      }

      Object[] ceUrlArray = request.getParameterList("ceurl");
      if(ceUrlArray == null || ceUrlArray.length < 1)
      {
         throw new IllegalArgumentException(
            "At least one ceUrlArray is required for translation");
      }

      if(contentIdArray.length != revisionArray.length ||
         contentIdArray.length != ceUrlArray.length )
      {
         throw new IllegalArgumentException(
            "Number of content ids and revisions and content editor urls does not match");
      }

      Object[] objArray = request.getParameterList(
         IPSHtmlParameters.SYS_LANG);
      if(objArray == null || objArray.length < 1)
      {
         throw new IllegalArgumentException(
            "A language is required for translation");
      }
      String lang = objArray[0].toString();

      String relationshipType = PSRelationshipConfig.TRANSLATION;
      objArray =
         request.getParameterList(IPSHtmlParameters.SYS_RELATIONSHIPTYPE);
      if (objArray != null && objArray.length > 0)
      {
         relationshipType = objArray[0].toString();
         PSRelationshipConfig relCfg =
            PSRelationshipCommandHandler.getRelationshipConfig(
               relationshipType);
         if (relCfg == null)
         {
            //TODO I18n 
            throw new IllegalArgumentException(
               "Relationship type '"
                  + relationshipType
                  + "' does not exist in the system");
         }
         String category = relCfg.getCategory(); 
         if(!category.equals(PSRelationshipConfig.CATEGORY_TRANSLATION))
         {
            //TODO I18n 
            throw new IllegalArgumentException(
                    "Relationship type '"
                  + relationshipType
                  + "' belongs to the category " + category 
                  + " while expected category is " 
                  + PSRelationshipConfig.CATEGORY_TRANSLATION);
         }
      }

      String contentid;
      String revision;
      String ceurl;
      HashMap paramMap = new HashMap();
      paramMap.put(IPSHtmlParameters.SYS_LANG, lang);
      paramMap.put(
         IPSHtmlParameters.SYS_COMMAND,
         PSRelationshipCommandHandler.COMMAND_NAME);
      paramMap.put(IPSHtmlParameters.SYS_RELATIONSHIPTYPE, relationshipType);

      Element statusElem = null;
      IPSInternalRequest ir = null;
      for(int i=0; i<contentIdArray.length; i++)
      {
         try
         {
            statusElem = doc.createElement("Status");
            contentid = contentIdArray[i].toString();
            revision = revisionArray[i].toString();
            paramMap.put(IPSHtmlParameters.SYS_CONTENTID, contentid);
            paramMap.put(IPSHtmlParameters.SYS_REVISION, revision);
            statusElem.setAttribute("contentid",contentid);

            ceurl = ceUrlArray[i].toString();
            int loc = ceurl.indexOf(".htm");
            if(loc > 0)
               ceurl = ceurl.substring(0, loc);
            loc = ceurl.lastIndexOf("/");
            loc = ceurl.lastIndexOf("/",loc-1);
            ceurl = ceurl.substring(loc+1);

            PSConditionalCloneHandler.initFixupRelationships(request);
            
            request.setParameters(paramMap);
            ir = request.getInternalRequest(ceurl);
            if (ir == null)
            {
               statusElem.setAttribute("status",
                  "Internal request for contenteditor is null");
               continue;
            }
            ir.makeRequest();
            String translatedid = ir.getRequestContext().getParameter(
               IPSHtmlParameters.SYS_CONTENTID);
            statusElem.setAttribute("translatedid", translatedid);
            statusElem.setAttribute("status", "Success");
            
            PSConditionalCloneHandler.fixupRelationships(request);
         }
         catch(Exception e)
         {
               log.error(PSExceptionUtils.getMessageForLog(e));
               log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            statusElem.setAttribute("status", PSExceptionUtils.getMessageForLog(e));
            continue;
         }
         finally
         {
            if(ir != null)
               ir.cleanUp();
            doc.getDocumentElement().appendChild(statusElem);
         }
      }
      return doc;
   }
}
