/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.publishingdesign.impl;

import com.percussion.publishingdesign.data.PSContentListSummary;
import com.percussion.publishingdesign.data.PSContextSummary;
import com.percussion.publishingdesign.data.PSCopyContentListRequest;
import com.percussion.publishingdesign.data.PSCopyEditionRequest;
import com.percussion.publishingdesign.data.PSDeliveryTypeSummary;
import com.percussion.publishingdesign.data.PSDemandPublishRequest;
import com.percussion.publishingdesign.data.PSEditionContentListAssoc;
import com.percussion.publishingdesign.data.PSEditionSummary;
import com.percussion.publishingdesign.data.PSLocationSchemeSummary;
import com.percussion.publishingdesign.data.PSRuntimeEditionStatus;
import com.percussion.publishingdesign.data.PSRuntimeJobResponse;
import com.percussion.publishingdesign.data.PSSchemeParameter;
import com.percussion.publishingdesign.data.PSSiteDesignSummary;
import com.percussion.publishingdesign.data.PSSitePropertyDto;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.publisher.IPSContentList;
import com.percussion.services.publisher.IPSDeliveryType;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSEditionContentList;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.publisher.data.PSEditionContentList;
import com.percussion.services.publisher.data.PSEditionContentListPK;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.system.utils.PSUrlUtils;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.ToLongFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Thin JSON façade over {@link IPSPublisherService} / {@link IPSSiteManager} for Design UI (feature
 * 990). Delegates only — no engine reimplementation.
 *
 * <p>Base path: {@code /services/sitemanage/publishingdesign}
 */
@Path("/publishingdesign")
@PSSiteManageBean("publishingDesignRestService")
@Lazy
public class PSPublishingDesignRestService {
  private static final Logger log = LogManager.getLogger(PSPublishingDesignRestService.class);

  static final String DESIGN_WRITE_FORBIDDEN =
      "Admin or Designer role required to save a publish edition";
  static final String EDITION_NAME_CONFLICT = "Edition name already exists";
  /** A publish job is still active for this edition. */
  static final String EDITION_IN_USE = "Edition is in use";
  /** Matches {@code RXEDITION.DISPLAYTITLE} VARCHAR(100). */
  static final int MAX_EDITION_NAME_LENGTH = 100;

  static final String EDITION_NAME_TOO_LONG =
      "Edition name must be 100 characters or fewer";

  /** {@link IPSEdition.Priority} is 1 (lowest) through 5 (highest). */
  static final String EDITION_PRIORITY_OUT_OF_RANGE =
      "Edition priority must be from 1 to 5";
  static final String CONTENT_LIST_NAME_CONFLICT = "Content list name already exists";
  /** Matches {@code RXCONTENTLIST.NAME} VARCHAR(100). */
  static final int MAX_CONTENT_LIST_NAME_LENGTH = 100;

  static final String CONTENT_LIST_NAME_TOO_LONG =
      "Content list name must be 100 characters or fewer";
  /** Matches {@code RXCONTENTLIST.DESCRIPTION} VARCHAR(255). */
  static final int MAX_CONTENT_LIST_DESCRIPTION_LENGTH = 255;

  static final String CONTENT_LIST_DESCRIPTION_TOO_LONG =
      "Content list description must be 255 characters or fewer";
  /** Request named an item filter that is not on the system. */
  static final String UNKNOWN_ITEM_FILTER = "Unknown item filter";
  /** Still linked to at least one edition. Removing that association is a separate action. */
  static final String CONTENT_LIST_IN_USE = "Content list is in use";
  static final String CONTENT_LIST_ALREADY_ASSOCIATED =
      "Content list is already associated with this edition";

  /** Move request was not the previous or next position in the current order. */
  static final String CONTENT_LIST_SEQUENCE_NOT_ADJACENT =
      "sequence must be the adjacent position";
  static final String DELIVERY_TYPE_NAME_CONFLICT = "Delivery type name already exists";
  /** Matches {@code PSX_DELIVERY_TYPE.NAME} VARCHAR(50). */
  static final int MAX_DELIVERY_TYPE_NAME_LENGTH = 50;

  static final String DELIVERY_TYPE_NAME_TOO_LONG =
      "Delivery type name must be 50 characters or fewer";
  /** Matches {@code PSX_DELIVERY_TYPE.DESCRIPTION} VARCHAR(255). */
  static final int MAX_DELIVERY_TYPE_DESCRIPTION_LENGTH = 255;

  static final String DELIVERY_TYPE_DESCRIPTION_TOO_LONG =
      "Delivery type description must be 255 characters or fewer";
  /**
   * A content list URL still names this delivery type ({@code sys_deliverytype}). Changing that
   * list is a separate action.
   */
  static final String DELIVERY_TYPE_IN_USE = "Delivery type is in use";
  static final String LOCATION_SCHEME_NAME_CONFLICT = "Location scheme name already exists";

  static final String LOCATION_SCHEME_ASSIGNMENT_CONFLICT =
      "A location scheme already exists for this context, template, and content type";
  /** Matches {@code RXLOCATIONSCHEME.SCHEMENAME} VARCHAR(50). */
  static final int MAX_LOCATION_SCHEME_NAME_LENGTH = 50;

  static final String LOCATION_SCHEME_NAME_TOO_LONG =
      "Location scheme name must be 50 characters or fewer";
  static final String CONTEXT_NAME_CONFLICT = "Publishing context name already exists";
  /** Matches {@code RXCONTEXT.CONTEXTNAME} VARCHAR(50). */
  static final int MAX_CONTEXT_NAME_LENGTH = 50;

  static final String CONTEXT_NAME_TOO_LONG =
      "Publishing context name must be 50 characters or fewer";
  /** Matches {@code RXCONTEXT.CONTEXTDESC} VARCHAR(255). */
  static final int MAX_CONTEXT_DESCRIPTION_LENGTH = 255;

  static final String CONTEXT_DESCRIPTION_TOO_LONG =
      "Publishing context description must be 255 characters or fewer";
  /**
   * Location schemes still belong to this context. Removing those schemes is a separate action.
   */
  static final String CONTEXT_HAS_LOCATION_SCHEMES = "Publishing context has location schemes";

  private final IPSPublisherService publisherService;
  private final IPSGuidManager guidManager;
  private final IPSSiteManager siteManager;
  private final PSPublishingRuntimeSupport runtimeSupport;
  private IPSUserService userService;
  /** When null, item-filter writes resolve {@link PSFilterServiceLocator} at call time. */
  private IPSFilterService filterService;
  private BooleanSupplier designWriteAllowed;
  /** Test hook: active job id for an edition; {@code > 0} means the edition is running. */
  private ToLongFunction<IPSGuid> editionRunningJobId;

  public PSPublishingDesignRestService() {
    this(
        PSPublisherServiceLocator.getPublisherService(),
        PSGuidManagerLocator.getGuidMgr(),
        PSSiteManagerLocator.getSiteManager(),
        new PSPublishingRuntimeSupport());
  }

  public PSPublishingDesignRestService(
      IPSPublisherService publisherService,
      IPSGuidManager guidManager,
      IPSSiteManager siteManager) {
    this(publisherService, guidManager, siteManager, null);
  }

  public PSPublishingDesignRestService(
      IPSPublisherService publisherService,
      IPSGuidManager guidManager,
      IPSSiteManager siteManager,
      PSPublishingRuntimeSupport runtimeSupport) {
    this.publisherService = publisherService;
    this.guidManager = guidManager;
    this.siteManager = siteManager;
    this.runtimeSupport = runtimeSupport;
  }

  /** Back-compat test constructor (site manager null → context/scheme endpoints fail clearly). */
  public PSPublishingDesignRestService(
      IPSPublisherService publisherService, IPSGuidManager guidManager) {
    this(publisherService, guidManager, null, null);
  }

  @Autowired(required = false)
  public void setUserService(IPSUserService userService) {
    this.userService = userService;
  }

  /** Test hook: when set, overrides Admin/Designer check (403). */
  void setDesignWriteAllowed(BooleanSupplier designWriteAllowed) {
    this.designWriteAllowed = designWriteAllowed;
  }

  /** Test hook: item-filter lookup. Production uses the filter service locator. */
  void setFilterService(IPSFilterService filterService) {
    this.filterService = filterService;
  }

  /** Test hook: when set, overrides the runtime running-job lookup (delete and disassociate). */
  void setEditionRunningJobId(ToLongFunction<IPSGuid> editionRunningJobId) {
    this.editionRunningJobId = editionRunningJobId;
  }

  // ---- Editions ----

