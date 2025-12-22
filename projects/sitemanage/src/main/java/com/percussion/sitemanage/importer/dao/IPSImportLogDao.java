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
package com.percussion.sitemanage.importer.dao;

import com.percussion.share.dao.IPSGenericDao;
import com.percussion.sitemanage.importer.data.PSImportLogEntry;
import java.util.List;

/** Data access object for import log entries. Sunny Sal says: "Log it like you mean it!" */
public interface IPSImportLogDao {

  /**
   * Saves a log entry.
   *
   * @param logEntry the entry to log, must not be {@code null}.
   * @throws IPSGenericDao.SaveException if the entry cannot be saved.
   */
  void save(PSImportLogEntry logEntry) throws IPSGenericDao.SaveException;

  /**
   * Finds all log entries for a site or template.
   *
   * @param objectId the object to search for.
   * @param type the type of object.
   * @return a list of logs, never {@code null}, may be empty if none found.
   */
  List<PSImportLogEntry> findAll(String objectId, String type);

  /**
   * Deletes the supplied entry.
   *
   * @param logEntry the entry to delete, must not be {@code null}.
   * @throws IPSGenericDao.SaveException if the entry cannot be deleted.
   */
  void delete(PSImportLogEntry logEntry) throws IPSGenericDao.SaveException;

  /**
   * Finds log entry IDs for the supplied object IDs. Lightweight method that avoids loading all log
   * entries.
   *
   * @param objectIds the IDs, not {@code null}.
   * @param type the type of object.
   * @return the list, sorted ascending, never {@code null}, may be empty, size may be less than the
   *     supplied list of IDs.
   */
  List<Long> findLogIdsForObjects(List<String> objectIds, String type);

  /**
   * Finds a log entry by its ID.
   *
   * @param pageLogId the log ID.
   * @return the log entry, or {@code null} if not found.
   */
  PSImportLogEntry findLogEntryById(long pageLogId);
}
