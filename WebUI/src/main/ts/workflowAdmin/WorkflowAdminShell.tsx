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
import { Link } from "react-router";
import { message, MSG } from "../i18n/message";
import styles from "../admin/AdminChrome.module.css";
import { WF_ADMIN_MSG } from "./messages";
import { WorkflowSection } from "./workflow/WorkflowSection";
import { RolesSection } from "./role/RolesSection";
import { UsersSection } from "./user/UsersSection";
import { CategoriesSection } from "./category/CategoriesSection";

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
  /**
   * When true (SPA AppLayout), shell is under product chrome.
   * Reserved for layout tweaks; no BrandBar today.
   */
  embedded?: boolean;
}

export const WorkflowAdminShell: React.FC<WorkflowAdminShellProps> = ({
  initialTab = "workflow",
  embedded: _embedded = false,
}) => {
  const [activeTab, setActiveTab] = useState<WorkflowAdminTab>(() =>
    normalizeWorkflowAdminTab(initialTab),
  );

  const tabClass = (tab: WorkflowAdminTab): string =>
    activeTab === tab ? `${styles.tab} ${styles.tabActive}` : styles.tab;

  return (
    <div
      className={`perc-workflow-admin-shell ${styles.shell}`}
      data-testid="perc-workflow-admin-shell"
    >
      <header className={styles.header}>
        <div className={styles.headerRow}>
          <h1 data-testid="perc-workflow-admin-shell-title">
            {message(WF_ADMIN_MSG.TITLE)}
          </h1>
          {/* Admin tools is top-nav landing (#2784/#2953); cross-link when deep-linked here. */}
          <Link
            to="/admin"
            className={styles.siblingLink}
            data-testid="admin-sibling-tools-link"
          >
            {message(MSG.NAV_ADMIN_TOOLS)}
          </Link>
        </div>
      </header>

      <nav
        className={`perc-tab-nav ${styles.tabNav}`}
        role="tablist"
        data-testid="perc-workflow-admin-tablist"
      >
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
        {activeTab === "workflow" && <WorkflowSection />}
        {activeTab === "roles" && <RolesSection />}
        {activeTab === "users" && <UsersSection />}
        {activeTab === "categories" && <CategoriesSection />}
      </main>
    </div>
  );
};
