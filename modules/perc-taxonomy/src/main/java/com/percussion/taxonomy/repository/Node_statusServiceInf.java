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
 * Service interface for managing Node_status entities. Provides CRUD operations for node status
 * information in the taxonomy system.
 */
public interface Node_statusServiceInf {

  /**
   * Retrieves all node statuses defined in the system.
   *
   * @return a collection of all Node_status entities, or an empty collection
   */
  public Collection<Node_status> getAllNode_statuss();

  /**
   * Retrieves a specific node status by its unique identifier.
   *
   * @param id the unique identifier of the node status
   * @return the Node_status entity, or null if not found
   */
  public Node_status getNode_status(int id);

  /**
   * Removes the specified node status from the system.
   *
   * @param node_status the Node_status entity to remove; must not be null
   */
  public void removeNode_status(Node_status node_status);

  /**
   * Saves or updates the specified node status in the system.
   *
   * @param node_status the Node_status entity to save; must not be null
   */
  public void saveNode_status(Node_status node_status);
}
