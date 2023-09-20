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
package com.percussion.cms;

import com.percussion.data.PSInternalRequestCallException;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.server.IPSInternalRequest;
import com.percussion.server.IPSRequestContext;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.xml.PSXmlDocumentBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This exit is specifically written to help processing inline links with 
 * promotable versions in Word OCX control. Expects 3 html parameters in the 
 * request, viz. "sys_contentid", "sys_revision" and "inlinecontentids". The 
 * first two parameters refer to the parent content item that has the inline 
 * links. The third parameter is a semi-colon separated list of contentids of 
 * the inline items for the parent item. The result document produced by the 
 * exit is described in {@link #processResultDocument(Object[], 
 * IPSRequestContext, Document)}. The Word control requests this document while 
 * processing inline links for an item and replaces the contentids and revisions 
 * appropriately.
 *
 */
public class PSGetModifiedInlineLinkData
   extends PSDefaultExtension
   implements IPSResultDocumentProcessor
{
   /* (non-Javadoc)
    * @see com.percussion.extension.IPSResultDocumentProcessor#canModifyStyleSheet()
    */
   public boolean canModifyStyleSheet()
   {
      // TODO Auto-generated method stub
      return false;
   }

   /**
    * Replaces the rusult document with the docoument like this.
    * <Parent sys_contentid="301" sys_revision="1">
    *    <Child oldContentId="312" newContentId="333" newRevision="1"/>
    *    <Child oldContentId="323" newContentId="323" newRevision="2"/>
    *    <Child oldContentId="345" newContentId="345" newRevision="2"/>
    * </Parent>
    * As the name indicates Parent represents the parent item and Child element 
    * represents the inline child of the parent item. oldContentId is the 
    * current content id of the inline item stored in the content. newContentId 
    * is the active promotable version of the old item. This will be the same 
    * as the old one if the item is not replaced by its promotable version. The 
    * newRevision always represents the current revision of the item with 
    * newContentId.
    * 
    * @see com.percussion.extension.IPSResultDocumentProcessor#
    * processResultDocument(java.lang.Object[], com.percussion.server.
    * IPSRequestContext, org.w3c.dom.Document)
    */
   public Document processResultDocument(
      Object[] params,
      IPSRequestContext request,
      Document resultDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException
   {
      try
      {
         PSRelationshipData inlineLinkRelData =
            PSSingleValueBuilder.buildRelationshipData(request, null);
         String dependentIds = request.getParameter("inlinecontentids", "");
         StringTokenizer tokenizer = new StringTokenizer(dependentIds, ";");
         Map cidMap = new HashMap<>();
         while (tokenizer.hasMoreTokens())
         {
            String cid = tokenizer.nextToken();

            if (cid == null || cid.length() < 1)
               continue;

            //avoid processing duplicates
            if (cidMap.containsKey(cid))
               continue;

            String newCid =
               PSSingleValueBuilder.getCorrectedContentId(
                  request,
                  cid,
                  null,
                  inlineLinkRelData,
                       null);
            if (newCid == null)
               newCid = cid;
            cidMap.put(cid, newCid);
         }
         if(cidMap.size()<1)
         {
            throw new PSExtensionProcessingException(
               m_def.getRef().getExtensionName(),
               new IllegalArgumentException(
                  "inlinecontentids parameter must not be null or empty"));
         }
         Iterator iter = cidMap.keySet().iterator();
         List paramList = new ArrayList<>();
         while (iter.hasNext())
         {
            String element = (String)cidMap.get(iter.next());
            paramList.add(element);
         }
         Map reqParams = new HashMap<>();
         reqParams.put(IPSHtmlParameters.SYS_CONTENTID, paramList);
         IPSInternalRequest iReq =
            request.getInternalRequest(
               "sys_psxCms/getCurrentRevision",
               reqParams,
               false);

         Document doc = iReq.getResultDoc();
         NodeList nl = doc.getElementsByTagName("Item");
         Map cidRevisionMap = new HashMap<>();
         for (int i = 0; nl != null && i < nl.getLength(); i++)
         {
            Element elem = (Element) nl.item(i);
            cidRevisionMap.put(
               elem.getAttribute(IPSHtmlParameters.SYS_CONTENTID),
               elem.getAttribute(IPSHtmlParameters.SYS_REVISION));
         }

         resultDoc.removeChild(resultDoc.getDocumentElement());
         Element root = PSXmlDocumentBuilder.createRoot(resultDoc, "Parent");
         root.setAttribute(
            IPSHtmlParameters.SYS_CONTENTID,
            request.getParameter(IPSHtmlParameters.SYS_CONTENTID, ""));
         root.setAttribute(
            IPSHtmlParameters.SYS_REVISION,
            request.getParameter(IPSHtmlParameters.SYS_REVISION, ""));

         Iterator cidIter = cidMap.keySet().iterator();
         while (cidIter.hasNext())
         {
            String oldCid = (String) cidIter.next();
            String newCid = cidMap.get(oldCid).toString();
            String newRevision = "";
            if(cidRevisionMap.containsKey(newCid))
               newRevision = cidRevisionMap.get(newCid).toString();
            Element child =
               PSXmlDocumentBuilder.addElement(resultDoc, root, "Child", null);
            child.setAttribute("oldContentId", oldCid);
            child.setAttribute("newContentId", newCid);
            child.setAttribute("newRevision", newRevision);
         }
      }
      catch (PSCmsException e)
      {
         throw new PSExtensionProcessingException(
            m_def.getRef().getExtensionName(),
            e);
      }
      catch (PSInternalRequestCallException e)
      {
         throw new PSExtensionProcessingException(
            m_def.getRef().getExtensionName(),
            e);
      }
      return resultDoc;
   }
}
