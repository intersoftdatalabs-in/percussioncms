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

import React, { useEffect, useRef, useState } from "react";
import { captureDialogOpener } from "../architecture/useDialogEscape";
import { extractRestErrorMessage, isApiError } from "../api/client";
import {
  createSlot,
  deleteSlot,
  getSlotDetail,
  isSlotCreateReady,
  lockSlot,
  unlockSlot,
  updateSlotDetail,
} from "../api/developer/assemblyApi";
import {
  buildSlotUpdateBody,
  normalizeSlotAssociations,
  normalizeSlotDesignGaps,
  normalizeSlotStringMap,
  slotFinderWriteRequested,
} from "../api/developer/slotLists";
import {
  designGapCode,
  designGapKey,
  formatDesignGap,
  type DesignGapWire,
} from "../api/developer/designGaps";
import type { SlotAssociationSummary, SlotDetail } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
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

type FinderArgRow = { key: string; value: string };

function argsFromMap(map: Record<string, string>): FinderArgRow[] {
  return Object.entries(map).map(([key, value]) => ({ key, value }));
}

function mapFromArgRows(rows: FinderArgRow[]): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    const k = row.key.trim();
    if (!k) {
      continue;
    }
    out[k] = row.value;
  }
  return out;
}

function revertFinderFromDetail(detail: SlotDetail | null): {
  finderName: string;
  relationshipName: string;
  argRows: FinderArgRow[];
} {
  return {
    finderName: detail?.finderName || "",
    relationshipName: detail?.relationshipName || "",
    argRows: argsFromMap(normalizeSlotStringMap(detail?.finderArguments)),
  };
}

function createSaveFallback(err: unknown): string {
  if (!isApiError(err)) return DEV_MSG.SLOT_SAVE_ERROR;
  if (err.status === 409) return DEV_MSG.SLOT_DUPLICATE;
  if (err.status === 403) return DEV_MSG.SLOT_FORBIDDEN;
  if (err.status === 400) {
    const msg = extractRestErrorMessage(err.body) || "";
    if (/slotType.*(invalid|must be|REGULAR|INLINE)/i.test(msg)) {
      return DEV_MSG.SLOT_TYPE_INVALID;
    }
    return DEV_MSG.SLOT_NAME_INVALID;
  }
  return DEV_MSG.SLOT_SAVE_ERROR;
}

function deleteFallback(err: unknown, systemSlot: boolean | undefined): string {
  if (!isApiError(err)) return DEV_MSG.SLOT_DELETE_ERROR;
  if (err.status === 403) return DEV_MSG.SLOT_FORBIDDEN;
  if (err.status === 409) {
    const msg = extractRestErrorMessage(err.body) || "";
    if (systemSlot || /system.?slot/i.test(msg)) return DEV_MSG.SLOT_DELETE_SYSTEM;
  }
  return DEV_MSG.SLOT_DELETE_ERROR;
}

function updateSaveFallback(err: unknown): string {
  if (!isApiError(err)) return DEV_MSG.SLOT_SAVE_ERROR;
  if (err.status === 403) return DEV_MSG.SLOT_FORBIDDEN;
  if (err.status === 409) return DEV_MSG.SLOT_LOCK_REQUIRED;
  if (err.status === 400) {
    const msg = extractRestErrorMessage(err.body) || "";
    if (/invalid finder|extension name not valid/i.test(msg)) {
      return DEV_MSG.SLOT_FINDER_INVALID;
    }
    if (/unknown relationship/i.test(msg)) return DEV_MSG.SLOT_RELATIONSHIP_INVALID;
  }
  return DEV_MSG.SLOT_SAVE_ERROR;
}

