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
// REFACTORED: CP-JAVA11
package com.percussion.pagemanagement.service.impl;

import static com.percussion.pagemanagement.assembler.PSResourceLinkAndLocationUtils.concatPath;
import static com.percussion.pagemanagement.assembler.PSResourceLinkAndLocationUtils.escapePathForUrl;
import static com.percussion.share.web.service.PSRestServicePathConstants.ID_PATH_PARAM;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.cms.PSSingleValueBuilder;
import com.percussion.error.PSException;
import com.percussion.pagemanagement.assembler.IPSRenderLinkContextFactory;
import com.percussion.pagemanagement.assembler.PSAbstractAssemblyContext.EditType;
import com.percussion.pagemanagement.assembler.impl.PSLegacyLinkGenerator;
import com.percussion.pagemanagement.assembler.impl.PSLegacyLinkGenerator.PSLegacyLink;
import com.percussion.pagemanagement.assembler.impl.PSResourceInstanceHelper;
import com.percussion.pagemanagement.data.IPSResourceDefinitionVisitor;
import com.percussion.pagemanagement.data.PSInlineLinkRequest;
import com.percussion.pagemanagement.data.PSInlineRenderLink;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRenderLink;
import com.percussion.pagemanagement.data.PSRenderLinkContext;
import com.percussion.pagemanagement.data.PSRenderLinkContext.Mode;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSFileResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSFileResource.PSFileResourceType;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSFolderResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.pagemanagement.data.PSResourceInstance;
import com.percussion.pagemanagement.data.PSResourceLinkAndLocation;
import com.percussion.pagemanagement.data.PSThemeResource;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSRenderLinkService;
import com.percussion.pagemanagement.service.IPSResourceDefinitionService;
import com.percussion.pagemanagement.service.IPSResourceLinkAndLocationService;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.io.PSPathInjectionGuard;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.share.dao.PSFolderPathUtils;
import com.percussion.share.data.IPSFolderPath;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSAbstractFilter;
import com.percussion.share.data.PSAbstractTransformer;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.share.service.IPSDataService.PSThemeNotFoundException;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.IPSLinkableItem;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSBeanValidationUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.theme.data.PSThemeSummary;
import com.percussion.theme.service.impl.PSThemeService;
import com.percussion.validation.ValidationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * See the interfaces for documentation.
 *
 * @author adamgent
 */
