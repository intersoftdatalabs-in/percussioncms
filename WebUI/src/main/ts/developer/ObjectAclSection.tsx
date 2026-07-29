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

import React, { useEffect, useMemo, useState } from "react";
import {
  ACL_PERMISSIONS,
  getAclForObject,
  saveObjectAcl,
  type AclPermissionName,
} from "../api/developer/aclApi";
import type {
  ObjectAcl,
  ObjectAclEntry,
  ObjectAclPermission,
} from "../api/developer/types";
import { isSessionRedirectError, type ApiError } from "../api/client";
import { errorAlert } from "./catalogStyles";
import { DEV_MSG } from "./messages";

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

/** Map permission name → existing UserAccessLevel id (preserve on save). */
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

function entryLabel(e: ObjectAclEntry): string {
  return (
    e.name ||
    e.principal?.name ||
    e.type?.name ||
    (e.id != null ? `entry-${e.id}` : "—")
  );
}

function entryKey(e: ObjectAclEntry, index: number): string {
  if (e.id != null) return `id:${e.id}`;
  if (e.name) return `name:${e.name}`;
  if (e.principal?.name) return `principal:${e.principal.name}`;
  return `idx:${index}`;
}

function entryTypeLabel(e: ObjectAclEntry): string {
  return e.type?.type || e.type?.name || e.principal?.type || "—";
}

function permsEqual(a: Set<string>, b: Set<string>): boolean {
  if (a.size !== b.size) return false;
  for (const p of a) {
    if (!b.has(p)) return false;
  }
  return true;
}

/**
 * Object ACL viewer/editor for Developer detail panels (SE-04).
 * Toggles design-time permissions on existing entries and saves via PUT /acls/bulk.
 * Add/remove entries remains a later slice.
 */
