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
package com.percussion.itemmanagement.community;

import java.util.Collection;

/**
 * Decides whether Explorer may assign a community to one page or asset (#5077).
 *
 * <p>The requested id must be a positive integer, different from the item's current community, and
 * present in the catalog the server will accept. Anything else is a failure — callers must not
 * report success.
 */
public final class ItemCommunityAssignmentRules {

  public enum Reason {
    OK,
    BLANK,
    MALFORMED,
    UNCHANGED,
    FORBIDDEN
  }

  private ItemCommunityAssignmentRules() {}

  /**
   * @param requested raw community id from the client
   * @param currentCommunityId the item's current community id ({@code -1} when none)
   * @param allowedCommunityIds communities the caller may assign; {@code null} allows nothing
   */
  public static Reason decide(
      String requested, int currentCommunityId, Collection<Integer> allowedCommunityIds) {
    if (requested == null || requested.isBlank()) {
      return Reason.BLANK;
    }
    final int id;
    try {
      id = Integer.parseInt(requested.trim());
    } catch (NumberFormatException ex) {
      return Reason.MALFORMED;
    }
    if (id <= 0) {
      return Reason.MALFORMED;
    }
    if (id == currentCommunityId) {
      return Reason.UNCHANGED;
    }
    if (allowedCommunityIds == null || !allowedCommunityIds.contains(id)) {
      return Reason.FORBIDDEN;
    }
    return Reason.OK;
  }
}
