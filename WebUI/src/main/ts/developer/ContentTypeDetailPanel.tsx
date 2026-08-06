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

import React, { useEffect, useState } from "react";
import {
  getContentTypeDetail,
  updateContentTypeDetail,
  type ContentTypeUpdateBody,
} from "../api/developer/contentTypesApi";
import type {
  ContentTypeDetail,
  ContentTypeFieldSummary,
  NamedObjectRef,
} from "../api/developer/types";
import { ObjectAclSection } from "./ObjectAclSection";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { catalogColors, tableHeaderRow, tableRow } from "./catalogStyles";


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

type FieldDraft = {
  name: string;
  searchable: boolean;
  required: boolean;
};

function fieldKey(f: ContentTypeFieldSummary): string {
  return `${f.fieldSet || "parent"}:${f.name || ""}`;
}

function toDrafts(fields: ContentTypeFieldSummary[] | undefined): Record<string, FieldDraft> {
  const out: Record<string, FieldDraft> = {};
  for (const f of fields || []) {
    if (!f.name) continue;
    out[fieldKey(f)] = {
      name: f.name,
      searchable: !!f.searchable,
      required: !!f.required,
    };
  }
  return out;
}

function cloneRefs(list: NamedObjectRef[] | undefined): NamedObjectRef[] {
  return (list || []).map((r) => ({
    name: r.name,
    label: r.label,
    isDefault: r.isDefault,
    guid: r.guid ? { ...r.guid } : undefined,
  }));
}

function refKey(r: NamedObjectRef, index: number): string {
  if (r.name) return `name:${r.name}`;
  if (r.guid?.stringValue) return `guid:${r.guid.stringValue}`;
  if (r.guid?.uuid != null) return `uuid:${r.guid.uuid}`;
  return `idx:${index}`;
}

/** Canonical Percussion GUID shape: type-host-uuid (three numeric groups). */
const PERC_GUID_RE = /^\d+-\d+-\d+$/;

function refsEqual(a: NamedObjectRef[], b: NamedObjectRef[]): boolean {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (refKey(a[i], i) !== refKey(b[i], i)) return false;
    if (!!a[i].isDefault !== !!b[i].isDefault) return false;
  }
  return true;
}

/**
 * Align isDefault flags with server defaultWorkflow (or first row when missing).
 */
function withDefaultFlags(
  list: NamedObjectRef[] | undefined,
  defaultWorkflow?: NamedObjectRef | null,
): NamedObjectRef[] {
  const wfs = cloneRefs(list);
  if (defaultWorkflow) {
    const defKey = refKey(defaultWorkflow, -1);
    for (const w of wfs) {
      w.isDefault = refKey(w, -1) === defKey || w.name === defaultWorkflow.name;
    }
  }
  if (wfs.length > 0 && !wfs.some((w) => w.isDefault)) {
    wfs[0] = { ...wfs[0], isDefault: true };
  }
  return wfs;
}

function toRefPayload(list: NamedObjectRef[]): NamedObjectRef[] {
  return list.map((r) => {
    const out: NamedObjectRef = {};
    if (r.name) out.name = r.name;
    if (r.guid?.stringValue || r.guid?.uuid != null) {
      out.guid = {};
      if (r.guid.stringValue) out.guid.stringValue = r.guid.stringValue;
      if (r.guid.uuid != null) out.guid.uuid = r.guid.uuid;
    }
    if (r.isDefault) out.isDefault = true;
    return out;
  });
}

