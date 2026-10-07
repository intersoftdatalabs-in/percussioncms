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
import { isApiError } from "../api/client";
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
import {
  asKeywordChoices,
  keywordUpdateForAddedChoice,
  keywordUpdateForRelabeledChoice,
  keywordUpdateForRemovedChoice,
  keywordUpdateForRevaluedChoice,
  savedChoicesAfterAdd,
  unwrapKeywordPayload,
} from "./keywordChoiceAdd";
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

function addChoiceFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.KW_ADD_CHOICE_FORBIDDEN);
    }
    if (err.status === 409) {
      return panelErrMsg(err, DEV_MSG.KW_ADD_CHOICE_CONFLICT);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.KW_ADD_CHOICE_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.KW_ADD_CHOICE_ERROR);
}

function removeChoiceFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.KW_REMOVE_CHOICE_FORBIDDEN);
    }
    if (err.status === 409) {
      return panelErrMsg(err, DEV_MSG.KW_REMOVE_CHOICE_CONFLICT);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.KW_REMOVE_CHOICE_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.KW_REMOVE_CHOICE_ERROR);
}

function relabelChoiceFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_LABEL_FORBIDDEN);
    }
    if (err.status === 409) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_LABEL_CONFLICT);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_LABEL_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_LABEL_ERROR);
}

function revalueChoiceFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_VALUE_FORBIDDEN);
    }
    if (err.status === 409) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_VALUE_CONFLICT);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_VALUE_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_VALUE_ERROR);
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
  const [choicesText, setChoicesText] = useState(
    choicesToText(asKeywordChoices(initial?.choices)),
  );
  const [listedChoices, setListedChoices] = useState<KeywordChoiceSummary[]>(
    asKeywordChoices(initial?.choices),
  );
  const [serverKeyword, setServerKeyword] = useState<KeywordSummary | null>(initial);
  const [detailReady, setDetailReady] = useState(isNew || !id);
  const [draftLabel, setDraftLabel] = useState("");
  const [draftValue, setDraftValue] = useState("");
  const [addBusy, setAddBusy] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);
  const [addNotice, setAddNotice] = useState<string | null>(null);
  const addInflight = useRef(false);
  const [removeBusy, setRemoveBusy] = useState(false);
  const [removeError, setRemoveError] = useState<string | null>(null);
  const [removeNotice, setRemoveNotice] = useState<string | null>(null);
  const removeInflight = useRef(false);
  const [pendingRemoveIndex, setPendingRemoveIndex] = useState<number | null>(null);
  const [labelEditIndex, setLabelEditIndex] = useState<number | null>(null);
  const [labelDraft, setLabelDraft] = useState("");
  const [labelBusy, setLabelBusy] = useState(false);
  const [labelError, setLabelError] = useState<string | null>(null);
  const [labelNotice, setLabelNotice] = useState<string | null>(null);
  const labelInflight = useRef(false);
  const [valueEditIndex, setValueEditIndex] = useState<number | null>(null);
  const [valueDraft, setValueDraft] = useState("");
  const [valueBusy, setValueBusy] = useState(false);
  const [valueError, setValueError] = useState<string | null>(null);
  const [valueNotice, setValueNotice] = useState<string | null>(null);
  const valueInflight = useRef(false);
  const [busy, setBusy] = useState(false);
  const [confirmKind, setConfirmKind] = useState<null | "keyword" | "choice">(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const choiceWriteBusy = addBusy || removeBusy || labelBusy || valueBusy;
  const labelEditOpen = labelEditIndex != null;
  const valueEditOpen = valueEditIndex != null;
  const choiceEditOpen = labelEditOpen || valueEditOpen;

  useEffect(() => {
    if (!id || isNew) return;
    let cancelled = false;
    getKeyword(id)
      .then((payload) => {
        if (cancelled) return;
        const kw = unwrapKeywordPayload(payload) ?? payload;
        const choices = asKeywordChoices(kw.choices);
        setServerKeyword({ ...kw, choices });
        setListedChoices(choices);
        setLabel(kw.label || "");
        setDescription(kw.description || "");
        setSequence(kw.sequence != null ? String(kw.sequence) : "0");
        setChoicesText(choicesToText(choices));
        setDetailReady(true);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.KW_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [id, isNew]);

  function applyLoadedKeyword(kw: KeywordSummary): void {
    setServerKeyword(kw);
    setListedChoices(kw.choices ?? []);
    setChoicesText(choicesToText(kw.choices));
  }

  async function handleSave() {
    if (
      choiceWriteBusy ||
      addInflight.current ||
      removeInflight.current ||
      labelInflight.current ||
      valueInflight.current ||
      confirmKind ||
      choiceEditOpen
    ) {
      return;
    }
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

  function clearChoiceDraft(): void {
    setDraftLabel("");
    setDraftValue("");
    setAddError(null);
    setAddNotice(null);
  }

  function cancelAddChoice(): void {
    if (addBusy || addInflight.current) return;
    clearChoiceDraft();
  }

  async function handleAddChoice(): Promise<void> {
    if (
      !id ||
      isNew ||
      !serverKeyword ||
      !detailReady ||
      addInflight.current ||
      removeInflight.current ||
      labelInflight.current ||
      valueInflight.current ||
      removeBusy ||
      labelBusy ||
      valueBusy ||
      choiceEditOpen ||
      busy ||
      confirmKind
    ) {
      return;
    }
    const sent = keywordUpdateForAddedChoice(serverKeyword, listedChoices, {
      label: draftLabel,
      value: draftValue,
    });
    if (sent === "blank") {
      setAddError(DEV_MSG.KW_ADD_CHOICE_BLANK);
      setAddNotice(null);
      return;
    }
    if (sent === "duplicate") {
      setAddError(DEV_MSG.KW_ADD_CHOICE_DUPLICATE);
      setAddNotice(null);
      return;
    }
    addInflight.current = true;
    setAddBusy(true);
    setAddError(null);
    setAddNotice(null);
    const previous = listedChoices;
    try {
      const payload = await updateKeyword(id, sent);
      const accepted = savedChoicesAfterAdd(sent, payload);
      if (!accepted) {
        setListedChoices(previous);
        setAddError(DEV_MSG.KW_ADD_CHOICE_ERROR);
        return;
      }
      const saved = unwrapKeywordPayload(payload);
      if (saved) {
        applyLoadedKeyword({ ...saved, choices: accepted });
      } else {
        setListedChoices(accepted);
        setChoicesText(choicesToText(accepted));
      }
      clearChoiceDraft();
      setAddNotice(DEV_MSG.KW_ADD_CHOICE_SAVED);
    } catch (err: unknown) {
      setListedChoices(previous);
      setAddError(addChoiceFailureMessage(err));
      setAddNotice(null);
    } finally {
      addInflight.current = false;
      setAddBusy(false);
    }
  }

  function requestRemoveChoice(index: number, ev: React.MouseEvent<HTMLElement>): void {
    if (
      !id ||
      isNew ||
      !detailReady ||
      choiceWriteBusy ||
      busy ||
      confirmKind === "keyword" ||
      choiceEditOpen ||
      !Number.isInteger(index) ||
      index < 0 ||
      index >= listedChoices.length
    ) {
      return;
    }
    captureDialogOpener(ev.currentTarget);
    setPendingRemoveIndex(index);
    setConfirmKind("choice");
    setRemoveError(null);
  }

  function cancelConfirm(): void {
    if (busy || removeBusy) return;
    setConfirmKind(null);
    setPendingRemoveIndex(null);
  }

  async function handleRemoveChoice(): Promise<void> {
    const index = pendingRemoveIndex;
    setConfirmKind(null);
    setPendingRemoveIndex(null);
    if (
      !id ||
      isNew ||
      !serverKeyword ||
      !detailReady ||
      index == null ||
      removeInflight.current ||
      labelInflight.current ||
      valueInflight.current ||
      addBusy ||
      labelBusy ||
      valueBusy ||
      busy
    ) {
      return;
    }
    const sent = keywordUpdateForRemovedChoice(serverKeyword, listedChoices, index);
    if (sent === "missing") {
      setRemoveError(DEV_MSG.KW_REMOVE_CHOICE_ERROR);
      setRemoveNotice(null);
      return;
    }
    removeInflight.current = true;
    setRemoveBusy(true);
    setRemoveError(null);
    setRemoveNotice(null);
    const previous = listedChoices;
    try {
      const payload = await updateKeyword(id, sent);
      const accepted = savedChoicesAfterAdd(sent, payload);
      if (!accepted) {
        setListedChoices(previous);
        setRemoveError(DEV_MSG.KW_REMOVE_CHOICE_ERROR);
        setRemoveNotice(null);
        return;
      }
      const saved = unwrapKeywordPayload(payload);
      if (saved) {
        applyLoadedKeyword({ ...saved, choices: accepted });
      } else {
        setListedChoices(accepted);
        setChoicesText(choicesToText(accepted));
      }
      setRemoveNotice(DEV_MSG.KW_REMOVE_CHOICE_SAVED);
    } catch (err: unknown) {
      setListedChoices(previous);
      setRemoveError(removeChoiceFailureMessage(err));
      setRemoveNotice(null);
    } finally {
      removeInflight.current = false;
      setRemoveBusy(false);
    }
  }

  function cancelChoiceLabelEdit(): void {
    if (labelBusy || labelInflight.current) return;
    setLabelEditIndex(null);
    setLabelDraft("");
    setLabelError(null);
  }

  function startChoiceLabelEdit(index: number): void {
    if (
      !id ||
      isNew ||
      !detailReady ||
      choiceWriteBusy ||
      busy ||
      confirmKind != null ||
      labelInflight.current ||
      valueInflight.current ||
      valueEditOpen ||
      !Number.isInteger(index) ||
      index < 0 ||
      index >= listedChoices.length
    ) {
      return;
    }
    setLabelEditIndex(index);
    setLabelDraft(listedChoices[index]?.label || "");
    setLabelError(null);
    setLabelNotice(null);
  }

  async function handleChoiceLabelSave(): Promise<void> {
    const index = labelEditIndex;
    if (
      !id ||
      isNew ||
      !serverKeyword ||
      !detailReady ||
      index == null ||
      labelInflight.current ||
      valueInflight.current ||
      addInflight.current ||
      removeInflight.current ||
      addBusy ||
      removeBusy ||
      valueBusy ||
      busy ||
      confirmKind
    ) {
      return;
    }
    const sent = keywordUpdateForRelabeledChoice(
      serverKeyword,
      listedChoices,
      index,
      labelDraft,
    );
    if (sent === "missing") {
      setLabelError(DEV_MSG.KW_CHANGE_CHOICE_LABEL_ERROR);
      setLabelNotice(null);
      return;
    }
    if (sent === "blank") {
      setLabelError(DEV_MSG.KW_CHANGE_CHOICE_LABEL_BLANK);
      setLabelNotice(null);
      return;
    }
    if (sent === "duplicate") {
      setLabelError(DEV_MSG.KW_CHANGE_CHOICE_LABEL_DUPLICATE);
      setLabelNotice(null);
      return;
    }
    if (sent === "unchanged") {
      setLabelEditIndex(null);
      setLabelDraft("");
      setLabelError(null);
      return;
    }
    labelInflight.current = true;
    setLabelBusy(true);
    setLabelError(null);
    setLabelNotice(null);
    const previous = listedChoices;
    try {
      const payload = await updateKeyword(id, sent);
      const accepted = savedChoicesAfterAdd(sent, payload);
      if (!accepted) {
        setListedChoices(previous);
        setLabelError(DEV_MSG.KW_CHANGE_CHOICE_LABEL_ERROR);
        setLabelNotice(null);
        return;
      }
      const saved = unwrapKeywordPayload(payload);
      if (saved) {
        applyLoadedKeyword({ ...saved, choices: accepted });
      } else {
        setListedChoices(accepted);
        setChoicesText(choicesToText(accepted));
      }
      setLabelEditIndex(null);
      setLabelDraft("");
      setLabelNotice(DEV_MSG.KW_CHANGE_CHOICE_LABEL_SAVED);
    } catch (err: unknown) {
      setListedChoices(previous);
      setLabelError(relabelChoiceFailureMessage(err));
      setLabelNotice(null);
    } finally {
      labelInflight.current = false;
      setLabelBusy(false);
    }
  }

  function cancelChoiceValueEdit(): void {
    if (valueBusy || valueInflight.current) return;
    setValueEditIndex(null);
    setValueDraft("");
    setValueError(null);
  }

  function startChoiceValueEdit(index: number): void {
    if (
      !id ||
      isNew ||
      !detailReady ||
      choiceWriteBusy ||
      busy ||
      confirmKind != null ||
      labelInflight.current ||
      valueInflight.current ||
      labelEditOpen ||
      valueEditOpen ||
      !Number.isInteger(index) ||
      index < 0 ||
      index >= listedChoices.length
    ) {
      return;
    }
    setValueEditIndex(index);
    setValueDraft(listedChoices[index]?.value || "");
    setValueError(null);
    setValueNotice(null);
  }

  async function handleChoiceValueSave(): Promise<void> {
    const index = valueEditIndex;
    if (
      !id ||
      isNew ||
      !serverKeyword ||
      !detailReady ||
      index == null ||
      valueInflight.current ||
      labelInflight.current ||
      addInflight.current ||
      removeInflight.current ||
      addBusy ||
      removeBusy ||
      labelBusy ||
      busy ||
      confirmKind
    ) {
      return;
    }
    const sent = keywordUpdateForRevaluedChoice(
      serverKeyword,
      listedChoices,
      index,
      valueDraft,
    );
    if (sent === "missing") {
      setValueError(DEV_MSG.KW_CHANGE_CHOICE_VALUE_ERROR);
      setValueNotice(null);
      return;
    }
    if (sent === "blank") {
      setValueError(DEV_MSG.KW_CHANGE_CHOICE_VALUE_BLANK);
      setValueNotice(null);
      return;
    }
    if (sent === "duplicate") {
      setValueError(DEV_MSG.KW_CHANGE_CHOICE_VALUE_DUPLICATE);
      setValueNotice(null);
      return;
    }
    if (sent === "unchanged") {
      setValueEditIndex(null);
      setValueDraft("");
      setValueError(null);
      return;
    }
    valueInflight.current = true;
    setValueBusy(true);
    setValueError(null);
    setValueNotice(null);
    const previous = listedChoices;
    try {
      const payload = await updateKeyword(id, sent);
      const accepted = savedChoicesAfterAdd(sent, payload);
      if (!accepted) {
        setListedChoices(previous);
        setValueError(DEV_MSG.KW_CHANGE_CHOICE_VALUE_ERROR);
        setValueNotice(null);
        return;
      }
      const saved = unwrapKeywordPayload(payload);
      if (saved) {
        applyLoadedKeyword({ ...saved, choices: accepted });
      } else {
        setListedChoices(accepted);
        setChoicesText(choicesToText(accepted));
      }
      setValueEditIndex(null);
      setValueDraft("");
      setValueNotice(DEV_MSG.KW_CHANGE_CHOICE_VALUE_SAVED);
    } catch (err: unknown) {
      setListedChoices(previous);
      setValueError(revalueChoiceFailureMessage(err));
      setValueNotice(null);
    } finally {
      valueInflight.current = false;
      setValueBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (!id || isNew || choiceWriteBusy || confirmKind === "choice" || choiceEditOpen) return;
    captureDialogOpener(ev.currentTarget);
    setPendingRemoveIndex(null);
    setConfirmKind("keyword");
  }

  async function handleDelete() {
    if (!id || isNew) return;
    setConfirmKind(null);
    setPendingRemoveIndex(null);
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

      {!isNew && id ? (
        <section data-testid="developer-kw-add-choice" aria-label={DEV_MSG.KW_ADD_CHOICE_SAVE}>
          <h3 style={{ marginBottom: "8px" }}>{DEV_MSG.KW_CHOICES_TITLE}</h3>
          <p style={{ color: "#4a5568", marginTop: 0, fontSize: "0.9rem" }}>
            {DEV_MSG.KW_ADD_CHOICE_HINT}
          </p>
          <p style={{ color: "#4a5568", marginTop: 0, fontSize: "0.9rem" }}>
            {DEV_MSG.KW_REMOVE_CHOICE_HINT}
          </p>
          <p style={{ color: "#4a5568", marginTop: 0, fontSize: "0.9rem" }}>
            {DEV_MSG.KW_CHANGE_CHOICE_LABEL_HINT}
          </p>
          <p style={{ color: "#4a5568", marginTop: 0, fontSize: "0.9rem" }}>
            {DEV_MSG.KW_CHANGE_CHOICE_VALUE_HINT}
          </p>
          {addError ? (
            <div role="alert" data-testid="developer-kw-add-choice-error" style={errorAlert}>
              {addError}
            </div>
          ) : null}
          {addNotice ? (
            <div data-testid="developer-kw-add-choice-notice" style={{ color: "#276749" }}>
              {addNotice}
            </div>
          ) : null}
          {removeError ? (
            <div role="alert" data-testid="developer-kw-remove-choice-error" style={errorAlert}>
              {removeError}
            </div>
          ) : null}
          {removeNotice ? (
            <div data-testid="developer-kw-remove-choice-notice" style={{ color: "#276749" }}>
              {removeNotice}
            </div>
          ) : null}
          {labelError ? (
            <div role="alert" data-testid="developer-kw-choice-label-error" style={errorAlert}>
              {labelError}
            </div>
          ) : null}
          {labelNotice ? (
            <div data-testid="developer-kw-choice-label-notice" style={{ color: "#276749" }}>
              {labelNotice}
            </div>
          ) : null}
          {valueError ? (
            <div role="alert" data-testid="developer-kw-choice-value-error" style={errorAlert}>
              {valueError}
            </div>
          ) : null}
          {valueNotice ? (
            <div data-testid="developer-kw-choice-value-notice" style={{ color: "#276749" }}>
              {valueNotice}
            </div>
          ) : null}
          {listedChoices.length === 0 ? (
            <p data-testid="developer-kw-choices-empty">{DEV_MSG.KW_CHOICES_EMPTY}</p>
          ) : (
            <ul data-testid="developer-kw-saved-choices" style={{ paddingLeft: "1.2rem" }}>
              {listedChoices.map((choice, index) => {
                const choiceLabel = choice.label || "";
                const choiceValue = choice.value || "";
                return (
                  <li
                    key={`${choiceLabel}-${choiceValue}-${choice.sequence ?? index}`}
                    data-testid="developer-kw-choice"
                    data-choice-label={choiceLabel}
                    data-choice-value={choiceValue}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "8px",
                      marginBottom: "6px",
                    }}
                  >
                    <span data-testid="developer-kw-choice-label-text">
                      {choiceLabel}
                      {choiceValue ? ` (${choiceValue})` : ""}
                    </span>
                    <button
                      type="button"
                      data-testid="developer-kw-choice-label-edit"
                      data-choice-label={choiceLabel}
                      aria-label={DEV_MSG.KW_CHANGE_CHOICE_LABEL_ACTION.replace(
                        "{0}",
                        choiceLabel || String(index + 1),
                      )}
                      disabled={
                        choiceWriteBusy || busy || !detailReady || confirmKind != null || choiceEditOpen
                      }
                      onClick={() => startChoiceLabelEdit(index)}
                      style={{
                        padding: "4px 10px",
                        background: "transparent",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        cursor: choiceWriteBusy ? "wait" : "pointer",
                      }}
                    >
                      {DEV_MSG.KW_CHANGE_CHOICE_LABEL}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-kw-choice-value-edit"
                      data-choice-label={choiceLabel}
                      aria-label={DEV_MSG.KW_CHANGE_CHOICE_VALUE_ACTION.replace(
                        "{0}",
                        choiceLabel || String(index + 1),
                      )}
                      disabled={
                        choiceWriteBusy || busy || !detailReady || confirmKind != null || choiceEditOpen
                      }
                      onClick={() => startChoiceValueEdit(index)}
                      style={{
                        padding: "4px 10px",
                        background: "transparent",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        cursor: choiceWriteBusy ? "wait" : "pointer",
                      }}
                    >
                      {DEV_MSG.KW_CHANGE_CHOICE_VALUE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-kw-choice-remove"
                      data-choice-label={choiceLabel}
                      aria-label={DEV_MSG.KW_REMOVE_CHOICE_ACTION.replace(
                        "{0}",
                        choiceLabel || String(index + 1),
                      )}
                      disabled={
                        choiceWriteBusy || busy || !detailReady || confirmKind != null || choiceEditOpen
                      }
                      onClick={(ev) => requestRemoveChoice(index, ev)}
                      style={{
                        padding: "4px 10px",
                        background: "transparent",
                        color: "#c53030",
                        border: "1px solid #c53030",
                        borderRadius: "4px",
                        cursor: choiceWriteBusy ? "wait" : "pointer",
                      }}
                    >
                      {DEV_MSG.KW_REMOVE_CHOICE}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
          {labelEditOpen ? (
            <div data-testid="developer-kw-choice-label-editor" style={{ marginBottom: "16px" }}>
              <div style={fieldStyle}>
                <label htmlFor="kw-choice-label-input">{DEV_MSG.KW_ADD_CHOICE_LABEL}</label>
                <input
                  id="kw-choice-label-input"
                  data-testid="developer-kw-choice-label-input"
                  style={inputStyle}
                  value={labelDraft}
                  onChange={(e) => setLabelDraft(e.target.value)}
                />
              </div>
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                <button
                  type="button"
                  data-testid="developer-kw-choice-label-save"
                  aria-label={DEV_MSG.KW_CHANGE_CHOICE_LABEL_SAVE}
                  disabled={
                    choiceWriteBusy ||
                    busy ||
                    !detailReady ||
                    confirmKind != null ||
                    labelDraft.trim().length === 0
                  }
                  onClick={() => void handleChoiceLabelSave()}
                  style={{
                    padding: "8px 16px",
                    background: catalogColors.accent,
                    color: "#fff",
                    border: "none",
                    borderRadius: "4px",
                    cursor: labelBusy ? "wait" : "pointer",
                  }}
                >
                  {DEV_MSG.KW_CHANGE_CHOICE_LABEL_SAVE}
                </button>
                <button
                  type="button"
                  data-testid="developer-kw-choice-label-cancel"
                  disabled={labelBusy}
                  onClick={cancelChoiceLabelEdit}
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
              </div>
            </div>
          ) : null}
          {valueEditOpen ? (
            <div data-testid="developer-kw-choice-value-editor" style={{ marginBottom: "16px" }}>
              <div style={fieldStyle}>
                <label htmlFor="kw-choice-value-input">{DEV_MSG.KW_ADD_CHOICE_VALUE}</label>
                <input
                  id="kw-choice-value-input"
                  data-testid="developer-kw-choice-value-input"
                  style={inputStyle}
                  value={valueDraft}
                  onChange={(e) => setValueDraft(e.target.value)}
                />
              </div>
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                <button
                  type="button"
                  data-testid="developer-kw-choice-value-save"
                  aria-label={DEV_MSG.KW_CHANGE_CHOICE_VALUE_SAVE}
                  disabled={choiceWriteBusy || busy || !detailReady || confirmKind != null}
                  onClick={() => void handleChoiceValueSave()}
                  style={{
                    padding: "8px 16px",
                    background: catalogColors.accent,
                    color: "#fff",
                    border: "none",
                    borderRadius: "4px",
                    cursor: valueBusy ? "wait" : "pointer",
                  }}
                >
                  {DEV_MSG.KW_CHANGE_CHOICE_VALUE_SAVE}
                </button>
                <button
                  type="button"
                  data-testid="developer-kw-choice-value-cancel"
                  disabled={valueBusy}
                  onClick={cancelChoiceValueEdit}
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
              </div>
            </div>
          ) : null}
          <div style={fieldStyle}>
            <label htmlFor="kw-add-choice-label">{DEV_MSG.KW_ADD_CHOICE_LABEL}</label>
            <input
              id="kw-add-choice-label"
              data-testid="developer-kw-add-choice-label"
              style={inputStyle}
              value={draftLabel}
              onChange={(e) => setDraftLabel(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="kw-add-choice-value">{DEV_MSG.KW_ADD_CHOICE_VALUE}</label>
            <input
              id="kw-add-choice-value"
              data-testid="developer-kw-add-choice-value"
              style={inputStyle}
              value={draftValue}
              onChange={(e) => setDraftValue(e.target.value)}
            />
          </div>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-kw-add-choice-save"
              aria-label={DEV_MSG.KW_ADD_CHOICE_SAVE}
              disabled={
                choiceWriteBusy ||
                busy ||
                !detailReady ||
                confirmKind != null ||
                choiceEditOpen ||
                draftLabel.trim().length === 0
              }
              onClick={() => void handleAddChoice()}
              style={{
                padding: "8px 16px",
                background: catalogColors.accent,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: addBusy ? "wait" : "pointer",
              }}
            >
              {DEV_MSG.KW_ADD_CHOICE_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-kw-add-choice-cancel"
              disabled={choiceWriteBusy}
              onClick={cancelAddChoice}
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
          </div>
        </section>
      ) : null}

      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
        <button
          type="button"
          data-testid="developer-kw-save"
          aria-label="Save keyword"
          disabled={busy || choiceWriteBusy || confirmKind != null || choiceEditOpen || !label.trim()}
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
            disabled={busy || choiceWriteBusy || confirmKind === "choice" || choiceEditOpen}
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
        open={confirmKind != null}
        busy={busy || removeBusy}
        message={
          confirmKind === "choice"
            ? DEV_MSG.KW_REMOVE_CHOICE_CONFIRM.replace(
                "{0}",
                pendingRemoveIndex != null
                  ? listedChoices[pendingRemoveIndex]?.label || ""
                  : "",
              )
            : DEV_MSG.KW_DELETE_CONFIRM
        }
        onCancel={cancelConfirm}
        onConfirm={() => {
          if (confirmKind === "choice") {
            void handleRemoveChoice();
          } else {
            void handleDelete();
          }
        }}
      />
    </div>
  );
}
