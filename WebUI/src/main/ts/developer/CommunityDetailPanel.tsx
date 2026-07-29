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
import { getCommunityDetail } from "../api/developer/assemblyApi";
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

export function CommunityDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<CommunityDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getCommunityDetail(idOrName)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.COMM_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const roles = asRoles(detail);

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
            {roles.length === 0 ? (
              <p style={{ color: "#718096" }}>{DEV_MSG.COMM_NO_ROLES}</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.95rem",
                  }}
                >
                  <thead>
                    <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_ROLE_NAME}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_ROLE_ID}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_ROLE_GUID}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roles.map((r, i) => (
                      <tr
                        key={
                          r.roleGuid?.stringValue ||
                          (r.roleId != null ? `role-${r.roleId}` : `role-idx-${i}`)
                        }
                        style={{ borderBottom: "1px solid #edf2f7" }}
                      >
                        <td style={{ padding: "8px" }}>{r.roleName || "—"}</td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {r.roleId != null ? String(r.roleId) : "—"}
                        </td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {r.roleGuid?.stringValue || "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section data-testid="developer-comm-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.COMM_GAPS}</h3>
            <ul style={{ color: "#4a5568", fontSize: "0.9rem" }}>
              <li>{DEV_MSG.COMM_GAP_ROLES_EDIT}</li>
              <li>{DEV_MSG.COMM_GAP_ACL}</li>
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
