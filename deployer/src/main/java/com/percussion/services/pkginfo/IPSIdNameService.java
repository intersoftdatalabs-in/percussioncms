// REFACTORED: CP-JAVA11
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

package com.percussion.services.pkginfo;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.pkginfo.data.PSIdName;
import com.percussion.utils.guid.IPSGuid;

/**
 * Service for saving, loading, and deleting dependency element id-name mappings.
 * <p>
 * This interface provides methods to manage mappings between dependency names and GUIDs.
 * All methods are thread-safe and must not return null unless explicitly documented.
 */
public interface IPSIdNameService {

  /**
   * Deletes all id-name mappings. Intended for unit testing only.
   */
  void deleteAll();

  /**
   * Saves the supplied id-name mapping to the repository.
   *
   * @param mapping The mapping to save; must not be {@code null}.
   */
  void saveIdName(PSIdName mapping);

  /**
   * Gets an id for the given name (case-insensitive) and type.
   *
   * @param name The dependency name; must not be {@code null} or empty.
   * @param type The dependency system type; must not be {@code null}.
   * @return An {@code IPSGuid} object, or {@code null} if not found.
   */
  IPSGuid findId(String name, PSTypeEnum type);

  /**
   * Gets a name for the given {@code IPSGuid}.
   *
   * @param guid The guid; must not be {@code null}.
   * @return The name of the dependency element corresponding to the guid,
   *     or {@code null} if not found.
   */
  String findName(IPSGuid guid);
}
