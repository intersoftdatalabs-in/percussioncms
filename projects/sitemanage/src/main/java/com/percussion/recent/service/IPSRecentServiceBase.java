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

package com.percussion.recent.service;

import com.percussion.recent.data.PSRecent.RecentType;
import java.util.List;

/** Service interface for managing recent items in the base service. */
public interface IPSRecentServiceBase {

  /** Finds recent values for the given user, site, and type. */
  List<String> findRecent(String user, String siteName, RecentType type);

  /** Adds a recent value for the given user, site, and type. */
  void addRecent(String user, String siteName, RecentType type, String value);

  /** Deletes all recent values for the given user, site, and type. */
  void deleteRecent(String user, String siteName, RecentType type);

  /** Deletes specific recent values for the given user, site, and type. */
  void deleteRecent(String user, String siteName, RecentType type, List<String> toDelete);

  /** Renames all recent items for the given site. */
  void renameSiteRecent(String oldSiteName, String newSiteName);
}
