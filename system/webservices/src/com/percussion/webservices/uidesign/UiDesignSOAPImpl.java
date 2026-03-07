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
package com.percussion.webservices.uidesign;

import com.percussion.cms.objectstore.PSAction;
import com.percussion.cms.objectstore.PSSearch;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.ui.PSUiException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSBaseSOAPImpl;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.common.PSObjectSummary;
import com.percussion.webservices.faults.PSContractViolationFault;
import com.percussion.webservices.faults.PSErrorResultsFault;
import com.percussion.webservices.faults.PSErrorsFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSNotAuthorizedFault;
import com.percussion.webservices.ui.IPSUiDesignWs;
import com.percussion.webservices.ui.PSUiWsLocator;
import com.percussion.webservices.ui.data.ActionType;
import com.percussion.webservices.ui.data.PSDisplayFormat;
import com.percussion.webservices.ui.data.PSHierarchyNode;
import com.percussion.webservices.ui.data.PSSearchDef;
import com.percussion.webservices.ui.data.PSViewDef;
import com.percussion.webservices.faults.PSLockFault;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Server side implementations for web services defined in
 * <code>rhythmyxDesign.wsdl</code> for operations defined in the
 * <code>uiDesignSOAP</code> bindings.
 */
public class UiDesignSOAPImpl extends PSBaseSOAPImpl
{
   /*
    * (non-Javadoc)
    *
    * @see UiDesign#createActions(CreateActionsRequest)
    */
   public com.percussion.webservices.uidesign.CreateActionsResponse createActions(
      CreateActionsRequest req) throws com.percussion.webservices.uidesign.InvalidSessionFaultMessage,
      com.percussion.webservices.uidesign.NotAuthorizedFaultMessage, com.percussion.webservices.uidesign.ContractViolationFaultMessage
   {
      final String serviceName = "createActions";

      String session;
      try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.uidesign.InvalidSessionFaultMessage(e.toString(), e); }
      String user = getRemoteUser().orElse(null);

      // get list of names and action types from the request (handle array or List)
      Object namesObj = req.getName();
      List<String> names;
      if (namesObj instanceof String[])
         names = Arrays.asList((String[]) namesObj);
      else
         names = (List<String>) namesObj;

      List<ActionType> types = new ArrayList<>();
      Object typesObj = req.getType();
      if (typesObj instanceof Object[])
      {
         for (Object t : (Object[]) typesObj)
         {
            String val = t == null ? "" : (t instanceof String ? (String) t : t.toString());
            if ("item".equalsIgnoreCase(val) || "_item".equalsIgnoreCase(val))
               types.add(ActionType.ITEM);
            else if ("cascading".equalsIgnoreCase(val) || "_cascading".equalsIgnoreCase(val))
               types.add(ActionType.CASCADING);
            else
               types.add(ActionType.DYNAMIC);
         }
      }
      else if (typesObj instanceof List)
      {
         for (Object t : (List<?>) typesObj)
         {
            String val = t == null ? "" : (t instanceof String ? (String) t : t.toString());
            if ("item".equalsIgnoreCase(val) || "_item".equalsIgnoreCase(val))
               types.add(ActionType.ITEM);
            else if ("cascading".equalsIgnoreCase(val) || "_cascading".equalsIgnoreCase(val))
               types.add(ActionType.CASCADING);
            else
               types.add(ActionType.DYNAMIC);
         }
      }

