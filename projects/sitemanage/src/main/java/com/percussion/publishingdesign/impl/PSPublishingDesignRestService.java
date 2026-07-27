/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import com.percussion.system.utils.PSSiteManageBean;
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
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

  private final IPSPublisherService publisherService;
  private final IPSGuidManager guidManager;
  private final IPSSiteManager siteManager;
  private final PSPublishingRuntimeSupport runtimeSupport;

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
    if (body == null || isBlank(body.getName()) || isBlank(body.getSiteId())) {
      throw badRequest("name and siteId are required");
    }
    try {
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
    requireNonBlank(editionId, "editionId");
    if (body == null) {
      throw badRequest("body is required");
    }
    try {
      IPSEdition edition = publisherService.loadEditionModifiable(toEditionGuid(editionId));
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
    requireNonBlank(editionId, "editionId");
    try {
      IPSEdition edition = publisherService.loadEdition(toEditionGuid(editionId));
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
    if (request == null
        || isBlank(request.getSourceEditionId())
        || isBlank(request.getTargetSiteId())) {
      throw badRequest("sourceEditionId and targetSiteId are required");
    }
    try {
      IPSEdition source = publisherService.loadEdition(toEditionGuid(request.getSourceEditionId()));
      IPSEdition copy = publisherService.createEdition();
      String newName =
          isBlank(request.getNewName()) ? source.getName() + "_copy" : request.getNewName().trim();
      copy.setName(newName);
      copy.setComment(source.getComment());
      if (source.getEditionType() != null) {
        copy.setEditionType(source.getEditionType());
      }
      if (source.getPriority() != null) {
        copy.setPriority(source.getPriority());
      }
      copy.setSiteId(toSiteGuid(request.getTargetSiteId()));
      if (source.getDisplayTitle() != null) {
        copy.setDisplayTitle(source.getDisplayTitle());
      }
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
      List<IPSEditionContentList> links =
          publisherService.loadEditionContentLists(toEditionGuid(editionId));
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
    if (body == null || isBlank(body.getName())) {
      throw badRequest("name is required");
    }
    try {
      IPSContentList cl = publisherService.createContentList(body.getName().trim());
      applyContentListFields(cl, body, true);
      publisherService.saveContentList(cl);
      return toContentListSummary(cl);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw internalError(e);
    }
  }

  @PUT
  @Path("/contentlists/{contentListId}")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContentListSummary updateContentList(
      @PathParam("contentListId") String contentListId, PSContentListSummary body) {
    requireNonBlank(contentListId, "contentListId");
    if (body == null) {
      throw badRequest("body is required");
    }
    try {
      IPSContentList cl =
          publisherService.loadContentListModifiable(toContentListGuid(contentListId));
      applyContentListFields(cl, body, false);
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
    requireNonBlank(contentListId, "contentListId");
    try {
      IPSContentList cl = publisherService.loadContentList(toContentListGuid(contentListId));
      publisherService.deleteContentLists(List.of(cl));
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
    if (body == null || isBlank(body.getName()) || isBlank(body.getBeanName())) {
      throw badRequest("name and beanName are required");
    }
    try {
      IPSDeliveryType t = publisherService.createDeliveryType();
      t.setName(body.getName().trim());
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
    requireNonBlank(deliveryTypeId, "deliveryTypeId");
    if (body == null) {
      throw badRequest("body is required");
    }
    try {
      IPSGuid guid = guidManager.makeGuid(deliveryTypeId, PSTypeEnum.DELIVERY_TYPE);
      IPSDeliveryType t = publisherService.loadDeliveryTypeModifiable(guid);
      if (!isBlank(body.getName())) {
        t.setName(body.getName().trim());
      }
      if (!isBlank(body.getBeanName())) {
        t.setBeanName(body.getBeanName().trim());
      }
      if (body.getDescription() != null) {
        t.setDescription(body.getDescription());
      }
      t.setUnpublishingRequiresAssembly(body.isUnpublishingRequiresAssembly());
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
    requireNonBlank(deliveryTypeId, "deliveryTypeId");
    try {
      IPSGuid guid = guidManager.makeGuid(deliveryTypeId, PSTypeEnum.DELIVERY_TYPE);
      IPSDeliveryType t = publisherService.loadDeliveryType(guid);
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
    requireNonBlank(editionId, "editionId");
    if (body == null || isBlank(body.getContentListId()) || isBlank(body.getDeliveryContextId())) {
      throw badRequest("contentListId and deliveryContextId are required");
    }
    try {
      IPSGuid edGuid = toEditionGuid(editionId);
      publisherService.loadEdition(edGuid); // existence
      IPSGuid clGuid = toContentListGuid(body.getContentListId());
      IPSContentList cl = publisherService.loadContentList(clGuid);
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
    requireNonBlank(editionId, "editionId");
    requireNonBlank(contentListId, "contentListId");
    try {
      IPSGuid edGuid = toEditionGuid(editionId);
      IPSGuid clGuid = toContentListGuid(contentListId);
      List<IPSEditionContentList> links = publisherService.loadEditionContentLists(edGuid);
      boolean removed = false;
      for (IPSEditionContentList link : links) {
        if (link.getContentListId() != null
            && link.getContentListId().longValue() == clGuid.longValue()) {
          publisherService.deleteEditionContentList(link);
          removed = true;
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
    if (body == null || isBlank(body.getName())) {
      throw badRequest("name is required");
    }
    try {
      IPSPublishingContext ctx = siteManager.createContext();
      ctx.setName(body.getName().trim());
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

  @PUT
  @Path("/contexts/{contextId}")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSContextSummary updateContext(
      @PathParam("contextId") String contextId, PSContextSummary body) {
    requireSiteManager();
    requireNonBlank(contextId, "contextId");
    if (body == null) {
      throw badRequest("body is required");
    }
    try {
      IPSPublishingContext ctx =
          siteManager.loadContextModifiable(guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT));
      if (!isBlank(body.getName())) {
        ctx.setName(body.getName().trim());
      }
      if (body.getDescription() != null) {
        ctx.setDescription(body.getDescription());
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
    requireSiteManager();
    requireNonBlank(contextId, "contextId");
    try {
      IPSPublishingContext ctx =
          siteManager.loadContext(guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT));
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
    requireNonBlank(contextId, "contextId");
    if (body == null || isBlank(body.getName()) || isBlank(body.getGenerator())) {
      throw badRequest("name and generator are required");
    }
    try {
      IPSLocationScheme scheme = siteManager.createScheme();
      scheme.setName(body.getName().trim());
      scheme.setGenerator(body.getGenerator().trim());
      scheme.setContextId(guidManager.makeGuid(contextId, PSTypeEnum.CONTEXT));
      if (body.getDescription() != null) {
        scheme.setDescription(body.getDescription());
      }
      if (body.getContentTypeId() != null) {
        scheme.setContentTypeId(body.getContentTypeId());
      }
      if (body.getTemplateId() != null) {
        scheme.setTemplateId(body.getTemplateId());
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
    requireNonBlank(schemeId, "schemeId");
    if (body == null) {
      throw badRequest("body is required");
    }
    try {
      IPSLocationScheme scheme =
          siteManager.loadSchemeModifiable(
              guidManager.makeGuid(schemeId, PSTypeEnum.LOCATION_SCHEME));
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
  public List<PSRuntimeEditionStatus> listRuntimeEditions(@QueryParam("siteId") String siteId) {
    return requireRuntime().listRuntimeEditions(siteId);
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
    if (!isBlank(body.getName())) {
      edition.setName(body.getName().trim());
    } else if (isCreate) {
      throw badRequest("name is required");
    }
    if (body.getComment() != null) {
      edition.setComment(body.getComment());
    }
    if (!isBlank(body.getSiteId())) {
      edition.setSiteId(toSiteGuid(body.getSiteId()));
    }
    if (body.getPriority() != null) {
      IPSEdition.Priority p =
          IPSEdition.Priority.findByValue(body.getPriority()).orElse(IPSEdition.Priority.MEDIUM);
      edition.setPriority(p);
    }
  }

  private void applyContentListFields(
      IPSContentList cl, PSContentListSummary body, boolean isCreate) {
    if (!isBlank(body.getName()) && !isCreate) {
      cl.setName(body.getName().trim());
    }
    if (body.getDescription() != null) {
      cl.setDescription(body.getDescription());
    }
    if (body.getGenerator() != null) {
      cl.setGenerator(body.getGenerator());
    }
    if (body.getUrl() != null && !body.getUrl().isBlank()) {
      cl.setUrl(body.getUrl().trim());
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
    return s;
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

  private static void requireNonBlank(String value, String field) {
    if (isBlank(value)) {
      throw badRequest(field + " is required");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static WebApplicationException badRequest(String msg) {
    return new WebApplicationException(msg, Response.Status.BAD_REQUEST);
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
