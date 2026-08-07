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
 * Service interface for managing Attribute entities. Provides CRUD operations and queries for
 * attributes within taxonomies.
 */
public interface AttributeServiceInf {

  /**
   * Retrieves all attributes for a specific taxonomy and language combination.
   *
   * @param taxonomy_id the unique identifier of the taxonomy
   * @param language_id the unique identifier of the language
   * @return a collection of Attribute entities matching the criteria, or an empty collection
   */
  public Collection<Attribute> getAllAttributes(int taxonomy_id, int language_id);

  /**
   * Retrieves a specific attribute by its unique identifier.
   *
   * @param id the unique identifier of the attribute
   * @return the Attribute entity with the given id, or null if not found
   */
  public Collection<Attribute> getAttribute(int id);

  /**
   * Removes the specified attribute from the system.
   *
   * @param attribute the Attribute entity to remove; must not be null
   */
  public void removeAttribute(Attribute attribute);

  /**
   * Saves or updates the specified attribute in the system.
   *
   * @param attribute the Attribute entity to save; must not be null
   */
  public void saveAttribute(Attribute attribute);

  /**
   * Retrieves all attribute names and their IDs for a specific taxonomy and language. Useful for
   * dropdown selections and list displays.
   *
   * @param taxonomy_id the unique identifier of the taxonomy
   * @param language_id the unique identifier of the language
   * @return a collection of Attribute names and IDs, or an empty collection
   */
  public Collection<Object[]> getAttributeNames(int taxonomy_id, int language_id);
}