@Path("/renderlink")
@Component("renderLinkService")
@Lazy
public class PSRenderLinkService
    implements IPSRenderLinkService, IPSResourceLinkAndLocationService {
  /** Name of the system (shared) resources dependencies file. */
  private static final String systemResourcesFileName = "percSystem";

  private IPSPageService pageService;

  private IPSResourceDefinitionService resourceDefinitionService;

  private IPSRenderLinkContextFactory renderLinkContextFactory;

  private PSLegacyLinkGenerator legacyLinkGenerator;

  private IPSManagedLinkDao managedLinkDao;

  private PSResourceInstanceHelper resourceInstanceHelper;

  private PSThemeService themeService;

  private IPSIdMapper idMapper;

  private static final IPSFolderPath assetsRootFolderPath =
      PSFolderPathUtils.toFolderPath(PSAssetPathItemService.ASSET_ROOT);
  private static final IPSFolderPath sitesRootFolderPath =
      PSFolderPathUtils.toFolderPath("//Sites");

  private String themePreviewUrlBase = "/Rhythmyx/web_resources";

  private String filePreviewUrlBase = "/Rhythmyx";

  @Autowired
  public PSRenderLinkService(
      IPSIdMapper idMapper,
      PSLegacyLinkGenerator legacyLinkGenerator,
      IPSPageService pageService,
      IPSRenderLinkContextFactory renderLinkContextFactory,
      IPSResourceDefinitionService resourceDefinitionService,
      PSResourceInstanceHelper resourceInstanceHelper,
      PSThemeService themeService,
      IPSManagedLinkDao managedLinkDao) {
    super();
    this.idMapper = idMapper;
    this.legacyLinkGenerator = legacyLinkGenerator;
    this.pageService = pageService;
    this.renderLinkContextFactory = renderLinkContextFactory;
    this.resourceDefinitionService = resourceDefinitionService;
    this.resourceInstanceHelper = resourceInstanceHelper;
    this.themeService = themeService;
    this.managedLinkDao = managedLinkDao;
  }

  @Override
  public List<PSResourceLinkAndLocation> resolveLinkAndLocations(
      PSResourceInstance resourceInstance) throws IPSAssetService.PSAssetServiceException {
    return resourceInstanceHelper.getLinkAndLocations(resourceInstance);
  }

  /** {@inheritDoc} */
  public PSRenderLink renderLink(PSRenderLinkContext context, IPSLinkableItem item)
      throws PSDataServiceException {
    PSAssetResource r = resolveResourceDefinition(null, null, item.getType());
    return renderLinkHelper(context, r, item);
  }

  /** {@inheritDoc} */
  public PSRenderLink renderLink(
      PSRenderLinkContext context, IPSLinkableItem item, String resourceDefinitionId)
      throws PSDataServiceException {
    notNull(resourceDefinitionId, "resourceDefinitionId");
    PSAssetResource r = resolveResourceDefinition(resourceDefinitionId, null, null);
    return renderLinkHelper(context, r, item);
  }

  /** {@inheritDoc} */
  @Override
  public List<PSRenderLink> renderCssLinks(PSRenderLinkContext context, Set<String> widgetDefIds)
      throws PSDataServiceException {
    return renderFileLinks(context, PSFileResourceType.css, widgetDefIds);
  }

  /** {@inheritDoc} */
  @Override
  public List<PSRenderLink> renderJavascriptLinks(
      PSRenderLinkContext context, Set<String> widgetDefIds) throws PSDataServiceException {
    return renderFileLinks(context, PSFileResourceType.javascript, widgetDefIds);
  }

  /**
   * Returns a list of files links sorted by depdendencies.
   *
   * @param context never <code>null</code>.
   * @param type the file type.
   * @return never <code>null</code>, maybe empty.
   */
  private List<PSRenderLink> renderFileLinks(
      final PSRenderLinkContext context,
      final PSFileResourceType type,
      final Set<String> widgetDefIds)
      throws PSDataServiceException {
    List<PSResourceDefinition> resources = resourceDefinitionService.findAllResources();
    notNull(resources, "resources should not be null");

    /*
     * Filter out the resources that are not files and of the given type.
     */
    resources =
        new PSAbstractFilter<PSResourceDefinition>() {

          @Override
          public boolean shouldKeep(PSResourceDefinition resource) {
            return (resource instanceof PSFileResource
                && ((PSFileResource) resource).getType() == type
                && (((PSFileResource) resource).getContext() == null
                    || ((PSFileResource) resource)
                        .getContext()
                        .toString()
                        .equals(context.getMode().toString()))
                && (widgetDefIds.contains(resource.getGroupId())
                    || resource.getGroupId().equalsIgnoreCase(systemResourcesFileName)));
          }
        }.filter(resources);

    /*
     * Remove the duplicated resources (eg: perc_common_ui.js is included in several files)
     */
    resources = removeDuplicatedResources(resources);

    /*
     * Sort the resources by dependencies.
     */
    resources = PSResourceDefinitionUtils.sortByDependencies(resources);

    /*
     * Now turn them into links.
     */
    return new PSAbstractTransformer<PSResourceDefinition, PSRenderLink>() {

      @Override
      protected PSRenderLink doTransform(PSResourceDefinition old) throws PSDataServiceException {
        return renderLink(context, old.getUniqueId());
      }
    }.collect(resources);
  }

  /**
   * Removes any duplicated resources based on the Id value (it's not a comparison between objects,
   * but of its file names)
   *
   * @author federicoromanelli
   * @param resources the list of resources that may contain duplicated values (comparison is based
   *     on id, not uniqueId).
   * @return the list of resources with the duplicated object deleted, or the original list if
   *     objects couldn't be removed, never <code>null</code>.
   */
  private List<PSResourceDefinition> removeDuplicatedResources(
      List<PSResourceDefinition> resources) {
    List<PSResourceDefinition> res = resources;
    List<PSResourceDefinition> resourcesToDelete = new ArrayList<>();

    HashSet<String> uniqueResources = new HashSet<>();
    Iterator<PSResourceDefinition> iterator = res.iterator();

    while (iterator.hasNext()) {
      PSResourceDefinition resourceDef = iterator.next();
      String id = resourceDef.getId();
      if (!uniqueResources.contains(id)) {
        uniqueResources.add(id);
      } else {
        resourcesToDelete.add(resourceDef);
      }
    }
    if (res.removeAll(resourcesToDelete)) {
      return res;
    }

    return resources;
  }

  /**
   * REST: preview page link. Optional {@code titleField} query param selects the target content
   * field for the HTML title attribute (#2242). Method name is distinct from {@link
   * #renderPreviewPageLink(String, String)} (renderType) to avoid Java signature clash.
   */
  @GET
  @Path("/preview/{id}/page")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSInlineRenderLink restRenderPreviewPageLink(
      @PathParam(ID_PATH_PARAM) String pageId, @QueryParam("titleField") String titleField) {
    try {
      return renderPreviewPageLink(pageId, "html", titleField);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  /** {@inheritDoc} */
  public PSInlineRenderLink renderPreviewPageLink(String pageId) {
    try {
      return renderPreviewPageLink(pageId, "html", null);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  /** {@inheritDoc} */
  public PSInlineRenderLink renderPreviewPageLink(String pageId, String renderType)
      throws PSDataServiceException {
    return renderPreviewPageLink(pageId, renderType, null);
  }

  /**
   * Renders a preview page link, optionally resolving {@code title} from a configured content field
   * (control setting / {@code titleField} query param). See {@link PSInlineLinkTitleResolver}.
   *
   * @param pageId page id, never blank
   * @param renderType html/xml/database
   * @param titleField optional target field name for the link title attribute
   * @return preview link or {@code null} if page missing
   */
  public PSInlineRenderLink renderPreviewPageLink(
      String pageId, String renderType, String titleField) throws PSDataServiceException {
    notNull(pageId, "pageId");
    PSPage page = null;
    try {
      page = pageService.find(pageId);
    } catch (DataServiceNotFoundException | DataServiceLoadException | PSValidationException e) {
      log.error(
          "page target {} does not exist. Error: {}", pageId, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      try {
        managedLinkDao.cleanupOrphanedLinks();
      } catch (Exception ex) {
        log.error("Cannot cleanup orphaned links.  Error: {}", ex.getMessage());
        log.debug(ex.getMessage(), ex);
      }
      return null;
    }
    if (page == null) return null;

    page.setId(pageId);
    PSRenderLinkContext context = renderLinkContextFactory.createPreview(page);
    PSInlineRenderLink renLink = new PSInlineRenderLink();
    String resourceId = "percSystem.page";
    if ("xml".equalsIgnoreCase(renderType)) resourceId = "percSystem.pageXml";
    else if ("database".equalsIgnoreCase(renderType)) resourceId = "percSystem.pageDatabase";

    PSAssetResource r = resolveResourceDefinition(resourceId, null, null);
    renderLinkHelper(renLink, context, r, page);
    renLink.setStateClass(getStateClass(pageId));
    renLink.setTitle(resolvePageInlineLinkTitle(page, pageId, titleField));
    legacyLinkGenerator.addLegacyDataToInlineLink(renLink, page);
    return renLink;
  }

  /**
   * REST: default preview link (page or asset). Optional {@code titleField} query param.
   *
   * <p>Exceptions surface as {@link WebApplicationException} from {@link
   * #renderPreviewLink(String, String, String, String)} (no outer DataService* catch — those
   * checked types no longer escape the 4-arg method).
   */
  @GET
  @Path("/preview/{id}/default")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSInlineRenderLink restRenderPreviewLinkDefault(
      @PathParam(ID_PATH_PARAM) String targetId, @QueryParam("titleField") String titleField) {
    return renderPreviewLink(targetId, null, null, titleField);
  }

  /**
   * REST: preview with resource definition id. Optional {@code titleField} query param.
   *
   * <p>See {@link #restRenderPreviewLinkDefault} for exception handling notes.
   */
  @GET
  @Path("/preview/{id}/{resourceDef}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSInlineRenderLink restRenderPreviewLinkWithResource(
      @PathParam(ID_PATH_PARAM) String targetId,
      @PathParam("resourceDef") String resourceDefinitionId,
      @QueryParam("titleField") String titleField) {
    return renderPreviewLink(targetId, resourceDefinitionId, null, titleField);
  }

  /**
   * REST + internal: preview with resource + thumbnail resource definition ids. Optional {@code
   * titleField} query param for title resolve (#2242).
   *
   * <p>All {@link PSDataServiceException} paths are converted to {@link WebApplicationException}.
   * Checked DataService* types do not escape (dead outer catches on REST wrappers removed).
   */
  @GET
  @Path("/preview/{id}/{resourceDef}/{thumbResourceDef}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSInlineRenderLink renderPreviewLink(
      @PathParam(ID_PATH_PARAM) String targetId,
      @PathParam("resourceDef") String resourceDefinitionId,
      @PathParam("thumbResourceDef") String thumbResourceDefinitionId,
      @QueryParam("titleField") String titleField) {

    try {
      IPSItemSummary itemSummary = resourceInstanceHelper.findResourceAsset(targetId);
      if (itemSummary.isPage()) {
        return renderPreviewPageLink(targetId, "html", titleField);
      }

      PSInlineLinkRequest lr = new PSInlineLinkRequest();
      lr.setTargetId(targetId);
      lr.setResourceDefinitionId(resourceDefinitionId);
      lr.setThumbResourceDefinitionId(thumbResourceDefinitionId);
      lr.setTitleField(titleField);

      return renderPreviewResourceLink(lr);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  /**
   * Backward-compatible overload without title field (internal callers / BC).
   */
  public PSInlineRenderLink renderPreviewLink(
      String targetId, String resourceDefinitionId, String thumbResourceDefinitionId) {
    return renderPreviewLink(targetId, resourceDefinitionId, thumbResourceDefinitionId, null);
  }

  /** {@inheritDoc} */
  @POST
  @Path("/preview")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSInlineRenderLink renderPreviewResourceLink(PSInlineLinkRequest request)
      throws PSDataServiceException {

    try {
      PSBeanValidationUtils.validate(request).throwIfInvalid();
    } catch (PSSpringValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
    PSAsset asset;
    try {
      asset = resourceInstanceHelper.loadPartialAsset(request.getTargetId());
    } catch (IPSAssetService.PSAssetServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }

    if (asset == null) {
      log.error("link target {} does not exist", request.getTargetId());
      try {
        managedLinkDao.cleanupOrphanedLinks();
      } catch (Exception e) {
        log.error("Cannot cleanup orphaned links. Error: {}", PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      return null;
    }

    List<PSAssetResource> resources =
        resourceDefinitionService.findAssetResourcesForType(asset.getType());

    /**
     * here we are assuming at most two resources, binary and thumb nail binary. currently assuming
     * the first resource that is not a thumb nail is the main binary type
     */
    for (PSAssetResource res : resources) {
      if (res.getId().toLowerCase().contains("thumbbinary")) {
        request.setThumbResourceDefinitionId(res.getUniqueId());
      } else {
        request.setResourceDefinitionId(res.getUniqueId());
      }
    }

    String folderPath = resolveFolderPathForAsset(asset);

    PSRenderLinkContext context = renderLinkContextFactory.createAssetPreview(folderPath, asset);
    PSLinkableAsset linkAsset = new PSLinkableAsset(asset, folderPath);

    PSInlineRenderLink renLink = new PSInlineRenderLink();
    try {
      /*
       * Resolve the targets resource definition.
       */
      PSAssetResource rd =
          resolveResourceDefinition(request.getResourceDefinitionId(), null, asset.getType());
      isTrue(rd != null, "Target does not have a resource definition: ", asset);

      renderLinkHelper(renLink, context, rd, linkAsset);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }

    /*
     * Resolve the targets thumbnail resource definition
     * if it has one.
     */
    if (isNotBlank(request.getThumbResourceDefinitionId())) {
      try {
        PSAssetResource thumbNailResource =
            resolveResourceDefinition(request.getThumbResourceDefinitionId(), null, null);

        if (thumbNailResource != null) {
          PSInlineRenderLink thumbLink = new PSInlineRenderLink();
          renderLinkHelper(thumbLink, context, thumbNailResource, linkAsset);
          renLink.setThumbUrl(thumbLink.getUrl());
          // getResourceDefinition() returns object directly
          renLink.setThumbResourceDefinition(thumbLink.getResourceDefinition());
        }
      } catch (PSDataServiceException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        // It is just the thumbnail so don't completely fail out
      }
    }

    renLink.setStateClass(getStateClass(request.getTargetId()));
    renLink.setAltText((String) asset.getFields().get("alttext"));
    renLink.setTitle(resolveAssetInlineLinkTitle(asset, request.getTitleField()));

    /*
     * Fill in legacy data.
     */
    legacyLinkGenerator.addLegacyDataToInlineLink(renLink, request, asset);

    return renLink;
  }

  /**
   * Resolves page inline link title: configured field → displaytitle → page link title (BC).
   *
   * <p>Uses the page DTO field map first. Loads a partial asset only when the configured field is
   * not already satisfied from the DTO (avoids a data-service round-trip for common configs like
   * {@code page_title} / {@code resource_link_title}). Custom / shared fields such as {@code
   * displaytitle} still trigger the partial load when needed.
   *
   * @param page loaded page, never null
   * @param pageId page id for optional field load
   * @param titleField optional configured field name
   * @return never null (may be empty)
   */
  // package-private for unit tests (#2242 review)
  String resolvePageInlineLinkTitle(PSPage page, String pageId, String titleField) {
    String typeDefault = page.getLinkTitle();
    if (StringUtils.isBlank(titleField)) {
      return StringUtils.defaultString(typeDefault);
    }
    Map<String, Object> fields = buildPageTitleFieldMap(page);
    // Skip partial load when DTO already has a non-blank value for the configured field.
    if (StringUtils.isBlank(PSInlineLinkTitleResolver.fieldAsString(fields, titleField))) {
      try {
        PSAsset partial = resourceInstanceHelper.loadPartialAsset(pageId);
        if (partial != null && partial.getFields() != null) {
          fields.putAll(partial.getFields());
        }
      } catch (Exception e) {
        log.debug(
            "Could not load page fields for titleField={}: {}",
            titleField,
            PSExceptionUtils.getMessageForLog(e));
      }
    }
    return PSInlineLinkTitleResolver.resolve(titleField, fields, typeDefault);
  }

  /**
   * Known page title-related fields from the page DTO. Keys use {@link PSInlineLinkTitleResolver}
   * constants and must stay aligned with {@code PSPageDao} field mapping ({@code page_title},
   * {@code resource_link_title}, etc.). Shared {@code displaytitle} is not invented here — if
   * absent, the resolver falls through to the page link-title type default.
   */
  // package-private for unit tests (#2242 review)
  static Map<String, Object> buildPageTitleFieldMap(PSPage page) {
    Map<String, Object> fields = new HashMap<>();
    fields.put(PSInlineLinkTitleResolver.PAGE_DEFAULT_TITLE_FIELD, page.getLinkTitle());
    fields.put(PSInlineLinkTitleResolver.PAGE_TITLE_FIELD, page.getTitle());
    fields.put(PSInlineLinkTitleResolver.SYS_TITLE_FIELD, page.getName());
    fields.put(PSInlineLinkTitleResolver.PAGE_DESCRIPTION_FIELD, page.getDescription());
    fields.put(PSInlineLinkTitleResolver.PAGE_SUMMARY_FIELD, page.getSummary());
    fields.put(PSInlineLinkTitleResolver.PAGE_AUTHOR_FIELD, page.getAuthor());
    return fields;
  }

  /**
   * Resolves asset/file/image inline link title: configured field → displaytitle → type default.
   */
  // package-private for unit tests (#2242 review)
  static String resolveAssetInlineLinkTitle(PSAsset asset, String titleField) {
    Map<String, Object> fields =
        asset.getFields() != null ? asset.getFields() : Collections.emptyMap();
    String typeDefault =
        PSInlineLinkTitleResolver.fieldAsString(
            fields, PSInlineLinkTitleResolver.ASSET_DEFAULT_TITLE_FIELD);
    return PSInlineLinkTitleResolver.resolve(titleField, fields, typeDefault);
  }

  private String getStateClass(String id) {
    String flag = PSSingleValueBuilder.getValidFlag(idMapper.getContentId(id));

    String classValue = "";
    if (flag.equals("u")) {
      classValue = PSSingleValueBuilder.PERC_BROKENLINK;
    } else if (!flag.equals("y") && !flag.equals("i")) {
      classValue = PSSingleValueBuilder.PERC_NOTPUBLICLINK;
    }
    return classValue;
  }

  /** {@inheritDoc} */
  public PSAssetResource resolveResourceDefinition(
      String resourceDefinitionId, String legacyTemplate, String contentType)
      throws PSDataServiceException {
    if (resourceDefinitionId != null) return findAssetResourceDefinition(resourceDefinitionId);
    if (legacyTemplate != null)
      return findAssetResourceDefinitionForTemplate(legacyTemplate, contentType);
    if (contentType != null)
      return resourceDefinitionService.findDefaultAssetResourceForType(contentType);
    throw new IllegalArgumentException("One of the arguments must not be null");
  }

  private String resolveFolderPathForAsset(PSAsset asset) {
    String folderPath = resolveFolderPath(asset);

    if (folderPath == null) {
      log.error("Asset does not have any valid folder paths: {}", asset);
      notEmpty(folderPath);
    } else {
      log.debug("Resolved folder path to: {}", folderPath);
    }
    return folderPath;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The assets or some other site folder path will be used if the other paths fail.
   */
  public String resolveFolderPath(IPSItemSummary item, IPSFolderPath... paths) {
    ArrayList<IPSFolderPath> combined = new ArrayList<>();
    if (paths != null) {
      Collections.addAll(combined, paths);
    }

    if (item == null) {
      log.warn("Unable to resolveFolderPath() IPSItemSummary item  = null");
      return null;
    } else if (item.isPage()) {
      /*
       * If its a page will also accept other site folder paths.
       */
      combined.add(sitesRootFolderPath);
    } else {
      /*
       * If its not a page but an asset then we will accept asset folder paths.
       */
      combined.add(assetsRootFolderPath);
    }
    return PSFolderPathUtils.resolveFolderPath(
        item, combined.toArray(new IPSFolderPath[combined.size()]));
  }

  @Override
  public String resolveFolderPath(IPSItemSummary item) {
    return resolveFolderPath(item, (IPSFolderPath[]) null);
  }

  private PSRenderLink renderLinkHelper(
      PSRenderLinkContext context, PSAssetResource resourceDefinition, IPSLinkableItem item)
      throws IPSAssetService.PSAssetServiceException,
          DataServiceNotFoundException,
          PSValidationException {
    PSRenderLink rl = new PSRenderLink();
    renderLinkHelper(rl, context, resourceDefinition, item);
    return rl;
  }

  /**
   * Process and fills a {@link PSRenderLink} object.
   *
   * @param <T> a render link type to process.
   * @param renderLink never <code>null</code>.
   * @param context never <code>null</code>.
   * @param resourceDefinition not <code>null</code>.
   * @param item never <code>null</code>.
   */
  private <T extends PSRenderLink> void renderLinkHelper(
      T renderLink,
      PSRenderLinkContext context,
      PSAssetResource resourceDefinition,
      IPSLinkableItem item)
      throws IPSAssetService.PSAssetServiceException,
          DataServiceNotFoundException,
          PSValidationException {
    notNull(resourceDefinition, "resourceDefinition");
    if (log.isTraceEnabled())
      log.trace(
          format(
              "Generating link for context:{0} resourceDefinitionId:{1} item:{2}",
              context, resourceDefinition, item));

    PSRenderLink rl = null;
    validateLinkContext(context);
    if (context.getMode() == PSRenderLinkContext.Mode.PUBLISH) {
      log.debug("Using resource instance to create location. Mode is publish");
      PSResourceInstance r =
          resourceInstanceHelper.createResourceInstance(context, item, resourceDefinition);
      List<PSResourceLinkAndLocation> links = resourceInstanceHelper.getLinkAndLocations(r);
      rl = links.get(0).getRenderLink();
    } else {
      PSLegacyLink link = new PSLegacyLink();
      try {
        legacyLinkGenerator.fillLegacyLink(context, item, resourceDefinition, link);
        String url = legacyLinkGenerator.generate(link);
        rl = new PSRenderLink(url, resourceDefinition);
      } catch (ValidationException | PSException ex) {
        log.warn("Error: {}", ex.getMessage());
        log.debug(ex.getMessage(), ex);
        rl = new PSRenderLink();
      }
    }

    // Making link pointing to section if canonical option is ON and set to "sections"
    if (context.getSite().isCanonical()
        && context.getSite().getCanonicalDist().equals("sections")) {
      String urlBase;

      // in case default document listed among params in url first take out params
      if (rl.getUrl().indexOf("?") > 0)
        urlBase = rl.getUrl().substring(0, rl.getUrl().indexOf("?"));
      else urlBase = rl.getUrl();

      // we need to remove the file name from url only if the it is a default document
      if (urlBase.lastIndexOf(context.getSite().getDefaultDocument())
          == rl.getUrl().lastIndexOf("/") + 1) {
        String urlParams = "";

        if (rl.getUrl().indexOf("?") > 0)
          urlParams = rl.getUrl().substring(rl.getUrl().indexOf("?"));
        urlBase = urlBase.substring(0, urlBase.lastIndexOf(context.getSite().getDefaultDocument()));
        rl.setUrl(urlBase + urlParams);
      }
    }

    renderLink.setUrl(rl.getUrl());
    renderLink.setResourceType(rl.getResourceType());
    renderLink.setResourceDefinitionId(rl.getResourceDefinitionId());
    renderLink.setResourceDefinition(rl.getResourceDefinition());
  }

  /** {@inheritDoc} */
  public PSResourceInstance createResourceInstance(
      PSRenderLinkContext context, IPSLinkableItem item, String resourceDefinitionId)
      throws PSDataServiceException {
    PSAssetResource rd = resolveResourceDefinition(resourceDefinitionId, null, item.getType());
    return resourceInstanceHelper.createResourceInstance(context, item, rd);
  }

  /** {@inheritDoc} */
  public PSRenderLink renderLinkThemeRegionCSS(
      final PSRenderLinkContext context, String themeName, boolean isEdit, EditType editType)
      throws PSThemeNotFoundException,
          PSValidationException,
          IPSResourceDefinitionService.PSResourceDefinitionInvalidIdException {
    boolean useCachedRegionCSS = isEdit && editType == EditType.TEMPLATE;

    log.debug(
        "context: {}, useCached: {}, editType: {}",
        context.getMode(),
        useCachedRegionCSS,
        editType.name());

    PSThemeResource resource = (PSThemeResource) getResourceDefinition("theme." + themeName);
    if (resource == null) return new PSRenderLink("", resource);

    PSThemeSummary summary = resource.getThemeSummary();
    if (!useCachedRegionCSS && StringUtils.isBlank(summary.getRegionCssFilePath())) {
      return new PSRenderLink("", resource);
    }
    String regionCssPath = getRegionCSSRelativePath(themeName, useCachedRegionCSS, summary);

    // Check if file is empty, don't add url
    try {
      File themesRoot = new File(themeService.getThemesRootDirectory());
      // regionCssPath is theme-relative (validated via theme summary); contain under themes root
      File cssFile = PSPathInjectionGuard.requireUnderBase(themesRoot, regionCssPath);
      if (!cssFile.exists() || cssFile.length() == 0) { // codeql[java/path-injection]
        return new PSRenderLink("", resource);
      }
    } catch (Exception e) {
      log.warn("Region CSS File not found. Filename: {} Error:{}", regionCssPath, e.getMessage());
    }
    final String baseUrl = resourceInstanceHelper.getBaseUrlPath(context);
    String url = makeThemeURL(context, baseUrl, regionCssPath, useCachedRegionCSS);
    String renderUrl = escapePathForUrl(url);
    if (context.getMode() != PSRenderLinkContext.Mode.PUBLISH) {
      renderUrl = String.format("%s?time=%d", renderUrl, System.currentTimeMillis());
    }
    return new PSRenderLink(renderUrl, resource);
  }

  private String getRegionCSSRelativePath(
      String themeName, boolean useCached, PSThemeSummary summary) throws PSThemeNotFoundException {
    String regionCssPath;
    if (useCached) {
      regionCssPath = themeService.getCachedRegionCSSRelativeURL(themeName);
    } else {
      regionCssPath = summary.getRegionCssFilePath();
    }
    return regionCssPath;
  }

  private String makeThemeURL(
      final PSRenderLinkContext context, String baseUrl, String fileName, boolean useCached) {
    fileName = "/" + removeStart(fileName, "/");
    if (context.getMode() == Mode.PUBLISH) {
      return concatPath(baseUrl, themeService.getThemesRootRelativeUrl(), fileName);
    }

    if (useCached)
      return concatPath(filePreviewUrlBase, themeService.getThemesTempRootRelativeUrl(), fileName);
    else return concatPath(themePreviewUrlBase, "themes", fileName);
  }

  /** {@inheritDoc} */
  public PSRenderLink renderLink(final PSRenderLinkContext context, String resourceDefinitionId)
      throws PSDataServiceException {
    PSResourceDefinition rd = getResourceDefinition(resourceDefinitionId);
    final String baseUrl = resourceInstanceHelper.getBaseUrlPath(context);
    final StringBuilder url = new StringBuilder();

    IPSResourceDefinitionVisitor v =
        new IPSResourceDefinitionVisitor() {
          public void visit(PSAssetResource resource) throws PSDataServiceException {
            throw new PSDataServiceException("Is not a file or folder");
          }

          public void visit(PSFileResource resource) {
            String file = resource.getFile();
            url.append(makeUrl(file));
          }

          public void visit(PSFolderResource resource) {
            String path = resource.getPath();
            url.append(makeUrl(path));
          }

          public void visit(PSThemeResource resource) {
            String cssFilePath = resource.getThemeSummary().getCssFilePath();
            File cssFile =
                new File(
                    themeService.getThemesRootDirectory().concat(File.separator + cssFilePath));
            if (cssFile.exists() && cssFile.length() > 0) {
              url.append(makeThemeUrl(cssFilePath));
            }
          }

          private String makeThemeUrl(String fileName) {
            return makeThemeURL(context, baseUrl, fileName, false);
          }

          private String makeUrl(String fileName) {
            fileName = "/" + removeStart(fileName, "/");
            if (context.getMode() == Mode.PUBLISH) {
              fileName = concatPath(baseUrl, fileName);
              return fileName;
            }
            return concatPath(filePreviewUrlBase, fileName);
          }
        };

    if (rd != null) {
      rd.accept(v);
    }
    final String renderUrl = escapePathForUrl(url.toString());
    return new PSRenderLink(renderUrl, rd);
  }

  private PSResourceDefinition getResourceDefinition(String resourceDefinitionId)
      throws IPSResourceDefinitionService.PSResourceDefinitionInvalidIdException,
          PSValidationException {
    PSResourceDefinition rd = null;
    try {
      rd = resourceDefinitionService.findResource(resourceDefinitionId);
    } catch (PSThemeNotFoundException e) {
      // issue CM-276 - If the user deletes the percussion theme folder,
      // we should allow the rendering anyway (in that case, this type of
      // exception will be thrown
      log.warn(
          "Theme folder may have been deleted. Error:{}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return rd;
  }

  private PSAssetResource findAssetResourceDefinition(String resourceDefinitionId)
      throws PSDataServiceException {
    PSResourceDefinition definition = resourceDefinitionService.findResource(resourceDefinitionId);
    if (definition instanceof PSAssetResource) {
      return (PSAssetResource) definition;
    }
    throw new DataServiceLoadException(
        String.format("resourceDefinitionId: %s is not an asset resource", resourceDefinitionId));
  }

  private PSAssetResource findAssetResourceDefinitionForTemplate(
      String legacyTemplate, final String contentType) throws PSDataServiceException {
    notEmpty(legacyTemplate, "template");

    List<PSAssetResource> definition =
        resourceDefinitionService.findAssetResourcesForLegacyTemplate(legacyTemplate);
    if (isNotBlank(contentType)) {
      definition =
          new PSAbstractFilter<PSAssetResource>() {
            @Override
            public boolean shouldKeep(PSAssetResource resource) {
              return contentType.equals(resource.getContentType());
            }
          }.filter(definition);
    }
    notEmpty(definition);
    isTrue(definition.size() == 1, "Should only have one legacy template for inline links");
    return definition.get(0);
  }

  public void validateLinkContext(PSRenderLinkContext context) throws PSBeanValidationException {
    PSBeanValidationUtils.getValidationErrorsOrFailIfInvalid(context);
  }

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(PSRenderLinkService.class);
}
