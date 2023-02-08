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
package com.percussion.extensions.general;

import static org.apache.commons.lang.Validate.notNull;

import com.percussion.extension.IPSWorkFlowContext;
import com.percussion.extension.IPSWorkflowAction;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.touchitem.IPSTouchItemService;
import com.percussion.services.touchitem.PSTouchItemLocator;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Touches Active Assembly parent items and Managed Navigation items (navons)
 * after workflow transition.
 * 
 * @author adamgent
 * @author yubingchen
 *
 */
public class PSTouchItemsWorkflowAction extends PSDefaultExtension implements IPSWorkflowAction
{

   private static final Logger ms_logger = LogManager.getLogger(PSTouchItemsWorkflowAction.class);
   
   /**
    * perform the workflow action.
    *
    * @param context the workflow context holds basic information about the
    *    the content item and its workflow state.
    * @param request the request context for the exit
    * @throws PSExtensionProcessingException if any processing errors occur
    */
   public void performAction(IPSWorkFlowContext context, 
      IPSRequestContext request) throws PSExtensionProcessingException
   {
      touchActiveAssemblyParents(context, request);
      IPSTouchItemService touchService = PSTouchItemLocator.getTouchItemService();
      PSLegacyGuid id = new PSLegacyGuid(context.getContentID(), context.getBaseRevisionNum());
      touchService.touchItems(id);
   }
   

   /**
    * Touches all "active assembly parent" items of the 
    * current item. These items are found by searching the related content table 
    * for parent items, and then searching for the parents of those items, etc.
    * The relationships are in the 'active assembly' category only.
    * <p>
    * The content items which are found are then updated so that the LastModifyDate
    * column contains the current date & time.
    *
    * @param context the workflow context holds basic information about the
    *    the content item and its workflow state.
    * @param request the request context for the exit
    * @throws PSExtensionProcessingException if any processing errors occur
    */
   private void touchActiveAssemblyParents(IPSWorkFlowContext context, 
      IPSRequestContext request) throws PSExtensionProcessingException
   {
      diagMessage(request,"Starting Workflow Action");
      Integer contentId = context.getContentID();

      try
      {
         Collection<Integer> cids = new ArrayList<>();
         cids.add(contentId);
         IPSPublisherService pub = PSPublisherServiceLocator.getPublisherService();
         pub.touchActiveAssemblyParents(cids);
      }
      catch (Exception e)
      {
         ms_logger.error("Failed to touch items on workflow action:", e);
         throw new PSExtensionProcessingException(getClass().getName(), e);
      }
   }
   
   /**
    * A private diagnostic message method, used primarily to hide the
    * details of tracing. Output can be to the trace file or console
    *
    * @param request the request context.  Must not be <code>null</code>.
    * @param msg the diagnostic message to output.
    */
   private void diagMessage(IPSRequestContext request, String msg)
   {
      
      notNull(request, "request may not be null");
      notNull(msg, "msg may not be null");
      request.printTraceMessage(msg);
      ms_logger.debug(msg);
   }
   

}
