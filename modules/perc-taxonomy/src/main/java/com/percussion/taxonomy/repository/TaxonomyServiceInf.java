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
import java.util.List;

/**
 * Service interface for managing Taxonomy entities.
 * Provides CRUD operations and queries for taxonomies including their associated nodes, attributes, and visibility settings.
 */
public interface TaxonomyServiceInf {

  /**
   * Retrieves all taxonomies defined in the system.
   *
   * @return a collection of all Taxonomy entities, or an empty collection
   */
  Collection<Taxonomy> getAllTaxonomys();

  /**
   * Retrieves a specific taxonomy by its unique identifier.
   *
   * @param id the unique identifier of the taxonomy
   * @return the Taxonomy entity, or null if not found
   */
  Taxonomy getTaxonomy(int id);

  /**
   * Determines if a taxonomy exists for a given name.
   * Comparison is case-insensitive.
   *
   * @param name the name to search for; must not be empty
   * @return true if a taxonomy with the given name exists, false otherwise
   */
  boolean doesTaxonomyExists(String name);

  /**
   * Removes a taxonomy and all associated entities.
   * This will delete all attributes, nodes, and visibilities associated with the taxonomy.
   *
   * @param taxonomy the Taxonomy entity to remove; must not be null
   */
  void removeTaxonomy(Taxonomy taxonomy);

  /**
   * Saves or updates the specified taxonomy in the system.
   *
   * @param taxonomy the Taxonomy entity to save; must not be null
   */
  void saveTaxonomy(Taxonomy taxonomy);

  /**
   * Retrieves nodes in an order suitable for deletion (children before parents).
   * This ensures referential integrity when deleting nodes in bulk.
   *
   * @param taxonomy the Taxonomy whose nodes are to be ordered
   * @return a list of Node entities in deletion order
   */
  List<Node> getNodesInDeletionOrder(Taxonomy taxonomy);
}
