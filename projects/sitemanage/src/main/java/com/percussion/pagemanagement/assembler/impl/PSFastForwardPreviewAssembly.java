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
package com.percussion.pagemanagement.assembler.impl;

import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSAssemblyTemplate.OutputFormat;
import com.percussion.services.assembly.IPSAssemblyTemplate.PublishWhen;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Preview assembly helpers for FastForward / legacy Rhythmyx types (rffHome and
 * peers). Those items are not {@code percPage}; assembling them with {@code
 * perc.base.plain} NPEs on a missing percPage template id (#3719).
 */
public final class PSFastForwardPreviewAssembly {

  private PSFastForwardPreviewAssembly() {}

  /**
   * {@code percPage} and {@code percPageTemplate} use the CM1 dispatch template.
   * FastForward content types must use the site default page template instead.
   *
   * @param contentTypeName internal type name, may be blank
   * @return {@code true} when perc.base.plain is the correct dispatcher
   */
  /**
   * Site associated templates without touching a lazy Hibernate collection that has no session
   * (Page Management Preview {@code getItemSites} sites — #3809).
   *
   * @param site may be {@code null}
   * @return never {@code null}; empty when the collection cannot be read
   */
  public static Collection<?> associatedTemplatesSafe(IPSSite site) {
    if (site == null) {
      return List.of();
    }
    try {
      Collection<?> templates = site.getAssociatedTemplates();
      if (templates == null || templates.isEmpty()) {
        return List.of();
      }
      return List.copyOf(templates);
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  public static boolean usesPercPageDispatcher(String contentTypeName) {
    if (StringUtils.isBlank(contentTypeName)) {
      return false;
    }
    return IPSPageService.PAGE_CONTENT_TYPE.equals(contentTypeName)
        || IPSTemplateService.TPL_CONTENT_TYPE.equals(contentTypeName);
  }

  /**
   * Picks the default page or binary template for a content type, preferring
   * templates associated with the site. When the site set is empty, falls back
   * to any default page/binary template on the type (sorted by name).
   *
   * @param byContentType templates for the item's content type, may be null
   * @param siteTemplates site associated templates or GUIDs, may be null
   * @return chosen template, or {@code null} when none qualify
   */
  public static IPSAssemblyTemplate pickDefaultPageTemplate(
      List<IPSAssemblyTemplate> byContentType, Collection<?> siteTemplates) {
    if (byContentType == null || byContentType.isEmpty()) {
      return null;
    }
    Set<String> siteKeys = siteTemplateKeys(siteTemplates);
    List<IPSAssemblyTemplate> siteMatches = new ArrayList<>();
    List<IPSAssemblyTemplate> fallback = new ArrayList<>();
    for (IPSAssemblyTemplate template : byContentType) {
      if (!isDefaultPageOrBinary(template)) {
        continue;
      }
      fallback.add(template);
      IPSGuid guid = template.getGUID();
      if (guid != null && siteKeys.contains(guid.toString())) {
        siteMatches.add(template);
      }
    }
    List<IPSAssemblyTemplate> chosen = siteMatches.isEmpty() ? fallback : siteMatches;
    if (chosen.isEmpty()) {
      return null;
    }
    chosen.sort(
        Comparator.comparing(
            t -> StringUtils.defaultString(t.getName()), String.CASE_INSENSITIVE_ORDER));
    return chosen.get(0);
  }

  /**
   * Assembler preview path used by {@code PSPreviewItemContent} forward.
   * CMS URL path (always {@code /}), not an OS filesystem path.
   *
   * @param contentId content id
   * @param revision revision, {@code <= 0} omitted
   * @param templateId template / variant uuid
   * @param siteId optional RXSITES id
   * @param folderId optional parent folder content id
   * @return {@code /assembler/render?...}, never blank
   */
  public static String buildAssemblerRenderUrl(
      int contentId, int revision, int templateId, Integer siteId, Integer folderId) {
    StringBuilder sb = new StringBuilder("/assembler/render?");
    sb.append(IPSHtmlParameters.SYS_CONTENTID).append('=').append(contentId);
    sb.append('&').append(IPSHtmlParameters.SYS_TEMPLATE).append('=').append(templateId);
    if (revision > 0) {
      sb.append('&').append(IPSHtmlParameters.SYS_REVISION).append('=').append(revision);
    }
    sb.append('&').append(IPSHtmlParameters.SYS_CONTEXT).append("=0");
    // XOR: sys_itemfilter XOR sys_authtype (both set throws filter error 7).
    sb.append('&').append(IPSHtmlParameters.SYS_ITEMFILTER).append("=preview");
    if (siteId != null) {
      sb.append('&').append(IPSHtmlParameters.SYS_SITEID).append('=').append(siteId);
    }
    if (folderId != null && folderId > 0) {
      sb.append('&').append(IPSHtmlParameters.SYS_FOLDERID).append('=').append(folderId);
    }
    return sb.toString();
  }

  /**
   * Finder path ({@code /Sites/…} or {@code /Assets/…}) from a servlet request.
   * Prefers servlet path + path info; strips context from {@code requestURI}
   * when the servlet path is not a Sites/Assets mapping.
   *
   * @param requestUri {@link jakarta.servlet.http.HttpServletRequest#getRequestURI()}
   * @param contextPath context path, may be blank
   * @param servletPath servlet path, may be blank
   * @param pathInfo extra path, may be blank
   * @return finder path starting with {@code /}, never {@code null}
   */
  public static String siteOrAssetPathFromRequest(
      String requestUri, String contextPath, String servletPath, String pathInfo) {
    String fromServlet = joinServletPath(servletPath, pathInfo);
    if (isSitesOrAssetsPath(fromServlet)) {
      return fromServlet;
    }
    String path = StringUtils.defaultString(requestUri);
    String ctx = StringUtils.defaultString(contextPath);
    if (!ctx.isEmpty() && !"/".equals(ctx) && path.startsWith(ctx)) {
      path = path.substring(ctx.length());
    }
    if (path.isEmpty()) {
      path = fromServlet;
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    return path;
  }

  /**
   * Parent of a repository path ({@code //Sites/site/item} → {@code //Sites/site}).
   * CMS path segments use {@code /} (not OS separators).
   *
   * @param folderPath repository or finder path, may be blank
   * @return parent path, or the input when there is no parent segment
   */
  /**
   * First site whose folder root contains {@code repoPath}. Reads only {@code
   * getFolderRoot()} / {@code getGUID()} so Hibernate lazy template
   * collections are not touched.
   *
   * @param repoPath repository path such as {@code //Sites/CorporateInvestments/Home}
   * @param sites sites to search, may be null
   * @return site uuid or {@code null}
   */
  public static Integer siteIdForRepositoryPath(String repoPath, Collection<? extends IPSSite> sites) {
    if (StringUtils.isBlank(repoPath) || sites == null) {
      return null;
    }
    for (IPSSite site : sites) {
      if (site == null || site.getGUID() == null) {
        continue;
      }
      String root = site.getFolderRoot();
      if (StringUtils.isBlank(root)) {
        continue;
      }
      String normalized = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
      if (repoPath.equals(normalized) || repoPath.startsWith(normalized + "/")) {
        return site.getGUID().getUUID();
      }
    }
    return null;
  }

  public static String parentCmsPath(String folderPath) {
    if (StringUtils.isBlank(folderPath)) {
      return folderPath;
    }
    String path = folderPath;
    while (path.length() > 2 && path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    int slash = path.lastIndexOf('/');
    if (slash <= 1) {
      return path;
    }
    return path.substring(0, slash);
  }

  static boolean isDefaultPageOrBinary(IPSAssemblyTemplate template) {
    if (template == null) {
      return false;
    }
    if (template.getPublishWhen() != PublishWhen.Default) {
      return false;
    }
    OutputFormat format = template.getOutputFormat();
    return format == OutputFormat.Page || format == OutputFormat.Binary;
  }

  private static Set<String> siteTemplateKeys(Collection<?> siteTemplates) {
    Set<String> keys = new HashSet<>();
    if (siteTemplates == null) {
      return keys;
    }
    for (Object entry : siteTemplates) {
      if (entry instanceof IPSAssemblyTemplate template && template.getGUID() != null) {
        keys.add(template.getGUID().toString());
      } else if (entry instanceof IPSGuid guid) {
        keys.add(guid.toString());
      }
    }
    return keys;
  }

  private static String joinServletPath(String servletPath, String pathInfo) {
    String base = StringUtils.defaultString(servletPath);
    String extra = StringUtils.defaultString(pathInfo);
    if (base.isEmpty()) {
      return extra;
    }
    if (extra.isEmpty()) {
      return base;
    }
    if (base.endsWith("/") && extra.startsWith("/")) {
      return base + extra.substring(1);
    }
    return base + extra;
  }

  private static boolean isSitesOrAssetsPath(String path) {
    if (StringUtils.isBlank(path)) {
      return false;
    }
    return path.equals("/Sites")
        || path.startsWith("/Sites/")
        || path.equals("/Assets")
        || path.startsWith("/Assets/");
  }
}
