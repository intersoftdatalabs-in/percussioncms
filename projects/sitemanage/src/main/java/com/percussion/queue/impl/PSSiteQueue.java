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

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import com.percussion.monitor.process.PSImportProcessMonitor;
import com.percussion.server.PSRequest;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.notification.impl.PSNotificationService;
import com.percussion.share.dao.impl.PSIdMapper;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.importer.PSLink;
import com.percussion.utils.request.PSRequestInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Represents a queue for a site's page import process. */
public class PSSiteQueue {

  private PSSite site = null;
  private final TreeSet<Integer> catalogedIds = new TreeSet<>();
  private final TreeSet<Integer> importedIds = new TreeSet<>();
  private final HashMap<String, PSLink> importedLinks = new HashMap<>();
  private final TreeSet<Integer> importingIds = new TreeSet<>();
  private String userAgent = null;
  private int maxImportCount = 0;
  private static final String POUND = "#";
  private static final String SLASH = "/";

  private static final Logger log = LogManager.getLogger(PSSiteQueue.class);

  /** The actual request that will result in spawning a thread. */
  private Map<String, Object> requestInfoMap = null;

  private PSNotificationService notificationService;
  private PSIdMapper idService;

  public PSSiteQueue() {
    if (this.notificationService == null) {
      notificationService =
          (PSNotificationService) getWebApplicationContext().getBean("sys_notificationService");
    }
    idService = (PSIdMapper) getWebApplicationContext().getBean("sys_idMapper");

    notificationService.addListener(
        EventType.PAGE_DELETE,
        new IPSNotificationListener() {
          @Override
          public void notifyEvent(PSNotificationEvent event) {
            try {
              var guid = (String) event.getTarget();
              var id = idService.getContentId(guid);
              importedIds.remove(id);
            } catch (Exception e) {
              // Swallow exception, log if needed
            }
          }
        });
  }

  public PSSiteQueue(PSSite site, String userAgent) {
    notNull(site);
    notNull(userAgent);
    notEmpty(userAgent);
    this.site = site;
    this.userAgent = userAgent;
  }

  public void setProcessedLink(final String link, PSLink linkObject) {
    if (importedLinks.size() > 50000) {
      importedLinks.clear();
    }
    importedLinks.put(processLinkForCache(link), linkObject);
  }

  public PSLink getProcessedLink(final String link) {
    return importedLinks.get(processLinkForCache(link));
  }

  public boolean hasLinkBeenProcessed(final String link) {
    return importedLinks.containsKey(processLinkForCache(link));
  }

  private String processLinkForCache(final String link) {
    var finalPart = "";
    if (link.contains(SLASH)) {
      finalPart = link.substring(link.lastIndexOf(SLASH));
    }
    var processedLink = link;
    if (finalPart.contains(POUND)) {
      processedLink = processedLink.substring(0, processedLink.indexOf(POUND) - 1);
    }
    // Future filter for indices here.
    // if (finalPart.toLowerCase().startsWith("index")) {
    //     processedLink = processedLink.substring(0, processedLink.toLowerCase().indexOf("index") +
    // 5);
    // }
    return processedLink.toLowerCase();
  }

  public void clearProcessedLinkCache() {
    importedLinks.clear();
  }

  public int sizeProcessedLinkCache() {
    return importedLinks.size();
  }

  public void setMaxImportCount(int max) {
    maxImportCount = max;
  }

  public int getMaxImportCount() {
    return maxImportCount;
  }

  public void setUserAgent(String userAgent) {
    notNull(userAgent);
    notEmpty(userAgent);
    this.userAgent = userAgent;
  }

  public void setSite(PSSite s) {
    site = s;
  }

  public String getUserAgent() {
    return userAgent;
  }

  /**
   * Get the request info. Must call {@link #setRequestInfoMap()} first. This is used to set the
   * request info for each importing page process.
   *
   * @return the stored request info, never null
   */
  public Map<String, Object> getRequestInfoMap() {
    if (requestInfoMap == null) {
      throw new IllegalStateException("The request info has not been set yet.");
    }
    return requestInfoMap;
  }

