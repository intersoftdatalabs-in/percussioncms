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

import type { KeywordChoiceSummary, KeywordSummary } from "../api/developer/types";

/**
 * JAXB/Jackson writes one choice as an object and several choices as an array
 * (sometimes under a KeywordChoice key). Callers always see a list.
 */
export function asKeywordChoices(raw: unknown): KeywordChoiceSummary[] {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    const out: KeywordChoiceSummary[] = [];
    for (const item of raw) {
      out.push(...asKeywordChoices(item));
    }
    return out;
  }
  if (typeof raw !== "object") {
    return [];
  }
  const record = raw as Record<string, unknown>;
  const nested = record.KeywordChoice ?? record.keywordChoice;
  if (nested != null) {
    return asKeywordChoices(nested);
  }
  if ("label" in record || "value" in record) {
    return [raw as KeywordChoiceSummary];
  }
  return [];
}

function withChoiceList(keyword: KeywordSummary): KeywordSummary {
  return { ...keyword, choices: asKeywordChoices(keyword.choices) };
}

/**
 * Accept a flat keyword or a `{ Keyword }` object envelope.
 * Kept local so editor tests can mock the API module without stripping this helper.
 */
export function unwrapKeywordPayload(payload: unknown): KeywordSummary | null {
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    return null;
  }
  const record = payload as Record<string, unknown>;
  const nested = record.Keyword ?? record.keyword;
  if (nested && typeof nested === "object" && !Array.isArray(nested)) {
    return withChoiceList(nested as KeywordSummary);
  }
  if (
    "label" in record ||
    "choices" in record ||
    "description" in record ||
    "guid" in record ||
    "sequence" in record
  ) {
    return withChoiceList(payload as KeywordSummary);
  }
  return null;
}

/** Draft fields for adding one choice. Value may be blank; it then defaults to the label. */
export interface KeywordChoiceDraft {
  label: string;
  value: string;
}

export type AddChoiceRejection = "blank" | "duplicate";

export interface PreparedKeywordChoice {
  label: string;
  value: string;
  sequence: number;
}

function sameOptionalText(left?: string | null, right?: string | null): boolean {
  if (left == null && right == null) {
    return true;
  }
  return left === right;
}

function sameOptionalNumber(left?: number | null, right?: number | null): boolean {
  if (left == null && right == null) {
    return true;
  }
  return left === right;
}

function choiceSnapshot(choice: KeywordChoiceSummary): string {
  return [
    choice.label ?? "",
    choice.value ?? "",
    choice.description ?? "",
    choice.sequence == null ? "" : String(choice.sequence),
  ].join("\u0000");
}

function effectiveValue(choice: { label?: string; value?: string }): string {
  const value = (choice.value ?? "").trim();
  if (value) {
    return value.toLowerCase();
  }
  return (choice.label ?? "").trim().toLowerCase();
}

/** True when the draft has no label. A blank choice must not be written. */
export function isBlankChoiceDraft(draft: KeywordChoiceDraft): boolean {
  return draft.label.trim().length === 0;
}

/**
 * True when the draft label or value matches an existing choice, ignoring case.
 * An empty value is compared as the label.
 */
export function isDuplicateChoice(
  existing: KeywordChoiceSummary[],
  draft: KeywordChoiceDraft,
): boolean {
  const label = draft.label.trim().toLowerCase();
  if (!label) {
    return false;
  }
  const value = effectiveValue(draft);
  return existing.some((choice) => {
    const choiceLabel = (choice.label ?? "").trim().toLowerCase();
    return choiceLabel === label || effectiveValue(choice) === value;
  });
}

function nextSequence(existing: KeywordChoiceSummary[]): number {
  let max = -1;
  for (const choice of existing) {
    if (typeof choice.sequence === "number" && Number.isFinite(choice.sequence)) {
      max = Math.max(max, choice.sequence);
    }
  }
  return max + 1;
}

function choiceForUpdate(choice: KeywordChoiceSummary): KeywordChoiceSummary {
  return {
    label: choice.label ?? "",
    value: choice.value ?? "",
    description: choice.description,
    sequence: choice.sequence != null ? choice.sequence : 0,
  };
}

export type RemoveChoiceRejection = "missing";

export type RelabelChoiceRejection = "blank" | "duplicate" | "missing" | "unchanged";

