/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import { WF_ADMIN_MSG } from "./messages";
import { WorkflowSection } from "./workflow/WorkflowSection";

export type WorkflowAdminTab = "workflow" | "roles" | "users" | "categories";

export interface WorkflowAdminShellProps {
  initialTab?: WorkflowAdminTab;
}

export const WorkflowAdminShell: React.FC<WorkflowAdminShellProps> = ({
  initialTab = "workflow",
}) => {
  const [activeTab, setActiveTab] = useState<WorkflowAdminTab>(initialTab);

  return (
    <div className="perc-workflow-admin-shell" data-testid="perc-workflow-admin-shell" style={{ padding: "20px", fontFamily: "sans-serif" }}>
      <header style={{ borderBottom: "2px solid #007ea8", paddingBottom: "12px", marginBottom: "20px" }}>
        <h2 style={{ margin: 0, color: "#007ea8" }}>{message(WF_ADMIN_MSG.TITLE)}</h2>
      </header>

      <nav
        className="perc-tab-nav"
        role="tablist"
        aria-label={message(WF_ADMIN_MSG.TITLE)}
        style={{ display: "flex", borderBottom: "1px solid #ccc", marginBottom: "20px" }}
      >
        <button
          type="button"
          role="tab"
          id="tab-workflow"
          aria-selected={activeTab === "workflow"}
          aria-controls="panel-workflow"
          onClick={() => setActiveTab("workflow")}
          style={{
            padding: "10px 20px",
            border: "none",
            borderBottom: activeTab === "workflow" ? "3px solid #007ea8" : "none",
            fontWeight: activeTab === "workflow" ? 600 : 400,
            background: "transparent",
            cursor: "pointer",
          }}
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
          style={{
            padding: "10px 20px",
            border: "none",
            borderBottom: activeTab === "roles" ? "3px solid #007ea8" : "none",
            fontWeight: activeTab === "roles" ? 600 : 400,
            background: "transparent",
            cursor: "pointer",
          }}
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
          style={{
            padding: "10px 20px",
            border: "none",
            borderBottom: activeTab === "users" ? "3px solid #007ea8" : "none",
            fontWeight: activeTab === "users" ? 600 : 400,
            background: "transparent",
            cursor: "pointer",
          }}
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
          style={{
            padding: "10px 20px",
            border: "none",
            borderBottom: activeTab === "categories" ? "3px solid #007ea8" : "none",
            fontWeight: activeTab === "categories" ? 600 : 400,
            background: "transparent",
            cursor: "pointer",
          }}
          data-testid="tab-categories"
        >
          {message(WF_ADMIN_MSG.TAB_CATEGORIES)}
        </button>
      </nav>

      <main
        className="perc-tab-content"
        role="tabpanel"
        id={`panel-${activeTab}`}
        aria-labelledby={`tab-${activeTab}`}
      >
        {activeTab === "workflow" && <WorkflowSection />}
        {activeTab === "roles" && (
          <div data-testid="roles-placeholder" style={{ padding: "20px" }}>
            Roles management section
          </div>
        )}
        {activeTab === "users" && (
          <div data-testid="users-placeholder" style={{ padding: "20px" }}>
            Users management section
          </div>
        )}
        {activeTab === "categories" && (
          <div data-testid="categories-placeholder" style={{ padding: "20px" }}>
            Categories management section
          </div>
        )}
      </main>
    </div>
  );
};
