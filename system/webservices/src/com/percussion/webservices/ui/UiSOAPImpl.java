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
package com.percussion.webservices.ui;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.webservices.PSBaseSOAPImpl;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.faults.PSContractViolationFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSNotAuthorizedFault;
import com.percussion.webservices.ui.data.PSAction;
import com.percussion.webservices.ui.data.PSDisplayFormat;
import com.percussion.webservices.ui.data.PSSearchDef;
import com.percussion.webservices.ui.data.PSViewDef;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Server side implementations for web services defined in
 * <code>rhythmyx.wsdl</code> for operations defined in the
 * <code>uiSOAP</code> bindings.
 */
public class UiSOAPImpl extends PSBaseSOAPImpl implements Ui
{
   /*
    * (non-Javadoc)
    *
    * @see Ui#loadActions(LoadActionsRequest)
    */
   public LoadActionsResponse loadActions(LoadActionsRequest loadActionsRequest)
      throws com.percussion.webservices.ui.InvalidSessionFaultMessage, com.percussion.webservices.ui.NotAuthorizedFaultMessage
   {
      try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.ui.InvalidSessionFaultMessage(e.getMessage(), e); }

      IPSUiWs uiws = PSUiWsLocator.getUiWebservice();
      LoadActionsResponse response = new LoadActionsResponse();
      try
      {
         List<com.percussion.cms.objectstore.PSAction> actions =
            uiws.loadActions(loadActionsRequest.getName());

         com.percussion.webservices.ui.data.PSAction[] result =
            (com.percussion.webservices.ui.data.PSAction[]) convert(com.percussion.webservices.ui.data.PSAction[].class, actions);
         for (com.percussion.webservices.ui.data.PSAction a : result) {
            response.getPSAction().add(a);
         }
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "loadActions"); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorException e)
      {
         // unknown error
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see Ui#loadDisplayFormats(LoadDisplayFormatsRequest)
    */
   public LoadDisplayFormatsResponse loadDisplayFormats(
      LoadDisplayFormatsRequest request)
      throws com.percussion.webservices.ui.InvalidSessionFaultMessage, com.percussion.webservices.ui.NotAuthorizedFaultMessage
   {
      try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.ui.InvalidSessionFaultMessage(e.getMessage(), e); }

      IPSUiWs uiws = PSUiWsLocator.getUiWebservice();
      LoadDisplayFormatsResponse response = new LoadDisplayFormatsResponse();
      try
      {
         List<com.percussion.cms.objectstore.PSDisplayFormat> actions =
            uiws.loadDisplayFormats(request.getName());

         com.percussion.webservices.ui.data.PSDisplayFormat[] result = (com.percussion.webservices.ui.data.PSDisplayFormat[]) convert(com.percussion.webservices.ui.data.PSDisplayFormat[].class, actions);
         for (com.percussion.webservices.ui.data.PSDisplayFormat f : result) response.getPSDisplayFormat().add(f);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "loadDisplayFormats"); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorException e)
      {
         // unknown error
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see Ui#loadSearches(LoadSearchesRequest)
    */
   public LoadSearchesResponse loadSearches(LoadSearchesRequest request)
      throws com.percussion.webservices.ui.InvalidSessionFaultMessage, com.percussion.webservices.ui.NotAuthorizedFaultMessage
   {
      try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.ui.InvalidSessionFaultMessage(e.getMessage(), e); }

      IPSUiWs uiws = PSUiWsLocator.getUiWebservice();
      LoadSearchesResponse response = new LoadSearchesResponse();
      try
      {
         List<com.percussion.cms.objectstore.PSSearch> actions =
            uiws.loadSearches(request.getName());

         com.percussion.webservices.ui.data.PSSearchDef[] result = (com.percussion.webservices.ui.data.PSSearchDef[]) convert(com.percussion.webservices.ui.data.PSSearchDef[].class, actions);
         for (com.percussion.webservices.ui.data.PSSearchDef d : result) response.getPSSearchDef().add(d);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "loadSearches"); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorException e)
      {
         // unknown error
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see Ui#loadViews(LoadViewsRequest)
    */
   public LoadViewsResponse loadViews(LoadViewsRequest request)
      throws com.percussion.webservices.ui.InvalidSessionFaultMessage, com.percussion.webservices.ui.NotAuthorizedFaultMessage
   {
      try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.ui.InvalidSessionFaultMessage(e.getMessage(), e); }

      IPSUiWs uiws = PSUiWsLocator.getUiWebservice();
      LoadViewsResponse response = new LoadViewsResponse();
      try
      {
         List<com.percussion.cms.objectstore.PSSearch> actions =
            uiws.loadViews(request.getName());

         com.percussion.webservices.ui.data.PSViewDef[] result = (com.percussion.webservices.ui.data.PSViewDef[]) convert(com.percussion.webservices.ui.data.PSViewDef[].class, actions);
         for (com.percussion.webservices.ui.data.PSViewDef v : result) response.getPSViewDef().add(v);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "loadViews"); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorException e)
      {
         // unknown error
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      return response;
   }
}
