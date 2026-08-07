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
  loadDefaultAclTemplate,
  saveDefaultAclTemplate,
} from "../api/developer/preferencesApi";
import { ACL_PERMISSIONS, type AclPermissionName } from "../api/developer/aclApi";
import { isSessionRedirectError, type ApiError } from "../api/client";
import { useSpaBootstrap } from "../app/bootstrap/BootstrapContext";
import { catalogColors, errorAlert } from "./catalogStyles";
import {
  cloneDefaultAclTemplate,
  defaultAclTemplatesEqual,
  DEFAULT_ACL_TEMPLATE_ENTRY_TYPES,
  systemDefaultAclTemplate,
  type DefaultAclTemplate,
  type DefaultAclTemplateEntry,
  type DefaultAclTemplateEntryType,
} from "./defaultAclTemplate";
import { DEV_MSG } from "./messages";

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

type DraftRow = DefaultAclTemplateEntry & { clientKey: string };

function toDraft(template: DefaultAclTemplate): DraftRow[] {
  return (template.entries ?? []).map((e, i) => ({
    name: e.name,
    type: e.type,
    permissions: [...(e.permissions ?? [])],
    clientKey: `row:${i}:${e.name}:${e.type}`,
  }));
}

function fromDraft(rows: DraftRow[]): DefaultAclTemplate {
  return {
    version: 1,
    entries: rows.map((r) => ({
      name: r.name.trim(),
      type: r.type,
      permissions: [...(r.permissions ?? [])],
    })),
  };
}

/**
 * Developer Preferences — Security: default ACL template for new object ACLs
 * (Workbench parity FR §5.9 / §5.4 #7).
 */