export function ContentTypeDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<ContentTypeDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [enabled, setEnabled] = useState(true);
  const [fieldDrafts, setFieldDrafts] = useState<Record<string, FieldDraft>>({});
  const [workflows, setWorkflows] = useState<NamedObjectRef[]>([]);
  const [templates, setTemplates] = useState<NamedObjectRef[]>([]);
  const [newWfName, setNewWfName] = useState("");
  const [newTplName, setNewTplName] = useState("");

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    getContentTypeDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setLabel(d.label || "");
        setDescription(d.description || "");
        setEnabled(d.enabled !== false);
        setFieldDrafts(toDrafts(d.fields));
        setWorkflows(withDefaultFlags(d.allowedWorkflows, d.defaultWorkflow));
        setTemplates(cloneRefs(d.allowedTemplates));
        setNewWfName("");
        setNewTplName("");
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.CT_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const initialDrafts = toDrafts(detail?.fields);
  const fieldsDirty =
    detail != null &&
    (detail.fields || []).some((f) => {
      if (!f.name) return false;
      const k = fieldKey(f);
      const d = fieldDrafts[k];
      const i = initialDrafts[k];
      if (!d || !i) return false;
      return d.searchable !== i.searchable || d.required !== i.required;
    });

  const initialWorkflows = withDefaultFlags(detail?.allowedWorkflows, detail?.defaultWorkflow);
  const initialTemplates = cloneRefs(detail?.allowedTemplates);
  const workflowsDirty = detail != null && !refsEqual(workflows, initialWorkflows);
  const templatesDirty = detail != null && !refsEqual(templates, initialTemplates);

  const dirty =
    detail != null &&
    (label !== (detail.label || "") ||
      description !== (detail.description || "") ||
      enabled !== (detail.enabled !== false) ||
      fieldsDirty ||
      workflowsDirty ||
      templatesDirty);

  function toggleField(key: string, prop: "searchable" | "required") {
    setFieldDrafts((prev) => {
      const cur = prev[key];
      if (!cur) return prev;
      return { ...prev, [key]: { ...cur, [prop]: !cur[prop] } };
    });
    setNotice(null);
  }

  function removeWorkflow(index: number) {
    setWorkflows((prev) => {
      const next = prev.filter((_, i) => i !== index);
      if (next.length > 0 && !next.some((w) => w.isDefault)) {
        next[0] = { ...next[0], isDefault: true };
      }
      return next;
    });
    setNotice(null);
  }

  function addWorkflow() {
    const name = newWfName.trim();
    if (!name) return;
    if (workflows.some((w) => (w.name || "").toLowerCase() === name.toLowerCase())) {
      setNewWfName("");
      return;
    }
    setWorkflows((prev) => [
      ...prev,
      { name, label: name, isDefault: prev.length === 0 },
    ]);
    setNewWfName("");
    setNotice(null);
  }

  function setDefaultWorkflow(index: number) {
    setWorkflows((prev) => prev.map((w, i) => ({ ...w, isDefault: i === index })));
    setNotice(null);
  }

  function removeTemplate(index: number) {
    setTemplates((prev) => prev.filter((_, i) => i !== index));
    setNotice(null);
  }

  function addTemplate() {
    const raw = newTplName.trim();
    if (!raw) return;
    const looksLikeGuid = PERC_GUID_RE.test(raw);
    const exists = templates.some((t) => {
      if (looksLikeGuid) return t.guid?.stringValue === raw;
      return (t.name || "").toLowerCase() === raw.toLowerCase();
    });
    if (exists) {
      setNewTplName("");
      return;
    }
    setTemplates((prev) => [
      ...prev,
      looksLikeGuid
        ? { guid: { stringValue: raw }, name: raw, label: raw }
        : { name: raw, label: raw },
    ]);
    setNewTplName("");
    setNotice(null);
  }

  async function handleSave() {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const fieldPatches = Object.values(fieldDrafts)
        .filter((d) => {
          const initial = Object.values(initialDrafts).find((i) => i.name === d.name);
          return (
            !initial ||
            initial.searchable !== d.searchable ||
            initial.required !== d.required
          );
        })
        .map((d) => ({
          name: d.name,
          searchable: d.searchable,
          required: d.required,
        }));

      const body: ContentTypeUpdateBody = {
        label,
        description,
        enabled,
        fields: fieldPatches,
      };
      if (workflowsDirty) {
        body.allowedWorkflows = toRefPayload(workflows);
        const def = workflows.find((w) => w.isDefault) || workflows[0];
        if (def) {
          body.defaultWorkflow = toRefPayload([def])[0];
        }
      }
      if (templatesDirty) {
        body.allowedTemplates = toRefPayload(templates);
      }

      const saved = await updateContentTypeDetail(idOrName, body);
      setDetail(saved);
      setLabel(saved.label || "");
      setDescription(saved.description || "");
      setEnabled(saved.enabled !== false);
      setFieldDrafts(toDrafts(saved.fields));
      setWorkflows(withDefaultFlags(saved.allowedWorkflows, saved.defaultWorkflow));
      setTemplates(cloneRefs(saved.allowedTemplates));
      setNotice(DEV_MSG.CT_SAVED);
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.CT_SAVE_ERROR));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="developer-ct-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-ct-back"
        style={{
          marginBottom: "12px",
          background: "transparent",
          border: `1px solid ${catalogColors.softBorder}`,
          borderRadius: "4px",
          padding: "6px 12px",
          cursor: "pointer",
        }}
      >
        ← {DEV_MSG.CT_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-ct-detail-error" style={{ color: catalogColors.error }}>
          {error}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-ct-detail-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-ct-detail-loading">{DEV_MSG.CT_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-ct-detail-title">
              {label || detail.name || idOrName}
            </h2>
            <div style={{ fontFamily: "monospace", color: catalogColors.muted }}>
              {detail.name}
              {detail.guid?.stringValue ? ` · ${detail.guid.stringValue}` : ""}
            </div>
            <div style={{ marginTop: "12px" }}>
              <label htmlFor="ct-label" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.CT_FORM_LABEL}
              </label>
              <input
                id="ct-label"
                data-testid="developer-ct-label"
                style={inputStyle}
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                disabled={busy}
              />
            </div>
            <div style={{ marginTop: "12px" }}>
              <label htmlFor="ct-desc" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.CT_FORM_DESCRIPTION}
              </label>
              <input
                id="ct-desc"
                data-testid="developer-ct-description"
                style={inputStyle}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                disabled={busy}
              />
            </div>
            <div style={{ marginTop: "12px" }}>
              <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <input
                  type="checkbox"
                  data-testid="developer-ct-enabled"
                  checked={enabled}
                  onChange={() => setEnabled((v) => !v)}
                  disabled={busy}
                />
                {DEV_MSG.CT_FORM_ENABLED}
              </label>
            </div>
            <dl
              style={{
                display: "grid",
                gridTemplateColumns: "auto 1fr",
                gap: "4px 16px",
                marginTop: "12px",
                fontSize: "0.9rem",
              }}
            >
              <dt>{DEV_MSG.CT_META_HIDDEN}</dt>
              <dd style={{ margin: 0 }}>
                {detail.hideFromMenu ? DEV_MSG.YES : DEV_MSG.NO}
              </dd>
              <dt>{DEV_MSG.CT_META_APP}</dt>
              <dd style={{ margin: 0, fontFamily: "monospace" }}>
                {detail.appName || "—"}
              </dd>
            </dl>
          </header>

          {detail.childFieldSets && detail.childFieldSets.length > 0 ? (
            <section style={{ marginBottom: "16px" }}>
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_CHILD_SETS}</h3>
              <ul data-testid="developer-ct-child-sets">
                {detail.childFieldSets.map((n) => (
                  <li key={n} style={{ fontFamily: "monospace" }}>
                    {n}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}

          <section style={{ marginBottom: "16px" }} data-testid="developer-ct-workflows">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_WORKFLOWS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.CT_WORKFLOWS_HINT}</p>
            {workflows.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-ct-wf-empty">
                {DEV_MSG.CT_NONE}
              </p>
            ) : (
              <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                {workflows.map((w, i) => (
                  <li
                    key={refKey(w, i)}
                    data-testid={`developer-ct-wf-row-${i}`}
                    style={{ ...tableRow, display: "flex",
                      alignItems: "center",
                      gap: 12,
                      padding: "6px 0"  }}
                  >
                    <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
                      <input
                        type="radio"
                        name="ct-default-wf"
                        data-testid={`developer-ct-wf-default-${i}`}
                        checked={!!w.isDefault}
                        disabled={busy}
                        onChange={() => setDefaultWorkflow(i)}
                        aria-label={`${DEV_MSG.CT_SET_DEFAULT} ${w.label || w.name}`}
                      />
                      <span style={{ fontSize: "0.85rem", color: catalogColors.muted }}>
                        {DEV_MSG.CT_SET_DEFAULT}
                      </span>
                    </label>
                    <span>
                      {w.label || w.name}
                      {w.name ? (
                        <span
                          style={{
                            fontFamily: "monospace",
                            color: catalogColors.empty,
                            marginLeft: "8px",
                            fontSize: "0.85rem",
                          }}
                        >
                          {w.name}
                        </span>
                      ) : null}
                    </span>
                    <button
                      type="button"
                      data-testid={`developer-ct-wf-remove-${i}`}
                      aria-label={`Remove workflow ${w.name || w.label}`}
                      disabled={busy}
                      onClick={() => removeWorkflow(i)}
                      style={{ ...smallBtnStyle, marginLeft: "auto", cursor: busy ? "not-allowed" : "pointer" }}
                    >
                      {DEV_MSG.CT_ASSOC_REMOVE}
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <div
              style={{
                marginTop: "12px",
                display: "grid",
                gridTemplateColumns: "1fr auto",
                gap: "8px",
                alignItems: "end",
              }}
            >
              <div>
                <label htmlFor="ct-wf-add" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_WORKFLOWS}
                </label>
                <input
                  id="ct-wf-add"
                  data-testid="developer-ct-wf-add-name"
                  style={inputStyle}
                  placeholder={DEV_MSG.CT_WF_NAME_PLACEHOLDER}
                  value={newWfName}
                  onChange={(e) => setNewWfName(e.target.value)}
                  disabled={busy}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      addWorkflow();
                    }
                  }}
                />
              </div>
              <button
                type="button"
                data-testid="developer-ct-wf-add"
                disabled={busy || !newWfName.trim()}
                onClick={addWorkflow}
                style={{
                  ...smallBtnStyle,
                  padding: "8px 12px",
                  cursor: busy || !newWfName.trim() ? "not-allowed" : "pointer",
                }}
              >
                {DEV_MSG.CT_ASSOC_ADD}
              </button>
            </div>
          </section>

          <section style={{ marginBottom: "16px" }} data-testid="developer-ct-templates">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_TEMPLATES}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.CT_TEMPLATES_HINT}</p>
            {templates.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-ct-tpl-empty">
                {DEV_MSG.CT_NONE}
              </p>
            ) : (
              <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                {templates.map((t, i) => (
                  <li
                    key={refKey(t, i)}
                    data-testid={`developer-ct-tpl-row-${i}`}
                    style={{ ...tableRow, display: "flex",
                      alignItems: "center",
                      gap: 12,
                      padding: "6px 0"  }}
                  >
                    <span>
                      {t.label || t.name}
                      {t.name ? (
                        <span
                          style={{
                            fontFamily: "monospace",
                            color: catalogColors.empty,
                            marginLeft: "8px",
                            fontSize: "0.85rem",
                          }}
                        >
                          {t.name}
                        </span>
                      ) : null}
                    </span>
                    <button
                      type="button"
                      data-testid={`developer-ct-tpl-remove-${i}`}
                      aria-label={`Remove template ${t.name || t.label}`}
                      disabled={busy}
                      onClick={() => removeTemplate(i)}
                      style={{ ...smallBtnStyle, marginLeft: "auto", cursor: busy ? "not-allowed" : "pointer" }}
                    >
                      {DEV_MSG.CT_ASSOC_REMOVE}
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <div
              style={{
                marginTop: "12px",
                display: "grid",
                gridTemplateColumns: "1fr auto",
                gap: "8px",
                alignItems: "end",
              }}
            >
              <div>
                <label htmlFor="ct-tpl-add" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_TEMPLATES}
                </label>
                <input
                  id="ct-tpl-add"
                  data-testid="developer-ct-tpl-add-name"
                  style={inputStyle}
                  placeholder={DEV_MSG.CT_TPL_NAME_PLACEHOLDER}
                  value={newTplName}
                  onChange={(e) => setNewTplName(e.target.value)}
                  disabled={busy}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      addTemplate();
                    }
                  }}
                />
              </div>
              <button
                type="button"
                data-testid="developer-ct-tpl-add"
                disabled={busy || !newTplName.trim()}
                onClick={addTemplate}
                style={{
                  ...smallBtnStyle,
                  padding: "8px 12px",
                  cursor: busy || !newTplName.trim() ? "not-allowed" : "pointer",
                }}
              >
                {DEV_MSG.CT_ASSOC_ADD}
              </button>
            </div>
          </section>

          <section style={{ marginBottom: "16px" }}>
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_FIELDS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.CT_FIELDS_HINT}</p>
            <div style={{ overflowX: "auto" }}>
              <table
                data-testid="developer-ct-fields-table"
                style={{
                  width: "100%",
                  borderCollapse: "collapse",
                  fontSize: "0.9rem",
                }}
              >
                <thead>
                  <tr style={tableHeaderRow}>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_FIELD}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_ORIGIN}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_DATATYPE}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_CONTROL}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_REQUIRED}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_READONLY}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_OCCURRENCE}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_RULES}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_SEARCH}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_FIELDSET}</th>
                  </tr>
                </thead>
                <tbody>
                  {(detail.fields || []).map((f) => {
                    const rules: string[] = [];
                    if (f.hasValidation) rules.push(DEV_MSG.CT_RULE_VALIDATION);
                    if (f.hasVisibilityRules) rules.push(DEV_MSG.CT_RULE_VISIBILITY);
                    if (f.hasInputTranslation) rules.push(DEV_MSG.CT_RULE_IN_XFORM);
                    if (f.hasOutputTranslation) rules.push(DEV_MSG.CT_RULE_OUT_XFORM);
                    const k = fieldKey(f);
                    const draft = fieldDrafts[k];
                    const isLocal = (f.fieldType || "").toLowerCase() === "local";
                    return (
                      <tr
                        key={k}
                        data-testid="developer-ct-field-row"
                        style={tableRow}
                      >
                        <td style={{ padding: "8px" }}>
                          <div>{f.label || f.name}</div>
                          <div
                            style={{
                              fontFamily: "monospace",
                              color: catalogColors.empty,
                              fontSize: "0.85rem",
                            }}
                          >
                            {f.name}
                          </div>
                        </td>
                        <td style={{ padding: "8px" }}>{f.fieldType || "—"}</td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.dataType || "—"}
                        </td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.control || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {draft && isLocal ? (
                            <input
                              type="checkbox"
                              data-testid={`developer-ct-field-required-${f.name}`}
                              checked={draft.required}
                              disabled={busy}
                              onChange={() => toggleField(k, "required")}
                              aria-label={`Required ${f.name}`}
                            />
                          ) : f.required ? (
                            DEV_MSG.YES
                          ) : (
                            DEV_MSG.NO
                          )}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {f.readOnly ? DEV_MSG.YES : DEV_MSG.NO}
                        </td>
                        <td
                          style={{ padding: "8px", fontFamily: "monospace" }}
                          data-testid="developer-ct-field-occurrence"
                        >
                          {f.occurrence || "—"}
                        </td>
                        <td
                          style={{ padding: "8px", fontSize: "0.85rem", color: catalogColors.muted }}
                          data-testid="developer-ct-field-rules"
                        >
                          {rules.length > 0 ? rules.join(", ") : "—"}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {draft ? (
                            <input
                              type="checkbox"
                              data-testid={`developer-ct-field-search-${f.name}`}
                              checked={draft.searchable}
                              disabled={busy}
                              onChange={() => toggleField(k, "searchable")}
                              aria-label={`Searchable ${f.name}`}
                            />
                          ) : f.searchable ? (
                            DEV_MSG.YES
                          ) : (
                            DEV_MSG.NO
                          )}
                        </td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.fieldSet || "—"}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          <div style={{ marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-ct-save"
              aria-label="Save content type"
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
              {DEV_MSG.CT_SAVE}
            </button>
          </div>

          <ObjectAclSection
            objectGuid={detail.guid?.stringValue}
            testIdPrefix="developer-ct-acl"
          />

          {detail.designGaps && detail.designGaps.length > 0 ? (
            <section data-testid="developer-ct-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_GAPS}</h3>
              <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {detail.designGaps.map((g) => (
                  <li key={g}>{g}</li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
