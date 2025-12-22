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

// REFACTORED: CP-JAVA11
package com.percussion.integritymanagement.service;

import com.percussion.integritymanagement.data.PSIntegrityStatus;
import com.percussion.share.dao.IPSGenericDao;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/** DAO interface for integrity checker persistence operations. */
public interface IPSIntegrityCheckerDao {

  /**
   * Finds an integrity status by token.
   *
   * @param token the unique token
   * @return the integrity status, or null if not found
   */
  @Transactional
  PSIntegrityStatus find(String token);

  /**
   * Finds all integrity statuses by status.
   *
   * @param status the status to filter by
   * @return list of matching integrity statuses
   */
  @Transactional
  List<PSIntegrityStatus> find(PSIntegrityStatus.Status status);

  /**
   * Deletes the given integrity status.
   *
   * @param intStatus the status to delete
   */
  @Transactional
  void delete(PSIntegrityStatus intStatus);

  /**
   * Saves or updates the given integrity status.
   *
   * @param status the status to save
   * @throws IPSGenericDao.SaveException if persistence fails
   */
  @Transactional
  void save(PSIntegrityStatus status) throws IPSGenericDao.SaveException;
}