export type RevalueChoiceRejection = "blank" | "duplicate" | "missing" | "unchanged";

export type DescribeChoiceRejection = "blank" | "missing" | "unchanged";

export type ClearChoiceDescriptionRejection = "missing" | "unchanged";

export type ResequenceChoiceRejection = "blank" | "invalid" | "missing" | "unchanged";

/** Java {@code Integer.MAX_VALUE}. The server stores choice sequence as an Integer >= 0. */
const CHOICE_SEQUENCE_MAX = 2_147_483_647;

function storedChoiceDescription(choice: KeywordChoiceSummary): string {
  return (choice.description ?? "").trim();
}

/**
 * Body for the existing keyword update that changes one choice label.
 * Keyword label, description, and sequence are copied from the loaded keyword.
 * That choice keeps its value, description, and sequence. The other choices
 * stay. A blank label is not a body. The same label is not a write. A label
 * that matches another choice, ignoring case, is not a body, so that choice
 * is not replaced.
 */
export function keywordUpdateForRelabeledChoice(
  baseline: Pick<KeywordSummary, "label" | "description" | "sequence">,
  existing: KeywordChoiceSummary[],
  index: number,
  nextLabel: string,
): KeywordSummary | RelabelChoiceRejection {
  if (!Number.isInteger(index) || index < 0 || index >= existing.length) {
    return "missing";
  }
  const label = nextLabel.trim();
  if (!label) {
    return "blank";
  }
  const current = (existing[index]?.label ?? "").trim();
  if (current === label) {
    return "unchanged";
  }
  const lower = label.toLowerCase();
  const duplicate = existing.some((choice, i) => {
    if (i === index) {
      return false;
    }
    return (choice.label ?? "").trim().toLowerCase() === lower;
  });
  if (duplicate) {
    return "duplicate";
  }
  return {
    label: baseline.label,
    description: baseline.description,
    sequence: baseline.sequence,
    choices: existing.map((choice, i) => {
      const copy = choiceForUpdate(choice);
      if (i === index) {
        return { ...copy, label };
      }
      return copy;
    }),
  };
}

/**
 * Value stored for one choice. A blank draft is the choice label, same as add.
 */
function storedChoiceValue(choice: KeywordChoiceSummary, nextValue: string): string {
  const raw = nextValue.trim();
  if (raw) {
    return raw;
  }
  return (choice.label ?? "").trim();
}

/**
 * Body for the existing keyword update that changes one choice value.
 * Keyword label, description, and sequence are copied from the loaded keyword.
 * That choice keeps its label, description, and sequence. The other choices
 * stay. A blank value is compared and stored as the choice label. The same
 * value is not a write. A blank value that already means the label is not a
 * write. A value that matches another choice, ignoring case, is not a body,
 * so that choice is not replaced. An empty label and an empty value are not
 * a body.
 */
export function keywordUpdateForRevaluedChoice(
  baseline: Pick<KeywordSummary, "label" | "description" | "sequence">,
  existing: KeywordChoiceSummary[],
  index: number,
  nextValue: string,
): KeywordSummary | RevalueChoiceRejection {
  if (!Number.isInteger(index) || index < 0 || index >= existing.length) {
    return "missing";
  }
  const currentChoice = existing[index];
  if (!currentChoice) {
    return "missing";
  }
  const value = storedChoiceValue(currentChoice, nextValue);
  if (!value) {
    return "blank";
  }
  const currentStored = (currentChoice.value ?? "").trim();
  if (currentStored === value) {
    return "unchanged";
  }
  if (!nextValue.trim() && effectiveValue(currentChoice) === value.toLowerCase()) {
    return "unchanged";
  }
  const nextEffective = value.toLowerCase();
  const duplicate = existing.some((choice, i) => {
    if (i === index) {
      return false;
    }
    return effectiveValue(choice) === nextEffective;
  });
  if (duplicate) {
    return "duplicate";
  }
  return {
    label: baseline.label,
    description: baseline.description,
    sequence: baseline.sequence,
    choices: existing.map((choice, i) => {
      const copy = choiceForUpdate(choice);
      if (i === index) {
        return { ...copy, value };
      }
      return copy;
    }),
  };
}

/**
 * Body for the existing keyword update that sets one choice description.
 * Keyword label, description, and sequence are copied from the loaded keyword.
 * That choice keeps its label, value, and sequence. The other choices stay.
 * A blank description is not a body, so it does not clear a stored description.
 * The same description is not a write. Descriptions are not unique across choices.
 */
