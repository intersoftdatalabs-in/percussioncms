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

import { extractRestErrorMessage, isApiError } from "../api/client";
import type { TemplateBindingSummary } from "../api/developer/types";
import { isValidAssemblerValue } from "./assemblerOptions";
import {
  slotRowIndexesForServerError,
  validateSlotRows,
  type SlotRowIssue,
} from "./slotMappingValidation";
import { validateBindings } from "./templateBindings";
import type { SlotEditorRow } from "./TemplateSlotsPanel";

/**
 * Save-flow helpers for the Design SPA template editor (#6566 / parent #2626).
 *
 * Extracted from {@code TemplateSourceEditor#handleSave} so the component
 * keeps a linear orchestrate-then-apply shape while every branch below stays
 * small enough for the complexity.cognitive gate. All helpers are pure (no
 * React state); the component applies their results to banners/rows.
 */

/** Outcome of the client-side pre-save validation pass. */
export type SavePrecheck =
  | { ok: true }
  | {
      ok: false;
      validationError: string;
      /**
       * Per-row inline errors aligned by row index, or null when the failure
       * leaves row errors untouched (assembler / bindings failures).
       */
      rowErrors: Array<string | null> | null;
    };

/**
 * Client-side validation before any lock/PUT: assembler, then bindings, then
 * slot-mapping rows. Returns the first failure; callers surface
 * {@code validationError} on the banner and {@code rowErrors} inline.
 */
export function precheckSaveInputs(
  assembler: string,
  bindings: TemplateBindingSummary[],
  slotRows: SlotEditorRow[],
): SavePrecheck {
  if (!isValidAssemblerValue(assembler)) {
    return { ok: false, validationError: "Assembler is required", rowErrors: null };
  }
  const bindingsError = validateBindings(bindings);
  if (bindingsError) {
    return { ok: false, validationError: bindingsError, rowErrors: null };
  }
  const slotIssues = validateSlotRows(slotRows);
  if (slotIssues.length === 0) {
    return { ok: true };
  }
  const first = slotIssues[0].message;
  return {
    ok: false,
    validationError:
      slotIssues.length === 1
        ? first
        : `${slotIssues.length} slot rows need attention: ${first}`,
    rowErrors: firstIssuePerRow(slotRows, slotIssues),
  };
}

/** Keep the first issue per row; rows without issues stay null. */
export function firstIssuePerRow(
  rows: SlotEditorRow[],
  issues: SlotRowIssue[],
): Array<string | null> {
  const byIndex: Array<string | null> = rows.map(() => null);
  for (const issue of issues) {
    if (byIndex[issue.index] == null) {
      byIndex[issue.index] = issue.message;
    }
  }
  return byIndex;
}

/**
 * Pure merge for a failed slot PUT: preserve prior inline row errors and stamp
 * the failure message on the offending row.
 */
export function mergeSlotPutRowError(
  prev: ReadonlyArray<string | null>,
  rowCount: number,
  rowIndex: number,
  message: string,
): Array<string | null> {
  const next: Array<string | null> = Array.from({ length: rowCount }, () => null);
  for (let j = 0; j < next.length && j < prev.length; j++) {
    const prior: string | null | undefined = prev[j];
    if (prior) {
      next[j] = prior;
    }
  }
  next[rowIndex] = message;
  return next;
}

/**
 * Refresh slot-row baselines after successful PUTs so the editor reads clean.
 * Rows without a matching PUT are returned untouched.
 */
export function withRefreshedSlotBaselines(
  rows: SlotEditorRow[],
  puts: Array<{ key: string }>,
): SlotEditorRow[] {
  return rows.map((r) => {
    const put = puts.find((p) => p.key === r.key);
    if (!put) return r;
    return {
      ...r,
      initialLayout: { ...r.layout },
      initialStyles: { ...r.styles },
    };
  });
}

/** Server slot-mapping failure positioned against editor rows. */
export type ServerSlotRowError = {
  /** Row indexes named by the server message (empty when none match). */
  indexes: number[];
  /** Extracted server message, if the error carried one. */
  serverMessage: string | undefined;
};

/**
 * Map a failed template PUT back onto slot rows: extract the server message
 * and resolve {@code slots[N]} / {@code slots[N] not found: <key>} refs to
 * row indexes. Returns empty indexes when the error names no row.
 */
export function serverSlotErrorForRows(
  rows: SlotEditorRow[],
  err: unknown,
): ServerSlotRowError {
  const serverMessage = isApiError(err)
    ? extractRestErrorMessage(err.body)
    : undefined;
  return {
    indexes: slotRowIndexesForServerError(rows, serverMessage ?? null),
    serverMessage,
  };
}
