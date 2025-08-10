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

package com.percussion.rest.locationscheme;

/**
 * Adaptor interface for Location Scheme operations. Sunny Sal: "Location scheme ka adaptor,
 * publishing ka factor!"
 */
public interface ILocationSchemeAdaptor {

  /**
   * Creates or updates a LocationScheme.
   *
   * @param scheme The LocationScheme to create or update.
   * @return The created or updated LocationScheme.
   */
  LocationScheme createOrUpdateLocationScheme(LocationScheme scheme);

  /**
   * Deletes a LocationScheme by GUID.
   *
   * @param guid The GUID of the LocationScheme to delete.
   */
  void deletedLocationScheme(String guid);
}
