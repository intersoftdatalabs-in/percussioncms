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
package com.percussion.recycle.service.impl;

import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSDeleteFolderCriteria.SkipItemsType;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.pathmanagement.service.IPSPathService.PSPathServiceException;
import com.percussion.recycle.data.PSEmptyRecycleResult;
import com.percussion.recycle.service.IPSEmptyRecycleService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.service.IPSUserService;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Empties the system Recycling bin by permanently purging only content under the Recycling root.
 *
 * <p>Safety: operates solely on finder path {@link #RECYCLING_FINDER_ROOT} which maps to {@link
 * PSRecycleService#RECYCLING_ROOT}. Live Sites/Assets outside Recycling are never targeted.
 */
@Component("emptyRecycleService")
@Lazy
public class PSEmptyRecycleService implements IPSEmptyRecycleService {

  /** Finder path for the Recycling root (must end with {@code /} for path matching). */
  public static final String RECYCLING_FINDER_ROOT = "/Recycling/";

  private final IPSPathService pathService;
  private final IPSUserService userService;
  private final IPSFolderHelper folderHelper;

  @Autowired
  public PSEmptyRecycleService(
      @Qualifier("pathService") IPSPathService pathService,
      IPSUserService userService,
      @Lazy IPSFolderHelper folderHelper) {
    this.pathService = pathService;
    this.userService = userService;
    this.folderHelper = folderHelper;
  }

  @Override
  public PSEmptyRecycleResult emptyRecyclingBin()
      throws PSDataServiceException, PSEmptyRecycleException, PSEmptyRecycleNotAuthorizedException {
    requireAdmin();

    PSEmptyRecycleResult result = new PSEmptyRecycleResult();
    List<PSPathItem> children;
    try {
      children = pathService.findChildren(RECYCLING_FINDER_ROOT);
    } catch (PSPathServiceException e) {
      throw new PSEmptyRecycleException(
          "Failed to list Recycling bin children: " + e.getMessage(), e);
    } catch (PSDataServiceException e) {
      throw e;
    }

    if (children == null || children.isEmpty()) {
      result.setAlreadyEmpty(true);
      log.info("Empty Recycling bin: already empty (idempotent no-op)");
      return result;
    }

    for (PSPathItem child : children) {
      if (child == null || StringUtils.isBlank(child.getPath())) {
        continue;
      }
      // Hard safety: never process paths outside the Recycling finder root
      String childPath = normalizeFinderPath(child.getPath());
      if (!isUnderRecyclingRoot(childPath)) {
        String msg =
            "Refusing to purge path outside Recycling root: " + sanitizePathForLog(childPath);
        log.error(msg);
        result.addError(msg);
        result.addUndeleted(1);
        continue;
      }

      try {
        if (child.isFolder() || !child.isLeaf()) {
          purgeFolder(childPath, child.getId(), result);
        } else {
          purgeLeaf(child, result);
        }
      } catch (Exception e) {
        String msg =
            "Failed to purge recycled path "
                + sanitizePathForLog(childPath)
                + ": "
                + PSExceptionUtils.getMessageForLog(e);
        log.warn(msg, e);
        result.addError(msg);
        result.addUndeleted(1);
      }
    }

    log.info(
        "Empty Recycling bin complete: folders={}, items={}, undeleted={}, errors={}",
        result.getPurgedFolderCount(),
        result.getPurgedItemCount(),
        result.getUndeletedCount(),
        result.getErrors().size());
    return result;
  }

  private void requireAdmin() throws PSDataServiceException, PSEmptyRecycleNotAuthorizedException {
    var current = userService.getCurrentUser();
    String name = current != null ? current.getName() : null;
    if (StringUtils.isBlank(name) || !userService.isAdminUser(name)) {
      throw new PSEmptyRecycleNotAuthorizedException(
          "Only Admin users may empty the Recycling bin.");
    }
  }

  private void purgeFolder(String finderPath, String guid, PSEmptyRecycleResult result)
      throws Exception {
    PSDeleteFolderCriteria criteria = new PSDeleteFolderCriteria();
    criteria.setPath(finderPath);
    criteria.setShouldPurge(true);
    // Skip failures for individual in-use/unauthorized leaves so the bulk empty can continue.
    criteria.setSkipItems(SkipItemsType.YES);
    if (StringUtils.isNotBlank(guid)) {
      criteria.setGuid(guid);
    }
    int undeleted = pathService.deleteFolder(criteria);
    result.incrementPurgedFolders();
    result.addUndeleted(undeleted);
  }

  private void purgeLeaf(PSPathItem child, PSEmptyRecycleResult result) throws Exception {
    // Permanent purge of a leaf under the system Recycling folder path only.
    folderHelper.removeItem(PSRecycleService.RECYCLING_ROOT, child.getId(), true);
    result.incrementPurgedItems();
  }

  /**
   * Ensures finder path form used by {@link IPSPathService} (leading and trailing slash for
   * folders).
   */
  static String normalizeFinderPath(String path) {
    String p = StringUtils.trimToEmpty(path);
    if (!p.startsWith("/")) {
      p = "/" + p;
    }
    if (!p.endsWith("/")) {
      p = p + "/";
    }
    // Collapse accidental double-leading slashes
    while (p.startsWith("//")) {
      p = p.substring(1);
    }
    return p;
  }

  /**
   * Returns true only for the Recycling finder root or its descendants. Rejects {@code /Sites},
   * {@code /Assets}, and any non-Recycling path.
   */
  static boolean isUnderRecyclingRoot(String normalizedFinderPath) {
    if (StringUtils.isBlank(normalizedFinderPath)) {
      return false;
    }
    String p = normalizedFinderPath;
    if (!p.startsWith("/")) {
      p = "/" + p;
    }
    // Exact root or prefix under /Recycling/
    return p.equalsIgnoreCase(RECYCLING_FINDER_ROOT)
        || StringUtils.startsWithIgnoreCase(p, RECYCLING_FINDER_ROOT);
  }

  /** Avoid logging full system paths with user-controlled segments in bulk. */
  static String sanitizePathForLog(String path) {
    if (path == null) {
      return "";
    }
    // Keep path structure; strip control characters only
    return path.replaceAll("[\\p{Cntrl}]", "");
  }

  private static final Logger log = LogManager.getLogger(PSEmptyRecycleService.class);
}
