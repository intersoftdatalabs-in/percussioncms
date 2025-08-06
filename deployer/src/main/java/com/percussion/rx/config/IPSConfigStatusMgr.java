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
package com.percussion.rx.config;

import com.percussion.rx.config.data.PSConfigStatus;
import com.percussion.services.error.PSNotFoundException;

import java.util.List;

/**
 * Manages CRUD and catalog operations for config status objects.
 */
public interface IPSConfigStatusMgr {

  /**
   * Creates the config status object with default values for the given config name.
   *
   * @param configName name of the configuration, must not be blank.
   * @return the config status object, never {@code null}.
   */
  PSConfigStatus createConfigStatus(String configName);

  /**
   * Saves or updates the given config status object.
   *
   * @param obj must not be {@code null}.
   */
  void saveConfigStatus(PSConfigStatus obj);

  /**
   * Loads config status object of the given status id.
   *
   * @param statusID the status id.
   * @return the config status object, never {@code null}.
   * @throws PSNotFoundException if not found.
   */
  PSConfigStatus loadConfigStatus(long statusID) throws PSNotFoundException;

  /**
   * Loads the modifiable config status object.
   *
   * @param statusID the status id.
   * @return the config status object, never {@code null}.
   * @throws PSNotFoundException if not found.
   */
  PSConfigStatus loadConfigStatusModifiable(long statusID) throws PSNotFoundException;

  /**
   * Finds objects whose name matches the supplied filter (case-insensitive).
   *
   * @param nameFilter pattern identifying objects to return. SQL-like wildcards (%) may be used. Never {@code null} or empty.
   * @return all matching objects, sorted by name (asc) then install date (desc). Never {@code null}, may be empty.
   */
  List<PSConfigStatus> findConfigStatus(String nameFilter);

  /**
   * Finds the latest config status objects whose name matches the supplied filter.
   *
   * @param nameFilter pattern identifying objects to return. SQL-like wildcards (%) may be used. Never {@code null} or empty.
   * @return latest of each set of objects, sorted by name (asc) then date applied (desc). Never {@code null}, may be empty.
   */
  List<PSConfigStatus> findLatestConfigStatus(String nameFilter);

  /**
   * Deletes the config status entry with the given status id.
   *
   * @param statusID the status id to delete.
   * @throws PSNotFoundException if not found.
   */
  void deleteConfigStatus(long statusID) throws PSNotFoundException;

  /**
   * Deletes all status entries that match the given name filter.
   *
   * @param nameFilter must not be {@code null}. SQL-like wildcards (%) may be used. Never {@code null} or empty.
   */
  void deleteConfigStatus(String nameFilter);

  /**
   * Finds the last successful config status object whose name matches the supplied name.
   *
   * @param configName the name of the configuration.
   * @return the last successful configuration, or {@code null} if not found.
   */
  PSConfigStatus findLastSuccessfulConfigStatus(String configName);
}
