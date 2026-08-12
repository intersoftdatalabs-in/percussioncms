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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import { Navigate } from "react-router";

/**
 * Former product shell for Workflow / Roles / Users / Categories administration.
 *
 * <p>#3088 folds those surfaces into {@code AdminShell} under {@code /admin/*}.
 * This component remains as a non-product redirect for residual registry loads
 * and any legacy embed that still names {@code WorkflowAdminShell}.</p>
 */
export type WorkflowAdminTab = "workflow" | "roles" | "users" | "categories";

const WORKFLOW_TABS: readonly WorkflowAdminTab[] = [
  "workflow",
  "roles",
  "users",
  "categories",
];

export function normalizeWorkflowAdminTab(
  raw: string | null | undefined,
): WorkflowAdminTab {
  if (raw == null || !raw.trim()) {
    return "workflow";
  }
  const n = raw.trim().toLowerCase();
  return (WORKFLOW_TABS as readonly string[]).includes(n)
    ? (n as WorkflowAdminTab)
    : "workflow";
}

export interface WorkflowAdminShellProps {
  initialTab?: WorkflowAdminTab | string;
  /** @deprecated No longer used; shell is redirect-only. */
  embedded?: boolean;
}

/**
 * Redirect-only stub: maps legacy Workflow admin tabs into unified Admin paths.
 */
export const WorkflowAdminShell: React.FC<WorkflowAdminShellProps> = ({
  initialTab = "workflow",
}) => {
  const tab = normalizeWorkflowAdminTab(initialTab);
  return <Navigate to={`/admin/${tab}`} replace />;
};
