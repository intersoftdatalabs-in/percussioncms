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

package com.percussion.taxonomy.service;

import com.percussion.taxonomy.domain.Attribute;
import com.percussion.taxonomy.domain.Node;
import com.percussion.taxonomy.domain.Value;
import com.percussion.taxonomy.repository.ValueDAO;
import com.percussion.taxonomy.repository.ValueServiceInf;
import java.util.Collection;
import java.util.Map;
import org.hibernate.HibernateException;

/**
 * Service implementation for managing Value entities. Provides CRUD operations and parameter-based
 * value saving for taxonomy values.
 *
 * @author rxengineer
 */
public class ValueService implements ValueServiceInf {

  ///////////////////////////////////////////////////////////////////////////////

  public ValueDAO valueDAO;

  ///////////////////////////////////////////////////////////////////////////////

  /**
   * Retrieves all values in the system.
   *
   * @return a collection of all Value entities, or an empty collection if none exist
   */
  public Collection<Value> getAllValues() {
    Collection<Value> values = null;
    try {
      values = this.valueDAO.getAllValues();
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return values;
  }

  /**
   * Retrieves a specific value by its unique identifier.
   *
   * @param id the unique identifier of the value
   * @return the Value entity with the given id, or null if not found
   */
  public Value getValue(int id) {
    Value value = null;
    try {
      value = this.valueDAO.getValue(id);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return value;
  }

  ///////////////////////////////////////////////////////////////////////////////

  /**
   * Removes the specified value from the system.
   *
   * @param value the Value entity to remove; must not be null
   */
  public void removeValue(Value value) {
    try {
      this.valueDAO.removeValue(value);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
  }

  /**
   * Saves or updates the specified value in the system.
   *
   * @param value the Value entity to save; must not be null
   */
  public void saveValue(Value value) {
    try {
      this.valueDAO.saveValue(value);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  /**
   * Saves values from HTTP request parameters for a given node.
   *
   * @param params the map of HTTP request parameters
   * @param attributes the collection of attributes to save values for
   * @param node the node to associate values with
   * @param langID the language identifier
   * @param user_name the username performing the save
   * @return a map of results (e.g., success/error messages)
   */
  @Override
  public Map<String, String> saveValuesFromParams(
      Map<String, String[]> params,
      Collection<Attribute> attributes,
      Node node,
      int langID,
      String user_name) {
    Map<String, String> results = null;
    try {
      results = this.valueDAO.saveValuesFromParams(params, attributes, node, langID, user_name);
    } catch (HibernateException e) {
      throw new HibernateException(e);
    }
    return results;
  }

  ///////////////////////////////////////////////////////////////////////////////

  /**
   * Sets the ValueDAO instance for this service.
   *
   * @param valueDAO the ValueDAO to use for database operations
   */
  public void setValueDAO(ValueDAO valueDAO) {
    this.valueDAO = valueDAO;
  }

  ///////////////////////////////////////////////////////////////////////////////

}
