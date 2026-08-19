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
// REFACTORED: CP-JAVA11, CP-SOAP
package com.percussion.webservices.content;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSItemChild;
import com.percussion.cms.objectstore.PSItemChildEntry;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.PSRelationshipProcessorProxy;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.search.objectstore.PSWSSearchRequest;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSSearchSummary;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.services.system.data.PSContentStatusHistory;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.utils.guid.IPSGuid;
import com.intsof.percussioncms.auditlog.codes.WebserviceErrorCodes;
import com.percussion.webservices.PSBaseSOAPImpl;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSInvalidStateException;
import com.percussion.webservices.PSUnknownChildException;
import com.percussion.webservices.PSUnknownContentTypeException;
import com.percussion.webservices.PSWebserviceErrors;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.faults.PSContractViolationFault;
import com.percussion.webservices.faults.PSErrorResultsFault;
import com.percussion.webservices.faults.PSErrorsFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSNotAuthorizedFault;
import com.percussion.webservices.faults.PSUnknownContentTypeFault;

import org.apache.commons.lang3.StringUtils;
import com.percussion.webservices.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jws.WebService;
import java.util.Iterator;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Server side implementations for content management web services defined in rhythmyx.wsdl
 * for operations defined in the contentSOAP bindings.
 *
 * <p>Modernized for Java 11 with enhanced type safety, Optional usage, Stream API,
 * and JAX-WS annotations for contemporary SOAP implementation.
 */
@WebService(endpointInterface = "com.percussion.webservices.content.Content")
public class ContentSOAPImpl extends PSBaseSOAPImpl implements Content {

    private static final Logger logger = LogManager.getLogger(ContentSOAPImpl.class);
    // Backwards-compatible alias for older code using 'log'
    private static final Logger log = logger;

    // Helper to authenticate and map checked PSInvalidSessionFault to runtime handler
    private boolean tryAuthenticate(String serviceName) {
        try {
            authenticate();
            return true;
        } catch (PSInvalidSessionFault e) {
            try {
                handleRuntimeException(new RuntimeException(e), serviceName);
            } catch (PSNotAuthorizedFault | RemoteException ex) {
                // Wrap checked faults as runtime exceptions to avoid changing callers' signatures
                throw new RuntimeException(ex);
            }
            return false;
        }
    }

    /**
     * Load translation settings and return a response wrapper.
     *
     * @return response with auto-translation configurations, never null
     */
    @Override
    public LoadTranslationSettingsResponse loadTranslationSettings()
        throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.NotAuthorizedFaultMessage {

        var serviceName = "loadTranslationSettings";
        logger.debug("Loading translation settings");

        LoadTranslationSettingsResponse response = new LoadTranslationSettingsResponse();

        try {
            try {
                authenticate();
            } catch (PSInvalidSessionFault e) {
                throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e);
            }

            var service = PSContentWsLocator.getContentWebservice();
            var translations = service.loadTranslationSettings();

            PSAutoTranslation[] converted = convert(PSAutoTranslation[].class, translations);
            response.getPSAutoTranslation().addAll(Arrays.asList(converted));
        } catch (IllegalArgumentException e) {
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
        } catch (RuntimeException e) {
            try {
                handleRuntimeException(e, serviceName);
            } catch (PSNotAuthorizedFault naf) {
                throw new com.percussion.webservices.content.NotAuthorizedFaultMessage(naf.toString(), naf);
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }

        return response;
    }

    /**
     * Load content types based on name filter with enhanced validation.
     *
     * @param loadContentTypesRequest the request containing optional name filter
     * @return array of content type summaries matching the criteria
     */
    @Override
    public LoadContentTypesResponse loadContentTypes(LoadContentTypesRequest loadContentTypesRequest) {

        var serviceName = "loadContentTypes";
        var requestName = Optional.ofNullable(loadContentTypesRequest)
            .map(LoadContentTypesRequest::getName)
            .orElse(null);

        logger.debug("Loading content types with name filter: {}", requestName);

        LoadContentTypesResponse response = new LoadContentTypesResponse();

        try {
            if (!tryAuthenticate(serviceName)) return response;
            var service = PSContentWsLocator.getContentWebservice();
            var summaries = service.loadContentTypes(requestName);

            PSContentTypeSummary[] converted = convert(PSContentTypeSummary[].class, summaries);
            response.getPSContentTypeSummary().addAll(Arrays.asList(converted));
        } catch (IllegalArgumentException e) {
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
        } catch (RuntimeException e) {
            try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (Exception ex) { throw new RuntimeException(ex); }
        }

        return response;
    }

    /**
     * Load keywords based on name filter and return a response wrapper.
     *
     * @param loadKeywordsRequest the request containing optional name filter
     * @return response containing keywords matching the criteria
     */
    @Override
    public LoadKeywordsResponse loadKeywords(LoadKeywordsRequest loadKeywordsRequest) {

        var serviceName = "loadKeywords";
        var requestName = Optional.ofNullable(loadKeywordsRequest)
            .map(LoadKeywordsRequest::getName)
            .orElse(null);

        logger.debug("Loading keywords with name filter: {}", requestName);

        LoadKeywordsResponse response = new LoadKeywordsResponse();

        try {
            if (!tryAuthenticate(serviceName)) return response;
            var service = PSContentWsLocator.getContentWebservice();
            var keywords = service.loadKeywords(requestName);

            PSKeyword[] converted = convert(PSKeyword[].class, keywords);
            response.getPSKeyword().addAll(Arrays.asList(converted));
        } catch (IllegalArgumentException e) {
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
        } catch (RuntimeException e) {
            try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (Exception ex) { throw new RuntimeException(ex); }
        }

        return response;
    }

    /**
     * Load locales based on code and name filters and return a response wrapper.
     *
     * @param loadLocalesRequest the request containing optional filters
     * @return response containing locales matching the criteria
     */
    @Override
    public LoadLocalesResponse loadLocales(LoadLocalesRequest loadLocalesRequest) {

        var serviceName = "loadLocales";
        var requestCode = Optional.ofNullable(loadLocalesRequest)
            .map(LoadLocalesRequest::getCode)
            .orElse(null);
        var requestName = Optional.ofNullable(loadLocalesRequest)
            .map(LoadLocalesRequest::getName)
            .orElse(null);

        logger.debug("Loading locales with code: {} and name: {}", requestCode, requestName);

        LoadLocalesResponse response = new LoadLocalesResponse();

        try {
            if (!tryAuthenticate(serviceName)) return response;
            var service = PSContentWsLocator.getContentWebservice();
            var locales = service.loadLocales(requestCode, requestName);

            PSLocale[] converted = convert(PSLocale[].class, locales);
            response.getPSLocale().addAll(Arrays.asList(converted));
        } catch (IllegalArgumentException e) {
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
        } catch (RuntimeException e) {
            try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (Exception ex) { throw new RuntimeException(ex); }
        }

        return response;
    }

