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
import { getSlotDetail, updateSlotDetail } from "../api/developer/assemblyApi";
import type { SlotAssociationSummary, SlotDetail } from "../api/developer/types";
import { backButton, errorAlert, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: "1px solid #cbd5e0",
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

function guidKey(g: { stringValue?: string; uuid?: number } | undefined, fallback: string): string {
  if (g?.stringValue) return g.stringValue;
  if (g?.uuid != null) return String(g.uuid);
  return fallback;
}

function assocKey(a: SlotAssociationSummary, index: number): string {
  return `${guidKey(a.contentTypeGuid, "ct")}:${guidKey(a.templateGuid, "tpl")}:${index}`;
}

function associationsEqual(
  a: SlotAssociationSummary[],
  b: SlotAssociationSummary[],
): boolean {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    const as =
      a[i].contentTypeGuid?.stringValue ||
      (a[i].contentTypeGuid?.uuid != null ? String(a[i].contentTypeGuid.uuid) : "");
    const at =
      a[i].templateGuid?.stringValue ||
      (a[i].templateGuid?.uuid != null ? String(a[i].templateGuid.uuid) : "");
    const bs =
      b[i].contentTypeGuid?.stringValue ||
      (b[i].contentTypeGuid?.uuid != null ? String(b[i].contentTypeGuid.uuid) : "");
    const bt =
      b[i].templateGuid?.stringValue ||
      (b[i].templateGuid?.uuid != null ? String(b[i].templateGuid.uuid) : "");
    if (as !== bs || at !== bt) return false;
  }
  return true;
}

function cloneAssociations(list: SlotAssociationSummary[] | undefined): SlotAssociationSummary[] {
  return (list || []).map((a) => ({
    contentTypeGuid: a.contentTypeGuid ? { ...a.contentTypeGuid } : undefined,
    templateGuid: a.templateGuid ? { ...a.templateGuid } : undefined,
  }));
}

