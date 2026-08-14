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

import React, { useEffect, useMemo, useState } from "react";
import {
  ACL_PERMISSIONS,
  createObjectAcl,
  getAclForObject,
  saveObjectAcl,
  type AclPermissionName,
} from "../api/developer/aclApi";
import { loadDefaultAclTemplate } from "../api/developer/preferencesApi";
import type {
  ObjectAcl,
  ObjectAclEntry,
  ObjectAclPermission,
} from "../api/developer/types";
import { isSessionRedirectError, type ApiError } from "../api/client";
import { catalogColors, errorAlert, tableHeaderRow, tableRow } from "./catalogStyles";
import {
  mergeTemplateOntoAclEntries,
  shouldApplyDefaultAclTemplate,
} from "./defaultAclTemplate";
import { DEV_MSG } from "./messages";
import {
  DESIGN_ACCESS_PERMISSIONS,
  RUNTIME_ACCESS_PERMISSIONS,
  hasRuntimeAccessPermission,
  shouldShowRuntimeAccessColumns,
  type AclDesignObjectKind,
  type AclPermissionLayer,
  visibleAclPermissionsForObject,
} from "./objectAclPermissionModel";
import {
  canRemoveAclEntry,
  createSpecialAclEntryTemplate,
  isDuplicateAclEntry,
  missingSpecialAclKinds,
  orderAclEntriesWithSpecialsFirst,
  specialAclKind,
  specialAclKindFromName,
  specialAclPrincipalName,
  specialAclPrincipalType,
  type SpecialAclKind,
} from "./objectAclSpecialEntries";

/** Principal types supported when adding an ACL entry (REST TypedPrincipal / PrincipalTypes). */
export const ACL_ENTRY_TYPES = ["ROLE", "USER", "COMMUNITY", "GROUP"] as const;
export type AclEntryTypeName = (typeof ACL_ENTRY_TYPES)[number];

type DraftEntry = ObjectAclEntry & { clientKey: string };

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

const smallBtnStyle: React.CSSProperties = {
  background: "transparent",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  padding: "4px 8px",
  cursor: "pointer",
};

function asEntries(acl: ObjectAcl | null): ObjectAclEntry[] {
  if (!acl?.aclEntries) return [];
  if (Array.isArray(acl.aclEntries)) return acl.aclEntries;
  const env = acl.aclEntries as { AclEntry?: ObjectAclEntry[] };
  return Array.isArray(env.AclEntry) ? env.AclEntry : [];
}

function asPermissions(entry: ObjectAclEntry): string[] {
  const raw = entry.permissions;
  if (!raw) return [];
  let list: ObjectAclPermission[] = [];
  if (Array.isArray(raw)) list = raw;
  else if (Array.isArray((raw as { UserAccessLevel?: ObjectAclPermission[] }).UserAccessLevel)) {
    list = (raw as { UserAccessLevel: ObjectAclPermission[] }).UserAccessLevel;
  }
  const out: string[] = [];
  for (const p of list) {
    if (!p) continue;
    if (p.permission) out.push(String(p.permission));
    const nested = p.permissions;
    if (Array.isArray(nested)) {
      for (const n of nested) out.push(String(n));
    } else if (nested && Array.isArray((nested as { Permission?: string[] }).Permission)) {
      for (const n of (nested as { Permission: string[] }).Permission) out.push(String(n));
    }
  }
  return out.length ? [...new Set(out)] : [];
}

function permissionIdMap(entry: ObjectAclEntry): Map<string, number> {
  const map = new Map<string, number>();
  const raw = entry.permissions;
  if (!raw) return map;
  let list: ObjectAclPermission[] = [];
  if (Array.isArray(raw)) list = raw;
  else if (Array.isArray((raw as { UserAccessLevel?: ObjectAclPermission[] }).UserAccessLevel)) {
    list = (raw as { UserAccessLevel: ObjectAclPermission[] }).UserAccessLevel;
  }
  for (const p of list) {
    if (p?.permission && p.id != null) {
      map.set(String(p.permission), p.id);
    }
  }
  return map;
}

function stableServerKey(e: ObjectAclEntry, index: number): string {
  if (e.id != null) return `id:${e.id}`;
  if (e.name) return `name:${e.name}`;
  if (e.principal?.name) return `principal:${e.principal.name}`;
  return `idx:${index}`;
}

