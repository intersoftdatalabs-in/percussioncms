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

import {
  ACL_PERMISSIONS,
  type AclPermissionName,
} from "../api/developer/aclApi";

/**
 * Workbench Object ACL permission layers (CD-19 / FR inventory §5.4):
 * <ul>
 *   <li><b>Design access</b> — Read, Update, Delete, Modify ACL (OWNER)</li>
 *   <li><b>Runtime access</b> — Content Explorer / community visibility
 *       ({@code RUNTIME_VISIBLE})</li>
 * </ul>
 * Matches REST {@code Permissions} and server {@code PSPermissions}.
 */
export type AclPermissionLayer = "design" | "runtime";

/**
 * Stable design-object kind keys for ACL editor mounts.
 * Runtime-relevant kinds are those Workbench shows runtime visibility for.
 */
export type AclDesignObjectKind =
  | "content-type"
  | "display-format"
  | "action-menu"
  | "menu-entry"
  | "search"
  | "site"
  | "template"
  | "variant"
  | "view"
  | "workflow"
  | "slot"
  | "keyword"
  | "locale"
  | "shared-field"
  | "item-filter"
  | "extension"
  | "relationship-type"
  | "pipeline"
  | "community"
  | "unknown";

/**
 * Design-time access flags (Workbench: Read, Update, Delete, Modify ACL).
 * OWNER is design-layer "Modify ACL", not runtime — ordered after DELETE.
 */
export const DESIGN_ACCESS_PERMISSIONS: readonly AclPermissionName[] = [
  "READ",
  "UPDATE",
  "DELETE",
  "OWNER",
] as const;

/**
 * Runtime access flag (Workbench: runtime Read / Content Explorer visibility).
 */
export const RUNTIME_ACCESS_PERMISSIONS: readonly AclPermissionName[] = [
  "RUNTIME_VISIBLE",
] as const;

/**
 * Column order for the ACL editor when both layers are shown:
 * design block, then runtime (Workbench dialog layout).
 */
export const LAYERED_ACL_PERMISSIONS: readonly AclPermissionName[] = [
  ...DESIGN_ACCESS_PERMISSIONS,
  ...RUNTIME_ACCESS_PERMISSIONS,
] as const;

/**
 * Design-object kinds where Workbench shows Runtime access (FR §5.4 item 5):
 * Content Types, Display Formats, Menus, Menu Entries, Searches, Sites,
 * Templates, Variants, Views, Workflows.
 */
export const RUNTIME_RELEVANT_OBJECT_KINDS: ReadonlySet<AclDesignObjectKind> =
  new Set<AclDesignObjectKind>([
    "content-type",
    "display-format",
    "action-menu",
    "menu-entry",
    "search",
    "site",
    "template",
    "variant",
    "view",
    "workflow",
  ]);

/** Classify a REST permission name into design vs runtime layer. */
export function aclPermissionLayer(
  permission: string | null | undefined,
): AclPermissionLayer | null {
  const p = (permission ?? "").trim().toUpperCase();
  if (!p) return null;
  if ((RUNTIME_ACCESS_PERMISSIONS as readonly string[]).includes(p)) {
    return "runtime";
  }
  if ((DESIGN_ACCESS_PERMISSIONS as readonly string[]).includes(p)) {
    return "design";
  }
  // Unknown / future flags: treat as design so they stay editable if present
  if ((ACL_PERMISSIONS as readonly string[]).includes(p)) {
    return "design";
  }
  return null;
}

export function isDesignAccessPermission(permission: string): boolean {
  return aclPermissionLayer(permission) === "design";
}

export function isRuntimeAccessPermission(permission: string): boolean {
  return aclPermissionLayer(permission) === "runtime";
}

export function isRuntimeRelevantObjectKind(
  kind: AclDesignObjectKind | null | undefined,
): boolean {
  if (kind == null || kind === "unknown") {
    // Unknown mounts: show runtime columns so RUNTIME_VISIBLE stays editable.
    return true;
  }
  return RUNTIME_RELEVANT_OBJECT_KINDS.has(kind);
}

/**
 * Whether the ACL editor should render the Runtime visibility column group.
 * Runtime-relevant kinds always show it; non-runtime kinds hide it unless the
 * caller forces via {@code forceShow} (e.g. existing runtime bits on load).
 */
export function shouldShowRuntimeAccessColumns(
  kind: AclDesignObjectKind | null | undefined,
  options?: { forceShow?: boolean },
): boolean {
  if (options?.forceShow) return true;
  return isRuntimeRelevantObjectKind(kind);
}

/**
 * Permissions to expose as toggle columns for the given object kind.
 * Design flags always included; runtime flags only when runtime columns show.
 */
export function visibleAclPermissionsForObject(
  kind: AclDesignObjectKind | null | undefined,
  options?: { forceShowRuntime?: boolean },
): readonly AclPermissionName[] {
  if (
    shouldShowRuntimeAccessColumns(kind, {
      forceShow: options?.forceShowRuntime,
    })
  ) {
    return LAYERED_ACL_PERMISSIONS;
  }
  return DESIGN_ACCESS_PERMISSIONS;
}

/**
 * Short English labels aligned with Workbench ACL dialog wording.
 * UI may prefer i18n via {@code DEV_MSG}; these are pure-helper fallbacks.
 */
export const ACL_PERMISSION_SHORT_LABEL: Readonly<
  Record<AclPermissionName, string>
> = {
  READ: "Read",
  UPDATE: "Update",
  DELETE: "Delete",
  OWNER: "Modify ACL",
  RUNTIME_VISIBLE: "Visible",
};

export function aclPermissionShortLabel(permission: string): string {
  const p = permission.trim().toUpperCase() as AclPermissionName;
  if (p in ACL_PERMISSION_SHORT_LABEL) {
    return ACL_PERMISSION_SHORT_LABEL[p];
  }
  return permission.replace(/_/g, " ");
}

/**
 * Partition chosen permission names into design vs runtime sets (for tests /
 * summary chips). Unknown names land in {@code other}.
 */
export function partitionAclPermissions(permissions: readonly string[]): {
  design: string[];
  runtime: string[];
  other: string[];
} {
  const design: string[] = [];
  const runtime: string[] = [];
  const other: string[] = [];
  for (const raw of permissions) {
    const p = String(raw ?? "").trim();
    if (!p) continue;
    const layer = aclPermissionLayer(p);
    if (layer === "design") design.push(p.toUpperCase());
    else if (layer === "runtime") runtime.push(p.toUpperCase());
    else other.push(p);
  }
  return { design, runtime, other };
}

/**
 * True when any of the given permission names is a runtime-layer flag.
 * Useful to force-show runtime columns when editing a non-runtime kind that
 * already has RUNTIME_VISIBLE (preserve/edit path).
 */
export function hasRuntimeAccessPermission(
  permissions: Iterable<string>,
): boolean {
  for (const p of permissions) {
    if (isRuntimeAccessPermission(p)) return true;
  }
  return false;
}