export function SlotDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<SlotDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [associations, setAssociations] = useState<SlotAssociationSummary[]>([]);
  const [newCtGuid, setNewCtGuid] = useState("");
  const [newTplGuid, setNewTplGuid] = useState("");

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setNewCtGuid("");
    setNewTplGuid("");
    getSlotDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setLabel(d.label || "");
        setDescription(d.description || "");
        setAssociations(cloneAssociations(d.associations));
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.SLOT_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const initialAssocs = cloneAssociations(detail?.associations);
  const dirty =
    detail != null &&
    (label !== (detail.label || "") ||
      description !== (detail.description || "") ||
      !associationsEqual(associations, initialAssocs));

  function removeAssociation(index: number) {
    setAssociations((prev) => prev.filter((_, i) => i !== index));
    setNotice(null);
  }

  function addAssociation() {
    const ct = newCtGuid.trim();
    const tpl = newTplGuid.trim();
    if (!ct || !tpl) return;
    setAssociations((prev) => [
      ...prev,
      {
        contentTypeGuid: { stringValue: ct },
        templateGuid: { stringValue: tpl },
      },
    ]);
    setNewCtGuid("");
    setNewTplGuid("");
    setNotice(null);
  }

  async function handleSave() {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await updateSlotDetail(idOrName, {
        label,
        description,
        associations: associations.map((a) => ({
          contentTypeGuid: a.contentTypeGuid?.stringValue
            ? { stringValue: a.contentTypeGuid.stringValue }
            : a.contentTypeGuid,
          templateGuid: a.templateGuid?.stringValue
            ? { stringValue: a.templateGuid.stringValue }
            : a.templateGuid,
        })),
      });
      setDetail(saved);
      setLabel(saved.label || "");
      setDescription(saved.description || "");
      setAssociations(cloneAssociations(saved.associations));
      setNotice(DEV_MSG.SLOT_SAVED);
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.SLOT_SAVE_ERROR));
    } finally {
      setBusy(false);
    }
  }

  const argEntries = Object.entries(detail?.finderArguments || {});

  return (
    <div data-testid="developer-slot-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-slot-back"
        aria-label="Back to slots list"
        style={backButton}
      >
        ← {DEV_MSG.SLOT_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-slot-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-slot-detail-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-slot-detail-loading">{DEV_MSG.SLOT_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-slot-detail-title">
              {label || detail.name || idOrName}
            </h2>
            <div style={{ fontFamily: "monospace", color: "#4a5568" }}>
              {detail.name}
              {detail.guid?.stringValue ? ` · ${detail.guid.stringValue}` : ""}
            </div>
            <div style={{ marginTop: "12px" }}>
              <label htmlFor="slot-label" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.SLOT_FORM_LABEL}
              </label>
              <input
                id="slot-label"
                data-testid="developer-slot-label"
                style={inputStyle}
                value={label}
                onChange={(e) => setLabel(e.target.value)}
              />
            </div>
            <div style={{ marginTop: "12px" }}>
              <label htmlFor="slot-desc" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.SLOT_FORM_DESCRIPTION}
              </label>
              <input
                id="slot-desc"
                data-testid="developer-slot-description"
                style={inputStyle}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <dl style={metaGrid}>
              <dt>{DEV_MSG.SLOT_META_TYPE}</dt>
              <dd style={{ margin: 0 }}>{detail.slotType || "—"}</dd>
              <dt>{DEV_MSG.SLOT_META_SYSTEM}</dt>
              <dd style={{ margin: 0 }}>
                {detail.systemSlot ? DEV_MSG.YES : DEV_MSG.NO}
              </dd>
              <dt>{DEV_MSG.SLOT_META_FINDER}</dt>
              <dd style={{ margin: 0, ...monoCell }}>
                {detail.finderName || "—"}
              </dd>
              <dt>{DEV_MSG.SLOT_META_RELATIONSHIP}</dt>
              <dd style={{ margin: 0, ...monoCell }}>
                {detail.relationshipName || "—"}
              </dd>
            </dl>
          </header>

          <section style={{ marginBottom: "16px" }} data-testid="developer-slot-args">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SLOT_ARGS}</h3>
            {argEntries.length === 0 ? (
              <p style={{ color: "#718096" }}>{DEV_MSG.SLOT_NONE}</p>
            ) : (
              <ul>
                {argEntries.map(([k, v]) => (
                  <li key={k}>
                    <span style={{ fontFamily: "monospace" }}>{k}</span>
                    {" = "}
                    <span style={{ fontFamily: "monospace", color: "#4a5568" }}>{v}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section style={{ marginBottom: "16px" }} data-testid="developer-slot-associations">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SLOT_ASSOCIATIONS}</h3>
            <p style={{ color: "#4a5568", fontSize: "0.9rem" }}>{DEV_MSG.SLOT_ASSOCIATIONS_HINT}</p>
            {associations.length === 0 ? (
              <p style={{ color: "#718096" }} data-testid="developer-slot-assoc-empty">
                {DEV_MSG.SLOT_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-slot-assoc-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.95rem",
                  }}
                >
                  <thead>
                    <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SLOT_COL_CT}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SLOT_COL_TPL}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SLOT_COL_ACTIONS}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {associations.map((a, i) => {
                      const ctDisplay =
                        a.contentTypeGuid?.stringValue ||
                        (a.contentTypeGuid?.uuid != null
                          ? String(a.contentTypeGuid.uuid)
                          : "—");
                      const tplDisplay =
                        a.templateGuid?.stringValue ||
                        (a.templateGuid?.uuid != null
                          ? String(a.templateGuid.uuid)
                          : "—");
                      return (
                        <tr
                          key={assocKey(a, i)}
                          style={{ borderBottom: "1px solid #edf2f7" }}
                          data-testid={`developer-slot-assoc-row-${i}`}
                        >
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>{ctDisplay}</td>
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>{tplDisplay}</td>
                          <td style={{ padding: "8px" }}>
                            <button
                              type="button"
                              data-testid={`developer-slot-assoc-remove-${i}`}
                              aria-label={`Remove association ${ctDisplay} / ${tplDisplay}`}
                              disabled={busy}
                              onClick={() => removeAssociation(i)}
                              style={{
                                background: "transparent",
                                border: "1px solid #cbd5e0",
                                borderRadius: "4px",
                                padding: "4px 8px",
                                cursor: busy ? "not-allowed" : "pointer",
                              }}
                            >
                              {DEV_MSG.SLOT_ASSOC_REMOVE}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
            <div
              style={{
                marginTop: "12px",
                display: "grid",
                gridTemplateColumns: "1fr 1fr auto",
                gap: "8px",
                alignItems: "end",
              }}
            >
              <div>
                <label htmlFor="slot-assoc-ct" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.SLOT_COL_CT}
                </label>
                <input
                  id="slot-assoc-ct"
                  data-testid="developer-slot-assoc-ct"
                  style={inputStyle}
                  placeholder={DEV_MSG.SLOT_ASSOC_CT_PLACEHOLDER}
                  value={newCtGuid}
                  onChange={(e) => setNewCtGuid(e.target.value)}
                  disabled={busy}
                />
              </div>
              <div>
                <label htmlFor="slot-assoc-tpl" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.SLOT_COL_TPL}
                </label>
                <input
                  id="slot-assoc-tpl"
                  data-testid="developer-slot-assoc-tpl"
                  style={inputStyle}
                  placeholder={DEV_MSG.SLOT_ASSOC_TPL_PLACEHOLDER}
                  value={newTplGuid}
                  onChange={(e) => setNewTplGuid(e.target.value)}
                  disabled={busy}
                />
              </div>
              <button
                type="button"
                data-testid="developer-slot-assoc-add"
                aria-label="Add slot association"
                disabled={busy || !newCtGuid.trim() || !newTplGuid.trim()}
                onClick={addAssociation}
                style={{
                  padding: "8px 12px",
                  background: newCtGuid.trim() && newTplGuid.trim() ? "#007ea8" : "#a0aec0",
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor:
                    busy || !newCtGuid.trim() || !newTplGuid.trim()
                      ? "not-allowed"
                      : "pointer",
                  whiteSpace: "nowrap",
                }}
              >
                {DEV_MSG.SLOT_ASSOC_ADD}
              </button>
            </div>
          </section>

          <div style={{ marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-slot-save"
              aria-label="Save slot"
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
              {DEV_MSG.SLOT_SAVE}
            </button>
          </div>

          {(detail.designGaps || []).length > 0 ? (
            <section data-testid="developer-slot-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SLOT_GAPS}</h3>
              <ul style={{ color: "#4a5568", fontSize: "0.9rem" }}>
                {(detail.designGaps || []).map((g) => (
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
