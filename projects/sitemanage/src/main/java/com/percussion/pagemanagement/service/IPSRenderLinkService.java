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
package com.percussion.pagemanagement.service;

import com.percussion.pagemanagement.assembler.PSAbstractAssemblyContext.EditType;
import com.percussion.pagemanagement.assembler.PSRenderAsset;
import com.percussion.pagemanagement.data.PSInlineLinkRequest;
import com.percussion.pagemanagement.data.PSInlineRenderLink;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRenderLink;
import com.percussion.pagemanagement.data.PSRenderLinkContext;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.share.data.IPSFolderPath;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSLinkableItem;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import java.util.List;
import java.util.Set;

/**
 * Generates a logical link to:
 *
 * <ul>
 *   <li>{@link PSPage}
 *   <li>{@link PSRenderAsset}
 *   <li>File with a given resource uniqueId.
 *   <li>Folder with a given resource uniqueId.
 * </ul>
 *
 * In most cases, this link is a URL that may or may not be a complete URL (host and port info).
 *
 * @author adamgent
 */
public interface IPSRenderLinkService {

  /**
   * Renders a link for the item using the default resource definition for its type.
   *
   * @param context never {@code null}
   * @param linkableItem never {@code null}
   * @return never {@code null}
   */
  PSRenderLink renderLink(PSRenderLinkContext context, IPSLinkableItem linkableItem)
      throws PSDataServiceException;

  /**
   * Renders a link for an item using the given resource definition.
   *
   * @param context never {@code null}
   * @param linkableItem never {@code null}
   * @param resourceDefinitionId never {@code null}, empty, or blank
   * @return never {@code null}
   */
  PSRenderLink renderLink(
      PSRenderLinkContext context, IPSLinkableItem linkableItem, String resourceDefinitionId)
      throws PSDataServiceException;

  /**
   * Renders a link to the region CSS file of the specified theme.
   *
   * @param context the link context, not {@code null}
   * @param themeName the theme name, not blank
   * @param isEdit if {@code true}, then the current context is in edit mode
   * @param editType the edited item type
   * @return the link, never {@code null}
   */
  PSRenderLink renderLinkThemeRegionCSS(
      PSRenderLinkContext context, String themeName, boolean isEdit, EditType editType)
      throws IPSDataService.PSThemeNotFoundException,
          PSValidationException,
          IPSResourceDefinitionService.PSResourceDefinitionInvalidIdException;

  /**
   * Renders a link to a file or folder.
   *
   * @param context never {@code null}
   * @param resourceDefinitionId must be the id of a file or folder resource, never {@code null},
   *     empty, or blank
   * @return never {@code null}
   */
  PSRenderLink renderLink(PSRenderLinkContext context, String resourceDefinitionId)
      throws PSDataServiceException;

  /**
   * All JavaScript links from the resource definitions that match the supplied widget definitions
   * in order based on the resources dependency. All JavaScript links from percSystem resource
   * definition file are always added.
   *
   * @param context never {@code null}
   * @param widgetDefIds set of widget definitions whose JavaScript resource definitions need to be
   *     returned
   * @return JavaScript links in correct order, never {@code null} but may be empty
   */
  List<PSRenderLink> renderJavascriptLinks(PSRenderLinkContext context, Set<String> widgetDefIds)
      throws PSDataServiceException;

  /**
   * All CSS links from the resource definitions that match the supplied widget definitions in order
   * based on the resources dependency. All CSS links from percSystem resource definition file are
   * always added.
   *
   * @param context never {@code null}
   * @param widgetDefIds set of widget definitions whose CSS resource definitions need to be
   *     returned
   * @return CSS links in correct order, never {@code null} but may be empty
   */
  List<PSRenderLink> renderCssLinks(PSRenderLinkContext context, Set<String> widgetDefIds)
      throws PSDataServiceException;

  /**
   * Renders a preview link to a file.
   *
   * @param pageId must be the id of a file, never {@code null}, empty, or blank
   * @return never {@code null}
   */
  PSInlineRenderLink renderPreviewPageLink(String pageId);

  /**
   * The same as {@link #renderPreviewPageLink(String)}, but this has option of the rendering type.
   *
   * @param pageId the page ID, not blank
   * @param renderType this is the rendered type, "html", "xml" or "database". It defaults to "html"
   * @return the link to a page, never blank
   */
  PSInlineRenderLink renderPreviewPageLink(String pageId, String renderType)
      throws PSDataServiceException;

  /**
   * Creates a preview image link. See {@link PSInlineLinkRequest} for what properties must be set.
   *
   * @param inlineLinkRequest a valid renderLinkRequest, never {@code null}
   * @return never {@code null}
   * @see PSInlineLinkRequest
   */
  PSInlineRenderLink renderPreviewResourceLink(PSInlineLinkRequest inlineLinkRequest)
      throws PSDataServiceException;

  /**
   * Resolves what the folder path should be for link generation for the given item and the given
   * related paths. The path is guaranteed to be one of the paths that the item has ({@link
   * IPSItemSummary#getFolderPaths()}).
   *
   * @param item the item
   * @param paths can be sites, pages or other items, may be null
   * @return may be {@code null} if a folder path cannot be resolved from the inputs
   */
  String resolveFolderPath(IPSItemSummary item, IPSFolderPath... paths);

  /**
   * Resolves the folder path for the given item.
   *
   * @param item the item
   * @return may be {@code null} if a folder path cannot be resolved from the inputs
   * @see #resolveFolderPath(IPSItemSummary, IPSFolderPath...)
   */
  String resolveFolderPath(IPSItemSummary item);

  /**
   * Resolves the asset resource definition in order of the given parameters. If all the parameters
   * are {@code null}, an {@link IllegalArgumentException} will be thrown.
   *
   * @param resourceDefinitionId fully qualified, may be {@code null}
   * @param legacyTemplate assembly template, may be {@code null}
   * @param contentType may be {@code null}
   * @return never {@code null}
   */
  PSAssetResource resolveResourceDefinition(
      String resourceDefinitionId, String legacyTemplate, String contentType)
      throws PSDataServiceException;
}
