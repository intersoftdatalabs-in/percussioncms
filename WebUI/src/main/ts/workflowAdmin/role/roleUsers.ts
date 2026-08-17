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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Dual-list membership for Admin → Roles (#3504).
 *
 * <p>The editor must not re-POST {@code /role/availableUsers} after every Add.
 * That envelope bind/unwrap path cleared the available list after the first
 * member. Filter GET-all-users minus assigned locally, matching the legacy
 * create-role path in {@code PercRoleController.getAvailableUsers}.</p>
 */

/**
 * Active users that are not already assigned to the role being edited.
 * Comparison is exact (CMS user names are case-sensitive).
 */
export function availableUsersMinusAssigned(
  allUsers: readonly string[],
  assignedUsers: readonly string[],
): string[] {
  const assigned = new Set(assignedUsers);
  return allUsers.filter((user) => user.length > 0 && !assigned.has(user));
}
