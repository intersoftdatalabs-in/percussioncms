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
package com.percussion.webservices.assemblydesign;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSBaseSOAPImpl;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.assembly.PSAssemblyWsLocator;
import com.percussion.webservices.assembly.data.PSAssemblyTemplate;
import com.percussion.webservices.assembly.data.PSTemplateSlot;
import com.percussion.webservices.common.PSObjectSummary;
import com.percussion.webservices.faults.PSContractViolationFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSNotAuthorizedFault;
import com.percussion.webservices.faults.PSErrorsFault;
import java.rmi.RemoteException;

/**
 * Server side implementations for web services defined in <code>rhythmyxDesign.wsdl</code> for
 * operations defined in the <code>assemblyDesignSOAP</code> bindings.
 */
public class AssemblyDesignSOAPImpl extends PSBaseSOAPImpl
   implements AssemblyDesign
{
  /**
   * Default constructor; provided so the implicit default constructor has explicit Javadoc and
   * doclint does not warn about its use.
   */
  public AssemblyDesignSOAPImpl() {
    // SOAP implementations are stateless; the superclass handles wiring
  }
{
   /*
    * (non-Javadoc)
    *
    * @see AssemblyDesign#createAssemblyTemplates(String[])
    */
   public com.percussion.webservices.assemblydesign.CreateAssemblyTemplatesResponse createAssemblyTemplates(
      CreateAssemblyTemplatesRequest req) throws com.percussion.webservices.assemblydesign.InvalidSessionFaultMessage, com.percussion.webservices.assemblydesign.ContractViolationFaultMessage, com.percussion.webservices.assemblydesign.NotAuthorizedFaultMessage
   {
      com.percussion.webservices.assemblydesign.CreateAssemblyTemplatesResponse result = new com.percussion.webservices.assemblydesign.CreateAssemblyTemplatesResponse();
      final String serviceName = "createAssemblyTemplates";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.assemblydesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSAssemblyDesignWs service =
            PSAssemblyWsLocator.getAssemblyDesignWebservice();

         java.util.List<String> namesList = req.getName();
         if (namesList == null)
            throw new IllegalArgumentException("names may not be null");

         java.util.List<?> templates = service.createAssemblyTemplates(namesList, session, user);

         PSAssemblyTemplate[] converted = (PSAssemblyTemplate[]) convert(PSAssemblyTemplate[].class, templates);
         result.setCreateAssemblyTemplatesResponse(converted);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (RuntimeException e)
      {
         throw e;
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see AssemblyDesign#createSlots(String[])
    */
   public com.percussion.webservices.assemblydesign.CreateSlotsResponse createSlots(
      CreateSlotsRequest req) throws com.percussion.webservices.assemblydesign.InvalidSessionFaultMessage, com.percussion.webservices.assemblydesign.ContractViolationFaultMessage, com.percussion.webservices.assemblydesign.NotAuthorizedFaultMessage
   {
      com.percussion.webservices.assemblydesign.CreateSlotsResponse result = new com.percussion.webservices.assemblydesign.CreateSlotsResponse();
      final String serviceName = "createSlots";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.assemblydesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSAssemblyDesignWs service =
            PSAssemblyWsLocator.getAssemblyDesignWebservice();

         java.util.List<String> namesList = req.getName();
         if (namesList == null)
            throw new IllegalArgumentException("names may not be null");

         java.util.List<?> slots = service.createSlots(namesList, session, user);
         PSTemplateSlot[] converted = (PSTemplateSlot[]) convert(PSTemplateSlot[].class, slots);
         result.setCreateSlotsResponse(converted);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (RuntimeException e)
      {
         throw e;
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see AssemblyDesign#deleteAssemblyTemplates(DeleteAssemblyTemplatesRequest)
    */
   public void deleteAssemblyTemplates(
      DeleteAssemblyTemplatesRequest deleteAssemblyTemplatesRequest) throws com.percussion.webservices.assemblydesign.InvalidSessionFaultMessage, com.percussion.webservices.assemblydesign.ContractViolationFaultMessage, com.percussion.webservices.assemblydesign.NotAuthorizedFaultMessage, com.percussion.webservices.assemblydesign.ErrorsFaultMessage
   {
      final String serviceName = "deleteAssemblyTemplate";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.assemblydesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSAssemblyDesignWs service =
            PSAssemblyWsLocator.getAssemblyDesignWebservice();

         java.util.List<Long> idList = deleteAssemblyTemplatesRequest.getId();
         java.util.List<IPSGuid> ids = idList == null ? java.util.Collections.emptyList() :
             idList.stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.TEMPLATE)).toList();
         boolean ignoreDependencies = false; // default to false; request binding may not expose ignore flag
         service.deleteAssemblyTemplates(ids, ignoreDependencies, session,
            user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.assemblydesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (com.percussion.webservices.faults.PSErrorsFault erf) { throw new com.percussion.webservices.assemblydesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.assemblydesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see AssemblyDesign#deleteSlots(DeleteSlotsRequest)
    */
   public void deleteSlots(DeleteSlotsRequest deleteSlotsRequest) throws com.percussion.webservices.assemblydesign.InvalidSessionFaultMessage, com.percussion.webservices.assemblydesign.ContractViolationFaultMessage, com.percussion.webservices.assemblydesign.NotAuthorizedFaultMessage, com.percussion.webservices.assemblydesign.ErrorsFaultMessage
   {
      final String serviceName = "deleteSlots";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.assemblydesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSAssemblyDesignWs service =
            PSAssemblyWsLocator.getAssemblyDesignWebservice();

         java.util.List<Long> idList = deleteSlotsRequest.getId();
         java.util.List<IPSGuid> ids = idList == null ? java.util.Collections.emptyList() :
             idList.stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.SLOT)).toList();
         boolean ignoreDependencies = false; // default
         service.deleteSlots(ids, ignoreDependencies, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.assemblydesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (com.percussion.webservices.faults.PSErrorsFault erf) { throw new com.percussion.webservices.assemblydesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.assemblydesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see AssemblyDesign#findAssemblyTemplates(FindAssemblyTemplatesRequest)
    */
   public com.percussion.webservices.assemblydesign.FindAssemblyTemplatesResponse findAssemblyTemplates(
      FindAssemblyTemplatesRequest findAssemblyTemplatesRequest)
   {
      final String serviceName = "findAssemblyTemplates";
      com.percussion.webservices.assemblydesign.FindAssemblyTemplatesResponse result = new com.percussion.webservices.assemblydesign.FindAssemblyTemplatesResponse();
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

         IPSAssemblyDesignWs service =
            PSAssemblyWsLocator.getAssemblyDesignWebservice();

         // Call service with minimal parameters; optional filters may not be present in this binding
         java.util.List<?> summaries = service.findAssemblyTemplates(
            findAssemblyTemplatesRequest.getName(),
            findAssemblyTemplatesRequest.getContentType(),
            null, null, null, null,
            null);

         result.setFindAssemblyTemplatesResponse((PSObjectSummary[]) convert(PSObjectSummary[].class, summaries));
         return result;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see AssemblyDesign#findSlots(FindSlotsRequest)
    */
   public com.percussion.webservices.assemblydesign.FindSlotsResponse findSlots(FindSlotsRequest findSlotsRequest)
   {
      final String serviceName = "findSlots";
      com.percussion.webservices.assemblydesign.FindSlotsResponse result = new com.percussion.webservices.assemblydesign.FindSlotsResponse();
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

         IPSAssemblyDesignWs service =
            PSAssemblyWsLocator.getAssemblyDesignWebservice();

         PSDesignGuid associatedTemplateId = null;
         if (findSlotsRequest.getAssociatedTemplateId() != null)
            associatedTemplateId = new PSDesignGuid(
               findSlotsRequest.getAssociatedTemplateId());
         java.util.List<?> summaries = service.findSlots(findSlotsRequest.getName(),
            associatedTemplateId);

         result.setFindSlotsResponse((PSObjectSummary[]) convert(PSObjectSummary[].class, summaries));
         return result;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see AssemblyDesign#loadAssemblyTemplates(LoadAssemblyTemplatesRequest)
    */
   public com.percussion.webservices.assemblydesign.LoadAssemblyTemplatesResponse loadAssemblyTemplates(
      LoadAssemblyTemplatesRequest loadAssemblyTemplatesRequest)
   {
      final String serviceName = "loadAssemblyTemplates";
      com.percussion.webservices.assemblydesign.LoadAssemblyTemplatesResponse result = new com.percussion.webservices.assemblydesign.LoadAssemblyTemplatesResponse();
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }
         String user = getRemoteUser().orElse(null);

         IPSAssemblyDesignWs service =
            PSAssemblyWsLocator.getAssemblyDesignWebservice();

         java.util.List<Long> idList = loadAssemblyTemplatesRequest.getId();
         java.util.List<IPSGuid> ids = idList == null ? java.util.Collections.emptyList() :
             idList.stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.TEMPLATE)).toList();
         boolean lock = false; // default when binding does not expose lock flag
         boolean overrideLock = false; // default when binding does not expose override flag
         java.util.List<?> templates = service.loadAssemblyTemplates(ids, lock,
            overrideLock, session, user);

         PSAssemblyTemplate[] converted = (PSAssemblyTemplate[]) convert(PSAssemblyTemplate[].class, templates);
         result.setLoadAssemblyTemplatesResponse(converted);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (RuntimeException e)
      {
         throw e;
      }
      catch (PSErrorResultsException e)
      {
         // Map to runtime to avoid checked-fault mismatch with current service interface
         throw new RuntimeException(e);
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see AssemblyDesign#loadSlots(LoadSlotsRequest)
    */
   public com.percussion.webservices.assemblydesign.LoadSlotsResponse loadSlots(LoadSlotsRequest loadSlotsRequest)
   {
      final String serviceName = "loadSlots";
      com.percussion.webservices.assemblydesign.LoadSlotsResponse result = new com.percussion.webservices.assemblydesign.LoadSlotsResponse();
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }
         String user = getRemoteUser().orElse(null);

         IPSAssemblyDesignWs service =
            PSAssemblyWsLocator.getAssemblyDesignWebservice();

         java.util.List<Long> idList = loadSlotsRequest.getId();
         java.util.List<IPSGuid> ids = idList == null ? java.util.Collections.emptyList() :
             idList.stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.SLOT)).toList();
         boolean lock = false; // default when binding does not expose lock flag
         boolean overrideLock = false; // default when binding does not expose override flag
         java.util.List<?> slots = service.loadSlots(ids, lock, overrideLock,
            session, user);

         PSTemplateSlot[] converted = (PSTemplateSlot[]) convert(PSTemplateSlot[].class, slots);
         result.setLoadSlotsResponse(converted);
         return result;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (RuntimeException e)
      {
         throw e;
      }
      catch (PSErrorResultsException e)
      {
         throw new RuntimeException(e);
      }

      // will never get here
      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see AssemblyDesign#saveAssemblyTemplates(SaveAssemblyTemplatesRequest)
    */

   public void saveAssemblyTemplates(
      SaveAssemblyTemplatesRequest saveAssemblyTemplatesRequest)
   {
      final String serviceName = "saveAssemblyTemplates";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }
         String user = getRemoteUser().orElse(null);

         IPSAssemblyDesignWs service =
            PSAssemblyWsLocator.getAssemblyDesignWebservice();

         java.util.List<com.percussion.webservices.assembly.data.PSAssemblyTemplateWs> templates =
             (java.util.List<com.percussion.webservices.assembly.data.PSAssemblyTemplateWs>) convert(java.util.List.class,
                saveAssemblyTemplatesRequest.getPSAssemblyTemplate());
         Boolean release = Boolean.TRUE; // default when binding does not expose release
         service.saveAssemblyTemplates(templates, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorsException e)
      {
         throw new RuntimeException(e);
      }
      catch (RuntimeException e)
      {
         throw e;
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see AssemblyDesign#saveSlots(SaveSlotsRequest)
    */

   public void saveSlots(SaveSlotsRequest saveSlotsRequest)
   {
      final String serviceName = "saveSlots";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }
         String user = getRemoteUser().orElse(null);

         IPSAssemblyDesignWs service =
            PSAssemblyWsLocator.getAssemblyDesignWebservice();

         java.util.List<com.percussion.services.assembly.IPSTemplateSlot> slots =
             (java.util.List<com.percussion.services.assembly.IPSTemplateSlot>) convert(java.util.List.class,
            saveSlotsRequest.getPSTemplateSlot());
         Boolean release = Boolean.TRUE; // default when binding does not expose release
         service.saveSlots(slots, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (RuntimeException e)
      {
         throw e;
      }
   }
}
