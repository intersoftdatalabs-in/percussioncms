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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Self-lockout detection for folder ACL edits (US4 / T062; FR-015).
 *
 * <p>The modern Content Explorer must warn the user before saving an
 * ACL edit that would revoke the current user's {@code ADMIN} or
 * {@code WRITE} access to the folder. The server is authoritative for
 * the actual permission; this helper is the client-side gate (UX) that
 * shows the warning and (host-side) requires an explicit
 * confirmation before the save reaches {@link saveFolderProperties}.</p>
 *
 * <p>Detection rule: if the principal list at a given access level
 * <em>before</em> the edit contained a principal matching the current
 * user, and the same-level list <em>after</em> the edit does not, the
 * user is being removed from that level — which may lock them out if
 * the level is the highest one they have. The same logic applies to
 * roles the user holds, but the principal hierarchy (USER vs ROLE)
 * does not matter for the warning — only the name match.</p>
 *
 * <p>This module is intentionally pure (no fetch, no DOM, no React) so
 * the unit tests can exercise the rules without rendering a component.</p>
 */

import type { PSFolderPermission, PSPrincipal } from "../api/contentExplorer/types";

export type AccessLevel = "ADMIN" | "WRITE" | "READ" | "VIEW";

/** Ordered from least to most restrictive; lower index = broader access. */
export const ACCESS_RANK: Record<AccessLevel, number> = {
  ADMIN: 0,
  WRITE: 1,
  READ: 2,
  VIEW: 3,
};

/**
 * The four principal-list keys of {@link PSFolderPermission} that this
 * helper considers for self-lockout detection, ordered ADMIN → VIEW
 * (least to most restrictive).
 */
export const PRINCIPAL_LIST_KEYS = [
  "adminPrincipals",
  "writePrincipals",
  "readPrincipals",
  "viewPrincipals",
] as const satisfies ReadonlyArray<keyof PSFolderPermission>;

export type PrincipalListKey = (typeof PRINCIPAL_LIST_KEYS)[number];

/** Single principal-list delta result used by {@link detectSelfLockout}. */
export interface SelfLockoutLevelDelta {
  /** Access level whose principal list lost the current user. */
  level: PrincipalListKey;
  /** Ranking of the level (lower = broader access). */
  rank: number;
}

/**
 * Detect whether the current user / role would be removed from any
 * access level by an ACL edit. The caller passes the {@link PSFolderPermission}
 * snapshot <em>before</em> and <em>after</em> the proposed edit and the
 * set of identities (user name + role names) that the current session
 * holds.
 *
 * @param before  Permission snapshot before the proposed ACL edit.
 * @param after   Permission snapshot after the proposed ACL edit.
 * @param identities  Identities the current user holds (USER name and any
 *   ROLE names). Each identity is matched against the principal `name`
 *   on a single {@link PSPrincipal} record.
 * @returns The list of levels the user is being removed from. Empty
 *   when no self-lockout risk is detected. The list is ordered ADMIN
 *   → VIEW (broadest first) so callers can render the highest-impact
 *   warning first.
 */
export function detectSelfLockout(
  before: PSFolderPermission,
  after: PSFolderPermission,
  identities: ReadonlyArray<string>,
): SelfLockoutLevelDelta[] {
  const identitySet = new Set(identities.filter((s) => s.length > 0));
  if (identitySet.size === 0) return [];

  const out: SelfLockoutLevelDelta[] = [];
  for (const key of PRINCIPAL_LIST_KEYS) {
    const beforeList = (before[key] ?? []) as PSPrincipal[];
    const afterList = (after[key] ?? []) as PSPrincipal[];
    if (principalListContainsAny(beforeList, identitySet)) {
      if (!principalListContainsAny(afterList, identitySet)) {
        out.push({ level: key, rank: ACCESS_RANK[levelKeyToAccess(key)] });
      }
    }
  }
  out.sort((a, b) => a.rank - b.rank);
  return out;
}

/**
 * Convenience predicate used by the folder-security panel: true when
 * {@link detectSelfLockout} returns at least one level. Reduces the
 * "should I show a warning?" check at the call-site to a single
 * boolean.
 */
export function wouldSelfLockout(
  before: PSFolderPermission,
  after: PSFolderPermission,
  identities: ReadonlyArray<string>,
): boolean {
  return detectSelfLockout(before, after, identities).length > 0;
}

function principalListContainsAny(
  list: ReadonlyArray<PSPrincipal>,
  identitySet: ReadonlySet<string>,
): boolean {
  for (const p of list) {
    if (p?.name && identitySet.has(p.name)) {
      return true;
    }
  }
  return false;
}

function levelKeyToAccess(key: PrincipalListKey): AccessLevel {
  // The principal-list key carries the access level in its prefix
  // (adminPrincipals → ADMIN, writePrincipals → WRITE, etc.).
  switch (key) {
    case "adminPrincipals":
      return "ADMIN";
    case "writePrincipals":
      return "WRITE";
    case "readPrincipals":
      return "READ";
    case "viewPrincipals":
      return "VIEW";
  }
}

/**
 * True when {@link PSFolderPermission.accessLevel} of {@link permission}
 * is at least {@code READ} (the user can view the security panel).
 *
 * <p>FR-016 (read-only without rights): when {@code accessLevel} is
 * {@code VIEW}, the security panel renders but every editable control
 * is disabled. When {@code accessLevel} is {@code READ}, the panel
 * renders read-only. This predicate is the gate at the call-site.</p>
 */
export function canViewSecurityPanel(
  permission: PSFolderPermission | undefined,
): boolean {
  if (!permission) return false;
  return ACCESS_RANK[permission.accessLevel] <= ACCESS_RANK.READ;
}

/**
 * True when {@link permission.accessLevel} is {@code ADMIN}. The
 * security panel's edit controls are enabled only when this returns
 * true.
 */
export function canEditSecurityPanel(
  permission: PSFolderPermission | undefined,
): boolean {
  return permission?.accessLevel === "ADMIN";
}
