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
package com.percussion.webservices.systemdesign;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.data.PSUserAccessLevel;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSBaseSOAPImpl;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.PSWebserviceErrors;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.common.PSObjectSummary;
import com.percussion.webservices.faults.PSContractViolationFault;
import com.percussion.webservices.faults.PSErrorResultsFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSErrorsFault;
import com.percussion.webservices.faults.PSLockFault;
import com.percussion.webservices.faults.PSNotAuthorizedFault;
import com.percussion.webservices.system.IPSSystemDesignWs;
import com.percussion.webservices.system.PSAclImpl;
import com.percussion.webservices.system.PSDependency;
import com.percussion.webservices.system.PSItemFilter;
import com.percussion.webservices.system.PSMimeContentAdapter;
import com.percussion.webservices.system.PSRelationshipConfig;
import com.percussion.webservices.system.PSSharedProperty;
import com.percussion.webservices.system.PSSystemWsLocator;
import com.percussion.webservices.system.RelationshipCategory;
import com.percussion.webservices.ExceptionUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Server side implementations for web services defined in
 * <code>rhythmyxDesign.wsdl</code> for operations defined in the
 * <code>systemDesignSOAP</code> bindings.
 */
public class SystemDesignSOAPImpl extends PSBaseSOAPImpl implements SystemDesign
{
   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#createAcls(CreateAclsRequest)
    */
   public com.percussion.webservices.systemdesign.CreateAclsResponse createAcls(com.percussion.webservices.systemdesign.CreateAclsRequest req) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      String session;
      try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
      String user = getRemoteUser().orElse(null);