export function keywordUpdateForDescribedChoice(
  baseline: Pick<KeywordSummary, "label" | "description" | "sequence">,
  existing: KeywordChoiceSummary[],
  index: number,
  nextDescription: string,
): KeywordSummary | DescribeChoiceRejection {
  if (!Number.isInteger(index) || index < 0 || index >= existing.length) {
    return "missing";
  }
  const currentChoice = existing[index];
  if (!currentChoice) {
    return "missing";
  }
  const description = nextDescription.trim();
  const current = storedChoiceDescription(currentChoice);
  if (!description) {
    return current ? "blank" : "unchanged";
  }
  if (description === current) {
    return "unchanged";
  }
  return {
    label: baseline.label,
    description: baseline.description,
    sequence: baseline.sequence,
    choices: existing.map((choice, i) => {
      const copy = choiceForUpdate(choice);
      if (i === index) {
        return { ...copy, description };
      }
      return copy;
    }),
  };
}

/**
 * Body for the existing keyword update that clears one choice description.
 * Keyword label, description, and sequence are copied from the loaded keyword.
 * That choice keeps its label, value, and sequence, and its description is an
 * empty string so the update clears it. The other choices stay. A choice that
 * already has no description is not a write. This is not the set-description
 * action: that action still refuses a blank description.
 */
export function keywordUpdateForClearedChoiceDescription(
  baseline: Pick<KeywordSummary, "label" | "description" | "sequence">,
  existing: KeywordChoiceSummary[],
  index: number,
): KeywordSummary | ClearChoiceDescriptionRejection {
  if (!Number.isInteger(index) || index < 0 || index >= existing.length) {
    return "missing";
  }
  const currentChoice = existing[index];
  if (!currentChoice) {
    return "missing";
  }
  if (!storedChoiceDescription(currentChoice)) {
    return "unchanged";
  }
  return {
    label: baseline.label,
    description: baseline.description,
    sequence: baseline.sequence,
    choices: existing.map((choice, i) => {
      const copy = choiceForUpdate(choice);
      if (i === index) {
        return { ...copy, description: "" };
      }
      return copy;
    }),
  };
}

/**
 * Whole number from a choice-sequence draft. Blank and non-integers are not
 * numbers. Leading and trailing space is ignored. {@code 03} is {@code 3}.
 * A negative number, a fraction, or a value above {@code Integer.MAX_VALUE}
 * is not a sequence, so the caller does not write it.
 */
function parseChoiceSequence(raw: string): number | "blank" | "invalid" {
  const text = raw.replace(/[\r\n]+/g, " ").trim();
  if (!text) {
    return "blank";
  }
  if (!/^\d+$/.test(text)) {
    return "invalid";
  }
  const value = Number(text);
  if (!Number.isSafeInteger(value) || value > CHOICE_SEQUENCE_MAX) {
    return "invalid";
  }
  return value;
}

function storedChoiceSequence(choice: KeywordChoiceSummary): number | null {
  const sequence = choice.sequence;
  if (typeof sequence !== "number" || !Number.isInteger(sequence)) {
    return null;
  }
  if (sequence < 0 || sequence > CHOICE_SEQUENCE_MAX) {
    return null;
  }
  return sequence;
}

/**
 * Body for the existing keyword update that changes one choice sequence.
 * Keyword label, description, and sequence are copied from the loaded keyword.
 * That choice keeps its label, value, and description. The other choices stay.
 * A blank sequence is not a body. A non-integer, a negative number, or a
 * value above {@code Integer.MAX_VALUE} is not a body. The same sequence is
 * not a write. Sequences are not unique across choices.
 */
export function keywordUpdateForResequencedChoice(
  baseline: Pick<KeywordSummary, "label" | "description" | "sequence">,
  existing: KeywordChoiceSummary[],
  index: number,
  nextSequenceText: string,
): KeywordSummary | ResequenceChoiceRejection {
  if (!Number.isInteger(index) || index < 0 || index >= existing.length) {
    return "missing";
  }
  const currentChoice = existing[index];
  if (!currentChoice) {
    return "missing";
  }
  const parsed = parseChoiceSequence(nextSequenceText);
  if (parsed === "blank" || parsed === "invalid") {
    return parsed;
  }
  if (storedChoiceSequence(currentChoice) === parsed) {
    return "unchanged";
  }
  return {
    label: baseline.label,
    description: baseline.description,
    sequence: baseline.sequence,
    choices: existing.map((choice, i) => {
      const copy = choiceForUpdate(choice);
      if (i === index) {
        return { ...copy, sequence: parsed };
      }
      return copy;
    }),
  };
}

