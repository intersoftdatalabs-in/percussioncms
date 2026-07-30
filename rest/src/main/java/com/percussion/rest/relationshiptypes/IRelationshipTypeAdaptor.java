/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest.relationshiptypes;

import java.util.List;

/** Adaptor for relationship type design catalog (SY-03 read). */
public interface IRelationshipTypeAdaptor {

  /**
   * List all relationship types with effects and properties.
   *
   * @return never null; may be empty
   */
  List<RelationshipType> listRelationshipTypes();

  /**
   * Resolve one relationship type by name or GUID string.
   *
   * @param idOrName name or type-host-uuid; blank/unsafe → null
   * @return type, or null when not found / unsafe key
   */
  RelationshipType findRelationshipType(String idOrName);
}
