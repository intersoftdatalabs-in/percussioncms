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

/**
 * Scheduled transitions (embargo/expiry triggers) for the React Content
 * Editor host (#5830, parent #4532).
 *
 * <p>An item's publish start date is its embargo (publish-from) and its
 * removal end date is its expiry (until): the two dates that arm the
 * workflow's scheduled (aging) transitions. This slice only reads the same
 * {@code GET .../itemmanagement/item/getitemdates/{id}} payload the
 * Schedule dialog already uses (#4917) and lists the non-blank sides.
 * Setting and clearing reuse the existing Schedule / Clear-schedule
 * dialogs — nothing here posts dates.</p>
 */

import type { ItemScheduleDates } from "../contentExplorer/itemScheduleDates";
import type { EditorHostMode } from "./editorHostUrl";

/** The two scheduled-transition triggers an item can arm. */
export type ScheduledTransitionKind = "embargo" | "expiry";

export interface ScheduledTransitionEntry {
  kind: ScheduledTransitionKind;
  /** Raw wire date text (e.g. {@code "09/18/2026 09:00 am"}). */
  value: string;
}

/** Scheduled transitions are viewable in edit and view, never in promote. */
export function canViewScheduledTransitionsFromEditor(
  mode: EditorHostMode,
): boolean {
  return mode === "edit" || mode === "view";
}

function cleanDate(value: string | null | undefined): string {
  return String(value ?? "").trim();
}

/**
 * Embargo (start) and expiry (end) rows for the panel, in trigger order.
 * A blank side is not a scheduled trigger, so it produces no row.
 */
export function scheduledTransitionEntries(
  dates: Pick<ItemScheduleDates, "startDate" | "endDate"> | null | undefined,
): ScheduledTransitionEntry[] {
  if (dates == null) {
    return [];
  }
  const entries: ScheduledTransitionEntry[] = [];
  const embargo = cleanDate(dates.startDate);
  if (embargo) {
    entries.push({ kind: "embargo", value: embargo });
  }
  const expiry = cleanDate(dates.endDate);
  if (expiry) {
    entries.push({ kind: "expiry", value: expiry });
  }
  return entries;
}

/** True when the item arms at least one scheduled (embargo/expiry) trigger. */
export function hasScheduledTransitions(
  dates: Pick<ItemScheduleDates, "startDate" | "endDate"> | null | undefined,
): boolean {
  return scheduledTransitionEntries(dates).length > 0;
}

/**
 * Display string for one panel row (e.g. {@code "Embargo: 09/18/2026 09:00 am"}).
 */
export function summarizeScheduledTransition(
  entry: ScheduledTransitionEntry,
  embargoLabel = "Embargo",
  expiryLabel = "Expiry",
): string {
  const label = entry.kind === "embargo" ? embargoLabel : expiryLabel;
  const value = cleanDate(entry.value);
  return value ? `${label}: ${value}` : label;
}
