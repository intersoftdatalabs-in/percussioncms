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
import { WF_ADMIN_MSG } from "../workflowAdmin/messages";
import { TasksSection } from "./TasksSection";
import { TaskLogsSection } from "./TaskLogsSection";
import { TaskNotifications } from "./TaskNotifications";
import { ToolsSection } from "./tools/ToolsSection";
import { WorkflowSection } from "../workflowAdmin/workflow/WorkflowSection";
import { RolesSection } from "../workflowAdmin/role/RolesSection";
import { UsersSection } from "../workflowAdmin/user/UsersSection";
import { CategoriesSection } from "../workflowAdmin/category/CategoriesSection";
import styles from "./AdminChrome.module.css";

/**
 * Unified Admin product shell tabs (#3088).
 * Former Workflow administration tabs (workflow / roles / users / categories)
 * live here alongside tasks, logs, notifications, and system tools.
 */
export type AdminTab =
  | "tasks"
  | "logs"
  | "notifications"
  | "tools"
  | "workflow"
  | "roles"
  | "users"
  | "categories";

const ADMIN_TABS: readonly AdminTab[] = [
  "tasks",
  "logs",
  "notifications",
  "tools",
  "workflow",
  "roles",
  "users",
  "categories",
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

  const tabClass = (tab: AdminTab): string =>
    activeTab === tab ? `${styles.tab} ${styles.tabActive}` : styles.tab;

  return (
    <div
      className={`perc-admin-shell ${styles.shell}`}
      data-testid="perc-admin-shell"
    >
      <header className={styles.header}>
        <div className={styles.headerRow}>
          <h1 data-testid="perc-admin-shell-title">
            {message(ADMIN_MSG.ADMIN_TITLE)}
          </h1>
        </div>
      </header>

      <nav
        className={`perc-tab-nav ${styles.tabNav}`}
        role="tablist"
        data-testid="perc-admin-tablist"
      >
        <button
          type="button"
          role="tab"
          id="tab-tasks"
          aria-selected={activeTab === "tasks"}
          aria-controls="panel-tasks"
          onClick={() => setActiveTab("tasks")}
          className={tabClass("tasks")}
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
          className={tabClass("logs")}
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
          className={tabClass("notifications")}
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
          className={tabClass("tools")}
          data-testid="tab-tools"
        >
          {message(ADMIN_MSG.TAB_TOOLS)}
        </button>

        <button
          type="button"
          role="tab"
          id="tab-workflow"
          aria-selected={activeTab === "workflow"}
          aria-controls="panel-workflow"
          onClick={() => setActiveTab("workflow")}
          className={tabClass("workflow")}
          data-testid="tab-workflow"
        >
          {message(WF_ADMIN_MSG.TAB_WORKFLOW)}
        </button>

        <button
          type="button"
          role="tab"
          id="tab-roles"
          aria-selected={activeTab === "roles"}
          aria-controls="panel-roles"
          onClick={() => setActiveTab("roles")}
          className={tabClass("roles")}
          data-testid="tab-roles"
        >
          {message(WF_ADMIN_MSG.TAB_ROLES)}
        </button>

        <button
          type="button"
          role="tab"
          id="tab-users"
          aria-selected={activeTab === "users"}
          aria-controls="panel-users"
          onClick={() => setActiveTab("users")}
          className={tabClass("users")}
          data-testid="tab-users"
        >
          {message(WF_ADMIN_MSG.TAB_USERS)}
        </button>

        <button
          type="button"
          role="tab"
          id="tab-categories"
          aria-selected={activeTab === "categories"}
          aria-controls="panel-categories"
          onClick={() => setActiveTab("categories")}
          className={tabClass("categories")}
          data-testid="tab-categories"
        >
          {message(WF_ADMIN_MSG.TAB_CATEGORIES)}
        </button>
      </nav>

      <main
        className={`perc-tab-content ${styles.tabContent}`}
        role="tabpanel"
        id={`panel-${activeTab}`}
        aria-labelledby={`tab-${activeTab}`}
      >
        {activeTab === "tasks" && <TasksSection />}
        {activeTab === "logs" && <TaskLogsSection />}
        {activeTab === "notifications" && <TaskNotifications />}
        {activeTab === "tools" && <ToolsSection />}
        {activeTab === "workflow" && <WorkflowSection />}
        {activeTab === "roles" && <RolesSection />}
        {activeTab === "users" && <UsersSection />}
        {activeTab === "categories" && <CategoriesSection />}
      </main>
    </div>
  );
};
