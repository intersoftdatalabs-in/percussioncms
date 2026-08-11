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

import React, { useEffect, useState } from "react";
import { get, post, put, del } from "../../api/client";
import { parseWorkflowList } from "../../api/developer/workflowsApi";
import type { WorkflowDef } from "../../api/developer/types";
import { PATHS } from "../../api/paths";
import { message } from "../../i18n/message";
import { WF_ADMIN_MSG } from "../messages";
import { WorkflowDefinition, WorkflowEditor } from "./WorkflowEditor";
import type { WorkflowStep } from "./WorkflowStepList";

/**
 * Map a workflowmanagement row (PSUiWorkflow / WorkflowDef, or legacy admin shape)
 * into the WorkflowSection editor model.
 */
export function toWorkflowDefinition(
  item: WorkflowDef | Record<string, unknown> | null | undefined,
): WorkflowDefinition {
  if (item == null || typeof item !== "object") {
    return { name: "", isDefault: false, steps: [] };
  }
  const o = item as Record<string, unknown>;
  const name = String(o.name ?? o.workflowName ?? "").trim();
  const isDefault = Boolean(o.isDefault ?? o.defaultWorkflow ?? false);
  const stagingRaw = o.stagingRoleId ?? o.stagingRoleNames;
  const stagingRoleId =
    typeof stagingRaw === "string" && stagingRaw.trim().length > 0
      ? stagingRaw
      : undefined;

  let steps: WorkflowStep[] = [];
  if (Array.isArray(o.steps)) {
    steps = o.steps as WorkflowStep[];
  } else if (Array.isArray(o.workflowSteps)) {
    steps = (o.workflowSteps as Record<string, unknown>[]).map((s, i) => {
      const roleNames = Array.isArray(s.roleNames)
        ? (s.roleNames as string[])
        : Array.isArray(s.stepRoles)
          ? (s.stepRoles as { roleName?: string }[])
              .map((r) => (typeof r?.roleName === "string" ? r.roleName : ""))
              .filter((n) => n.length > 0)
          : [];
      return {
        name: String(s.name ?? s.stepName ?? "").trim(),
        roleNames,
        position: typeof s.position === "number" ? s.position : i,
      };
    });
  }

  return { name, isDefault, stagingRoleId, steps };
}

