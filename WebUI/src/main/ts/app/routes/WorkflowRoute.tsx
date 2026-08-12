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

import React from "react";
import { Navigate, useParams } from "react-router";
import { RequireRole } from "./RequireRole";
import { normalizeWorkflowTab } from "../deepLinks/allowlists";

/**
 * Legacy SPA Administration (workflow) routes — Admin only.
 *
 * <p>#3088: {@code /workflow} and {@code /workflow/:tab} redirect into the
 * unified Admin shell ({@code /admin/workflow}, {@code /admin/roles}, …)
 * so bookmarks and docs keep working without a sibling product chrome.</p>
 */
export function WorkflowRoute(): React.ReactElement {
  const { tab } = useParams();
  const normalized = normalizeWorkflowTab(tab) ?? "workflow";
  const target = `/admin/${normalized}`;

  return (
    <RequireRole gate="admin">
      <Navigate to={target} replace />
    </RequireRole>
  );
}
