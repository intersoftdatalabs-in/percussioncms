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
 * Service interface for managing Related_node entities. Provides CRUD operations for node
 * relationships in the taxonomy system.
 */
public interface Related_nodeServiceInf {

  /**
   * Retrieves all related node associations defined in the system.
   *
   * @return a collection of all Related_node entities, or an empty collection
   */
  public Collection<Related_node> getAllRelated_nodes();

  /**
   * Retrieves a specific related node by its unique identifier.
   *
   * @param id the unique identifier of the related node
   * @return the Related_node entity, or null if not found
   */
  public Related_node getRelated_node(int id);

  /**
   * Removes the specified related node from the system.
   *
   * @param related_node the Related_node entity to remove; must not be null
   */
  public void removeRelated_node(Related_node related_node);

  /**
   * Saves or updates the specified related node in the system.
   *
   * @param related_node the Related_node entity to save; must not be null
   */
  public void saveRelated_node(Related_node related_node);
}
