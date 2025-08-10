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

package com.percussion.recent.service.impl;

import com.percussion.recent.dao.IPSRecentDao;
import com.percussion.recent.dao.impl.PSRecentDao;
import com.percussion.recent.data.PSRecent;
import com.percussion.recent.data.PSRecent.RecentType;
import com.percussion.recent.service.IPSRecentServiceBase;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A CM1 independent implementation of the recent service. This class controls the number of entries
 * stored, but does not clean up or validate the values that are stored in the table.
 *
 * @author stephenbolton
 */
@Transactional(propagation = Propagation.REQUIRED)
@Component("recentServiceBase")
@Lazy
public class PSRecentServiceBase implements IPSRecentServiceBase {

  @Autowired private IPSRecentDao recentDao;

  public PSRecentServiceBase(IPSRecentDao recentDao) {
    this.recentDao = recentDao;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> findRecent(String user, String siteName, RecentType type) {
    var returnRecents = recentDao.find(user, siteName, type);
    var returnValues = new ArrayList<String>();
    if (returnRecents != null) {
      for (var recent : returnRecents) {
        returnValues.add(recent.getValue());
      }
    }
    return returnValues;
  }

  /** {@inheritDoc} */
  @Override
  public void addRecent(String user, String siteName, RecentType type, String value) {
    var existingRecents = recentDao.find(user, siteName, type);

    // If the most recent is the same as value do not have to update anything.
    if (!existingRecents.isEmpty() && existingRecents.get(0).getValue().equals(value)) {
      return;
    }

    existingRecents = deleteExtraRecents(type, existingRecents, value, true);

    // Add new item with index 0.
    var newRecent = new PSRecent(user, siteName, type, 0, value);
    var updatedRecents = new ArrayList<PSRecent>();
    updatedRecents.add(newRecent);

    // Update index of existing values.
    for (int i = 0; i < existingRecents.size(); i++) {
      var existing = existingRecents.get(i);
      existing.setOrder(i + 1);
      updatedRecents.add(existing);
    }
    recentDao.saveAll(updatedRecents);
  }

  /** {@inheritDoc} */
  @Override
  public void deleteRecent(String user, String siteName, RecentType type) {
    var existingRecents = recentDao.find(user, siteName, type);
    if (CollectionUtils.isNotEmpty(existingRecents)) {
      recentDao.deleteAll(existingRecents);
    }
  }

  /**
   * Removes extra items from the list of recent items to maintain the correct number. Cleans up if
   * the maximum values for the recent type change.
   *
   * @param type the type to get maximum size
   * @param recents list of recents
   * @param value current value to check if it is already in list
   * @param forAdd true if adding an item to the list, so need one less item
   * @return cleaned up list of recent items
   */
  private List<PSRecent> deleteExtraRecents(
      RecentType type, List<PSRecent> recents, String value, boolean forAdd) {
    int numOfElementsToKeep = forAdd ? type.maxSize() - 1 : type.maxSize();
    var toDelete = new ArrayList<PSRecent>();
    // Remove other entries of value
    var it = recents.iterator();
    if (value != null) {
      while (it.hasNext()) {
        var del = it.next();
        if (del.getValue().equals(value)) {
          toDelete.add(del);
          it.remove();
        }
      }
    }
    if (recents.size() > numOfElementsToKeep) {
      toDelete.addAll(recents.subList(numOfElementsToKeep, recents.size()));
      recents = recents.subList(0, numOfElementsToKeep);
    }
    if (!toDelete.isEmpty()) {
      recentDao.deleteAll(toDelete);
    }
    return recents;
  }

  /** {@inheritDoc} */
  @Override
  public void deleteRecent(String user, String siteName, RecentType type, List<String> toDelete) {
    var existingRecents = recentDao.find(user, siteName, type);
    var iterator = existingRecents.iterator();
    while (iterator.hasNext()) {
      if (!toDelete.contains(iterator.next().getValue())) {
        iterator.remove();
      }
    }
    recentDao.deleteAll(existingRecents);
  }

  /** {@inheritDoc} */
  @Override
  public void renameSiteRecent(String oldSiteName, String newSiteName) {
    var siteRecents = recentDao.find(null, oldSiteName, null);
    for (var recent : siteRecents) {
      if (recent.getSiteName().equals(oldSiteName)) {
        recent.setSiteName(newSiteName);
        if (recent.getValue().contains(oldSiteName)) {
          recent.setValue(recent.getValue().replaceAll(oldSiteName, newSiteName));
        }
      }
    }
    recentDao.saveAll(siteRecents);
  }

  public IPSRecentDao getRecentDao() {
    return recentDao;
  }

  public void setRecentDao(PSRecentDao recentDao) {
    this.recentDao = recentDao;
  }
}
