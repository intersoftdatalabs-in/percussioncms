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
  getCommunityDetail,
  listAvailableRoles,
  updateCommunityRoles,
} from "../api/developer/assemblyApi";
import type {
  CommunityDetail,
  CommunityRoleSummary,
} from "../api/developer/types";
import { backButton, errorAlert, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

function asRoles(detail: CommunityDetail | null): CommunityRoleSummary[] {
  if (!detail?.roleList) return [];
  if (Array.isArray(detail.roleList)) return detail.roleList;
  const env = detail.roleList as { CommunityRole?: CommunityRoleSummary[] };
  if (Array.isArray(env.CommunityRole)) return env.CommunityRole;
  return [];
}

function roleKey(r: CommunityRoleSummary, index = 0): string {
  if (r.roleGuid?.stringValue) return r.roleGuid.stringValue;
  if (r.roleId != null) return `id:${r.roleId}`;
  if (r.roleName) return `name:${r.roleName}`;
  // Stable synthetic key so checkbox rows remain selectable even without ids
  return `role-idx:${index}`;
}

export function CommunityDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<CommunityDetail | null>(null);
  const [allRoles, setAllRoles] = useState<CommunityRoleSummary[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    Promise.all([getCommunityDetail(idOrName), listAvailableRoles()])
      .then(([d, roles]) => {
        if (cancelled) return;
        setDetail(d);
        setAllRoles(roles);
        setSelectedKeys(
          new Set(asRoles(d).map((r, i) => roleKey(r, i)).filter((k) => k.length > 0)),
        );
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.COMM_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const initialKeys = useMemo(
    () =>
      new Set(
        asRoles(detail)
          .map((r, i) => roleKey(r, i))
          .filter((k) => k.length > 0),
      ),
    [detail],
  );
  const dirty =
    selectedKeys.size !== initialKeys.size ||
    [...selectedKeys].some((k) => !initialKeys.has(k));

  function toggle(key: string) {
    setSelectedKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  }

  async function handleSave() {
    setBusy(true);
    setError(null);
    setNotice(null);
    const body = allRoles.filter((r, i) => selectedKeys.has(roleKey(r, i)));
    try {
      const saved = await updateCommunityRoles(idOrName, body);
      setDetail(saved);
      setSelectedKeys(
        new Set(asRoles(saved).map((r, i) => roleKey(r, i)).filter((k) => k.length > 0)),
      );
      setNotice(DEV_MSG.COMM_ROLES_SAVED);
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.COMM_ROLES_SAVE_ERROR));
    } finally {
      setBusy(false);
    }
  }

  const rolesForTable = allRoles.length > 0 ? allRoles : asRoles(detail);

  return (
    <div data-testid="developer-comm-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-comm-back"
        aria-label="Back to communities list"
        style={backButton}
      >
        ← {DEV_MSG.COMM_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-comm-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-comm-detail-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-comm-detail-loading">
          {DEV_MSG.COMM_DETAIL_LOADING}
        </div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-comm-detail-title">
              {detail.label || detail.name || idOrName}
            </h2>
            <div style={{ fontFamily: "monospace", color: "#4a5568" }}>
              {detail.name}
              {detail.id != null ? ` · id ${detail.id}` : ""}
              {detail.guid?.stringValue ? ` · ${detail.guid.stringValue}` : ""}
            </div>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: "#2d3748" }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.COMM_META_ID}</dt>
              <dd style={{ margin: 0, ...monoCell }}>
                {detail.id != null ? String(detail.id) : "—"}
              </dd>
              <dt>{DEV_MSG.COMM_META_GUID}</dt>
              <dd style={{ margin: 0, ...monoCell }}>
                {detail.guid?.stringValue || "—"}
              </dd>
            </dl>
          </header>

          <section style={{ marginBottom: "16px" }} data-testid="developer-comm-roles">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.COMM_ROLES}</h3>
            <p style={{ color: "#4a5568", fontSize: "0.9rem" }}>{DEV_MSG.COMM_ROLES_HINT}</p>
            {rolesForTable.length === 0 ? (
              <p style={{ color: "#718096" }}>
                {allRoles.length === 0 && asRoles(detail).length === 0
                  ? DEV_MSG.COMM_NO_ROLES_SYSTEM
                  : DEV_MSG.COMM_NO_ROLES}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-comm-roles-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.95rem",
                  }}
                >
                  <thead>
                    <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_MEMBER}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_ROLE_NAME}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_ROLE_ID}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rolesForTable.map((r, i) => {
                      const key = roleKey(r, i);
                      const checked = selectedKeys.has(key);
                      return (
                        <tr
                          key={key}
                          style={{ borderBottom: "1px solid #edf2f7" }}
                        >
                          <td style={{ padding: "8px" }}>
                            <input
                              type="checkbox"
                              data-testid={`developer-comm-role-check-${key}`}
                              checked={checked}
                              onChange={() => toggle(key)}
                              aria-label={`Include role ${r.roleName || key}`}
                            />
                          </td>
                          <td style={{ padding: "8px" }}>{r.roleName || "—"}</td>
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>
                            {r.roleId != null ? String(r.roleId) : "—"}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
            <div style={{ marginTop: "12px", display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-comm-roles-save"
                aria-label="Save community roles"
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
                {DEV_MSG.COMM_ROLES_SAVE}
              </button>
            </div>
          </section>

          <section data-testid="developer-comm-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.COMM_GAPS}</h3>
            <ul style={{ color: "#4a5568", fontSize: "0.9rem" }}>
              <li>{DEV_MSG.COMM_GAP_ACL}</li>
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
