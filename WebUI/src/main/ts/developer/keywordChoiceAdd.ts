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
