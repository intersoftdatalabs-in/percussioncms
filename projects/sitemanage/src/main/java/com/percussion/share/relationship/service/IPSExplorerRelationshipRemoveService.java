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
package com.percussion.share.relationship.service;

/**
 * Lists, adds, and deletes one non-folder relationship owned by a selected Explorer item.
 *
 * <p>Sibling of {@link IPSRelationshipSummaryService}, which stays read-only.
 */
public interface IPSExplorerRelationshipRemoveService {

  ExplorerRelationshipAction listOwned(String itemId);

  ExplorerRelationshipAction removeOwned(String itemId, int relationshipId);

  /**
   * Creates one non-folder relationship owned by {@code itemId} and pointing at {@code
   * targetItemId}. Folder types are refused. Does not report success on a bad request, a missing
   * item, a permission failure, or a save conflict.
   */
  ExplorerRelationshipAction addOwned(String itemId, String targetItemId, String configName);
}
