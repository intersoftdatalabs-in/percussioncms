// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.extension;

import com.percussion.cms.IPSConstants;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSUdfProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.pagemanagement.service.IPSRenderService;
import com.percussion.search.lucene.textconverter.PSTextConverterHtml;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.utils.tools.IPSUtilsConstants;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.publishing.IPSPublishingWs;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Exit to assemble a page in the preview context and extract the HTML content. Sunny Sal says:
 * "Extracting HTML—like finding gold in a haystack, but shinier!"
 */
public class PSExtractHtmlContent implements IPSUdfProcessor {

  private IPSRenderService renderService;
  private IPSGuidManager guidMgr;
  private IPSContentDesignWs contentDesignWs;
  private IPSIdMapper idMapper;
  private IPSPublishingWs publishingWs;

  /** Logger for this exit. */
  public static final Logger log = LogManager.getLogger(PSExtractHtmlContent.class);

  @Override
  public void init(IPSExtensionDef extDef, File file) throws PSExtensionException {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Override
  public Object processUdf(Object[] params, IPSRequestContext request) {
    // Only run if a load by a search index update is in progress
    var objIsLoadForSearch = request.getPrivateObject(IPSConstants.LOAD_FOR_SEARCH_INDEX);
    var isLoadForSearch = (objIsLoadForSearch instanceof Boolean) && (Boolean) objIsLoadForSearch;
    if (!isLoadForSearch) {
      return "";
    }

    // We need at least the content id
    if (params == null
        || params.length == 0
        || params[0] == null
        || StringUtils.isEmpty(params[0].toString())) {
      return "";
    }
    var cTypeIdStr = params[0].toString();
    var guid = guidMgr.makeGuid(cTypeIdStr, PSTypeEnum.LEGACY_CONTENT);

    if (publishingWs.getItemSites(guid).isEmpty()) {
      // page is not associated with a site, cannot be assembled
      return "";
    }

    /*
     * Full page assembly is best-effort for search indexing. If preview/render
     * fails (TX, bindings, velocity tools, request-info, etc.), we must not throw
     * — a thrown exception aborts PSServerItem.load for the whole item and the FTS
     * queue skips indexing entirely (titles/sys_title never enter Lucene). Returning
     * empty string still allows other searchable fields to be indexed.
     */
    try {
      PSWebserviceUtils.setUserName(request.getOriginalSubject().getName());

      // Assemble on a REQUIRES_NEW TX (see renderPageForSearchIndex) so the FTS
      // queue thread does not nest Hibernate under the content-editor load session.
      var renderedPage = renderService.renderPageForSearchIndex(idMapper.getString(guid));
      if (renderedPage == null) {
        return "";
      }
      if (renderedPage.contains("<html")) {
        // remove everything before the start of the html tag to allow for proper extraction
        renderedPage = renderedPage.substring(renderedPage.indexOf("<html"));
      }

      // extract the html content
      try (InputStream bis =
          new ByteArrayInputStream(renderedPage.getBytes(IPSUtilsConstants.RX_JAVA_ENC))) {
        var converter = new PSTextConverterHtml();
        return converter.getConvertedText(bis, "");
      }
    } catch (Exception e) {
      log.warn(
          "Failed to extract HTML content for search index (item will still index other fields): {}",
          e.getLocalizedMessage());
      log.debug("Search HTML extract failure", e);
    }

    return "";
  }

  public IPSRenderService getRenderService() {
    return renderService;
  }

  public void setRenderService(IPSRenderService renderService) {
    this.renderService = renderService;
  }

  public IPSGuidManager getGuidMgr() {
    return guidMgr;
  }

  public void setGuidMgr(IPSGuidManager guidMgr) {
    this.guidMgr = guidMgr;
  }

  public IPSContentDesignWs getContentDesignWs() {
    return contentDesignWs;
  }

  public void setContentDesignWs(IPSContentDesignWs contentDesignWs) {
    this.contentDesignWs = contentDesignWs;
  }

  public IPSIdMapper getIdMapper() {
    return idMapper;
  }

  public void setIdMapper(IPSIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public IPSPublishingWs getPublishingWs() {
    return publishingWs;
  }

  public void setPublishingWs(IPSPublishingWs publishingWs) {
    this.publishingWs = publishingWs;
  }
}