export function DeveloperPreferencesPanel(): React.ReactElement {
  const bootstrap = useSpaBootstrap();
  const userName = (bootstrap.userName || "").trim();

  const [baseline, setBaseline] = useState<DefaultAclTemplate>(() =>
    systemDefaultAclTemplate(),
  );
  const [rows, setRows] = useState<DraftRow[]>(() =>
    toDraft(systemDefaultAclTemplate()),
  );
  const [fromPreference, setFromPreference] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [newName, setNewName] = useState("");
  const [newType, setNewType] =
    useState<DefaultAclTemplateEntryType>("ROLE");
  const [newSeq, setNewSeq] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setNotice(null);
    loadDefaultAclTemplate()
      .then((result) => {
        if (cancelled) return;
        setBaseline(cloneDefaultAclTemplate(result.template));
        setRows(toDraft(result.template));
        setFromPreference(result.fromPreference);
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
        const status =
          api && typeof api.status === "number" ? ` (${api.status})` : "";
        const msg = err instanceof Error && err.message ? ` ${err.message}` : "";
        setError(`${DEV_MSG.PREF_ACL_LOAD_ERROR}${status}${msg}`);
        // Keep system default in editor so user can still save once API is up.
        const sys = systemDefaultAclTemplate();
        setBaseline(sys);
        setRows(toDraft(sys));
        setFromPreference(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const draftTemplate = useMemo(() => fromDraft(rows), [rows]);
  const dirty = !defaultAclTemplatesEqual(draftTemplate, baseline);

  function formatApiErr(fallback: string, err: unknown): string {
    if (isSessionRedirectError(err)) return DEV_MSG.SESSION_REDIRECT;
    const api = err as ApiError;
    const status =
      api && typeof api.status === "number" ? ` (${api.status})` : "";
    const msg = err instanceof Error && err.message ? ` ${err.message}` : "";
    return `${fallback}${status}${msg}`;
  }

  function togglePerm(clientKey: string, perm: AclPermissionName) {
    setRows((prev) =>
      prev.map((r) => {
        if (r.clientKey !== clientKey) return r;
        const set = new Set(r.permissions);
        if (set.has(perm)) set.delete(perm);
        else set.add(perm);
        return { ...r, permissions: Array.from(set) };
      }),
    );
    setNotice(null);
  }

  function removeRow(clientKey: string) {
    setRows((prev) => prev.filter((r) => r.clientKey !== clientKey));
    setNotice(null);
  }

  function addRow() {
    const name = newName.trim();
    if (!name) return;
    const dup = rows.some(
      (r) =>
        r.name.trim().toLowerCase() === name.toLowerCase() &&
        r.type === newType,
    );
    if (dup) {
      setError(DEV_MSG.PREF_ACL_ENTRY_DUP);
      return;
    }
    const seq = newSeq + 1;
    setNewSeq(seq);
    setRows((prev) => [
      ...prev,
      {
        clientKey: `__new:${seq}`,
        name,
        type: newType,
        permissions: ["READ"],
      },
    ]);
    setNewName("");
    setError(null);
    setNotice(null);
  }

  function resetToSystemDefault() {
    const sys = systemDefaultAclTemplate();
    setRows(toDraft(sys));
    setError(null);
    setNotice(DEV_MSG.PREF_ACL_RESET_NOTICE);
  }

  async function handleSave() {
    if (!userName) {
      setError(DEV_MSG.PREF_ACL_NO_USER);
      return;
    }
    // Drop blank names before save
    const cleaned = fromDraft(
      rows.filter((r) => r.name.trim().length > 0),
    );
    if (cleaned.entries.length === 0) {
      setError(DEV_MSG.PREF_ACL_EMPTY_TEMPLATE);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await saveDefaultAclTemplate(cleaned, userName);
      setBaseline(cloneDefaultAclTemplate(cleaned));
      setRows(toDraft(cleaned));
      setFromPreference(true);
      setNotice(DEV_MSG.PREF_ACL_SAVED);
    } catch (err: unknown) {
      setError(formatApiErr(DEV_MSG.PREF_ACL_SAVE_ERROR, err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section data-testid="developer-prefs-panel">
      <h2 style={{ fontSize: "1.15rem", marginBottom: "8px" }}>
        {DEV_MSG.PREF_TITLE}
      </h2>
      <p
        style={{ color: catalogColors.muted, maxWidth: "48rem" }}
        data-testid="developer-prefs-intro"
      >
        {DEV_MSG.PREF_INTRO}
      </p>

      <div
        style={{
          marginTop: "20px",
          padding: "16px",
          border: `1px solid ${catalogColors.softBorder}`,
          borderRadius: "6px",
        }}
        data-testid="developer-prefs-security"
      >
        <h3 style={{ fontSize: "1rem", marginTop: 0 }}>
          {DEV_MSG.PREF_SECURITY_TITLE}
        </h3>
        <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
          {DEV_MSG.PREF_ACL_HINT}
        </p>
        <p
          style={{ color: catalogColors.muted, fontSize: "0.85rem" }}
          data-testid="developer-prefs-acl-source"
        >
          {fromPreference
            ? DEV_MSG.PREF_ACL_SOURCE_SAVED
            : DEV_MSG.PREF_ACL_SOURCE_SYSTEM}
        </p>

        {loading ? (
          <div data-testid="developer-prefs-acl-loading">
            {DEV_MSG.PREF_ACL_LOADING}
          </div>
        ) : null}

        {error ? (
          <div
            role="alert"
            data-testid="developer-prefs-acl-error"
            style={errorAlert}
          >
            {error}
          </div>
        ) : null}

        {notice ? (
          <div
            data-testid="developer-prefs-acl-notice"
            style={{ color: "#276749" }}
          >
            {notice}
          </div>
        ) : null}

        {!loading && rows.length === 0 ? (
          <p
            style={{ color: catalogColors.empty }}
            data-testid="developer-prefs-acl-empty"
          >
            {DEV_MSG.PREF_ACL_NO_ENTRIES}
          </p>
        ) : null}

        {!loading && rows.length > 0 ? (
          <div style={{ overflowX: "auto", marginTop: "8px" }}>
            <table
              data-testid="developer-prefs-acl-table"
              style={{
                width: "100%",
                borderCollapse: "collapse",
                fontSize: "0.9rem",
              }}
            >
              <thead>
                <tr
                  style={{
                    borderBottom: `1px solid ${catalogColors.headerBorder}`,
                    textAlign: "left",
                  }}
                >
                  <th style={{ padding: "8px" }}>{DEV_MSG.ACL_COL_ENTRY}</th>
                  <th style={{ padding: "8px" }}>{DEV_MSG.ACL_COL_TYPE}</th>
                  {ACL_PERMISSIONS.map((p) => (
                    <th
                      key={p}
                      style={{
                        padding: "8px",
                        textAlign: "center",
                        fontSize: "0.8rem",
                      }}
                    >
                      {p.replace("_", " ")}
                    </th>
                  ))}
                  <th style={{ padding: "8px" }}>{DEV_MSG.ACL_COL_ACTIONS}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => {
                  const chosen = new Set(r.permissions);
                  return (
                    <tr
                      key={r.clientKey}
                      data-testid={`developer-prefs-acl-row-${r.clientKey}`}
                      style={{
                        borderBottom: `1px solid ${catalogColors.softBorder}`,
                      }}
                    >
                      <td style={{ padding: "8px" }}>
                        <input
                          data-testid={`developer-prefs-acl-name-${r.clientKey}`}
                          style={inputStyle}
                          value={r.name}
                          disabled={busy}
                          onChange={(e) => {
                            const v = e.target.value;
                            setRows((prev) =>
                              prev.map((x) =>
                                x.clientKey === r.clientKey
                                  ? { ...x, name: v }
                                  : x,
                              ),
                            );
                            setNotice(null);
                          }}
                          aria-label={DEV_MSG.ACL_ENTRY_NAME}
                        />
                      </td>
                      <td style={{ padding: "8px" }}>
                        <select
                          data-testid={`developer-prefs-acl-type-${r.clientKey}`}
                          style={{ ...inputStyle, width: "auto", minWidth: 120 }}
                          value={r.type}
                          disabled={busy}
                          onChange={(e) => {
                            const v = e.target
                              .value as DefaultAclTemplateEntryType;
                            setRows((prev) =>
                              prev.map((x) =>
                                x.clientKey === r.clientKey
                                  ? { ...x, type: v }
                                  : x,
                              ),
                            );
                            setNotice(null);
                          }}
                        >
                          {DEFAULT_ACL_TEMPLATE_ENTRY_TYPES.map((t) => (
                            <option key={t} value={t}>
                              {t}
                            </option>
                          ))}
                        </select>
                      </td>
                      {ACL_PERMISSIONS.map((p) => (
                        <td
                          key={p}
                          style={{ padding: "8px", textAlign: "center" }}
                        >
                          <input
                            type="checkbox"
                            data-testid={`developer-prefs-acl-perm-${r.clientKey}-${p}`}
                            checked={chosen.has(p)}
                            disabled={busy}
                            onChange={() => togglePerm(r.clientKey, p)}
                            aria-label={`${p} for ${r.name || "entry"}`}
                          />
                        </td>
                      ))}
                      <td style={{ padding: "8px" }}>
                        <button
                          type="button"
                          data-testid={`developer-prefs-acl-remove-${r.clientKey}`}
                          disabled={busy}
                          onClick={() => removeRow(r.clientKey)}
                          style={{
                            ...smallBtnStyle,
                            cursor: busy ? "not-allowed" : "pointer",
                          }}
                        >
                          {DEV_MSG.ACL_ENTRY_REMOVE}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : null}

        {!loading ? (
          <div
            style={{
              marginTop: "12px",
              display: "grid",
              gridTemplateColumns: "1fr auto auto",
              gap: "8px",
              alignItems: "end",
            }}
            data-testid="developer-prefs-acl-add-form"
          >
            <div>
              <label
                htmlFor="developer-prefs-acl-add-name"
                style={{ display: "block", marginBottom: 4 }}
              >
                {DEV_MSG.ACL_ENTRY_NAME}
              </label>
              <input
                id="developer-prefs-acl-add-name"
                data-testid="developer-prefs-acl-add-name"
                style={inputStyle}
                placeholder={DEV_MSG.ACL_ENTRY_NAME_PLACEHOLDER}
                value={newName}
                disabled={busy}
                onChange={(e) => setNewName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addRow();
                  }
                }}
              />
            </div>
            <div>
              <label
                htmlFor="developer-prefs-acl-add-type"
                style={{ display: "block", marginBottom: 4 }}
              >
                {DEV_MSG.ACL_COL_TYPE}
              </label>
              <select
                id="developer-prefs-acl-add-type"
                data-testid="developer-prefs-acl-add-type"
                style={{ ...inputStyle, width: "auto", minWidth: 120 }}
                value={newType}
                disabled={busy}
                onChange={(e) =>
                  setNewType(e.target.value as DefaultAclTemplateEntryType)
                }
              >
                {DEFAULT_ACL_TEMPLATE_ENTRY_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
            <button
              type="button"
              data-testid="developer-prefs-acl-add"
              disabled={busy || !newName.trim()}
              onClick={addRow}
              style={{
                ...smallBtnStyle,
                padding: "8px 12px",
                cursor: busy || !newName.trim() ? "not-allowed" : "pointer",
              }}
            >
              {DEV_MSG.ACL_ENTRY_ADD}
            </button>
          </div>
        ) : null}

        {!loading ? (
          <div
            style={{
              marginTop: "16px",
              display: "flex",
              flexWrap: "wrap",
              gap: "8px",
            }}
          >
            <button
              type="button"
              data-testid="developer-prefs-acl-save"
              disabled={busy || !dirty || !userName}
              onClick={() => void handleSave()}
              style={{
                padding: "8px 16px",
                background:
                  dirty && userName ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: busy || !dirty || !userName ? "not-allowed" : "pointer",
              }}
            >
              {busy ? DEV_MSG.PREF_ACL_SAVING : DEV_MSG.PREF_ACL_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-prefs-acl-reset"
              disabled={busy}
              onClick={resetToSystemDefault}
              style={{
                ...smallBtnStyle,
                padding: "8px 12px",
                cursor: busy ? "not-allowed" : "pointer",
              }}
            >
              {DEV_MSG.PREF_ACL_RESET}
            </button>
          </div>
        ) : null}
      </div>
    </section>
  );
}