export function SlotDetailPanel({
  idOrName,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  idOrName: string | null;
  onBack: () => void;
  onSaved?: (detail: SlotDetail) => void;
  onDeleted?: () => void;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrName == null && createdKey == null;
  const [detail, setDetail] = useState<SlotDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [loading, setLoading] = useState(idOrName != null);
  const [name, setName] = useState("");
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [slotType, setSlotType] = useState("REGULAR");
  const [associations, setAssociations] = useState<SlotAssociationSummary[]>([]);
  const [newCtGuid, setNewCtGuid] = useState("");
  const [newTplGuid, setNewTplGuid] = useState("");
  const [finderName, setFinderName] = useState("");
  const [relationshipName, setRelationshipName] = useState("");
  const [argRows, setArgRows] = useState<FinderArgRow[]>([]);
  const [newArgKey, setNewArgKey] = useState("");
  const [newArgValue, setNewArgValue] = useState("");
  const [heldLock, setHeldLock] = useState(false);
  const inflight = useRef(false);
  const heldLockRef = useRef(false);

  useEffect(() => {
    if (idOrName == null) {
      return;
    }
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setNewCtGuid("");
    setNewTplGuid("");
    setNewArgKey("");
    setNewArgValue("");
    heldLockRef.current = false;
    setHeldLock(false);
    setLoading(true);
    getSlotDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        const associations = cloneAssociations(d.associations);
        const designGaps = slotDesignGaps(d.designGaps);
        const finderArguments = normalizeSlotStringMap(d.finderArguments);
        setDetail({ ...d, associations, designGaps, finderArguments });
        setName(d.name || idOrName);
        setLabel(d.label || "");
        setDescription(d.description || "");
        setSlotType((d.slotType || "REGULAR").toUpperCase());
        setAssociations(associations);
        setFinderName(d.finderName || "");
        setRelationshipName(d.relationshipName || "");
        setArgRows(argsFromMap(finderArguments));
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.SLOT_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const writeKey = idOrName || createdKey || name.trim();
  const initialAssocs = cloneAssociations(detail?.associations);
  const finderArgs = mapFromArgRows(argRows);
  const putPreview =
    detail != null
      ? buildSlotUpdateBody({
          label,
          description,
          associations,
          finderName,
          relationshipName,
          finderArguments: finderArgs,
          initial: detail,
        })
      : null;
  const finderDirty = putPreview != null && slotFinderWriteRequested(putPreview);
  const dirty =
    detail != null &&
    (label !== (detail.label || "") ||
      description !== (detail.description || "") ||
      !associationsEqual(associations, initialAssocs) ||
      finderDirty);
  const canSave = isNew
    ? !busy && isSlotCreateReady({ name, slotType })
    : !busy && dirty;

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

  function addFinderArg() {
    const k = newArgKey.trim();
    if (!k || !heldLock) return;
    setArgRows((prev) => [...prev.filter((r) => r.key.trim() !== k), { key: k, value: newArgValue }]);
    setNewArgKey("");
    setNewArgValue("");
    setNotice(null);
  }

  function removeFinderArg(index: number) {
    if (!heldLock) return;
    setArgRows((prev) => prev.filter((_, i) => i !== index));
    setNotice(null);
  }

  function applyFinderFromDetail(next: SlotDetail | null) {
    const restored = revertFinderFromDetail(next);
    setFinderName(restored.finderName);
    setRelationshipName(restored.relationshipName);
    setArgRows(restored.argRows);
  }

  async function handleLock() {
    if (isNew || !writeKey || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await lockSlot(writeKey);
      heldLockRef.current = true;
      setHeldLock(true);
      setNotice(DEV_MSG.SLOT_LOCKED);
    } catch (err: unknown) {
      heldLockRef.current = false;
      setHeldLock(false);
      setError(panelErrMsg(err, DEV_MSG.SLOT_LOCK_ERROR));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  async function handleUnlock() {
    if (isNew || !writeKey || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await unlockSlot(writeKey);
      heldLockRef.current = false;
      setHeldLock(false);
      applyFinderFromDetail(detail);
      setNewArgKey("");
      setNewArgValue("");
      setNotice(DEV_MSG.SLOT_UNLOCKED_NOTICE);
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.SLOT_UNLOCK_ERROR));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  async function handleBack() {
    if (heldLockRef.current && writeKey) {
      try {
        await unlockSlot(writeKey);
      } catch {
        // Best-effort release so Back cannot trap the operator on a stale lock.
      }
      heldLockRef.current = false;
      setHeldLock(false);
    }
    onBack();
  }

  async function handleSave() {
    if (!canSave || inflight.current) return;
    if (!isNew && finderDirty && !heldLock) {
      setError(DEV_MSG.SLOT_LOCK_REQUIRED);
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = isNew
        ? await createSlot({
            name: name.trim(),
            label: label.trim() || undefined,
            description: description.trim() || undefined,
            slotType: slotType.trim() || undefined,
          })
        : await updateSlotDetail(writeKey, putPreview as NonNullable<typeof putPreview>);
      const savedAssocs = cloneAssociations(saved.associations);
      const savedGaps = slotDesignGaps(saved.designGaps);
      const savedArgs = normalizeSlotStringMap(saved.finderArguments);
      const nextDetail = {
        ...saved,
        associations: savedAssocs,
        designGaps: savedGaps,
        finderArguments: savedArgs,
      };
      setDetail(nextDetail);
      if (isNew) {
        setCreatedKey(saved.name || name.trim());
      }
      setName(saved.name || name);
      setLabel(saved.label || label);
      setDescription(saved.description || "");
      setSlotType((saved.slotType || slotType || "REGULAR").toUpperCase());
      setAssociations(savedAssocs);
      applyFinderFromDetail(nextDetail);
      setNotice(DEV_MSG.SLOT_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      const fallback = isNew ? createSaveFallback(err) : updateSaveFallback(err);
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (isNew || !writeKey || inflight.current) return;
    captureDialogOpener(ev.currentTarget);
    setConfirmOpen(true);
  }

  async function handleDelete() {
    if (isNew || !writeKey || inflight.current) return;
    setConfirmOpen(false);
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteSlot(writeKey);
      setNotice(DEV_MSG.SLOT_DELETED);
      onDeleted?.();
    } catch (err: unknown) {
      setError(panelErrMsg(err, deleteFallback(err, detail?.systemSlot)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const gaps = slotDesignGaps(detail?.designGaps);
  const finderEditable = !isNew && heldLock && !busy;
  const title = isNew
    ? DEV_MSG.SLOT_NEW
    : label || detail?.name || idOrName || DEV_MSG.SLOT_NEW;
  const showForm = isNew || (!loading && detail != null);

  return (
    <div data-testid="developer-slot-detail">
      <button
        type="button"
        onClick={() => void handleBack()}
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

      {!isNew && (detail != null || loading) ? (
        <div
          role="toolbar"
          aria-label={DEV_MSG.SLOT_LOCK_TOOLBAR}
          data-testid="developer-slot-lock-toolbar"
          style={{
            marginBottom: "16px",
            display: "flex",
            flexWrap: "wrap",
            gap: "8px",
            alignItems: "center",
          }}
        >
          <p style={{ margin: 0, width: "100%", color: catalogColors.muted, fontSize: "0.9rem" }}>
            {DEV_MSG.SLOT_LOCK_HINT}
          </p>
          <div
            role="status"
            aria-live="polite"
            data-testid="developer-slot-lock-status"
            style={{ marginRight: "8px", fontSize: "0.9rem" }}
          >
            {heldLock ? DEV_MSG.SLOT_LOCKED : DEV_MSG.SLOT_UNLOCKED}
          </div>
          <button
            type="button"
            data-testid="developer-slot-lock"
            aria-label={DEV_MSG.SLOT_LOCK}
            disabled={busy || heldLock || detail == null}
            onClick={() => void handleLock()}
            style={{
              padding: "8px 16px",
              background: heldLock ? catalogColors.disabled : catalogColors.accent,
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: busy || heldLock || detail == null ? "not-allowed" : "pointer",
            }}
          >
            {DEV_MSG.SLOT_LOCK}
          </button>
          <button
            type="button"
            data-testid="developer-slot-unlock"
            aria-label={DEV_MSG.SLOT_UNLOCK}
            disabled={busy || !heldLock}
            onClick={() => void handleUnlock()}
            style={{
              padding: "8px 16px",
              background: "transparent",
              color: "inherit",
              border: `1px solid ${catalogColors.softBorder}`,
              borderRadius: "4px",
              cursor: busy || !heldLock ? "not-allowed" : "pointer",
            }}
          >
            {DEV_MSG.SLOT_UNLOCK}
          </button>
        </div>
      ) : null}

      {loading && !isNew ? (
        <div data-testid="developer-slot-detail-loading">{DEV_MSG.SLOT_DETAIL_LOADING}</div>
      ) : null}

      {showForm ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-slot-detail-title">
              {title}
            </h2>
            {!isNew && detail ? (
              <div style={{ fontFamily: "monospace", color: catalogColors.muted }}>
                {detail.name}
                {detail.guid?.stringValue ? ` · ${detail.guid.stringValue}` : ""}
              </div>
            ) : null}
            <div style={{ marginTop: "12px" }}>
              <label htmlFor="slot-name" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.SLOT_FORM_NAME}
              </label>
              <input
                id="slot-name"
                data-testid="developer-slot-name"
                style={{ ...inputStyle, fontFamily: "monospace" }}
                value={name}
                disabled={!isNew || busy}
                onChange={(e) => setName(e.target.value)}
                autoComplete="off"
              />
              {!isNew ? (
                <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                  {DEV_MSG.SLOT_NAME_READONLY}
                </span>
              ) : null}
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
                disabled={busy}
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
                disabled={busy}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            {isNew ? (
              <div style={{ marginTop: "12px" }}>
                <label htmlFor="slot-type" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.SLOT_FORM_TYPE}
                </label>
                <select
                  id="slot-type"
                  data-testid="developer-slot-type"
                  style={inputStyle}
                  value={slotType}
                  disabled={busy}
                  onChange={(e) => setSlotType(e.target.value)}
                >
                  <option value="REGULAR">{DEV_MSG.SLOT_TYPE_REGULAR}</option>
                  <option value="INLINE">{DEV_MSG.SLOT_TYPE_INLINE}</option>
                </select>
              </div>
            ) : (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.SLOT_META_TYPE}</dt>
                <dd style={{ margin: 0 }}>{detail?.slotType || "—"}</dd>
                <dt>{DEV_MSG.SLOT_META_SYSTEM}</dt>
                <dd style={{ margin: 0 }}>
                  {detail?.systemSlot ? DEV_MSG.YES : DEV_MSG.NO}
                </dd>
              </dl>
            )}
            {!isNew && detail ? (
              <>
                <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  {DEV_MSG.SLOT_FINDER_HINT}
                </p>
                <div style={{ marginTop: "12px" }}>
                  <label htmlFor="slot-finder" style={{ display: "block", marginBottom: 4 }}>
                    {DEV_MSG.SLOT_FORM_FINDER}
                  </label>
                  <input
                    id="slot-finder"
                    data-testid="developer-slot-finder"
                    style={{ ...inputStyle, ...monoCell }}
                    value={finderName}
                    disabled={!finderEditable}
                    onChange={(e) => setFinderName(e.target.value)}
                    autoComplete="off"
                  />
                </div>
                <div style={{ marginTop: "12px" }}>
                  <label htmlFor="slot-relationship" style={{ display: "block", marginBottom: 4 }}>
                    {DEV_MSG.SLOT_FORM_RELATIONSHIP}
                  </label>
                  <input
                    id="slot-relationship"
                    data-testid="developer-slot-relationship"
                    style={{ ...inputStyle, ...monoCell }}
                    value={relationshipName}
                    disabled={!finderEditable}
                    onChange={(e) => setRelationshipName(e.target.value)}
                    autoComplete="off"
                  />
                </div>
              </>
            ) : null}
          </header>

          {!isNew && detail ? (
            <>
              <section style={{ marginBottom: "16px" }} data-testid="developer-slot-args">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SLOT_ARGS}</h3>
                {argRows.length === 0 ? (
                  <p style={{ color: catalogColors.empty }} data-testid="developer-slot-args-empty">
                    {DEV_MSG.SLOT_NONE}
                  </p>
                ) : (
                  <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                    {argRows.map((row, i) => (
                      <li
                        key={`${row.key}:${i}`}
                        data-testid={`developer-slot-arg-row-${i}`}
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr auto",
                          gap: "8px",
                          marginBottom: "8px",
                          alignItems: "center",
                        }}
                      >
                        <input
                          data-testid={`developer-slot-arg-key-${i}`}
                          aria-label={`${DEV_MSG.SLOT_ARG_KEY} ${i + 1}`}
                          style={{ ...inputStyle, ...monoCell }}
                          value={row.key}
                          disabled={!finderEditable}
                          onChange={(e) => {
                            const next = e.target.value;
                            setArgRows((prev) =>
                              prev.map((r, idx) => (idx === i ? { ...r, key: next } : r)),
                            );
                          }}
                        />
                        <input
                          data-testid={`developer-slot-arg-value-${i}`}
                          aria-label={`${DEV_MSG.SLOT_ARG_VALUE} ${i + 1}`}
                          style={{ ...inputStyle, ...monoCell }}
                          value={row.value}
                          disabled={!finderEditable}
                          onChange={(e) => {
                            const next = e.target.value;
                            setArgRows((prev) =>
                              prev.map((r, idx) => (idx === i ? { ...r, value: next } : r)),
                            );
                          }}
                        />
                        <button
                          type="button"
                          data-testid={`developer-slot-arg-remove-${i}`}
                          aria-label={`${DEV_MSG.SLOT_ARG_REMOVE} ${row.key || i + 1}`}
                          disabled={!finderEditable}
                          onClick={() => removeFinderArg(i)}
                          style={{
                            background: "transparent",
                            border: `1px solid ${catalogColors.softBorder}`,
                            borderRadius: "4px",
                            padding: "4px 8px",
                            cursor: finderEditable ? "pointer" : "not-allowed",
                          }}
                        >
                          {DEV_MSG.SLOT_ARG_REMOVE}
                        </button>
                      </li>
                    ))}
                  </ul>
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
                    <label htmlFor="slot-arg-key" style={{ display: "block", marginBottom: 4 }}>
                      {DEV_MSG.SLOT_ARG_KEY}
                    </label>
                    <input
                      id="slot-arg-key"
                      data-testid="developer-slot-arg-key"
                      style={{ ...inputStyle, ...monoCell }}
                      placeholder={DEV_MSG.SLOT_ARG_KEY_PLACEHOLDER}
                      value={newArgKey}
                      disabled={!finderEditable}
                      onChange={(e) => setNewArgKey(e.target.value)}
                    />
                  </div>
                  <div>
                    <label htmlFor="slot-arg-value" style={{ display: "block", marginBottom: 4 }}>
                      {DEV_MSG.SLOT_ARG_VALUE}
                    </label>
                    <input
                      id="slot-arg-value"
                      data-testid="developer-slot-arg-value"
                      style={{ ...inputStyle, ...monoCell }}
                      placeholder={DEV_MSG.SLOT_ARG_VALUE_PLACEHOLDER}
                      value={newArgValue}
                      disabled={!finderEditable}
                      onChange={(e) => setNewArgValue(e.target.value)}
                    />
                  </div>
                  <button
                    type="button"
                    data-testid="developer-slot-arg-add"
                    aria-label={DEV_MSG.SLOT_ARG_ADD}
                    disabled={!finderEditable || !newArgKey.trim()}
                    onClick={addFinderArg}
                    style={{
                      padding: "8px 12px",
                      background:
                        finderEditable && newArgKey.trim()
                          ? catalogColors.accent
                          : catalogColors.disabled,
                      color: "#fff",
                      border: "none",
                      borderRadius: "4px",
                      cursor:
                        !finderEditable || !newArgKey.trim() ? "not-allowed" : "pointer",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {DEV_MSG.SLOT_ARG_ADD}
                  </button>
                </div>
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
            </>
          ) : null}

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-slot-save"
              aria-label={DEV_MSG.SLOT_SAVE}
              disabled={!canSave}
              onClick={() => void handleSave()}
              style={{
                padding: "8px 16px",
                background: canSave ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: canSave ? "pointer" : "not-allowed",
              }}
            >
              {DEV_MSG.SLOT_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-slot-cancel"
              disabled={busy}
              onClick={() => void handleBack()}
              style={{
                padding: "8px 16px",
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                cursor: "pointer",
              }}
            >
              {DEV_MSG.SLOT_CANCEL}
            </button>
            {!isNew && writeKey ? (
              <button
                type="button"
                data-testid="developer-slot-delete"
                aria-label={DEV_MSG.SLOT_DELETE}
                disabled={busy}
                onClick={requestDelete}
                style={{
                  padding: "8px 16px",
                  background: "#c53030",
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: busy ? "wait" : "pointer",
                  marginLeft: "auto",
                }}
              >
                {DEV_MSG.SLOT_DELETE}
              </button>
            ) : null}
          </div>

          {!isNew && gaps.length > 0 ? (
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
      <CatalogConfirmDialog
        open={confirmOpen}
        busy={busy}
        message={DEV_MSG.SLOT_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
