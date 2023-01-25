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

package com.percussion.workflow;
import com.percussion.error.PSException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSExtensionRef;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.i18n.PSI18nUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.xml.PSXmlDocumentBuilder;

import java.io.File;
import java.util.Iterator;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Adds a list of all installed workflow action extensions to the result XML
 * document.
 */
public class PSGetWorkflowActionList implements IPSResultDocumentProcessor
{
   /*
    * Flag to indicate indicating that this exit has not been initialized yet.
    */
   static private boolean  m_extensionInitialized = false;

   /**
    * The fully qualified name of this extension.
    */
   static private String m_fullExtensionName = "";

   /* *************  IPSExtension Interface Implementation ************* */

   public void init(IPSExtensionDef extensionDef, File codeRoot)
      throws PSExtensionException
   {
      if (!m_extensionInitialized)
      {
         m_extensionInitialized = true;
         m_fullExtensionName = extensionDef.getRef().toString();
      }
   }

   /* *******  IPSResultDocumentProcessor Interface Implementation ******* */

   /**
    * Return false, this extension can not modify the style sheet.
    */
   public boolean canModifyStyleSheet()
   {
      return false;
   }

   /**
    * Adds a list of all installed workflow action extensions to the result XML
    * document.
    *
    * @param  params          the parameters for this extension. Should be
    *                         <CODE>null</CODE> or of size 0, because this
    *                         extension does not have any parameters.
    * @param requestContext   the context of the request associated with this
    *                         extension
    * @param  resultDoc       the result XML document with an element added
    *                         specifying the list of workflow actions, with the
    *                         following format:
    *                         <PRE>
    *                         &lt;workflowactionlist&gt;
    *                           &lt;workflowaction
    *                            handlername="Java"
    *                            context="sys/"&gt;
    *                            extensionname="wfaction1"&gt;
    *                            Java/sys/wfaction1
    *                           &lt;/workflowaction&gt;
    *                           &lt;workflowaction
    *                            handlername="Java"
    *                            context="transitionactions/"&gt;
    *                            extensionname="wfaction2"&gt;
    *                            Java/transitionactions/wfaction2
    *                           &lt;/workflowaction&gt;
    *                           &lt;workflowaction
    *                            handlername="Java"
    *                            context="user/"&gt;
    *                            extensionname="wfaction3"&gt;
    *                            Java/user/wfaction3
    *                           &lt;/workflowaction&gt;
    *                         &lt;/workflowactionlist&gt;
    *                         </PRE>
    *
    * @return                 <code>resultDoc</code> is returned with an
    *                         element listing the installed  workflow action
    *                         extensions to the document root.
    *
    * @throws                 PSParameterMismatchException
    *                         if any parameters are supplied.
    * @throws                 PSExtensionProcessingException if
    *                         <ul>
    *               <li>the request context is <CODE>null</CODE>
    *               </li>
    *                         <li>the result document is <CODE>null</CODE>
    *               </li>
    *                         <li>an exception is thrown by the extension
    *                         manager
    *               </ul>
    */
   public Document processResultDocument(Object[] params,
                                         IPSRequestContext requestContext,
                                         Document resultDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException
   {
      String lang = (String)requestContext.getSessionPrivateObject(
       PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG);
      if (lang == null)
         lang =   PSI18nUtils.DEFAULT_LANG;
      PSWorkFlowUtils.printWorkflowMessage(requestContext,
         "Entering PSGetWorkflowActionList processResultDocument");
      int size = (params == null) ? 0 : params.length;
      if (size != 0)
      { // no parameters are required
         throw new PSParameterMismatchException(lang, size, 0);
      }

      if (null == requestContext)
      {
         throw new PSExtensionProcessingException(
            PSWorkFlowUtils.ERROR_INVALID_PARAMETER_TYPE,
            "The request context must not be null");
      }

      if (null == resultDoc)
      {
         throw new PSExtensionProcessingException(
            PSWorkFlowUtils.ERROR_INVALID_PARAMETER_TYPE,
            "The result document must not be null");
      }

      Exception fatal = null;

      try
      {
         Iterator wfActionRefIter =
               PSWorkFlowUtils.getWorkflowActionExtensionRefs();

         // If there are no workflow actions installed we are done
         if (!wfActionRefIter.hasNext())
         {
            return resultDoc;
         }
         Element workflowActionList = PSXmlDocumentBuilder.addElement
               (
                  resultDoc,                       // document
                  resultDoc.getDocumentElement(),  // parent
                  "workflowactionlist",            // name
                  null);                           // value

         while (wfActionRefIter.hasNext())
         {

            PSExtensionRef wfActionRef
                  = (PSExtensionRef) wfActionRefIter.next();
            Element workflowAction =
                  PSXmlDocumentBuilder.addElement(resultDoc,
                                                  workflowActionList,
                                                  "workflowaction",
                                                  wfActionRef.toString());

            workflowAction.setAttribute("extensionhandlername",
                                        wfActionRef.getHandlerName());
            workflowAction.setAttribute("extensioncontext",
                                        wfActionRef.getContext());
            workflowAction.setAttribute("extensionname",
                                        wfActionRef.getExtensionName());

         }
      }
         catch(DOMException e)
         {
            fatal = e;
         }
         catch (PSExtensionException e)
         {
            fatal = e;
         }
         finally
         {
            if (null != fatal )
            {
               PSWorkFlowUtils.printWorkflowException(requestContext, fatal);
               if (fatal instanceof PSException)
               {
                  String language = ((PSException)fatal).getLanguageString();
                  if (language == null)
                     language = PSI18nUtils.DEFAULT_LANG;
                  throw new PSExtensionProcessingException(language,
                   m_fullExtensionName, fatal);
               }
               else
                  throw new PSExtensionProcessingException(m_fullExtensionName,
                   fatal);
            }
            PSWorkFlowUtils.printWorkflowMessage(requestContext,
               "Exiting PSGetWorkflowActionList: processResultDocument");
         }
      return resultDoc;
   }
}
