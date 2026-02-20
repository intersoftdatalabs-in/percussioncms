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
package com.percussion.webservices.securitydesign;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.PSSecurityCatalogException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSBaseSOAPImpl;
import com.percussion.webservices.common.PSObjectSummary;
import com.percussion.webservices.faults.PSContractViolationFault;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.faults.PSErrorResultsFault;
import com.percussion.webservices.faults.PSErrorsFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSNotAuthorizedFault;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import com.percussion.webservices.security.PSSecurityWsLocator;
import com.percussion.webservices.security.data.PSCommunity;
import com.percussion.webservices.security.data.PSCommunityVisibility;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

/**
 * Server side implementations for web services defined in
 * <code>rhythmyxDesign.wsdl</code> for operations defined in the
 * <code>securityDesignSOAP</code> bindings.
 */
public class SecurityDesignSOAPImpl extends PSBaseSOAPImpl implements SecurityDesign
{
   /*
    * (non-Javadoc)
    *
    * @see SecurityDesign#createCommunities(CreateCommunitiesRequest)
    */
   @SuppressWarnings("unused")
   public CreateCommunitiesResponse createCommunities(CreateCommunitiesRequest createCommunitiesRequest)
      throws com.percussion.webservices.securitydesign.InvalidSessionFaultMessage, com.percussion.webservices.securitydesign.ContractViolationFaultMessage, com.percussion.webservices.securitydesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "createCommunities";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securitydesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSecurityDesignWs service =
            PSSecurityWsLocator.getSecurityDesignWebservice();

         List<String> names = createCommunitiesRequest.getName() == null ? java.util.Collections.emptyList() : createCommunitiesRequest.getName();

         PSCommunity[] converted = (PSCommunity[]) convert(PSCommunity[].class,
            service.createCommunities(names, session, user));

