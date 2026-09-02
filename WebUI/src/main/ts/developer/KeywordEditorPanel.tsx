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
import { captureDialogOpener } from "../architecture/useDialogEscape";
import {
  createKeyword,
  deleteKeyword,
  getKeyword,
  updateKeyword,
} from "../api/developer/keywordsApi";
import type { KeywordChoiceSummary, KeywordSummary } from "../api/developer/types";
import { catalogColors, backButton, errorAlert } from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

function choicesToText(choices: KeywordChoiceSummary[] | undefined): string {
  if (!choices?.length) return "";
  return choices
    .map((c) => {
      const parts = [c.label || "", c.value || ""];
      if (c.sequence != null) parts.push(String(c.sequence));
      return parts.join("|");
    })
    .join("\n");
}

function textToChoices(text: string): KeywordChoiceSummary[] {
  return text
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const [label, value, seq] = line.split("|").map((s) => s.trim());
      const parsed = seq != null && seq !== "" ? Number(seq) : NaN;
      return {
        label: label || `choice-${index + 1}`,
        value: value || label || "",
        sequence: Number.isFinite(parsed) ? parsed : index,
      };
    });
}

function keywordId(kw: KeywordSummary): string | null {
  if (kw.guid?.uuid != null) return String(kw.guid.uuid);
  if (kw.guid?.stringValue) return kw.guid.stringValue;
  return null;
}

const fieldStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "4px",
  marginBottom: "12px",
};

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
};

export function KeywordEditorPanel({
  initial,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  initial: KeywordSummary | null;
  onBack: () => void;
  onSaved: (kw: KeywordSummary) => void;
  onDeleted: () => void;
}): React.ReactElement {
  const isNew = initial == null;
  const id = initial ? keywordId(initial) : null;

  const [label, setLabel] = useState(initial?.label || "");
  const [description, setDescription] = useState(initial?.description || "");
  const [sequence, setSequence] = useState(
    initial?.sequence != null ? String(initial.sequence) : "0",
  );
  const [choicesText, setChoicesText] = useState(choicesToText(initial?.choices));
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    if (!id || isNew) return;
    let cancelled = false;
    getKeyword(id)
      .then((kw) => {
        if (cancelled) return;
        setLabel(kw.label || "");
        setDescription(kw.description || "");
        setSequence(kw.sequence != null ? String(kw.sequence) : "0");
        setChoicesText(choicesToText(kw.choices));
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.KW_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [id, isNew]);

  async function handleSave() {
    setBusy(true);
    setError(null);
    setNotice(null);
    const body: KeywordSummary = {
      label: label.trim(),
      description,
      sequence: Number.isFinite(Number(sequence)) ? Number(sequence) : 0,
      choices: textToChoices(choicesText),
    };
    try {
      const saved =
        isNew || !id
          ? await createKeyword(body)
          : await updateKeyword(id, body);
      setNotice(DEV_MSG.KW_SAVED);
      onSaved(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.KW_SAVE_ERROR));
    } finally {
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (!id || isNew) return;
    captureDialogOpener(ev.currentTarget);
    setConfirmOpen(true);
  }

  async function handleDelete() {
    if (!id || isNew) return;
    setConfirmOpen(false);
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteKeyword(id);
      setNotice(DEV_MSG.KW_DELETED);
      onDeleted();
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.KW_DELETE_ERROR));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="developer-kw-editor">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-kw-back"
        aria-label="Back to keywords list"
        style={backButton}
      >
        ← {DEV_MSG.KW_BACK}
      </button>

      <h2 style={{ marginTop: 0 }}>
        {isNew ? DEV_MSG.KW_NEW : `${DEV_MSG.KW_EDIT}: ${initial?.label || id}`}
      </h2>

      {error ? (
        <div role="alert" data-testid="developer-kw-editor-error" style={errorAlert}>
          {error}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-kw-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      <div style={fieldStyle}>
        <label htmlFor="kw-label">{DEV_MSG.KW_FORM_LABEL}</label>
        <input
          id="kw-label"
          data-testid="developer-kw-label"
          style={inputStyle}
          value={label}
          onChange={(e) => setLabel(e.target.value)}
        />
      </div>
      <div style={fieldStyle}>
        <label htmlFor="kw-desc">{DEV_MSG.KW_FORM_DESCRIPTION}</label>
        <input
          id="kw-desc"
          data-testid="developer-kw-description"
          style={inputStyle}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      <div style={fieldStyle}>
        <label htmlFor="kw-seq">{DEV_MSG.KW_FORM_SEQUENCE}</label>
        <input
          id="kw-seq"
          data-testid="developer-kw-sequence"
          style={inputStyle}
          value={sequence}
          onChange={(e) => setSequence(e.target.value)}
        />
      </div>
      <div style={fieldStyle}>
        <label htmlFor="kw-choices">{DEV_MSG.KW_FORM_CHOICES}</label>
        <textarea
          id="kw-choices"
          data-testid="developer-kw-choices"
          style={{ ...inputStyle, minHeight: "120px", fontFamily: "monospace" }}
          value={choicesText}
          onChange={(e) => setChoicesText(e.target.value)}
        />
      </div>

      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
        <button
          type="button"
          data-testid="developer-kw-save"
          aria-label="Save keyword"
          disabled={busy || !label.trim()}
          onClick={() => void handleSave()}
          style={{
            padding: "8px 16px",
            background: catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: busy ? "wait" : "pointer",
          }}
        >
          {DEV_MSG.KW_SAVE}
        </button>
        <button
          type="button"
          data-testid="developer-kw-cancel"
          disabled={busy}
          onClick={onBack}
          style={{
            padding: "8px 16px",
            background: "transparent",
            border: `1px solid ${catalogColors.softBorder}`,
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {DEV_MSG.KW_CANCEL}
        </button>
        {!isNew && id ? (
          <button
            type="button"
            data-testid="developer-kw-delete"
            aria-label="Delete keyword"
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
            {DEV_MSG.KW_DELETE}
          </button>
        ) : null}
      </div>
      <CatalogConfirmDialog
        open={confirmOpen}
        busy={busy}
        message={DEV_MSG.KW_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