/**
 * Body for the existing keyword update that drops one choice by list index.
 * Keyword label, description, and sequence are copied from the loaded keyword.
 * The other choices keep their label, value, description, and sequence.
 * An empty list is a valid body: it clears choices and does not delete the keyword.
 * Returns a rejection instead of a body when the index is not a current choice.
 */
export function keywordUpdateForRemovedChoice(
  baseline: Pick<KeywordSummary, "label" | "description" | "sequence">,
  existing: KeywordChoiceSummary[],
  index: number,
): KeywordSummary | RemoveChoiceRejection {
  if (!Number.isInteger(index) || index < 0 || index >= existing.length) {
    return "missing";
  }
  return {
    label: baseline.label,
    description: baseline.description,
    sequence: baseline.sequence,
    choices: existing.filter((_, i) => i !== index).map(choiceForUpdate),
  };
}

/**
 * Body for the existing keyword update that appends one choice.
 * Keyword label, description, and sequence are copied from the loaded keyword.
 * Returns a rejection instead of a body when the draft must not be written.
 */
export function keywordUpdateForAddedChoice(
  baseline: Pick<KeywordSummary, "label" | "description" | "sequence">,
  existing: KeywordChoiceSummary[],
  draft: KeywordChoiceDraft,
): KeywordSummary | AddChoiceRejection {
  if (isBlankChoiceDraft(draft)) {
    return "blank";
  }
  if (isDuplicateChoice(existing, draft)) {
    return "duplicate";
  }
  const added: PreparedKeywordChoice = {
    label: draft.label.trim(),
    value: draft.value.trim() || draft.label.trim(),
    sequence: nextSequence(existing),
  };
  return {
    label: baseline.label,
    description: baseline.description,
    sequence: baseline.sequence,
    choices: [
      ...existing.map(choiceForUpdate),
      { label: added.label, value: added.value, sequence: added.sequence },
    ],
  };
}

/**
 * Choices to show after a successful add, or null when the response must not
 * replace the previous list (metadata changed, a previous choice is missing,
 * or the new choice is not present exactly once). An empty sent list is
 * accepted only when the response is also empty, so removing the last choice
 * can clear the list without treating a partial body as success.
 */
export function savedChoicesAfterAdd(
  sent: KeywordSummary,
  payload: unknown,
): KeywordChoiceSummary[] | null {
  const saved = unwrapKeywordPayload(payload);
  if (!saved) {
    return null;
  }
  const sentChoices = sent.choices ?? [];
  if (!sameOptionalText(sent.label ?? null, saved.label ?? null)) {
    return null;
  }
  if (!sameOptionalText(sent.description ?? null, saved.description ?? null)) {
    return null;
  }
  if (!sameOptionalNumber(sent.sequence ?? null, saved.sequence ?? null)) {
    return null;
  }
  const next = saved.choices ?? [];
  if (next.length !== sentChoices.length) {
    return null;
  }
  if (sentChoices.length === 0) {
    return next;
  }
  const remaining = new Map<string, number>();
  for (const choice of next) {
    const key = choiceSnapshot(choice);
    remaining.set(key, (remaining.get(key) ?? 0) + 1);
  }
  for (const choice of sentChoices) {
    const key = choiceSnapshot(choice);
    const count = remaining.get(key) ?? 0;
    if (count < 1) {
      return null;
    }
    if (count === 1) {
      remaining.delete(key);
    } else {
      remaining.set(key, count - 1);
    }
  }
  return remaining.size === 0 ? next : null;
}

export type KeywordDescriptionRejection = "unchanged";

