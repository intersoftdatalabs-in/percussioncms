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
 * Service interface for managing Language entities. Provides CRUD operations and queries for
 * supported languages in the taxonomy system.
 */
public interface LanguageServiceInf {

  /**
   * Retrieves all languages available in the system.
   *
   * @return a collection of all Language entities, or an empty collection if none exist
   */
  public Collection<Language> getAllLanguages();

  /**
   * Retrieves a specific language by its unique identifier.
   *
   * @param id the unique identifier of the language
   * @return the Language entity with the given id, or null if not found
   */
  public Language getLanguage(int id);

  /**
   * Removes the specified language from the system.
   *
   * @param language the Language entity to remove; must not be null
   */
  public void removeLanguage(Language language);

  /**
   * Saves or updates the specified language in the system.
   *
   * @param language the Language entity to save; must not be null
   */
  public void saveLanguage(Language language);
}