      String service = "createAcls";
      try
      {
         java.util.List<java.lang.Long> ids = req.getId();
         if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("Ids may not be null or empty");

         IPSSystemDesignWs svce = PSSystemWsLocator.getSystemDesignWebservice();
         java.util.List<com.percussion.services.security.data.PSAclImpl> aclList = new java.util.ArrayList<>();
         for (java.lang.Long id : ids)
         {
            aclList.add(svce.createAcl(new PSDesignGuid(id.longValue()), session, user));
         }

         com.percussion.webservices.systemdesign.CreateAclsResponse resp = new com.percussion.webservices.systemdesign.CreateAclsResponse();
         PSAclImpl[] converted = (PSAclImpl[]) convert(PSAclImpl[].class, aclList);
         resp.getPSAclImpl().addAll(java.util.Arrays.asList(converted));
         return resp;

      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, service); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSLockErrorException e)
      {
         try { handleLockError(e); } catch (PSLockFault lf) { throw new RuntimeException(lf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, service); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new com.percussion.webservices.systemdesign.CreateAclsResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#createRelationshipTypes(CreateRelationshipTypesRequest)
    */
   public com.percussion.webservices.systemdesign.CreateRelationshipTypesResponse createRelationshipTypes(com.percussion.webservices.systemdesign.CreateRelationshipTypesRequest req) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      String session;
      try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
      String user = getRemoteUser().orElse(null);

      String service = "createRelationshipTypes";
      try
      {
         IPSSystemDesignWs svce = PSSystemWsLocator.getSystemDesignWebservice();

         java.util.List<java.lang.String> categories = new java.util.ArrayList<>();
         for (RelationshipCategory cat : req.getCategory())
            categories.add(getRelationshipCategory(cat).orElse(null));

         java.util.List<java.lang.String> names = req.getName();

         java.util.List<com.percussion.design.objectstore.PSRelationshipConfig> configs = svce.createRelationshipTypes(names, categories, session, user);

         com.percussion.webservices.systemdesign.CreateRelationshipTypesResponse resp = new com.percussion.webservices.systemdesign.CreateRelationshipTypesResponse();
         PSRelationshipConfig[] converted = (PSRelationshipConfig[]) convert(PSRelationshipConfig[].class, configs);
         resp.getPSRelationshipConfig().addAll(java.util.Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, service); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSLockErrorException e)
      {
         try { handleLockError(e); } catch (PSLockFault lf) { throw new RuntimeException(lf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, service); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new com.percussion.webservices.systemdesign.CreateRelationshipTypesResponse();
   }

   /**
    * Convertes relationship categories from webservice to objectstore format.
    *
    * @param cats the to be converted categories, assumed not <code>null</code>.
    *
    * @return the converted categories, never <code>null</code>.
    */
   private List<String> getRelationshipCategories(RelationshipCategory[] cats)
   {
      List<String> categories = new ArrayList<>();
      for (RelationshipCategory cat : cats)
      {
         if (cat == null)
            throw new IllegalArgumentException("Relationship Category must not be null.");
         categories.add(getRelationshipCategory(cat).orElse(null));
      }
      return categories;
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#deleteAcls(DeleteAclsRequest)
    */
   public void deleteAcls(com.percussion.webservices.systemdesign.DeleteAclsRequest deleteAclsRequest) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String service = "deleteAcls";
      String session;
      try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
      String user = getRemoteUser().orElse(null);

      IPSSystemDesignWs ws = PSSystemWsLocator.getSystemDesignWebservice();
      try
      {
         java.util.List<java.lang.Long> idList = deleteAclsRequest.getId();
         if (idList == null || idList.isEmpty())
            throw new IllegalArgumentException("Ids may not be null or empty");

         java.util.List<IPSGuid> guids = new java.util.ArrayList<>();
         for (java.lang.Long id : idList)
            guids.add(new PSDesignGuid(id.longValue()));

         boolean ignoreDep = extractBooleanValue(deleteAclsRequest.isIgnoreDependencies(), false);

         ws.deleteAcls(guids, ignoreDep, session, user);
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, service); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.systemdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, service); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, service); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#deleteRelationshipTypes(DeleteRelationshipTypesRequest)
    */
   public void deleteRelationshipTypes(DeleteRelationshipTypesRequest req) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String service = "deleteRelationshipTypes";
      String session;
      try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
      String user = getRemoteUser().orElse(null);

      java.util.List<java.lang.Long> idList = req.getId();
      if (idList == null || idList.isEmpty())
         throw new IllegalArgumentException("Ids may not be null or empty");

      java.util.List<IPSGuid> ids = new java.util.ArrayList<>();
      for (java.lang.Long id : idList)
         ids.add(new PSDesignGuid(id.longValue()));

      boolean ignoreDep = extractBooleanValue(req.isIgnoreDependencies(),
         false);

      IPSSystemDesignWs ws = PSSystemWsLocator.getSystemDesignWebservice();
      try
      {
         ws.deleteRelationshipTypes(ids, ignoreDep, session, user);
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, service); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.systemdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, service); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, service); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#deleteSharedProperties(DeleteSharedPropertiesRequest)
    */

   public void deleteSharedProperties(com.percussion.webservices.systemdesign.DeleteSharedPropertiesRequest deleteSharedPropertiesRequest) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "deleteSharedProperties";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs webService =
            PSSystemWsLocator.getSystemDesignWebservice();

         IPSSystemService service = PSSystemServiceLocator.getSystemService();

         // convert from client to server object
         java.util.List<com.percussion.webservices.system.PSSharedProperty> props = deleteSharedPropertiesRequest.getPSSharedProperty();
         java.util.List<java.lang.String> names = new java.util.ArrayList<>();
         for (com.percussion.webservices.system.PSSharedProperty p : props)
            names.add(p.getName());

         java.util.List<com.percussion.services.system.data.PSSharedProperty> properties =
            (java.util.List<com.percussion.services.system.data.PSSharedProperty>) convert(List.class, deleteSharedPropertiesRequest.getPSSharedProperty());

         // get the correct guid / version for existing properties
         for (com.percussion.services.system.data.PSSharedProperty property : properties)
         {
            java.util.List<com.percussion.services.system.data.PSSharedProperty> existingProperties =
               service.findSharedPropertiesByName(property.getName());
            if (!existingProperties.isEmpty())
            {
               com.percussion.services.system.data.PSSharedProperty existingProperty =
                  existingProperties.get(0);
               property.setGUID(existingProperty.getGUID());
               property.setVersion(existingProperty.getVersion());
            }
         }

         boolean ignoreDeps = extractBooleanValue(deleteSharedPropertiesRequest.isIgnoreDependencies(), false);

         webService.deleteSharedProperties(properties, ignoreDeps, session, user);

      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         PSErrorsFault fault = (PSErrorsFault) convert(
            PSErrorsFault.class, e);
         fault.setService(serviceName);

         throw new com.percussion.webservices.systemdesign.ErrorsFaultMessage(fault.toString(), fault);
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#extendLocks(long[])
    */
   public void extendLocks(ExtendLocksRequest req) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorsFaultMessage
   {
      final String serviceName = "extendLocks";
      try
      {
         if (req == null)
            throw new IllegalArgumentException("extendLockRequest cannot be null");

         List<Long> idList = req.getId();
         if (idList == null || idList.isEmpty())
            throw new IllegalArgumentException("extendLockRequest cannot be empty");

         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service = PSSystemWsLocator.getSystemDesignWebservice();

         List<IPSGuid> ids = new ArrayList<>();
         for (Long id : idList) ids.add(new PSDesignGuid(id.longValue()));
         service.extendLocks(ids, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.systemdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#findDependencies(FindDependenciesRequest)
    */
   public FindDependenciesResponse findDependencies(FindDependenciesRequest req) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage
   {
      final String serviceName = "findDependencies";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }

         if (req == null || req.getId() == null || req.getId().isEmpty())
            throw new IllegalArgumentException("findDependenciesRequest may not be null or empty");

         IPSSystemDesignWs service = PSSystemWsLocator.getSystemDesignWebservice();

         List<IPSGuid> ids = new ArrayList<>();
         for (Long l : req.getId()) ids.add(new PSDesignGuid(l.longValue()));

         List<com.percussion.services.system.data.PSDependency> deps = service.findDependencies(ids);

         FindDependenciesResponse resp = new FindDependenciesResponse();
         PSDependency[] converted = (PSDependency[]) convert(PSDependency[].class, deps);
         resp.getPSDependency().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }

      // will never get here
      return new FindDependenciesResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#findRelationshipTypes(FindRelationshipTypesRequest)
    */
   public FindRelationshipTypesResponse findRelationshipTypes(
      FindRelationshipTypesRequest req) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage
   {
      final String service = "findRelationshipTypes";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }

         IPSSystemDesignWs svce = PSSystemWsLocator.getSystemDesignWebservice();

         List<IPSCatalogSummary> configs = svce.findRelationshipTypes(req.getName(),
               getRelationshipCategory(req.getCategory()).orElse(null));
         PSObjectSummary[] converted = (PSObjectSummary[]) convert(PSObjectSummary[].class, configs);
         FindRelationshipTypesResponse resp = new FindRelationshipTypesResponse();
         resp.getPSObjectSummary().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, service); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         // Map to ContractViolationFaultMessage to satisfy generated signature
         throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(e.toString(), e);
      }

      // will never get here
      return new FindRelationshipTypesResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#findWorkflows(FindWorkflowsRequest)
    */
   public FindWorkflowsResponse findWorkflows(
      FindWorkflowsRequest findWorkflowsRequest) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage
   {
      final String serviceName = "findWorkflows";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }

         IPSSystemDesignWs sysws = PSSystemWsLocator.getSystemDesignWebservice();

         List<IPSCatalogSummary> objects = sysws.findWorkflows(
            findWorkflowsRequest.getName());
         objects = PSWebserviceUtils.toObjectSummaries(objects);

         List<com.percussion.services.catalog.data.PSObjectSummary> summaries =
            new ArrayList<>(
               objects.size());
         for (IPSCatalogSummary object : objects)
            summaries
                  .add((com.percussion.services.catalog.data.PSObjectSummary) object);

         PSObjectSummary[] converted = (PSObjectSummary[]) convert(PSObjectSummary[].class, summaries);
         FindWorkflowsResponse resp = new FindWorkflowsResponse();
         resp.getPSObjectSummary().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }

      // will never get here
      return new FindWorkflowsResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#loadAcls(LoadAclsRequest)
    */
   public com.percussion.webservices.systemdesign.LoadAclsResponse loadAcls(com.percussion.webservices.systemdesign.LoadAclsRequest req) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage, com.percussion.webservices.systemdesign.ErrorResultsFaultMessage
   {
      String service = "loadAcls";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs svce = PSSystemWsLocator.getSystemDesignWebservice();

         // convert List<Long> to List<IPSGuid>
         List<IPSGuid> guids = new ArrayList<>();
         List<Long> ids = req.getId();
         if (ids == null || ids.isEmpty())
         {
            throw new IllegalArgumentException("Ids may not be null or empty");
         }

         for (Long id : ids)
            guids.add(new PSGuid(id.longValue()));

         boolean lock = extractBooleanValue(req.isLock(), false);
         boolean overrideLock = extractBooleanValue(req.isOverrideLock(),
            false);

         List<com.percussion.services.security.data.PSAclImpl> aclList = svce
               .loadAcls(guids, lock, overrideLock, session, user);

         com.percussion.webservices.systemdesign.LoadAclsResponse resp = new com.percussion.webservices.systemdesign.LoadAclsResponse();
         PSAclImpl[] converted = (PSAclImpl[]) convert(PSAclImpl[].class, aclList);
         resp.getPSAclImpl().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, service); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, service); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, service); } catch (PSErrorResultsFault pef) { throw new com.percussion.webservices.systemdesign.ErrorResultsFaultMessage(pef.toString(), pef); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new com.percussion.webservices.systemdesign.LoadAclsResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#loadConfiguration(LoadConfigurationRequest)
    */
   public LoadConfigurationResponse loadConfiguration(
      LoadConfigurationRequest loadConfigurationRequest)
      throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage, com.percussion.webservices.systemdesign.UnknownConfigurationFaultMessage, com.percussion.webservices.systemdesign.LockFaultMessage
   {
      final String serviceName = "loadConfiguration";
      String session;
      try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
      String user = getRemoteUser().orElse(null);

      try
      {
         IPSSystemDesignWs service = PSSystemWsLocator.getSystemDesignWebservice();

         boolean lock = extractBooleanValue(
            loadConfigurationRequest.isLock(), false);
         boolean overrideLock = extractBooleanValue(
            loadConfigurationRequest.isOverrideLock(), false);

         com.percussion.services.system.data.PSMimeContentAdapter config =
            service
            .loadConfiguration(loadConfigurationRequest.getName(), lock,
               overrideLock, session, user);

         LoadConfigurationResponse response = new LoadConfigurationResponse();
         response.setPSMimeContentAdapter((PSMimeContentAdapter) convert(
            PSMimeContentAdapter.class, config));

         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSLockErrorException e)
      {
         try { handleLockError(e); } catch (PSLockFault lf) { throw new com.percussion.webservices.systemdesign.LockFaultMessage(lf.toString(), lf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (FileNotFoundException e)
      {
         logger.error(PSExceptionUtils.getMessageForLog(e));
         throw new com.percussion.webservices.systemdesign.UnknownConfigurationFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#loadRelationshipTypes(LoadRelationshipTypesRequest)
    */
   public LoadRelationshipTypesResponse loadRelationshipTypes(
      LoadRelationshipTypesRequest req) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorResultsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      String service = "loadRelationshipTypes";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs svce = PSSystemWsLocator.getSystemDesignWebservice();

         // convert List<Long> to List<IPSGuid>
         List<IPSGuid> guids = new ArrayList<>();
         List<Long> ids = req.getId();
         if (ids != null)
         {
            for (Long id : ids)
               guids.add(new PSDesignGuid(id.longValue()));
         }
         boolean lock = extractBooleanValue(req.isLock(), false);
         boolean overrideLock = extractBooleanValue(req.isOverrideLock(), false);

         List<com.percussion.design.objectstore.PSRelationshipConfig> configs =
            svce.loadRelationshipTypes(guids, lock, overrideLock, session, user);

         LoadRelationshipTypesResponse resp = new LoadRelationshipTypesResponse();
         PSRelationshipConfig[] converted = (PSRelationshipConfig[]) convert(PSRelationshipConfig[].class, configs);
         resp.getPSRelationshipConfig().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, service); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, service); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.systemdesign.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, service); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new LoadRelationshipTypesResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#loadSharedProperties(LoadSharedPropertiesRequest)
    */
   public LoadSharedPropertiesResponse loadSharedProperties(
      LoadSharedPropertiesRequest request) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorResultsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "loadSharedProperties";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service =
            PSSystemWsLocator.getSystemDesignWebservice();

         String[] names = request.getName() == null ? new String[0] : request.getName().toArray(new String[0]);
         boolean lock = extractBooleanValue(
            request.isLock(), false);
         boolean overrideLock = extractBooleanValue(
            request.isOverrideLock(), false);

         List<com.percussion.services.system.data.PSSharedProperty> properties = service.loadSharedProperties(
            names, lock, overrideLock,
            session, user);

         LoadSharedPropertiesResponse resp = new LoadSharedPropertiesResponse();
         PSSharedProperty[] converted = (PSSharedProperty[]) convert(PSSharedProperty[].class, properties);
         resp.getPSSharedProperty().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         PSErrorResultsFault err = (PSErrorResultsFault) convert(
            PSErrorResultsFault.class, e);
         err.setService(serviceName);
         throw new com.percussion.webservices.systemdesign.ErrorResultsFaultMessage(err.toString(), err);
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new LoadSharedPropertiesResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#releaseLocks(long[])
    */
   public void releaseLocks(ReleaseLocksRequest req) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage
   {
      final String serviceName = "releaseLocks";
      try
      {
         if (req == null)
            throw new IllegalArgumentException("releaseLockRequest cannot be null");

         List<Long> idList = req.getId();
         if (idList == null || idList.isEmpty())
            throw new IllegalArgumentException("releaseLockRequest cannot be empty");

         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service =
            PSSystemWsLocator.getSystemDesignWebservice();

         List<IPSGuid> ids = new ArrayList<>();
         for (Long id : idList)
            ids.add(new PSDesignGuid(id.longValue()));
         service.releaseLocks(ids, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
   }

   /* (non-Javadoc)
    * @see SystemDesign#getLockedSummaries()
    */
   public GetLockedSummariesResponse getLockedSummaries()
      throws com.percussion.webservices.systemdesign.ContractViolationFaultMessage
   {
      final String serviceName = "getLockedSummaries";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service = PSSystemWsLocator.getSystemDesignWebservice();

         List<com.percussion.services.catalog.data.PSObjectSummary> summaries = service.getLockedSummaries(session, user);

         GetLockedSummariesResponse resp = new GetLockedSummariesResponse();
         PSObjectSummary[] converted = (PSObjectSummary[]) convert(PSObjectSummary[].class, summaries);
         resp.getPSObjectSummary().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         // Map to ContractViolationFaultMessage to satisfy generated signature
         throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(e.toString(), e);
      }

      // will never get here
      return new GetLockedSummariesResponse();
   }

   /* (non-Javadoc)
    * @see SystemDesign#createLocks(CreateLocksRequest)
    */
   public void createLocks(CreateLocksRequest createLocksRequest)
      throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorsFaultMessage
   {
      final String serviceName = "createLocks";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service = PSSystemWsLocator
            .getSystemDesignWebservice();

         List<Long> idList = createLocksRequest.getId();
         List<IPSGuid> ids = new ArrayList<>();
         for (Long id : idList)
            ids.add(new PSDesignGuid(id.longValue()));
         boolean overrideLock = extractBooleanValue(createLocksRequest
            .isOverrideLock(), false);

         service.createLocks(ids, overrideLock, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.systemdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#isLocked(IsLockedRequest)
    */
   public IsLockedResponse isLocked(IsLockedRequest req) throws com.percussion.webservices.systemdesign.ContractViolationFaultMessage
   {
      final String serviceName = "isLocked";
      try
      {
         if (req == null || req.getId() == null || req.getId().isEmpty())
            throw new IllegalArgumentException("isLockedRequest may not be null or empty");

         List<IPSGuid> ids = new ArrayList<>();
         for (Long id : req.getId()) ids.add(new PSDesignGuid(id.longValue()));

         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service = PSSystemWsLocator.getSystemDesignWebservice();

         List<com.percussion.services.catalog.data.PSObjectSummary> summaries = service.isLocked(ids, user);

         IsLockedResponse resp = new IsLockedResponse();
         PSObjectSummary[] results = new PSObjectSummary[summaries.size()];
         for (int i = 0; i < summaries.size(); i++) {
            com.percussion.services.catalog.data.PSObjectSummary summary = summaries.get(i);
            results[i] = summary == null ? null : (PSObjectSummary) convert(PSObjectSummary.class, summary);
         }
         resp.getPSObjectSummary().addAll(Arrays.asList(results));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         // this should never happen
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      // will never get here
      return new IsLockedResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#saveAcls(SaveAclsRequest)
    */
   @SuppressWarnings(value={"unchecked"})
   public SaveAclsResponse saveAcls(SaveAclsRequest req)
      throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorResultsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "saveAcls";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         List<PSAclImpl> aclarr = req.getPSAclImpl();
         if (aclarr == null || aclarr.isEmpty())
            throw new IllegalArgumentException("PSAclImpl may not be null or empty");

         List<com.percussion.services.security.data.PSAclImpl> aclList =
            (List<com.percussion.services.security.data.PSAclImpl>) convert(List.class, aclarr);

         IPSSystemDesignWs webService = PSSystemWsLocator.getSystemDesignWebservice();
         boolean release = extractBooleanValue(req.isRelease(), false);

         List<PSUserAccessLevel> accessLevels = webService.saveAcls(aclList, release, session, user);

         SaveAclsResponse resp = new SaveAclsResponse();
         for (int i = 0; i < accessLevels.size() && i < aclarr.size(); i++)
         {
            SaveAclsResponse.Permissions perm = new SaveAclsResponse.Permissions();
            perm.setId(aclarr.get(i).getId());
            Set<PSPermissions> permsSet = accessLevels.get(i).getPermissions();
            for (PSPermissions p : permsSet)
               perm.getPermission().add(Integer.valueOf(p.getOrdinal()));
            resp.getPermissions().add(perm);
         }

         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         PSErrorResultsFault err = (PSErrorResultsFault) convert(PSErrorResultsFault.class, e);
         err.setService(serviceName);
         throw new com.percussion.webservices.systemdesign.ErrorResultsFaultMessage(err.toString(), err);
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new SaveAclsResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#saveConfiguration(SaveConfigurationRequest)
    */
   public void saveConfiguration(SaveConfigurationRequest request)
      throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage, com.percussion.webservices.systemdesign.LockFaultMessage
   {
      final String serviceName = "saveConfiguration";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service = PSSystemWsLocator.getSystemDesignWebservice();

         com.percussion.services.system.data.PSMimeContentAdapter config =
            (com.percussion.services.system.data.PSMimeContentAdapter) convert(
               com.percussion.services.system.data.PSMimeContentAdapter.class,
               request.getPSMimeContentAdapter());

         boolean release = extractBooleanValue(request.isRelease(), true);

         service.saveConfiguration(config, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSLockErrorException e)
      {
         try { handleLockError(e); } catch (PSLockFault lf) { throw new com.percussion.webservices.systemdesign.LockFaultMessage(lf.toString(), lf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (IOException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#saveRelationshipTypes(SaveRelationshipTypesRequest)
    */

   public void saveRelationshipTypes(SaveRelationshipTypesRequest request)
      throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "saveRelationshipTypes";

      try {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         List<com.percussion.design.objectstore.PSRelationshipConfig> configs =
            (List<com.percussion.design.objectstore.PSRelationshipConfig>) convert(List.class, request.getPSRelationshipConfig());

         IPSSystemDesignWs webService = PSSystemWsLocator.getSystemDesignWebservice();
         webService.saveRelationshipTypes(configs, extractBooleanValue(request.isRelease(), false), session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.systemdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see SystemDesign#saveSharedProperties(SaveSharedPropertiesRequest)
    */

   public void saveSharedProperties(SaveSharedPropertiesRequest request)
      throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "saveSharedProperties";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs webService = PSSystemWsLocator.getSystemDesignWebservice();

         IPSSystemService service = PSSystemServiceLocator.getSystemService();

         // convert from client to server object
         List<com.percussion.services.system.data.PSSharedProperty> properties =
            (List<com.percussion.services.system.data.PSSharedProperty>) convert(List.class, request.getPSSharedProperty());

         // get the correct guid / version for existing properties
         for (com.percussion.services.system.data.PSSharedProperty property : properties)
         {
            List<com.percussion.services.system.data.PSSharedProperty> existingProperties = service.findSharedPropertiesByName(property.getName());
            if (!existingProperties.isEmpty())
            {
               com.percussion.services.system.data.PSSharedProperty existingProperty = existingProperties.get(0);
               property.setGUID(existingProperty.getGUID());
               property.setVersion(existingProperty.getVersion());
            }
         }

         boolean release = extractBooleanValue(request.isRelease(), true);

         webService.saveSharedProperties(properties, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.systemdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /* (non-Javadoc)
    * @see SystemDesign#createGuids(CreateGuidsRequest)
    */
   public CreateGuidsResponse createGuids(CreateGuidsRequest request) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage
   {
      final String serviceName = "createGuids";
      try
      {
         if (request == null)
            throw new IllegalArgumentException("createGuidsRequest cannot be null");

         PSTypeEnum type = PSTypeEnum.valueOf(request.getType());
         if (type == null)
            throw new IllegalArgumentException("an unknown type was specified with the supplied request");

         int count = request.getCount() == null ? 1 : request.getCount();

         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }

         IPSSystemDesignWs service = PSSystemWsLocator.getSystemDesignWebservice();

         List<IPSGuid> guids = service.createGuids(type, count);
         CreateGuidsResponse resp = new CreateGuidsResponse();
         for (IPSGuid g : guids)
            resp.getId().add(new PSDesignGuid(g).getValue());
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new CreateGuidsResponse();
   }

   /* (non-Javadoc)
    * @see SystemDesign#createItemFilters(CreateItemFiltersRequest)
    */
   public CreateItemFiltersResponse createItemFilters(CreateItemFiltersRequest req) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "createItemFilters";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service = PSSystemWsLocator.getSystemDesignWebservice();

         List<com.percussion.services.filter.data.PSItemFilter> filters = service.createItemFilters(req.getName(), session, user);

         CreateItemFiltersResponse resp = new CreateItemFiltersResponse();
         PSItemFilter[] converted = (PSItemFilter[]) convert(PSItemFilter[].class, filters);
         resp.getPSItemFilter().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new CreateItemFiltersResponse();
   }

   /* (non-Javadoc)
    * @see SystemDesign#deleteItemFilters(DeleteItemFiltersRequest)
    */
   public void deleteItemFilters(
      DeleteItemFiltersRequest deleteItemFiltersRequest) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "deleteItemFilters";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service =
            PSSystemWsLocator.getSystemDesignWebservice();

         List<Long> idList = deleteItemFiltersRequest.getId();
         if (idList == null || idList.isEmpty())
            throw new IllegalArgumentException("Ids may not be null or empty");
         List<IPSGuid> ids = new ArrayList<>();
         for (Long id : idList)
            ids.add(new PSDesignGuid(id.longValue()));
         boolean ignoreDependencies = extractBooleanValue(
            deleteItemFiltersRequest.isIgnoreDependencies(), false);
         service.deleteItemFilters(ids, ignoreDependencies, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.systemdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /* (non-Javadoc)
    * @see SystemDesign#findItemFilters(FindItemFiltersRequest)
    */
   public FindItemFiltersResponse findItemFilters(
      FindItemFiltersRequest findItemFiltersRequest) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage
   {
      try {
         authenticate();
      } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }

      IPSSystemDesignWs service =
         PSSystemWsLocator.getSystemDesignWebservice();

      List<IPSCatalogSummary> summaries = service.findItemFilters(
         findItemFiltersRequest.getName());

      FindItemFiltersResponse resp = new FindItemFiltersResponse();
      PSObjectSummary[] converted = (PSObjectSummary[]) convert(PSObjectSummary[].class, summaries);
      resp.getPSObjectSummary().addAll(Arrays.asList(converted));
      return resp;
   }

   /* (non-Javadoc)
    * @see SystemDesign#loadItemFilters(LoadItemFiltersRequest)
    */
   public LoadItemFiltersResponse loadItemFilters(
      LoadItemFiltersRequest loadItemFiltersRequest) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorResultsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "loadItemFilters";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service =
            PSSystemWsLocator.getSystemDesignWebservice();

         List<Long> idList = loadItemFiltersRequest.getId();
         List<IPSGuid> ids = new ArrayList<>();
         if (idList == null || idList.isEmpty())
            return new LoadItemFiltersResponse();

         for (Long id : idList)
            ids.add(new PSDesignGuid(id.longValue()));
         boolean lock = extractBooleanValue(
            loadItemFiltersRequest.isLock(), false);
         boolean overrideLock = extractBooleanValue(
            loadItemFiltersRequest.isOverrideLock(), false);
         List<com.percussion.services.filter.data.PSItemFilter> filters = service.loadItemFilters(ids, lock, overrideLock,
            session, user);

         LoadItemFiltersResponse resp = new LoadItemFiltersResponse();
         PSItemFilter[] converted = (PSItemFilter[]) convert(PSItemFilter[].class, filters);
         resp.getPSItemFilter().addAll(Arrays.asList(converted));
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.systemdesign.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new LoadItemFiltersResponse();
   }

   /* (non-Javadoc)
    * @see SystemDesign#saveItemFilters(SaveItemFiltersRequest)
    */

   public void saveItemFilters(SaveItemFiltersRequest saveItemFiltersRequest) throws com.percussion.webservices.systemdesign.InvalidSessionFaultMessage, com.percussion.webservices.systemdesign.ContractViolationFaultMessage, com.percussion.webservices.systemdesign.ErrorsFaultMessage, com.percussion.webservices.systemdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "saveItemFilters";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.systemdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSSystemDesignWs service =
            PSSystemWsLocator.getSystemDesignWebservice();

         List<com.percussion.services.filter.data.PSItemFilter> filters = (List<com.percussion.services.filter.data.PSItemFilter>) convert(List.class, saveItemFiltersRequest.getPSItemFilter());
         boolean release = extractBooleanValue(
            saveItemFiltersRequest.isRelease(), true);
         service.saveItemFilters(filters, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.systemdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.systemdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }
}

