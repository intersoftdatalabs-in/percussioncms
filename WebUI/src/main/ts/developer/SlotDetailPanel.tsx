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
import { getSlotDetail, updateSlotDetail } from "../api/developer/assemblyApi";
import {
  normalizeSlotAssociations,
  normalizeSlotDesignGaps,
  normalizeSlotStringMap,
} from "../api/developer/slotLists";
import {
  designGapCode,
  designGapKey,
  formatDesignGap,
  type DesignGapWire,
} from "../api/developer/designGaps";
import type { SlotAssociationSummary, SlotDetail } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
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
    // Indexed access may be undefined under noUncheckedIndexedAccess.
    const left = a[i];
    const right = b[i];
    if (!left || !right) return false;
    const as =
      left.contentTypeGuid?.stringValue ||
      (left.contentTypeGuid?.uuid != null
        ? String(left.contentTypeGuid.uuid)
        : "");
    const at =
      left.templateGuid?.stringValue ||
      (left.templateGuid?.uuid != null ? String(left.templateGuid.uuid) : "");
    const bs =
      right.contentTypeGuid?.stringValue ||
      (right.contentTypeGuid?.uuid != null
        ? String(right.contentTypeGuid.uuid)
        : "");
    const bt =
      right.templateGuid?.stringValue ||
      (right.templateGuid?.uuid != null ? String(right.templateGuid.uuid) : "");
    if (as !== bs || at !== bt) return false;
  }
  return true;
}

function cloneAssociations(list: unknown): SlotAssociationSummary[] {
  return normalizeSlotAssociations(list).map((a) => ({
    contentTypeGuid: a.contentTypeGuid ? { ...a.contentTypeGuid } : undefined,
    templateGuid: a.templateGuid ? { ...a.templateGuid } : undefined,
  }));
}

function slotDesignGaps(list: unknown): DesignGapWire[] {
  return normalizeSlotDesignGaps(list);
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
        const associations = cloneAssociations(d.associations);
        const designGaps = slotDesignGaps(d.designGaps);
        const finderArguments = normalizeSlotStringMap(d.finderArguments);
        setDetail({ ...d, associations, designGaps, finderArguments });
        setLabel(d.label || "");
        setDescription(d.description || "");
        setAssociations(associations);
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
      const savedAssocs = cloneAssociations(saved.associations);
      const savedGaps = slotDesignGaps(saved.designGaps);
      const savedArgs = normalizeSlotStringMap(saved.finderArguments);
      setDetail({
        ...saved,
        associations: savedAssocs,
        designGaps: savedGaps,
        finderArguments: savedArgs,
      });
      setLabel(saved.label || "");
      setDescription(saved.description || "");
      setAssociations(savedAssocs);
      setNotice(DEV_MSG.SLOT_SAVED);
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.SLOT_SAVE_ERROR));
    } finally {
      setBusy(false);
    }
  }

  const argEntries = Object.entries(normalizeSlotStringMap(detail?.finderArguments));
  const gaps = slotDesignGaps(detail?.designGaps);

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
            <div style={{ fontFamily: "monospace", color: catalogColors.muted }}>
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
              <p style={{ color: catalogColors.empty }}>{DEV_MSG.SLOT_NONE}</p>
            ) : (
              <ul>
                {argEntries.map(([k, v]) => (
                  <li key={k}>
                    <span style={{ fontFamily: "monospace" }}>{k}</span>
                    {" = "}
                    <span style={{ fontFamily: "monospace", color: catalogColors.muted }}>
                      {typeof v === "string" ? v : String(v ?? "")}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section style={{ marginBottom: "16px" }} data-testid="developer-slot-associations">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SLOT_ASSOCIATIONS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.SLOT_ASSOCIATIONS_HINT}</p>
            {associations.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-slot-assoc-empty">
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
                    <tr style={tableHeaderRow}>
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
                          style={tableRow}
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
                                border: `1px solid ${catalogColors.softBorder}`,
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
                  background: newCtGuid.trim() && newTplGuid.trim() ? catalogColors.accent : catalogColors.disabled,
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
                background: dirty ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: busy || !dirty ? "not-allowed" : "pointer",
              }}
            >
              {DEV_MSG.SLOT_SAVE}
            </button>
          </div>

          {gaps.length > 0 ? (
            <section data-testid="developer-slot-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SLOT_GAPS}</h3>
              <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {gaps.map((g, i) => (
                  <li key={designGapKey(g, i)} data-gap-code={designGapCode(g)}>
                    {formatDesignGap(g)}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
