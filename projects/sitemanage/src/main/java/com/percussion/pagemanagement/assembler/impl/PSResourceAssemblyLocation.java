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
package com.percussion.pagemanagement.assembler.impl;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.error.PSException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.pagemanagement.service.IPSRenderLinkService;
import com.percussion.pagemanagement.service.impl.PSLinkableAsset;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.PSFolderPathUtils;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSBeanValidationUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.service.IPSSiteDataService;
import java.io.File;

/**
 * A legacy location scheme generator that uses resource definitions. This is mainly used for Inline
 * links since the inline link generator calls the location scheme generator directly. Non-inline
 * links usually call {@link IPSRenderLinkService} directly through JEXL methods.
 *
 * <p>Right now this generator is used only for links (urls) and locations (file paths).
 *
 * @author adamgent
 */
public class PSResourceAssemblyLocation extends PSAbstractAssemblyLocationAdapter {

  private static final String PREVIEW_ITEM_FILTER = "preview";
  private static final String PUBLIC_ITEM_FILTER = "perc_public";
  private IPSRenderLinkService renderLinkService;
  private IPSIdMapper idMapper;
  private IPSFolderHelper folderHelper;
  private IPSAssetService assetService;
  private IPSSiteDataService siteDataService;

  @Override
  public void init(IPSExtensionDef extensionDef, File file) {
    super.init(extensionDef, file);
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Override
  protected String createLocation(PSAssemblyLocationRequest locationRequest)
      throws PSDataServiceException, PSException {
    PSBeanValidationUtils.validate(locationRequest).throwIfInvalid();
    var resourceId = getResourceDefinitionId(locationRequest);
    notEmpty(resourceId, "resourceId");
    var i = getItemAndContext(locationRequest);
    var link = renderLinkService.renderLink(i.linkContext, i.asset, resourceId);
    notNull(link);
    return link.getUrl();
  }

  protected String getResourceDefinitionId(PSAssemblyLocationRequest locationRequest)
      throws PSDataServiceException, PSException {
    var resourceId =
        locationRequest.getParameters().get(PSAssemblyConfig.PERC_RESOURCE_ID_PARAM_NAME);
    if (resourceId != null) {
      log.debug("Found resource in parameters");
      return resourceId;
    }
    var contentType = getContentTypeName(locationRequest);
    var templateName = getTemplateName(locationRequest);
    return renderLinkService
        .resolveResourceDefinition(resourceId, templateName, contentType)
        .getUniqueId();
  }

  protected String getTemplateName(PSAssemblyLocationRequest locationRequest) {
    return getTemplate(locationRequest).getName();
  }

  private ItemAndContext getItemAndContext(PSAssemblyLocationRequest locationRequest)
      throws IPSAssetService.PSAssetServiceException,
          IPSDataService.DataServiceLoadException,
          PSValidationException {
    var context = new PSAssemblyRenderLinkContext();

    var authType = getAuthtype(locationRequest);
    var filter = locationRequest.getItemFilter();
    isTrue(
        isNotBlank(authType) || isNotBlank(filter),
        "The filter and authtype cannot both be null or empty");
    if (isBlank(filter) && "0".equals(authType)) {
      filter = PREVIEW_ITEM_FILTER;
    } else if (isBlank(filter)) {
      filter = PUBLIC_ITEM_FILTER;
    }
    context.setFilter(filter);

    var linkContext = locationRequest.getAssemblyContext();
    var fileContext = locationRequest.getDeliveryContext();
    linkContext = linkContext == null ? locationRequest.getContext() : linkContext;
    fileContext = fileContext == null ? locationRequest.getContext() : fileContext;
    context.setLegacyLinkContext(linkContext);
    context.setLegacyFileContext(fileContext);
    context.setDeliveryContext(fileContext.intValue() == locationRequest.getContext().intValue());

    var contentId = idMapper.getString(locationRequest.getItemId());
    var asset = assetService.load(contentId, true);
    IPSItemSummary itemSummary = asset;

    var siteGuid = locationRequest.getSiteId();
    notNull(siteGuid, "siteGuid");
    var siteSummary = siteDataService.findByLegacySiteId(idMapper.getString(siteGuid), false);

    var providedPath = getFolderPath(locationRequest);
    var folderPath =
        renderLinkService.resolveFolderPath(
            itemSummary, siteSummary, PSFolderPathUtils.toFolderPath(providedPath));

    context.setSite(siteSummary);
    context.setFolderPath(folderPath);

    var i = new ItemAndContext();
    i.asset = new PSLinkableAsset(asset, folderPath);
    i.linkContext = context;
    return i;
  }

  private String getFolderPath(PSAssemblyLocationRequest locationRequest) {
    String providedPath = null;
    if (locationRequest.getFolderId() != null) {
      try {
        var legacyFolderId = idMapper.getLocator(locationRequest.getFolderId()).getId();
        providedPath = folderHelper.findPathFromLegacyFolderId(legacyFolderId);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return providedPath;
  }

  protected static class ItemAndContext {
    protected PSAssemblyRenderLinkContext linkContext;
    protected PSLinkableAsset asset;
  }

  public IPSRenderLinkService getRenderLinkService() {
    return renderLinkService;
  }

  public void setRenderLinkService(IPSRenderLinkService renderLinkService) {
    this.renderLinkService = renderLinkService;
  }

  public IPSIdMapper getIdMapper() {
    return idMapper;
  }

  public void setIdMapper(IPSIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public IPSFolderHelper getFolderHelper() {
    return folderHelper;
  }

  public void setFolderHelper(IPSFolderHelper folderHelper) {
    this.folderHelper = folderHelper;
  }

  public IPSAssetService getAssetService() {
    return assetService;
  }

  public void setAssetService(IPSAssetService assetService) {
    this.assetService = assetService;
  }

  public IPSSiteDataService getSiteDataService() {
    return siteDataService;
  }

  public void setSiteDataService(IPSSiteDataService siteDataService) {
    this.siteDataService = siteDataService;
  }
}
