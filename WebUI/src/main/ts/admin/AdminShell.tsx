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
import { message } from "../i18n/message";
import { ADMIN_MSG } from "./messages";
import { TasksSection } from "./TasksSection";
import { TaskLogsSection } from "./TaskLogsSection";
import { TaskNotifications } from "./TaskNotifications";
import { ToolsSection } from "./tools/ToolsSection";

export type AdminTab = "tasks" | "logs" | "notifications" | "tools";

const ADMIN_TABS: readonly AdminTab[] = [
  "tasks",
  "logs",
  "notifications",
  "tools",
];

export function normalizeAdminShellTab(
  raw: string | null | undefined,
): AdminTab {
  if (raw == null || !raw.trim()) {
    return "tasks";
  }
  const n = raw.trim().toLowerCase();
  return (ADMIN_TABS as readonly string[]).includes(n)
    ? (n as AdminTab)
    : "tasks";
}

export interface AdminShellProps {
  initialTab?: AdminTab | string;
  /**
   * When true (SPA AppLayout), shell is under product chrome.
   * Reserved for layout tweaks; no BrandBar today.
   */
  embedded?: boolean;
}

export const AdminShell: React.FC<AdminShellProps> = ({
  initialTab = "tasks",
  embedded: _embedded = false,
}) => {
  const [activeTab, setActiveTab] = useState<AdminTab>(() =>
    normalizeAdminShellTab(initialTab),
  );

  return (
    <div
      className="perc-admin-shell"
      data-testid="perc-admin-shell"
      style={{
        fontFamily: "var(--perc-font-family, sans-serif)",
        padding: "20px",
        maxWidth: "1200px",
        margin: "0 auto",
      }}
    >
      <header style={{ marginBottom: "20px" }}>
        <h1>{message(ADMIN_MSG.ADMIN_TITLE)}</h1>
      </header>

      <nav
        className="perc-tab-nav"
        role="tablist"
        style={{
          display: "flex",
          borderBottom: "1px solid #e2e8f0",
          marginBottom: "20px",
        }}
      >
        <button
          type="button"
          role="tab"
          id="tab-tasks"
          aria-selected={activeTab === "tasks"}
          aria-controls="panel-tasks"
          onClick={() => setActiveTab("tasks")}
          style={{
            padding: "10px 20px",
            border: "none",
            borderBottom: activeTab === "tasks" ? "3px solid #007ea8" : "none",
            fontWeight: activeTab === "tasks" ? 600 : 400,
            background: "transparent",
            cursor: "pointer",
          }}
          data-testid="tab-tasks"
        >
          {message(ADMIN_MSG.TAB_TASKS)}
        </button>

        <button
          type="button"
          role="tab"
          id="tab-logs"
          aria-selected={activeTab === "logs"}
          aria-controls="panel-logs"
          onClick={() => setActiveTab("logs")}
          style={{
            padding: "10px 20px",
            border: "none",
            borderBottom: activeTab === "logs" ? "3px solid #007ea8" : "none",
            fontWeight: activeTab === "logs" ? 600 : 400,
            background: "transparent",
            cursor: "pointer",
          }}
          data-testid="tab-logs"
        >
          {message(ADMIN_MSG.TAB_LOGS)}
        </button>

        <button
          type="button"
          role="tab"
          id="tab-notifications"
          aria-selected={activeTab === "notifications"}
          aria-controls="panel-notifications"
          onClick={() => setActiveTab("notifications")}
          style={{
            padding: "10px 20px",
            border: "none",
            borderBottom: activeTab === "notifications" ? "3px solid #007ea8" : "none",
            fontWeight: activeTab === "notifications" ? 600 : 400,
            background: "transparent",
            cursor: "pointer",
          }}
          data-testid="tab-notifications"
        >
          {message(ADMIN_MSG.TAB_NOTIFICATIONS)}
        </button>

        <button
          type="button"
          role="tab"
          id="tab-tools"
          aria-selected={activeTab === "tools"}
          aria-controls="panel-tools"
          onClick={() => setActiveTab("tools")}
          style={{
            padding: "10px 20px",
            border: "none",
            borderBottom: activeTab === "tools" ? "3px solid #007ea8" : "none",
            fontWeight: activeTab === "tools" ? 600 : 400,
            background: "transparent",
            cursor: "pointer",
          }}
          data-testid="tab-tools"
        >
          System Tools
        </button>
      </nav>

      <main
        className="perc-tab-content"
        role="tabpanel"
        id={`panel-${activeTab}`}
        aria-labelledby={`tab-${activeTab}`}
      >
        {activeTab === "tasks" && <TasksSection />}
        {activeTab === "logs" && <TaskLogsSection />}
        {activeTab === "notifications" && <TaskNotifications />}
        {activeTab === "tools" && <ToolsSection />}
      </main>
    </div>
  );
};
