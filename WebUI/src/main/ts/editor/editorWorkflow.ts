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
 * Pure gates for React Content Editor workflow transitions (#4539 / parent #4532).
 *
 * <p>Allowed triggers come from itemmanagement {@code getTransitions}. Comment
 * required is a client gate for reject-style triggers (the getTransitions DTO
 * does not carry {@code requiresComment}). Unauthorized names never run.</p>
 */

import type { EditorHostMode } from "./editorHostUrl";

/** Trigger names that must have a non-empty comment before invoke. */
export const COMMENT_REQUIRED_TRIGGERS: readonly string[] = [
  "Reject",
  "Return",
  "Disapprove",
  "Decline",
  "SendBack",
  "Send Back",
];

export type EditorTransitionBlockReason =
  | "readonly"
  | "unauthorized"
  | "comment";

export type EditorTransitionGate =
  | { ok: true }
  | { ok: false; reason: EditorTransitionBlockReason };

export function uniqueTransitionTriggers(
  raw: readonly string[] | null | undefined,
): string[] {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const entry of raw ?? []) {
    const t = String(entry ?? "").trim();
    if (!t || seen.has(t)) {
      continue;
    }
    seen.add(t);
    out.push(t);
  }
  return out;
}

export function isAllowedTransitionTrigger(
  trigger: string,
  allowed: readonly string[] | null | undefined,
): boolean {
  const name = String(trigger ?? "").trim();
  if (!name) {
    return false;
  }
  return uniqueTransitionTriggers(allowed).includes(name);
}

function foldTrigger(value: string): string {
  return String(value ?? "")
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "");
}

/**
 * True when this trigger must have a comment. Extra names (tests / hosts)
 * are folded the same way as the built-in reject-style list.
 */
export function triggerRequiresComment(
  trigger: string,
  extra: readonly string[] | null | undefined = COMMENT_REQUIRED_TRIGGERS,
): boolean {
  const folded = foldTrigger(trigger);
  if (!folded) {
    return false;
  }
  const names = extra?.length ? extra : COMMENT_REQUIRED_TRIGGERS;
  return names.some((n) => foldTrigger(n) === folded);
}

/**
 * Whether the editor host may invoke {@code transitionWithComments}.
 *
 * <p>View / promote stay read-only. Triggers not in the loaded allowlist
 * never run. Comment-required triggers block until the comment is non-blank.</p>
 */
export function canRunEditorTransition(input: {
  mode: EditorHostMode;
  trigger: string;
  allowed: readonly string[] | null | undefined;
  comment: string;
  commentRequiredTriggers?: readonly string[] | null;
}): EditorTransitionGate {
  if (input.mode !== "edit") {
    return { ok: false, reason: "readonly" };
  }
  if (!isAllowedTransitionTrigger(input.trigger, input.allowed)) {
    return { ok: false, reason: "unauthorized" };
  }
  const required = triggerRequiresComment(
    input.trigger,
    input.commentRequiredTriggers ?? COMMENT_REQUIRED_TRIGGERS,
  );
  if (required && String(input.comment ?? "").trim().length === 0) {
    return { ok: false, reason: "comment" };
  }
  return { ok: true };
}