  @GET
  @Path("/editions")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSEditionSummary> listEditionsBySite(@QueryParam("siteId") String siteId) {
    requireNonBlank(siteId, "siteId");
    try {
      List<IPSEdition> editions = publisherService.findAllEditionsBySite(toSiteGuid(siteId));
      List<PSEditionSummary> out = new ArrayList<>();
      for (IPSEdition edition : editions) {
        out.add(toEditionSummary(edition, siteId));
      }
      return out;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @GET
  @Path("/editions/{editionId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSEditionSummary getEdition(@PathParam("editionId") String editionId) {
    requireNonBlank(editionId, "editionId");
    try {
      IPSEdition edition = publisherService.loadEdition(toEditionGuid(editionId));
      if (edition == null) {
        throw notFound("Edition not found");
      }
      String siteId =
          edition.getSiteId() != null ? String.valueOf(edition.getSiteId().getUUID()) : null;
      return toEditionSummary(edition, siteId);
    } catch (PSNotFoundException e) {
      throw notFound("Edition not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @POST
  @Path("/editions")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSEditionSummary createEdition(PSEditionSummary body) {
    requireDesignWrite();
    if (body == null || isBlank(body.getName()) || isBlank(body.getSiteId())) {
      throw badRequest("name and siteId are required");
    }
    String trimmedName = body.getName().trim();
    if (trimmedName.length() > MAX_EDITION_NAME_LENGTH) {
      throw badRequest(EDITION_NAME_TOO_LONG);
    }
    requireEditionPriority(body.getPriority());
    try {
      requireUniqueEditionName(trimmedName, null);
      IPSEdition edition = publisherService.createEdition();
      applyEditionFields(edition, body, true);
      publisherService.saveEdition(edition);
      return toEditionSummary(edition, body.getSiteId());
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @PUT
  @Path("/editions/{editionId}")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSEditionSummary updateEdition(
      @PathParam("editionId") String editionId, PSEditionSummary body) {
    requireDesignWrite();
    requireNonBlank(editionId, "editionId");
    if (body == null) {
      throw badRequest("body is required");
    }
    requireEditionPriority(body.getPriority());
    try {
      IPSEdition edition = publisherService.loadEditionModifiable(toEditionGuid(editionId));
      if (!isBlank(body.getName())) {
        String trimmedName = body.getName().trim();
        if (trimmedName.length() > MAX_EDITION_NAME_LENGTH) {
          throw badRequest(EDITION_NAME_TOO_LONG);
        }
        requireUniqueEditionName(trimmedName, editionId);
      }
      applyEditionFields(edition, body, false);
      publisherService.saveEdition(edition);
      String siteId =
          edition.getSiteId() != null
              ? String.valueOf(edition.getSiteId().getUUID())
              : body.getSiteId();
      return toEditionSummary(edition, siteId);
    } catch (PSNotFoundException e) {
      throw notFound("Edition not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @DELETE
  @Path("/editions/{editionId}")
  public void deleteEdition(@PathParam("editionId") String editionId) {
    requireDesignWrite();
    requireNonBlank(editionId, "editionId");
    try {
      IPSGuid editionGuid = toEditionGuid(editionId);
      rejectEditionInUse(editionGuid);
      IPSEdition edition = publisherService.loadEdition(editionGuid);
      if (edition == null) {
        throw notFound("Edition not found");
      }
      publisherService.deleteEdition(edition);
    } catch (PSNotFoundException e) {
      throw notFound("Edition not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @POST
  @Path("/editions/copy")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSEditionSummary copyEdition(PSCopyEditionRequest request) {
    requireDesignWrite();
    if (request == null
        || isBlank(request.getSourceEditionId())
        || isBlank(request.getTargetSiteId())) {
      throw badRequest("sourceEditionId and targetSiteId are required");
    }
    try {
      IPSEdition source = publisherService.loadEdition(toEditionGuid(request.getSourceEditionId()));
      String newName =
          isBlank(request.getNewName()) ? source.getName() + "_copy" : request.getNewName().trim();
      requireUniqueEditionName(newName, null);
      IPSEdition copy = publisherService.createEdition();
      copy.setComment(source.getComment());
      if (source.getEditionType() != null) {
        copy.setEditionType(source.getEditionType());
      }
      if (source.getPriority() != null) {
        copy.setPriority(source.getPriority());
      }
      copy.setSiteId(toSiteGuid(request.getTargetSiteId()));
      // setName writes the visible display title. Apply it last so the source title cannot replace it.
      copy.setName(newName);
      publisherService.saveEdition(copy);

      if (request.isCopyContentLists() && source.getGUID() != null && copy.getGUID() != null) {
        List<IPSEditionContentList> links =
            publisherService.loadEditionContentLists(source.getGUID());
        for (IPSEditionContentList link : links) {
          if (link.getContentListId() == null || link.getDeliveryContextId() == null) {
            continue;
          }
          IPSEditionContentList newLink = publisherService.createEditionContentList();
          if (newLink instanceof PSEditionContentList pcl) {
            PSEditionContentListPK pk = pcl.getEditionContentListPK();
            pk.setEditionid(copy.getGUID().longValue());
            pk.setContentlistid(link.getContentListId().longValue());
            pcl.setEditionContentListPK(pk);
            pcl.setDeliveryContextId(link.getDeliveryContextId());
            if (link.getAssemblyContextId() != null) {
              pcl.setAssemblyContextId(link.getAssemblyContextId());
            }
            if (link.getSequence() != null) {
              pcl.setSequence(link.getSequence());
            }
            publisherService.saveEditionContentList(pcl);
          }
        }
      }
      return toEditionSummary(copy, request.getTargetSiteId());
    } catch (PSNotFoundException e) {
      throw notFound("Source edition not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @GET
  @Path("/editions/{editionId}/contentlists")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSContentListSummary> listEditionContentLists(
      @PathParam("editionId") String editionId) {
    requireNonBlank(editionId, "editionId");
    try {
      return summariesForAssociations(orderedEditionContentLists(toEditionGuid(editionId)));
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  // ---- Content lists ----

  @GET
  @Path("/contentlists")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSContentListSummary> listContentLists() {
    try {
      List<IPSContentList> lists = publisherService.findAllContentLists("");
      List<PSContentListSummary> out = new ArrayList<>();
      for (IPSContentList cl : lists) {
        out.add(toContentListSummary(cl));
      }
      return out;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @GET
  @Path("/contentlists/{contentListId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContentListSummary getContentList(@PathParam("contentListId") String contentListId) {
    requireNonBlank(contentListId, "contentListId");
    try {
      IPSContentList cl = publisherService.loadContentList(toContentListGuid(contentListId));
      if (cl == null) {
        throw notFound("Content list not found");
      }
      return toContentListSummary(cl);
    } catch (PSNotFoundException e) {
      throw notFound("Content list not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @POST
  @Path("/contentlists")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContentListSummary createContentList(PSContentListSummary body) {
    requireDesignWrite();
    if (body == null || isBlank(body.getName())) {
      throw badRequest("name is required");
    }
    try {
      requireUniqueContentListName(body.getName().trim(), null);
      ItemFilterUpdate filterUpdate = resolveItemFilterUpdate(body);
      IPSContentList cl = publisherService.createContentList(body.getName().trim());
      applyContentListFields(cl, body, true, filterUpdate);
      publisherService.saveContentList(cl);
      return toContentListSummary(cl);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  /**
   * Update one content list. A description-only body leaves the name, type, generator, URL, and
   * item filter stored. A blank description clears it. A description longer than
   * {@link #MAX_CONTENT_LIST_DESCRIPTION_LENGTH} is HTTP 400 and writes nothing.
   */
  @PUT
  @Path("/contentlists/{contentListId}")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContentListSummary updateContentList(
      @PathParam("contentListId") String contentListId, PSContentListSummary body) {
    requireDesignWrite();
    requireNonBlank(contentListId, "contentListId");
    if (body == null) {
      throw badRequest("body is required");
    }
    try {
      ItemFilterUpdate filterUpdate = resolveItemFilterUpdate(body);
      IPSContentList cl =
          publisherService.loadContentListModifiable(toContentListGuid(contentListId));
      if (!isBlank(body.getName())) {
        requireUniqueContentListName(body.getName().trim(), contentListId);
      }
      applyContentListFields(cl, body, false, filterUpdate);
      publisherService.saveContentList(cl);
      return toContentListSummary(cl);
    } catch (PSNotFoundException e) {
      throw notFound("Content list not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @DELETE
  @Path("/contentlists/{contentListId}")
  public void deleteContentList(@PathParam("contentListId") String contentListId) {
    requireDesignWrite();
    requireNonBlank(contentListId, "contentListId");
    try {
      IPSGuid contentListGuid = toContentListGuid(contentListId);
      IPSContentList cl = publisherService.loadContentList(contentListGuid);
      if (cl == null) {
        throw notFound("Content list not found");
      }
      rejectContentListInUse(contentListGuid);
      publisherService.deleteContentLists(List.of(cl));
    } catch (PSNotFoundException e) {
      throw notFound("Content list not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  /**
   * Copy one content list to a new id and name. Description, generator, expander, item filter,
   * edition type, content-list type, URL, and generator/expander parameters are copied. The source
   * row is not saved. This does not associate the copy with an edition.
   */
  @POST
  @Path("/contentlists/copy")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContentListSummary copyContentList(PSCopyContentListRequest request) {
    requireDesignWrite();
    if (request == null
        || isBlank(request.getSourceContentListId())
        || isBlank(request.getNewName())) {
      throw badRequest("sourceContentListId and newName are required");
    }
    String newName = request.getNewName().trim();
    if (newName.length() > MAX_CONTENT_LIST_NAME_LENGTH) {
      throw badRequest(CONTENT_LIST_NAME_TOO_LONG);
    }
    try {
      IPSContentList source =
          publisherService.loadContentList(
              toContentListGuid(request.getSourceContentListId().trim()));
      if (source == null) {
        throw notFound("Content list not found");
      }
      requireUniqueContentListName(newName, null);
      IPSContentList copy = publisherService.createContentList(newName);
      copyContentListFields(source, copy);
      // setName writes the visible title. Apply it last so copied fields cannot replace it.
      copy.setName(newName);
      publisherService.saveContentList(copy);
      return toContentListSummary(copy);
    } catch (PSNotFoundException e) {
      throw notFound("Content list not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  // ---- Delivery types ----

  @GET
  @Path("/deliverytypes")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSDeliveryTypeSummary> listDeliveryTypes() {
    try {
      List<IPSDeliveryType> types = publisherService.findAllDeliveryTypes();
      List<PSDeliveryTypeSummary> out = new ArrayList<>();
      for (IPSDeliveryType t : types) {
        out.add(toDeliveryTypeSummary(t));
      }
      return out;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @GET
  @Path("/deliverytypes/{deliveryTypeId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSDeliveryTypeSummary getDeliveryType(@PathParam("deliveryTypeId") String deliveryTypeId) {
    requireNonBlank(deliveryTypeId, "deliveryTypeId");
    try {
      IPSGuid guid = guidManager.makeGuid(deliveryTypeId, PSTypeEnum.DELIVERY_TYPE);
      IPSDeliveryType t = publisherService.loadDeliveryType(guid);
      return toDeliveryTypeSummary(t);
    } catch (PSNotFoundException e) {
      throw notFound("Delivery type not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @POST
  @Path("/deliverytypes")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSDeliveryTypeSummary createDeliveryType(PSDeliveryTypeSummary body) {
    requireDesignWrite();
    if (body == null || isBlank(body.getName()) || isBlank(body.getBeanName())) {
      throw badRequest("name and beanName are required");
    }
    String trimmedName = body.getName().trim();
    if (trimmedName.length() > MAX_DELIVERY_TYPE_NAME_LENGTH) {
      throw badRequest(DELIVERY_TYPE_NAME_TOO_LONG);
    }
    try {
      requireUniqueDeliveryTypeName(trimmedName, null);
      IPSDeliveryType t = publisherService.createDeliveryType();
      t.setName(trimmedName);
      t.setBeanName(body.getBeanName().trim());
      if (body.getDescription() != null) {
        t.setDescription(body.getDescription());
      }
      t.setUnpublishingRequiresAssembly(body.isUnpublishingRequiresAssembly());
      publisherService.saveDeliveryType(t);
      return toDeliveryTypeSummary(t);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @PUT
  @Path("/deliverytypes/{deliveryTypeId}")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSDeliveryTypeSummary updateDeliveryType(
      @PathParam("deliveryTypeId") String deliveryTypeId, PSDeliveryTypeSummary body) {
    requireDesignWrite();
    requireNonBlank(deliveryTypeId, "deliveryTypeId");
    if (body == null) {
      throw badRequest("body is required");
    }
    try {
      IPSGuid guid = guidManager.makeGuid(deliveryTypeId, PSTypeEnum.DELIVERY_TYPE);
      IPSDeliveryType t = publisherService.loadDeliveryTypeModifiable(guid);
      // Reject an overlong description before any field is written so 400 leaves name and bean.
      String nextDescription = null;
      boolean applyDescription = body.getDescription() != null;
      if (applyDescription) {
        nextDescription = body.getDescription().trim();
        if (nextDescription.length() > MAX_DELIVERY_TYPE_DESCRIPTION_LENGTH) {
          throw badRequest(DELIVERY_TYPE_DESCRIPTION_TOO_LONG);
        }
        if (nextDescription.isEmpty()) {
          nextDescription = null;
        }
      }
      if (!isBlank(body.getName())) {
        String trimmedName = body.getName().trim();
        if (trimmedName.length() > MAX_DELIVERY_TYPE_NAME_LENGTH) {
          throw badRequest(DELIVERY_TYPE_NAME_TOO_LONG);
        }
        requireUniqueDeliveryTypeName(trimmedName, deliveryTypeId);
        t.setName(trimmedName);
      }
      if (!isBlank(body.getBeanName())) {
        t.setBeanName(body.getBeanName().trim());
      }
      if (applyDescription) {
        t.setDescription(nextDescription);
      }
      // Omitted on a name-only rename. Do not clear the stored flag with the primitive default.
      if (body.isUnpublishingRequiresAssemblySpecified()) {
        t.setUnpublishingRequiresAssembly(body.isUnpublishingRequiresAssembly());
      }
      publisherService.saveDeliveryType(t);
      return toDeliveryTypeSummary(t);
    } catch (PSNotFoundException e) {
      throw notFound("Delivery type not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @DELETE
  @Path("/deliverytypes/{deliveryTypeId}")
  public void deleteDeliveryType(@PathParam("deliveryTypeId") String deliveryTypeId) {
    requireDesignWrite();
    requireNonBlank(deliveryTypeId, "deliveryTypeId");
    try {
      IPSGuid guid = guidManager.makeGuid(deliveryTypeId, PSTypeEnum.DELIVERY_TYPE);
      IPSDeliveryType t = publisherService.loadDeliveryType(guid);
      rejectDeliveryTypeInUse(t);
      publisherService.deleteDeliveryType(t);
    } catch (PSNotFoundException e) {
      throw notFound("Delivery type not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  // ---- Design sites + context variables ----

  @GET
  @Path("/sites")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSSiteDesignSummary> listDesignSites() {
    requireSiteManager();
    try {
      List<IPSSite> sites = siteManager.findAllSites();
      List<PSSiteDesignSummary> out = new ArrayList<>();
      for (IPSSite site : sites) {
        out.add(toSiteDesignSummary(site));
      }
      return out;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @GET
  @Path("/sites/{siteId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSiteDesignSummary getDesignSite(@PathParam("siteId") String siteId) {
    requireSiteManager();
    requireNonBlank(siteId, "siteId");
    try {
      return toSiteDesignSummary(siteManager.loadSite(toSiteGuid(siteId)));
    } catch (PSNotFoundException e) {
      throw notFound("Site not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @GET
  @Path("/sites/{siteId}/properties")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSSitePropertyDto> listSiteProperties(
      @PathParam("siteId") String siteId, @QueryParam("contextId") String contextId) {
    requireSiteManager();
    requireNonBlank(siteId, "siteId");
    requireNonBlank(contextId, "contextId");
    try {
      IPSSite site = siteManager.loadSite(toSiteGuid(siteId));
      IPSGuid ctx = guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT);
      List<PSSitePropertyDto> out = new ArrayList<>();
      for (String name : site.getPropertyNames(ctx)) {
        PSSitePropertyDto dto = new PSSitePropertyDto();
        dto.setName(name);
        dto.setContextId(contextId);
        dto.setValue(site.getProperty(name, ctx));
        out.add(dto);
      }
      return out;
    } catch (PSNotFoundException e) {
      throw notFound("Site not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @PUT
  @Path("/sites/{siteId}/properties")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSitePropertyDto putSiteProperty(
      @PathParam("siteId") String siteId, PSSitePropertyDto body) {
    requireSiteManager();
    requireNonBlank(siteId, "siteId");
    if (body == null || isBlank(body.getName()) || isBlank(body.getContextId())) {
      throw badRequest("name and contextId are required");
    }
    try {
      IPSSite site = siteManager.loadSiteModifiable(toSiteGuid(siteId));
      IPSGuid ctx = guidManager.makeGuid(body.getContextId(), PSTypeEnum.CONTEXT);
      site.setProperty(body.getName().trim(), ctx, body.getValue() != null ? body.getValue() : "");
      siteManager.saveSite(site);
      PSSitePropertyDto out = new PSSitePropertyDto();
      out.setName(body.getName().trim());
      out.setContextId(body.getContextId());
      out.setValue(site.getProperty(body.getName().trim(), ctx));
      return out;
    } catch (PSNotFoundException e) {
      throw notFound("Site not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @DELETE
  @Path("/sites/{siteId}/properties")
  public void deleteSiteProperty(
      @PathParam("siteId") String siteId,
      @QueryParam("name") String name,
      @QueryParam("contextId") String contextId) {
    requireSiteManager();
    requireNonBlank(siteId, "siteId");
    requireNonBlank(name, "name");
    requireNonBlank(contextId, "contextId");
    try {
      IPSSite site = siteManager.loadSiteModifiable(toSiteGuid(siteId));
      IPSGuid ctx = guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT);
      site.removeProperty(name.trim(), ctx);
      siteManager.saveSite(site);
    } catch (PSNotFoundException e) {
      throw notFound("Site not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  // ---- Edition content-list association ----

  @POST
  @Path("/editions/{editionId}/contentlists")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContentListSummary associateContentList(
      @PathParam("editionId") String editionId, PSEditionContentListAssoc body) {
    requireDesignWrite();
    requireNonBlank(editionId, "editionId");
    if (body == null || isBlank(body.getContentListId()) || isBlank(body.getDeliveryContextId())) {
      throw badRequest("contentListId and deliveryContextId are required");
    }
    try {
      IPSGuid edGuid = toEditionGuid(editionId);
      publisherService.loadEdition(edGuid); // existence
      IPSGuid clGuid = toContentListGuid(body.getContentListId());
      IPSContentList cl = publisherService.loadContentList(clGuid);
      List<IPSEditionContentList> existingLinks = publisherService.loadEditionContentLists(edGuid);
      if (existingLinks != null) {
        for (IPSEditionContentList link : existingLinks) {
          if (link != null
              && link.getContentListId() != null
              && link.getContentListId().longValue() == clGuid.longValue()) {
            throw conflict(CONTENT_LIST_ALREADY_ASSOCIATED);
          }
        }
      }
      IPSEditionContentList newLink = publisherService.createEditionContentList();
      if (!(newLink instanceof PSEditionContentList pcl)) {
        throw new WebApplicationException(
            "Unable to create association", Response.Status.INTERNAL_SERVER_ERROR);
      }
      PSEditionContentListPK pk = pcl.getEditionContentListPK();
      pk.setEditionid(edGuid.longValue());
      pk.setContentlistid(clGuid.longValue());
      pcl.setEditionContentListPK(pk);
      pcl.setDeliveryContextId(
          guidManager.makeGuid(body.getDeliveryContextId(), PSTypeEnum.CONTEXT));
      if (!isBlank(body.getAssemblyContextId())) {
        pcl.setAssemblyContextId(
            guidManager.makeGuid(body.getAssemblyContextId(), PSTypeEnum.CONTEXT));
      }
      if (body.getSequence() != null) {
        pcl.setSequence(body.getSequence());
      } else {
        pcl.setSequence(nextAssociationSequence(existingLinks));
      }
      publisherService.saveEditionContentList(pcl);
      return toContentListSummary(cl);
    } catch (PSNotFoundException e) {
      throw notFound("Edition or content list not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @DELETE
  @Path("/editions/{editionId}/contentlists/{contentListId}")
  public void disassociateContentList(
      @PathParam("editionId") String editionId, @PathParam("contentListId") String contentListId) {
    requireDesignWrite();
    requireNonBlank(editionId, "editionId");
    requireNonBlank(contentListId, "contentListId");
    try {
      IPSGuid edGuid = toEditionGuid(editionId);
      // Same in-use rule as edition delete: a running job keeps the association.
      rejectEditionInUse(edGuid);
      IPSGuid clGuid = toContentListGuid(contentListId);
      List<IPSEditionContentList> links = publisherService.loadEditionContentLists(edGuid);
      boolean removed = false;
      if (links != null) {
        for (IPSEditionContentList link : links) {
          if (link == null || link.getContentListId() == null) {
            continue;
          }
          if (link.getContentListId().longValue() == clGuid.longValue()) {
            publisherService.deleteEditionContentList(link);
            removed = true;
          }
        }
      }
      if (!removed) {
        throw notFound("Association not found");
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  /**
   * Move one association to an adjacent 0-based position. Stored sequences are rewritten to 1..n
   * in the new order. HTTP 400 when the position is missing or not adjacent; 403 when the caller
   * cannot write design; 409 while a publish job is running for the edition.
   */
  @PUT
  @Path("/editions/{editionId}/contentlists/{contentListId}/sequence")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void reorderEditionContentList(
      @PathParam("editionId") String editionId,
      @PathParam("contentListId") String contentListId,
      PSEditionContentListAssoc body) {
    requireDesignWrite();
    requireNonBlank(editionId, "editionId");
    requireNonBlank(contentListId, "contentListId");
    if (body == null || body.getSequence() == null) {
      throw badRequest("sequence is required");
    }
    try {
      IPSGuid edGuid = toEditionGuid(editionId);
      rejectEditionInUse(edGuid);
      IPSGuid clGuid = toContentListGuid(contentListId);
      List<IPSEditionContentList> ordered = orderedEditionContentLists(edGuid);
      int from = -1;
      for (int i = 0; i < ordered.size(); i++) {
        if (ordered.get(i).getContentListId().longValue() == clGuid.longValue()) {
          from = i;
          break;
        }
      }
      if (from < 0) {
        throw notFound("Association not found");
      }
      int to = body.getSequence();
      if (to < 0 || to >= ordered.size() || Math.abs(to - from) != 1) {
        throw badRequest(CONTENT_LIST_SEQUENCE_NOT_ADJACENT);
      }
      IPSEditionContentList moving = ordered.remove(from);
      ordered.add(to, moving);
      for (int i = 0; i < ordered.size(); i++) {
        int want = i + 1;
        IPSEditionContentList link = ordered.get(i);
        if (link.getSequence() == null || link.getSequence().intValue() != want) {
          link.setSequence(want);
          publisherService.saveEditionContentList(link);
        }
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  // ---- Contexts / schemes ----

  @GET
  @Path("/contexts")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSContextSummary> listContexts() {
    requireSiteManager();
    try {
      List<IPSPublishingContext> contexts = siteManager.findAllContexts();
      List<PSContextSummary> out = new ArrayList<>();
      for (IPSPublishingContext c : contexts) {
        out.add(toContextSummary(c));
      }
      return out;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @GET
  @Path("/contexts/{contextId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContextSummary getContext(@PathParam("contextId") String contextId) {
    requireSiteManager();
    requireNonBlank(contextId, "contextId");
    try {
      return toContextSummary(
          siteManager.loadContext(guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT)));
    } catch (PSNotFoundException e) {
      throw notFound("Context not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @POST
  @Path("/contexts")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContextSummary createContext(PSContextSummary body) {
    requireSiteManager();
    requireDesignWrite();
    if (body == null || isBlank(body.getName())) {
      throw badRequest("name is required");
    }
    String trimmedName = body.getName().trim();
    if (trimmedName.length() > MAX_CONTEXT_NAME_LENGTH) {
      throw badRequest(CONTEXT_NAME_TOO_LONG);
    }
    try {
      requireUniqueContextName(trimmedName, null);
      IPSPublishingContext ctx = siteManager.createContext();
      ctx.setName(trimmedName);
      if (body.getDescription() != null) {
        ctx.setDescription(body.getDescription());
      }
      if (!isBlank(body.getDefaultSchemeId())) {
        ctx.setDefaultSchemeId(
            guidManager.makeGuid(body.getDefaultSchemeId(), PSTypeEnum.LOCATION_SCHEME));
      }
      siteManager.saveContext(ctx);
      return toContextSummary(ctx);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  /**
   * Update one publishing context. A description-only body leaves the name and default scheme
   * stored and does not create or move location schemes. A blank description clears it. A
   * description longer than {@link #MAX_CONTEXT_DESCRIPTION_LENGTH} is HTTP 400 and writes
   * nothing.
   */
  @PUT
  @Path("/contexts/{contextId}")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContextSummary updateContext(
      @PathParam("contextId") String contextId, PSContextSummary body) {
    requireSiteManager();
    requireDesignWrite();
    requireNonBlank(contextId, "contextId");
    if (body == null) {
      throw badRequest("body is required");
    }
    try {
      IPSPublishingContext ctx =
          siteManager.loadContextModifiable(guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT));
      // Reject an overlong description before any field is written so 400 leaves name and schemes.
      String nextDescription = null;
      boolean applyDescription = body.getDescription() != null;
      if (applyDescription) {
        nextDescription = body.getDescription().trim();
        if (nextDescription.length() > MAX_CONTEXT_DESCRIPTION_LENGTH) {
          throw badRequest(CONTEXT_DESCRIPTION_TOO_LONG);
        }
        if (nextDescription.isEmpty()) {
          nextDescription = null;
        }
      }
      if (!isBlank(body.getName())) {
        String trimmedName = body.getName().trim();
        if (trimmedName.length() > MAX_CONTEXT_NAME_LENGTH) {
          throw badRequest(CONTEXT_NAME_TOO_LONG);
        }
        requireUniqueContextName(trimmedName, contextId);
        ctx.setName(trimmedName);
      }
      if (applyDescription) {
        ctx.setDescription(nextDescription);
      }
      if (body.getDefaultSchemeId() != null) {
        if (body.getDefaultSchemeId().isBlank()) {
          ctx.setDefaultSchemeId(null);
        } else {
          ctx.setDefaultSchemeId(
              guidManager.makeGuid(body.getDefaultSchemeId(), PSTypeEnum.LOCATION_SCHEME));
        }
      }
      siteManager.saveContext(ctx);
      return toContextSummary(ctx);
    } catch (PSNotFoundException e) {
      throw notFound("Context not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @DELETE
  @Path("/contexts/{contextId}")
  public void deleteContext(@PathParam("contextId") String contextId) {
    requireDesignWrite();
    requireNonBlank(contextId, "contextId");
    requireSiteManager();
    try {
      IPSGuid contextGuid = guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT);
      IPSPublishingContext ctx = siteManager.loadContext(contextGuid);
      rejectContextHasSchemes(contextGuid);
      siteManager.deleteContext(ctx);
    } catch (PSNotFoundException e) {
      throw notFound("Context not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @GET
  @Path("/contexts/{contextId}/schemes")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSLocationSchemeSummary> listSchemesForContext(
      @PathParam("contextId") String contextId) {
    requireSiteManager();
    requireNonBlank(contextId, "contextId");
    try {
      IPSGuid ctxGuid = guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT);
      List<IPSLocationScheme> schemes = siteManager.findSchemesByContextId(ctxGuid);
      List<PSLocationSchemeSummary> out = new ArrayList<>();
      for (IPSLocationScheme scheme : schemes) {
        out.add(toSchemeSummary(scheme, false));
      }
      return out;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @GET
  @Path("/schemes/{schemeId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSLocationSchemeSummary getScheme(@PathParam("schemeId") String schemeId) {
    requireSiteManager();
    requireNonBlank(schemeId, "schemeId");
    try {
      IPSLocationScheme scheme =
          siteManager.loadScheme(guidManager.makeGuid(schemeId, PSTypeEnum.LOCATION_SCHEME));
      return toSchemeSummary(scheme, true);
    } catch (PSNotFoundException e) {
      throw notFound("Scheme not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @POST
  @Path("/contexts/{contextId}/schemes")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSLocationSchemeSummary createScheme(
      @PathParam("contextId") String contextId, PSLocationSchemeSummary body) {
    requireSiteManager();
    requireDesignWrite();
    requireNonBlank(contextId, "contextId");
    if (body == null || isBlank(body.getName()) || isBlank(body.getGenerator())) {
      throw badRequest("name and generator are required");
    }
    String trimmedName = body.getName().trim();
    if (trimmedName.length() > MAX_LOCATION_SCHEME_NAME_LENGTH) {
      throw badRequest(LOCATION_SCHEME_NAME_TOO_LONG);
    }
    try {
      requireUniqueLocationSchemeName(contextId, trimmedName, null);
      IPSLocationScheme scheme = siteManager.createScheme();
      scheme.setName(trimmedName);
      scheme.setGenerator(body.getGenerator().trim());
      scheme.setContextId(guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT));
      if (body.getDescription() != null) {
        scheme.setDescription(body.getDescription());
      }
      Long contentTypeId = body.getContentTypeId();
      Long templateId = body.getTemplateId();
      if (Boolean.TRUE.equals(body.getCopy())) {
        long schemeKey = scheme.getGUID().getUUID();
        if (contentTypeId == null) {
          contentTypeId = schemeKey;
        }
        if (templateId == null) {
          templateId = schemeKey;
        }
        templateId =
            unusedCopyTemplateId(
                siteManager.findSchemesByContextId(
                    guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT)),
                contentTypeId,
                templateId,
                schemeKey);
      }
      if (contentTypeId != null) {
        scheme.setContentTypeId(contentTypeId);
      }
      if (templateId != null) {
        scheme.setTemplateId(templateId);
      }
      applySchemeParameters(scheme, body.getParameters(), true);
      siteManager.saveScheme(scheme);
      return toSchemeSummary(scheme, true);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @PUT
  @Path("/schemes/{schemeId}")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSLocationSchemeSummary updateScheme(
      @PathParam("schemeId") String schemeId, PSLocationSchemeSummary body) {
    requireSiteManager();
    requireDesignWrite();
    requireNonBlank(schemeId, "schemeId");
    if (body == null) {
      throw badRequest("body is required");
    }
    try {
      IPSLocationScheme scheme =
          siteManager.loadSchemeModifiable(
              guidManager.makeGuid(schemeId, PSTypeEnum.LOCATION_SCHEME));
      String contextId =
          !isBlank(body.getContextId())
              ? body.getContextId().trim()
              : (scheme.getContextId() != null
                  ? String.valueOf(scheme.getContextId().getUUID())
                  : null);
      if (!isBlank(body.getName()) && contextId != null) {
        requireUniqueLocationSchemeName(contextId, body.getName().trim(), schemeId);
      }
      if (!isBlank(body.getName())) {
        scheme.setName(body.getName().trim());
      }
      if (!isBlank(body.getGenerator())) {
        scheme.setGenerator(body.getGenerator().trim());
      }
      if (body.getDescription() != null) {
        scheme.setDescription(body.getDescription());
      }
      if (body.getContentTypeId() != null) {
        scheme.setContentTypeId(body.getContentTypeId());
      }
      if (body.getTemplateId() != null) {
        scheme.setTemplateId(body.getTemplateId());
      }
      if (!isBlank(body.getContextId())) {
        scheme.setContextId(guidManager.makeGuid(body.getContextId(), PSTypeEnum.CONTEXT));
      }
      applySchemeParameters(scheme, body.getParameters(), false);
      siteManager.saveScheme(scheme);
      return toSchemeSummary(scheme, true);
    } catch (PSNotFoundException e) {
      throw notFound("Scheme not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @DELETE
  @Path("/schemes/{schemeId}")
  public void deleteScheme(@PathParam("schemeId") String schemeId) {
    requireSiteManager();
    requireNonBlank(schemeId, "schemeId");
    try {
      IPSLocationScheme scheme =
          siteManager.loadScheme(guidManager.makeGuid(schemeId, PSTypeEnum.LOCATION_SCHEME));
      siteManager.deleteScheme(scheme);
    } catch (PSNotFoundException e) {
      throw notFound("Scheme not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  // ---- Runtime (US5) ----

  @GET
  @Path("/runtime/editions")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSRuntimeEditionStatus> listRuntimeEditions(
      @QueryParam("siteId") String siteId, @QueryParam("pubServerId") String pubServerId) {
    return requireRuntime().listRuntimeEditions(siteId, pubServerId);
  }

  @POST
  @Path("/runtime/editions/{editionId}/start")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSRuntimeJobResponse startEditionJob(@PathParam("editionId") String editionId) {
    return requireRuntime().startEdition(editionId);
  }

  @POST
  @Path("/runtime/jobs/{jobId}/stop")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSRuntimeJobResponse stopRuntimeJob(@PathParam("jobId") String jobId) {
    return requireRuntime().stopJob(jobId);
  }

  @GET
  @Path("/runtime/jobs/{jobId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSRuntimeJobResponse getRuntimeJob(@PathParam("jobId") String jobId) {
    return requireRuntime().getJobStatus(jobId);
  }

  @POST
  @Path("/runtime/editions/{editionId}/demand")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSRuntimeJobResponse demandPublish(
      @PathParam("editionId") String editionId, PSDemandPublishRequest request) {
    return requireRuntime().demandPublish(editionId, request);
  }

  @POST
  @Path("/runtime/logs/purge")
  public void purgeRuntimeJobLog(@QueryParam("jobId") String jobId) {
    requireRuntime().purgeJobLog(jobId);
  }

  @POST
  @Path("/runtime/sites/{siteId}/clearItems")
  public void clearSiteItems(@PathParam("siteId") String siteId) {
    requireRuntime().clearSiteItems(siteId);
  }

  private PSPublishingRuntimeSupport requireRuntime() {
    if (runtimeSupport == null) {
      throw new WebApplicationException(
          "Runtime support unavailable", Response.Status.SERVICE_UNAVAILABLE);
    }
    return runtimeSupport;
  }

  // ---- Mapping helpers ----

  private void applyEditionFields(IPSEdition edition, PSEditionSummary body, boolean isCreate) {
    requireEditionPriority(body.getPriority());
    String trimmedName = body.getName() == null ? null : body.getName().trim();
    if (isBlank(trimmedName)) {
      if (isCreate) {
        throw badRequest("name is required");
      }
    } else if (trimmedName.length() > MAX_EDITION_NAME_LENGTH) {
      throw badRequest(EDITION_NAME_TOO_LONG);
    }
    if (body.getComment() != null) {
      edition.setComment(body.getComment());
    }
    if (!isBlank(body.getSiteId())) {
      edition.setSiteId(toSiteGuid(body.getSiteId()));
    }
    if (body.getPriority() != null) {
      IPSEdition.Priority p =
          IPSEdition.Priority.findByValue(body.getPriority())
              .orElseThrow(() -> badRequest(EDITION_PRIORITY_OUT_OF_RANGE));
      edition.setPriority(p);
    }
    // setName writes the visible display title. Apply it last so site assignment
    // cannot replace the title the operator typed (same class as edition copy).
    if (!isBlank(trimmedName)) {
      edition.setName(trimmedName);
    }
  }

  private void applyContentListFields(
      IPSContentList cl, PSContentListSummary body, boolean isCreate, ItemFilterUpdate filterUpdate) {
    // Reject an overlong description before any field is written so 400 leaves the stored row.
    String nextDescription = null;
    boolean applyDescription = body.getDescription() != null;
    if (applyDescription) {
      nextDescription = body.getDescription().trim();
      if (nextDescription.length() > MAX_CONTENT_LIST_DESCRIPTION_LENGTH) {
        throw badRequest(CONTENT_LIST_DESCRIPTION_TOO_LONG);
      }
      if (nextDescription.isEmpty()) {
        nextDescription = null;
      }
    }
    if (!isBlank(body.getName()) && !isCreate) {
      cl.setName(body.getName().trim());
    }
    if (applyDescription) {
      cl.setDescription(nextDescription);
    }
    if (body.getGenerator() != null) {
      cl.setGenerator(body.getGenerator());
    }
    if (body.getUrl() != null && !body.getUrl().isBlank()) {
      cl.setUrl(body.getUrl().trim());
    }
    if (filterUpdate.apply()) {
      cl.setFilterId(filterUpdate.filterId());
    }
  }

  /**
   * {@code apply} false leaves the stored filter alone ({@code itemFilterId} omitted). Blank clears
   * it. Any other value must name an existing item filter by uuid or name.
   */
  private ItemFilterUpdate resolveItemFilterUpdate(PSContentListSummary body) {
    if (body.getItemFilterId() == null) {
      return ItemFilterUpdate.unchanged();
    }
    String raw = body.getItemFilterId().trim();
    if (raw.isEmpty()) {
      return ItemFilterUpdate.clear();
    }
    IPSItemFilter filter = findItemFilter(raw);
    if (filter == null || filter.getGUID() == null) {
      throw badRequest(UNKNOWN_ITEM_FILTER);
    }
    return ItemFilterUpdate.set(filter.getGUID());
  }

  private IPSItemFilter findItemFilter(String idOrName) {
    IPSFilterService filters = requireFilterService();
    if (idOrName.chars().allMatch(Character::isDigit)) {
      IPSItemFilter byId = findItemFilterByUuid(filters, idOrName);
      if (byId != null) {
        return byId;
      }
    }
    try {
      return filters.findFilterByName(idOrName);
    } catch (PSFilterException e) {
      return null;
    }
  }

  private IPSItemFilter findItemFilterByUuid(IPSFilterService filters, String uuid) {
    try {
      IPSGuid guid = guidManager.makeGuid(uuid, PSTypeEnum.ITEM_FILTER);
      return filters.findFilterByID(guid);
    } catch (PSNotFoundException | IllegalArgumentException e) {
      return null;
    }
  }

  private IPSFilterService requireFilterService() {
    if (filterService != null) {
      return filterService;
    }
    return PSFilterServiceLocator.getFilterService();
  }

  /**
   * {@code apply == false} means the request did not mention the filter. {@code filterId == null}
   * with {@code apply == true} clears it.
   */
  private record ItemFilterUpdate(boolean apply, IPSGuid filterId) {
    static ItemFilterUpdate unchanged() {
      return new ItemFilterUpdate(false, null);
    }

    static ItemFilterUpdate clear() {
      return new ItemFilterUpdate(true, null);
    }

    static ItemFilterUpdate set(IPSGuid filterId) {
      return new ItemFilterUpdate(true, filterId);
    }
  }

  /**
   * Copy definition fields onto a new list. Does not copy the id or name, and does not write the
   * source. Parameter maps are copied by value so the source argument beans stay attached to the
   * source list.
   */
  private void copyContentListFields(IPSContentList source, IPSContentList copy) {
    if (source.getDescription() != null) {
      copy.setDescription(source.getDescription());
    }
    if (source.getEditionType() != null) {
      copy.setEditionType(source.getEditionType());
    }
    if (!isBlank(source.getExpander())) {
      copy.setExpander(source.getExpander());
    }
    if (source.getGenerator() != null) {
      copy.setGenerator(source.getGenerator());
    }
    if (source.getFilterId() != null) {
      copy.setFilterId(source.getFilterId());
    }
    String url = source.getUrl();
    if (!isBlank(url)) {
      copy.setUrl(url.trim());
    }
    if (source.getContentListType() != null) {
      copy.setContentListType(source.getContentListType());
    }
    Map<String, String> generatorParams = source.getGeneratorParams();
    if (generatorParams != null && !generatorParams.isEmpty()) {
      copy.setGeneratorParams(new HashMap<>(generatorParams));
    }
    Map<String, String> expanderParams = source.getExpanderParams();
    if (expanderParams != null && !expanderParams.isEmpty()) {
      copy.setExpanderParams(new HashMap<>(expanderParams));
    }
  }

  private PSEditionSummary toEditionSummary(IPSEdition edition, String siteId) {
    PSEditionSummary s = new PSEditionSummary();
    if (edition.getGUID() != null) {
      s.setEditionId(String.valueOf(edition.getGUID().getUUID()));
    }
    s.setName(edition.getName());
    s.setSiteId(siteId);
    s.setComment(edition.getComment());
    if (edition.getPriority() != null) {
      s.setPriority(edition.getPriority().getValue());
    }
    return s;
  }

  private PSContentListSummary toContentListSummary(IPSContentList cl) {
    PSContentListSummary s = new PSContentListSummary();
    if (cl.getGUID() != null) {
      s.setContentListId(String.valueOf(cl.getGUID().getUUID()));
    }
    s.setName(cl.getName());
    s.setDescription(cl.getDescription());
    s.setListType(cl.isLegacy() ? "legacy" : "modern");
    s.setGenerator(cl.getGenerator());
    try {
      s.setUrl(cl.getUrl());
    } catch (Exception ignored) {
      // optional
    }
    applyStoredItemFilter(s, cl.getFilterId());
    return s;
  }

  /** Id always; name when the filter service can resolve it. A lookup miss keeps the id. */
  private void applyStoredItemFilter(PSContentListSummary summary, IPSGuid filterId) {
    if (filterId == null) {
      return;
    }
    summary.setItemFilterId(String.valueOf(filterId.getUUID()));
    IPSFilterService filters = filterService;
    if (filters == null) {
      try {
        filters = PSFilterServiceLocator.getFilterService();
      } catch (RuntimeException e) {
        return;
      }
    }
    try {
      IPSItemFilter filter = filters.findFilterByID(filterId);
      if (filter != null && !isBlank(filter.getName())) {
        summary.setItemFilterName(filter.getName());
      }
    } catch (RuntimeException e) {
      // id without a display name (includes a missing filter)
    }
  }

  private PSDeliveryTypeSummary toDeliveryTypeSummary(IPSDeliveryType t) {
    PSDeliveryTypeSummary s = new PSDeliveryTypeSummary();
    if (t.getGUID() != null) {
      s.setDeliveryTypeId(String.valueOf(t.getGUID().getUUID()));
    }
    s.setName(t.getName());
    s.setBeanName(t.getBeanName());
    s.setDescription(t.getDescription());
    s.setUnpublishingRequiresAssembly(t.isUnpublishingRequiresAssembly());
    return s;
  }

  private PSContextSummary toContextSummary(IPSPublishingContext c) {
    PSContextSummary s = new PSContextSummary();
    if (c.getGUID() != null) {
      s.setContextId(String.valueOf(c.getGUID().getUUID()));
    }
    s.setName(c.getName());
    s.setDescription(c.getDescription());
    if (c.getDefaultScheme() != null && c.getDefaultScheme().getGUID() != null) {
      s.setDefaultSchemeId(String.valueOf(c.getDefaultScheme().getGUID().getUUID()));
    }
    return s;
  }

  private PSLocationSchemeSummary toSchemeSummary(
      IPSLocationScheme scheme, boolean includeParameters) {
    PSLocationSchemeSummary s = new PSLocationSchemeSummary();
    if (scheme.getGUID() != null) {
      s.setSchemeId(String.valueOf(scheme.getGUID().getUUID()));
    }
    s.setName(scheme.getName());
    s.setDescription(scheme.getDescription());
    if (scheme.getContextId() != null) {
      s.setContextId(String.valueOf(scheme.getContextId().getUUID()));
    }
    s.setGenerator(scheme.getGenerator());
    s.setContentTypeId(scheme.getContentTypeId());
    s.setTemplateId(scheme.getTemplateId());
    // Legacy schemes typically lack a modern generator expression style.
    s.setSchemeType(isBlank(scheme.getGenerator()) ? "legacy" : "modern");
    if (includeParameters) {
      List<PSSchemeParameter> params = new ArrayList<>();
      for (String pname : scheme.getParameterNames()) {
        PSSchemeParameter p = new PSSchemeParameter();
        p.setName(pname);
        p.setType(scheme.getParameterType(pname));
        p.setValue(scheme.getParameterValue(pname));
        p.setSequence(scheme.getParameterSequence(pname));
        params.add(p);
      }
      s.setParameters(params);
    }
    return s;
  }

  private PSSiteDesignSummary toSiteDesignSummary(IPSSite site) {
    PSSiteDesignSummary s = new PSSiteDesignSummary();
    if (site.getGUID() != null) {
      s.setSiteId(String.valueOf(site.getGUID().getUUID()));
    }
    s.setName(site.getName());
    s.setDescription(site.getDescription());
    s.setFolderRoot(site.getFolderRoot());
    s.setBaseUrl(site.getBaseUrl());
    return s;
  }

  /**
   * Replace or append scheme parameters. When {@code replaceAll} is false and parameters is null,
   * leaves existing params unchanged; when non-null, clears unknown names then sets listed ones.
   */
  private void applySchemeParameters(
      IPSLocationScheme scheme, List<PSSchemeParameter> parameters, boolean isCreate) {
    if (parameters == null) {
      return;
    }
    // Remove parameters not in the new set (update path) or clear for create with empty list.
    List<String> existing = new ArrayList<>(scheme.getParameterNames());
    for (String existingName : existing) {
      boolean keep = false;
      for (PSSchemeParameter p : parameters) {
        if (p != null && existingName.equals(p.getName())) {
          keep = true;
          break;
        }
      }
      if (!keep) {
        scheme.removeParameter(existingName);
      }
    }
    int seq = 0;
    for (PSSchemeParameter p : parameters) {
      if (p == null || isBlank(p.getName()) || isBlank(p.getValue())) {
        continue;
      }
      String type = isBlank(p.getType()) ? "String" : p.getType().trim();
      int sequence = p.getSequence() != null ? p.getSequence() : seq;
      scheme.addParameter(p.getName().trim(), sequence, type, p.getValue());
      seq++;
    }
  }

  private void requireSiteManager() {
    if (siteManager == null) {
      throw new WebApplicationException(
          "Site manager not available", Response.Status.SERVICE_UNAVAILABLE);
    }
  }

  private IPSGuid toSiteGuid(String siteId) {
    return guidManager.makeGuid(siteId, PSTypeEnum.SITE);
  }

  private IPSGuid toEditionGuid(String editionId) {
    return guidManager.makeGuid(editionId, PSTypeEnum.EDITION);
  }

  private IPSGuid toContentListGuid(String contentListId) {
    return guidManager.makeGuid(contentListId, PSTypeEnum.CONTENT_LIST);
  }

  /**
   * Associations in {@link IPSEditionContentList#compareBySequence} order. Null sequences sort
   * last, then by content-list id. The publisher query itself is unordered.
   */
  private List<IPSEditionContentList> orderedEditionContentLists(IPSGuid editionGuid) {
    List<IPSEditionContentList> links = publisherService.loadEditionContentLists(editionGuid);
    List<IPSEditionContentList> ordered = new ArrayList<>();
    if (links == null) {
      return ordered;
    }
    for (IPSEditionContentList link : links) {
      if (link != null && link.getContentListId() != null) {
        ordered.add(link);
      }
    }
    ordered.sort(PSPublishingDesignRestService::compareAssociations);
    return ordered;
  }

  /**
   * Same order as {@link IPSEditionContentList#compareBySequence}: sequence ascending, a missing
   * sequence last, then content-list id.
   */
  static int compareAssociations(IPSEditionContentList left, IPSEditionContentList right) {
    int leftSeq = left.getSequence() == null ? Integer.MAX_VALUE : left.getSequence();
    int rightSeq = right.getSequence() == null ? Integer.MAX_VALUE : right.getSequence();
    int bySequence = Integer.compare(leftSeq, rightSeq);
    if (bySequence != 0) {
      return bySequence;
    }
    return Long.compare(left.getContentListId().longValue(), right.getContentListId().longValue());
  }

  private List<PSContentListSummary> summariesForAssociations(List<IPSEditionContentList> links) {
    List<PSContentListSummary> out = new ArrayList<>();
    for (IPSEditionContentList link : links) {
      if (link.getContentListId() == null) {
        continue;
      }
      try {
        IPSContentList cl = publisherService.loadContentList(link.getContentListId());
        out.add(toContentListSummary(cl));
      } catch (PSNotFoundException ignored) {
        // skip orphan associations
      }
    }
    return out;
  }

  /** Next 1-based sequence, or 1 when no association has a sequence yet. */
  private static int nextAssociationSequence(List<IPSEditionContentList> existing) {
    int next = 1;
    if (existing == null) {
      return next;
    }
    for (IPSEditionContentList link : existing) {
      if (link == null || link.getSequence() == null) {
        continue;
      }
      int current = link.getSequence();
      if (current >= next && current < Integer.MAX_VALUE) {
        next = current + 1;
      }
    }
    return next;
  }

  private static void requireNonBlank(String value, String field) {
    if (isBlank(value)) {
      throw badRequest(field + " is required");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private void requireDesignWrite() {
    boolean allowed;
    try {
      if (designWriteAllowed != null) {
        allowed = designWriteAllowed.getAsBoolean();
      } else if (userService != null) {
        allowed = currentUserMayWriteDesign();
      } else {
        allowed = false;
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.debug("Design write check failed: {}", e.getMessage());
      throw new WebApplicationException(DESIGN_WRITE_FORBIDDEN, Response.Status.FORBIDDEN);
    }
    if (!allowed) {
      throw new WebApplicationException(DESIGN_WRITE_FORBIDDEN, Response.Status.FORBIDDEN);
    }
  }

  private boolean currentUserMayWriteDesign() {
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || isBlank(current.getName())) {
        return false;
      }
      String name = current.getName();
      return userService.isAdminUser(name) || userService.isDesignUser(name);
    } catch (PSDataServiceException e) {
      log.debug("Unable to resolve current user for edition save: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Refuse delete while a publish job is active for the edition (HTTP 409).
   * Idle ({@code 0}) is allowed, including a finished job still retained until reap.
   * A missing runtime lookup is treated as idle so unit tests that do not install
   * runtime support can still delete.
   */
  private void rejectEditionInUse(IPSGuid editionGuid) {
    long jobId;
    try {
      if (editionRunningJobId != null) {
        jobId = editionRunningJobId.applyAsLong(editionGuid);
      } else if (runtimeSupport != null) {
        jobId = runtimeSupport.runningJobId(editionGuid);
      } else {
        jobId = 0L;
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw internalError(e);
    }
    if (jobId > 0L) {
      throw conflict(EDITION_IN_USE);
    }
  }

  /**
   * Refuse delete while any location scheme still belongs to the context (HTTP 409).
   * A context with no schemes is deleted. This does not delete those schemes.
   */
  private void rejectContextHasSchemes(IPSGuid contextGuid) {
    if (contextGuid == null) {
      return;
    }
    List<IPSLocationScheme> schemes;
    try {
      schemes = siteManager.findSchemesByContextId(contextGuid);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw internalError(e);
    }
    if (schemes != null && !schemes.isEmpty()) {
      throw conflict(CONTEXT_HAS_LOCATION_SCHEMES);
    }
  }

  /**
   * Refuse delete while any content list URL still names this delivery type (HTTP 409). An unused
   * type is deleted. This does not change those content lists.
   */
  private void rejectDeliveryTypeInUse(IPSDeliveryType deliveryType) {
    if (deliveryType == null || isBlank(deliveryType.getName())) {
      return;
    }
    String name = deliveryType.getName().trim();
    List<IPSContentList> lists;
    try {
      lists = publisherService.findAllContentLists("");
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw internalError(e);
    }
    if (lists == null || lists.isEmpty()) {
      return;
    }
    for (IPSContentList list : lists) {
      if (list == null) {
        continue;
      }
      String referenced = deliveryTypeNameFromContentListUrl(list.getUrl());
      if (name.equals(referenced)) {
        throw conflict(DELIVERY_TYPE_IN_USE);
      }
    }
  }

  /**
   * {@code sys_deliverytype} query value from a content-list URL, or null when the list does not
   * name one. Percent-decoding matches values stored by the publisher URL helpers.
   */
  private static String deliveryTypeNameFromContentListUrl(String url) {
    if (isBlank(url)) {
      return null;
    }
    String value;
    try {
      value = PSUrlUtils.getUrlParameterValue(url, IPSHtmlParameters.SYS_DELIVERYTYPE);
    } catch (IllegalArgumentException e) {
      return null;
    }
    if (isBlank(value)) {
      return null;
    }
    String trimmed = value.trim();
    try {
      return java.net.URLDecoder.decode(trimmed, java.nio.charset.StandardCharsets.UTF_8).trim();
    } catch (IllegalArgumentException e) {
      return trimmed;
    }
  }

  /**
   * Refuse delete while any edition still references the content list (HTTP 409).
   * An unassociated list is deleted. This does not remove edition associations.
   */
  private void rejectContentListInUse(IPSGuid contentListGuid) {
    if (contentListGuid == null) {
      return;
    }
    List<IPSEdition> editions;
    try {
      editions = publisherService.findAllEditions("");
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw internalError(e);
    }
    if (editions == null || editions.isEmpty()) {
      return;
    }
    long id = contentListGuid.longValue();
    for (IPSEdition edition : editions) {
      if (edition == null || edition.getGUID() == null) {
        continue;
      }
      List<IPSEditionContentList> links;
      try {
        links = publisherService.loadEditionContentLists(edition.getGUID());
      } catch (WebApplicationException e) {
        throw e;
      } catch (RuntimeException e) {
        throw internalError(e);
      }
      if (links == null) {
        continue;
      }
      for (IPSEditionContentList link : links) {
        if (link == null || link.getContentListId() == null) {
          continue;
        }
        if (link.getContentListId().longValue() == id) {
          throw conflict(CONTENT_LIST_IN_USE);
        }
      }
    }
  }

  private void requireUniqueEditionName(String name, String currentEditionId) {
    IPSEdition existing = publisherService.findEditionByName(name);
    if (existing == null || existing.getGUID() == null) {
      return;
    }
    String existingId = String.valueOf(existing.getGUID().getUUID());
    if (currentEditionId != null && currentEditionId.equals(existingId)) {
      return;
    }
    throw conflict(EDITION_NAME_CONFLICT);
  }

  private void requireUniqueContentListName(String name, String currentContentListId) {
    Optional<IPSContentList> existing = publisherService.findContentListByName(name);
    if (existing.isEmpty() || existing.get().getGUID() == null) {
      return;
    }
    String existingId = String.valueOf(existing.get().getGUID().getUUID());
    if (currentContentListId != null && currentContentListId.equals(existingId)) {
      return;
    }
    throw conflict(CONTENT_LIST_NAME_CONFLICT);
  }

  /**
   * One scheme per context, template, and content type ({@code UIX_RXLOCSCHEME}).
   * A copy keeps the source assignment and uses another template id when that
   * triple is already stored.
   */
  static Long unusedCopyTemplateId(
      List<IPSLocationScheme> existing,
      long contentTypeId,
      long requestedTemplateId,
      long schemeKey) {
    long candidate =
        locationSchemeTripleTaken(existing, requestedTemplateId, contentTypeId)
            ? schemeKey
            : requestedTemplateId;
    for (int attempt = 0; attempt < 1000; attempt++) {
      if (!locationSchemeTripleTaken(existing, candidate, contentTypeId)) {
        return candidate;
      }
      candidate++;
    }
    throw conflict(LOCATION_SCHEME_ASSIGNMENT_CONFLICT);
  }

  private static boolean locationSchemeTripleTaken(
      List<IPSLocationScheme> existing, long templateId, long contentTypeId) {
    if (existing == null) {
      return false;
    }
    for (IPSLocationScheme scheme : existing) {
      if (scheme == null || scheme.getTemplateId() == null || scheme.getContentTypeId() == null) {
        continue;
      }
      if (scheme.getTemplateId().longValue() == templateId
          && scheme.getContentTypeId().longValue() == contentTypeId) {
        return true;
      }
    }
    return false;
  }

  private void requireUniqueLocationSchemeName(
      String contextId, String name, String currentSchemeId) {
    IPSGuid ctxGuid = guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT);
    List<IPSLocationScheme> schemes = siteManager.findSchemesByContextId(ctxGuid);
    if (schemes == null) {
      return;
    }
    for (IPSLocationScheme existing : schemes) {
      if (existing == null || isBlank(existing.getName())) {
        continue;
      }
      if (!existing.getName().equalsIgnoreCase(name)) {
        continue;
      }
      String existingId =
          existing.getGUID() != null ? String.valueOf(existing.getGUID().getUUID()) : null;
      if (currentSchemeId != null && currentSchemeId.equals(existingId)) {
        continue;
      }
      throw conflict(LOCATION_SCHEME_NAME_CONFLICT);
    }
  }

  private void requireUniqueContextName(String name, String currentContextId)
      throws PSNotFoundException {
    List<IPSPublishingContext> contexts = siteManager.findAllContexts();
    if (contexts == null) {
      return;
    }
    for (IPSPublishingContext existing : contexts) {
      if (existing == null || isBlank(existing.getName())) {
        continue;
      }
      if (!existing.getName().equalsIgnoreCase(name)) {
        continue;
      }
      String existingId =
          existing.getGUID() != null ? String.valueOf(existing.getGUID().getUUID()) : null;
      if (currentContextId != null && currentContextId.equals(existingId)) {
        continue;
      }
      throw conflict(CONTEXT_NAME_CONFLICT);
    }
  }

  private void requireUniqueDeliveryTypeName(String name, String currentDeliveryTypeId) {
    try {
      IPSDeliveryType existing = publisherService.loadDeliveryType(name);
      if (existing == null || existing.getGUID() == null) {
        return;
      }
      String existingId = String.valueOf(existing.getGUID().getUUID());
      if (currentDeliveryTypeId != null && currentDeliveryTypeId.equals(existingId)) {
        return;
      }
      throw conflict(DELIVERY_TYPE_NAME_CONFLICT);
    } catch (PSNotFoundException e) {
      // name is free
    }
  }

  /** Null leaves the stored priority. Any other value must be 1 through 5. */
  private static void requireEditionPriority(Integer priority) {
    if (priority == null) {
      return;
    }
    if (IPSEdition.Priority.findByValue(priority).isEmpty()) {
      throw badRequest(EDITION_PRIORITY_OUT_OF_RANGE);
    }
  }

  private static WebApplicationException badRequest(String msg) {
    return new WebApplicationException(msg, Response.Status.BAD_REQUEST);
  }

  private static WebApplicationException conflict(String msg) {
    return new WebApplicationException(msg, Response.Status.CONFLICT);
  }

  private static WebApplicationException notFound(String msg) {
    return new WebApplicationException(msg, Response.Status.NOT_FOUND);
  }

  private WebApplicationException internalError(Exception e) {
    log.error(PSExceptionUtils.getMessageForLog(e));
    log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    return new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
  }
}
