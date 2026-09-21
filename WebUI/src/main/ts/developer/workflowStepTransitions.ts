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

import type { WorkflowStepSummary } from "../api/developer/types";

/**
 * Unique transition labels already present on a workflow-step DTO
 * ({@code stepRoles[].roleTransitions[].transitionPermission}).
 * Read-only catalog preview — not a graph editor.
 */
export function collectStepTransitionNames(step: WorkflowStepSummary): string[] {
  const names = new Set<string>();
  const roles = Array.isArray(step.stepRoles) ? step.stepRoles : [];
  for (const role of roles) {
    const trans = Array.isArray(role.roleTransitions) ? role.roleTransitions : [];
    for (const t of trans) {
      const raw =
        (typeof t.transitionPermission === "string" && t.transitionPermission) ||
        (typeof t.transitionName === "string" && t.transitionName) ||
        "";
      const n = raw.trim();
      if (n) {
        names.add(n);
      }
    }
  }
  return [...names];
}

export function formatStepTransitionNames(step: WorkflowStepSummary): string {
  return collectStepTransitionNames(step).join(", ");
}