function toDraftEntries(list: ObjectAclEntry[]): DraftEntry[] {
  const ordered = orderAclEntriesWithSpecialsFirst(list);
  return ordered.map((e, i) => ({
    ...e,
    principal: e.principal ? { ...e.principal } : undefined,
    type: e.type ? { ...e.type } : undefined,
    permissions: Array.isArray(e.permissions)
      ? e.permissions.map((p) => ({ ...p }))
      : e.permissions,
    clientKey: stableServerKey(e, i),
  }));
}

function entryLabel(e: ObjectAclEntry): string {
  const kind = specialAclKind(e);
  if (kind === "default") return DEV_MSG.ACL_SPECIAL_DEFAULT_LABEL;
  if (kind === "any-community") return DEV_MSG.ACL_SPECIAL_ANY_COMMUNITY_LABEL;
  return (
    e.name ||
    e.principal?.name ||
    e.type?.name ||
    (e.id != null ? `entry-${e.id}` : "—")
  );
}

function entryTypeLabel(e: ObjectAclEntry): string {
  const kind = specialAclKind(e);
  if (kind === "default") return DEV_MSG.ACL_SPECIAL_TYPE_DEFAULT;
  if (kind === "any-community") return DEV_MSG.ACL_SPECIAL_TYPE_ANY_COMMUNITY;
  // Prefer PrincipalTypes enum on type.type; fall back to type.name / principal.type
  return e.type?.type || e.type?.name || e.principal?.type || "—";
}

function permsEqual(a: Set<string>, b: Set<string>): boolean {
  if (a.size !== b.size) return false;
  for (const p of a) {
    if (!b.has(p)) return false;
  }
  return true;
}

function buildSelectedMap(entries: DraftEntry[]): Record<string, Set<string>> {
  const next: Record<string, Set<string>> = {};
  for (const e of entries) {
    next[e.clientKey] = new Set(asPermissions(e));
  }
  return next;
}

function structureEqual(a: DraftEntry[], b: DraftEntry[]): boolean {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (a[i].clientKey !== b[i].clientKey) return false;
  }
  return true;
}

/** Kind-aware empty copy when the parent panel cannot supply an object GUID. */
export function noGuidCopy(objectKind?: AclDesignObjectKind | null): string {
  switch (objectKind) {
    case "site":
      return DEV_MSG.ACL_NO_GUID_SITE;
    case "display-format":
      return DEV_MSG.ACL_NO_GUID_DISPLAY_FORMAT;
    default:
      return DEV_MSG.ACL_NO_GUID;
  }
}

/** i18n label for a known REST permission column (Workbench wording). */
function permissionColumnLabel(perm: AclPermissionName): string {
  switch (perm) {
    case "READ":
      return DEV_MSG.ACL_PERM_READ;
    case "UPDATE":
      return DEV_MSG.ACL_PERM_UPDATE;
    case "DELETE":
      return DEV_MSG.ACL_PERM_DELETE;
    case "OWNER":
      return DEV_MSG.ACL_PERM_OWNER;
    case "RUNTIME_VISIBLE":
      return DEV_MSG.ACL_PERM_RUNTIME_VISIBLE;
    default:
      return String(perm).replace(/_/g, " ");
  }
}

/**
 * Object ACL viewer/editor for Developer detail panels (SE-04 / CD-19).
 * Design vs runtime permission columns (Workbench parity), toggle permissions,
 * add/remove entries, save via PUT /acls/bulk.
 */
