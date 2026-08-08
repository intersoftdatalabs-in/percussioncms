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

import com.percussion.taxonomy.domain.*;
import java.util.Collection;

/**
 * Service interface for managing Relationship_type entities. Provides CRUD operations for defining
 * types of relationships between nodes.
 */
public interface Relationship_typeServiceInf {

  /**
   * Retrieves all relationship types defined in the system.
   *
   * @return a collection of all Relationship_type entities, or an empty collection
   */
  public Collection<Relationship_type> getAllRelationship_types();

  /**
   * Retrieves a specific relationship type by its unique identifier.
   *
   * @param id the unique identifier of the relationship type
   * @return the Relationship_type entity, or null if not found
   */
  public Relationship_type getRelationship_type(int id);

  /**
   * Removes the specified relationship type from the system.
   *
   * @param relationship_type the Relationship_type entity to remove; must not be null
   */
  public void removeRelationship_type(Relationship_type relationship_type);

  /**
   * Saves or updates the specified relationship type in the system.
   *
   * @param relationship_type the Relationship_type entity to save; must not be null
   */
  public void saveRelationship_type(Relationship_type relationship_type);
}
