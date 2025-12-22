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
import com.percussion.integritymanagement.data.PSIntegrityStatus.Status;
import com.percussion.share.service.exception.PSDataServiceException;
import java.util.List;

/** Service interface for integrity checker operations. */
public interface IPSIntegrityCheckerService {

  /**
   * Starts the integrity check process for the given task type.
   *
   * @param type the integrity task type
   * @return the token for the started process
   * @throws PSDataServiceException if not authorized or environment is unsupported
   */
  String start(IntegrityTaskType type) throws PSDataServiceException;

  /**
   * Stops the currently running integrity check.
   *
   * @throws PSDataServiceException if not authorized or environment is unsupported
   */
  void stop() throws PSDataServiceException;

  /**
   * Gets the status for the given token.
   *
   * @param token the token
   * @return the integrity status
   * @throws PSDataServiceException if not authorized or environment is unsupported
   */
  PSIntegrityStatus getStatus(String token) throws PSDataServiceException;

  /**
   * Gets the history of all integrity checks.
   *
   * @return list of integrity statuses
   * @throws PSDataServiceException if not authorized or environment is unsupported
   */
  List<PSIntegrityStatus> getHistory() throws PSDataServiceException;

  /**
   * Gets the history of integrity checks filtered by status.
   *
   * @param status the status to filter by
   * @return list of integrity statuses
   * @throws PSDataServiceException if not authorized or environment is unsupported
   */
  List<PSIntegrityStatus> getHistory(Status status) throws PSDataServiceException;

  /**
   * Deletes the integrity status for the given token.
   *
   * @param token the token
   * @throws PSDataServiceException if not authorized or environment is unsupported
   */
  void delete(String token) throws PSDataServiceException;

  /** Types of integrity tasks. */
  enum IntegrityTaskType {
    all,
    dts,
    cm1
  }
}
