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
package com.percussion.sitemanage.servlet;

import static com.percussion.pathmanagement.service.impl.PSPathUtils.SITES_FINDER_ROOT;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.pagemanagement.assembler.impl.PSFastForwardPreviewAssembly;
import com.percussion.pagemanagement.data.PSInlineLinkRequest;
import com.percussion.pagemanagement.data.PSInlineRenderLink;
import com.percussion.pagemanagement.service.impl.PSRenderLinkService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSRequestParsingException;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.system.utils.PSMutableUrl;
import com.percussion.utils.guid.IPSGuid;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The servlet used to preview content of the item, where the item is specified by its folder path.
 *
 * <p>Refactored for Java 11 and Google Java Style.
 *
 * <p>Final so the constructor may call Spring dependency injection with {@code this} without {@code
 * this-escape}.
 *
 * @author YuBingChen
 */
public final class PSPreviewItemContent extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger(PSPreviewItemContent.class);
  private static PSRenderLinkService linkService;
  private static IPSiteDao siteDao;

  public PSPreviewItemContent() {
    super();
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response) {
    var requestUri =
        PSFastForwardPreviewAssembly.siteOrAssetPathFromRequest(
            request.getRequestURI(),
            request.getContextPath(),
            request.getServletPath(),
            request.getPathInfo());
    var revision = request.getParameter(IPSHtmlParameters.SYS_REVISION);

    try {
      var type = request.getParameter("type");
      if (requestUri.endsWith("favicon.ico")) {
        return;
      }
      var url = createAssemblyUrl(requestUri, revision, type);
      var forwardReq = getRequestFromUrl(url, request);
      var disp = request.getRequestDispatcher("/assembler/render");
      disp.forward(forwardReq, response);
    } catch (ServletException
        | UnsupportedOperationException
        | PSCmsException
        | PSDataServiceException
        | PSRequestParsingException
        | PSNotFoundException
        | IOException e) {
      log.error(
          "Unable to preview resource:{} Error: {}",
          requestUri,
          PSExceptionUtils.getMessageForLog(e));
      try {
        response.sendError(404);
      } catch (IOException ioException) {
        response.setStatus(404);
      }
    }
  }

  private HttpServletRequest getRequestFromUrl(String url, HttpServletRequest request)
      throws PSRequestParsingException {
    var mutableUrl = new PSMutableUrl(url);
    // PSMutableUrl#getParamMap() is historically raw Map.
    Map<?, ?> params = mutableUrl.getParamMap();

    var params2 = new HashMap<String, String[]>();
    for (Map.Entry<?, ?> entry : params.entrySet()) {
      params2.put(
          String.valueOf(entry.getKey()),
          new String[] {entry.getValue() == null ? null : String.valueOf(entry.getValue())});
    }

    var wrapReq = new PSServletRequestWrapper(request);
    wrapReq.setParameterMap(params2);

    return wrapReq;
  }

  /**
   * Creates the assembly URL from the specified item path.
   *
   * @param path the path of the item, assumed not blank.
   * @param revision the id of the revision, can be blank
   * @param renderType it is "xml", "html" or "database", assumed not blank.
   * @return the assembly URL, not blank.
   */
  private String createAssemblyUrl(String path, String revision, String renderType)
      throws PSDataServiceException,
          PSNotFoundException,
          PSCmsException,
          UnsupportedEncodingException {
    var id = getItemId(path, revision);
    if (id != null) {
      var objMgr = PSCmsObjectMgrLocator.getObjectManager();
      var item = objMgr.loadComponentSummary(id.getUUID());

      if (item.isFolder()) {
        var siteSum = siteDao.findByPath("/" + path);
        if (!path.endsWith("/")) {
          path += "/";
        }
        path += siteSum.getDefaultDocument();
        // get default page id
        id = getItemId(path, revision);
      }
    }
    if (id == null) {
      throw new PSNotFoundException(path);
    }
    var linkRequest = new PSInlineLinkRequest();
    linkRequest.setTargetId(id.toString());
    PSInlineRenderLink renderLink;
    if (path.startsWith(SITES_FINDER_ROOT)) {
      if (isPercPageItem(id)) {
        renderLink = linkService.renderPreviewPageLink(id.toString(), renderType);
      } else {
        // FastForward rffHome and other legacy types are not percPage.
        // perc.base.plain NPEs on a missing template id (#3719).
        try {
          return createFastForwardAssemblyUrl(id, path, revision);
        } catch (PSAssemblyException e) {
          throw new PSNotFoundException(
              "No default page template for FastForward preview of " + id); 
        }
      }
    } else {
      renderLink = linkService.renderPreviewResourceLink(linkRequest);
    }
    if (renderLink == null || StringUtils.isBlank(renderLink.getUrl())) {
      throw new PSNotFoundException("Cannot build preview URL for path = \"" + path + "\".");
    }
    return renderLink.getUrl();
  }

  /**
   * True when the item is a CM1 {@code percPage} (or page template) that should use {@code
   * perc.base.plain}. FastForward types return {@code false}.
   */
  private boolean isPercPageItem(IPSGuid id) {
    try {
      var summary = PSCmsObjectMgrLocator.getObjectManager().loadComponentSummary(id.getUUID());
      if (summary == null) {
        return false;
      }
      String typeName =
          PSItemDefManager.getInstance().contentTypeIdToName(summary.getContentTypeId());
      return PSFastForwardPreviewAssembly.usesPercPageDispatcher(typeName);
    } catch (PSInvalidContentTypeException e) {
      log.debug("Content type lookup failed for {}: {}", id, e.toString());
      return false;
    }
  }

  /**
   * Assembler URL using the site default page template for a FastForward item.
   *
   * @param id item guid, never {@code null}
   * @param path finder path, never blank
   * @param revision optional revision
   * @return {@code /assembler/render?...}
   */
  private String createFastForwardAssemblyUrl(IPSGuid id, String path, String revision)
      throws PSNotFoundException, PSCmsException, PSAssemblyException {
    var objMgr = PSCmsObjectMgrLocator.getObjectManager();
    PSComponentSummary summary = objMgr.loadComponentSummary(id.getUUID());
    if (summary == null) {
      throw new PSNotFoundException("Cannot find item id = " + id);
    }
    int rev = -1;
    if (!StringUtils.isBlank(revision)) {
      rev = Integer.parseInt(revision);
    } else if (summary.getCurrentLocator() != null) {
      rev = summary.getCurrentLocator().getRevision();
    }
    Integer siteId = null;
    Collection<?> siteTemplates = List.of();
    String repoPath = PSPathUtils.getFolderPath(path);
    try {
      siteId =
          PSFastForwardPreviewAssembly.siteIdForRepositoryPath(
              repoPath, PSSiteManagerLocator.getSiteManager().findAllSites());
    } catch (Exception e) {
      log.warn("Site lookup via folder root failed for {}: {}", path, e.toString());
    }
    if (siteId == null && siteDao != null) {
      try {
        PSSiteSummary siteSum = siteDao.findByPath(repoPath);
        if (siteSum != null) {
          siteId = siteSum.getSiteId().map(Long::intValue).orElse(null);
        }
      } catch (Exception e) {
        log.warn("Site lookup via path failed for {}: {}", path, e.toString());
      }
    }
    if (siteId != null) {
      try {
        IPSSite site =
            PSSiteManagerLocator.getSiteManager()
                .loadUnmodifiableSite(new PSGuid(PSTypeEnum.SITE, siteId.longValue()));
        if (site != null && site.getAssociatedTemplates() != null) {
          siteTemplates = List.copyOf(site.getAssociatedTemplates());
        }
      } catch (Exception e) {
        log.debug("Site template load failed for site {}: {}", siteId, e.toString());
      }
    }
    if (siteId == null) {
      log.warn("FastForward preview of {} has no sys_siteid; preview filter requires a site", id);
    }

    IPSAssemblyService asm = PSAssemblyServiceLocator.getAssemblyService();
    var ctype = new PSGuid(PSTypeEnum.NODEDEF, summary.getContentTypeId());
    List<IPSAssemblyTemplate> byType = asm.findTemplatesByContentType(ctype);
    IPSAssemblyTemplate def =
        PSFastForwardPreviewAssembly.pickDefaultPageTemplate(byType, siteTemplates);
    if (def == null || def.getGUID() == null) {
      throw new PSNotFoundException(
          "No default page template for FastForward item " + id.getUUID());
    }

    Integer folderId = null;
    String parentPath = PSFastForwardPreviewAssembly.parentCmsPath(repoPath);
    if (StringUtils.isNotBlank(parentPath)) {
      int fid = PSServerFolderProcessor.getInstance().getIdByPath(parentPath);
      if (fid > 0) {
        folderId = fid;
      }
    }

    return PSFastForwardPreviewAssembly.buildAssemblerRenderUrl(
        id.getUUID(), rev, def.getGUID().getUUID(), siteId, folderId);
  }

  /**
   * Gets the ID of the item from its path.
   *
   * @param path the path of the item in question.
   * @param revision the id of the revision, can be blank
   * @return the ID of the item, never {@code null}.
   * @throws PSNotFoundException if cannot find the item from the path.
   */
  private IPSGuid getItemId(String path, String revision)
      throws PSNotFoundException, PSCmsException, UnsupportedEncodingException {
    path = escapeChars(path);
    path = PSPathUtils.getFolderPath(path);

    var revisionId = -1;
    var srv = PSServerFolderProcessor.getInstance();

    if (!StringUtils.isBlank(revision)) {
      revisionId = Integer.parseInt(revision);
    }

    var id = srv.getIdByPath(path);

    if (id == -1) {
      throw new PSNotFoundException("Cannot find item with path = \"" + path + "\".");
    }

    return new PSLegacyGuid(id, revisionId);
  }

  /**
   * Manually decode the '+' character as the browser may not do it correctly, which will cause the
   * {@code URLDecoder.decode} method to mistakenly replace it with a whitespace. Tested in the
   * following browsers:
   *
   * <ul>
   *   <li>Internet Explorer 8
   *   <li>Firefox 3.6
   *   <li>Safari 4
   *   <li>Google Chrome
   * </ul>
   *
   * @param path the path to decode manually
   * @return the same path but with the '+' symbol replaced by '%2B'
   */
  private String escapeChars(String path) throws UnsupportedEncodingException {
    var ret = path;
    var test = URLDecoder.decode(path, "utf8");
    if (!test.equals(path)) {
      ret = test;
    }
    return ret;
  }

  public static void setRenderLinkService(PSRenderLinkService service) {
    linkService = service;
  }

  public static PSRenderLinkService getRenderLinkService() {
    return linkService;
  }

  public static IPSiteDao getSiteDao() {
    return siteDao;
  }

  public static void setSiteDao(IPSiteDao dao) {
    siteDao = dao;
  }
}
