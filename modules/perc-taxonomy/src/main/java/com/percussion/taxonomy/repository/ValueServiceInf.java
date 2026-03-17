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
import java.util.Map;

/**
 * Service interface for managing Value entities. Provides CRUD operations and batch operations for
 * attribute values associated with nodes.
 */
public interface ValueServiceInf {

  /**
   * Retrieves all values in the system.
   *
   * @return a collection of all Value entities, or an empty collection
   */
  public Collection<Value> getAllValues();

  /**
   * Retrieves a specific value by its unique identifier.
   *
   * @param id the unique identifier of the value
   * @return the Value entity, or null if not found
   */
  public Value getValue(int id);

  /**
   * Removes the specified value from the system.
   *
   * @param value the Value entity to remove; must not be null
   */
  public void removeValue(Value value);

  /**
   * Saves or updates the specified value in the system.
   *
   * @param value the Value entity to save; must not be null
   */
  public void saveValue(Value value);

  /**
   * Saves multiple values from HTTP request parameters. Processes the parameters map and
   * creates/updates values for the given node and attributes.
   *
   * @param params the HTTP request parameters containing value data
   * @param attributes the collection of attributes to process
   * @param node the node to associate values with
   * @param langID the language identifier
   * @param user_name the username performing the operation
   * @return a map of error messages keyed by parameter name, or an empty map if successful
   */
  public Map<String, String> saveValuesFromParams(
      Map<String, String[]> params,
      Collection<Attribute> attributes,
      Node node,
      int langID,
      String user_name);
}
