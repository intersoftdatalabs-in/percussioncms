/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

/**
 * Adaptor for relationship type design catalog and Admin user-type write (SY-03).
 *
 * <p>Write methods persist <strong>user</strong> relationship types through {@code
 * IPSSystemDesignWs}. System relationship types are immutable.
 */
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

  /**
   * Admin. Create a user relationship type, optionally copying mutable fields from an existing
   * type ({@code copyFrom}).
   *
   * @param body required; {@code name} required; {@code category} or {@code copyFrom} required
   * @return the persisted relationship type
   */
  RelationshipType createRelationshipType(RelationshipType body);

  /**
   * Admin. Update mutable fields of a user relationship type. Does not mutate system types.
   *
   * @param idOrName name or GUID string (same rules as {@link #findRelationshipType})
   * @param body required writable fields
   * @return the persisted type, or {@code null} when missing/unsafe
   */
  RelationshipType updateRelationshipType(String idOrName, RelationshipType body);

  /**
   * Admin. Delete a user relationship type. Does not mutate system types.
   *
   * @param idOrName name or GUID string (same rules as {@link #findRelationshipType})
   * @return {@code true} when deleted, {@code false} when missing/unsafe
   */
  boolean deleteRelationshipType(String idOrName);
}
