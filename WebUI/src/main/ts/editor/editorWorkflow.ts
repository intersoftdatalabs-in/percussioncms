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
 * <p>Allowed triggers come from itemmanagement {@code getTransitions}. The
 * server-driven {@code commentRequiredTriggers} list from the same DTO
 * extends the built-in reject-style list (#5828). Unauthorized names never
 * run. A transition comment that contains a NUL never runs (#5443).</p>
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
  | "comment"
  | "nul"
  | "assignees";

/** HTTP failures that must not be shown as a completed transition (#5163). */
export type EditorTransitionHttpFailure =
  | "badRequest"
  | "forbidden"
  | "conflict"
  | "failed";

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
 * Effective comment-required triggers for the editor host (#5828).
 *
 * <p>The server-driven `getTransitions` `commentRequiredTriggers` list
 * extends the built-in reject-style list: workflow-designer-configured names
 * (for example a comment-required **Approve**) are enforced like the classic
 * surface, while the built-in names keep their long-standing gate (QA #4554,
 * `content-explorer.md`). An explicit override (the
 * `commentRequiredTriggers` test seam) replaces the built-in base but the
 * server list still extends it; an empty override still means the built-in
 * list. A missing server list (never loaded or load failed) leaves the base
 * unchanged.</p>
 */
export function resolveCommentRequiredTriggers(
  override: readonly string[] | null | undefined,
  server: readonly string[] | null | undefined,
): string[] {
  const base =
    override != null && override.length > 0
      ? override
      : COMMENT_REQUIRED_TRIGGERS;
  return uniqueTransitionTriggers([...base, ...(server ?? [])]);
}

/**
 * True when a workflow transition comment contains a NUL.
 * A blank comment is not a NUL. A normal comment is not a NUL.
 * A NUL is refused before {@code transitionWithComments} (#5443).
 */
export function workflowCommentContainsNul(
  comment: string | null | undefined,
): boolean {
  return (comment ?? "").includes("\u0000");
}

/**
 * Whether the editor host may invoke {@code transitionWithComments}.
 *
 * <p>View / promote stay read-only. Triggers not in the loaded allowlist
 * never run. Comment-required triggers block until the comment is non-blank;
 * pass the resolved list from {@code resolveCommentRequiredTriggers} so a
 * workflow-designer-configured `getTransitions` `commentRequiredTriggers`
 * list is honored (#5828). A comment that contains a NUL never runs,
 * including on a comment-optional trigger and on a required trigger whose
 * comment is otherwise non-blank.</p>
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
  if (workflowCommentContainsNul(input.comment)) {
    return { ok: false, reason: "nul" };
  }
  return { ok: true };
}

/**
 * Trimmed unique assignee names in first-seen order. Matches the comma-separated
 * {@code adhocAssignees} query the editor in-context transition flow sends (#5163).
 */
export function normalizeAdhocAssignees(
  raw: readonly string[] | null | undefined,
): string[] {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const entry of raw ?? []) {
    const name = String(entry ?? "").trim();
    if (!name || seen.has(name)) {
      continue;
    }
    seen.add(name);
    out.push(name);
  }
  return out;
}

/**
 * True when getTransitions listed this trigger in {@code assigneeRequiredTriggers}
 * (destination state has ad-hoc assignment enabled).
 */
export function triggerRequiresAssignees(
  trigger: string,
  required: readonly string[] | null | undefined,
): boolean {
  const name = String(trigger ?? "").trim();
  if (!name) {
    return false;
  }
  return (required ?? []).some((entry) => String(entry ?? "").trim() === name);
}

/**
 * Confirm step when the transition requires assignees. Otherwise run immediately.
 * Chosen names are included only when the list is non-empty so a comment-only
 * call stays a three-argument transition.
 */
export function editorAssigneeStep(input: {
  requiresAssignees: boolean;
  assignees: readonly string[] | null | undefined;
}):
  | { action: "confirm" }
  | { action: "run"; assignees: string[] } {
  if (input.requiresAssignees) {
    return { action: "confirm" };
  }
  return { action: "run", assignees: normalizeAdhocAssignees(input.assignees) };
}

/**
 * Confirm is allowed only with at least one assignee. Empty does not fire.
 */
export function confirmEditorAssignees(
  raw: readonly string[] | null | undefined,
): { ok: true; assignees: string[] } | { ok: false; reason: "assignees" } {
  const assignees = normalizeAdhocAssignees(raw);
  if (assignees.length === 0) {
    return { ok: false, reason: "assignees" };
  }
  return { ok: true, assignees };
}

/**
 * Display list for the delegation notice after an adhoc-assignee transition
 * succeeds (#5890). Names are normalized first (trimmed, blanks dropped,
 * de-duped in first-seen order) and joined for the `{0}` slot of
 * `WORKFLOW_DELEGATED`. Empty when nobody was delegated to.
 */
export function formatDelegatedAssigneeList(
  raw: readonly string[] | null | undefined,
): string {
  return normalizeAdhocAssignees(raw).join(", ");
}

/**
 * Apply the `{0}` delegation placeholder after TMX resolve (works with key
 * fallbacks, mirroring the admin audit summary).
 */
export function formatDelegationNotice(
  template: string,
  raw: readonly string[] | null | undefined,
): string {
  return String(template ?? "").replace(
    "{0}",
    formatDelegatedAssigneeList(raw),
  );
}

/**
 * 400, 403, and 409 are failures. Anything else is a generic failure.
 * None of these claim the workflow state changed.
 */
export function editorTransitionHttpFailure(
  status: number | undefined,
): EditorTransitionHttpFailure {
  if (status === 400) {
    return "badRequest";
  }
  if (status === 403) {
    return "forbidden";
  }
  if (status === 409) {
    return "conflict";
  }
  return "failed";
}

export type EditorWorkflowChangeReason = "blank" | "unchanged" | "forbidden";

export type EditorWorkflowChangeGate =
  | { ok: true; workflowId: string }
  | { ok: false; reason: EditorWorkflowChangeReason };

/**
 * Client gate before POST changeWorkflow (#4861). The server repeats the
 * association check; this only stops an empty, current, or unlisted id.
 */
export function canChangeEditorWorkflow(input: {
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[] | null | undefined;
}): EditorWorkflowChangeGate {
  const workflowId = String(input.selectedId ?? "").trim();
  if (!workflowId) {
    return { ok: false, reason: "blank" };
  }
  const current = String(input.currentId ?? "").trim();
  if (current && workflowId === current) {
    return { ok: false, reason: "unchanged" };
  }
  const allowed = (input.allowedIds ?? [])
    .map((id) => String(id ?? "").trim())
    .filter((id) => id.length > 0);
  if (!allowed.includes(workflowId)) {
    return { ok: false, reason: "forbidden" };
  }
  return { ok: true, workflowId };
}
