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
import com.percussion.services.guidmgr.PSGuidHelper;
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
import com.percussion.services.sitemgr.data.PSLocationScheme;
import com.percussion.services.sitemgr.data.PSLocationSchemeParameter;
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
  /** Matches {@code RXCONTENTLIST.GENERATOR} VARCHAR(256). */
  static final int MAX_CONTENT_LIST_GENERATOR_LENGTH = 256;

  static final String CONTENT_LIST_GENERATOR_REQUIRED = "Content list generator is required";

  static final String CONTENT_LIST_GENERATOR_TOO_LONG =
      "Content list generator must be 256 characters or fewer";

  static final String CONTENT_LIST_GENERATOR_LEGACY =
      "A legacy content list does not use a generator";
  /** Matches {@code RXCONTENTLIST.URL} VARCHAR(2100). */
  static final int MAX_CONTENT_LIST_URL_LENGTH = 2100;

  static final String CONTENT_LIST_URL_REQUIRED = "Content list URL is required";

  static final String CONTENT_LIST_URL_TOO_LONG =
      "Content list URL must be 2100 characters or fewer";

  static final String CONTENT_LIST_URL_MODERN =
      "A modern content list does not use a legacy URL";
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
  /** Matches {@code PSX_DELIVERY_TYPE.BEAN_NAME} VARCHAR(255). */
  static final int MAX_DELIVERY_TYPE_BEAN_NAME_LENGTH = 255;

  static final String DELIVERY_TYPE_BEAN_NAME_REQUIRED = "Bean name is required";

  static final String DELIVERY_TYPE_BEAN_NAME_TOO_LONG =
      "Delivery type bean name must be 255 characters or fewer";
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
  /** Matches {@code RXLOCATIONSCHEME.GENERATOR} VARCHAR(255). */
  static final int MAX_LOCATION_SCHEME_GENERATOR_LENGTH = 255;

  static final String LOCATION_SCHEME_GENERATOR_REQUIRED =
      "Location scheme generator is required";

  static final String LOCATION_SCHEME_GENERATOR_TOO_LONG =
      "Location scheme generator must be 255 characters or fewer";
  /** Matches {@code RXLOCATIONSCHEME.DESCRIPTION} VARCHAR(255). */
  static final int MAX_LOCATION_SCHEME_DESCRIPTION_LENGTH = 255;

  static final String LOCATION_SCHEME_DESCRIPTION_TOO_LONG =
      "Location scheme description must be 255 characters or fewer";

  /**
   * {@code RXLOCATIONSCHEME.CONTENTTYPEID} is a required positive id. Zero, a negative value, or
   * a blank field must not replace the stored id.
   */
  static final String LOCATION_SCHEME_CONTENT_TYPE_INVALID =
      "Location scheme content type must be a number";

  /**
   * {@code RXLOCATIONSCHEME.TEMPLATEID} is a required positive id when the field is sent. Zero or
   * a negative value must not replace the stored id. A blank field is omitted by the client.
   */
  static final String LOCATION_SCHEME_TEMPLATE_INVALID =
      "Location scheme template must be a number";
  /** Matches {@code RXLOCATIONSCHEMEPARAMS.NAME} VARCHAR(50). */
  static final int MAX_LOCATION_SCHEME_PARAMETER_NAME_LENGTH = 50;

  /** Matches {@code RXLOCATIONSCHEMEPARAMS.TYPE} VARCHAR(50). */
  static final int MAX_LOCATION_SCHEME_PARAMETER_TYPE_LENGTH = 50;

  static final String LOCATION_SCHEME_PARAMETER_ONE_REQUIRED =
      "Add one location scheme parameter at a time";

  static final String LOCATION_SCHEME_PARAMETER_NAME_REQUIRED = "Parameter name is required";

  static final String LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG =
      "Parameter name must be 50 characters or fewer";

  static final String LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED = "Parameter type is required";

  static final String LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG =
      "Parameter type must be 50 characters or fewer";

  static final String LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED = "Parameter value is required";

  static final String LOCATION_SCHEME_PARAMETER_EXISTS =
      "Parameter name already exists on this scheme";

  static final String LOCATION_SCHEME_PARAMETER_REMOVE_ONE_REQUIRED =
      "Remove one location scheme parameter at a time";

  static final String LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME =
      "Parameter is not on this scheme";

  static final String LOCATION_SCHEME_PARAMETER_ADD_AND_REMOVE =
      "Cannot add and remove a location scheme parameter in one update";

  static final String LOCATION_SCHEME_PARAMETER_VALUE_ONE_REQUIRED =
      "Change one location scheme parameter value at a time";

  static final String LOCATION_SCHEME_PARAMETER_VALUE_NOT_WITH_ADD_OR_REMOVE =
      "Cannot change a location scheme parameter value while adding or removing one";

  static final String LOCATION_SCHEME_PARAMETER_TYPE_ONE_REQUIRED =
      "Change one location scheme parameter type at a time";

  static final String LOCATION_SCHEME_PARAMETER_TYPE_NOT_WITH_OTHER =
      "Cannot change a location scheme parameter type while adding, removing, or changing a value";

  static final String LOCATION_SCHEME_PARAMETER_SEQUENCE_ONE_REQUIRED =
      "Change one location scheme parameter sequence at a time";

  static final String LOCATION_SCHEME_PARAMETER_SEQUENCE_NOT_WITH_OTHER =
      "Cannot change a location scheme parameter sequence while adding, removing, or changing a value or type";

  static final String LOCATION_SCHEME_PARAMETER_SEQUENCE_INVALID =
      "Parameter sequence must be a number";

  static final String LOCATION_SCHEME_PARAMETER_NAME_ONE_REQUIRED =
      "Rename one location scheme parameter at a time";

  static final String LOCATION_SCHEME_PARAMETER_NAME_NOT_WITH_OTHER =
      "Cannot rename a location scheme parameter while adding, removing, or changing a value, type, or sequence";

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
  /** Matches {@code RXASSEMBLERPROPERTIES.PROPERTYNAME} VARCHAR(50). */
  static final int MAX_CONTEXT_VARIABLE_NAME_LENGTH = 50;

  static final String CONTEXT_VARIABLE_NAME_REQUIRED = "Context variable name is required";

  static final String CONTEXT_VARIABLE_NAME_TOO_LONG =
      "Context variable name must be 50 characters or fewer";
  /** Matches {@code RXASSEMBLERPROPERTIES.PROPERTYVALUE} VARCHAR(255). */
  static final int MAX_CONTEXT_VARIABLE_VALUE_LENGTH = 255;

  static final String CONTEXT_VARIABLE_VALUE_REQUIRED = "Context variable value is required";

  static final String CONTEXT_VARIABLE_VALUE_TOO_LONG =
      "Context variable value must be 255 characters or fewer";

  static final String CONTEXT_VARIABLE_EXISTS = "Context variable already exists";

  /**
   * A value change or delete names a variable that is not stored on this context. The request does
   * not create it and does not remove another variable.
   */
  static final String CONTEXT_VARIABLE_NOT_LISTED = "Context variable is not listed";

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
   * {@link #MAX_CONTENT_LIST_DESCRIPTION_LENGTH} is HTTP 400 and writes nothing. A generator-only
   * body leaves the name, description, type, URL, and item filter stored. A blank generator, a
   * generator longer than {@link #MAX_CONTENT_LIST_GENERATOR_LENGTH}, or a generator sent for a
   * legacy list is HTTP 400 and writes nothing, including the legacy URL. A URL-only body on a
   * legacy list leaves the name, description, type, and item filter stored. A blank URL, a URL
   * longer than {@link #MAX_CONTENT_LIST_URL_LENGTH}, or a URL sent for a modern list is HTTP 400
   * and writes nothing.
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
      // Reject a blank or overlong bean before any field is written. A null bean is omitted
      // so a name-only or description-only update leaves the stored bean. Blank does not clear it.
      String nextBeanName = null;
      if (body.getBeanName() != null) {
        nextBeanName = body.getBeanName().trim();
        if (nextBeanName.isEmpty()) {
          throw badRequest(DELIVERY_TYPE_BEAN_NAME_REQUIRED);
        }
        if (nextBeanName.length() > MAX_DELIVERY_TYPE_BEAN_NAME_LENGTH) {
          throw badRequest(DELIVERY_TYPE_BEAN_NAME_TOO_LONG);
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
      if (nextBeanName != null) {
        t.setBeanName(nextBeanName);
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

  /**
   * Create one context variable, or when {@code updateValue} is true replace the value of one that
   * is already listed. A create whose name is already stored is HTTP 409 and writes nothing. A
   * value change whose name is not stored is HTTP 409 and does not create it. A blank name or
   * value, or a name or value longer than its column, is HTTP 400 and writes nothing. A blank
   * value does not clear a stored variable. The name stays on a value change. Other variables on
   * the context stay.
   */
  @PUT
  @Path("/sites/{siteId}/properties")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSitePropertyDto putSiteProperty(
      @PathParam("siteId") String siteId, PSSitePropertyDto body) {
    requireDesignWrite();
    requireSiteManager();
    requireNonBlank(siteId, "siteId");
    if (body == null) {
      throw badRequest("name and contextId are required");
    }
    String name = body.getName() == null ? "" : body.getName().trim();
    String contextId = body.getContextId() == null ? "" : body.getContextId().trim();
    String value = body.getValue() == null ? "" : body.getValue().trim();
    boolean updateValue = Boolean.TRUE.equals(body.getUpdateValue());
    requireContextVariableFields(name, contextId, value);
    try {
      IPSSite site = siteManager.loadSiteModifiable(toSiteGuid(siteId));
      IPSGuid ctx = guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT);
      String writtenName = contextVariableNameToWrite(site, ctx, name, updateValue);
      site.setProperty(writtenName, ctx, value);
      siteManager.saveSite(site);
      PSSitePropertyDto out = new PSSitePropertyDto();
      out.setName(writtenName);
      out.setContextId(contextId);
      out.setValue(site.getProperty(writtenName, ctx));
      return out;
    } catch (PSNotFoundException e) {
      throw notFound("Site not found");
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  /**
   * Blank or overlong name or value is HTTP 400. A blank value does not clear a stored variable.
   * Does not load or save the site.
   */
  private static void requireContextVariableFields(String name, String contextId, String value) {
    if (name.isEmpty()) {
      throw badRequest(CONTEXT_VARIABLE_NAME_REQUIRED);
    }
    if (contextId.isEmpty()) {
      throw badRequest("contextId is required");
    }
    if (value.isEmpty()) {
      throw badRequest(CONTEXT_VARIABLE_VALUE_REQUIRED);
    }
    if (name.length() > MAX_CONTEXT_VARIABLE_NAME_LENGTH) {
      throw badRequest(CONTEXT_VARIABLE_NAME_TOO_LONG);
    }
    if (value.length() > MAX_CONTEXT_VARIABLE_VALUE_LENGTH) {
      throw badRequest(CONTEXT_VARIABLE_VALUE_TOO_LONG);
    }
  }

  /**
   * Name to pass to {@code setProperty}. A value change uses the stored spelling and is HTTP 409
   * when the name is not listed, so it does not create a variable. A create is HTTP 409 when the
   * name is already stored. Does not change the site.
   */
  private static String contextVariableNameToWrite(
      IPSSite site, IPSGuid contextId, String name, boolean updateValue) {
    String storedName = storedContextVariableName(site, contextId, name);
    if (updateValue) {
      if (storedName == null) {
        throw conflict(CONTEXT_VARIABLE_NOT_LISTED);
      }
      return storedName;
    }
    if (storedName != null) {
      throw conflict(CONTEXT_VARIABLE_EXISTS);
    }
    return name;
  }

  /**
   * Stored spelling of {@code name} on this context, or {@code null} when it is not listed. Does
   * not change the site. A blank stored name does not match. Comparison is the trimmed spelling,
   * so a second variable on the context is left alone.
   */
  private static String storedContextVariableName(IPSSite site, IPSGuid contextId, String name) {
    var existingNames = site.getPropertyNames(contextId);
    if (existingNames == null) {
      return null;
    }
    for (String existingName : existingNames) {
      if (existingName != null && name.equals(existingName.trim())) {
        return existingName;
      }
    }
    return null;
  }

  /**
   * Delete one context variable. A blank name or context, or a name longer than its column, is HTTP
   * 400 and saves nothing. HTTP 403 when the caller is not Admin or Designer. HTTP 409 when the
   * name is not listed on this context, and nothing is saved. A successful delete removes only that
   * stored name. Other variables on the context stay.
   */
  @DELETE
  @Path("/sites/{siteId}/properties")
  public void deleteSiteProperty(
      @PathParam("siteId") String siteId,
      @QueryParam("name") String name,
      @QueryParam("contextId") String contextId) {
    requireDesignWrite();
    requireSiteManager();
    requireNonBlank(siteId, "siteId");
    String trimmedName = name == null ? "" : name.trim();
    String trimmedContext = contextId == null ? "" : contextId.trim();
    if (trimmedName.isEmpty()) {
      throw badRequest(CONTEXT_VARIABLE_NAME_REQUIRED);
    }
    if (trimmedContext.isEmpty()) {
      throw badRequest("contextId is required");
    }
    if (trimmedName.length() > MAX_CONTEXT_VARIABLE_NAME_LENGTH) {
      throw badRequest(CONTEXT_VARIABLE_NAME_TOO_LONG);
    }
    try {
      IPSSite site = siteManager.loadSiteModifiable(toSiteGuid(siteId));
      IPSGuid ctx = guidManager.makeGuid(trimmedContext, PSTypeEnum.CONTEXT);
      String storedName = storedContextVariableName(site, ctx, trimmedName);
      if (storedName == null) {
        throw conflict(CONTEXT_VARIABLE_NOT_LISTED);
      }
      site.removeProperty(storedName, ctx);
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
        // Include parameters so Design can list a parameter that was stored.
        out.add(toSchemeSummary(scheme, true));
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
      assignMissingParameterIds(scheme);
      siteManager.saveScheme(scheme);
      return toSchemeSummary(scheme, true);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  /**
   * Update one location scheme. A generator-only body leaves the name, description, content type,
   * template, context, and parameters stored. A blank or whitespace generator, or one longer than
   * {@link #MAX_LOCATION_SCHEME_GENERATOR_LENGTH}, is HTTP 400 and writes nothing. Omitting the
   * generator leaves it stored so a rename does not change it. A description-only body leaves the
   * name, generator, content type, template, context, and parameters stored. A blank description
   * clears it. A description longer than {@link #MAX_LOCATION_SCHEME_DESCRIPTION_LENGTH} is HTTP
   * 400 and writes nothing. Omitting the description leaves it stored. When {@code addParameter}
   * is true, the body carries exactly one parameter to append. Stored parameters are not removed
   * or rewritten. Name, generator, description, content type, and template change only when those
   * fields are present. A content-type-only body leaves the name, generator, description,
   * template, context, and parameters stored. A content type that is not a positive number is
   * HTTP 400 and writes nothing. Omitting the content type leaves the stored id. A content type
   * that is already stored for the same context and template ({@code UIX_RXLOCSCHEME}) is HTTP
   * 409 and writes nothing. A template-only body leaves the name, generator, description, content
   * type, context, and parameters stored. A template that is not a positive number is HTTP 400
   * and writes nothing. Omitting the template leaves the stored id. A template that is already
   * stored for the same context and content type ({@code UIX_RXLOCSCHEME}) is HTTP 409 and writes
   * nothing. A blank parameter name, type, or value, or a name or type longer than
   * its column, is HTTP 400 and writes nothing. A parameter name that already exists on the scheme
   * is HTTP 409 and writes nothing. When {@code removeParameter} is true, the body carries exactly
   * one parameter name to remove. Other stored parameters stay, including when this removal leaves
   * none. The scheme itself is not deleted. Name, generator, description, content type, and
   * template change only when those fields are present. A blank name, a name longer than its
   * column, or any count other than one is HTTP 400 and writes nothing. A name that is not stored
   * on the scheme is HTTP 409 and writes nothing. When {@code updateParameterValue} is true, the
   * body carries exactly one stored parameter name and a new value. That parameter's name, type,
   * and sequence stay. Other stored parameters stay. The stored set is not replaced. Name,
   * generator, description, content type, and template change only when those fields are present.
   * A blank or whitespace value does not clear the stored value: it is HTTP 400 and writes
   * nothing. A blank name, a name longer than its column, or any count other than one is HTTP 400
   * and writes nothing. A name that is not stored is HTTP 409 and writes nothing. When
   * {@code updateParameterType} is true, the body carries exactly one stored parameter name and a
   * new type. That parameter's name, value, and sequence stay. Other stored parameters stay. The
   * stored set is not replaced. Name, generator, description, content type, and template change
   * only when those fields are present. A blank or whitespace type does not clear the stored type:
   * it is HTTP 400 and writes nothing. A type longer than its column is HTTP 400 and writes
   * nothing. A blank name, a name longer than its column, or any count other than one is HTTP 400
   * and writes nothing. A name that is not stored is HTTP 409 and writes nothing. When
   * {@code updateParameterSequence} is true, the body carries exactly one stored parameter name
   * and a new sequence. That parameter's name, type, and value stay. Other stored parameters stay,
   * including their sequences. The stored set is not replaced. Name, generator, description,
   * content type, and template change only when those fields are present. A missing sequence is
   * HTTP 400 and writes nothing. A blank name, a name longer than its column, or any count other
   * than one is HTTP 400 and writes nothing. A name that is not stored is HTTP 409 and writes
   * nothing. When {@code updateParameterName} is true, the body carries exactly one stored
   * parameter name and {@code newName}. That parameter's type, value, and sequence stay. Other
   * stored parameters stay. The stored set is not replaced. Name, generator, description, content
   * type, and template change only when those fields are present. A blank or whitespace new name
   * does not clear the stored name: it is HTTP 400 and writes nothing. A new name longer than its
   * column is HTTP 400 and writes nothing. A new name already stored on a different parameter is
   * HTTP 409 and writes nothing. A blank stored name, a stored name longer than its column, or
   * any count other than one is HTTP 400 and writes nothing. A stored name that is not on the
   * scheme is HTTP 409 and writes nothing. A type, value, or sequence sent with the rename is not
   * applied. Add, remove, a value change, a type change, a sequence change, and a rename cannot
   * be combined.
   */
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
    rejectCombinedSchemeParameterModes(body);
    try {
      IPSLocationScheme scheme =
          siteManager.loadSchemeModifiable(
              guidManager.makeGuid(schemeId, PSTypeEnum.LOCATION_SCHEME));
      // Validate the parameter change before any field is written. Apply it after the other fields.
      SchemeParameterMutation parameterMutation = prepareSchemeParameterMutation(scheme, body);
      // Reject a bad generator, description, content type, or template before any field is written.
      SchemeTextChange generatorChange = prepareLocationSchemeGenerator(body.getGenerator());
      SchemeTextChange descriptionChange = prepareLocationSchemeDescription(body.getDescription());
      String contextId =
          !isBlank(body.getContextId())
              ? body.getContextId().trim()
              : (scheme.getContextId() != null
                  ? String.valueOf(scheme.getContextId().getUUID())
                  : null);
      rejectLocationSchemeContentType(scheme, body, contextId, schemeId);
      rejectLocationSchemeTemplate(scheme, body, contextId, schemeId);
      if (!isBlank(body.getName()) && contextId != null) {
        requireUniqueLocationSchemeName(contextId, body.getName().trim(), schemeId);
      }
      if (!isBlank(body.getName())) {
        scheme.setName(body.getName().trim());
      }
      if (generatorChange.apply()) {
        scheme.setGenerator(generatorChange.value());
      }
      if (descriptionChange.apply()) {
        scheme.setDescription(descriptionChange.value());
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
      applySchemeParameterMutation(scheme, parameterMutation);
      assignMissingParameterIds(scheme);
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
    // Reject a bad generator before any field is written so 400 leaves the stored row.
    String nextGenerator = null;
    boolean applyGenerator = !isCreate && body.getGenerator() != null;
    if (applyGenerator) {
      nextGenerator = body.getGenerator().trim();
      if (nextGenerator.isEmpty()) {
        throw badRequest(CONTENT_LIST_GENERATOR_REQUIRED);
      }
      if (nextGenerator.length() > MAX_CONTENT_LIST_GENERATOR_LENGTH) {
        throw badRequest(CONTENT_LIST_GENERATOR_TOO_LONG);
      }
      if (cl.isLegacy()) {
        throw badRequest(CONTENT_LIST_GENERATOR_LEGACY);
      }
    }
    // Reject a bad legacy URL before any field is written so 400 leaves the stored row.
    // Create still ignores a blank URL (the full editor may omit it). An omitted URL on update
    // leaves the stored URL.
    String nextUrl = null;
    boolean applyUrl = !isCreate && body.getUrl() != null;
    if (applyUrl) {
      nextUrl = body.getUrl().trim();
      if (nextUrl.isEmpty()) {
        throw badRequest(CONTENT_LIST_URL_REQUIRED);
      }
      if (nextUrl.length() > MAX_CONTENT_LIST_URL_LENGTH) {
        throw badRequest(CONTENT_LIST_URL_TOO_LONG);
      }
      if (!cl.isLegacy()) {
        throw badRequest(CONTENT_LIST_URL_MODERN);
      }
    }
    if (!isBlank(body.getName()) && !isCreate) {
      cl.setName(body.getName().trim());
    }
    if (applyDescription) {
      cl.setDescription(nextDescription);
    }
    if (applyGenerator) {
      cl.setGenerator(nextGenerator);
    } else if (isCreate && body.getGenerator() != null) {
      cl.setGenerator(body.getGenerator());
    }
    if (applyUrl) {
      cl.setUrl(nextUrl);
    } else if (isCreate && body.getUrl() != null && !body.getUrl().isBlank()) {
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
   * Add and remove cannot be combined. A value change cannot be combined with add or remove. A
   * type change cannot be combined with add, remove, or a value change. A sequence change cannot
   * be combined with add, remove, a value change, or a type change. A rename cannot be combined
   * with add, remove, a value change, a type change, or a sequence change. Any mix is HTTP 400
   * and writes nothing.
   */
  private void rejectCombinedSchemeParameterModes(PSLocationSchemeSummary body) {
    boolean add = Boolean.TRUE.equals(body.getAddParameter());
    boolean remove = Boolean.TRUE.equals(body.getRemoveParameter());
    boolean value = Boolean.TRUE.equals(body.getUpdateParameterValue());
    boolean type = Boolean.TRUE.equals(body.getUpdateParameterType());
    boolean sequence = Boolean.TRUE.equals(body.getUpdateParameterSequence());
    boolean name = Boolean.TRUE.equals(body.getUpdateParameterName());
    if (add && remove) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_ADD_AND_REMOVE);
    }
    if (value && (add || remove)) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_VALUE_NOT_WITH_ADD_OR_REMOVE);
    }
    if (type && (add || remove || value)) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_TYPE_NOT_WITH_OTHER);
    }
    if (sequence && (add || remove || value || type)) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_SEQUENCE_NOT_WITH_OTHER);
    }
    if (name && (add || remove || value || type || sequence)) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_NOT_WITH_OTHER);
    }
  }

  /**
   * Choose the one parameter change for this request. Does not change the scheme. Combined flags
   * are rejected earlier. No flag means the stored set is replaced from the body list.
   */
  private SchemeParameterMutation prepareSchemeParameterMutation(
      IPSLocationScheme scheme, PSLocationSchemeSummary body) {
    if (Boolean.TRUE.equals(body.getUpdateParameterValue())) {
      return SchemeParameterMutation.valueChange(
          prepareSchemeParameterValueChange(scheme, body.getParameters()));
    }
    if (Boolean.TRUE.equals(body.getUpdateParameterType())) {
      return SchemeParameterMutation.typeChange(
          prepareSchemeParameterTypeChange(scheme, body.getParameters()));
    }
    if (Boolean.TRUE.equals(body.getUpdateParameterSequence())) {
      return SchemeParameterMutation.sequenceChange(
          prepareSchemeParameterSequenceChange(scheme, body.getParameters()));
    }
    if (Boolean.TRUE.equals(body.getUpdateParameterName())) {
      return SchemeParameterMutation.nameChange(
          prepareSchemeParameterNameChange(scheme, body.getParameters()));
    }
    if (Boolean.TRUE.equals(body.getRemoveParameter())) {
      return SchemeParameterMutation.removal(
          prepareSchemeParameterRemoval(scheme, body.getParameters()));
    }
    if (Boolean.TRUE.equals(body.getAddParameter())) {
      return SchemeParameterMutation.addition(
          prepareSchemeParameterAddition(scheme, body.getParameters()));
    }
    return SchemeParameterMutation.replace(body.getParameters());
  }

  /** Apply a change already validated by {@link #prepareSchemeParameterMutation}. */
  private void applySchemeParameterMutation(
      IPSLocationScheme scheme, SchemeParameterMutation mutation) {
    SchemeParameterValueChange valueChange = mutation.valueChange();
    if (valueChange != null) {
      scheme.addParameter(
          valueChange.name(), valueChange.sequence(), valueChange.type(), valueChange.value());
      return;
    }
    SchemeParameterTypeChange typeChange = mutation.typeChange();
    if (typeChange != null) {
      scheme.addParameter(
          typeChange.name(), typeChange.sequence(), typeChange.type(), typeChange.value());
      return;
    }
    SchemeParameterSequenceChange sequenceChange = mutation.sequenceChange();
    if (sequenceChange != null) {
      scheme.addParameter(
          sequenceChange.name(),
          sequenceChange.sequence(),
          sequenceChange.type(),
          sequenceChange.value());
      return;
    }
    SchemeParameterNameChange nameChange = mutation.nameChange();
    if (nameChange != null) {
      applySchemeParameterNameChange(scheme, nameChange);
      return;
    }
    SchemeParameterAddition addition = mutation.addition();
    if (addition != null) {
      scheme.addParameter(addition.name(), addition.sequence(), addition.type(), addition.value());
      return;
    }
    SchemeParameterRemoval removal = mutation.removal();
    if (removal != null) {
      scheme.removeParameter(removal.name());
      return;
    }
    applySchemeParameters(scheme, mutation.parameters(), false);
  }

  /**
   * Exactly one of the parameter fields is set. {@code parameters} is the replace-all list and is
   * null for add, remove, value change, type change, sequence change, and rename.
   */
  private record SchemeParameterMutation(
      SchemeParameterValueChange valueChange,
      SchemeParameterTypeChange typeChange,
      SchemeParameterSequenceChange sequenceChange,
      SchemeParameterNameChange nameChange,
      SchemeParameterAddition addition,
      SchemeParameterRemoval removal,
      List<PSSchemeParameter> parameters) {

    static SchemeParameterMutation valueChange(SchemeParameterValueChange valueChange) {
      return new SchemeParameterMutation(valueChange, null, null, null, null, null, null);
    }

    static SchemeParameterMutation typeChange(SchemeParameterTypeChange typeChange) {
      return new SchemeParameterMutation(null, typeChange, null, null, null, null, null);
    }

    static SchemeParameterMutation sequenceChange(SchemeParameterSequenceChange sequenceChange) {
      return new SchemeParameterMutation(null, null, sequenceChange, null, null, null, null);
    }

    static SchemeParameterMutation nameChange(SchemeParameterNameChange nameChange) {
      return new SchemeParameterMutation(null, null, null, nameChange, null, null, null);
    }

    static SchemeParameterMutation addition(SchemeParameterAddition addition) {
      return new SchemeParameterMutation(null, null, null, null, addition, null, null);
    }

    static SchemeParameterMutation removal(SchemeParameterRemoval removal) {
      return new SchemeParameterMutation(null, null, null, null, null, removal, null);
    }

    static SchemeParameterMutation replace(List<PSSchemeParameter> parameters) {
      return new SchemeParameterMutation(null, null, null, null, null, null, parameters);
    }
  }

  /**
   * Validate one parameter to append. Does not change the scheme. Blank name, type, or value, a
   * name or type longer than its column, or any count other than one is HTTP 400. A name already
   * stored on the scheme is HTTP 409.
   */
  private SchemeParameterAddition prepareSchemeParameterAddition(
      IPSLocationScheme scheme, List<PSSchemeParameter> parameters) {
    if (parameters == null || parameters.size() != 1 || parameters.get(0) == null) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_ONE_REQUIRED);
    }
    PSSchemeParameter incoming = parameters.get(0);
    String name = incoming.getName() == null ? "" : incoming.getName().trim();
    String value = incoming.getValue() == null ? "" : incoming.getValue().trim();
    String type = incoming.getType() == null ? "" : incoming.getType().trim();
    if (name.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_REQUIRED);
    }
    if (name.length() > MAX_LOCATION_SCHEME_PARAMETER_NAME_LENGTH) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG);
    }
    if (type.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED);
    }
    if (type.length() > MAX_LOCATION_SCHEME_PARAMETER_TYPE_LENGTH) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG);
    }
    if (value.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED);
    }
    List<String> existingNames = scheme.getParameterNames();
    if (existingNames != null) {
      for (String existingName : existingNames) {
        if (existingName != null && name.equals(existingName.trim())) {
          throw conflict(LOCATION_SCHEME_PARAMETER_EXISTS);
        }
      }
    }
    return new SchemeParameterAddition(name, nextParameterSequence(scheme), type, value);
  }

  /** Next sequence is one past the highest stored sequence, or zero when none are stored. */
  private int nextParameterSequence(IPSLocationScheme scheme) {
    int next = 0;
    List<String> existingNames = scheme.getParameterNames();
    if (existingNames == null) {
      return next;
    }
    for (String existingName : existingNames) {
      if (existingName == null) {
        continue;
      }
      Integer sequence = scheme.getParameterSequence(existingName);
      if (sequence != null && sequence >= next && sequence < Integer.MAX_VALUE) {
        next = sequence + 1;
      }
    }
    return next;
  }

  private record SchemeParameterAddition(String name, int sequence, String type, String value) {}

  /**
   * Validate one parameter name to remove. Does not change the scheme. Any count other than one,
   * a blank name, or a name longer than its column is HTTP 400. A name that is not stored is HTTP
   * 409. The returned name is the stored spelling so {@link IPSLocationScheme#removeParameter}
   * matches that row and leaves every other parameter alone.
   */
  private SchemeParameterRemoval prepareSchemeParameterRemoval(
      IPSLocationScheme scheme, List<PSSchemeParameter> parameters) {
    if (parameters == null || parameters.size() != 1 || parameters.get(0) == null) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_REMOVE_ONE_REQUIRED);
    }
    String name =
        parameters.get(0).getName() == null ? "" : parameters.get(0).getName().trim();
    if (name.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_REQUIRED);
    }
    if (name.length() > MAX_LOCATION_SCHEME_PARAMETER_NAME_LENGTH) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG);
    }
    List<String> existingNames = scheme.getParameterNames();
    if (existingNames != null) {
      for (String existingName : existingNames) {
        if (existingName != null && name.equals(existingName.trim())) {
          return new SchemeParameterRemoval(existingName);
        }
      }
    }
    throw conflict(LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME);
  }

  private record SchemeParameterRemoval(String name) {}

  /**
   * Validate one stored parameter value to replace. Does not change the scheme. Any count other
   * than one, a blank name, or a name longer than its column is HTTP 400. A blank or whitespace
   * value is HTTP 400 and does not clear the stored value. A name that is not stored is HTTP 409.
   * The returned name, type, and sequence are the stored ones. A type or sequence on the request
   * is ignored so this update does not replace the parameter row.
   */
  private SchemeParameterValueChange prepareSchemeParameterValueChange(
      IPSLocationScheme scheme, List<PSSchemeParameter> parameters) {
    if (parameters == null || parameters.size() != 1 || parameters.get(0) == null) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_VALUE_ONE_REQUIRED);
    }
    PSSchemeParameter incoming = parameters.get(0);
    String name = incoming.getName() == null ? "" : incoming.getName().trim();
    String value = incoming.getValue() == null ? "" : incoming.getValue().trim();
    if (name.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_REQUIRED);
    }
    if (name.length() > MAX_LOCATION_SCHEME_PARAMETER_NAME_LENGTH) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG);
    }
    if (value.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED);
    }
    List<String> existingNames = scheme.getParameterNames();
    if (existingNames != null) {
      for (String existingName : existingNames) {
        if (existingName != null && name.equals(existingName.trim())) {
          String storedType = scheme.getParameterType(existingName);
          if (isBlank(storedType)) {
            throw badRequest(LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED);
          }
          Integer storedSequence = scheme.getParameterSequence(existingName);
          int sequence = storedSequence == null ? 0 : storedSequence;
          return new SchemeParameterValueChange(existingName, sequence, storedType, value);
        }
      }
    }
    throw conflict(LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME);
  }

  private record SchemeParameterValueChange(
      String name, int sequence, String type, String value) {}

  /**
   * Validate one stored parameter type to replace. Does not change the scheme. Any count other
   * than one, a blank name, or a name longer than its column is HTTP 400. A blank or whitespace
   * type is HTTP 400 and does not clear the stored type. A type longer than its column is HTTP
   * 400. A name that is not stored is HTTP 409. The returned name, value, and sequence are the
   * stored ones. A value or sequence on the request is ignored so this update does not replace
   * the parameter row. A stored value that is blank is HTTP 400 so this update does not invent
   * one.
   */
  private SchemeParameterTypeChange prepareSchemeParameterTypeChange(
      IPSLocationScheme scheme, List<PSSchemeParameter> parameters) {
    if (parameters == null || parameters.size() != 1 || parameters.get(0) == null) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_TYPE_ONE_REQUIRED);
    }
    PSSchemeParameter incoming = parameters.get(0);
    String name = incoming.getName() == null ? "" : incoming.getName().trim();
    String type = incoming.getType() == null ? "" : incoming.getType().trim();
    if (name.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_REQUIRED);
    }
    if (name.length() > MAX_LOCATION_SCHEME_PARAMETER_NAME_LENGTH) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG);
    }
    if (type.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED);
    }
    if (type.length() > MAX_LOCATION_SCHEME_PARAMETER_TYPE_LENGTH) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG);
    }
    List<String> existingNames = scheme.getParameterNames();
    if (existingNames != null) {
      for (String existingName : existingNames) {
        if (existingName != null && name.equals(existingName.trim())) {
          String storedValue = scheme.getParameterValue(existingName);
          if (isBlank(storedValue)) {
            throw badRequest(LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED);
          }
          Integer storedSequence = scheme.getParameterSequence(existingName);
          int sequence = storedSequence == null ? 0 : storedSequence;
          return new SchemeParameterTypeChange(existingName, sequence, type, storedValue);
        }
      }
    }
    throw conflict(LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME);
  }

  private record SchemeParameterTypeChange(
      String name, int sequence, String type, String value) {}

  /**
   * Validate one stored parameter sequence to replace. Does not change the scheme. Any count other
   * than one, a blank name, or a name longer than its column is HTTP 400. A missing sequence is
   * HTTP 400 and does not clear the stored sequence. A name that is not stored is HTTP 409. The
   * returned name, type, and value are the stored ones. A type or value on the request is ignored
   * so this update does not replace the parameter row. A stored type or value that is blank is
   * HTTP 400 so this update does not invent one. Other parameters, including their sequences, are
   * not rewritten.
   */
  private SchemeParameterSequenceChange prepareSchemeParameterSequenceChange(
      IPSLocationScheme scheme, List<PSSchemeParameter> parameters) {
    if (parameters == null || parameters.size() != 1 || parameters.get(0) == null) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_SEQUENCE_ONE_REQUIRED);
    }
    PSSchemeParameter incoming = parameters.get(0);
    String name = incoming.getName() == null ? "" : incoming.getName().trim();
    if (name.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_REQUIRED);
    }
    if (name.length() > MAX_LOCATION_SCHEME_PARAMETER_NAME_LENGTH) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG);
    }
    if (incoming.getSequence() == null) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_SEQUENCE_INVALID);
    }
    int sequence = incoming.getSequence();
    List<String> existingNames = scheme.getParameterNames();
    if (existingNames != null) {
      for (String existingName : existingNames) {
        if (existingName != null && name.equals(existingName.trim())) {
          String storedType = scheme.getParameterType(existingName);
          if (isBlank(storedType)) {
            throw badRequest(LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED);
          }
          String storedValue = scheme.getParameterValue(existingName);
          if (isBlank(storedValue)) {
            throw badRequest(LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED);
          }
          return new SchemeParameterSequenceChange(existingName, sequence, storedType, storedValue);
        }
      }
    }
    throw conflict(LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME);
  }

  private record SchemeParameterSequenceChange(
      String name, int sequence, String type, String value) {}

  /**
   * Validate one stored parameter name to replace. Does not change the scheme. Any count other
   * than one, a blank stored name, or a stored name longer than its column is HTTP 400. A blank
   * or whitespace new name is HTTP 400 and does not clear the stored name. A new name longer than
   * its column is HTTP 400. A stored name that is not on the scheme is HTTP 409. A new name that
   * matches a different stored parameter is HTTP 409. The returned stored name is the spelling
   * already on the scheme. A type, value, or sequence on the request is ignored. The same name,
   * after trim, is not a duplicate of itself.
   */
  private SchemeParameterNameChange prepareSchemeParameterNameChange(
      IPSLocationScheme scheme, List<PSSchemeParameter> parameters) {
    if (parameters == null || parameters.size() != 1 || parameters.get(0) == null) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_ONE_REQUIRED);
    }
    PSSchemeParameter incoming = parameters.get(0);
    String lookup = incoming.getName() == null ? "" : incoming.getName().trim();
    String next = incoming.getNewName() == null ? "" : incoming.getNewName().trim();
    if (lookup.isEmpty() || next.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_REQUIRED);
    }
    if (lookup.length() > MAX_LOCATION_SCHEME_PARAMETER_NAME_LENGTH
        || next.length() > MAX_LOCATION_SCHEME_PARAMETER_NAME_LENGTH) {
      throw badRequest(LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG);
    }
    String storedName = storedParameterName(scheme, lookup);
    if (storedName == null) {
      throw conflict(LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME);
    }
    List<String> existingNames = scheme.getParameterNames();
    if (existingNames != null) {
      for (String existingName : existingNames) {
        if (existingName == null) {
          continue;
        }
        if (next.equals(existingName.trim()) && !storedName.equals(existingName)) {
          throw conflict(LOCATION_SCHEME_PARAMETER_EXISTS);
        }
      }
    }
    return new SchemeParameterNameChange(storedName, next);
  }

  /** Stored spelling of {@code lookup}, or null when that parameter is not on the scheme. */
  private String storedParameterName(IPSLocationScheme scheme, String lookup) {
    List<String> existingNames = scheme.getParameterNames();
    if (existingNames == null) {
      return null;
    }
    for (String existingName : existingNames) {
      if (existingName != null && lookup.equals(existingName.trim())) {
        return existingName;
      }
    }
    return null;
  }

  /**
   * Rename one parameter in place so its id, type, value, and sequence stay. A name that already
   * matches is left alone. {@link PSLocationSchemeParameter#hashCode()} is the name, so the row is
   * removed before the name changes and then put back on the same instance. Other implementations
   * remove and add, which cannot keep the id.
   */
  private void applySchemeParameterNameChange(
      IPSLocationScheme scheme, SchemeParameterNameChange change) {
    if (change.storedName().equals(change.newName())) {
      return;
    }
    if (scheme instanceof PSLocationScheme concrete) {
      PSLocationSchemeParameter target = null;
      for (PSLocationSchemeParameter param : concrete.getParameterSet()) {
        if (param != null && change.storedName().equals(param.getName())) {
          target = param;
          break;
        }
      }
      if (target == null) {
        throw conflict(LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME);
      }
      concrete.removeParameter(change.storedName());
      target.setName(change.newName());
      var rows = concrete.getParameterSet();
      rows.add(target);
      concrete.setParameterSet(rows);
      return;
    }
    String type = scheme.getParameterType(change.storedName());
    String value = scheme.getParameterValue(change.storedName());
    Integer sequence = scheme.getParameterSequence(change.storedName());
    scheme.removeParameter(change.storedName());
    scheme.addParameter(
        change.newName(),
        sequence == null ? 0 : sequence,
        type,
        value);
  }

  private record SchemeParameterNameChange(String storedName, String newName) {}

  /**
   * {@code RXLOCATIONSCHEMEPARAMS.SCHEMEPARAMID} is assigned, not generated. New rows from
   * add-one and from a full parameter replace need a next-number id before {@code persist}.
   * Mocks and other {@link IPSLocationScheme} implementations are left alone.
   */
  private void assignMissingParameterIds(IPSLocationScheme scheme) {
    if (!(scheme instanceof PSLocationScheme concrete)) {
      return;
    }
    for (PSLocationSchemeParameter param : concrete.getParameterSet()) {
      if (param != null && param.getParameterId() == null) {
        param.setParameterId(
            Math.toIntExact(PSGuidHelper.generateNextLong(PSTypeEnum.LOCATION_PROPERTY)));
      }
    }
  }

  /**
   * Replace or append scheme parameters. When {@code replaceAll} is false and parameters is null,
   * leaves existing params unchanged; when non-null, clears unknown names then sets listed ones.
   * An add-one, remove-one, value-one, type-one, sequence-one, or rename-one update does not use
   * this path.
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

  /**
   * Generator text for one update. {@code apply} is false when the field was omitted. A blank
   * generator is HTTP 400. An empty prepared value is never stored.
   */
  private static SchemeTextChange prepareLocationSchemeGenerator(String raw) {
    if (raw == null) {
      return SchemeTextChange.omit();
    }
    String next = raw.trim();
    if (next.isEmpty()) {
      throw badRequest(LOCATION_SCHEME_GENERATOR_REQUIRED);
    }
    if (next.length() > MAX_LOCATION_SCHEME_GENERATOR_LENGTH) {
      throw badRequest(LOCATION_SCHEME_GENERATOR_TOO_LONG);
    }
    return SchemeTextChange.apply(next);
  }

  /**
   * Description text for one update. {@code apply} is false when the field was omitted. Blank or
   * whitespace clears the stored description. An overlong description is HTTP 400.
   */
  private static SchemeTextChange prepareLocationSchemeDescription(String raw) {
    if (raw == null) {
      return SchemeTextChange.omit();
    }
    String next = raw.trim();
    if (next.length() > MAX_LOCATION_SCHEME_DESCRIPTION_LENGTH) {
      throw badRequest(LOCATION_SCHEME_DESCRIPTION_TOO_LONG);
    }
    if (next.isEmpty()) {
      return SchemeTextChange.apply(null);
    }
    return SchemeTextChange.apply(next);
  }

  /**
   * Reject a non-positive content type (HTTP 400) or a context/template/content-type triple that
   * another scheme already uses (HTTP 409). A null content type is omitted. Nothing is written
   * here.
   */
  private void rejectLocationSchemeContentType(
      IPSLocationScheme scheme,
      PSLocationSchemeSummary body,
      String contextId,
      String schemeId) {
    if (body.getContentTypeId() == null) {
      return;
    }
    long nextContentType = body.getContentTypeId();
    if (nextContentType <= 0) {
      throw badRequest(LOCATION_SCHEME_CONTENT_TYPE_INVALID);
    }
    Long storedTemplate = scheme.getTemplateId();
    long templateId =
        body.getTemplateId() != null
            ? body.getTemplateId()
            : (storedTemplate != null ? storedTemplate.longValue() : -1L);
    if (templateId > 0 && contextId != null) {
      requireUniqueLocationSchemeAssignment(contextId, templateId, nextContentType, schemeId);
    }
  }

  /**
   * Reject a non-positive template (HTTP 400) or a context/template/content-type triple that
   * another scheme already uses (HTTP 409). A null template is omitted. Nothing is written here.
   */
  private void rejectLocationSchemeTemplate(
      IPSLocationScheme scheme,
      PSLocationSchemeSummary body,
      String contextId,
      String schemeId) {
    if (body.getTemplateId() == null) {
      return;
    }
    long nextTemplate = body.getTemplateId();
    if (nextTemplate <= 0) {
      throw badRequest(LOCATION_SCHEME_TEMPLATE_INVALID);
    }
    Long storedContentType = scheme.getContentTypeId();
    long contentTypeId =
        body.getContentTypeId() != null
            ? body.getContentTypeId()
            : (storedContentType != null ? storedContentType.longValue() : -1L);
    if (contentTypeId > 0 && contextId != null) {
      requireUniqueLocationSchemeAssignment(contextId, nextTemplate, contentTypeId, schemeId);
    }
  }

  /** Present text change, or an omitted field that must not be written. */
  private record SchemeTextChange(boolean apply, String value) {
    static SchemeTextChange omit() {
      return new SchemeTextChange(false, null);
    }

    static SchemeTextChange apply(String value) {
      return new SchemeTextChange(true, value);
    }
  }

  /**
   * One scheme per context, template, and content type ({@code UIX_RXLOCSCHEME}). The scheme being
   * updated is not a conflict with itself. Called only after the content type is known to be a
   * positive id, and before any field is written.
   */
  private void requireUniqueLocationSchemeAssignment(
      String contextId, long templateId, long contentTypeId, String currentSchemeId) {
    IPSGuid ctxGuid = guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT);
    List<IPSLocationScheme> schemes = siteManager.findSchemesByContextId(ctxGuid);
    if (schemes == null) {
      return;
    }
    for (IPSLocationScheme existing : schemes) {
      if (existing == null
          || existing.getTemplateId() == null
          || existing.getContentTypeId() == null) {
        continue;
      }
      if (existing.getTemplateId().longValue() != templateId
          || existing.getContentTypeId().longValue() != contentTypeId) {
        continue;
      }
      String existingId =
          existing.getGUID() != null ? String.valueOf(existing.getGUID().getUUID()) : null;
      if (currentSchemeId != null && currentSchemeId.equals(existingId)) {
        continue;
      }
      throw conflict(LOCATION_SCHEME_ASSIGNMENT_CONFLICT);
    }
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
