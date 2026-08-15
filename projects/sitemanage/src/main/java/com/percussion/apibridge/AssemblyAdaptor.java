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

package com.percussion.apibridge;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.rest.assembly.IAssemblyAdaptor;
import com.percussion.rest.assembly.PreviewLocation;
import com.percussion.server.PSServer;
import com.percussion.server.cache.IPSCacheHandler;
import com.percussion.server.cache.PSAssemblerCacheHandler;
import com.percussion.server.cache.PSCacheManager;
import com.percussion.services.assembly.impl.nav.PSNavConfig;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.system.utils.PSSiteManageBean;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Builds assembly preview locations for Explorer template preview. Same query shape as {@code
 * PSTemplateActionMenuHelper} / {@code PSGeneratePubLocation} preview context — not a Data Flow
 * HTML page.
 */
@PSSiteManageBean
public class AssemblyAdaptor implements IAssemblyAdaptor {

  private static final Logger log = LogManager.getLogger(AssemblyAdaptor.class);

  /** Classic assembler render path (context-root relative). */
  static final String ASSEMBLER_RENDER_PATH = "/assembler/render";

  private final RevisionLookup revisions;
  private final String requestRoot;
  private final CacheFlusher cacheFlusher;
  private final NavReset navReset;

  public AssemblyAdaptor() {
    this(
        PSServer.getRequestRoot(),
        AssemblyAdaptor::lookupCurrentRevision,
        AssemblyAdaptor::flushAllAssemblerPages,
        AssemblyAdaptor::reloadNavConfig);
  }

  /** Package-visible for unit tests. Delegates to the four-arg constructor. */
  AssemblyAdaptor(String requestRoot, RevisionLookup revisions) {
    this(
        requestRoot,
        revisions,
        AssemblyAdaptor::flushAllAssemblerPages,
        AssemblyAdaptor::reloadNavConfig);
  }

  /** Package-visible for unit tests. */
  AssemblyAdaptor(
      String requestRoot, RevisionLookup revisions, CacheFlusher cacheFlusher, NavReset navReset) {
    this.requestRoot = requestRoot == null ? "" : requestRoot;
    this.revisions = revisions;
    this.cacheFlusher = cacheFlusher;
    this.navReset = navReset;
  }

  @Override
  public PreviewLocation previewLocation(int contentId, int templateId, Integer revision) {
    Integer rev = revision;
    if (rev == null || rev <= 0) {
      rev = revisions.currentRevision(contentId);
    }
    if (rev == null || rev <= 0) {
      log.debug("No revision for content id {}", contentId);
      return null;
    }
    String url = buildPreviewUrl(requestRoot, contentId, templateId, rev);
    return new PreviewLocation(url, contentId, templateId, rev);
  }

  @Override
  public void flushAssemblerCache() {
    cacheFlusher.flush();
  }

  @Override
  public void resetNavigation() {
    navReset.reset();
  }

  /**
   * Empty keys flush all assembler pages — same as {@code PSExitFlushAssemblerCache} with omitted
   * keys. Not scoped to a selected item.
   */
  static void flushAllAssemblerPages() {
    PSCacheManager mgr = PSCacheManager.getInstance();
    IPSCacheHandler handler = mgr.getCacheHandler(PSAssemblerCacheHandler.HANDLER_TYPE);
    if (handler == null) {
      return;
    }
    String[] keys = handler.getKeyNames();
    Map<String, String> keyMap = new HashMap<>();
    for (String key : keys) {
      keyMap.put(key, "");
    }
    mgr.flush(keyMap);
  }

  /**
   * Same goal as classic {@code PSNavReset}. On 8.2 FastForward {@code PSNavConfig.reset} is
   * typically a no-op once navigation is loaded ({@code m_allVariants == null}).
   */
  static void reloadNavConfig() {
    PSNavConfig.getInstance();
    try {
      PSNavConfig.reset(null);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Managed navigation reset failed", e);
    }
  }

  /**
   * Preview context {@code 0} + {@code preview} item filter — same as template action menus.
   */
  static String buildPreviewUrl(String requestRoot, int contentId, int templateId, int revision) {
    String root = StringUtils.defaultString(requestRoot).trim();
    if (root.endsWith("/")) {
      root = root.substring(0, root.length() - 1);
    }
    String path = root + ASSEMBLER_RENDER_PATH;
    return path
        + "?"
        + IPSHtmlParameters.SYS_CONTENTID
        + "="
        + encode(Integer.toString(contentId))
        + "&"
        + IPSHtmlParameters.SYS_TEMPLATE
        + "="
        + encode(Integer.toString(templateId))
        + "&"
        + IPSHtmlParameters.SYS_REVISION
        + "="
        + encode(Integer.toString(revision))
        + "&"
        + IPSHtmlParameters.SYS_CONTEXT
        + "=0&"
        + IPSHtmlParameters.SYS_ITEMFILTER
        + "=preview";
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static Integer lookupCurrentRevision(int contentId) {
    try {
      IPSCmsObjectMgr mgr = PSCmsObjectMgrLocator.getObjectManager();
      PSComponentSummary sum = mgr.loadComponentSummary(contentId);
      if (sum == null || sum.getCurrentLocator() == null) {
        return null;
      }
      return sum.getCurrentLocator().getRevision();
    } catch (RuntimeException e) {
      log.debug("Component summary load failed for {}: {}", contentId, e.toString());
      return null;
    }
  }

  @FunctionalInterface
  interface RevisionLookup {
    Integer currentRevision(int contentId);
  }

  @FunctionalInterface
  interface CacheFlusher {
    void flush();
  }

  @FunctionalInterface
  interface NavReset {
    void reset();
  }
}