/** Stored keyword description with surrounding space and line breaks removed. */
export function storedKeywordDescription(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

function sameChoiceList(
  left: KeywordChoiceSummary[],
  right: KeywordChoiceSummary[],
): boolean {
  if (left.length !== right.length) {
    return false;
  }
  const remaining = new Map<string, number>();
  for (const choice of right) {
    const key = choiceSnapshot(choice);
    remaining.set(key, (remaining.get(key) ?? 0) + 1);
  }
  for (const choice of left) {
    const key = choiceSnapshot(choice);
    const count = remaining.get(key) ?? 0;
    if (count < 1) {
      return false;
    }
    if (count === 1) {
      remaining.delete(key);
    } else {
      remaining.set(key, count - 1);
    }
  }
  return remaining.size === 0;
}

/**
 * Body for the existing keyword update that sets the keyword description.
 * Label and sequence are copied from the loaded keyword. Choices are omitted
 * so stored choices stay. A blank description is an empty string and clears
 * a stored description. The same description is not a write. This is not a
 * choice description.
 */
export function keywordUpdateForKeywordDescription(
  baseline: Pick<KeywordSummary, "label" | "description" | "sequence">,
  nextDescription: string,
): KeywordSummary | KeywordDescriptionRejection {
  const description = storedKeywordDescription(nextDescription);
  const current = storedKeywordDescription(baseline.description);
  if (description === current) {
    return "unchanged";
  }
  return {
    label: baseline.label,
    description,
    sequence: baseline.sequence,
  };
}

/**
 * Description to show after a keyword-description update, or null when the
 * response must not replace the previous description (metadata changed, the
 * description is not the one sent, or stored choices are not the previous
 * list). The sent body must omit choices.
 */
export function savedKeywordDescription(
  sent: KeywordSummary,
  previousChoices: KeywordChoiceSummary[],
  payload: unknown,
): string | null {
  if ("choices" in sent) {
    return null;
  }
  const saved = unwrapKeywordPayload(payload);
  if (!saved) {
    return null;
  }
  if (!sameOptionalText(sent.label ?? null, saved.label ?? null)) {
    return null;
  }
  if (!sameOptionalNumber(sent.sequence ?? null, saved.sequence ?? null)) {
    return null;
  }
  const sentDescription = storedKeywordDescription(sent.description);
  if (storedKeywordDescription(saved.description) !== sentDescription) {
    return null;
  }
  if (!sameChoiceList(previousChoices, saved.choices ?? [])) {
    return null;
  }
  return sentDescription;
}

export type KeywordLabelRejection = "blank" | "unchanged";

/** Stored keyword label with surrounding space and line breaks removed. */
export function storedKeywordLabel(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Body for the existing keyword update that changes the keyword label.
 * Description is copied when the loaded keyword has one, and sequence is
 * copied. Choices are omitted so stored choices stay. A blank label is not a
 * write. The same label is not a write. This is not a choice label.
 */
export function keywordUpdateForKeywordLabel(
  baseline: Pick<KeywordSummary, "label" | "description" | "sequence">,
  nextLabel: string,
): KeywordSummary | KeywordLabelRejection {
  const label = storedKeywordLabel(nextLabel);
  if (!label) {
    return "blank";
  }
  if (label === storedKeywordLabel(baseline.label)) {
    return "unchanged";
  }
  const body: KeywordSummary = {
    label,
    sequence: baseline.sequence,
  };
  if (baseline.description != null) {
    body.description = baseline.description;
  }
  return body;
}

/**
 * Label to show after a keyword-label update, or null when the response must
 * not replace the previous label (metadata changed, the label is not the one
 * sent, or stored choices are not the previous list). The sent body must omit
 * choices.
 */
export function savedKeywordLabel(
  sent: KeywordSummary,
  previousChoices: KeywordChoiceSummary[],
  payload: unknown,
): string | null {
  if ("choices" in sent) {
    return null;
  }
  const saved = unwrapKeywordPayload(payload);
  if (!saved) {
    return null;
  }
  const sentLabel = storedKeywordLabel(sent.label);
  if (!sentLabel || storedKeywordLabel(saved.label) !== sentLabel) {
    return null;
  }
  if ("description" in sent) {
    if (!sameOptionalText(sent.description ?? null, saved.description ?? null)) {
      return null;
    }
  } else if (storedKeywordDescription(saved.description) !== "") {
    return null;
  }
  if (!sameOptionalNumber(sent.sequence ?? null, saved.sequence ?? null)) {
    return null;
  }
  if (!sameChoiceList(previousChoices, saved.choices ?? [])) {
    return null;
  }
  return sentLabel;
}