export function ObjectAclSection({
  objectGuid,
  testIdPrefix = "developer-acl",
}: {
  objectGuid: string | null | undefined;
  testIdPrefix?: string;
}): React.ReactElement {
  const [acl, setAcl] = useState<ObjectAcl | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [missing, setMissing] = useState(false);
  const [busy, setBusy] = useState(false);
  /** entryKey → selected permission names */
  const [selected, setSelected] = useState<Record<string, Set<string>>>({});

  useEffect(() => {
    if (!objectGuid) {
      setAcl(null);
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
    setSelected({});
    getAclForObject(objectGuid)
      .then((a) => {
        if (cancelled) return;
        setAcl(a);
        const next: Record<string, Set<string>> = {};
        asEntries(a).forEach((e, i) => {
          next[entryKey(e, i)] = new Set(asPermissions(e));
        });
        setSelected(next);
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

  const entries = asEntries(acl);

  const initialSelected = useMemo(() => {
    const next: Record<string, Set<string>> = {};
    asEntries(acl).forEach((e, i) => {
      next[entryKey(e, i)] = new Set(asPermissions(e));
    });
    return next;
  }, [acl]);

  const dirty =
    entries.length > 0 &&
    entries.some((e, i) => {
      const key = entryKey(e, i);
      const cur = selected[key] ?? new Set<string>();
      const init = initialSelected[key] ?? new Set<string>();
      return !permsEqual(cur, init);
    });

  function togglePerm(key: string, perm: AclPermissionName) {
    setSelected((prev) => {
      const cur = new Set(prev[key] ?? []);
      if (cur.has(perm)) cur.delete(perm);
      else cur.add(perm);
      return { ...prev, [key]: cur };
    });
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

  /**
   * Build permission list for save: known ACL_PERMISSIONS toggles + any unknown
   * permission names already present on the entry (preserve on save).
   */
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
    // Preserve unknown/custom permissions still selected (or never shown as checkbox)
    for (const p of chosen) {
      if (known.has(p)) continue;
      const id = idByPerm.get(p);
      out.push(id != null ? { id, permission: p } : { permission: p });
    }
    // Also keep unknown permissions that were loaded but are not in chosen —
    // only if they never appear as toggles (cannot uncheck). Treat as sticky.
    for (const p of asPermissions(entry)) {
      if (known.has(p)) continue;
      if (out.some((x) => x.permission === p)) continue;
      const id = idByPerm.get(p);
      out.push(id != null ? { id, permission: p } : { permission: p });
    }
    return out;
  }

  async function handleSave() {
    if (!acl || !objectGuid) return;
    setBusy(true);
    setError(null);
    setNotice(null);

    const rebuiltEntries: ObjectAclEntry[] = entries.map((e, i) => {
      const key = entryKey(e, i);
      const chosen = selected[key] ?? new Set<string>();
      return {
        id: e.id,
        name: e.name || e.principal?.name || e.type?.name,
        aclId: e.aclId ?? acl.id,
        principal: e.principal || (e.name ? { name: e.name } : undefined),
        type: e.type,
        permissions: buildPermissionsForSave(e, chosen),
      };
    });

    try {
      await saveObjectAcl({ ...acl, aclEntries: rebuiltEntries });
    } catch (err: unknown) {
      setError(formatApiErr(DEV_MSG.ACL_SAVE_ERROR, err));
      setBusy(false);
      return;
    }

    // Save succeeded — refresh independently so reload failure is not reported as save failure
    try {
      const refreshed = await getAclForObject(objectGuid);
      setAcl(refreshed);
      const next: Record<string, Set<string>> = {};
      asEntries(refreshed).forEach((e, i) => {
        next[entryKey(e, i)] = new Set(asPermissions(e));
      });
      setSelected(next);
      setNotice(DEV_MSG.ACL_SAVED);
    } catch (err: unknown) {
      // Local state already matches what we saved; still surface reload issue
      setAcl({ ...acl, aclEntries: rebuiltEntries });
      const next: Record<string, Set<string>> = {};
      rebuiltEntries.forEach((e, i) => {
        next[entryKey(e, i)] = new Set(asPermissions(e));
      });
      setSelected(next);
      setNotice(DEV_MSG.ACL_SAVED);
      setError(formatApiErr(DEV_MSG.ACL_RELOAD_ERROR, err));
    } finally {
      setBusy(false);
    }
  }

  if (!objectGuid) {
    return (
      <section style={{ marginBottom: "16px" }} data-testid={`${testIdPrefix}-section`}>
        <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.ACL_TITLE}</h3>
        <p style={{ color: "#718096" }}>{DEV_MSG.ACL_NO_GUID}</p>
      </section>
    );
  }

  return (
    <section style={{ marginBottom: "16px" }} data-testid={`${testIdPrefix}-section`}>
      <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.ACL_TITLE}</h3>
      <p style={{ color: "#4a5568", fontSize: "0.9rem" }}>{DEV_MSG.ACL_HINT}</p>

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
        <p data-testid={`${testIdPrefix}-empty`} style={{ color: "#718096" }}>
          {DEV_MSG.ACL_EMPTY}
        </p>
      ) : null}

      {acl && !loading ? (
        <>
          <div
            style={{ fontFamily: "monospace", color: "#4a5568", fontSize: "0.85rem" }}
            data-testid={`${testIdPrefix}-meta`}
          >
            {acl.name || "ACL"}
            {acl.id != null ? ` · id ${acl.id}` : ""}
            {acl.guid?.stringValue ? ` · ${acl.guid.stringValue}` : ""}
          </div>
          {entries.length === 0 ? (
            <p style={{ color: "#718096" }}>{DEV_MSG.ACL_NO_ENTRIES}</p>
          ) : (
            <div style={{ overflowX: "auto", marginTop: "8px" }}>
              <table
                data-testid={`${testIdPrefix}-table`}
                style={{
                  width: "100%",
                  borderCollapse: "collapse",
                  fontSize: "0.9rem",
                }}
              >
                <thead>
                  <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                    <th style={{ padding: "8px" }}>{DEV_MSG.ACL_COL_ENTRY}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.ACL_COL_TYPE}</th>
                    {ACL_PERMISSIONS.map((p) => (
                      <th
                        key={p}
                        style={{ padding: "8px", textAlign: "center", fontSize: "0.8rem" }}
                      >
                        {p.replace("_", " ")}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {entries.map((e, i) => {
                    const key = entryKey(e, i);
                    const chosen = selected[key] ?? new Set<string>();
                    return (
                      <tr
                        key={key}
                        style={{ borderBottom: "1px solid #edf2f7" }}
                        data-testid={`${testIdPrefix}-row-${key}`}
                      >
                        <td style={{ padding: "8px" }}>{entryLabel(e)}</td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {entryTypeLabel(e)}
                        </td>
                        {ACL_PERMISSIONS.map((p) => (
                          <td key={p} style={{ padding: "8px", textAlign: "center" }}>
                            <input
                              type="checkbox"
                              data-testid={`${testIdPrefix}-perm-${key}-${p}`}
                              checked={chosen.has(p)}
                              disabled={busy}
                              onChange={() => togglePerm(key, p)}
                              aria-label={`${p} for ${entryLabel(e)}`}
                            />
                          </td>
                        ))}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
          {entries.length > 0 ? (
            <div style={{ marginTop: "12px", display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid={`${testIdPrefix}-save`}
                aria-label="Save object ACL"
                disabled={busy || !dirty}
                onClick={() => void handleSave()}
                style={{
                  padding: "8px 16px",
                  background: dirty ? "#007ea8" : "#a0aec0",
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: busy || !dirty ? "not-allowed" : "pointer",
                }}
              >
                {busy ? DEV_MSG.ACL_SAVING : DEV_MSG.ACL_SAVE}
              </button>
            </div>
          ) : null}
        </>
      ) : null}
    </section>
  );
}
