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

import type { SlotEditorRow } from "./TemplateSlotsPanel";

/**
 * Slot-mapping validation for the Design template editor (#6477 / parent #2626).
 *
 * Mirrors {@code templateBindings#validateBindings}: client-side checks aligned
 * with the sitemanage {@code TemplateAdaptor#toSlots} / {@code SlotsAdaptor}
 * 400 contract ({@code slots[i] ...}), so mapping problems are reported inline
 * on the offending slot row before (and after) the server round-trip.
 */

/** One row-level mapping problem, positioned by row index. */
export type SlotRowIssue = {
  index: number;
  message: string;
};

const NON_NEGATIVE_INT_RE = /^\d+$/;

/** Orientation values the assembler slot model accepts (empty = default). */
const ALLOWED_ORIENTATIONS = new Set(["", "horizontal", "vertical"]);

/** Server 400 shape emitted by {@code TemplateAdaptor#toSlots}. */
const SERVER_SLOT_INDEX_RE = /slots\[(\d+)\]/;

/** Server 400 shape for unknown slot refs: {@code slots[i] not found: <key>}. */
const SERVER_SLOT_NOT_FOUND_RE = /slots\[\d+\]\s+not found:\s*(.+?)\s*$/i;

function isBlank(value: string | null | undefined): boolean {
  return (value || "").trim().length === 0;
}

/**
 * Client-side validation for loaded slot-mapping rows.
 *
 * Rows that failed to load ({@code loadError}) already render an inline load
 * error and are skipped here. Returns one issue per problem, in row order;
 * empty array means the mapping is save-ready.
 */
export function validateSlotRows(rows: SlotEditorRow[]): SlotRowIssue[] {
  const issues: SlotRowIssue[] = [];
  for (let i = 0; i < rows.length; i++) {
    const row = rows[i];
    if (row == null || isBlank(row.key)) {
      issues.push({ index: i, message: `slots[${i}] is not mapped to a known slot` });
      continue;
    }
    if (row.loadError) {
      continue;
    }
    const orientation = (row.layout.orientation || "").trim();
    if (!ALLOWED_ORIENTATIONS.has(orientation)) {
      issues.push({
        index: i,
        message: `slots[${i}].orientation must be horizontal, vertical, or empty`,
      });
    }
    const columns = (row.layout.columns || "").trim();
    if (columns && !NON_NEGATIVE_INT_RE.test(columns)) {
      issues.push({
        index: i,
        message: `slots[${i}].columns must be a non-negative whole number`,
      });
    }
    const maxItems = (row.layout.maxItems || "").trim();
    if (maxItems && !NON_NEGATIVE_INT_RE.test(maxItems)) {
      issues.push({
        index: i,
        message: `slots[${i}].maxItems must be a non-negative whole number`,
      });
    }
  }
  return issues;
}

/**
 * Map a server-side slot-mapping failure back to editor row indexes.
 *
 * Matches {@code slots[N]} index references (when in range) and
 * {@code slots[N] not found: <key>} key references against row keys/names.
 * Returns [] when the message names no row (caller keeps the banner error).
 */
export function slotRowIndexesForServerError(
  rows: SlotEditorRow[],
  serverMessage: string | null | undefined,
): number[] {
  if (!serverMessage || rows.length === 0) {
    return [];
  }
  const found: number[] = [];
  const push = (index: number) => {
    if (Number.isInteger(index) && index >= 0 && index < rows.length && !found.includes(index)) {
      found.push(index);
    }
  };

  const indexMatch = SERVER_SLOT_INDEX_RE.exec(serverMessage);
  if (indexMatch) {
    push(Number(indexMatch[1]));
  }

  const keyMatch = SERVER_SLOT_NOT_FOUND_RE.exec(serverMessage);
  const wanted = (keyMatch?.[1] || "").trim().toLowerCase();
  if (wanted) {
    rows.forEach((row, i) => {
      if (!row) return;
      const candidates = [row.key, row.name, row.label]
        .map((v) => (v || "").trim().toLowerCase())
        .filter((v) => v.length > 0);
      if (candidates.includes(wanted)) {
        push(i);
      }
    });
  }

  return found.sort((a, b) => a - b);
}
