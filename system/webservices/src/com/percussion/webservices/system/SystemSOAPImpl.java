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
package com.percussion.webservices.system;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.system.data.PSContentStatusHistory;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSBaseSOAPImpl;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSInvalidLocaleException;
import com.percussion.webservices.PSUserNotMemberOfCommunityException;
import com.percussion.webservices.PSWebserviceErrors;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.faults.PSContractViolationFault;
import com.percussion.webservices.faults.PSErrorResultsFault;
import com.percussion.webservices.faults.PSErrorsFault;
import com.percussion.webservices.faults.PSInvalidLocaleFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSNotAuthorizedFault;
import com.percussion.webservices.faults.PSUnknownRelationshipTypeFault;
import com.percussion.webservices.faults.PSUseSpecificMethodsFault;
import com.percussion.webservices.faults.PSUserNotMemberOfCommunityFault;
import org.apache.commons.lang3.StringUtils;
import com.percussion.webservices.ExceptionUtils;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Arrays;

/**
 * Server side implementations for web services defined in
 * <code>rhythmyx.wsdl</code> for operations defined in the
 * <code>systemSOAP</code> bindings.
 */
public class SystemSOAPImpl extends PSBaseSOAPImpl
   implements com.percussion.webservices.system.System
{
   /*
    * (non-Javadoc)
    *
    * @see System#createRelationship(CreateRelationshipRequest)
    */
   public CreateRelationshipResponse createRelationship(
      CreateRelationshipRequest req)
      throws com.percussion.webservices.system.UnknownRelationshipTypeFaultMessage, com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage, com.percussion.webservices.system.UseSpecificMethodsFaultMessage
   {
      final String serviceName = "createRelationship";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         // get data from request
         PSLegacyGuid ownerId = new PSLegacyGuid(req.getOwnerId());
         PSLegacyGuid dependentId = new PSLegacyGuid(req.getDependentId());
         String configName = req.getName();

         // create and save relationships
         IPSSystemWs service = PSSystemWsLocator.getSystemWebservice();
         com.percussion.design.objectstore.PSRelationship relationship =
            service.createRelationship(configName, ownerId, dependentId);

         // convert the saved relationship
         PSRelationship result = (PSRelationship) convert(
               PSRelationship.class, relationship);

         CreateRelationshipResponse resp = new CreateRelationshipResponse();
         resp.setPSRelationship(result);
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      // will never get here
      return new CreateRelationshipResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see System#deleteRelationships(DeleteRelationshipsRequest)
    */
   public void deleteRelationships(DeleteRelationshipsRequest deleteRelationshipsRequest)
      throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage, com.percussion.webservices.system.ErrorsFaultMessage
   {
      final String serviceName = "deleteRelationships";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         List<Long> ids = deleteRelationshipsRequest.getId() == null ? java.util.Collections.emptyList() : deleteRelationshipsRequest.getId();
         List<IPSGuid> idList = PSWebserviceUtils.getLegacyGuidFromLong(ids.stream().mapToLong(Long::longValue).toArray());

         IPSSystemWs service = PSSystemWsLocator.getSystemWebservice();
         service.deleteRelationships(idList);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.system.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see System#findChildren(FindChildrenRequest)
    */
   public FindDependentsResponse findDependents(FindDependentsRequest request)
      throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage
   {
      final String serviceName = "findDependents";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         // converts the request data
         PSRelationshipFilter filter;
         PSLegacyGuid ownerId;
         if (request.getPSRelationshipFilter() != null)
         {
            request.getPSRelationshipFilter().setOwner(request.getId());
            filter = PSWebserviceUtils.getRelationshipFilter(
               request.getPSRelationshipFilter());
            // the revision of the owner may have been modified if it was -1
            // get the owner id from the filter
            ownerId = new PSLegacyGuid(filter.getOwner());
         }
         else
         {
            filter = null;
            ownerId = new PSLegacyGuid(request.getId());
         }

         // find the dependents
         IPSSystemWs service = PSSystemWsLocator.getSystemWebservice();
         List<IPSGuid> ids = service.findDependents(ownerId, filter);

         FindDependentsResponse resp = new FindDependentsResponse();
         FindDependentsResponse.Ids idsBean = new FindDependentsResponse.Ids();
         long[] idArr = PSWebserviceUtils.getLongsFromGuids(ids);
         for (long l : idArr) idsBean.getId().add(l);
         resp.setIds(idsBean);
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      // will never get here
      return new FindDependentsResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see System#findParents(FindParentsRequest)
    */
   public FindOwnersResponse findOwners(FindOwnersRequest request)
      throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage
   {
      final String serviceName = "findOwners";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         // converts the request data
         PSRelationshipFilter filter = PSWebserviceUtils.getRelationshipFilter(
               request.getPSRelationshipFilter());
         PSLegacyGuid dependentId = new PSLegacyGuid(request.getId());

         // finds the owners
         IPSSystemWs service = PSSystemWsLocator.getSystemWebservice();
         List<IPSGuid> ids = service.findOwners(dependentId, filter);

         FindOwnersResponse resp = new FindOwnersResponse();
         FindOwnersResponse.Ids idsBean = new FindOwnersResponse.Ids();
         long[] idArr = PSWebserviceUtils.getLongsFromGuids(ids);
         for (long l : idArr) idsBean.getId().add(l);
         resp.setIds(idsBean);
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      // will never get here
      return new FindOwnersResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see System#loadAuditTrails(LoadAuditTrailsRequest)
    */
   public LoadAuditTrailsResponse loadAuditTrails(
      LoadAuditTrailsRequest request) throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage, com.percussion.webservices.system.ErrorResultsFaultMessage
   {
      final String serviceName = "loadAuditTrails";
      try
      {
         List<Long> ids = request.getId() == null ? java.util.Collections.emptyList() : request.getId();

         List<IPSGuid> idList = PSWebserviceUtils.getLegacyGuidFromLong(ids.stream().mapToLong(Long::longValue).toArray());

         IPSSystemWs service = PSSystemWsLocator.getSystemWebservice();
         Map<IPSGuid, List<PSContentStatusHistory>> auditTrails =
            service.loadAuditTrails(idList);

         LoadAuditTrailsResponse resp = new LoadAuditTrailsResponse();
         resp.getPSAuditTrail().addAll(Arrays.asList(convertAuditTrails(idList, auditTrails)));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         PSErrorResultsFault err = new PSErrorResultsFault();
         err.setService(serviceName);
         throw new com.percussion.webservices.system.ErrorResultsFaultMessage(err.toString(), err);
      }

      // will never get here
      return new LoadAuditTrailsResponse();
   }

   /**
    * Converts a list of content histories to audit trail objects.
    *
    * @param idList the id list, assumed matches map key and not
    *   <code>null</code> or empty.
    * @param auditTrails the to be converted content histories, assumed not
    *    <code>null</code> or empty.
    *
    * @return the converted list in the order of id list, never
    *   <code>null</code> or empty.
    */
   private PSAuditTrail[] convertAuditTrails(List<IPSGuid> idList,
      Map<IPSGuid, List<PSContentStatusHistory>> auditTrails)
   {
      List<PSAuditTrail> result = new java.util.ArrayList<>();
      for (int i=0; i < idList.size(); i++)
      {
         IPSGuid id = idList.get(i);
         List<PSContentStatusHistory> hists = auditTrails.get(id);

         PSAuditTrail trail = new PSAuditTrail();
         PSAuditTrail.Audits auditsContainer = new PSAuditTrail.Audits();

         for (PSContentStatusHistory hist : hists)
         {
            PSAudit audit = getAudit(hist);
            auditsContainer.getPSAudit().add(audit);
         }

         trail.setAudits(auditsContainer);
         trail.setId(new PSDesignGuid(id).getValue());

         result.add(trail);
      }
      return result.toArray(new PSAuditTrail[0]);
   }

   /**
    * Creates an audit instance from a specified content status history.
    * Note, the the transition name will be reseted if the transition id is
    * <code>0</code>, where the transition name will be set {@link #CHECKIN}
    * when there is no check out user name; otherwise it will be set to
    * {@link #CHECKOUT}.
    *
    * @param hist the source data, assumed not <code>null</code>.
    *
    * @return the created audit object, never <code>null</code>.
    */
   private PSAudit getAudit(PSContentStatusHistory hist)
   {
      PSDesignGuid id = new PSDesignGuid(new PSLegacyGuid(hist.getContentId(),
         hist.getRevision()));
      java.util.GregorianCalendar time = new java.util.GregorianCalendar();
      time.setTime(hist.getEventTime());

      PSAudit audit = new PSAudit();
      audit.setId(id.getValue());
      audit.setRevision(hist.getRevision());
      try {
          javax.xml.datatype.XMLGregorianCalendar xmlTime = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(time);
          audit.setEventTime(xmlTime);
      } catch (javax.xml.datatype.DatatypeConfigurationException dce) {
          // ignore date on failure
      }
      audit.setActor(hist.getActor());
      audit.setStateId(hist.getStateId());
      audit.setStateName(hist.getStateName());
      audit.setTransitionId(hist.getTransitionId());

      String label = hist.getTransitionLabel();
      if (hist.getTransitionId() == 0)
      {
         if (StringUtils.isBlank(hist.getCheckoutUserName()))
            label = CHECKIN;
         else
            label = CHECKOUT;
      }

      audit.setTransitionName(label);
      audit.setTransitionComment(hist.getTransitionComment());
      audit.setPublishable(hist.isValid());

      return audit;
   }

   /**
    * The interpreted string for the transition name of the {@link PSAudit}
    * when the transition id is <code>0</code> and the checkout user of the
    * related {@link PSComponentSummary} is <code>null</code> or empty.
    */
   private static String CHECKIN = "Checked in";

   /**
    * The interpreted string for the transition name of the {@link PSAudit}
    * when the transition id is <code>0</code> and the checkout user of the
    * related {@link PSComponentSummary} is not empty.
    */
   private static String CHECKOUT = "Checked out";

   /*
    * (non-Javadoc)
    *
    * @see System#loadRelationships(LoadRelationshipsRequest)
    */
   public LoadRelationshipsResponse loadRelationships(
      LoadRelationshipsRequest request) throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage, com.percussion.webservices.system.NotAuthorizedFaultMessage
   {
      final String serviceName = "loadRelationships";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         PSRelationshipFilter filter = PSWebserviceUtils.getRelationshipFilter(
               request.getPSRelationshipFilter());

         IPSSystemWs service = PSSystemWsLocator.getSystemWebservice();
         List<com.percussion.design.objectstore.PSRelationship> relationships =
            service.loadRelationships(filter);

         // convert the saved relationships
         PSRelationship[] converted = (PSRelationship[]) convert(
               PSRelationship[].class, relationships);

         LoadRelationshipsResponse resp = new LoadRelationshipsResponse();
         resp.getPSRelationship().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      // will never get here
      return new LoadRelationshipsResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see System#loadRelationshipTypes(LoadRelationshipTypesRequest)
    */
   public LoadRelationshipTypesResponse loadRelationshipTypes(
      LoadRelationshipTypesRequest req) throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage, com.percussion.webservices.system.NotAuthorizedFaultMessage
   {
      final String serviceName = "loadRelationshipTypes";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         IPSSystemWs service = PSSystemWsLocator.getSystemWebservice();

         List<com.percussion.design.objectstore.PSRelationshipConfig> configs = service
               .loadRelationshipTypes(req.getName(),
                     getRelationshipCategory(req.getCategory()).orElse(null));

         RelationshipConfigSummary[] converted = (RelationshipConfigSummary[]) convert(
               RelationshipConfigSummary[].class, configs);

         LoadRelationshipTypesResponse resp = new LoadRelationshipTypesResponse();
         resp.getPSRelationshipConfigSummary().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         // unknown error
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      // will never get here
      return new LoadRelationshipTypesResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see System#loadWorkflows(LoadWorkflowsRequest)
    */
   public LoadWorkflowsResponse loadWorkflows(LoadWorkflowsRequest loadWorkflowsRequest)
      throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage, com.percussion.webservices.system.NotAuthorizedFaultMessage
   {
      final String serviceName = "loadWorkflows";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         IPSSystemWs service = PSSystemWsLocator.getSystemWebservice();

         List<com.percussion.services.workflow.data.PSWorkflow> wfs =
            service.loadWorkflows(loadWorkflowsRequest.getName());

         PSWorkflow[] converted = (PSWorkflow[]) convert(
            PSWorkflow[].class, wfs);

         LoadWorkflowsResponse resp = new LoadWorkflowsResponse();
         resp.getPSWorkflow().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      // will never get here
      return new LoadWorkflowsResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see System#saveRelationships(com.percussion.services.system.data.PSRelationship[])
    */
   public void saveRelationships(SaveRelationshipsRequest saveRelationshipsRequest) throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage, com.percussion.webservices.system.ErrorsFaultMessage
   {
      final String serviceName = "saveRelationships";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         // Convert webservice PSRelationship beans to design objectstore PSRelationship and save
         List<com.percussion.design.objectstore.PSRelationship> relationships = (List<com.percussion.design.objectstore.PSRelationship>) convert(List.class, saveRelationshipsRequest.getPSRelationship());
         IPSSystemWs service = PSSystemWsLocator.getSystemWebservice();
         service.saveRelationships(relationships);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.system.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see System#switchCommunity(SwitchCommunityRequest)
    */
   public void switchCommunity(SwitchCommunityRequest switchCommunityRequest)
      throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage, com.percussion.webservices.system.UserNotMemberOfCommunityFaultMessage
   {
      final String serviceName = "switchCommunity";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         IPSSystemWs sysSvc = PSSystemWsLocator.getSystemWebservice();
         sysSvc.switchCommunity(switchCommunityRequest.getName());
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSUserNotMemberOfCommunityException e)
      {
         throw new com.percussion.webservices.system.UserNotMemberOfCommunityFaultMessage(e.toString(), e);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see System#switchLocale(SwitchLocaleRequest)
    */
   public void switchLocale(SwitchLocaleRequest switchLocaleRequest)
      throws com.percussion.webservices.system.InvalidLocaleFaultMessage, com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage
   {
      final String serviceName = "switchLocale";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         IPSSystemWs sysSvc = PSSystemWsLocator.getSystemWebservice();
         sysSvc.switchLocale(switchLocaleRequest.getCode());
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSInvalidLocaleException e)
      {
         throw new com.percussion.webservices.system.InvalidLocaleFaultMessage(e.toString(), e);
      }

   }

   /*
    * (non-Javadoc)
    *
    * @see System#transitionItems(TransitionItemsRequest)
    */
   public TransitionItemsResponse transitionItems(TransitionItemsRequest request)
      throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage, com.percussion.webservices.system.ErrorResultsFaultMessage
   {
      final String serviceName = "transitionItems";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         List<IPSGuid> guids = request.getId() == null ? java.util.Collections.emptyList() : request.getId().stream().map(id -> PSGuidUtils.makeGuid(id.longValue(), PSTypeEnum.ITEM)).toList();

         IPSSystemWs sysSvc = PSSystemWsLocator.getSystemWebservice();
         List<String> states = sysSvc.transitionItems(guids, request.getTransition());

         TransitionItemsResponse resp = new TransitionItemsResponse();
         TransitionItemsResponse.States st = new TransitionItemsResponse.States();
         st.getState().addAll(states);
         resp.setStates(st);
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.system.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException ex)
      {
         PSErrorResultsFault err = new PSErrorResultsFault();
         err.setService(serviceName);
         throw new com.percussion.webservices.system.ErrorResultsFaultMessage(ex.getLocalizedMessage(), err);
      }

      // will never get here
      return new TransitionItemsResponse();
   }

   /* (non-Javadoc)
    * @see com.percussion.webservices.system.System#getAllowedTransitions(long[])
    */
   public GetAllowedTransitionsResponse getAllowedTransitions(GetAllowedTransitionsRequest request) throws com.percussion.webservices.system.InvalidSessionFaultMessage, com.percussion.webservices.system.ContractViolationFaultMessage
   {
      String serviceName = "getAllowedTransitions";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.system.InvalidSessionFaultMessage(e.toString(), e); }

         List<Long> ids = request.getId() == null ? java.util.Collections.emptyList() : request.getId();
         List<IPSGuid> guids = PSWebserviceUtils.getLegacyGuidFromLong(ids.stream().mapToLong(Long::longValue).toArray());

         IPSSystemWs sysSvc = PSSystemWsLocator.getSystemWebservice();
         Map<String, String> transitions = sysSvc.getAllowedTransitions(guids);

         GetAllowedTransitionsResponse resp =
            new GetAllowedTransitionsResponse();
         for (Entry<String, String> entry : transitions.entrySet())
         {
            resp.getTransition().add(entry.getKey());
            resp.getLabel().add(entry.getValue());
         }

         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.system.ContractViolationFaultMessage(cv.toString(), cv); }
      }

      // will never get here
      return new GetAllowedTransitionsResponse();
   }
}
