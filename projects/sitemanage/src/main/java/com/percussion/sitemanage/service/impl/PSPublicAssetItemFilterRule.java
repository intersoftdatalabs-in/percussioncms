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
package com.percussion.sitemanage.service.impl;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.itemmanagement.service.impl.PSAbstractWorkflowExtension;
import com.percussion.itemmanagement.service.impl.PSAbstractWorkflowExtension.WorkflowItem.AssetType;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.services.filter.IPSFilterItem;
import com.percussion.services.filter.IPSItemFilterRule;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.service.IPSSitePublishService;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.webservices.system.PSSystemWsLocator;
import java.io.File;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Filter for publish and unpublish items. Removes assets not in correct workflow states and
 * corrects revisions of non-local assets. If sys_publish=unpublish, removes items that have public
 * revision (or are publishable).
 */
public class PSPublicAssetItemFilterRule extends PSAbstractWorkflowExtension
    implements IPSItemFilterRule {

  private static final String LIVE_STATE = "Live";
  public static final String PUBLISH_IGNORE_UNMODIFIED_ASSETS_PROPERTY = "ignoreUnModifiedAssets";

  @Autowired private IPSRecycleService recyclerService;
  private IPSFolderHelper folderHelper;
  private IPSPublisherService pubService;
  private IPSPubServerService pubServerService;
  private IPSGuidManager guidMgr;
  private IPSSitePublishService sitePublishService;
  private IPSSiteManager siteMgr;
  private IPSContentChangeService contentChangeService;

  @Override
  public List<IPSFilterItem> filter(List<IPSFilterItem> items, Map<String, String> params) {
    boolean isPublish = !"unpublish".equals(params.get(IPSHtmlParameters.SYS_PUBLISH));
    var pubServer = findPubServer(params.get(IPSHtmlParameters.SYS_EDITIONID));
    var ignoreAssets =
        pubServer == null
            ? "false"
            : pubServer.getPropertyValue(PUBLISH_IGNORE_UNMODIFIED_ASSETS_PROPERTY);
    boolean ignoreUnModAssets = StringUtils.equals(ignoreAssets, "true");
    Long serverId = pubServer == null ? null : pubServer.getServerId();

    var worker = getWorker(params);
    var rvalue = new ArrayList<IPSFilterItem>();
    List<Integer> changedIds = null;
    Set<Integer> changedIdsSet = null;
    if (ignoreUnModAssets && contentChangeService != null && pubServer != null) {
      changedIds =
          contentChangeService.getChangedContent(
              pubServer.getSiteId(), PSContentChangeType.PENDING_LIVE);
      changedIdsSet = new HashSet<>(changedIds);
    }
    for (var item : items) {
      try {
        var wfItem = worker.getWorkflowItem(item.getItemId());
        var r =
            process(worker, item, wfItem, isPublish, ignoreUnModAssets, serverId, changedIdsSet);
        if (r != null) rvalue.add(r);
      } catch (Exception e) {
        log.warn("Filter removing item: {} because of error: {}", item.getItemId(), e.getMessage());
        log.debug("Filtered item stack trace: ", e);
      }
    }
    return rvalue;
  }

  /**
   * @param worker never null.
   * @param original never null.
   * @param wfItem never null.
   * @param isPublish is publish operation.
   * @param ignoreUnModAssets ignore unmodified assets.
   * @param serverId server id.
   * @param changedIdsSet set of changed ids.
   * @return null if the item should be removed from the original list.
   */
  protected IPSFilterItem process(
      WorkflowItemWorker worker,
      IPSFilterItem original,
      WorkflowItem wfItem,
      boolean isPublish,
      boolean ignoreUnModAssets,
      Long serverId,
      Set<Integer> changedIdsSet) {
    if (wfItem == null) {
      return null;
    }

    if (wfItem.assetType != AssetType.LOCAL && wfItem.assetType != AssetType.PAGE) {
      if (!isPublishableAsset(original, wfItem, ignoreUnModAssets, serverId, changedIdsSet)) {
        return null;
      }
    }

    if (wfItem.assetType == AssetType.LOCAL) {
      if (log.isDebugEnabled()) log.debug("Found local asset: {}", wfItem);
      return original;
    } else if ((!isPublish) && (!wfItem.publishable)) {
      return original;
    } else if (isPublish
        && wfItem.publishable
        && isScheduled(wfItem)
        && wfItem.publicRevision > 0) {
      int oldRevision = wfItem.publicRevision;
      var systemws = PSSystemWsLocator.getSystemWebservice();
      var auditTrails = systemws.loadAuditTrails(Collections.singletonList(original.getItemId()));
      var historyList = auditTrails.get(original.getItemId());
      Collections.reverse(historyList);
      for (var historyItem : historyList) {
        if (LIVE_STATE.equals(historyItem.getStateName())) {
          oldRevision = historyItem.getRevision();
          log.debug(
              "Publishing previous Item Revision: {} Name:{}",
              oldRevision,
              wfItem.itemSummary.getName());
          break;
        }
      }
      if (log.isDebugEnabled())
        log.debug(
            "Keeping original page or shared asset {} due to scheduling: {}", oldRevision, wfItem);

      var newGuid = worker.makeGuidFromRevision(original.getItemId(), oldRevision);
      return original.getItemId().equals(newGuid) ? original : original.clone(newGuid);
    } else if (isPublish && wfItem.publishable && !isScheduled(wfItem)) {
      log.debug("Keeping page or shared asset: {}", wfItem);
      var newGuid = worker.makeGuidFromRevision(original.getItemId(), wfItem.publicRevision);
      return original.getItemId().equals(newGuid) ? original : original.clone(newGuid);
    } else {
      log.debug("Removing item: {}", wfItem);
      return null;
    }
  }

  protected PSPubServer findPubServer(String editionId) {
    if (StringUtils.isBlank(editionId)) {
      return null;
    }
    try {
      var edition = pubService.loadEdition(guidMgr.makeGuid(editionId, PSTypeEnum.EDITION));
      var serverGuid = edition.getPubServerId();
      return pubServerService.findPubServer(serverGuid.longValue());
    } catch (Exception e) {
      log.info(
          "Error occurred while finding the status of ignore assets property, setting the value as"
              + " false",
          e);
      return null;
    }
  }

  /**
   * Determine if the item is scheduled to be published. This means it has a start date in the
   * future.
   */
  private boolean isScheduled(WorkflowItem wfItem) {
    var nowCal = Calendar.getInstance();
    var startDate = wfItem.itemSummary.getContentStartDate();
    if (startDate != null) {
      var startCal = Calendar.getInstance();
      startCal.setTime(startDate);
      return nowCal.before(startCal);
    }
    return false;
  }

  private boolean isPublishableAsset(
      IPSFilterItem assetFilterItem,
      WorkflowItem wfItem,
      boolean ignoreUnModAssets,
      Long serverId,
      Set<Integer> changedIdsSet) {
    try {
      // If Asset is in Recycle Folder, don't publish it.
      if (recyclerService.isInRecycler(assetFilterItem.getItemId())) {
        return false;
      }
      var rootLevelFolderAllowedSites =
          folderHelper.getRootLevelFolderAllowedSitesPropertyValue(
              assetFilterItem.getItemId().toString());
      if (rootLevelFolderAllowedSites != null && assetFilterItem.getSiteId() != null) {
        if (!rootLevelFolderAllowedSites.contains(
            String.valueOf(assetFilterItem.getSiteId().longValue()))) {
          return false;
        }
      } else if (ignoreUnModAssets
          && wfItem.publicRevision != null
          && wfItem.publicRevision > 0
          && serverId != null) {
        return changedIdsSet != null && changedIdsSet.contains(wfItem.itemSummary.getContentId());
      }
      return true;
    } catch (NullPointerException npe) {
      return true;
    }
  }

  @Override
  public int getPriority() {
    // We should be the only filter in the chain so this number should not matter.
    return 10;
  }

  @Override
  public void init(IPSExtensionDef def, File codeRoot) {
    super.init(def, codeRoot);
    // Wire dependencies
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  // Dependency injection setters/getters
  public IPSContentChangeService getContentChangeService() {
    return contentChangeService;
  }

  public void setContentChangeService(IPSContentChangeService contentChangeService) {
    this.contentChangeService = contentChangeService;
  }

  public IPSSiteManager getSiteMgr() {
    return siteMgr;
  }

  public void setSiteMgr(IPSSiteManager siteMgr) {
    this.siteMgr = siteMgr;
  }

  public IPSSitePublishService getSitePublishService() {
    return sitePublishService;
  }

  public void setSitePublishService(IPSSitePublishService sitePublishService) {
    this.sitePublishService = sitePublishService;
  }

  public IPSGuidManager getGuidMgr() {
    return guidMgr;
  }

  public void setGuidMgr(IPSGuidManager guidMgr) {
    this.guidMgr = guidMgr;
  }

  public IPSPubServerService getPubServerService() {
    return pubServerService;
  }

  public void setPubServerService(IPSPubServerService pubServerService) {
    this.pubServerService = pubServerService;
  }

  public void setFolderHelper(IPSFolderHelper folderHelper) {
    this.folderHelper = folderHelper;
  }

  public IPSFolderHelper getFolderHelper() {
    return folderHelper;
  }

  public IPSPublisherService getPubService() {
    return pubService;
  }

  public void setPubService(IPSPublisherService pubService) {
    this.pubService = pubService;
  }
}
