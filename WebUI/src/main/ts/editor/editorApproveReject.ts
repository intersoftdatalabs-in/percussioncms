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
 * Quick Approve/Reject action bar for the React Content Editor host (#6483,
 * parent #4532).
 *
 * <p>Resolves the single Approve-style and Reject-style triggers from the
 * loaded {@code getTransitions} allowlist so the header can offer one-click
 * Approve / Reject with a comment dialog. The run gate stays
 * {@code canRunEditorTransition} ({@code editorWorkflow.ts}): unauthorized
 * names never run, comment-required triggers block until the dialog comment
 * is non-blank, and a NUL comment never runs. This module only decides which
 * allowlisted names the bar drives.</p>
 */

import {
  COMMENT_REQUIRED_TRIGGERS,
  uniqueTransitionTriggers,
} from "./editorWorkflow";

/** Allowlisted names that count as the quick Approve action. */
export const APPROVE_TRIGGER_NAMES: readonly string[] = ["Approve"];

export interface ApproveRejectTriggers {
  approve: string | null;
  reject: string | null;
}

function foldTriggerName(value: string): string {
  return String(value ?? "")
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "");
}

function firstAllowlistedMatch(
  allowlisted: readonly string[],
  names: readonly string[],
): string | null {
  const wanted = names
    .map((name) => foldTriggerName(name))
    .filter((name) => name.length > 0);
  for (const entry of allowlisted) {
    const raw = String(entry ?? "").trim();
    if (!raw) {
      continue;
    }
    if (wanted.includes(foldTriggerName(raw))) {
      return raw;
    }
  }
  return null;
}

/**
 * Resolve the quick-bar triggers from the loaded allowlist (#6483).
 *
 * <p>The Approve button drives the first allowlisted Approve-style name;
 * the Reject button drives the first allowlisted reject-style name (the
 * built-in reject-style list by default, so designer renames such as
 * {@code SendBack} resolve the same way the panel gate treats them).
 * Overrides replace the built-in bases for tests and hosts. A missing or
 * empty allowlist resolves both to {@code null} and the bar stays hidden —
 * the empty state is "no bar", never a dead button.</p>
 */
export function resolveApproveRejectTriggers(
  allowed: readonly string[] | null | undefined,
  options?: {
    approveNames?: readonly string[] | null;
    rejectNames?: readonly string[] | null;
  },
): ApproveRejectTriggers {
  const allowlisted = uniqueTransitionTriggers(allowed);
  const approveNames =
    options?.approveNames != null && options.approveNames.length > 0
      ? options.approveNames
      : APPROVE_TRIGGER_NAMES;
  const rejectNames =
    options?.rejectNames != null && options.rejectNames.length > 0
      ? options.rejectNames
      : COMMENT_REQUIRED_TRIGGERS;
  return {
    approve: firstAllowlistedMatch(allowlisted, approveNames),
    reject: firstAllowlistedMatch(allowlisted, rejectNames),
  };
}

/**
 * Apply the `{0}` trigger placeholder after TMX resolve (mirrors the admin
 * audit summary and the delegation notice).
 */
export function formatApproveRejectBody(
  template: string,
  trigger: string,
): string {
  return String(template ?? "").replace("{0}", String(trigger ?? ""));
}
