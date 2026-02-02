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
package com.percussion.webservices.contentdesign;

import com.percussion.cms.IPSConstants;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSBaseSOAPImpl;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.common.PSObjectSummary;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSAutoTranslation;
import com.percussion.webservices.content.PSContentEditorDefinition;
import com.percussion.webservices.content.PSContentTemplateDesc;
import com.percussion.webservices.content.PSContentType;
import com.percussion.webservices.content.PSContentWsLocator;
import com.percussion.webservices.content.PSKeyword;
import com.percussion.webservices.content.PSLocale;
import com.percussion.webservices.faults.PSLockFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSContractViolationFault;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;
import com.percussion.webservices.faults.PSNotAuthorizedFault;
import com.percussion.webservices.faults.PSErrorResultsFault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Server side implementations for web services defined in
 * <code>rhythmyxDesign.wsdl</code> for operations defined in the
 * <code>contentDesignSOAP</code> bindings.
 */
public class ContentDesignSOAPImpl extends PSBaseSOAPImpl
   implements ContentDesign
{
   /*
    * New binding: accept wrapper request with Name array element
    */
   public CreateContentTypesResponse createContentTypes(CreateContentTypesRequest request) throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage
   {
      String[] names = request == null || request.getName() == null ? new String[0] : request.getName().toArray(new String[0]);
      try {
         PSContentType[] arr = createContentTypes(names);
         CreateContentTypesResponse response = new CreateContentTypesResponse();
         response.getPSContentType().addAll(Arrays.asList(arr));
         return response;
      } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); } catch (RemoteException re) { throw new RuntimeException(re); }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#createContentTypes(String[])
    */
   public PSContentType[] createContentTypes(String[] names)
      throws RemoteException
   {
      final String serviceName = "createContentTypes";
      try
      {
         String session = authenticate();
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         return (PSContentType[]) convert(PSContentType[].class,
            service.createContentTypes(Arrays.asList(names), session, user));
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (PSInvalidSessionFault e)
      {
         // Map to runtime to maintain compatibility with older SOAP signatures
         throw new RuntimeException(e);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }

      // Should never get here - return empty array if we do
      return new PSContentType[]{};
   }

   /*
    * New binding: accept wrapper request with Name array element
    */
   public CreateKeywordsResponse createKeywords(CreateKeywordsRequest request) throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage
   {
      String[] names = request == null || request.getName() == null ? new String[0] : request.getName().toArray(new String[0]);
      try {
         PSKeyword[] arr = createKeywords(names);
         CreateKeywordsResponse response = new CreateKeywordsResponse();
         response.getPSKeyword().addAll(Arrays.asList(arr));
         return response;
      } catch (RuntimeException e) {
         Throwable cause = e.getCause();
         if (cause instanceof PSInvalidSessionFault) {
            PSInvalidSessionFault isf = (PSInvalidSessionFault) cause;
            throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(isf.toString(), isf);
         }
         if (cause instanceof PSContractViolationFault) {
            PSContractViolationFault cv = (PSContractViolationFault) cause;
            throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv);
         }
         if (cause instanceof PSErrorsException) {
            throw new RuntimeException(cause);
         }
         throw e;
      } catch (RemoteException re) { throw new RuntimeException(re); }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#createKeywords(String[])
    */
   public PSKeyword[] createKeywords(String[] names) throws RemoteException
   {
      final String serviceName = "createKeywords";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         return (PSKeyword[]) convert(PSKeyword[].class,
            service.createKeywords(Arrays.asList(names), session, user));
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }

      // Should never get here - return an empty array in case
      return new PSKeyword[]{};
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#createLocales(CreateLocalesRequest)
    */
   public CreateLocalesResponse createLocales(
      CreateLocalesRequest createLocalesRequest) throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "createLocales";
      try
      {
         String session = authenticate();
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         java.util.List<String> codesList = createLocalesRequest.getCode();
         if (codesList == null)
            throw new IllegalArgumentException("codes may not be null");
         java.util.List<String> namesList = createLocalesRequest.getLabel();
         if (namesList == null)
            throw new IllegalArgumentException("names may not be null");

         PSLocale[] locales = (PSLocale[]) convert(PSLocale[].class,
            service.createLocales(codesList, namesList,
               session, user));
         CreateLocalesResponse response = new CreateLocalesResponse();
         response.getPSLocale().addAll(Arrays.asList(locales));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (PSInvalidSessionFault e)
      {
         throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e);
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new CreateLocalesResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#deleteContentTypes(DeleteContentTypesRequest)
    */
   public void deleteContentTypes(
      DeleteContentTypesRequest deleteContentTypesRequest)
      throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorsFaultMessage
   {
      final String serviceName = "deleteContentTypes";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         java.util.List<Long> idList = deleteContentTypesRequest.getId();
         java.util.List<IPSGuid> ids = idList == null ? java.util.Collections.emptyList() :
             idList.stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.NODEDEF)).toList();

         Boolean ignore = deleteContentTypesRequest.isIgnoreDependencies();
         boolean ignoreDependencies = ignore != null && ignore;

         service.deleteContentTypes(ids, ignoreDependencies, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (com.percussion.webservices.faults.PSErrorsFault erf) { throw new com.percussion.webservices.contentdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#deleteKeywords(DeleteKeywordsRequest)
    */
   public void deleteKeywords(DeleteKeywordsRequest deleteKeywordsRequest)
      throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorsFaultMessage
   {
      final String serviceName = "deleteKeywords";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         List<IPSGuid> ids = deleteKeywordsRequest.getId() == null ? java.util.Collections.emptyList() : deleteKeywordsRequest.getId().stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.KEYWORD_DEF)).toList();
         Boolean ignore = deleteKeywordsRequest.isIgnoreDependencies();
         boolean ignoreDependencies = ignore != null && ignore;
         service.deleteKeywords(ids, ignoreDependencies, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (com.percussion.webservices.faults.PSErrorsFault erf) { throw new com.percussion.webservices.contentdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#deleteLocales(DeleteLocalesRequest)
    */
   public void deleteLocales(DeleteLocalesRequest deleteLocalesRequest)
      throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorsFaultMessage
   {
      final String serviceName = "deleteLocales";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         java.util.List<Long> idList = deleteLocalesRequest.getId();
         java.util.List<IPSGuid> ids = idList == null ? java.util.Collections.emptyList() : idList.stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.LOCALE)).toList();

         Boolean ignore = deleteLocalesRequest.isIgnoreDependencies();
         boolean ignoreDependencies = ignore != null && ignore;

         service.deleteLocales(ids, ignoreDependencies, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (com.percussion.webservices.faults.PSErrorsFault erf) { throw new com.percussion.webservices.contentdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#findContentTypes(FindContentTypesRequest)
    */
   public FindContentTypesResponse findContentTypes(
      FindContentTypesRequest findContentTypesRequest) throws com.percussion.webservices.contentdesign.InvalidSessionFaultMessage
   {
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }

         IPSContentDesignWs service = PSContentWsLocator.getContentDesignWebservice();

         List<IPSCatalogSummary> sums = service.findContentTypes(
            findContentTypesRequest.getName());

         PSObjectSummary[] converted = (PSObjectSummary[]) convert(PSObjectSummary[].class, sums);

         FindContentTypesResponse response = new FindContentTypesResponse();
         response.getPSObjectSummary().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "findContentTypes"); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
         FindContentTypesResponse response = new FindContentTypesResponse();
         return response;
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#findKeywords(FindKeywordsRequest)
    */
   public FindKeywordsResponse findKeywords(FindKeywordsRequest findKeywordsRequest)
      throws com.percussion.webservices.contentdesign.InvalidSessionFaultMessage
   {
      final String serviceName = "findKeywords";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }

         IPSContentDesignWs service = PSContentWsLocator.getContentDesignWebservice();

         List<IPSCatalogSummary> summaries = service.findKeywords(findKeywordsRequest.getName());

         PSObjectSummary[] converted = (PSObjectSummary[]) convert(PSObjectSummary[].class, summaries);
         FindKeywordsResponse response = new FindKeywordsResponse();
         response.getPSObjectSummary().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
         return new FindKeywordsResponse();
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#findLocales(FindLocalesRequest)
    */
   public FindLocalesResponse findLocales(FindLocalesRequest findLocalesRequest)
      throws com.percussion.webservices.contentdesign.InvalidSessionFaultMessage
   {
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }

         IPSContentDesignWs service = PSContentWsLocator.getContentDesignWebservice();

         List<IPSCatalogSummary> sums = service.findLocales(findLocalesRequest.getCode(),
            findLocalesRequest.getName());

         PSObjectSummary[] converted = (PSObjectSummary[]) convert(PSObjectSummary[].class, sums);
         FindLocalesResponse response = new FindLocalesResponse();
         response.getPSObjectSummary().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "findLocales"); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
         return new FindLocalesResponse();
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#loadContentTypes(LoadContentTypesRequest)
    */
   public LoadContentTypesResponse loadContentTypes(
      LoadContentTypesRequest loadContentTypesRequest) throws com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorResultsFaultMessage
   {
      final String serviceName = "loadContentTypes";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service = PSContentWsLocator.getContentDesignWebservice();

         List<IPSGuid> ids = loadContentTypesRequest.getId().stream()
            .map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.NODEDEF)).toList();
         boolean lock = loadContentTypesRequest.isLock() != null && loadContentTypesRequest.isLock();
         boolean overrideLock = loadContentTypesRequest.isOverrideLock() != null && loadContentTypesRequest.isOverrideLock();
         List<PSItemDefinition> defs = service.loadContentTypes(ids, lock, overrideLock, session, user);

         PSContentType[] converted = (PSContentType[]) convert(PSContentType[].class, defs);

         LoadContentTypesResponse response = new LoadContentTypesResponse();
         response.getPSContentType().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.contentdesign.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new LoadContentTypesResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#loadKeywords(LoadKeywordsRequest)
    */
   public LoadKeywordsResponse loadKeywords(LoadKeywordsRequest loadKeywordsRequest) throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorResultsFaultMessage
   {
      final String serviceName = "loadKeywords";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service = PSContentWsLocator.getContentDesignWebservice();

         List<IPSGuid> ids = loadKeywordsRequest.getId().stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.KEYWORD_DEF)).toList();
         boolean lock = extractBooleanValue(loadKeywordsRequest.isLock(), false);
         boolean overrideLock = extractBooleanValue(loadKeywordsRequest.isOverrideLock(), false);
         List<com.percussion.services.content.data.PSKeyword> keywords = service.loadKeywords(ids, lock, overrideLock, session, user);

         PSKeyword[] converted = (PSKeyword[]) convert(PSKeyword[].class, keywords);
         LoadKeywordsResponse response = new LoadKeywordsResponse();
         response.getPSKeyword().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.contentdesign.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new LoadKeywordsResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#loadLocales(LoadLocalesRequest)
    */
   public LoadLocalesResponse loadLocales(LoadLocalesRequest loadLocalesRequest) throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorResultsFaultMessage
   {
      final String serviceName = "loadLocales";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         List<IPSGuid> ids = loadLocalesRequest.getId().stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.LOCALE)).toList();
         boolean lock = loadLocalesRequest.isLock() != null && loadLocalesRequest.isLock();
         boolean overrideLock = loadLocalesRequest.isOverrideLock() != null && loadLocalesRequest.isOverrideLock();
         List<com.percussion.i18n.PSLocale> locales = service.loadLocales(ids, lock, overrideLock, session, user);

         PSLocale[] converted = (PSLocale[]) convert(PSLocale[].class, locales);
         LoadLocalesResponse response = new LoadLocalesResponse();
         response.getPSLocale().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorResultsException e)
      {
         handleErrorResultsException(e, serviceName);
      }

      // will never get here
      return new LoadLocalesResponse();
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#saveContentTypes(SaveContentTypesRequest)
    */
   @SuppressWarnings(value={"unchecked"})
   public void saveContentTypes(
      SaveContentTypesRequest saveContentTypesRequest) throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorsFaultMessage
   {
      final String serviceName = "saveContentTypes";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         List contentTypes = (List) convert(
            List.class,
            saveContentTypesRequest.getPSContentType());
         boolean release = saveContentTypesRequest.isRelease() == null ?
            Boolean.TRUE : saveContentTypesRequest.isRelease().booleanValue();

         service.saveContentTypes(contentTypes, release,
            session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (com.percussion.webservices.faults.PSErrorsFault erf) { throw new com.percussion.webservices.contentdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#saveKeywords(SaveKeywordsRequest)
    */
   @SuppressWarnings("unchecked")
   public void saveKeywords(SaveKeywordsRequest saveKeywordsRequest)
      throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorsFaultMessage
   {
      final String serviceName = "saveKeywords";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         List keywords = (List) convert(List.class,
            saveKeywordsRequest.getPSKeyword());
         boolean release = extractBooleanValue(
            saveKeywordsRequest.isRelease(), true);
         service.saveKeywords(keywords, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (com.percussion.webservices.faults.PSErrorsFault erf) { throw new com.percussion.webservices.contentdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#saveLocales(SaveLocalesRequest)
    */
   @SuppressWarnings(value={"unchecked"})
   public void saveLocales(SaveLocalesRequest saveLocalesRequest)
      throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorsFaultMessage
   {
      final String serviceName = "saveLocales";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         List locales = (List) convert(
            List.class,
            saveLocalesRequest.getPSLocale());
         boolean release = saveLocalesRequest.isRelease() == null ?
            Boolean.TRUE : saveLocalesRequest.isRelease().booleanValue();

         service.saveLocales(locales, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (com.percussion.webservices.faults.PSErrorsFault erf) { throw new com.percussion.webservices.contentdesign.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#loadSharedDefinition(LoadSharedDefinitionRequest)
    */
   public LoadSharedDefinitionResponse loadSharedDefinition(
      LoadSharedDefinitionRequest loadSharedDefinitionRequest)
      throws com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.LockFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "loadSharedDefinition";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         boolean lock = extractBooleanValue(
            loadSharedDefinitionRequest.isLock(), false);
         boolean overrideLock = extractBooleanValue(
            loadSharedDefinitionRequest.isOverrideLock(), false);

         PSContentEditorSharedDef sharedDef =
            service.loadContentEditorSharedDef(lock, overrideLock, session,
               user);

         PSContentEditorDefinition def = (PSContentEditorDefinition) convert(
            PSContentEditorDefinition.class, sharedDef);

         LoadSharedDefinitionResponse resp = new LoadSharedDefinitionResponse();
         resp.setPSContentEditorDefinition(def);
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSLockErrorException e)
      {
         throw (com.percussion.webservices.contentdesign.LockFaultMessage) convert(
               com.percussion.webservices.contentdesign.LockFaultMessage.class, e);
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#loadSystemDefinition(LoadSystemDefinitionRequest)
    */
   public LoadSystemDefinitionResponse loadSystemDefinition(
      LoadSystemDefinitionRequest loadSystemDefinitionRequest)
      throws com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.LockFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "loadSystemDefinition";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         boolean lock = extractBooleanValue(
            loadSystemDefinitionRequest.isLock(), false);
         boolean overrideLock = extractBooleanValue(
            loadSystemDefinitionRequest.isOverrideLock(), false);

         PSContentEditorSystemDef systemDef =
            service.loadContentEditorSystemDef(lock, overrideLock, session,
               user);

         PSContentEditorDefinition def = (PSContentEditorDefinition) convert(
            PSContentEditorDefinition.class, systemDef);

         LoadSystemDefinitionResponse resp = new LoadSystemDefinitionResponse();
         resp.setPSContentEditorDefinition(def);
         return resp;
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (PSLockErrorException e)
      {
         throw (com.percussion.webservices.contentdesign.LockFaultMessage) convert(
               com.percussion.webservices.contentdesign.LockFaultMessage.class, e);
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }

      // will never get here
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#saveSharedDefinition(SaveSharedDefinitionRequest)
    */
   public void saveSharedDefinition(
      SaveSharedDefinitionRequest saveSharedDefinitionRequest)
      throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.LockFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "saveSharedDefinition";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         PSContentEditorSharedDef def = (PSContentEditorSharedDef) convert(
            PSContentEditorSharedDef.class,
            saveSharedDefinitionRequest.getPSContentEditorDefinition());
         boolean release = extractBooleanValue(
            saveSharedDefinitionRequest.isRelease(), true);
         service.saveContentEditorSharedDef(def, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSLockErrorException e)
      {
         throw (com.percussion.webservices.contentdesign.LockFaultMessage) convert(
               com.percussion.webservices.contentdesign.LockFaultMessage.class, e);
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see ContentDesign#saveSystemDefinition(SaveSystemDefinitionRequest)
    */
   public void saveSystemDefinition(
      SaveSystemDefinitionRequest saveSystemDefinitionRequest)
      throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.LockFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "saveSystemDefinition";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         PSContentEditorSystemDef def = (PSContentEditorSystemDef) convert(
            PSContentEditorSystemDef.class,
            saveSystemDefinitionRequest.getPSContentEditorDefinition());
         boolean release = extractBooleanValue(
            saveSystemDefinitionRequest.isRelease(), true);
         service.saveContentEditorSystemDef(def, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (PSLockErrorException e)
      {
         throw (com.percussion.webservices.contentdesign.LockFaultMessage) convert(
               com.percussion.webservices.contentdesign.LockFaultMessage.class, e);
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
      catch (RuntimeException e)
      {
         handleRuntimeException(e, serviceName);
      }
   }

   /* (non-Javadoc)
    * @see ContentDesign#loadAssociatedTemplates(LoadAssociatedTemplatesRequest)
    */
   public LoadAssociatedTemplatesResponse loadAssociatedTemplates(
      LoadAssociatedTemplatesRequest loadAssociatedTemplatesRequest) throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorResultsFaultMessage
   {
      final String serviceName = "loadAssociatedTemplates";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         long contentTypeId = loadAssociatedTemplatesRequest.getContentTypeId();

         Boolean lockValue = loadAssociatedTemplatesRequest.isLock();
         boolean lock = lockValue != null && lockValue;
         Boolean overrideLockValue =
            loadAssociatedTemplatesRequest.isOverrideLock();
         boolean overrideLock = overrideLockValue != null && overrideLockValue;
         IPSGuid guid = null;
         if (contentTypeId != -1)
            guid = new PSGuid(PSTypeEnum.NODEDEF, contentTypeId);

         List<com.percussion.services.contentmgr.data.PSContentTemplateDesc> results = service.loadAssociatedTemplates(guid, lock,
            overrideLock, session, user);

         PSContentTemplateDesc[] converted = (PSContentTemplateDesc[]) convert(PSContentTemplateDesc[].class,
            results);

         LoadAssociatedTemplatesResponse response = new LoadAssociatedTemplatesResponse();
         response.getPSContentTemplateDesc().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.contentdesign.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // will never get here
      return new LoadAssociatedTemplatesResponse();
   }

   /* (non-Javadoc)
    * @see ContentDesign#saveAssociatedTemplates(SaveAssociatedTemplatesRequest)
    */
   public void saveAssociatedTemplates(
      SaveAssociatedTemplatesRequest saveAssociatedTemplatesRequest)
      throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage, com.percussion.webservices.contentdesign.ErrorsFaultMessage
   {
      final String serviceName = "saveContentTypes";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         long contentTypeId = saveAssociatedTemplatesRequest.getContentTypeId();
         java.util.List<Long> templateIds = saveAssociatedTemplatesRequest.getTemplateId();
         if (templateIds == null)
            templateIds = java.util.Collections.emptyList();

         java.util.List<IPSGuid> templateGuids = templateIds.stream().map(id -> PSGuidUtils.makeGuid(id, PSTypeEnum.TEMPLATE)).toList();

         boolean release = saveAssociatedTemplatesRequest.isRelease() == null ?
            Boolean.TRUE :
               saveAssociatedTemplatesRequest.isRelease().booleanValue();

         service.saveAssociatedTemplates(new PSGuid(PSTypeEnum.NODEDEF,
            contentTypeId), templateGuids, release,
               session, user);
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

   /* (non-Javadoc)
    * @see ContentDesign#loadTranslationSettings(LoadTranslationSettingsRequest)
    */
   public LoadTranslationSettingsResponse loadTranslationSettings(
      LoadTranslationSettingsRequest loadTranslationSettingsRequest)
      throws com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.LockFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "loadTranslationSettings";
      LoadTranslationSettingsResponse response = new LoadTranslationSettingsResponse();
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         Boolean lockValue = loadTranslationSettingsRequest.isLock();
         boolean lock = lockValue != null && lockValue;
         Boolean overrideLockValue =
            loadTranslationSettingsRequest.isOverrideLock();
         boolean overrideLock = overrideLockValue != null && overrideLockValue;
         List<com.percussion.services.content.data.PSAutoTranslation> ats = service.loadTranslationSettings(lock, overrideLock,
            session, user);

         PSAutoTranslation[] arr = (PSAutoTranslation[]) convert(PSAutoTranslation[].class, ats);
         response.getPSAutoTranslation().addAll(Arrays.asList(arr));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSLockErrorException e)
      {
         throw (com.percussion.webservices.contentdesign.LockFaultMessage) convert(
               com.percussion.webservices.contentdesign.LockFaultMessage.class, e);
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return new LoadTranslationSettingsResponse();
   }

   /* (non-Javadoc)
    * @see ContentDesign#saveTranslationSettings(SaveTranslationSettingsRequest)
    */
   @SuppressWarnings(value={"unchecked"})
   public void saveTranslationSettings(
      SaveTranslationSettingsRequest saveTranslationSettingsRequest)
      throws com.percussion.webservices.contentdesign.ContractViolationFaultMessage, com.percussion.webservices.contentdesign.InvalidSessionFaultMessage, com.percussion.webservices.contentdesign.LockFaultMessage, com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage
   {
      final String serviceName = "saveTranslationSettings";
      try
      {
         String session;
         try { session = authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.contentdesign.InvalidSessionFaultMessage(e.toString(), e); }
         String user = getRemoteUser().orElse(null);

         IPSContentDesignWs service =
            PSContentWsLocator.getContentDesignWebservice();

         List<com.percussion.services.content.data.PSAutoTranslation> ats;
         if (saveTranslationSettingsRequest.getPSAutoTranslation() == null)
         {
            ats = new ArrayList<>();
         }
         else
         {
            ats = (List<com.percussion.services.content.data.PSAutoTranslation>) convert(
               List.class,
               saveTranslationSettingsRequest.getPSAutoTranslation());
         }

         boolean release = saveTranslationSettingsRequest.isRelease() == null ?
            Boolean.TRUE :
               saveTranslationSettingsRequest.isRelease().booleanValue();

         service.saveTranslationSettings(ats, release, session, user);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.contentdesign.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSLockErrorException e)
      {
         throw (com.percussion.webservices.contentdesign.LockFaultMessage) convert(
               com.percussion.webservices.contentdesign.LockFaultMessage.class, e);
      }
      catch (RuntimeException e)
      {
         try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.contentdesign.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   private static final Logger log = LogManager.getLogger(IPSConstants.WEBSERVICES_LOG);
}
