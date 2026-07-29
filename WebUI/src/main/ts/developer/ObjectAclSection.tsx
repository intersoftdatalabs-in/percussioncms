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

import React, { useEffect, useState } from "react";
import { getAclForObject } from "../api/developer/aclApi";
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

function entryLabel(e: ObjectAclEntry): string {
  return (
    e.name ||
    e.principal?.name ||
    e.type?.name ||
    (e.id != null ? `entry-${e.id}` : "—")
  );
}

/**
 * Read-only object ACL viewer for Developer detail panels (SE-04 read path).
 * Edit/save remains a later slice.
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
  const [loading, setLoading] = useState(false);
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    if (!objectGuid) {
      setAcl(null);
      setError(null);
      setMissing(false);
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    setMissing(false);
    setAcl(null);
    getAclForObject(objectGuid)
      .then((a) => {
        if (cancelled) return;
        setAcl(a);
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

  if (!objectGuid) {
    return (
      <section style={{ marginBottom: "16px" }} data-testid={`${testIdPrefix}-section`}>
        <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.ACL_TITLE}</h3>
        <p style={{ color: "#718096" }}>{DEV_MSG.ACL_NO_GUID}</p>
      </section>
    );
  }

  const entries = asEntries(acl);

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
                  fontSize: "0.95rem",
                }}
              >
                <thead>
                  <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                    <th style={{ padding: "8px" }}>{DEV_MSG.ACL_COL_ENTRY}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.ACL_COL_TYPE}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.ACL_COL_PERMS}</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.map((e, i) => (
                    <tr
                      key={e.id ?? `${entryLabel(e)}-${i}`}
                      style={{ borderBottom: "1px solid #edf2f7" }}
                    >
                      <td style={{ padding: "8px" }}>{entryLabel(e)}</td>
                      <td style={{ padding: "8px", fontFamily: "monospace" }}>
                        {e.type?.type || e.type?.name || e.principal?.type || "—"}
                      </td>
                      <td style={{ padding: "8px", fontFamily: "monospace" }}>
                        {asPermissions(e).join(", ") || "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      ) : null}
    </section>
  );
}