    /**
     * Add content relations with enhanced GUID handling and validation.
     *
     * @param req the request containing relationship details
     * @return array of created relationships
     * @throws com.percussion.webservices.content.InvalidSessionFaultMessage if the session is invalid
     * @throws com.percussion.webservices.content.ContractViolationFaultMessage if the request contract is violated
     * @throws com.percussion.webservices.content.NotAuthorizedFaultMessage if the user is not authorized
     */
    @Override
    public AddContentRelationsResponse addContentRelations(AddContentRelationsRequest req)
        throws com.percussion.webservices.content.InvalidSessionFaultMessage,
               com.percussion.webservices.content.ContractViolationFaultMessage,
               com.percussion.webservices.content.NotAuthorizedFaultMessage
    {
        var serviceName = "addContentRelations";
        logger.debug("Adding content relations for owner ID: {}",
                Optional.ofNullable(req).map(AddContentRelationsRequest::getId).orElse(0L));

        AddContentRelationsResponse response = new AddContentRelationsResponse();

        try {
            try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

            // Validate and extract request data using modern patterns
            var ownerId = new PSLegacyGuid(req.getId());
            List<IPSGuid> relatedIds = req.getRelatedId().stream()
                    .map(id -> (IPSGuid) new PSLegacyGuid(id))
                    .collect(Collectors.toList());
            int index = Optional.ofNullable(req.getIndex())
                    .map(Integer::intValue)
                    .orElse(-1);

            // Create and save relationships
            var service = PSContentWsLocator.getContentWebservice();
            var relationships = service.addContentRelations(
                    ownerId,
                    relatedIds,
                    req.getSlot(),
                    req.getTemplate(),
                    req.getRelationshipConfig(),
                    index
            );

            logger.debug("Successfully created {} content relations", relationships.size());
            PSAaRelationship[] converted = convert(PSAaRelationship[].class, relationships);
            response.getPSAaRelationship().addAll(Arrays.asList(converted));
            return response;

        } catch (IllegalArgumentException e) {
            try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
        } catch (PSErrorException e) {
            // Map validation/server errors to ContractViolationFaultMessage for SOAP contract
            throw new com.percussion.webservices.content.ContractViolationFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
        } catch (RuntimeException e) {
            try { handleRuntimeException(e, serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (RemoteException re) { throw new RuntimeException(re); }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see Content#addFolder(AddFolderRequest)
     */
    public AddFolderResponse addFolder(AddFolderRequest request)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage,
             com.percussion.webservices.content.ContractViolationFaultMessage,
             com.percussion.webservices.content.ErrorsFaultMessage
   {
      var serviceName = "addFolder";
      logger.debug("Adding folder: {} at path: {}", request.getName(), request.getPath());

      try
      {
         try {
            authenticate();
         } catch (PSInvalidSessionFault e) {
            throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e);
         }

         // Enhanced validation using modern Optional patterns
         var folderName = Optional.ofNullable(request.getName())
            .filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new IllegalArgumentException("Folder name must not be null or empty"));

         var folderPath = Optional.ofNullable(request.getPath())
            .filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new IllegalArgumentException("Parent folder path must not be null or empty"));

         var service = PSContentWsLocator.getContentWebservice();
         var folder = service.addFolder(folderName, folderPath);
         var result = convert(PSFolder.class, folder);

         var response = new AddFolderResponse();
         response.setPSFolder(result);

         logger.debug("Successfully created folder: {}", folderName);
         return response;

      } catch (IllegalArgumentException e) {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      } catch (PSErrorException e) {
         throw new com.percussion.webservices.content.ErrorsFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
      } catch (RuntimeException e) {
         try {
            handleRuntimeException(e, serviceName);
         } catch (PSNotAuthorizedFault naf) {
            throw new RuntimeException(naf);
         } catch (RemoteException ex) {
            throw new RuntimeException(ex);
         }
      }

      return null; // Never reached
   }

   /**
    * Validates folder reference with enhanced null safety.
    *
    * @param ref the folder reference to validate
    * @throws IllegalArgumentException if validation fails
    */
   private void validateFolderRef(FolderRef ref) {
      Optional.ofNullable(ref)
         .orElseThrow(() -> new IllegalArgumentException("Folder reference must not be null"));

      var hasId = ref.getId() != null;
      var hasPath = StringUtils.isNotBlank(ref.getPath());

      if (hasId && hasPath) {
         throw new IllegalArgumentException(
               "Cannot specify both folder id and path. Either id or path must be null or empty.");
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#addFolderChildren(AddFolderChildrenRequest)
    */
   public void addFolderChildren(
      AddFolderChildrenRequest request)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage,
             com.percussion.webservices.content.ContractViolationFaultMessage,
             com.percussion.webservices.content.ErrorsFaultMessage
   {
      try {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

         validateFolderRef(request.getParent());
         if (request.getChildIds() == null || request.getChildIds().getId() == null || request.getChildIds().getId().isEmpty())
            throw new IllegalArgumentException("ChildIds must not be null or empty");

         // convert list of Longs to legacy GUIDs
         List<IPSGuid> childIds = PSWebserviceUtils
               .getLegacyGuidFromLong(request.getChildIds().getId());

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         if (request.getParent().getId() != null)
         {
            IPSGuid parentId = new PSLegacyGuid(request.getParent().getId());
            service.addFolderChildren(parentId, childIds);
         }
         else
         {
            service.addFolderChildren(request.getParent().getPath(), childIds);
         }
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "addFolderChildren"); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#addFolderTree(AddFolderTreeRequest)
    */
   public AddFolderTreeResponse addFolderTree(AddFolderTreeRequest request)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorResultsFaultMessage
   {
      String serviceName = "addFolderTree";
      try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

      AddFolderTreeResponse response = new AddFolderTreeResponse();

      try
      {
         if (StringUtils.isBlank(request.getPath()))
            throw new IllegalArgumentException(
               "The parent folder path must not be null or empty.");

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<com.percussion.cms.objectstore.PSFolder> folders =
            service.addFolderTree(request.getPath());

         PSFolder[] result;
         if (folders.isEmpty())
            result = new PSFolder[0];
         else
            result = (PSFolder[]) convert(PSFolder[].class, folders);

         response.getPSFolder().addAll(Arrays.asList(result));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.content.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(e);
      }

      return null;
   }

   /* (non-Javadoc)
    * @see Content#checkinItems(CheckinItemsRequest)
    */
   public void checkinItems(CheckinItemsRequest checkinItemsRequest)
   {
      String serviceName = "checkinItems";
      try
      {
         if (!tryAuthenticate(serviceName)) return;

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         service.checkinItems(PSWebserviceUtils.getLegacyGuidFromLong(
            checkinItemsRequest.getId()), checkinItemsRequest.getComment());
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new RuntimeException(erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /* (non-Javadoc)
    * @see Content#checkoutItems(CheckoutItemsRequest)
    */
   public void checkoutItems(CheckoutItemsRequest checkoutItemsRequest)
   {
      String serviceName = "checkoutItems";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         service.checkoutItems(PSWebserviceUtils.getLegacyGuidFromLong(
            checkoutItemsRequest.getId()), checkoutItemsRequest.getComment());
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new RuntimeException(erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /* (non-Javadoc)
    * @see Content#createChildEntries(CreateChildEntriesRequest)
    */
   public CreateChildEntriesResponse createChildEntries(
      CreateChildEntriesRequest createChildEntriesRequest)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.UnknownChildFaultMessage
   {
      String serviceName = "createChildEntries";

      CreateChildEntriesResponse response = new CreateChildEntriesResponse();

      try
      {
         if (!tryAuthenticate(serviceName)) return response;

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         String childName = createChildEntriesRequest.getName();
         int count = createChildEntriesRequest.getCount() == null ? 1 :
            createChildEntriesRequest.getCount();
         PSLegacyGuid id = new PSLegacyGuid(createChildEntriesRequest.getId());

         PSChildEntry[] converted = (PSChildEntry[]) convert(PSChildEntry[].class,
            service.createChildEntries(id, childName, count));

         response.getPSChildEntry().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSUnknownChildException e)
      {
         var fault = convert(com.percussion.webservices.faults.PSUnknownChildFault.class, e);
         throw new com.percussion.webservices.content.UnknownChildFaultMessage(fault.toString(), fault);
      }
      catch (PSInvalidStateException e)
      {
         var fault = convert(com.percussion.webservices.faults.PSContractViolationFault.class, e);
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(fault.toString(), fault);
      }
      catch (PSErrorException e)
      {
         logger.error(PSExceptionUtils.getMessageForLog(e));
         logger.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new RuntimeException(new RemoteException(PSExceptionUtils.getMessageForLog(e)));
      }

      // no-op list returned by getters; nothing to add for empty result
      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#createItems(CreateItemsRequest)
    */
   public CreateItemsResponse createItems(CreateItemsRequest createItemsRequest)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.UnknownContentTypeFaultMessage, com.percussion.webservices.content.NotAuthorizedFaultMessage
   {
      String serviceName = "createItems";
      var contentType = Optional.ofNullable(createItemsRequest)
         .map(CreateItemsRequest::getContentType)
         .orElseThrow(() -> new IllegalArgumentException("Content type is required"));
      var count = Optional.ofNullable(createItemsRequest.getCount()).orElse(1);

      logger.debug("Creating {} items of type: {}", count, contentType);

      CreateItemsResponse response = new CreateItemsResponse();

      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }
         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<PSCoreItem> items = service.createItems(contentType,
            count.intValue());

         PSItem[] converted = convertItems(items, null, null, null);
         response.getPSItem().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSUnknownContentTypeException e)
      {
         PSUnknownContentTypeFault fault = new PSUnknownContentTypeFault();
         fault.setCode(WebserviceErrorCodes.UNKNOWN_CONTENT_TYPE.numericCode());
         fault.setErrorMessage(PSWebserviceErrors.createErrorMessage(WebserviceErrorCodes.UNKNOWN_CONTENT_TYPE, serviceName, createItemsRequest.getContentType(), e.getLocalizedMessage()));
         fault.setStack(ExceptionUtils.getFullStackTrace(e));
         throw new com.percussion.webservices.content.UnknownContentTypeFaultMessage(e.getLocalizedMessage(), fault);
      }
      catch (PSErrorsException e)
      {
         // Map to SOAP contract violation
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(e.getLocalizedMessage(), e);
      }
      catch (PSErrorException e)
      {
         // Unexpected server error -> convert to contract violation for SOAP clients
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
      }

      // no-op for empty result; list accessors return live lists
      return response;
   }

   /**
    * Convert the supplied items. Can't use the list to array converter
    * because the returned list may contain server items and we can't
    * register the core and server item at the same time. This also converts
    * the items id into a client side legacy GUID with the revision set to -1.
    *
    * @param items the list of items to convert, assumed not <code>null</code>,
    *    may be empty.
    * @param fieldNames a list of names for all parent fields to be returned
    *    with the converted items, may be <code>null</code> to return all
    *    fields or empty to return no fields.
    * @param childNames a list of names for all child fieldsets to be returned
    *    with the converted items, may be <code>null</code> to return all
    *    children or empty to return no children.
    * @param slotNames a list of names for all slots for which to return the
    *    related content with the converted items, may be <code>null</code> to
    *    return all related content or empty to return no related content.
    *
    * @return an array with all items converted, never <code>null</code>, may
    *    be empty. If any of the items contains related items, then the related
    *    items will not contain their related items (if there is any), they
    *    will also not contain binary data (if there is any).
    */
   private PSItem[] convertItems(List<PSCoreItem> items,
      List<String> fieldNames, List<String> childNames, List<String> slotNames)
   {
      return items.stream()
         .map(item -> {
            var convertedItem = convert(PSItem.class, item);
            filterFields(convertedItem, fieldNames);
            filterChildren(convertedItem, childNames);
            filterSlots(convertedItem, slotNames);
            return convertedItem;
         })
         .toArray(PSItem[]::new);
   }

   /**
    * Filter the supplied item for all requested fields.
    *
    * @param item the item to filter, assumed not <code>null</code>.
    * @param fieldNames a list of field names for which to filter the item,
    *    may be <code>null</code> to skip the filter or empty to filter all
    *    fields.
    */
   private void filterFields(PSItem item, List<String> fieldNames)
   {
      Optional.ofNullable(fieldNames).ifPresentOrElse(
         names -> {
            if (names.isEmpty()) {
               item.setFields(null);
            } else {
               PSItem.Fields filtered = new PSItem.Fields();
               if (item.getFields() != null && item.getFields().getPSField() != null) {
                  item.getFields().getPSField().stream()
                      .filter(field -> names.contains(field.getName()))
                      .forEach(filtered.getPSField()::add);
               }
               item.setFields(filtered);
            }
         },
         () -> { /* No filtering needed */ }
      );
   }

   /**
    * Filter the supplied item for all requested children.
    *
    * @param item the item to filter, assumed not <code>null</code>.
    * @param childNames a list of child names for which to filter the item,
    *    may be <code>null</code> to skip the filter or empty to filter all
    *    children.
    */
   private void filterChildren(PSItem item, List<String> childNames)
   {
      Optional.ofNullable(childNames).ifPresentOrElse(
         names -> {
            if (names.isEmpty()) {
               item.getChildren().clear();
            } else {
               List<PSItem.Children> filteredChildren = item.getChildren().stream()
                  .filter(child -> names.contains(child.getName()))
                  .collect(Collectors.toList());
               item.getChildren().clear();
               item.getChildren().addAll(filteredChildren);
            }
         },
         () -> { /* No filtering needed */ }
      );
   }

   /**
    * Filter the supplied item for all requested slots.
    *
    * @param item the item to filter, assumed not <code>null</code>.
    * @param slotNames a list of slot names for which to filter the item,
    *    may be <code>null</code> or empty to skip the filter.
    */
   private void filterSlots(PSItem item, List<String> slotNames)
   {
      Optional.ofNullable(slotNames).ifPresentOrElse(
         names -> {
            if (names.isEmpty()) {
               item.getSlots().clear();
            } else {
               List<PSItem.Slots> filteredSlots = item.getSlots().stream()
                  .filter(slot -> names.contains(slot.getName()))
                  .collect(Collectors.toList());
               item.getSlots().clear();
               item.getSlots().addAll(filteredSlots);
            }
         },
         () -> { /* No filtering needed */ }
      );
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#deleteChildEntries(DeleteChildEntriesRequest)
    */
   public void deleteChildEntries(
      DeleteChildEntriesRequest deleteChildEntriesRequest)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage,
             com.percussion.webservices.content.ContractViolationFaultMessage,
             com.percussion.webservices.content.ErrorsFaultMessage,
             com.percussion.webservices.content.UnknownChildFaultMessage
   {
      String serviceName = "deleteChildEntries";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         IPSGuid id = new PSLegacyGuid(deleteChildEntriesRequest.getId());
         String name = deleteChildEntriesRequest.getName();
         List<IPSGuid> childIds = new ArrayList<IPSGuid>();
         for (long childid : deleteChildEntriesRequest.getChildId())
         {
            childIds.add(new PSLegacyGuid(childid));
         }

         service.deleteChildEntries(id, name, childIds);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSUnknownChildException e)
      {
         var fault = convert(com.percussion.webservices.faults.PSUnknownChildFault.class, e);
         throw new com.percussion.webservices.content.UnknownChildFaultMessage(fault.toString(), fault);
      }
      catch (PSInvalidStateException e)
      {
         var fault = convert(com.percussion.webservices.faults.PSContractViolationFault.class, e);
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(fault.toString(), fault);
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.content.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#deleteContentRelations(DeleteContentRelationsRequest)
    */
   public void deleteContentRelations(DeleteContentRelationsRequest request)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage,
             com.percussion.webservices.content.ContractViolationFaultMessage,
             com.percussion.webservices.content.ErrorsFaultMessage,
             com.percussion.webservices.content.NotAuthorizedFaultMessage
   {
      String serviceName = "deleteContentRelations";
      try
      {
         if (!tryAuthenticate(serviceName)) return;

         List<IPSGuid> ids = null;
         if (request != null && request.getId() != null && !request.getId().isEmpty()) {
             ids = PSWebserviceUtils.getLegacyGuidFromLong(request.getId());
         }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         service.deleteContentRelations(ids);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.content.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#deleteFolders(DeleteFoldersRequest)
    */
   public void deleteFolders(DeleteFoldersRequest request)
   {
      String serviceName = "deleteFolders";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

         // get data from request
         List<IPSGuid> ids = PSWebserviceUtils.getLegacyGuidFromLong(request.getId().stream().mapToLong(Long::longValue).toArray());
         boolean isPurgeItem = request.isPurgItems() == null ? false : request
               .isPurgItems().booleanValue();

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         service.deleteFolders(ids, isPurgeItem);
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new RuntimeException(erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
   }

   /**
    * Converts the specified Guid values to a list of Guids.
    *
    * @param longIds a list of Guid values, it may not be <code>null</code> or
    *    empty.
    * @param type the type of the to be created Guid, assumed not
    *    <code>null</code>.
    *
    * @return the converted Guids, never <code>null</code> or empty.
    */
   List<IPSGuid> getGuidFromLong(long[] longIds, PSTypeEnum type)
   {
      return Arrays.stream(Optional.ofNullable(longIds)
            .filter(ids -> ids.length > 0)
            .orElseThrow(() -> new IllegalArgumentException("longIds must not be null or empty")))
         .mapToObj(id -> new PSGuid(type, id))
         .collect(Collectors.toList());
   }

   List<IPSGuid> getGuidFromLong(List<Long> longIds, PSTypeEnum type)
   {
      List<Long> ids = Optional.ofNullable(longIds)
            .filter(l -> !l.isEmpty())
            .orElseThrow(() -> new IllegalArgumentException("longIds must not be null or empty"));
      return ids.stream()
            .map(id -> new PSGuid(type, id.longValue()))
            .collect(Collectors.toList());
   }

   /*
    * New binding: accept wrapper request with Id array element
    */
   public void deleteItems(DeleteItemsRequest request) throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorsFaultMessage
   {
      long[] ids = request == null || request.getId() == null ? null : request.getId().stream().mapToLong(Long::longValue).toArray();
      try {
         deleteItems(ids);
      } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.content.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#deleteItems(long[])
    */
   public void deleteItems(long[] deleteItemsRequest) throws RemoteException,
      PSInvalidSessionFault, PSErrorsFault, PSContractViolationFault
   {
      String serviceName = "deleteItems";
      try
      {
         authenticate();

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<IPSGuid> ids = PSWebserviceUtils.getLegacyGuidFromLong(deleteItemsRequest);

         service.deleteItems(ids);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, serviceName);
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#findChildItems(FindChildItemsRequest)
    */
   public FindChildItemsResponse findChildItems(
      FindChildItemsRequest request)
   {
      var serviceName = "findChildItems";
      logger.debug("Finding child items for owner id: {}", Optional.ofNullable(request).map(FindChildItemsRequest::getId).orElse(0L));

      FindChildItemsResponse response = new FindChildItemsResponse();

      try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

      try
      {
         PSRelationshipFilter filter;
         PSLegacyGuid ownerId;
         if (request.getPSAaRelationshipFilter() != null)
         {
            request.getPSAaRelationshipFilter().setOwner(request.getId());
            filter = getRelationshipFilter(request.getPSAaRelationshipFilter());
            // the revision of the owner may have been modified if it was -1
            // get the owner id from the filter
            ownerId = new PSLegacyGuid(filter.getOwner());
         }
         else
         {
            filter = getRelationshipFilter(null);
            ownerId = new PSLegacyGuid(request.getId());
         }


         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         List<com.percussion.services.content.data.PSItemSummary> children =
            service.findDependents(ownerId, filter, request.isLoadOperations());

         PSItemSummary[] converted = getItemSummaries(children);
         response.getPSItemSummary().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (Exception e)
      {
         logger.error(PSExceptionUtils.getMessageForLog(e));
         logger.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      return response; // fallback
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#findFolderChildren(FindFolderChildrenRequest)
    */
   public FindFolderChildrenResponse findFolderChildren( FindFolderChildrenRequest request) throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage
   {
      var serviceName = "findFolderChildren";
      logger.debug("Finding folder children for folder: {}", Optional.ofNullable(request).map(FindFolderChildrenRequest::getFolder).orElse(null));

      FindFolderChildrenResponse response = new FindFolderChildrenResponse();

      try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

      try
      {
         validateFolderRef(request.getFolder());

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         List<com.percussion.services.content.data.PSItemSummary> children;
         if (request.getFolder().getId() != null )
         {
            IPSGuid parentId = new PSLegacyGuid(request.getFolder().getId());
            children = service.findFolderChildren(parentId, request
                  .isLoadOperations());
         }
         else
         {
            children = service.findFolderChildren(
                  request.getFolder().getPath(), request.isLoadOperations());
         }
         PSItemSummary[] converted = getItemSummaries(children);
         response.getPSItemSummary().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "findFolderChildren"); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      return null;
   }

   /**
    * Converts the specified item summaries from server to client format.
    *
    * @param srcList the to be converted objects, assumed not <code>null</code>.
    *
    * @return the converted object, never <code>null</code>, may be empty.
    */
   private PSItemSummary[] getItemSummaries(
      List<com.percussion.services.content.data.PSItemSummary> srcList)
   {
      return convert(PSItemSummary[].class, srcList);
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#findFolderPath(FindFolderPathRequest)
    */
   public FindFolderPathResponse findFolderPath(FindFolderPathRequest request)
   {
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }
         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         IPSGuid id = new PSLegacyGuid(request.getId());
         String[] paths = service.findFolderPaths(id);

         FindFolderPathResponse response = new FindFolderPathResponse();
         FindFolderPathResponse.Paths pathsObj = new FindFolderPathResponse.Paths();
         pathsObj.getPath().addAll(Arrays.asList(paths));
         response.setPaths(pathsObj);
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "findFolderPath"); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorException e)
      {
         logger.error(PSExceptionUtils.getMessageForLog(e));
         logger.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#findItems(FindItemsRequest)
    */
   public FindItemsResponse findItems(FindItemsRequest findItemsRequest)
   {
      String serviceName = "findItems";
      FindItemsResponse response = new FindItemsResponse();
      try
      {
         if (!tryAuthenticate(serviceName)) return response;

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         PSWSSearchRequest search = (PSWSSearchRequest) convert(
            PSWSSearchRequest.class, findItemsRequest.getPSSearch());
         boolean loadOperations = extractBooleanValue(
            findItemsRequest.isLoadOperations(), false);

         List<PSSearchSummary> results = service.findItems(search,
            loadOperations);

         PSSearchResults[] converted = (PSSearchResults[]) convert(PSSearchResults[].class, results);
         response.getPSSearchResults().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new RuntimeException(new RemoteException(PSExceptionUtils.getMessageForLog(e)));
      }

      // no-op for empty search results; use list accessor to add results if any
      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#findParentItems(FindParentItemsRequest)
    */
   public FindParentItemsResponse findParentItems(
      FindParentItemsRequest request)
   {
      try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

      String serviceName = "findParentItems";
      FindParentItemsResponse response = new FindParentItemsResponse();

      try
      {
         PSRelationshipFilter filter = getRelationshipFilter(
               request.getPSAaRelationshipFilter());
         PSLegacyGuid dependentId = new PSLegacyGuid(request.getId());

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<com.percussion.services.content.data.PSItemSummary> parents =
            service.findOwners(dependentId, filter, request.isLoadOperations());

         PSItemSummary[] converted = getItemSummaries(parents);
         response.getPSItemSummary().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         try { handleRuntimeException(new RuntimeException(e), serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#findPathIds(FindPathIdsRequest)
    */
   public FindPathIdsResponse findPathIds(FindPathIdsRequest request)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage
   {
      try {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         List<IPSGuid> ids = service.findPathIds(request.getPath());
         long[] idArr = PSWebserviceUtils.getLongsFromGuids(ids);
         FindPathIdsResponse response = new FindPathIdsResponse();
         FindPathIdsResponse.Ids idsBlock = new FindPathIdsResponse.Ids();
         for (long l : idArr) {
            idsBlock.getId().add(l);
         }
         response.setIds(idsBlock);
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "findPathIds"); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
      }
      return null;
   }

   /*
    * New binding: accept wrapper request with Id array element
    */
   public FindRevisionsResponse findRevisions(FindRevisionsRequest request)
   {
      long[] ids = (request == null || request.getId() == null) ? new long[0] : request.getId().stream().mapToLong(Long::longValue).toArray();
      return findRevisions(ids);
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#findRevisions(long[])
    */
   public FindRevisionsResponse findRevisions(long[] findRevisionsRequest)
   {
      String serviceName = "findRevisions";
      FindRevisionsResponse response = new FindRevisionsResponse();
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<IPSGuid> guids = PSWebserviceUtils.getLegacyGuidFromLong(
            findRevisionsRequest);
         List<com.percussion.services.content.data.PSRevisions> revisionsList =
            service.findRevisions(guids);

         com.percussion.webservices.content.PSRevisions[] results =
            new com.percussion.webservices.content
            .PSRevisions[revisionsList.size()];

         for (int i = 0; i < results.length; i++)
         {
            com.percussion.services.content.data.PSRevisions revisions =
               revisionsList.get(i);
            PSComponentSummary sum = revisions.getSummary();
            int contentId = sum.getContentId();
            int editRev = sum.getEditLocator().getRevision();
            int curRev = sum.getCurrentLocator().getRevision();

            List<PSContentStatusHistory> revList = revisions.getRevisions();
            PSRevision[] revArr = new PSRevision[revList.size()];
            for (int j = 0; j < revArr.length; j++)
            {
               PSContentStatusHistory hist = revList.get(j);
               int rev = hist.getRevision();
               PSLegacyGuid guid = new PSLegacyGuid(contentId, rev);
               Calendar time = Calendar.getInstance();
               time.setTime(hist.getEventTime());

               PSRevision psr = new PSRevision();
               psr.setId(guid.longValue());
               psr.setRevision(rev);
               psr.setIsCurrentRevision(rev == curRev);
               psr.setIsEditRevision(rev == editRev);
               try {
                   javax.xml.datatype.XMLGregorianCalendar xmlTime = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar((java.util.GregorianCalendar) time);
                   psr.setCreationDate(xmlTime);
               } catch (javax.xml.datatype.DatatypeConfigurationException dce) {
                   // fallback: ignore date if conversion fails
               }
               psr.setCreator(hist.getActor());
               psr.setComment(hist.getTransitionComment());
               revArr[j] = psr;
            }

            PSRevisions prs = new PSRevisions();
            prs.setEditRevision(editRev);
            prs.setCurrentRevision(curRev);
            PSRevisions.Revisions rblock = new PSRevisions.Revisions();
            rblock.getPSRevision().addAll(Arrays.asList(revArr));
            prs.setRevisions(rblock);
            results[i] = prs;
         }

         response.getPSRevisions().addAll(Arrays.asList(results));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorException e)
      {
         try { handleRuntimeException(new RuntimeException(e), serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#getAssemblyUrls(GetAssemblyUrlsRequest)
    */
   public GetAssemblyUrlsResponse getAssemblyUrls(GetAssemblyUrlsRequest request)
         throws com.percussion.webservices.content.InvalidSessionFaultMessage,
                com.percussion.webservices.content.ContractViolationFaultMessage
   {
      String serviceName = "getAssemblyUrls";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<IPSGuid> guids = PSWebserviceUtils.getLegacyGuidFromLong(request.getId());
         List<String> urls = service.getAssemblyUrls(guids,
               request.getTemplate(), request.getContext(),
               request.getItemFilter(), request.getSite(),
               request.getFolderPath());

         GetAssemblyUrlsResponse response = new GetAssemblyUrlsResponse();
         GetAssemblyUrlsResponse.Urls urlsBlock = new GetAssemblyUrlsResponse.Urls();
         urls.forEach(urlsBlock.getUrl()::add);
         response.setUrls(urlsBlock);
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
      }

      return null;
   }

   /* (non-Javadoc)
    * @see Content#loadChildEntries(LoadChildEntriesRequest)
    */
   public LoadChildEntriesResponse loadChildEntries(
      LoadChildEntriesRequest loadChildEntriesRequest)
   {
      String serviceName = "loadChildEntries";
      LoadChildEntriesResponse response = new LoadChildEntriesResponse();
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         String childName = loadChildEntriesRequest.getName();
         PSLegacyGuid id = new PSLegacyGuid(loadChildEntriesRequest.getId());
         boolean includeBinary = extractBooleanValue(
            loadChildEntriesRequest.isIncludeBinaries(), false);
         boolean attachBinaries = extractBooleanValue(
            loadChildEntriesRequest.isAttachBinaries(), false);

         List<PSItemChildEntry> childEntries = service.loadChildEntries(id,
            childName, includeBinary);

         if (includeBinary && attachBinaries)
         {
            for (PSItemChildEntry childEntry : childEntries)
               addAttachments(childEntry.getAllFields());
         }

         PSChildEntry[] result = (PSChildEntry[]) convert(PSChildEntry[].class, childEntries);
         response.getPSChildEntry().addAll(Arrays.asList(result));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSUnknownChildException e)
      {
         throw new RuntimeException(e);
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new RuntimeException(erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         try { handleRuntimeException(new RuntimeException(e), serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#loadContentRelations(LoadContentRelationsRequest)
    */
   public LoadContentRelationsResponse loadContentRelations(
      LoadContentRelationsRequest req)
   {
      String serviceName = "loadContentRelations";
      LoadContentRelationsResponse response = new LoadContentRelationsResponse();
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

         PSRelationshipFilter filter = getRelationshipFilter(
               req.getPSAaRelationshipFilter());

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         List<com.percussion.cms.objectstore.PSAaRelationship> relationships =
            service.loadContentRelations(filter, req.isLoadReferenceInfo());

         // convert the saved relationships
         PSAaRelationship[] result = (PSAaRelationship[]) convert(
               PSAaRelationship[].class, relationships);

         response.getPSAaRelationship().addAll(Arrays.asList(result));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         try { handleRuntimeException(new RuntimeException(e), serviceName); } catch (PSNotAuthorizedFault naf) { throw new RuntimeException(naf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return null;
   }

   /**
    * Converts the specified AA Relationship Filter to
    * {@link PSRelationshipFilter}.
    * <p>
    * Note, the owner revision will be <code>-1</code> if the
    * isLimitToOwnerRevisions of the source filter is <code>false</code>;
    * otherwise, the owner revision will be the Edit (or Tip) revision if the
    * item is checked out by the current user; otherwise the owner
    * revision is the current revision of the owner item.
    *
    * @param src the to be converted AA Filter, may be <code>null</code>.
    * @return the converted filter, never <code>null</code>.
    *
    * @throws PSErrorException if any of properties in the source filter
    *    is invalid.
    */
   private PSRelationshipFilter getRelationshipFilter(
      PSAaRelationshipFilter src)
      throws PSErrorException
   {
      PSRelationshipFilter filter = PSWebserviceUtils.getRelationshipFilter(
            src);

      if (src == null)
      {
         filter.setCategory(PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
         return filter;
      }

      // set slot id property
      if (! StringUtils.isBlank(src.getSlot()))
      {
         IPSTemplateSlot slot =
            (IPSTemplateSlot) PSWebserviceUtils.getSlotOrTemplateFromName(
               src.getSlot(), true);
         filter.setProperty(IPSHtmlParameters.SYS_SLOTID, String.valueOf(
               slot.getGUID().longValue()));
      }

      // set template id property
      if (! StringUtils.isBlank(src.getTemplate()))
      {
         IPSAssemblyTemplate template =
            (IPSAssemblyTemplate) PSWebserviceUtils.getSlotOrTemplateFromName(
               src.getTemplate(), false);
         filter.setProperty(IPSHtmlParameters.SYS_VARIANTID, String.valueOf(
               template.getGUID().longValue()));
      }

      // set site id property
      if (! StringUtils.isBlank(src.getSite()))
      {
         IPSSiteManager sitemgr = PSSiteManagerLocator.getSiteManager();
         IPSSite site;
         try
         {
            site = sitemgr.loadSite(src.getSite());
         }
         catch (PSNotFoundException e)
         {
            throw new IllegalArgumentException(e); // cannot find site.
         }
         filter.setProperty(IPSHtmlParameters.SYS_SITEID,
               String.valueOf(site.getGUID().longValue()));
      }

      // set folder id property
      if (! StringUtils.isBlank(src.getFolderPath()))
      {
         try
         {
            PSRelationshipProcessor processor =
               PSWebserviceUtils.getRelationshipProcessor();
            int id = processor.getIdByPath(
                  PSRelationshipProcessorProxy.RELATIONSHIP_COMPTYPE,
                  src.getFolderPath(),
                  PSRelationshipConfig.TYPE_FOLDER_CONTENT);
            filter.setProperty(IPSHtmlParameters.SYS_FOLDERID,
                  String.valueOf(id));
         }
         catch (PSCmsException e)
         {
            logger.error(PSExceptionUtils.getMessageForLog(e));
            logger.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new IllegalArgumentException(e);
         }
      }

      return filter;
   }


   /*
    * (non-Javadoc)
    *
    * @see Content#loadFolders(LoadFoldersRequest)
    */
   public LoadFoldersResponse loadFolders(LoadFoldersRequest request)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorResultsFaultMessage
   {
      String serviceName = "loadFolders";

      try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

      LoadFoldersResponse response = new LoadFoldersResponse();

      try
      {
         if (request.getId() != null && !request.getId().isEmpty() &&
            request.getPath() != null && !request.getPath().isEmpty())
            throw new IllegalArgumentException("Cannot load folders by both " +
               "ids and paths. Either ids or paths must be null or empty.");

         List<com.percussion.cms.objectstore.PSFolder> ats = null;
         List<IPSGuid> ids = null;
         if (request.getId() != null && !request.getId().isEmpty())
            ids = PSWebserviceUtils.getLegacyGuidFromLong(request.getId());

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         if (ids != null)
            ats = service.loadFolders(ids);
         else
            ats = service.loadFolders(request.getPath() == null ? new String[0] : request.getPath().toArray(new String[0]));

         PSFolder[] result = (PSFolder[]) convert(PSFolder[].class, ats);

         response.getPSFolder().addAll(Arrays.asList(result));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.content.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      // no-op for empty result; list accessor will be empty
      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#loadItems(LoadItemsRequest)
    */

   public LoadItemsResponse loadItems(LoadItemsRequest loadItemsRequest)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorResultsFaultMessage
   {
      String serviceName = "loadItems";
      LoadItemsResponse response = new LoadItemsResponse();
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<IPSGuid> ids = PSWebserviceUtils.getLegacyGuidFromLong(
            loadItemsRequest.getId());

         List<String> fieldNames = null;
         if (loadItemsRequest.getFieldName() != null &&
            !loadItemsRequest.getFieldName().isEmpty())
            fieldNames = loadItemsRequest.getFieldName();

         boolean includeBinary = extractBooleanValue(
            loadItemsRequest.isIncludeBinary(), false);
         boolean attachBinaries = extractBooleanValue(
            loadItemsRequest.isAttachBinaries(), false);

         boolean includeChildren = extractBooleanValue(
            loadItemsRequest.isIncludeChildren(), false);
         List<String> childNames = null;
         if (includeChildren && loadItemsRequest.getChildName() != null && !loadItemsRequest.getChildName().isEmpty())
            childNames = loadItemsRequest.getChildName();
         else if (!includeChildren)
            childNames = new ArrayList<String>();

         boolean includeRelated = extractBooleanValue(
            loadItemsRequest.isIncludeRelated(), false);
         List<String> slotNames = null;
         if (includeRelated && loadItemsRequest.getSlotName() != null && !loadItemsRequest.getSlotName().isEmpty())
            slotNames = loadItemsRequest.getSlotName();
         else if (!includeRelated)
            slotNames = new ArrayList<String>();

         boolean includeFolderPath = extractBooleanValue(
            loadItemsRequest.isIncludeFolderPath(), false);

         // always load both AA relationships and the related (or dependent)
         // items together; otherwise the returned items may fail to be
         // converted by {@link PSRelatedItemConverter}
         List<PSCoreItem> items = service.loadItems(ids, includeBinary,
            includeChildren, includeRelated, includeFolderPath, includeRelated);

         if (includeBinary && attachBinaries)
            attachBinaryFields(items);

         PSItem[] resultArray = convertItems(items, fieldNames, childNames, slotNames);
         response.getPSItem().addAll(Arrays.asList(resultArray));
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.content.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#moveFolderChildren(MoveFolderChildrenRequest)
    */
   public void moveFolderChildren(MoveFolderChildrenRequest request)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorsFaultMessage
   {
      String serviceName = "moveFolderChildren";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }
         // validating, cannot specified both id & path for source/target
         validateFolderRef(request.getSource());
         validateFolderRef(request.getTarget());

         PSLegacyGuid sourceId = request.getSource().getId() == null ? null
               : new PSLegacyGuid(request.getSource().getId());
         PSLegacyGuid targetId = request.getTarget().getId() == null ? null
               : new PSLegacyGuid(request.getTarget().getId());
         String sourcePath = request.getSource().getPath();
         String targetPath = request.getTarget().getPath();

         if (sourceId != null && (!StringUtils.isBlank(sourcePath)))
            throw new IllegalArgumentException(
               "Cannot specified both source id and source path.");
         if (targetId != null && (!StringUtils.isBlank(targetPath)))
            throw new IllegalArgumentException(
               "Cannot specified both target id and target path.");

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         List<IPSGuid> childIds = null;
         if (request.getChildId() != null && !request.getChildId().isEmpty())
            childIds = PSWebserviceUtils.getLegacyGuidFromLong(request.getChildId());
         if (sourceId != null)
            service.moveFolderChildren(sourceId, targetId, childIds);
         else
            service.moveFolderChildren(sourcePath, targetPath, childIds);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#newCopies(NewCopiesRequest)
    */
   public NewCopiesResponse newCopies(NewCopiesRequest newCopiesRequest)
   {
      String serviceName = "newCopies";
      NewCopiesResponse response = new NewCopiesResponse();

      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<IPSGuid> ids = PSWebserviceUtils.getLegacyGuidFromLong(
            newCopiesRequest.getIds().getId());

         List<String> paths = getPaths(newCopiesRequest.getPaths().getPath().toArray(new String[0]));

         String relationshipType = newCopiesRequest.getType();

         boolean enableRevisions = extractBooleanValue(
            newCopiesRequest.isEnableRevisions(), false);

         List<PSCoreItem> items = service.newCopies(ids, paths,
            relationshipType, enableRevisions);

         PSItem[] converted = convertItems(items, null, null, null);
         response.getPSItem().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new RuntimeException(erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      return response;
   }

   /**
    * Creates a list of {@link String} from the specified array of
    * {@link String}.
    *
    * @param paths the specified array of string; must not be <code>null</code>
    *    or empty.
    *
    * @return the created list, never <code>null</code> or empty.
    *
    * @throw IllegalArgumentException if paths is <code>null</code> or empty.
    */
   private List<String> getPaths(String[] paths)
   {
      return Arrays.stream(Optional.ofNullable(paths)
            .filter(p -> p.length > 0)
            .orElseThrow(() -> new IllegalArgumentException("paths must not be null or empty")))
         .collect(Collectors.toList());
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#newPromotableVersions(NewPromotableVersionsRequest)
    */
   public NewPromotableVersionsResponse newPromotableVersions(
      NewPromotableVersionsRequest newPromotableVersionsRequest)
   {
      String serviceName = "newPromotableVersions";
      NewPromotableVersionsResponse response = new NewPromotableVersionsResponse();

      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<IPSGuid> ids = PSWebserviceUtils.getLegacyGuidFromLong(
            newPromotableVersionsRequest.getIds().getId());

         List<String> paths = getPaths(newPromotableVersionsRequest.getPaths().getPath().toArray(new String[0]));

         String relationshipType = newPromotableVersionsRequest.getType();

         boolean enableRevisions = extractBooleanValue(
            newPromotableVersionsRequest.isEnableRevisions(), false);

         List<PSCoreItem> items = service.newPromotableVersions(ids, paths,
            relationshipType, enableRevisions);

         PSItem[] converted = convertItems(items, null, null, null);
         response.getPSItem().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new RuntimeException(erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#newTranslations(NewTranslationsRequest)
    */

   public NewTranslationsResponse newTranslations(NewTranslationsRequest newTranslationsRequest) throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorResultsFaultMessage
   {
      String serviceName = "newTranslations";
      NewTranslationsResponse response = new NewTranslationsResponse();
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }
         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<IPSGuid> ids = newTranslationsRequest.getIds() == null ? java.util.Collections.emptyList() :
            newTranslationsRequest.getIds().getId().stream().map(id -> PSGuidUtils.makeGuid(id.longValue(), PSTypeEnum.ITEM)).toList();

         List<com.percussion.services.content.data.PSAutoTranslation> autoTranslations = null;
         if (newTranslationsRequest.getAutoTranslations() != null)
            autoTranslations = (List<com.percussion.services.content.data.PSAutoTranslation>) convert(List.class,
               newTranslationsRequest.getAutoTranslations());

         String relationshipType = newTranslationsRequest.getType();

         boolean enableRevisions = extractBooleanValue(
            newTranslationsRequest.isEnableRevisions(), false);

         List<PSCoreItem> items = service.newTranslations(ids, autoTranslations,
            relationshipType, enableRevisions);

         PSItem[] converted = convertItems(items, null, null, null);
         response.getPSItem().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.content.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }

      return response;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#prepareForEdit(long[])
    */
   public PrepareForEditResponse prepareForEdit(PrepareForEditRequest request)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorResultsFaultMessage
   {
      String serviceName = "prepareForEdit";
      PrepareForEditResponse response = new PrepareForEditResponse();
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

         List<IPSGuid> idList = new ArrayList<IPSGuid>();
         if (request != null && request.getId() != null && !request.getId().isEmpty()) {
            for (long id : request.getId())
               idList.add(new PSLegacyGuid(id));
         }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         List<com.percussion.services.content.data.PSItemStatus> status =
            service.prepareForEdit(idList);

         PSItemStatus[] converted = (PSItemStatus[]) convert(PSItemStatus[].class, status);
         response.getPSItemStatus().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.content.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
      }
      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#promoteRevisions(PromoteRevisionsRequest)
    */
   public void promoteRevisions(PromoteRevisionsRequest promoteRevisionsRequest)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorsFaultMessage
   {
      String serviceName = "promoteRevisions";
      try {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

         List<IPSGuid> ids = null;
         if (promoteRevisionsRequest != null && !promoteRevisionsRequest.getId().isEmpty()) {
            ids = PSWebserviceUtils.getLegacyGuidFromLong(promoteRevisionsRequest.getId());
         }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         service.promoteRevisions(ids);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.content.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#releaseFromEdit(ReleaseFromEditRequest)
    */

   public void releaseFromEdit(ReleaseFromEditRequest req)
   {
      String serviceName = "releaseFromEdit";
      try
      {
         if (!tryAuthenticate(serviceName)) return;

         List<com.percussion.services.content.data.PSItemStatus> status = (List<com.percussion.services.content.data.PSItemStatus>) convert(List.class,
               req.getPSItemStatus());

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         service.releaseFromEdit(status, req.isCheckInOnly());
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new RuntimeException(erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#removeFolderChildren(RemoveFolderChildrenRequest)
    */
   public void removeFolderChildren(RemoveFolderChildrenRequest request)
   {
      String serviceName = "removeFolderChildren";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new RuntimeException(e); }
         validateFolderRef(request.getParent());

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         List<IPSGuid> childIds = null;
         if (request.getChildIds() != null && request.getChildIds().getValue() != null &&
               !request.getChildIds().getValue().getId().isEmpty())
            childIds = PSWebserviceUtils.getLegacyGuidFromLong(request.getChildIds().getValue().getId());
         boolean purgeItem = false;
         if (request.isPurgeItems() != null)
            purgeItem = request.isPurgeItems().booleanValue();
         if (request.getParent().getId() != null)
         {
            PSLegacyGuid parentId = new PSLegacyGuid(request.getParent()
                  .getId());
            service.removeFolderChildren(parentId, childIds, purgeItem);
         }
         else
         {
            service.removeFolderChildren(request.getParent().getPath(),
                  childIds, purgeItem);
         }
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new RuntimeException(erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#reorderChildEntries(ReorderChildEntriesRequest)
    */
   public void reorderChildEntries(
      ReorderChildEntriesRequest reorderChildEntriesRequest)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorsFaultMessage, com.percussion.webservices.content.UnknownChildFaultMessage
   {
      String serviceName = "reorderChildEntries";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }
         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         IPSGuid id = new PSLegacyGuid(reorderChildEntriesRequest.getId());
         String name = reorderChildEntriesRequest.getName();
         List<IPSGuid> childIds = new ArrayList<IPSGuid>();
         for (Long childid : reorderChildEntriesRequest.getChildId())
         {
            childIds.add(new PSLegacyGuid(childid));
         }

         service.reorderChildEntries(id, name, childIds);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSUnknownChildException e)
      {
         var fault = convert(com.percussion.webservices.faults.PSUnknownChildFault.class, e);
         throw new com.percussion.webservices.content.UnknownChildFaultMessage(fault.toString(), fault);
      }
      catch (PSInvalidStateException e)
      {
         var fault = convert(com.percussion.webservices.faults.PSContractViolationFault.class, e);
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(fault.toString(), fault);
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.content.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#reorderContentRelations(ReorderContentRelationsRequest)
    */
   public void reorderContentRelations(ReorderContentRelationsRequest req)
   {
      try
      {
         if (req.getId() == null || req.getId().isEmpty())
            throw new IllegalArgumentException("ids must not be null or empty.");

         if (!tryAuthenticate("reorderContentRelations")) return;

         // get data from request
         List<IPSGuid> relatedIds = getGuidFromLong(req.getId(),
            PSTypeEnum.RELATIONSHIP);

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         int index = (req.getIndex() == null) ? -1 : req.getIndex().intValue();
         service.reorderContentRelations(relatedIds, index);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, "reorderContentRelations"); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorException e)
      {
         throw new RuntimeException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#saveChildEntries(SaveChildEntriesRequest)
    */

   public void saveChildEntries(SaveChildEntriesRequest saveChildEntriesRequest)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorsFaultMessage, com.percussion.webservices.content.UnknownChildFaultMessage
   {
      String serviceName = "saveChildEntries";
      try
      {
         if (!tryAuthenticate(serviceName)) return;

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         IPSGuid id = new PSLegacyGuid(saveChildEntriesRequest.getId());
         String name = saveChildEntriesRequest.getName();
         List<PSItemChildEntry> entries = (List<PSItemChildEntry>)
            convert(List.class, saveChildEntriesRequest.getPSChildEntry());

         service.saveChildEntries(id, name, entries);
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSUnknownChildException e)
      {
         var fault = convert(com.percussion.webservices.faults.PSUnknownChildFault.class, e);
         throw new com.percussion.webservices.content.UnknownChildFaultMessage(fault.toString(), fault);
      }
      catch (PSInvalidStateException e)
      {
         var fault = convert(com.percussion.webservices.faults.PSContractViolationFault.class, e);
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(fault.toString(), fault);
      }
      catch (PSErrorsException e)
      {
         try { handleErrorsException(e, serviceName); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.content.ErrorsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (PSErrorException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new com.percussion.webservices.content.ContractViolationFaultMessage(PSExceptionUtils.getMessageForLog(e), e);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#saveContentRelations(data.PSAaRelationship[])
    */

   public void saveContentRelations(
      PSAaRelationship[] relationships) throws RemoteException,
      PSInvalidSessionFault, PSErrorsFault, PSContractViolationFault,
      PSNotAuthorizedFault
   {
      String serviceName = "saveContentRelations";
      authenticate();

      try
      {
         List<com.percussion.cms.objectstore.PSAaRelationship> rels =
            (List<com.percussion.cms.objectstore.PSAaRelationship>) convert(
               List.class, relationships);

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         service.saveContentRelations(rels);
      }
      catch (IllegalArgumentException e)
      {
         handleInvalidContract(e, serviceName);
      }
      catch (PSErrorsException e)
      {
         handleErrorsException(e, serviceName);
      }
      catch (Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new RemoteException(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /*
    * New binding: accept wrapper request with PSAaRelationship array element
    */
   public void saveContentRelations(SaveContentRelationsRequest request)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorsFaultMessage, com.percussion.webservices.content.NotAuthorizedFaultMessage
   {
      PSAaRelationship[] relationships = request == null || request.getPSAaRelationship() == null ? null : request.getPSAaRelationship().toArray(new PSAaRelationship[0]);
      try {
         saveContentRelations(relationships);
      } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); } catch (PSErrorsFault erf) { throw new com.percussion.webservices.content.ErrorsFaultMessage(erf.toString(), erf); } catch (PSNotAuthorizedFault naf) { throw new com.percussion.webservices.content.NotAuthorizedFaultMessage(naf.toString(), naf); } catch (RemoteException re) { throw new RuntimeException(re); }
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#saveFolders(data.PSFolder[])
    */
   public SaveFoldersResponse saveFolders(SaveFoldersRequest request)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorResultsFaultMessage
   {
      PSFolder[] folders = request == null || request.getPSFolder() == null ? null : request.getPSFolder().toArray(new PSFolder[0]);
      return saveFolders(folders);
   }


   public SaveFoldersResponse saveFolders(PSFolder[] folders)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorResultsFaultMessage
   {
      String serviceName = "saveFolders";
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

         // get data from request
         List<com.percussion.cms.objectstore.PSFolder> folderList =
            (List<com.percussion.cms.objectstore.PSFolder>) convert(
               List.class,
               folders);

         IPSContentWs service = PSContentWsLocator.getContentWebservice();
         List<IPSGuid> ids = service.saveFolders(folderList);

         SaveFoldersResponse response = new SaveFoldersResponse();
         SaveFoldersResponse.Ids idsBlock = new SaveFoldersResponse.Ids();
         for (long l : PSWebserviceUtils.getLongsFromGuids(ids)) idsBlock.getId().add(l);
         response.setIds(idsBlock);
         return response;
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.content.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }

      return null;
   }

   /*
    * (non-Javadoc)
    *
    * @see Content#saveItems(SaveItemsRequest)
    */

   public SaveItemsResponse saveItems(SaveItemsRequest saveItemsRequest)
      throws com.percussion.webservices.content.InvalidSessionFaultMessage, com.percussion.webservices.content.ContractViolationFaultMessage, com.percussion.webservices.content.ErrorResultsFaultMessage
   {
      String serviceName = "saveItems";

      List<PSPurgableTempFile> tempFiles =
                                    new ArrayList<PSPurgableTempFile>();
      try
      {
         try { authenticate(); } catch (PSInvalidSessionFault e) { throw new com.percussion.webservices.content.InvalidSessionFaultMessage(e.toString(), e); }

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<PSCoreItem> items = (List<PSCoreItem>) convert(List.class,
            saveItemsRequest.getPSItem());
         try { processAttachedFields(items, tempFiles); } catch (RemoteException re) { throw new RuntimeException(re); }

         boolean enableRevisions = extractBooleanValue(
            saveItemsRequest.isEnableRevisions(), false);
         boolean checkin = extractBooleanValue(
            saveItemsRequest.isCheckin(), false);

         List<IPSGuid> ids = service.saveItems(items, enableRevisions, checkin);

         SaveItemsResponse response = new SaveItemsResponse();
         SaveItemsResponse.Ids idsBlock = new SaveItemsResponse.Ids();
         for (Long l : PSGuidUtils.toLongArray(ids)) idsBlock.getId().add(l);
         response.setIds(idsBlock);
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new com.percussion.webservices.content.ContractViolationFaultMessage(cv.toString(), cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new com.percussion.webservices.content.ErrorResultsFaultMessage(erf.toString(), erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }
      finally
      {
         for(PSPurgableTempFile file:tempFiles)
         {
            file.release();
         }
      }

      return null;
   }

   /**
    * First retrieves all attachments from the current message. If there are
    * attachments then all parent and child binary fields are walked to match up
    * its attachment id with the attachment. If we find a match, the field value
    * is set with teh attachment input stream.
    *
    * @param items all items to map the message attachments to, assumed not
    * <code>null</code>, may be empty.
    * @param tempFiles list of purgable files to store so that we can release
    * them later assumed not <code>null</code>, may be empty.
    * @throws RemoteException for any error mapping the attachments.
    */
   private void processAttachedFields(List<PSCoreItem> items,
                                      List<PSPurgableTempFile> tempFiles)
      throws RemoteException
   {
      // Attachment handling not yet ported to JAX-WS. No-op for now.
      logger.debug("processAttachedFields: attachment handling is a no-op in JAX-WS migration stub.");
      return;
   }

   /**
    * Walks the supplied field list and sets the supplied attachment as binary
    * value if a match is found.
    *
    * @param attachment the attachment to set, assumed not <code>null</code>.
    * @param fields the fields to walk, assumed not <code>null</code>, may be
    *    empty.
    * @return <code>true</code> if we found a matching field for the
    *    supplied attachment, <code>false</code> otherwise.
    * @throws RemoteException for any error.
    */
   private boolean mapAttachmentToField(Object attachment,
      Iterator<PSItemField> fields, List<PSPurgableTempFile> tempFiles)
   throws RemoteException
   {
      // Attachment mapping not ported to JAX-WS. Return false to indicate no mapping.
      return false;
   }

   /**
    * Creates attachments for all binary fields (parent and child) found in
    * the supplied items and adds them to the current message.
    *
    * @param items all items for which to attach the binary field contents
    *    to the current message, assumed not <code>null</code>, may be empty.
    */
   private void attachBinaryFields(List<PSCoreItem> items)
   {
      for (PSCoreItem item : items)
      {
         addAttachments(item.getAllFields());

         Iterator<PSItemChild> children = item.getAllChildren();
         while (children.hasNext())
         {
            PSItemChild child = children.next();
            Iterator<PSItemChildEntry> entries = child.getAllEntries();
            while (entries.hasNext())
            {
               PSItemChildEntry entry = entries.next();
               addAttachments(entry.getAllFields());
            }
         }
      }
   }

   /**
    * Creates attachments for all binary fields supplied and adds them to the
    * current message. The fields href location will be updated with the
    * ccontent id of the new attachment created.
    *
    * @param fields the fields to walk, assumed not <code>null</code>, may be
    *    empty.
    */
   private void addAttachments(Iterator<PSItemField> fields)
   {
      // Attachment creation is not yet ported to JAX-WS; no-op for now.
      logger.debug("addAttachments: attachment creation is a no-op in JAX-WS migration stub.");
      return;
   }

   /* (non-Javadoc)
    * @see Content#viewItems(ViewItemsRequest)
    */
   public ViewItemsResponse viewItems(ViewItemsRequest viewItemsRequest)
   {
      String serviceName = "viewItems";
      ViewItemsResponse response = new ViewItemsResponse();
      try
      {
         if (!tryAuthenticate(serviceName)) return response;

         IPSContentWs service = PSContentWsLocator.getContentWebservice();

         List<IPSGuid> ids = PSWebserviceUtils.getLegacyGuidFromLong(
            viewItemsRequest.getId());

         List<String> fieldNames = null;
         if (viewItemsRequest.getFieldName() != null && !viewItemsRequest.getFieldName().isEmpty())
            fieldNames = viewItemsRequest.getFieldName();

         boolean includeBinary = extractBooleanValue(
            viewItemsRequest.isIncludeBinary(), false);
         boolean attachBinaries = extractBooleanValue(
            viewItemsRequest.isAttachBinaries(), false);

         boolean includeChildren = extractBooleanValue(
            viewItemsRequest.isIncludeChildren(), false);
         List<String> childNames = null;
         if (includeChildren && viewItemsRequest.getChildName() != null && !viewItemsRequest.getChildName().isEmpty())
            childNames = viewItemsRequest.getChildName();
         else if (!includeChildren)
            childNames = new ArrayList<String>();

         boolean includeRelated = extractBooleanValue(
            viewItemsRequest.isIncludeRelated(), false);
         List<String> slotNames = null;
         if (includeRelated && viewItemsRequest.getSlotName() != null && !viewItemsRequest.getSlotName().isEmpty())
            slotNames = viewItemsRequest.getSlotName();
         else if (!includeRelated)
            slotNames = new ArrayList<String>();

         boolean includeFolderPath = extractBooleanValue(
            viewItemsRequest.isIncludeFolderPath(), false);

         List<PSCoreItem> items = service.viewItems(ids, includeBinary,
            includeChildren, includeRelated, includeFolderPath);

         if (includeBinary && attachBinaries)
            attachBinaryFields(items);

         PSItem[] converted = convertItems(items, fieldNames, childNames, slotNames);
         response.getPSItem().addAll(Arrays.asList(converted));
         return response;
      }
      catch (IllegalArgumentException e)
      {
         try { handleInvalidContract(e, serviceName); } catch (PSContractViolationFault cv) { throw new RuntimeException(cv); }
      }
      catch (PSErrorResultsException e)
      {
         try { handleErrorResultsException(e, serviceName); } catch (PSErrorResultsFault erf) { throw new RuntimeException(erf); } catch (RemoteException re) { throw new RuntimeException(re); }
      }

      return response;
   }
}