export function ObjectAclSection({
  objectGuid,
  objectKind = null,
  testIdPrefix = "developer-acl",
}: {
  objectGuid: string | null | undefined;
  /**
   * Design-object kind for runtime-visibility column gating (CD-19).
   * Content types / templates / etc. show Runtime visibility; others hide it
   * unless an entry already carries RUNTIME_VISIBLE.
   */
  objectKind?: AclDesignObjectKind | null;
  testIdPrefix?: string;
}): React.ReactElement {
  const [acl, setAcl] = useState<ObjectAcl | null>(null);
  const [draftEntries, setDraftEntries] = useState<DraftEntry[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [missing, setMissing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [selected, setSelected] = useState<Record<string, Set<string>>>({});
  const [newName, setNewName] = useState("");
  const [newType, setNewType] = useState<AclEntryTypeName>("ROLE");
  const [newSeq, setNewSeq] = useState(0);
  const [ownerName, setOwnerName] = useState("");
  const [ownerType, setOwnerType] = useState<AclEntryTypeName>("USER");

  useEffect(() => {
    if (!objectGuid) {
      setAcl(null);
      setDraftEntries([]);
      setError(null);
      setNotice(null);
      setMissing(false);
      setLoading(false);
      setSelected({});
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    setNotice(null);
    setMissing(false);
    setAcl(null);
    setDraftEntries([]);
    setSelected({});
    getAclForObject(objectGuid)
      .then((a) => {
        if (cancelled) return;
        setAcl(a);
        const entries = toDraftEntries(asEntries(a));
        setDraftEntries(entries);
        setSelected(buildSelectedMap(entries));
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setLoading(false);
        if (isSessionRedirectError(err)) {
          setError(DEV_MSG.SESSION_REDIRECT);
          return;
        }
        const api = err as ApiError;
        if (api && typeof api.status === "number" && api.status === 404) {
          setMissing(true);
          return;
        }
        const status =
          api && typeof api.status === "number" ? ` (${api.status})` : "";
        const msg = err instanceof Error && err.message ? ` ${err.message}` : "";
        setError(`${DEV_MSG.ACL_ERROR}${status}${msg}`);
      });
    return () => {
      cancelled = true;
    };
  }, [objectGuid]);

  const initialEntries = useMemo(() => toDraftEntries(asEntries(acl)), [acl]);
  const initialSelected = useMemo(() => buildSelectedMap(initialEntries), [initialEntries]);

  const structureDirty = !structureEqual(draftEntries, initialEntries);
  const permsDirty = draftEntries.some((e) => {
    if (e.clientKey.startsWith("__new:")) return true;
    const cur = selected[e.clientKey] ?? new Set<string>();
    const init = initialSelected[e.clientKey] ?? new Set<string>();
    return !permsEqual(cur, init);
  });
  const dirty = acl != null && (structureDirty || permsDirty);
  const missingSpecials = useMemo(
    () => missingSpecialAclKinds(draftEntries),
    [draftEntries],
  );

  /** Force-show runtime columns when any draft entry already has runtime bits. */
  const forceShowRuntime = useMemo(() => {
    for (const e of draftEntries) {
      const chosen = selected[e.clientKey];
      if (chosen && hasRuntimeAccessPermission(chosen)) return true;
      if (hasRuntimeAccessPermission(asPermissions(e))) return true;
    }
    return false;
  }, [draftEntries, selected]);

  const showRuntimeColumns = shouldShowRuntimeAccessColumns(objectKind, {
    forceShow: forceShowRuntime,
  });

  const visiblePermissions = useMemo(
    () =>
      visibleAclPermissionsForObject(objectKind, {
        forceShowRuntime: forceShowRuntime,
      }),
    [objectKind, forceShowRuntime],
  );

  const designPerms = DESIGN_ACCESS_PERMISSIONS;
  const runtimePerms = showRuntimeColumns ? RUNTIME_ACCESS_PERMISSIONS : [];

  function togglePerm(key: string, perm: AclPermissionName) {
    setSelected((prev) => {
      const cur = new Set(prev[key] ?? []);
      if (cur.has(perm)) cur.delete(perm);
      else cur.add(perm);
      return { ...prev, [key]: cur };
    });
    setNotice(null);
  }

  function removeEntry(clientKey: string) {
    const target = draftEntries.find((e) => e.clientKey === clientKey);
    if (target && !canRemoveAclEntry(target)) {
      return;
    }
    setDraftEntries((prev) => prev.filter((e) => e.clientKey !== clientKey));
    setSelected((prev) => {
      const copy = { ...prev };
      delete copy[clientKey];
      return copy;
    });
    setError(null);
    setNotice(null);
  }

  function appendDraftEntry(partial: ObjectAclEntry) {
    const seq = newSeq + 1;
    setNewSeq(seq);
    const clientKey = `__new:${seq}`;
    const entry: DraftEntry = {
      ...partial,
      clientKey,
      permissions: Array.isArray(partial.permissions)
        ? partial.permissions.map((p) => ({ ...p }))
        : partial.permissions,
    };
    setDraftEntries((prev) => orderAclEntriesWithSpecialsFirst([...prev, entry]));
    setSelected((prev) => ({
      ...prev,
      [clientKey]: new Set<string>(asPermissions(entry).length ? asPermissions(entry) : ["READ"]),
    }));
    return clientKey;
  }

  function addSpecialEntry(kind: SpecialAclKind) {
    if (isDuplicateAclEntry(draftEntries, specialAclPrincipalName(kind), specialAclPrincipalType(kind))) {
      setError(DEV_MSG.ACL_ENTRY_DUP);
      return;
    }
    appendDraftEntry(createSpecialAclEntryTemplate(kind, acl?.id));
    setError(null);
    setNotice(null);
  }

  function addEntry() {
    const name = newName.trim();
    if (!name) return;
    // Coerce special names to server PrincipalTypes (USER / COMMUNITY).
    const special = specialAclKindFromName(name);
    const effectiveType: AclEntryTypeName = special
      ? specialAclPrincipalType(special)
      : newType;
    const effectiveName = special ? specialAclPrincipalName(special) : name;
    if (isDuplicateAclEntry(draftEntries, effectiveName, effectiveType)) {
      setError(DEV_MSG.ACL_ENTRY_DUP);
      return;
    }
    appendDraftEntry({
      name: effectiveName,
      principal: { name: effectiveName, type: effectiveType },
      type: { type: effectiveType, name: effectiveName },
      permissions: [{ permission: "READ" }],
      aclId: acl?.id,
    });
    setNewName("");
    setError(null);
    setNotice(null);
  }

  function formatApiErr(fallback: string, err: unknown): string {
    if (isSessionRedirectError(err)) return DEV_MSG.SESSION_REDIRECT;
    const api = err as ApiError;
    const status =
      api && typeof api.status === "number" ? ` (${api.status})` : "";
    const msg = err instanceof Error && err.message ? ` ${err.message}` : "";
    return `${fallback}${status}${msg}`;
  }

  function buildPermissionsForSave(
    entry: ObjectAclEntry,
    chosen: Set<string>,
  ): ObjectAclPermission[] {
    const idByPerm = permissionIdMap(entry);
    const known = new Set<string>(ACL_PERMISSIONS as readonly string[]);
    const out: ObjectAclPermission[] = [];
    for (const p of ACL_PERMISSIONS) {
      if (!chosen.has(p)) continue;
      const id = idByPerm.get(p);
      out.push(id != null ? { id, permission: p } : { permission: p });
    }
    for (const p of chosen) {
      if (known.has(p)) continue;
      const id = idByPerm.get(p);
      out.push(id != null ? { id, permission: p } : { permission: p });
    }
    for (const p of asPermissions(entry)) {
      if (known.has(p)) continue;
      if (out.some((x) => x.permission === p)) continue;
      const id = idByPerm.get(p);
      out.push(id != null ? { id, permission: p } : { permission: p });
    }
    return out;
  }

  function rebuildEntriesForSave(): ObjectAclEntry[] {
    return draftEntries.map((e) => {
      const chosen = selected[e.clientKey] ?? new Set<string>();
      // Preserve name fallbacks used by the pre-add/remove save path
      const special = specialAclKind(e);
      const principalName = special
        ? specialAclPrincipalName(special)
        : e.name || e.principal?.name || e.type?.name || "";
      // Specials always use server PrincipalTypes (USER / COMMUNITY), even if a
      // historical payload mis-typed Default as ROLE.
      const typeName = special
        ? specialAclPrincipalType(special)
        : e.type?.type || "ROLE";
      // Preserve extra fields from server principal/type objects when present
      const principal = e.principal
        ? {
            ...e.principal,
            name: principalName || e.principal.name,
            type: special ? typeName : e.principal.type || typeName,
          }
        : principalName
          ? { name: principalName, type: typeName }
          : undefined;
      const type = e.type
        ? {
            ...e.type,
            type: special ? typeName : e.type.type || typeName,
            name: principalName || e.type.name,
          }
        : {
            type: typeName,
            name: principalName || undefined,
          };
      return {
        id: e.id,
        name: principalName || undefined,
        aclId: e.aclId ?? acl?.id,
        principal,
        type,
        permissions: buildPermissionsForSave(e, chosen),
      };
    });
  }

  async function handleSave() {
    if (!acl || !objectGuid) return;
    setBusy(true);
    setError(null);
    setNotice(null);

    const rebuiltEntries = rebuildEntriesForSave();
    const toSave: ObjectAcl = {
      ...acl,
      objectGuid: acl.objectGuid ?? { stringValue: objectGuid },
      aclEntries: rebuiltEntries,
    };

    try {
      await saveObjectAcl(toSave);
    } catch (err: unknown) {
      setError(formatApiErr(DEV_MSG.ACL_SAVE_ERROR, err));
      setBusy(false);
      return;
    }

    try {
      const refreshed = await getAclForObject(objectGuid);
      setAcl(refreshed);
      const entries = toDraftEntries(asEntries(refreshed));
      setDraftEntries(entries);
      setSelected(buildSelectedMap(entries));
      setNotice(DEV_MSG.ACL_SAVED);
    } catch (err: unknown) {
      setAcl({ ...acl, aclEntries: rebuiltEntries });
      const entries = toDraftEntries(rebuiltEntries);
      setDraftEntries(entries);
      setSelected(buildSelectedMap(entries));
      setNotice(DEV_MSG.ACL_SAVED);
      setError(formatApiErr(DEV_MSG.ACL_RELOAD_ERROR, err));
    } finally {
      setBusy(false);
    }
  }

  async function handleCreate() {
    if (!objectGuid) return;
    const name = ownerName.trim();
    if (!name) return;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      let created = await createObjectAcl(objectGuid, {
        name,
        type: ownerType,
      });
      // Workbench parity: merge default ACL template preference onto the new ACL.
      let templateApplied = false;
      let templateApplyFailed = false;
      try {
        const { template } = await loadDefaultAclTemplate();
        if (shouldApplyDefaultAclTemplate(template)) {
          const existing = asEntries(created);
          const { entries: merged, added } = mergeTemplateOntoAclEntries(
            existing,
            template,
            created.id,
          );
          if (added > 0) {
            await saveObjectAcl({
              ...created,
              objectGuid: created.objectGuid ?? { stringValue: objectGuid },
              aclEntries: merged,
            });
            try {
              created = await getAclForObject(objectGuid);
            } catch {
              created = { ...created, aclEntries: merged };
            }
            templateApplied = true;
          }
        }
      } catch {
        // Create succeeded; template is best-effort (pref API / bulk save).
        templateApplyFailed = true;
      }
      setMissing(false);
      setAcl(created);
      const entries = toDraftEntries(asEntries(created));
      setDraftEntries(entries);
      setSelected(buildSelectedMap(entries));
      if (templateApplied) {
        setNotice(DEV_MSG.ACL_TEMPLATE_APPLIED);
      } else {
        setNotice(DEV_MSG.ACL_SAVED);
      }
      if (templateApplyFailed) {
        setError(DEV_MSG.ACL_TEMPLATE_APPLY_ERROR);
      }
    } catch (err: unknown) {
      setError(formatApiErr(DEV_MSG.ACL_CREATE_ERROR, err));
    } finally {
      setBusy(false);
    }
  }

  if (!objectGuid) {
    // Still expose kind + runtime gating so product-path / peer mounts can assert
    // Workbench-aligned column policy when list/detail payloads omit guid (#2642).
    const showRuntimeNoGuid = shouldShowRuntimeAccessColumns(objectKind, {
      forceShow: false,
    });
    return (
      <section
        style={{ marginBottom: "16px" }}
        data-testid={`${testIdPrefix}-section`}
        data-acl-object-kind={objectKind ?? "unknown"}
        data-acl-show-runtime={showRuntimeNoGuid ? "true" : "false"}
        data-acl-has-guid="false"
      >
        <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.ACL_TITLE}</h3>
        <p style={{ color: catalogColors.empty }} data-testid={`${testIdPrefix}-no-guid`}>
          {noGuidCopy(objectKind)}
        </p>
      </section>
    );
  }

  return (
    <section
      style={{ marginBottom: "16px" }}
      data-testid={`${testIdPrefix}-section`}
      data-acl-object-kind={objectKind ?? "unknown"}
      data-acl-has-guid="true"
    >
      <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.ACL_TITLE}</h3>
      <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.ACL_HINT}</p>
      <p
        style={{ color: catalogColors.muted, fontSize: "0.85rem" }}
        data-testid={`${testIdPrefix}-special-hint`}
      >
        {DEV_MSG.ACL_SPECIAL_HINT}
      </p>

      {loading ? (
        <div data-testid={`${testIdPrefix}-loading`}>{DEV_MSG.ACL_LOADING}</div>
      ) : null}

      {error ? (
        <div role="alert" data-testid={`${testIdPrefix}-error`} style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid={`${testIdPrefix}-notice`} style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {missing && !loading ? (
        <div data-testid={`${testIdPrefix}-empty`}>
          <p style={{ color: catalogColors.empty }}>{DEV_MSG.ACL_EMPTY}</p>
          <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.ACL_EMPTY_HINT}</p>
          <div
            style={{
              marginTop: "12px",
              display: "grid",
              gridTemplateColumns: "1fr auto auto",
              gap: "8px",
              alignItems: "end",
            }}
            data-testid={`${testIdPrefix}-create-form`}
          >
            <div>
              <label
                htmlFor={`${testIdPrefix}-owner-name`}
                style={{ display: "block", marginBottom: 4 }}
              >
                {DEV_MSG.ACL_OWNER_NAME}
              </label>
              <input
                id={`${testIdPrefix}-owner-name`}
                data-testid={`${testIdPrefix}-owner-name`}
                style={inputStyle}
                placeholder={DEV_MSG.ACL_OWNER_NAME_PLACEHOLDER}
                value={ownerName}
                onChange={(e) => setOwnerName(e.target.value)}
                disabled={busy}
              />
            </div>
            <div>
              <label
                htmlFor={`${testIdPrefix}-owner-type`}
                style={{ display: "block", marginBottom: 4 }}
              >
                {DEV_MSG.ACL_COL_TYPE}
              </label>
              <select
                id={`${testIdPrefix}-owner-type`}
                data-testid={`${testIdPrefix}-owner-type`}
                style={{ ...inputStyle, width: "auto", minWidth: 120 }}
                value={ownerType}
                onChange={(e) => setOwnerType(e.target.value as AclEntryTypeName)}
                disabled={busy}
              >
                {ACL_ENTRY_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
            <button
              type="button"
              data-testid={`${testIdPrefix}-create`}
              disabled={busy || !ownerName.trim()}
              onClick={() => void handleCreate()}
              style={{
                padding: "8px 16px",
                background: ownerName.trim() ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: busy || !ownerName.trim() ? "not-allowed" : "pointer",
              }}
            >
              {busy ? DEV_MSG.ACL_CREATING : DEV_MSG.ACL_CREATE}
            </button>
          </div>
        </div>
      ) : null}

      {acl && !loading ? (
        <>
          <div
            style={{ fontFamily: "monospace", color: catalogColors.muted, fontSize: "0.85rem" }}
            data-testid={`${testIdPrefix}-meta`}
          >
            {acl.name || "ACL"}
            {acl.id != null ? ` · id ${acl.id}` : ""}
            {acl.guid?.stringValue ? ` · ${acl.guid.stringValue}` : ""}
          </div>
          {draftEntries.length === 0 ? (
            <p style={{ color: catalogColors.empty }} data-testid={`${testIdPrefix}-no-entries`}>
              {DEV_MSG.ACL_NO_ENTRIES}
            </p>
          ) : (
            <div style={{ overflowX: "auto", marginTop: "8px" }}>
              <table
                data-testid={`${testIdPrefix}-table`}
                data-acl-show-runtime={showRuntimeColumns ? "true" : "false"}
                data-acl-object-kind={objectKind ?? "unknown"}
                style={{
                  width: "100%",
                  borderCollapse: "collapse",
                  fontSize: "0.9rem",
                }}
              >
                <thead>
                  {/* Layer group headers — Design access | Runtime visibility (CD-19) */}
                  <tr
                    style={tableHeaderRow}
                    data-testid={`${testIdPrefix}-layer-headers`}
                  >
                    <th style={{ padding: "8px" }} rowSpan={2}>
                      {DEV_MSG.ACL_COL_ENTRY}
                    </th>
                    <th style={{ padding: "8px" }} rowSpan={2}>
                      {DEV_MSG.ACL_COL_TYPE}
                    </th>
                    <th
                      colSpan={designPerms.length}
                      style={{
                        padding: "6px 8px",
                        textAlign: "center",
                        fontSize: "0.8rem",
                        borderBottom: `1px solid ${catalogColors.softBorder}`,
                        background: "rgba(0,0,0,0.02)",
                      }}
                      title={DEV_MSG.ACL_LAYER_DESIGN_HINT}
                      data-testid={`${testIdPrefix}-layer-design`}
                      data-acl-layer={"design" satisfies AclPermissionLayer}
                    >
                      {DEV_MSG.ACL_LAYER_DESIGN}
                    </th>
                    {showRuntimeColumns ? (
                      <th
                        colSpan={runtimePerms.length}
                        style={{
                          padding: "6px 8px",
                          textAlign: "center",
                          fontSize: "0.8rem",
                          borderBottom: `1px solid ${catalogColors.softBorder}`,
                          background: "rgba(0,0,0,0.02)",
                        }}
                        title={DEV_MSG.ACL_LAYER_RUNTIME_HINT}
                        data-testid={`${testIdPrefix}-layer-runtime`}
                        data-acl-layer={"runtime" satisfies AclPermissionLayer}
                      >
                        {DEV_MSG.ACL_LAYER_RUNTIME}
                      </th>
                    ) : null}
                    <th style={{ padding: "8px" }} rowSpan={2}>
                      {DEV_MSG.ACL_COL_ACTIONS}
                    </th>
                  </tr>
                  <tr
                    style={tableHeaderRow}
                    data-testid={`${testIdPrefix}-perm-headers`}
                  >
                    {visiblePermissions.map((p) => (
                      <th
                        key={p}
                        style={{
                          padding: "8px",
                          textAlign: "center",
                          fontSize: "0.8rem",
                          fontWeight: 500,
                        }}
                        data-testid={`${testIdPrefix}-perm-header-${p}`}
                        data-acl-permission={p}
                        title={p}
                      >
                        {permissionColumnLabel(p)}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {draftEntries.map((e) => {
                    const key = e.clientKey;
                    const chosen = selected[key] ?? new Set<string>();
                    const label = entryLabel(e);
                    const kind = specialAclKind(e);
                    const removable = canRemoveAclEntry(e);
                    return (
                      <tr
                        key={key}
                        style={tableRow}
                        data-testid={`${testIdPrefix}-row-${key}`}
                        data-special-acl={kind ?? undefined}
                      >
                        <td style={{ padding: "8px" }}>
                          <span data-testid={`${testIdPrefix}-label-${key}`}>{label}</span>
                          {kind ? (
                            <span
                              data-testid={`${testIdPrefix}-special-badge-${kind}`}
                              style={{
                                marginLeft: "8px",
                                fontSize: "0.75rem",
                                color: catalogColors.muted,
                              }}
                            >
                              {DEV_MSG.ACL_SPECIAL_PROTECTED}
                            </span>
                          ) : null}
                        </td>
                        <td
                          style={{ padding: "8px", fontFamily: "monospace" }}
                          data-testid={`${testIdPrefix}-type-${key}`}
                        >
                          {entryTypeLabel(e)}
                        </td>
                        {visiblePermissions.map((p) => (
                          <td key={p} style={{ padding: "8px", textAlign: "center" }}>
                            <input
                              type="checkbox"
                              data-testid={`${testIdPrefix}-perm-${key}-${p}`}
                              checked={chosen.has(p)}
                              disabled={busy}
                              onChange={() => togglePerm(key, p)}
                              aria-label={`${permissionColumnLabel(p)} (${p}) for ${label}`}
                            />
                          </td>
                        ))}
                        <td style={{ padding: "8px" }}>
                          {removable ? (
                            <button
                              type="button"
                              data-testid={`${testIdPrefix}-remove-${key}`}
                              aria-label={`Remove ACL entry ${label}`}
                              disabled={busy}
                              onClick={() => removeEntry(key)}
                              style={{
                                ...smallBtnStyle,
                                cursor: busy ? "not-allowed" : "pointer",
                              }}
                            >
                              {DEV_MSG.ACL_ENTRY_REMOVE}
                            </button>
                          ) : (
                            <span
                              data-testid={`${testIdPrefix}-protected-${key}`}
                              style={{ color: catalogColors.muted, fontSize: "0.85rem" }}
                              title={DEV_MSG.ACL_SPECIAL_HINT}
                            >
                              {DEV_MSG.ACL_SPECIAL_PROTECTED}
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {missingSpecials.length > 0 ? (
            <div
              style={{
                marginTop: "12px",
                display: "flex",
                flexWrap: "wrap",
                gap: "8px",
                alignItems: "center",
              }}
              data-testid={`${testIdPrefix}-special-actions`}
            >
              {missingSpecials.includes("default") ? (
                <button
                  type="button"
                  data-testid={`${testIdPrefix}-add-default`}
                  disabled={busy}
                  onClick={() => addSpecialEntry("default")}
                  style={{
                    ...smallBtnStyle,
                    padding: "8px 12px",
                    cursor: busy ? "not-allowed" : "pointer",
                  }}
                >
                  {DEV_MSG.ACL_SPECIAL_ADD_DEFAULT}
                </button>
              ) : null}
              {missingSpecials.includes("any-community") ? (
                <button
                  type="button"
                  data-testid={`${testIdPrefix}-add-any-community`}
                  disabled={busy}
                  onClick={() => addSpecialEntry("any-community")}
                  style={{
                    ...smallBtnStyle,
                    padding: "8px 12px",
                    cursor: busy ? "not-allowed" : "pointer",
                  }}
                >
                  {DEV_MSG.ACL_SPECIAL_ADD_ANY_COMMUNITY}
                </button>
              ) : null}
            </div>
          ) : null}

          <div
            style={{
              marginTop: "12px",
              display: "grid",
              gridTemplateColumns: "1fr auto auto",
              gap: "8px",
              alignItems: "end",
            }}
            data-testid={`${testIdPrefix}-add-form`}
          >
            <div>
              <label
                htmlFor={`${testIdPrefix}-add-name`}
                style={{ display: "block", marginBottom: 4 }}
              >
                {DEV_MSG.ACL_ENTRY_NAME}
              </label>
              <input
                id={`${testIdPrefix}-add-name`}
                data-testid={`${testIdPrefix}-add-name`}
                style={inputStyle}
                placeholder={DEV_MSG.ACL_ENTRY_NAME_PLACEHOLDER}
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                disabled={busy}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addEntry();
                  }
                }}
              />
            </div>
            <div>
              <label
                htmlFor={`${testIdPrefix}-add-type`}
                style={{ display: "block", marginBottom: 4 }}
              >
                {DEV_MSG.ACL_COL_TYPE}
              </label>
              <select
                id={`${testIdPrefix}-add-type`}
                data-testid={`${testIdPrefix}-add-type`}
                style={{ ...inputStyle, width: "auto", minWidth: 120 }}
                value={newType}
                onChange={(e) => setNewType(e.target.value as AclEntryTypeName)}
                disabled={busy}
              >
                {ACL_ENTRY_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
            <button
              type="button"
              data-testid={`${testIdPrefix}-add`}
              disabled={busy || !newName.trim()}
              onClick={addEntry}
              style={{
                ...smallBtnStyle,
                padding: "8px 12px",
                cursor: busy || !newName.trim() ? "not-allowed" : "pointer",
              }}
            >
              {DEV_MSG.ACL_ENTRY_ADD}
            </button>
          </div>

          <div style={{ marginTop: "12px", display: "flex", gap: "8px" }}>
            <button
              type="button"
              data-testid={`${testIdPrefix}-save`}
              aria-label="Save object ACL"
              disabled={busy || !dirty}
              onClick={() => void handleSave()}
              style={{
                padding: "8px 16px",
                background: dirty ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: busy || !dirty ? "not-allowed" : "pointer",
              }}
            >
              {busy ? DEV_MSG.ACL_SAVING : DEV_MSG.ACL_SAVE}
            </button>
          </div>
        </>
      ) : null}
    </section>
  );
}
