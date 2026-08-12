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

package com.percussion.queue.impl;

import static com.percussion.share.service.IPSSystemProperties.IMPORT_PAGE_MAX;

import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.queue.IPSPageImportQueue;
import com.percussion.queue.PSAbstractEventQueue;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.service.IPSSiteImportService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Queue for managing page imports for sites. */
@Component("pageImportQueue")
@Lazy
@Transactional
public class PSPageImportQueue extends PSAbstractEventQueue<PSSiteQueue>
    implements IPSPageImportQueue, IPSNotificationListener, IPSPerformPageImport {

  private final Map<Long, PSSiteQueue> siteCache = new ConcurrentHashMap<>();
  private PSSiteQueue importingSite;
  private final PSSiteImportCtx importContext = new PSSiteImportCtx();

  private final IPSSiteImportService importService;
  private final IPSIdMapper idMapper;
  private IPSSystemProperties systemProps;

  private boolean isServerStarted = false;
  private final IPSPageCatalogService pageCatalogService;
  private final IPSSiteManager siteMgr;
  private static final Object siteQueueLock = new Object();

  private static final Logger log = LogManager.getLogger(PSPageImportQueue.class);

  private IPSPerformPageImport pageImporter = this;

  /**
   * Intentional publish-to-registry of {@code this} as a notification listener during
   * construction. Justified {@code this-escape} suppress: not made {@code final} because the bean
   * is {@code @Transactional} (CGLIB-friendly); registration is required for server lifecycle.
   */
  @SuppressWarnings("this-escape")
  @Autowired
  public PSPageImportQueue(
      @Qualifier("pageImportService") IPSSiteImportService importService,
      IPSIdMapper idMapper,
      IPSNotificationService notifyService,
      IPSPageCatalogService pageCatalogService,
      IPSSiteManager siteMgr) {
    super();
    this.importService = importService;
    this.idMapper = idMapper;
    this.pageCatalogService = pageCatalogService;
    this.siteMgr = siteMgr;
    registerServerShutdownNotification(notifyService);
  }

  private void registerServerShutdownNotification(IPSNotificationService notifyService) {
    notifyService.addListener(EventType.CORE_SERVER_SHUTDOWN, this);
    notifyService.addListener(EventType.CORE_SERVER_INITIALIZED, this);
    notifyService.addListener(EventType.SITE_DELETED, this);
  }

  @Override
  public void addCatalogedPageIds(PSSite site, String userAgent, List<Integer> ids) {
    var sq = getSiteQueue(site.getSiteId().orElse(null));
    if (sq.getUserAgent() == null) {
      sq.setSite(site);
      sq.setUserAgent(userAgent);
      sq.setRequestInfoMap();
    }
    sq.addCatalogedIds(ids);
    if (log.isDebugEnabled()) {
      log.debug("Site[{}][addCatalogedPageIds] ids = {}", site.getSiteId(), ids);
    }
    notifyEventQueue();
  }

  @Override
  public void removeImportPage(String siteName, String pageId) {
    var site = siteMgr.findSite(siteName);
    if (site == null) {
      return;
    }
    // siteMgr returns an IPSSite (interface) whose getSiteId() returns a Long,
    // not an Optional.  We were mistakenly calling orElse() on the result which
    // caused a compilation error because Long does not have that method.
    // Simply pass the raw value (which may be null) to getSiteQueue().
    var sq = getSiteQueue(site.getSiteId());
    var id = idMapper.getContentId(pageId);
    sq.removeImportedId(id);
  }

  @Override
  public void notifyEvent(PSNotificationEvent notification) {
    if (notification.getType() == EventType.CORE_SERVER_INITIALIZED) {
      setMaxImportCountForAllSites();
      isServerStarted = true;
      start();
    } else if (notification.getType() == EventType.CORE_SERVER_SHUTDOWN) {
      importContext.setCanceled(true);
      doShutdown();
    } else if (notification.getType() == EventType.SITE_DELETED) {
      deleteSiteCache(notification);
    }
  }

  /** Delegate shutdown to another thread to unblock the notification service. */
  private void doShutdown() {
    var runner = new Thread(this::shutdown);
    runner.setDaemon(true);
    runner.start();
  }

  private int getMaxImportPage() {
    var maxVal = systemProps.getProperty(IMPORT_PAGE_MAX);
    return NumberUtils.toInt(maxVal, -1);
  }

  @Autowired
  public void setSystemProps(IPSSystemProperties systemProps) {
    this.systemProps = systemProps;
    if (isServerStarted) {
      setMaxImportCountForAllSites();
    }
  }

  public IPSSystemProperties getSystemProps() {
    return systemProps;
  }

  @Override
  public List<Integer> getImportingPageIds(Long siteId) {
    return getSiteQueue(siteId).getImportingIds();
  }

  @Override
  public PSSiteQueue getPageIds(PSSiteImportCtx context) {
    return getSiteQueue(context);
  }

  @Override
  public PSSiteQueue getPageIds(Long siteId) {
    return getSiteQueue(siteId);
  }

  @Override
  public List<Integer> getCatalogedPageIds(Long siteId) {
    return getSiteQueue(siteId).getCatalogedIds();
  }

  @Override
  public List<Integer> getImportedPageIds(Long siteId) {
    return getSiteQueue(siteId).getImportedIds();
  }

  @Override
  public void addImportedId(Long siteId, Integer id) {
    getSiteQueue(siteId).addImportedId(id);
  }

  @Override
  protected String getQueueName() {
    return "PageImportQueue";
  }

  @Override
  protected void preStart() {
    // No-op for now
  }

  @Override
  protected boolean doRun() {
    try {
      PSPair<PSSiteQueue, Integer> nextEvent;
      try {
        nextEvent = getNextQueueEvent(0);
        if (nextEvent == null) {
          return true;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      } catch (Throwable t) {
        if (isThreadDeath(t)) {
          return false;
        }
        return true;
      }
      var importingSite = nextEvent.getFirst();
      if (importingSite == null) {
        return true;
      }
      var ids = importingSite.getImportingIds();
      if (ids.isEmpty()) {
        return true;
      }
      var site = importingSite.getSite();
      Integer id = nextEvent.getSecond();
      try {
        setRequestInfo(importingSite);
        pageImporter.performPageImport(site, id, importingSite.getUserAgent());
        importingSite.checkSearchIndexQueueStatus(false);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      } catch (Throwable t) {
        if (isThreadDeath(t)) {
          return false;
        }
        log.error(
            "Failed to import page id={}, for site: {}, Error: {}",
            id,
            site.getName(),
            t.getMessage());
        log.debug(t.getMessage(), t);
        importingSite.removeImportingId(id);
      }
      return true;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * {@link ThreadDeath} is deprecated for removal; keep historical stop-thread behavior without
   * littering call sites with removal warnings.
   */
  @SuppressWarnings("removal")
  private static boolean isThreadDeath(Throwable t) {
    return t instanceof ThreadDeath;
  }

  private void setRequestInfo(PSSiteQueue importingSite) {
    if (PSRequestInfo.isInited()) {
      PSRequestInfo.resetRequestInfo();
    }
    PSRequestInfo.initRequestInfo(importingSite.getRequestInfoMap());
  }

  @Override
  public void performPageImport(PSSite site, Integer id, String userAgent)
      throws InterruptedException, PSSiteImportException {
    var pageId = idMapper.getString(new PSLegacyGuid(id, -1));
    var siteImportContext = new PSSiteImportCtx();
    siteImportContext.setCanceled(this.importContext.isCanceled());
    siteImportContext.setImportConfiguration(
        this.importContext.getImportConfiguration().orElse(null));
    importService.importCatalogedPage(site, pageId, userAgent, siteImportContext);
  }

  public void setPageImporter(IPSPerformPageImport pageImporter) {
    this.pageImporter = pageImporter;
  }

  public IPSPerformPageImport getPageImporter() {
    return pageImporter;
  }

  @Override
  protected void preShutdown() {
    importContext.setCanceled(true);
  }

  @Override
  protected PSPair<PSSiteQueue, Integer> getNextEvent() {
    var pair = new PSPair<PSSiteQueue, Integer>();
    if (importingSite == null) {
      var sq = getNextWaitingSite();
      if (sq == null) {
        return null;
      }
      importingSite = sq;
    }
    var id = importingSite.getNextId();
    if (id == null) {
      importingSite = getNextWaitingSite();
      if (importingSite != null) {
        id = importingSite.getNextId();
        if (id == null) {
          return null;
        }
      }
    }
    pair.setFirst(importingSite);
    pair.setSecond(id);
    return pair;
  }

  private void setMaxImportCountForAllSites() {
    var max = getMaxImportPage();
    for (var sq : siteCache.values()) {
      sq.setMaxImportCount(max);
    }
  }

  private void deleteSiteCache(PSNotificationEvent notification) {
    var siteId = (IPSGuid) notification.getTarget();
    var id = siteId.longValue();
    siteCache.remove(id);
  }

  private PSSiteQueue getNextWaitingSite() {
    for (var sq : siteCache.values()) {
      if (sq.containsPagesForImport()) {
        return sq;
      }
    }
    return null;
  }

  private PSSiteQueue getSiteQueue(Long siteId) {
    var siteQueue = siteCache.get(siteId);
    if (siteQueue == null) {
      synchronized (siteQueueLock) {
        if (siteCache.get(siteId) == null) {
          siteQueue = createSiteQueue(siteId);
          siteQueue.setMaxImportCount(getMaxImportPage());
          siteCache.put(siteId, siteQueue);
        }
      }
    }
    return siteQueue;
  }

  private PSSiteQueue getSiteQueue(PSSiteImportCtx context) {
    var siteId = context.getSite().flatMap(PSSite::getSiteId).orElse(null);
    var siteQueue = siteCache.get(siteId);
    if (siteQueue == null) {
      synchronized (siteQueueLock) {
        if (siteCache.get(siteId) == null) {
          siteQueue = createSiteQueue(context);
          importContext.setImportConfiguration(context.getImportConfiguration().orElse(null));
          siteQueue.setMaxImportCount(getMaxImportPage());
          siteCache.put(siteId, siteQueue);
        }
      }
    }
    return siteQueue;
  }

  @Override
  public void dirtySiteQueue(Long siteId) {
    siteCache.remove(siteId);
  }

  private PSSiteQueue createSiteQueue(Long siteId) {
    var siteName = getSiteName(siteId);
    try {
      if (siteName == null) {
        return new PSSiteQueue();
      }
      var siteQueue = new PSSiteQueue();
      var importedPages = pageCatalogService.findImportedPageIds(siteName);
      siteQueue.addImportedIds(getContentIds(importedPages));
      var catalogedPages = pageCatalogService.findCatalogPages(siteName);
      siteQueue.addCatalogedIds(getContentIds(catalogedPages));
      return siteQueue;
    } catch (Exception e) {
      log.error(
          "An error occurred when getting the imported and cataloged pages for site name: {},"
              + " Error: {}",
          siteName,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return new PSSiteQueue();
    }
  }

  private PSSiteQueue createSiteQueue(PSSiteImportCtx context) {
    var siteId = context.getSite().flatMap(PSSite::getSiteId).orElse(null);
    return getSiteQueue(siteId);
  }

  private String getSiteName(Long siteId) {
    try {
      var id = new PSGuid(PSTypeEnum.SITE, siteId);
      var site = siteMgr.findSite(id);
      return site != null ? site.getName() : null;
    } catch (Throwable e) {
      return null;
    }
  }

  /**
   * Builds a list of content ids from a list of complete ids.
   *
   * @param ids list of string representation of ids, not null
   * @return list of content ids, never null but may be empty
   */
  private List<Integer> getContentIds(List<String> ids) {
    var contentIds = new ArrayList<Integer>();
    for (var id : ids) {
      contentIds.add(((PSLegacyGuid) idMapper.getGuid(id)).getContentId());
    }
    return contentIds;
  }
}