         CreateCommunitiesResponse resp = new CreateCommunitiesResponse();
         resp.getPSCommunity().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.securitydesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.securitydesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see SecurityDesign#deleteCommunities(DeleteCommunitiesRequest)
    */
   @SuppressWarnings("unused")
   public void deleteCommunities(
      DeleteCommunitiesRequest deleteCommunitiesRequest)
      throws com.percussion.webservices.securitydesign.InvalidSessionFaultMessage, com.percussion.webservices.securitydesign.ContractViolationFaultMessage, com.percussion.webservices.securitydesign.NotAuthorizedFaultMessage, com.percussion.webservices.securitydesign.ErrorsFaultMessage
   {
      final String serviceName = "deleteCommunities";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securitydesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSecurityDesignWs service =
            PSSecurityWsLocator.getSecurityDesignWebservice();

         List<IPSGuid> ids = deleteCommunitiesRequest.getId() == null ? java.util.Collections.emptyList() : deleteCommunitiesRequest.getId().stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.COMMUNITY_DEF)).toList();
         boolean ignoreDependencies = extractBooleanValue(
            deleteCommunitiesRequest.isIgnoreDependencies(), false);
         service.deleteCommunities(ids, ignoreDependencies, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.securitydesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.securitydesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.securitydesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see SecurityDesign#findCommunities(FindCommunitiesRequest)
    */
   public FindCommunitiesResponse findCommunities(
      FindCommunitiesRequest findCommunitiesRequest) throws com.percussion.webservices.securitydesign.InvalidSessionFaultMessage
   {
      final String serviceName = "findCommunities";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securitydesign.InvalidSessionFaultMessage(e.toString(), e); }

         IPSSecurityDesignWs service =
            PSSecurityWsLocator.getSecurityDesignWebservice();

         List<com.percussion.services.catalog.IPSCatalogSummary> summaries = service.findCommunities(
            findCommunitiesRequest.getName());

         PSObjectSummary[] converted = (PSObjectSummary[]) convert(PSObjectSummary[].class, summaries);
         FindCommunitiesResponse resp = new FindCommunitiesResponse();
         resp.getPSObjectSummary().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }

      // will never get here
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see SecurityDesign#findRoles(FindRolesRequest)
    */
   public FindRolesResponse findRoles(FindRolesRequest findRolesRequest)
      throws com.percussion.webservices.securitydesign.InvalidSessionFaultMessage
   {
      final String serviceName = "findRoles";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securitydesign.InvalidSessionFaultMessage(e.toString(), e); }

         IPSSecurityDesignWs service =
            PSSecurityWsLocator.getSecurityDesignWebservice();

         List<com.percussion.services.catalog.IPSCatalogSummary> summaries = service.findRoles(findRolesRequest.getName());

         PSObjectSummary[] converted = (PSObjectSummary[]) convert(PSObjectSummary[].class, summaries);
         FindRolesResponse resp = new FindRolesResponse();
         resp.getPSObjectSummary().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }

      // will never get here
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see SecurityDesign#loadCommunities(LoadCommunitiesRequest)
    */
   @SuppressWarnings("unused")
   public LoadCommunitiesResponse loadCommunities(
      LoadCommunitiesRequest loadCommunitiesRequest) throws com.percussion.webservices.securitydesign.InvalidSessionFaultMessage, com.percussion.webservices.securitydesign.ContractViolationFaultMessage, com.percussion.webservices.securitydesign.NotAuthorizedFaultMessage, com.percussion.webservices.securitydesign.ErrorResultsFaultMessage
   {
      final String serviceName = "loadCommunities";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securitydesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSecurityDesignWs service =
            PSSecurityWsLocator.getSecurityDesignWebservice();

         List<IPSGuid> ids = loadCommunitiesRequest.getId() == null ? java.util.Collections.emptyList() : loadCommunitiesRequest.getId().stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.COMMUNITY_DEF)).toList();
         boolean lock = extractBooleanValue(
            loadCommunitiesRequest.isLock(), false);
         boolean overrideLock = extractBooleanValue(
            loadCommunitiesRequest.isOverrideLock(), false);
         List<com.percussion.services.security.data.PSCommunity> communities = service.loadCommunities(ids, lock, overrideLock,
            session, user);

         PSCommunity[] converted = (PSCommunity[]) convert(PSCommunity[].class, communities);
         LoadCommunitiesResponse resp = new LoadCommunitiesResponse();
         resp.getPSCommunity().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.securitydesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.securitydesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.securitydesign.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see SecurityDesign#saveCommunities(SaveCommunitiesRequest)
    */
   @SuppressWarnings({"unchecked","unused"})
   public void saveCommunities(
      SaveCommunitiesRequest saveCommunitiesRequest) throws com.percussion.webservices.securitydesign.ErrorsFaultMessage, com.percussion.webservices.securitydesign.NotAuthorizedFaultMessage, com.percussion.webservices.securitydesign.ContractViolationFaultMessage, com.percussion.webservices.securitydesign.InvalidSessionFaultMessage
   {
      final String serviceName = "saveCommunities";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securitydesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSecurityDesignWs service =
            PSSecurityWsLocator.getSecurityDesignWebservice();

         List<com.percussion.services.security.data.PSCommunity> communities = (List<com.percussion.services.security.data.PSCommunity>) convert(List.class,
            saveCommunitiesRequest.getPSCommunity());
         boolean release = extractBooleanValue(
            saveCommunitiesRequest.isRelease(), true);
         service.saveCommunities(communities, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.securitydesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.securitydesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.securitydesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /* (non-Javadoc)
    * @see SecurityDesign#isValidRhythmyxUser(IsValidRhythmyxUserRequest)
    */
   public IsValidRhythmyxUserResponse isValidRhythmyxUser(
      IsValidRhythmyxUserRequest isValidRhythmyxUserRequest)
      throws com.percussion.webservices.securitydesign.InvalidSessionFaultMessage, com.percussion.webservices.securitydesign.ContractViolationFaultMessage
   {
      final String serviceName = "isValidRhythmyxUser";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securitydesign.InvalidSessionFaultMessage(e.toString(), e); }

         IPSSecurityDesignWs service =
            PSSecurityWsLocator.getSecurityDesignWebservice();

         boolean isValidUser = service.isValidRhythmyxUser(
            isValidRhythmyxUserRequest.getUsername());

         IsValidRhythmyxUserResponse resp = new IsValidRhythmyxUserResponse();
         resp.setIsValid(isValidUser);
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.securitydesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSSecurityCatalogException e)
      {
         // should not occur; wrap in runtime to let framework handle
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      // will never get here
      return null;
   }
   /* (non-Javadoc)
    * @see SecurityDesign#getVisibilityByCommunity(
    *    GetVisibilityByCommunityRequest)
    */
   public GetVisibilityByCommunityResponse getVisibilityByCommunity(
      GetVisibilityByCommunityRequest getVisibilityByCommunityRequest)
      throws com.percussion.webservices.securitydesign.ErrorResultsFaultMessage, com.percussion.webservices.securitydesign.NotAuthorizedFaultMessage, com.percussion.webservices.securitydesign.ContractViolationFaultMessage, com.percussion.webservices.securitydesign.InvalidSessionFaultMessage
   {
      final String serviceName = "getVisibilityByCommunity";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.securitydesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSecurityDesignWs service =
            PSSecurityWsLocator.getSecurityDesignWebservice();

         List<IPSGuid> ids = getVisibilityByCommunityRequest.getId() == null ? java.util.Collections.emptyList() : getVisibilityByCommunityRequest.getId().stream().map(id -> PSGuidUtils.makeGuid(id, null)).toList();
         PSTypeEnum type = null;
         if (getVisibilityByCommunityRequest.getType() != null)
         {
            type = PSTypeEnum.valueOf(
               getVisibilityByCommunityRequest.getType());
            if (type == null)
               throw new IllegalArgumentException("unknown type was supplied");
         }
         List<com.percussion.services.security.data.PSCommunityVisibility> visibilities = service.getVisibilityByCommunity(ids, type,
            session, user);

         PSCommunityVisibility[] converted = (PSCommunityVisibility[]) convert(PSCommunityVisibility[].class,
            visibilities);
         GetVisibilityByCommunityResponse resp = new GetVisibilityByCommunityResponse();
         resp.getPSCommunityVisibility().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.securitydesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.securitydesign.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RemoteException re)
      {
         throw new RuntimeException(re);
      }

      // will never get here
      return new GetVisibilityByCommunityResponse();
   }
}
