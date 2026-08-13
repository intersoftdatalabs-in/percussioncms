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

import React, { useState } from "react";
import { message } from "../../i18n/message";
import { ADMIN_MSG } from "../messages";
import { AdminSectionErrorBoundary } from "../AdminSectionErrorBoundary";
import { ConsistencyChecker } from "./ConsistencyChecker";
import { SecurityAuditLogViewer } from "./SecurityAuditLogViewer";

export type AdminToolId = "consistency" | "security-audit";

const TOOLS: readonly AdminToolId[] = ["security-audit", "consistency"];

export function normalizeAdminTool(
  raw: string | null | undefined,
): AdminToolId {
  if (raw == null || !raw.trim()) {
    return "security-audit";
  }
  const n = raw.trim().toLowerCase();
  if (n === "consistency" || n === "security-audit") {
    return n;
  }
  return "security-audit";
}

export interface ToolsSectionProps {
  /** Optional initial tool (e.g. deep-link query). Defaults to security audit. */
  initialTool?: AdminToolId | string;
}

export const ToolsSection: React.FC<ToolsSectionProps> = ({
  initialTool = "security-audit",
}) => {
  const [activeTool, setActiveTool] = useState<AdminToolId>(() =>
    normalizeAdminTool(initialTool),
  );

  const tabStyle = (id: AdminToolId): React.CSSProperties => ({
    padding: "8px 16px",
    background: "none",
    border: "none",
    borderBottom:
      activeTool === id ? "2px solid #0284c7" : "2px solid transparent",
    fontWeight: activeTool === id ? 600 : 400,
    color: activeTool === id ? "#0284c7" : "#64748b",
    cursor: "pointer",
  });

  return (
    <div style={{ padding: "16px" }} data-testid="perc-tools-section">
      <div
        role="tablist"
        aria-label={message(ADMIN_MSG.TAB_TOOLS)}
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: "8px 16px",
          borderBottom: "1px solid #e2e8f0",
          marginBottom: "16px",
        }}
        data-testid="perc-tools-tablist"
      >
        {TOOLS.map((id) => (
          <button
            key={id}
            type="button"
            role="tab"
            id={`tab-${id}`}
            aria-controls={`panel-${id}`}
            aria-selected={activeTool === id}
            onClick={() => setActiveTool(id)}
            style={tabStyle(id)}
            data-testid={`tool-tab-${id}`}
          >
            {id === "security-audit"
              ? message(ADMIN_MSG.TOOL_SECURITY_AUDIT)
              : message(ADMIN_MSG.TOOL_CONSISTENCY)}
          </button>
        ))}
      </div>

      {activeTool === "security-audit" && (
        <div
          role="tabpanel"
          id="panel-security-audit"
          aria-labelledby="tab-security-audit"
          data-testid="panel-security-audit"
        >
          <AdminSectionErrorBoundary
            label={message(ADMIN_MSG.TOOL_SECURITY_AUDIT)}
          >
            <SecurityAuditLogViewer />
          </AdminSectionErrorBoundary>
        </div>
      )}
      {activeTool === "consistency" && (
        <div
          role="tabpanel"
          id="panel-consistency"
          aria-labelledby="tab-consistency"
          data-testid="panel-consistency"
        >
          <AdminSectionErrorBoundary
            label={message(ADMIN_MSG.TOOL_CONSISTENCY)}
          >
            <ConsistencyChecker />
          </AdminSectionErrorBoundary>
        </div>
      )}
    </div>
  );
};
