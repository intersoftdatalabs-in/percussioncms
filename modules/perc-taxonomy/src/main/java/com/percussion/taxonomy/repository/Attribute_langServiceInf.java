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
 * Service interface for managing Attribute_lang entities. Provides CRUD operations for localized
 * attribute metadata in the taxonomy system.
 */
public interface Attribute_langServiceInf {

  /**
   * Retrieves all attribute language localizations defined in the system.
   *
   * @return a collection of all Attribute_lang entities, or an empty collection
   */
  public Collection<Attribute_lang> getAllAttribute_langs();

  /**
   * Retrieves a specific attribute language by its unique identifier.
   *
   * @param id the unique identifier of the attribute language
   * @return the Attribute_lang entity, or null if not found
   */
  public Attribute_lang getAttribute_lang(int id);

  /**
   * Removes the specified attribute language from the system.
   *
   * @param attribute_lang the Attribute_lang entity to remove; must not be null
   */
  public void removeAttribute_lang(Attribute_lang attribute_lang);

  /**
   * Saves or updates the specified attribute language in the system.
   *
   * @param attribute_lang the Attribute_lang entity to save; must not be null
   */
  public void saveAttribute_lang(Attribute_lang attribute_lang);
}