  /**
   * Set the request info. This cannot be called if the request info has not been initialized in the
   * current thread. It does nothing if the request info has already been configured.
   */
  public void setRequestInfoMap() {
    if (requestInfoMap != null) {
      return;
    }
    if (!PSRequestInfo.isInited()) {
      throw new IllegalStateException("The request info has not been initialized.");
    }
    requestInfoMap = PSRequestInfo.copyRequestInfoMap();
    var request = (PSRequest) requestInfoMap.get(PSRequestInfo.KEY_PSREQUEST);
    requestInfoMap.put(PSRequestInfo.KEY_PSREQUEST, request.cloneRequest());
  }

  public PSSite getSite() {
    return site;
  }

  public List<Integer> getImportingIds() {
    return new ArrayList<>(importingIds);
  }

  public void removeImportingId(Integer id) {
    importingIds.remove(id);
    checkSearchIndexQueueStatus();
  }

  /**
   * Ensure the search index queue is paused while there is a site importing, and resumed once it is
   * done.
   */
  public void checkSearchIndexQueueStatus() {
    checkSearchIndexQueueStatus(true);
  }

  /**
   * Ensure the search index queue is paused while there is a site importing, and resumed once it is
   * done.
   *
   * @param checkImporting true to check if a page is importing, false to only consider if there are
   *     cataloged pages waiting for import
   */
  public void checkSearchIndexQueueStatus(boolean checkImporting) {
    // Need to start and stop during the entire import job.
    // No-op for now.
  }

  public synchronized boolean containsPagesForImport() {
    return !catalogedIds.isEmpty() && site != null && userAgent != null && !isMaxCountReached();
  }

  public synchronized List<Integer> getCatalogedIds() {
    return new ArrayList<>(catalogedIds);
  }

  public synchronized List<Integer> getImportedIds() {
    return new ArrayList<>(importedIds);
  }

  public synchronized void addCatalogedIds(List<Integer> ids) {
    // De-dupe the IDs if there is any
    catalogedIds.removeAll(ids);
    catalogedIds.addAll(ids);
    checkSearchIndexQueueStatus();
    PSImportProcessMonitor.setCatalogCount(catalogedIds.size());
  }

  public synchronized Integer getNextId() {
    try {
      if (isMaxCountReached()) {
        return handleReachMaxCount();
      }
      if (catalogedIds.isEmpty()) {
        return handleEmptyCatalogedIds();
      }
      return processNextCatalogedId();
    } finally {
      checkSearchIndexQueueStatus();
    }
  }

  private Integer processNextCatalogedId() {
    var nextId = catalogedIds.first();
    importingIds.add(nextId);
    catalogedIds.remove(nextId);
    logState();
    PSImportProcessMonitor.setCatalogCount(catalogedIds.size());
    return nextId;
  }

  private Integer handleEmptyCatalogedIds() {
    return null;
  }

  private Integer handleReachMaxCount() {
    logState();
    PSImportProcessMonitor.setCatalogCount(0);
    return null;
  }

  private boolean isMaxCountReached() {
    if (maxImportCount < 0) {
      return false;
    }
    var currentCount = importedIds.size();
    if (!importingIds.isEmpty()) {
      currentCount += importingIds.size();
    }
    return currentCount >= maxImportCount;
  }

  private void logState() {
    if (!log.isDebugEnabled()) {
      return;
    }
    log.debug("[getNextId] importingIds: {}", importingIds);
    log.debug("[getNextId] catalogedIds: {}", catalogedIds);
    log.debug("[getNextId] importedIds: {}", importedIds);
  }

  public void addImportedId(Integer id) {
    importedIds.add(id);
    importingIds.remove(id);
    checkSearchIndexQueueStatus();
  }

  public synchronized void addImportedIds(List<Integer> ids) {
    // De-dupe the IDs if there is any
    importedIds.removeAll(ids);
    importedIds.addAll(ids);
    importingIds.removeAll(ids);
    checkSearchIndexQueueStatus();
  }

  public synchronized void removeImportedId(Integer id) {
    importedIds.remove(id);
    importingIds.remove(id);
    checkSearchIndexQueueStatus();
  }
}