      // create the actions
      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      com.percussion.webservices.uidesign.CreateActionsResponse response = new com.percussion.webservices.uidesign.CreateActionsResponse();
      try
      {
         List<PSAction> actions = uiws.createActions(names, types, session, user);
         // convert the actions
         com.percussion.webservices.ui.data.PSAction[] result = (com.percussion.webservices.ui.data.PSAction[]) convert(
            com.percussion.webservices.ui.data.PSAction[].class, actions);
         for (com.percussion.webservices.ui.data.PSAction a : result) response.getPSAction().add(a);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.uidesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSLockErrorException e)
      {
         try { handleLockError(e); } catch (PSLockFault lf) { throw new RuntimeException(lf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.uidesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#createDisplayFormats(String[])
    */
   public PSDisplayFormat[] createDisplayFormats(String[] names)
      throws com.percussion.webservices.uidesign.InvalidSessionFaultMessage, com.percussion.webservices.uidesign.ContractViolationFaultMessage, com.percussion.webservices.uidesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "createDisplayFormats";

      String session;
      try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.uidesign.InvalidSessionFaultMessage(e.toString(), e); }
      String user = getRemoteUser().orElse(null);

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      PSDisplayFormat[] result = null;
      try
      {
         List<com.percussion.cms.objectstore.PSDisplayFormat> dspFormats =
            uiws.createDisplayFormats(Arrays.asList(names), session, user);
         result = (PSDisplayFormat[]) convert(PSDisplayFormat[].class, dspFormats);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.uidesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSLockErrorException e)
      {
         try { handleLockError(e); } catch (PSLockFault lf) { throw new RuntimeException(lf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.uidesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#createSearches(CreateSearchesRequest)
    */
   public com.percussion.webservices.uidesign.CreateSearchesResponse createSearches(
      CreateSearchesRequest req) throws com.percussion.webservices.uidesign.InvalidSessionFaultMessage,
      com.percussion.webservices.uidesign.ContractViolationFaultMessage, com.percussion.webservices.uidesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "createSearches";

      String session;
      try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.uidesign.InvalidSessionFaultMessage(e.toString(), e); }
      String user = getRemoteUser().orElse(null);

      // get list of names and types from the request (handle array or List return types)
      Object namesObj = req.getName();
      List<String> names;
      if (namesObj instanceof String[])
         names = Arrays.asList((String[]) namesObj);
      else
         names = (List<String>) namesObj;

      List<String> types = new ArrayList<>();
      Object typesObj = req.getType();
      if (typesObj instanceof Object[])
      {
         for (Object t : (Object[]) typesObj)
         {
            String val = t == null ? "" : (t instanceof String ? (String) t : t.toString());
            if ("custom".equalsIgnoreCase(val) || "_custom".equalsIgnoreCase(val))
               types.add(PSSearch.TYPE_CUSTOMSEARCH);
            else if ("standard".equalsIgnoreCase(val) || "_standard".equalsIgnoreCase(val))
               types.add(PSSearch.TYPE_STANDARDSEARCH);
            else
               types.add(PSSearch.TYPE_USERSEARCH);
         }
      }
      else
      {
         for (Object t : (List<?>) typesObj)
         {
            String val = t == null ? "" : (t instanceof String ? (String) t : t.toString());
            if ("custom".equalsIgnoreCase(val) || "_custom".equalsIgnoreCase(val))
               types.add(PSSearch.TYPE_CUSTOMSEARCH);
            else if ("standard".equalsIgnoreCase(val) || "_standard".equalsIgnoreCase(val))
               types.add(PSSearch.TYPE_STANDARDSEARCH);
            else
               types.add(PSSearch.TYPE_USERSEARCH);
         }
      }

      // create the searches
      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      com.percussion.webservices.uidesign.CreateSearchesResponse response = new com.percussion.webservices.uidesign.CreateSearchesResponse();
      try
      {
         List<PSSearch> searches = uiws.createSearches(names, types, session, user);
         PSSearchDef[] result = (PSSearchDef[]) convert(PSSearchDef[].class, searches);
         for (PSSearchDef d : result) response.getPSSearchDef().add(d);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.uidesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSLockErrorException e)
      {
         try { handleLockError(e); } catch (PSLockFault lf) { throw new RuntimeException(lf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.uidesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#createViews(String[])
    */
   public PSViewDef[] createViews(String[] names)
      throws RemoteException, com.percussion.webservices.uidesign.InvalidSessionFaultMessage, com.percussion.webservices.uidesign.ContractViolationFaultMessage,
      com.percussion.webservices.uidesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "createViews";

      String session;
      try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.uidesign.InvalidSessionFaultMessage(e.toString(), e); }
      String user = getRemoteUser().orElse(null);

      // get list of names ane types from the request
      List<String> nameList = Arrays.asList( names);

      // create the searches
      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      PSViewDef[] result = null;
      try
      {
         List<PSSearch> searches = uiws.createViews(nameList, session, user);
         result = (PSViewDef[]) convert(PSViewDef[].class, searches);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.uidesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSLockErrorException e)
      {
         try { handleLockError(e); } catch (PSLockFault lf) { throw new RuntimeException(lf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.uidesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#deleteActions(DeleteActionsRequest)
    */
   public void deleteActions(DeleteActionsRequest req)
      throws RemoteException, PSInvalidSessionFault, PSErrorsFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String service = "deleteActions";
      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      // convert ids to a list of GUIDs
      List<IPSGuid> ids = new ArrayList<IPSGuid>();
      for (long id : req.getId())
         ids.add(new PSDesignGuid(id));

      boolean ignoreDep = extractBooleanValue(req.isIgnoreDependencies(),
         false);

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      try
      {
         uiws.deleteActions(ids, ignoreDep, session, user);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, service);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, service);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, service);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#deleteDisplayFormats(DeleteDisplayFormatsRequest)
    */
   public void deleteDisplayFormats(
      DeleteDisplayFormatsRequest req)
      throws RemoteException, PSInvalidSessionFault, PSErrorsFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String service = "deleteDisplayFormats";
      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      // convert ids to a list of GUIDs
      List<IPSGuid> ids = new ArrayList<IPSGuid>();
      for (long id : req.getId())
         ids.add(new PSDesignGuid(id));
      boolean ignoreDep = extractBooleanValue(req.isIgnoreDependencies(),
         false);

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      try
      {
         uiws.deleteDisplayFormats(ids, ignoreDep, session, user);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, service);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, service);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, service);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#deleteSearches(DeleteSearchesRequest)
    */
   public void deleteSearches(DeleteSearchesRequest req)
      throws RemoteException, PSInvalidSessionFault, PSErrorsFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String service = "deleteSearches";
      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      // convert ids to a list of GUIDs
      List<IPSGuid> ids = new ArrayList<IPSGuid>();
      for (long id : req.getId())
         ids.add(new PSDesignGuid(id));

      boolean ignoreDep = extractBooleanValue(req.isIgnoreDependencies(),
         false);

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      try
      {
         uiws.deleteSearches(ids, ignoreDep, session, user);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, service);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, service);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, service);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#deleteViews(DeleteViewsRequest)
    */
   public void deleteViews(DeleteViewsRequest req)
      throws RemoteException, PSInvalidSessionFault, PSErrorsFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String service = "deleteViews";
      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      // convert ids to a list of GUIDs
      List<IPSGuid> ids = new ArrayList<IPSGuid>();
      for (long id : req.getId())
         ids.add(new PSDesignGuid(id));

      boolean ignoreDep = extractBooleanValue(req.isIgnoreDependencies(),
         false);

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      try
      {
         uiws.deleteViews(ids, ignoreDep, session, user);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, service);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, service);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, service);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#findActions(FindActionsRequest)
    */
   public PSObjectSummary[] findActions(FindActionsRequest request)
      throws RemoteException, PSInvalidSessionFault, PSContractViolationFault
   {
      authenticate();

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      try
      {
         List<ActionType> types = null;
         Object typesObj = request.getType();
         if (typesObj != null)
         {
            types = new ArrayList<>();
            if (typesObj instanceof Object[])
            {
               for (Object t : (Object[]) typesObj)
                  types.add((ActionType) convert(ActionType.class, t));
            }
            else
            {
               for (Object t : (List<?>) typesObj)
                  types.add((ActionType) convert(ActionType.class, t));
            }
         }

         List<IPSCatalogSummary> objects = uiws.findActions(request.getName(),
               request.getLabel(), types);
         return (PSObjectSummary[]) convert(PSObjectSummary[].class, objects);
      }
      catch (PSErrorException e)
      {
         // unknown error
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#findDisplayFormats(FindDisplayFormatsRequest)
    */
   public PSObjectSummary[] findDisplayFormats(
      FindDisplayFormatsRequest request)
      throws RemoteException, PSInvalidSessionFault, PSContractViolationFault
   {
      authenticate();

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      try
      {
         List<IPSCatalogSummary> objects = uiws.findDisplayFormats(request
               .getName(), request.getLabel());
         return (PSObjectSummary[]) convert(PSObjectSummary[].class, objects);
      }
      catch (PSErrorException e)
      {
         // unknown error
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#findSearches(FindSearchesRequest)
    */
   public PSObjectSummary[] findSearches(FindSearchesRequest request)
      throws RemoteException, PSInvalidSessionFault, PSContractViolationFault
   {
      authenticate();

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      try
      {
         List<IPSCatalogSummary> objects = uiws.findSearches(request.getName(),
               request.getLabel());
         return (PSObjectSummary[]) convert(PSObjectSummary[].class, objects);
      }
      catch (PSErrorException e)
      {
         // unknown error
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#findViews(FindViewsRequest)
    */
   public PSObjectSummary[] findViews(FindViewsRequest request)
      throws RemoteException, PSInvalidSessionFault, PSContractViolationFault
   {
      authenticate();

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      try
      {
         List<IPSCatalogSummary> objects = uiws.findViews(request.getName(),
               request.getLabel());
         return (PSObjectSummary[]) convert(PSObjectSummary[].class, objects);
      }
      catch (PSErrorException e)
      {
         // unknown error
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#loadActions(LoadActionsRequest)
    */
   public com.percussion.webservices.ui.data.PSAction[] loadActions(
         LoadActionsRequest loadActionsRequest)
      throws RemoteException, PSErrorResultsFault, PSInvalidSessionFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String service = "loadActions";
      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      // convert ids to a list of GUIDs
      List<IPSGuid> ids = new ArrayList<IPSGuid>();
      for (long id : loadActionsRequest.getId())
         ids.add(new PSDesignGuid(id));

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      com.percussion.webservices.ui.data.PSAction[] result = null;
      try
      {
         List<PSAction> actions = uiws.loadActions(ids, loadActionsRequest
               .isLock(), loadActionsRequest.isOverrideLock(), session, user);

         result = (com.percussion.webservices.ui.data.PSAction[]) convert(
               com.percussion.webservices.ui.data.PSAction[].class, actions);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, service);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, service);
      }
      catch (PSErrorResultsException e)
      {
         handleErrorResultsException(e, service);
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#loadDisplayFormats(LoadDisplayFormatsRequest)
    */
   public PSDisplayFormat[] loadDisplayFormats(
      LoadDisplayFormatsRequest request)
      throws RemoteException, PSErrorResultsFault, PSInvalidSessionFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String service = "loadDisplayFormats";
      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      // convert ids to a list of GUIDs
      List<IPSGuid> ids = new ArrayList<IPSGuid>();
      for (long id : request.getId())
         ids.add(new PSDesignGuid(id));

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      PSDisplayFormat[] result = null;
      try
      {
         List<com.percussion.cms.objectstore.PSDisplayFormat> dspFormats = uiws
               .loadDisplayFormats(ids, request.isLock(), request
                     .isOverrideLock(), session, user);

         result = (PSDisplayFormat[]) convert(PSDisplayFormat[].class, dspFormats);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, service);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, service);
      }
      catch (PSErrorResultsException e)
      {
         handleErrorResultsException(e, service);
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#loadSearches(LoadSearchesRequest)
    */
   public PSSearchDef[] loadSearches(LoadSearchesRequest request)
      throws RemoteException, PSErrorResultsFault, PSInvalidSessionFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String service = "loadSearches";
      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      // convert ids to a list of GUIDs
      List<IPSGuid> ids = new ArrayList<IPSGuid>();
      for (long id : request.getId())
         ids.add(new PSDesignGuid(id));

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      PSSearchDef[] result = null;
      try
      {
         List<com.percussion.cms.objectstore.PSSearch> searches = uiws
               .loadSearches(ids, request.isLock(), request
                     .isOverrideLock(), session, user);

         result = (PSSearchDef[]) convert(PSSearchDef[].class, searches);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, service);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, service);
      }
      catch (PSErrorResultsException e)
      {
         handleErrorResultsException(e, service);
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#loadViews(LoadViewsRequest)
    */
   public PSViewDef[] loadViews(LoadViewsRequest request)
      throws RemoteException, PSErrorResultsFault, PSInvalidSessionFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String service = "loadViews";
      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      // convert ids to a list of GUIDs
      List<IPSGuid> ids = new ArrayList<IPSGuid>();
      for (long id : request.getId())
         ids.add(new PSDesignGuid(id));

      IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
      PSViewDef[] result = null;
      try
      {
         List<com.percussion.cms.objectstore.PSSearch> views = uiws
               .loadViews(ids, request.isLock(), request
                     .isOverrideLock(), session, user);

         result = (PSViewDef[]) convert(PSViewDef[].class, views);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, service);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, service);
      }
      catch (PSErrorResultsException e)
      {
         handleErrorResultsException(e, service);
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#saveActions(SaveActionsRequest)
    */

   public void saveActions(SaveActionsRequest saveActionsRequest)
      throws RemoteException, PSErrorsFault, com.percussion.webservices.uidesign.InvalidSessionFaultMessage,
      PSContractViolationFault, com.percussion.webservices.uidesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "saveActions";

      String session;
      try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.uidesign.InvalidSessionFaultMessage(e.toString(), e); }
      String user = getRemoteUser().orElse(null);

      try
      {
         List<PSAction> actions = (List<PSAction>) convert(List.class,
               saveActionsRequest.getPSAction());

         IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
         uiws.saveActions(actions, saveActionsRequest.isRelease()
               .booleanValue(), session, user);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, serviceName);
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.uidesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#saveDisplayFormats(SaveDisplayFormatsRequest)
    */

   public void saveDisplayFormats(SaveDisplayFormatsRequest request)
      throws RemoteException, PSErrorsFault, PSInvalidSessionFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String serviceName = "saveDisplayFormats";

      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      try
      {
         List<com.percussion.cms.objectstore.PSDisplayFormat> dspFormats =
            (List<com.percussion.cms.objectstore.PSDisplayFormat>) convert(
               List.class, request.getPSDisplayFormat());

         IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
         uiws.saveDisplayFormats(dspFormats, request.isRelease().booleanValue(),
               session, user);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, serviceName);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#saveSearches(SaveSearchesRequest)
    */

   public void saveSearches(SaveSearchesRequest saveSearchesRequest)
      throws RemoteException, PSErrorsFault, PSInvalidSessionFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String serviceName = "saveSearches";

      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      try
      {
         List<PSSearch> searches = (List<PSSearch>) convert(List.class,
               saveSearchesRequest.getPSSearchDef());

         IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
         uiws.saveSearches(searches, saveSearchesRequest.isRelease()
               .booleanValue(), session, user);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, serviceName);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#saveViews(SaveViewsRequest)
    */

   public void saveViews(SaveViewsRequest saveViewsRequest)
      throws RemoteException, PSErrorsFault, PSInvalidSessionFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String serviceName = "saveViews";

      String session = authenticate();
      String user = getRemoteUser().orElse(null);

      try
      {
         List<PSSearch> searches = (List<PSSearch>) convert(List.class,
               saveViewsRequest.getPSViewDef());

         IPSUiDesignWs uiws = PSUiWsLocator.getUiDesignWebservice();
         boolean release = extractBooleanValue(saveViewsRequest.isRelease(), false);
         uiws.saveViews(searches, release, session, user);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, serviceName);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#createHierarchyNodes(CreateHierarchyNodesRequest)
    */

   public PSHierarchyNode[] createHierarchyNodes(
      CreateHierarchyNodesRequest createHierarchyNodesRequest)
      throws RemoteException, PSInvalidSessionFault, PSContractViolationFault,
      PSNotAuthorizedFault
   {
      final String serviceName = "createHierarchyNodes";
      try
      {
         String session = authenticate();
         String user = getRemoteUser().orElse(null);

         IPSUiDesignWs service = PSUiWsLocator.getUiDesignWebservice();

         Object namesObj = createHierarchyNodesRequest.getName();
         List<String> namesList;
         if (namesObj instanceof String[])
            namesList = Arrays.asList((String[]) namesObj);
         else
            namesList = (List<String>) namesObj;

         List<IPSGuid> parents = new ArrayList<IPSGuid>();
         for (long parent : createHierarchyNodesRequest.getParentId())
            parents.add(parent == 0 ? null : new PSDesignGuid(parent));

         List<com.percussion.services.ui.data.PSHierarchyNode.NodeType> types =
            new ArrayList<com.percussion.services.ui.data.PSHierarchyNode.NodeType>();
         Object typesObj = createHierarchyNodesRequest.getType();
         if (typesObj instanceof Object[])
         {
            for (Object t : (Object[]) typesObj)
            {
               types.add((com.percussion.services.ui.data.PSHierarchyNode.NodeType) convert(
                  com.percussion.services.ui.data.PSHierarchyNode.NodeType.class, t));
            }
         }
         else
         {
            for (Object t : (List<?>) typesObj)
            {
               types.add((com.percussion.services.ui.data.PSHierarchyNode.NodeType) convert(
                  com.percussion.services.ui.data.PSHierarchyNode.NodeType.class, t));
            }
         }

         return (PSHierarchyNode[]) convert(PSHierarchyNode[].class,
            service.createHierarchyNodes(namesList, parents, types,
               session, user));
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }
      catch (PSUiException e)
      {
         // this should never happen
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }

      // will never get here
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#deleteHierarchyNodes(DeleteHierarchyNodesRequest)
    */
   public void deleteHierarchyNodes(
      DeleteHierarchyNodesRequest deleteHierarchyNodesRequest)
      throws RemoteException, PSInvalidSessionFault, PSErrorsFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String serviceName = "deleteHierarchyNodes";
      try
      {
         String session = authenticate();
         String user = getRemoteUser().orElse(null);

         IPSUiDesignWs service = PSUiWsLocator.getUiDesignWebservice();

         // convert incoming ids (may be long[] or List<Long>) to GUIDs
         List<IPSGuid> ids = new ArrayList<>();
         Object idObj = deleteHierarchyNodesRequest.getId();
         if (idObj instanceof long[])
         {
            for (long id : (long[]) idObj)
               ids.add(new PSDesignGuid(id));
         }
         else if (idObj instanceof Long[])
         {
            for (Long id : (Long[]) idObj)
               ids.add(new PSDesignGuid(id.longValue()));
         }
         else
         {
            for (Long id : (List<Long>) idObj)
               ids.add(new PSDesignGuid(id));
         }

         boolean ignoreDependencies = extractBooleanValue(
            deleteHierarchyNodesRequest.isIgnoreDependencies(), false);
         service.deleteHierarchyNodes(ids, ignoreDependencies, session, user);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, serviceName);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#findHierarchyNodes(FindHierarchyNodesRequest)
    */
   public PSObjectSummary[] findHierarchyNodes(
      FindHierarchyNodesRequest findHierarchyNodesRequest)
      throws RemoteException, PSInvalidSessionFault, PSContractViolationFault
   {
      final String serviceName = "findHierarchyNodes";
      try
      {
         authenticate();

         IPSUiDesignWs service = PSUiWsLocator.getUiDesignWebservice();

         com.percussion.services.ui.data.PSHierarchyNode.NodeType type = null;
         if (findHierarchyNodesRequest.getType() != null)
         {
            type =
               (com.percussion.services.ui.data.PSHierarchyNode.NodeType) convert(
                  com.percussion.services.ui.data.PSHierarchyNode.NodeType.class,
                  findHierarchyNodesRequest.getType());
         }

         List summaries = service.findHierarchyNodes(
            findHierarchyNodesRequest.getPath(), type);

         return (PSObjectSummary[]) convert(PSObjectSummary[].class, summaries);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
         return null; // never here, used to turn off compiling error
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#getChildren(GetChildrenRequest)
    */
   public long[] getChildren(GetChildrenRequest getChildrenRequest)
      throws RemoteException, PSInvalidSessionFault, PSContractViolationFault
   {
      final String serviceName = "getChildren";
      try
      {
         authenticate();

         IPSUiDesignWs service = PSUiWsLocator.getUiDesignWebservice();

         IPSGuid parentId = null;
         if (getChildrenRequest.getId() != 0)
            parentId = new PSDesignGuid(getChildrenRequest.getId());
         List<IPSGuid> children = service.getChildren(parentId);

         Long[] longs = PSGuidUtils.toLongArray(children);
         long[] result = new long[longs.length];
         for (int i = 0; i < longs.length; i++)
            result[i] = longs[i] == null ? 0L : longs[i].longValue();

         return result;
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
         return null; // never here, used to turn off compiling error
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#idsToPaths(long[])
    */
   public String[] idsToPaths(long[] idsToPathsRequest) throws RemoteException,
      PSInvalidSessionFault, PSContractViolationFault, PSErrorResultsFault
   {
      final String serviceName = "idsToPaths";
      try
      {
         authenticate();

         IPSUiDesignWs service = PSUiWsLocator.getUiDesignWebservice();

         if (idsToPathsRequest == null)
            throw new IllegalArgumentException("ids cannot be null");

         List<String> paths = service.idsToPaths(
            PSGuidUtils.toGuidList(idsToPathsRequest));

         return paths.toArray(new String[paths.size()]);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (PSErrorResultsException e)
      {
         handleErrorResultsException(e, serviceName);
      }

      return null; // never here, used to turn off compiling error
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#loadHierarchyNodes(LoadHierarchyNodesRequest)
    */
   public PSHierarchyNode[] loadHierarchyNodes(
      LoadHierarchyNodesRequest loadHierarchyNodesRequest)
      throws RemoteException, PSErrorResultsFault, PSInvalidSessionFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String serviceName = "loadHierarchyNodes";
      try
      {
         String session = authenticate();
         String user = getRemoteUser().orElse(null);

         IPSUiDesignWs service = PSUiWsLocator.getUiDesignWebservice();

         // convert ids (may be long[]/Long[]/List<Long>) to GUIDs
         List<IPSGuid> ids = new ArrayList<>();
         Object idObj = loadHierarchyNodesRequest.getId();
         if (idObj instanceof long[])
         {
            for (long id : (long[]) idObj)
               ids.add(new PSDesignGuid(id));
         }
         else if (idObj instanceof Long[])
         {
            for (Long id : (Long[]) idObj)
               ids.add(new PSDesignGuid(id.longValue()));
         }
         else
         {
            for (Long id : (List<Long>) idObj)
               ids.add(new PSDesignGuid(id));
         }

         boolean lock = extractBooleanValue(
            loadHierarchyNodesRequest.isLock(), false);
         boolean overrideLock = extractBooleanValue(
            loadHierarchyNodesRequest.isOverrideLock(), false);
         List nodes = service.loadHierachyNodes(ids, lock, overrideLock,
            session, user);

         return (PSHierarchyNode[]) convert(PSHierarchyNode[].class, nodes);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }
      catch (PSErrorResultsException e)
      {
         handleErrorResultsException(e, serviceName);
      }

      return null; // never here, used to turn off compiling error
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#moveChildren(MoveChildrenRequest)
    */
   public void moveChildren(MoveChildrenRequest moveChildrenRequest)
      throws RemoteException, PSInvalidSessionFault, PSContractViolationFault
   {
      final String serviceName = "moveChildren";
      try
      {
         authenticate();

         IPSUiDesignWs service = PSUiWsLocator.getUiDesignWebservice();

         IPSGuid sourceId = new PSDesignGuid(
            moveChildrenRequest.getSourceId());
         IPSGuid targetId = new PSDesignGuid(
            moveChildrenRequest.getTargetId());
         // convert id argument (may be long[]/Long[]/List<Long>) to GUIDs
         List<IPSGuid> children = new ArrayList<>();
         Object idObj = moveChildrenRequest.getId();
         if (idObj instanceof long[])
         {
            for (long id : (long[]) idObj)
               children.add(new PSDesignGuid(id));
         }
         else if (idObj instanceof Long[])
         {
            for (Long id : (Long[]) idObj)
               children.add(new PSDesignGuid(id.longValue()));
         }
         else
         {
            for (Long id : (List<Long>) idObj)
               children.add(new PSDesignGuid(id));
         }
         service.moveChildren(sourceId, targetId, children);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#pathsToIds(String[])
    */
   public long[][] pathsToIds(String[] pathsToIdsRequest) throws RemoteException,
      PSInvalidSessionFault, PSContractViolationFault, PSErrorResultsFault
   {
      final String serviceName = "pathsToIds";
      try
      {
         authenticate();

         IPSUiDesignWs service = PSUiWsLocator.getUiDesignWebservice();

         if (pathsToIdsRequest == null)
            throw new IllegalArgumentException("paths cannot be null");

         List<List<IPSGuid>> idsList = service.pathsToIds(
            Arrays.asList(pathsToIdsRequest));

         long[][] idsArray = new long[idsList.size()][0];
         int index = 0;
         for (List<IPSGuid> ids : idsList)
         {
            Long[] longs = PSGuidUtils.toLongArray(ids);
            long[] prim = new long[longs.length];
            for (int i = 0; i < longs.length; i++)
               prim[i] = longs[i] == null ? 0L : longs[i].longValue();
            idsArray[index++] = prim;
         }

         return idsArray;
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (PSErrorException e)
      {
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }

      return null; // never here, used to turn off compiling error
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#removeChildren(RemoveChildrenRequest)
    */
   public void removeChildren(RemoveChildrenRequest removeChildrenRequest)
      throws RemoteException, PSInvalidSessionFault, PSContractViolationFault
   {
      final String serviceName = "removeChildren";
      try
      {
         authenticate();

         IPSUiDesignWs service = PSUiWsLocator.getUiDesignWebservice();

         IPSGuid parentId = null;
         if (removeChildrenRequest.getParentId() != 0)
            parentId = new PSDesignGuid(removeChildrenRequest.getParentId());
         // convert id argument (may be long[]/Long[]/List<Long>) to GUIDs
         List<IPSGuid> children = new ArrayList<>();
         Object idObj = removeChildrenRequest.getId();
         if (idObj instanceof long[])
         {
            for (long id : (long[]) idObj)
               children.add(new PSDesignGuid(id));
         }
         else if (idObj instanceof Long[])
         {
            for (Long id : (Long[]) idObj)
               children.add(new PSDesignGuid(id.longValue()));
         }
         else
         {
            for (Long id : (List<Long>) idObj)
               children.add(new PSDesignGuid(id));
         }
         service.removeChildren(parentId, children);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see UiDesign#saveHierarchyNodes(SaveHierarchyNodesRequest)
    */

   public void saveHierarchyNodes(
      SaveHierarchyNodesRequest saveHierarchyNodesRequest)
      throws RemoteException, PSErrorsFault, PSInvalidSessionFault,
      PSContractViolationFault, PSNotAuthorizedFault
   {
      final String serviceName = "saveHierarchyNodes";
      try
      {
         String session = authenticate();
         String user = getRemoteUser().orElse(null);

         IPSUiDesignWs service = PSUiWsLocator.getUiDesignWebservice();

         List nodes = (List) convert(List.class,
            saveHierarchyNodesRequest.getPSHierarchyNode());
         boolean release = extractBooleanValue(
            saveHierarchyNodesRequest.isRelease(), true);
         service.saveHierarchyNodes(nodes, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, serviceName);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }
   }
}
