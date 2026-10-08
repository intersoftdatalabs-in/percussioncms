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
  keywordUpdateForClearedChoiceDescription,
  keywordUpdateForDescribedChoice,
  keywordUpdateForKeywordDescription,
  keywordUpdateForKeywordLabel,
  keywordUpdateForRelabeledChoice,
  keywordUpdateForRemovedChoice,
  keywordUpdateForResequencedChoice,
  keywordUpdateForRevaluedChoice,
  savedChoicesAfterAdd,
  savedKeywordDescription,
  savedKeywordLabel,
  storedKeywordDescription,
  storedKeywordLabel,
  unwrapKeywordPayload,
} from "./keywordChoiceAdd";
import { KW_LABEL_MSG } from "./keywordLabelMessages";
import { DEV_MSG } from "./messages";

function choiceDescriptionText(choice: KeywordChoiceSummary): string {
  return (choice.description ?? "").replace(/[\r\n]+/g, " ").trim();
}

function choicesToText(choices: KeywordChoiceSummary[] | undefined): string {
  if (!choices?.length) return "";
  return choices
    .map((c, index) => {
      const parts = [c.label || "", c.value || ""];
      const description = choiceDescriptionText(c);
      if (c.sequence != null || description) {
        parts.push(c.sequence != null ? String(c.sequence) : String(index));
      }
      if (description) parts.push(description);
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
      const pieces = line.split("|").map((s) => s.trim());
      const label = pieces[0] ?? "";
      const value = pieces[1] ?? "";
      const seq = pieces[2];
      const description = pieces.length > 3 ? pieces.slice(3).join("|").trim() : "";
      const parsed = seq != null && seq !== "" ? Number(seq) : NaN;
      const choice: KeywordChoiceSummary = {
        label: label || `choice-${index + 1}`,
        value: value || label || "",
        sequence: Number.isFinite(parsed) ? parsed : index,
      };
      if (description) {
        choice.description = description;
      }
      return choice;
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

function describeChoiceFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_FORBIDDEN);
    }
    if (err.status === 409) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_CONFLICT);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_ERROR);
}

function clearChoiceDescriptionFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_FORBIDDEN);
    }
    if (err.status === 409) {
      return panelErrMsg(err, DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_CONFLICT);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_ERROR);
}

function keywordDescriptionFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.KW_SET_DESCRIPTION_FORBIDDEN);
    }
    if (err.status === 409) {
      return panelErrMsg(err, DEV_MSG.KW_SET_DESCRIPTION_CONFLICT);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.KW_SET_DESCRIPTION_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.KW_SET_DESCRIPTION_ERROR);
}

function keywordLabelFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, KW_LABEL_MSG.FORBIDDEN);
    }
    if (err.status === 409) {
      return panelErrMsg(err, KW_LABEL_MSG.CONFLICT);
    }
    if (err.status === 400) {
      return panelErrMsg(err, KW_LABEL_MSG.INVALID);
    }
  }
  return panelErrMsg(err, KW_LABEL_MSG.ERROR);
}

function resequenceChoiceFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_FORBIDDEN);
    }
    if (err.status === 409) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_CONFLICT);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_ERROR);
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
  const [descriptionEditIndex, setDescriptionEditIndex] = useState<number | null>(null);
  const [descriptionDraft, setDescriptionDraft] = useState("");
  const [descriptionBusy, setDescriptionBusy] = useState(false);
  const [descriptionError, setDescriptionError] = useState<string | null>(null);
  const [descriptionNotice, setDescriptionNotice] = useState<string | null>(null);
  const descriptionInflight = useRef(false);
  const [keywordDescriptionShown, setKeywordDescriptionShown] = useState(
    storedKeywordDescription(initial?.description),
  );
  const [keywordDescriptionEditing, setKeywordDescriptionEditing] = useState(false);
  const [keywordDescriptionDraft, setKeywordDescriptionDraft] = useState("");
  const [keywordDescriptionBusy, setKeywordDescriptionBusy] = useState(false);
  const [keywordDescriptionError, setKeywordDescriptionError] = useState<string | null>(null);
  const [keywordDescriptionNotice, setKeywordDescriptionNotice] = useState<string | null>(null);
  const keywordDescriptionInflight = useRef(false);
  const [keywordLabelShown, setKeywordLabelShown] = useState(storedKeywordLabel(initial?.label));
  const [keywordLabelEditing, setKeywordLabelEditing] = useState(false);
  const [keywordLabelDraft, setKeywordLabelDraft] = useState("");
  const [keywordLabelBusy, setKeywordLabelBusy] = useState(false);
  const [keywordLabelError, setKeywordLabelError] = useState<string | null>(null);
  const [keywordLabelNotice, setKeywordLabelNotice] = useState<string | null>(null);
  const keywordLabelInflight = useRef(false);
  const [pendingClearDescriptionIndex, setPendingClearDescriptionIndex] = useState<number | null>(
    null,
  );
  const [clearDescriptionError, setClearDescriptionError] = useState<string | null>(null);
  const [clearDescriptionNotice, setClearDescriptionNotice] = useState<string | null>(null);
  const [sequenceEditIndex, setSequenceEditIndex] = useState<number | null>(null);
  const [sequenceDraft, setSequenceDraft] = useState("");
  const [sequenceBusy, setSequenceBusy] = useState(false);
  const [sequenceError, setSequenceError] = useState<string | null>(null);
  const [sequenceNotice, setSequenceNotice] = useState<string | null>(null);
  const sequenceInflight = useRef(false);
  const [busy, setBusy] = useState(false);
  const [confirmKind, setConfirmKind] = useState<
    null | "keyword" | "choice" | "clear-description"
  >(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const choiceWriteBusy =
    addBusy ||
    removeBusy ||
    labelBusy ||
    valueBusy ||
    descriptionBusy ||
    sequenceBusy ||
    keywordDescriptionBusy ||
    keywordLabelBusy;
  const labelEditOpen = labelEditIndex != null;
  const valueEditOpen = valueEditIndex != null;
  const descriptionEditOpen = descriptionEditIndex != null;
  const sequenceEditOpen = sequenceEditIndex != null;
  const choiceEditOpen =
    labelEditOpen ||
    valueEditOpen ||
    descriptionEditOpen ||
    sequenceEditOpen ||
    keywordDescriptionEditing ||
    keywordLabelEditing;

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
        setKeywordLabelShown(storedKeywordLabel(kw.label));
        setDescription(kw.description || "");
        setKeywordDescriptionShown(storedKeywordDescription(kw.description));
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
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      sequenceInflight.current ||
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
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      sequenceInflight.current ||
      removeBusy ||
      labelBusy ||
      valueBusy ||
      descriptionBusy ||
      sequenceBusy ||
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
      confirmKind === "clear-description" ||
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
    if (busy || removeBusy || descriptionBusy) return;
    setConfirmKind(null);
    setPendingRemoveIndex(null);
    setPendingClearDescriptionIndex(null);
  }

  function requestClearChoiceDescription(index: number, ev: React.MouseEvent<HTMLElement>): void {
    if (
      !id ||
      isNew ||
      !detailReady ||
      choiceWriteBusy ||
      busy ||
      confirmKind != null ||
      choiceEditOpen ||
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      !Number.isInteger(index) ||
      index < 0 ||
      index >= listedChoices.length
    ) {
      return;
    }
    captureDialogOpener(ev.currentTarget);
    setPendingClearDescriptionIndex(index);
    setConfirmKind("clear-description");
    setClearDescriptionError(null);
    setClearDescriptionNotice(null);
  }

  async function handleClearChoiceDescription(): Promise<void> {
    const index = pendingClearDescriptionIndex;
    setConfirmKind(null);
    setPendingClearDescriptionIndex(null);
    if (
      !id ||
      isNew ||
      !serverKeyword ||
      !detailReady ||
      index == null ||
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      labelInflight.current ||
      valueInflight.current ||
      sequenceInflight.current ||
      addInflight.current ||
      removeInflight.current ||
      addBusy ||
      removeBusy ||
      labelBusy ||
      valueBusy ||
      sequenceBusy ||
      busy
    ) {
      return;
    }
    const sent = keywordUpdateForClearedChoiceDescription(serverKeyword, listedChoices, index);
    if (sent === "missing") {
      setClearDescriptionError(DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_ERROR);
      setClearDescriptionNotice(null);
      return;
    }
    if (sent === "unchanged") {
      setClearDescriptionError(null);
      setClearDescriptionNotice(null);
      return;
    }
    descriptionInflight.current = true;
    setDescriptionBusy(true);
    setClearDescriptionError(null);
    setClearDescriptionNotice(null);
    setDescriptionError(null);
    setDescriptionNotice(null);
    const previous = listedChoices;
    try {
      const payload = await updateKeyword(id, sent);
      const accepted = savedChoicesAfterAdd(sent, payload);
      if (!accepted) {
        setListedChoices(previous);
        setClearDescriptionError(DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_ERROR);
        setClearDescriptionNotice(null);
        return;
      }
      const saved = unwrapKeywordPayload(payload);
      if (saved) {
        applyLoadedKeyword({ ...saved, choices: accepted });
      } else {
        setListedChoices(accepted);
        setChoicesText(choicesToText(accepted));
      }
      setClearDescriptionNotice(DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_SAVED);
    } catch (err: unknown) {
      setListedChoices(previous);
      setClearDescriptionError(clearChoiceDescriptionFailureMessage(err));
      setClearDescriptionNotice(null);
    } finally {
      descriptionInflight.current = false;
      setDescriptionBusy(false);
    }
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
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      sequenceInflight.current ||
      addBusy ||
      labelBusy ||
      valueBusy ||
      descriptionBusy ||
      sequenceBusy ||
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
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      sequenceInflight.current ||
      valueEditOpen ||
      descriptionEditOpen ||
      sequenceEditOpen ||
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
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      sequenceInflight.current ||
      addInflight.current ||
      removeInflight.current ||
      addBusy ||
      removeBusy ||
      valueBusy ||
      descriptionBusy ||
      sequenceBusy ||
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
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      sequenceInflight.current ||
      labelEditOpen ||
      valueEditOpen ||
      descriptionEditOpen ||
      sequenceEditOpen ||
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
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      sequenceInflight.current ||
      addInflight.current ||
      removeInflight.current ||
      addBusy ||
      removeBusy ||
      labelBusy ||
      descriptionBusy ||
      sequenceBusy ||
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

  function cancelChoiceDescriptionEdit(): void {
    if (
      descriptionBusy ||
      descriptionInflight.current ||
      keywordDescriptionInflight.current ||
      keywordLabelInflight.current
    )
      return;
    setDescriptionEditIndex(null);
    setDescriptionDraft("");
    setDescriptionError(null);
  }

  function startChoiceDescriptionEdit(index: number): void {
    if (
      !id ||
      isNew ||
      !detailReady ||
      choiceWriteBusy ||
      busy ||
      confirmKind != null ||
      labelInflight.current ||
      valueInflight.current ||
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      sequenceInflight.current ||
      labelEditOpen ||
      valueEditOpen ||
      descriptionEditOpen ||
      sequenceEditOpen ||
      !Number.isInteger(index) ||
      index < 0 ||
      index >= listedChoices.length
    ) {
      return;
    }
    setDescriptionEditIndex(index);
    setDescriptionDraft(listedChoices[index]?.description || "");
    setDescriptionError(null);
    setDescriptionNotice(null);
  }

  async function handleChoiceDescriptionSave(): Promise<void> {
    const index = descriptionEditIndex;
    if (
      !id ||
      isNew ||
      !serverKeyword ||
      !detailReady ||
      index == null ||
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      labelInflight.current ||
      valueInflight.current ||
      sequenceInflight.current ||
      addInflight.current ||
      removeInflight.current ||
      addBusy ||
      removeBusy ||
      labelBusy ||
      valueBusy ||
      sequenceBusy ||
      busy ||
      confirmKind
    ) {
      return;
    }
    const sent = keywordUpdateForDescribedChoice(
      serverKeyword,
      listedChoices,
      index,
      descriptionDraft,
    );
    if (sent === "missing") {
      setDescriptionError(DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_ERROR);
      setDescriptionNotice(null);
      return;
    }
    if (sent === "blank") {
      setDescriptionError(DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_BLANK);
      setDescriptionNotice(null);
      return;
    }
    if (sent === "unchanged") {
      setDescriptionEditIndex(null);
      setDescriptionDraft("");
      setDescriptionError(null);
      return;
    }
    descriptionInflight.current = true;
    setDescriptionBusy(true);
    setDescriptionError(null);
    setDescriptionNotice(null);
    const previous = listedChoices;
    try {
      const payload = await updateKeyword(id, sent);
      const accepted = savedChoicesAfterAdd(sent, payload);
      if (!accepted) {
        setListedChoices(previous);
        setDescriptionError(DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_ERROR);
        setDescriptionNotice(null);
        return;
      }
      const saved = unwrapKeywordPayload(payload);
      if (saved) {
        applyLoadedKeyword({ ...saved, choices: accepted });
      } else {
        setListedChoices(accepted);
        setChoicesText(choicesToText(accepted));
      }
      setDescriptionEditIndex(null);
      setDescriptionDraft("");
      setDescriptionNotice(DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_SAVED);
    } catch (err: unknown) {
      setListedChoices(previous);
      setDescriptionError(describeChoiceFailureMessage(err));
      setDescriptionNotice(null);
    } finally {
      descriptionInflight.current = false;
      setDescriptionBusy(false);
    }
  }

  function cancelChoiceSequenceEdit(): void {
    if (sequenceBusy || sequenceInflight.current) return;
    setSequenceEditIndex(null);
    setSequenceDraft("");
    setSequenceError(null);
  }

  function startChoiceSequenceEdit(index: number): void {
    if (
      !id ||
      isNew ||
      !detailReady ||
      choiceWriteBusy ||
      busy ||
      confirmKind != null ||
      labelInflight.current ||
      valueInflight.current ||
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      sequenceInflight.current ||
      labelEditOpen ||
      valueEditOpen ||
      descriptionEditOpen ||
      sequenceEditOpen ||
      !Number.isInteger(index) ||
      index < 0 ||
      index >= listedChoices.length
    ) {
      return;
    }
    const current = listedChoices[index]?.sequence;
    setSequenceEditIndex(index);
    setSequenceDraft(current != null ? String(current) : "");
    setSequenceError(null);
    setSequenceNotice(null);
  }

  async function handleChoiceSequenceSave(): Promise<void> {
    const index = sequenceEditIndex;
    if (
      !id ||
      isNew ||
      !serverKeyword ||
      !detailReady ||
      index == null ||
      sequenceInflight.current ||
      labelInflight.current ||
      valueInflight.current ||
      descriptionInflight.current || keywordDescriptionInflight.current || keywordLabelInflight.current ||
      addInflight.current ||
      removeInflight.current ||
      addBusy ||
      removeBusy ||
      labelBusy ||
      valueBusy ||
      descriptionBusy ||
      busy ||
      confirmKind
    ) {
      return;
    }
    const sent = keywordUpdateForResequencedChoice(
      serverKeyword,
      listedChoices,
      index,
      sequenceDraft,
    );
    if (sent === "missing") {
      setSequenceError(DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_ERROR);
      setSequenceNotice(null);
      return;
    }
    if (sent === "blank") {
      setSequenceError(DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_BLANK);
      setSequenceNotice(null);
      return;
    }
    if (sent === "invalid") {
      setSequenceError(DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_NOT_INTEGER);
      setSequenceNotice(null);
      return;
    }
    if (sent === "unchanged") {
      setSequenceEditIndex(null);
      setSequenceDraft("");
      setSequenceError(null);
      return;
    }
    sequenceInflight.current = true;
    setSequenceBusy(true);
    setSequenceError(null);
    setSequenceNotice(null);
    const previous = listedChoices;
    try {
      const payload = await updateKeyword(id, sent);
      const accepted = savedChoicesAfterAdd(sent, payload);
      if (!accepted) {
        setListedChoices(previous);
        setSequenceError(DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_ERROR);
        setSequenceNotice(null);
        return;
      }
      const saved = unwrapKeywordPayload(payload);
      if (saved) {
        applyLoadedKeyword({ ...saved, choices: accepted });
      } else {
        setListedChoices(accepted);
        setChoicesText(choicesToText(accepted));
      }
      setSequenceEditIndex(null);
      setSequenceDraft("");
      setSequenceNotice(DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_SAVED);
    } catch (err: unknown) {
      setListedChoices(previous);
      setSequenceError(resequenceChoiceFailureMessage(err));
      setSequenceNotice(null);
    } finally {
      sequenceInflight.current = false;
      setSequenceBusy(false);
    }
  }

  function cancelKeywordDescriptionEdit(): void {
    if (keywordDescriptionBusy || keywordDescriptionInflight.current || keywordLabelInflight.current)
      return;
    setKeywordDescriptionEditing(false);
    setKeywordDescriptionDraft("");
    setKeywordDescriptionError(null);
  }

  function startKeywordDescriptionEdit(): void {
    if (
      !id ||
      isNew ||
      !detailReady ||
      choiceWriteBusy ||
      busy ||
      confirmKind != null ||
      choiceEditOpen ||
      keywordDescriptionInflight.current || keywordLabelInflight.current ||
      descriptionInflight.current ||
      labelInflight.current ||
      valueInflight.current ||
      sequenceInflight.current ||
      addInflight.current ||
      removeInflight.current
    ) {
      return;
    }
    setKeywordDescriptionEditing(true);
    setKeywordDescriptionDraft(keywordDescriptionShown);
    setKeywordDescriptionError(null);
    setKeywordDescriptionNotice(null);
  }

  async function handleKeywordDescriptionSave(): Promise<void> {
    if (
      !id ||
      isNew ||
      !serverKeyword ||
      !detailReady ||
      !keywordDescriptionEditing ||
      keywordDescriptionInflight.current || keywordLabelInflight.current ||
      descriptionInflight.current ||
      labelInflight.current ||
      valueInflight.current ||
      sequenceInflight.current ||
      addInflight.current ||
      removeInflight.current ||
      addBusy ||
      removeBusy ||
      labelBusy ||
      valueBusy ||
      descriptionBusy ||
      sequenceBusy ||
      busy ||
      confirmKind
    ) {
      return;
    }
    const sent = keywordUpdateForKeywordDescription(serverKeyword, keywordDescriptionDraft);
    if (sent === "unchanged") {
      setKeywordDescriptionEditing(false);
      setKeywordDescriptionDraft("");
      setKeywordDescriptionError(null);
      return;
    }
    keywordDescriptionInflight.current = true;
    setKeywordDescriptionBusy(true);
    setKeywordDescriptionError(null);
    setKeywordDescriptionNotice(null);
    const previousShown = keywordDescriptionShown;
    try {
      const payload = await updateKeyword(id, sent);
      const accepted = savedKeywordDescription(sent, listedChoices, payload);
      if (accepted == null) {
        setKeywordDescriptionShown(previousShown);
        setDescription(previousShown);
        setKeywordDescriptionError(DEV_MSG.KW_SET_DESCRIPTION_ERROR);
        setKeywordDescriptionNotice(null);
        return;
      }
      setKeywordDescriptionShown(accepted);
      setDescription(accepted);
      setServerKeyword({ ...serverKeyword, description: accepted });
      setKeywordDescriptionEditing(false);
      setKeywordDescriptionDraft("");
      setKeywordDescriptionNotice(
        accepted ? DEV_MSG.KW_SET_DESCRIPTION_SAVED : DEV_MSG.KW_SET_DESCRIPTION_CLEARED,
      );
    } catch (err: unknown) {
      setKeywordDescriptionShown(previousShown);
      setDescription(previousShown);
      setKeywordDescriptionError(keywordDescriptionFailureMessage(err));
      setKeywordDescriptionNotice(null);
    } finally {
      keywordDescriptionInflight.current = false;
      setKeywordDescriptionBusy(false);
    }
  }

  function cancelKeywordLabelEdit(): void {
    if (keywordLabelBusy || keywordLabelInflight.current) return;
    setKeywordLabelEditing(false);
    setKeywordLabelDraft("");
    setKeywordLabelError(null);
  }

  function startKeywordLabelEdit(): void {
    if (
      !id ||
      isNew ||
      !detailReady ||
      choiceWriteBusy ||
      busy ||
      confirmKind != null ||
      choiceEditOpen ||
      keywordLabelInflight.current ||
      keywordDescriptionInflight.current ||
      descriptionInflight.current ||
      labelInflight.current ||
      valueInflight.current ||
      sequenceInflight.current ||
      addInflight.current ||
      removeInflight.current
    ) {
      return;
    }
    setKeywordLabelEditing(true);
    setKeywordLabelDraft(keywordLabelShown);
    setKeywordLabelError(null);
    setKeywordLabelNotice(null);
  }

  async function handleKeywordLabelSave(): Promise<void> {
    if (
      !id ||
      isNew ||
      !serverKeyword ||
      !detailReady ||
      !keywordLabelEditing ||
      keywordLabelInflight.current ||
      keywordDescriptionInflight.current ||
      descriptionInflight.current ||
      labelInflight.current ||
      valueInflight.current ||
      sequenceInflight.current ||
      addInflight.current ||
      removeInflight.current ||
      addBusy ||
      removeBusy ||
      labelBusy ||
      valueBusy ||
      descriptionBusy ||
      sequenceBusy ||
      keywordDescriptionBusy ||
      busy ||
      confirmKind
    ) {
      return;
    }
    const sent = keywordUpdateForKeywordLabel(serverKeyword, keywordLabelDraft);
    if (sent === "blank") {
      setKeywordLabelError(KW_LABEL_MSG.BLANK);
      setKeywordLabelNotice(null);
      return;
    }
    if (sent === "unchanged") {
      setKeywordLabelEditing(false);
      setKeywordLabelDraft("");
      setKeywordLabelError(null);
      return;
    }
    keywordLabelInflight.current = true;
    setKeywordLabelBusy(true);
    setKeywordLabelError(null);
    setKeywordLabelNotice(null);
    const previousShown = keywordLabelShown;
    try {
      const payload = await updateKeyword(id, sent);
      const accepted = savedKeywordLabel(sent, listedChoices, payload);
      if (accepted == null) {
        setKeywordLabelShown(previousShown);
        setLabel(previousShown);
        setKeywordLabelError(KW_LABEL_MSG.ERROR);
        setKeywordLabelNotice(null);
        return;
      }
      setKeywordLabelShown(accepted);
      setLabel(accepted);
      setServerKeyword({ ...serverKeyword, label: accepted });
      setKeywordLabelEditing(false);
      setKeywordLabelDraft("");
      setKeywordLabelNotice(KW_LABEL_MSG.SAVED);
    } catch (err: unknown) {
      setKeywordLabelShown(previousShown);
      setLabel(previousShown);
      setKeywordLabelError(keywordLabelFailureMessage(err));
      setKeywordLabelNotice(null);
    } finally {
      keywordLabelInflight.current = false;
      setKeywordLabelBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (
      !id ||
      isNew ||
      choiceWriteBusy ||
      confirmKind === "choice" ||
      confirmKind === "clear-description" ||
      choiceEditOpen
    ) {
      return;
    }
    captureDialogOpener(ev.currentTarget);
    setPendingRemoveIndex(null);
    setPendingClearDescriptionIndex(null);
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
        {isNew ? DEV_MSG.KW_NEW : `${DEV_MSG.KW_EDIT}: ${keywordLabelShown || id}`}
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
      {!isNew && id ? (
        <section
          data-testid="developer-kw-keyword-label"
          aria-label={KW_LABEL_MSG.ACTION}
          style={{ marginBottom: "16px" }}
        >
          <h3 style={{ marginBottom: "8px" }}>{KW_LABEL_MSG.ACTION}</h3>
          <p style={{ color: "#4a5568", marginTop: 0, fontSize: "0.9rem" }}>{KW_LABEL_MSG.HINT}</p>
          <p
            data-testid="developer-kw-keyword-label-text"
            data-keyword-label={keywordLabelShown}
            style={{ marginTop: 0 }}
          >
            {keywordLabelShown}
          </p>
          {keywordLabelError ? (
            <div role="alert" data-testid="developer-kw-keyword-label-error" style={errorAlert}>
              {keywordLabelError}
            </div>
          ) : null}
          {keywordLabelNotice ? (
            <div data-testid="developer-kw-keyword-label-notice" style={{ color: "#276749" }}>
              {keywordLabelNotice}
            </div>
          ) : null}
          {keywordLabelEditing ? (
            <div data-testid="developer-kw-keyword-label-editor">
              <label htmlFor="kw-keyword-label-input">{KW_LABEL_MSG.FIELD}</label>
              <input
                id="kw-keyword-label-input"
                data-testid="developer-kw-keyword-label-input"
                style={inputStyle}
                value={keywordLabelDraft}
                onChange={(e) => setKeywordLabelDraft(e.target.value)}
              />
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginTop: "8px" }}>
                <button
                  type="button"
                  data-testid="developer-kw-keyword-label-save"
                  aria-label={KW_LABEL_MSG.SAVE}
                  disabled={keywordLabelBusy}
                  onClick={() => void handleKeywordLabelSave()}
                  style={{
                    padding: "8px 16px",
                    background: catalogColors.accent,
                    color: "#fff",
                    border: "none",
                    borderRadius: "4px",
                    cursor: keywordLabelBusy ? "wait" : "pointer",
                  }}
                >
                  {KW_LABEL_MSG.SAVE}
                </button>
                <button
                  type="button"
                  data-testid="developer-kw-keyword-label-cancel"
                  disabled={keywordLabelBusy}
                  onClick={cancelKeywordLabelEdit}
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
          ) : (
            <button
              type="button"
              data-testid="developer-kw-keyword-label-edit"
              aria-label={KW_LABEL_MSG.ACTION}
              disabled={
                choiceWriteBusy ||
                busy ||
                !detailReady ||
                confirmKind != null ||
                labelEditOpen ||
                valueEditOpen ||
                descriptionEditOpen ||
                sequenceEditOpen ||
                keywordDescriptionEditing
              }
              onClick={startKeywordLabelEdit}
              style={{
                padding: "4px 10px",
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                cursor: choiceWriteBusy ? "wait" : "pointer",
              }}
            >
              {KW_LABEL_MSG.ACTION}
            </button>
          )}
        </section>
      ) : null}
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
      {!isNew && id ? (
        <section
          data-testid="developer-kw-keyword-description"
          aria-label={DEV_MSG.KW_SET_DESCRIPTION_ACTION}
          style={{ marginBottom: "16px" }}
        >
          <h3 style={{ marginBottom: "8px" }}>{DEV_MSG.KW_SET_DESCRIPTION}</h3>
          <p style={{ color: "#4a5568", marginTop: 0, fontSize: "0.9rem" }}>
            {DEV_MSG.KW_SET_DESCRIPTION_HINT}
          </p>
          <p
            data-testid="developer-kw-keyword-description-text"
            data-keyword-description={keywordDescriptionShown}
            style={{ marginTop: 0 }}
          >
            {keywordDescriptionShown}
          </p>
          {keywordDescriptionError ? (
            <div
              role="alert"
              data-testid="developer-kw-keyword-description-error"
              style={errorAlert}
            >
              {keywordDescriptionError}
            </div>
          ) : null}
          {keywordDescriptionNotice ? (
            <div
              data-testid="developer-kw-keyword-description-notice"
              style={{ color: "#276749" }}
            >
              {keywordDescriptionNotice}
            </div>
          ) : null}
          {keywordDescriptionEditing ? (
            <div data-testid="developer-kw-keyword-description-editor">
              <label htmlFor="kw-keyword-description-input">
                {DEV_MSG.KW_SET_DESCRIPTION_FIELD}
              </label>
              <input
                id="kw-keyword-description-input"
                data-testid="developer-kw-keyword-description-input"
                style={inputStyle}
                value={keywordDescriptionDraft}
                onChange={(e) => setKeywordDescriptionDraft(e.target.value)}
              />
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginTop: "8px" }}>
                <button
                  type="button"
                  data-testid="developer-kw-keyword-description-save"
                  aria-label={DEV_MSG.KW_SET_DESCRIPTION_SAVE}
                  disabled={keywordDescriptionBusy}
                  onClick={() => void handleKeywordDescriptionSave()}
                  style={{
                    padding: "8px 16px",
                    background: catalogColors.accent,
                    color: "#fff",
                    border: "none",
                    borderRadius: "4px",
                    cursor: keywordDescriptionBusy ? "wait" : "pointer",
                  }}
                >
                  {DEV_MSG.KW_SET_DESCRIPTION_SAVE}
                </button>
                <button
                  type="button"
                  data-testid="developer-kw-keyword-description-cancel"
                  disabled={keywordDescriptionBusy}
                  onClick={cancelKeywordDescriptionEdit}
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
          ) : (
            <button
              type="button"
              data-testid="developer-kw-keyword-description-edit"
              aria-label={DEV_MSG.KW_SET_DESCRIPTION_ACTION}
              disabled={
                choiceWriteBusy ||
                busy ||
                !detailReady ||
                confirmKind != null ||
                labelEditOpen ||
                valueEditOpen ||
                descriptionEditOpen ||
                sequenceEditOpen ||
                keywordLabelEditing
              }
              onClick={startKeywordDescriptionEdit}
              style={{
                padding: "4px 10px",
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                cursor: choiceWriteBusy ? "wait" : "pointer",
              }}
            >
              {DEV_MSG.KW_SET_DESCRIPTION}
            </button>
          )}
        </section>
      ) : null}
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
          <p style={{ color: "#4a5568", marginTop: 0, fontSize: "0.9rem" }}>
            {DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_HINT}
          </p>
          <p style={{ color: "#4a5568", marginTop: 0, fontSize: "0.9rem" }}>
            {DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_HINT}
          </p>
          <p style={{ color: "#4a5568", marginTop: 0, fontSize: "0.9rem" }}>
            {DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_HINT}
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
          {descriptionError ? (
            <div
              role="alert"
              data-testid="developer-kw-choice-description-error"
              style={errorAlert}
            >
              {descriptionError}
            </div>
          ) : null}
          {descriptionNotice ? (
            <div data-testid="developer-kw-choice-description-notice" style={{ color: "#276749" }}>
              {descriptionNotice}
            </div>
          ) : null}
          {clearDescriptionError ? (
            <div
              role="alert"
              data-testid="developer-kw-choice-description-clear-error"
              style={errorAlert}
            >
              {clearDescriptionError}
            </div>
          ) : null}
          {clearDescriptionNotice ? (
            <div
              data-testid="developer-kw-choice-description-clear-notice"
              style={{ color: "#276749" }}
            >
              {clearDescriptionNotice}
            </div>
          ) : null}
          {sequenceError ? (
            <div role="alert" data-testid="developer-kw-choice-sequence-error" style={errorAlert}>
              {sequenceError}
            </div>
          ) : null}
          {sequenceNotice ? (
            <div data-testid="developer-kw-choice-sequence-notice" style={{ color: "#276749" }}>
              {sequenceNotice}
            </div>
          ) : null}
          {listedChoices.length === 0 ? (
            <p data-testid="developer-kw-choices-empty">{DEV_MSG.KW_CHOICES_EMPTY}</p>
          ) : (
            <ul data-testid="developer-kw-saved-choices" style={{ paddingLeft: "1.2rem" }}>
              {listedChoices.map((choice, index) => {
                const choiceLabel = choice.label || "";
                const choiceValue = choice.value || "";
                const choiceDescription = choiceDescriptionText(choice);
                const choiceSequence = choice.sequence != null ? String(choice.sequence) : "";
                return (
                  <li
                    key={`${choiceLabel}-${choiceValue}-${choice.sequence ?? index}`}
                    data-testid="developer-kw-choice"
                    data-choice-label={choiceLabel}
                    data-choice-value={choiceValue}
                    data-choice-description={choiceDescription}
                    data-choice-sequence={choiceSequence}
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
                    <span data-testid="developer-kw-choice-description-text">
                      {choiceDescription ? ` — ${choiceDescription}` : ""}
                    </span>
                    <span data-testid="developer-kw-choice-sequence-text">
                      {choiceSequence ? ` [${choiceSequence}]` : ""}
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
                      data-testid="developer-kw-choice-description-edit"
                      data-choice-label={choiceLabel}
                      aria-label={DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_ACTION.replace(
                        "{0}",
                        choiceLabel || String(index + 1),
                      )}
                      disabled={
                        choiceWriteBusy || busy || !detailReady || confirmKind != null || choiceEditOpen
                      }
                      onClick={() => startChoiceDescriptionEdit(index)}
                      style={{
                        padding: "4px 10px",
                        background: "transparent",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        cursor: choiceWriteBusy ? "wait" : "pointer",
                      }}
                    >
                      {DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-kw-choice-description-clear"
                      data-choice-label={choiceLabel}
                      aria-label={DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_ACTION.replace(
                        "{0}",
                        choiceLabel || String(index + 1),
                      )}
                      disabled={
                        choiceWriteBusy || busy || !detailReady || confirmKind != null || choiceEditOpen
                      }
                      onClick={(ev) => requestClearChoiceDescription(index, ev)}
                      style={{
                        padding: "4px 10px",
                        background: "transparent",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        cursor: choiceWriteBusy ? "wait" : "pointer",
                      }}
                    >
                      {DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-kw-choice-sequence-edit"
                      data-choice-label={choiceLabel}
                      aria-label={DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_ACTION.replace(
                        "{0}",
                        choiceLabel || String(index + 1),
                      )}
                      disabled={
                        choiceWriteBusy || busy || !detailReady || confirmKind != null || choiceEditOpen
                      }
                      onClick={() => startChoiceSequenceEdit(index)}
                      style={{
                        padding: "4px 10px",
                        background: "transparent",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        cursor: choiceWriteBusy ? "wait" : "pointer",
                      }}
                    >
                      {DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE}
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
          {descriptionEditOpen ? (
            <div
              data-testid="developer-kw-choice-description-editor"
              style={{ marginBottom: "16px" }}
            >
              <div style={fieldStyle}>
                <label htmlFor="kw-choice-description-input">
                  {DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_FIELD}
                </label>
                <input
                  id="kw-choice-description-input"
                  data-testid="developer-kw-choice-description-input"
                  style={inputStyle}
                  value={descriptionDraft}
                  onChange={(e) => setDescriptionDraft(e.target.value)}
                />
              </div>
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                <button
                  type="button"
                  data-testid="developer-kw-choice-description-save"
                  aria-label={DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_SAVE}
                  disabled={choiceWriteBusy || busy || !detailReady || confirmKind != null}
                  onClick={() => void handleChoiceDescriptionSave()}
                  style={{
                    padding: "8px 16px",
                    background: catalogColors.accent,
                    color: "#fff",
                    border: "none",
                    borderRadius: "4px",
                    cursor: descriptionBusy ? "wait" : "pointer",
                  }}
                >
                  {DEV_MSG.KW_CHANGE_CHOICE_DESCRIPTION_SAVE}
                </button>
                <button
                  type="button"
                  data-testid="developer-kw-choice-description-cancel"
                  disabled={descriptionBusy}
                  onClick={cancelChoiceDescriptionEdit}
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
          {sequenceEditOpen ? (
            <div data-testid="developer-kw-choice-sequence-editor" style={{ marginBottom: "16px" }}>
              <div style={fieldStyle}>
                <label htmlFor="kw-choice-sequence-input">
                  {DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_FIELD}
                </label>
                <input
                  id="kw-choice-sequence-input"
                  data-testid="developer-kw-choice-sequence-input"
                  style={inputStyle}
                  inputMode="numeric"
                  value={sequenceDraft}
                  onChange={(e) => setSequenceDraft(e.target.value)}
                />
              </div>
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                <button
                  type="button"
                  data-testid="developer-kw-choice-sequence-save"
                  aria-label={DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_SAVE}
                  disabled={choiceWriteBusy || busy || !detailReady || confirmKind != null}
                  onClick={() => void handleChoiceSequenceSave()}
                  style={{
                    padding: "8px 16px",
                    background: catalogColors.accent,
                    color: "#fff",
                    border: "none",
                    borderRadius: "4px",
                    cursor: sequenceBusy ? "wait" : "pointer",
                  }}
                >
                  {DEV_MSG.KW_CHANGE_CHOICE_SEQUENCE_SAVE}
                </button>
                <button
                  type="button"
                  data-testid="developer-kw-choice-sequence-cancel"
                  disabled={sequenceBusy}
                  onClick={cancelChoiceSequenceEdit}
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
            disabled={
              busy ||
              choiceWriteBusy ||
              confirmKind === "choice" ||
              confirmKind === "clear-description" ||
              choiceEditOpen
            }
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
        busy={busy || removeBusy || descriptionBusy}
        message={
          confirmKind === "choice"
            ? DEV_MSG.KW_REMOVE_CHOICE_CONFIRM.replace(
                "{0}",
                pendingRemoveIndex != null
                  ? listedChoices[pendingRemoveIndex]?.label || ""
                  : "",
              )
            : confirmKind === "clear-description"
              ? DEV_MSG.KW_CLEAR_CHOICE_DESCRIPTION_CONFIRM.replace(
                  "{0}",
                  pendingClearDescriptionIndex != null
                    ? listedChoices[pendingClearDescriptionIndex]?.label || ""
                    : "",
                )
              : DEV_MSG.KW_DELETE_CONFIRM
        }
        onCancel={cancelConfirm}
        onConfirm={() => {
          if (confirmKind === "choice") {
            void handleRemoveChoice();
          } else if (confirmKind === "clear-description") {
            void handleClearChoiceDescription();
          } else {
            void handleDelete();
          }
        }}
      />
    </div>
  );
}
