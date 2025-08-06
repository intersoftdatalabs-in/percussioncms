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

package com.percussion.recent.dao;

import com.percussion.recent.data.PSRecent;
import com.percussion.share.dao.IPSGenericDao;

import java.util.List;

/**
 * DAO interface for managing recent items.
 */
public interface IPSRecentDao {

    /**
     * Finds recent items for the given user, site, and type.
     *
     * @param user the user name, may be null
     * @param siteName the site name, may be null
     * @param type the recent type, may be null
     * @return list of recent items, never null
     */
    List<PSRecent> find(String user, String siteName, PSRecent.RecentType type);

    /**
     * Saves all recent items in the list.
     *
     * @param recentList the list of recent items to save, not null
     */
    void saveAll(List<PSRecent> recentList);

    /**
     * Deletes the given recent item.
     *
     * @param recent the recent item to delete, not null
     */
    void delete(PSRecent recent);

    /**
     * Deletes all recent items in the list.
     *
     * @param recentList the list of recent items to delete, not null
     */
    void deleteAll(List<PSRecent> recentList);

    /**
     * Saves the given recent item.
     *
     * @param recent the recent item to save, not null
     * @throws IPSGenericDao.SaveException if the save fails
     */
    void save(PSRecent recent) throws IPSGenericDao.SaveException;
}