export const WorkflowSection: React.FC = () => {
  const [workflows, setWorkflows] = useState<WorkflowDefinition[]>([]);
  const [availableRoles, setAvailableRoles] = useState<string[]>([]);
  const [selectedWorkflow, setSelectedWorkflow] = useState<WorkflowDefinition | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      // Role names for the workflow editor come from user/roles (GET), not
      // role/find. POST role/find expects PSStringWrapper root "psstring"
      // ({ psstring: { value } }); a bare { name: "" } body fails Jackson with
      // "Root name ('name') does not match expected ('psstring')" (#2701).
      //
      // Workflow metadata is often a Jackson root wrapper ({ Workflow: [...] }),
      // not a bare array — parseWorkflowList unwraps before map (#2959).
      const [wfPayload, rolesRes] = await Promise.all([
        get<unknown>(PATHS.WORKFLOW_METADATA),
        get<{ RoleList: { roles: string[] } }>(PATHS.USER_ROLES).catch(() => null),
      ]);
      const parsed = parseWorkflowList(wfPayload);
      const list = Array.isArray(parsed)
        ? parsed.map((row) => toWorkflowDefinition(row))
        : [];
      setWorkflows(list);
      setAvailableRoles(rolesRes?.RoleList?.roles || []);
    } catch (err) {
      setWorkflows([]);
      setError(message(WF_ADMIN_MSG.ERROR_GENERIC));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreate = () => {
    setSelectedWorkflow(null);
    setIsEditing(true);
  };

  const handleEdit = (wf: WorkflowDefinition) => {
    setSelectedWorkflow(wf);
    setIsEditing(true);
  };

  const handleDelete = async (wf: WorkflowDefinition) => {
    if (wf.isDefault) {
      alert(message(WF_ADMIN_MSG.CANNOT_DELETE_DEFAULT));
      return;
    }
    if (!confirm(message(WF_ADMIN_MSG.CONFIRM_DELETE_WORKFLOW, [wf.name]))) return;

    try {
      await del(`${PATHS.WORKFLOWS}${encodeURIComponent(wf.name)}`);
      await loadData();
    } catch (err) {
      alert(message(WF_ADMIN_MSG.DELETE_FAILED));
    }
  };

  const handleSave = async (wf: WorkflowDefinition) => {
    try {
      if (selectedWorkflow) {
        await put(`${PATHS.WORKFLOWS}${encodeURIComponent(wf.name)}`, wf);
      } else {
        await post(`${PATHS.WORKFLOWS}${encodeURIComponent(wf.name)}`, wf);
      }
      setIsEditing(false);
      await loadData();
    } catch (err) {
      alert(message(WF_ADMIN_MSG.SAVE_FAILED));
    }
  };

  if (isLoading) {
    return <div style={{ padding: "24px" }}>{message(WF_ADMIN_MSG.LOADING)}</div>;
  }

  if (isEditing) {
    return (
      <WorkflowEditor
        workflow={selectedWorkflow}
        availableRoles={availableRoles}
        onSave={handleSave}
        onCancel={() => setIsEditing(false)}
      />
    );
  }

  return (
    <div className="perc-workflow-section" data-testid="perc-workflow-section">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
        <h3>{message(WF_ADMIN_MSG.WORKFLOWS_TITLE)}</h3>
        <button
          type="button"
          className="perc-button-primary"
          onClick={handleCreate}
          data-testid="create-workflow-button"
        >
          {message(WF_ADMIN_MSG.CREATE_WORKFLOW)}
        </button>
      </div>

      {error && (
        <div style={{ color: "#d9534f", marginBottom: "16px", padding: "12px", background: "#fdf7f7" }}>
          {error}
        </div>
      )}

      <table className="perc-table" style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr style={{ borderBottom: "2px solid #ccc", textAlign: "left" }}>
            <th style={{ padding: "8px" }}>{message(WF_ADMIN_MSG.WORKFLOW_NAME)}</th>
            <th style={{ padding: "8px" }}>{message(WF_ADMIN_MSG.IS_DEFAULT)}</th>
            <th style={{ padding: "8px" }}>{message(WF_ADMIN_MSG.STAGING_ROLE)}</th>
            <th style={{ padding: "8px", textAlign: "right" }}>{message(WF_ADMIN_MSG.TABLE_ACTIONS)}</th>
          </tr>
        </thead>
        <tbody>
          {workflows.length === 0 ? (
            <tr>
              <td colSpan={4} style={{ padding: "16px", textAlign: "center", color: "#666" }}>
                {message(WF_ADMIN_MSG.NO_WORKFLOWS_FOUND)}
              </td>
            </tr>
          ) : (
            workflows.map((wf) => (
              <tr key={wf.name} style={{ borderBottom: "1px solid #eee" }} data-testid={`workflow-row-${wf.name}`}>
                <td style={{ padding: "8px", fontWeight: 600 }}>{wf.name}</td>
                <td style={{ padding: "8px" }}>
                  {wf.isDefault ? (
                    <span style={{ background: "#5cb85c", color: "#fff", padding: "2px 8px", borderRadius: "10px", fontSize: "12px" }}>
                      {message(WF_ADMIN_MSG.IS_DEFAULT)}
                    </span>
                  ) : (
                    "-"
                  )}
                </td>
                <td style={{ padding: "8px" }}>{wf.stagingRoleId || <em>{message(WF_ADMIN_MSG.NONE)}</em>}</td>
                <td style={{ padding: "8px", textAlign: "right" }}>
                  <button
                    type="button"
                    onClick={() => handleEdit(wf)}
                    style={{ marginRight: "8px" }}
                    data-testid={`edit-wf-${wf.name}`}
                  >
                    {message(WF_ADMIN_MSG.EDIT)}
                  </button>
                  <button
                    type="button"
                    disabled={wf.isDefault}
                    onClick={() => handleDelete(wf)}
                    style={{ color: wf.isDefault ? "#ccc" : "#d9534f" }}
                    data-testid={`delete-wf-${wf.name}`}
                  >
                    {message(WF_ADMIN_MSG.DELETE)}
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};
