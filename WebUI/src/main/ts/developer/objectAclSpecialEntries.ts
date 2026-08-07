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

import type { ObjectAclEntry } from "../api/developer/types";

/**
 * Canonical special ACL principal names from {@code PSTypedPrincipal}:
 * <ul>
 *   <li>{@code Default} — system USER entry used when no user/group/role matches</li>
 *   <li>{@code AnyCommunity} — system COMMUNITY entry used when no community matches</li>
 * </ul>
 * Workbench keeps these non-deletable on object ACL dialogs (CD-19 / FR §5.4).
 */
export const SPECIAL_ACL_DEFAULT_NAME = "Default";
export const SPECIAL_ACL_ANY_COMMUNITY_NAME = "AnyCommunity";

export type SpecialAclKind = "default" | "any-community";

/** PrincipalTypes used when creating special entries (server contract). */
export type SpecialAclPrincipalType = "USER" | "COMMUNITY";

/** Resolve a display/compare principal name from an ACL entry. */
export function entryPrincipalName(e: ObjectAclEntry): string {
  return (e.name || e.principal?.name || e.type?.name || "").trim();
}

/** Resolve PrincipalTypes enum string (upper-case). */
export function entryPrincipalType(e: ObjectAclEntry): string {
  return (e.type?.type || e.principal?.type || "").toUpperCase();
}

/**
 * Classify Default / AnyCommunity specials by canonical principal name.
 * Name match is authoritative (server always uses these exact names); type is
 * not required so mis-typed historical payloads still count as specials.
 */
export function specialAclKind(e: ObjectAclEntry): SpecialAclKind | null {
  const name = entryPrincipalName(e);
  if (name === SPECIAL_ACL_DEFAULT_NAME) return "default";
  if (name === SPECIAL_ACL_ANY_COMMUNITY_NAME) return "any-community";
  return null;
}

export function isProtectedSpecialAclEntry(e: ObjectAclEntry): boolean {
  return specialAclKind(e) != null;
}

/** Workbench: cannot delete Default / AnyCommunity. */
export function canRemoveAclEntry(e: ObjectAclEntry): boolean {
  return !isProtectedSpecialAclEntry(e);
}

export function specialAclPrincipalType(kind: SpecialAclKind): SpecialAclPrincipalType {
  return kind === "default" ? "USER" : "COMMUNITY";
}

export function specialAclPrincipalName(kind: SpecialAclKind): string {
  return kind === "default"
    ? SPECIAL_ACL_DEFAULT_NAME
    : SPECIAL_ACL_ANY_COMMUNITY_NAME;
}

/**
 * Map a free-text principal name to a special kind when it matches the
 * canonical special names (case-sensitive, server contract).
 */
export function specialAclKindFromName(
  name: string | undefined | null,
): SpecialAclKind | null {
  const n = (name ?? "").trim();
  if (n === SPECIAL_ACL_DEFAULT_NAME) return "default";
  if (n === SPECIAL_ACL_ANY_COMMUNITY_NAME) return "any-community";
  return null;
}

export function hasSpecialAclEntry(
  entries: readonly ObjectAclEntry[],
  kind: SpecialAclKind,
): boolean {
  return entries.some((e) => specialAclKind(e) === kind);
}

/** Which specials are missing from the current entry list (Default then AnyCommunity). */
export function missingSpecialAclKinds(
  entries: readonly ObjectAclEntry[],
): SpecialAclKind[] {
  const missing: SpecialAclKind[] = [];
  if (!hasSpecialAclEntry(entries, "default")) missing.push("default");
  if (!hasSpecialAclEntry(entries, "any-community")) missing.push("any-community");
  return missing;
}

/**
 * Template entry for add/save — matches REST TypedPrincipal shapes used by
 * ObjectAclSection (type.type = PrincipalTypes, name = principal name).
 */
export function createSpecialAclEntryTemplate(
  kind: SpecialAclKind,
  aclId?: number,
): ObjectAclEntry {
  const name = specialAclPrincipalName(kind);
  const type = specialAclPrincipalType(kind);
  return {
    name,
    principal: { name, type },
    type: { type, name },
    // READ is a safe design default; user can toggle other permissions.
    permissions: [{ permission: "READ" }],
    aclId,
  };
}

/**
 * Duplicate check: specials are unique by kind (one Default, one AnyCommunity);
 * ordinary entries remain unique by name+type (case-insensitive name).
 */
export function isDuplicateAclEntry(
  entries: readonly ObjectAclEntry[],
  name: string,
  type: string,
): boolean {
  const special = specialAclKindFromName(name);
  if (special) {
    return hasSpecialAclEntry(entries, special);
  }
  const wantType = (type || "").toUpperCase();
  const wantName = name.trim().toLowerCase();
  return entries.some((e) => {
    const en = entryPrincipalName(e).toLowerCase();
    const et = entryPrincipalType(e);
    return en === wantName && et === wantType;
  });
}

/**
 * Stable display order: Default, AnyCommunity, then remaining entries in
 * original relative order (Workbench-style).
 */
export function orderAclEntriesWithSpecialsFirst<T extends ObjectAclEntry>(
  entries: readonly T[],
): T[] {
  const defaults: T[] = [];
  const anyCommunity: T[] = [];
  const rest: T[] = [];
  for (const e of entries) {
    const kind = specialAclKind(e);
    if (kind === "default") defaults.push(e);
    else if (kind === "any-community") anyCommunity.push(e);
    else rest.push(e);
  }
  return [...defaults, ...anyCommunity, ...rest];
}
