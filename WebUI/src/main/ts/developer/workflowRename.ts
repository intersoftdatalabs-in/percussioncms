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

/** Stock names the server rejects for workflow writes (case-insensitive). */
const PACKAGED_WORKFLOW_NAMES = [
  "default workflow",
  "simple workflow",
  "local content",
  "localcontent",
];

const WORKFLOW_NAME_PATTERN = /^[\s\w-]+$/;
const WORKFLOW_NAME_MAX_LENGTH = 50;

/** True for Default Workflow, Simple Workflow, Local Content, and LocalContent. */
export function isPackagedWorkflowName(name: string | undefined | null): boolean {
  const n = (name ?? "").trim().toLowerCase();
  return PACKAGED_WORKFLOW_NAMES.includes(n);
}

/**
 * Rename is offered only for a custom workflow that is not the system default.
 * Packaged names and the current default must not call the rename API.
 */
export function canOfferWorkflowRename(opts: {
  name: string | undefined | null;
  defaultWorkflow?: boolean;
}): boolean {
  if (opts.defaultWorkflow === true) {
    return false;
  }
  return !isPackagedWorkflowName(opts.name);
}

/**
 * Confirm is enabled when the next name is a legal workflow name and differs
 * from the current name (a case-only change still counts).
 */
export function isWorkflowRenameReady(
  currentName: string | undefined | null,
  nextName: string | undefined | null,
): boolean {
  const current = (currentName ?? "").trim();
  const next = (nextName ?? "").trim();
  if (!next || next.length > WORKFLOW_NAME_MAX_LENGTH) {
    return false;
  }
  if (next.includes("*") || next.includes("%")) {
    return false;
  }
  if (!WORKFLOW_NAME_PATTERN.test(next)) {
    return false;
  }
  return next !== current;
}
