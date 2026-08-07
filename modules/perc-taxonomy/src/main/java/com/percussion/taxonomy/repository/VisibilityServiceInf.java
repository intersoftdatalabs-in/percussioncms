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

package com.percussion.taxonomy.repository;

import com.percussion.taxonomy.domain.Visibility;
import java.util.Collection;

/**
 * Service interface for managing Visibility entities. Provides CRUD operations for controlling node
 * visibility in different contexts.
 */
public interface VisibilityServiceInf {

  /**
   * Retrieves all visibility options defined in the system.
   *
   * @return a collection of all Visibility entities, or an empty collection
   */
  public Collection<Visibility> getAllVisibilities();

  /**
   * Retrieves all visibility options for a specific taxonomy.
   *
   * @param taxonomy_id the unique identifier of the taxonomy
   * @return a collection of Visibility entities associated with the taxonomy
   */
  public Collection<Visibility> getAllVisibilitiesForTaxonomyId(int taxonomy_id);

  /**
   * Retrieves a specific visibility by its unique identifier.
   *
   * @param id the unique identifier of the visibility
   * @return the Visibility entity, or null if not found
   */
  public Visibility getVisibility(int id);

  /**
   * Removes the specified visibility from the system.
   *
   * @param visibility the Visibility entity to remove; must not be null
   */
  public void removeVisibility(Visibility visibility);

  /**
   * Removes multiple visibility entities from the system.
   *
   * @param visibilities the collection of Visibility entities to remove
   */
  public void removeVisibilities(Collection<Visibility> visibilities);

  /**
   * Saves or updates the specified visibility in the system.
   *
   * @param visibility the Visibility entity to save; must not be null
   */
  public void saveVisibility(Visibility visibility);
}
