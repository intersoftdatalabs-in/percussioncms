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
import { message } from "../../i18n/message";
import { WF_ADMIN_MSG } from "../messages";
import { WorkflowStep, WorkflowStepList } from "./WorkflowStepList";

export interface WorkflowDefinition {
  name: string;
  isDefault: boolean;
  stagingRoleId?: string;
  steps: WorkflowStep[];
}

interface WorkflowEditorProps {
  workflow: WorkflowDefinition | null; // null if creating new
  availableRoles: string[];
  onSave: (wf: WorkflowDefinition) => void;
  onCancel: () => void;
}

export const WorkflowEditor: React.FC<WorkflowEditorProps> = ({
  workflow,
  availableRoles,
  onSave,
  onCancel,
}) => {
  const [name, setName] = useState(workflow?.name || "");
  const [isDefault, setIsDefault] = useState(workflow?.isDefault || false);
  const [stagingRoleId, setStagingRoleId] = useState(workflow?.stagingRoleId || "");
  const [steps, setSteps] = useState<WorkflowStep[]>(workflow?.steps || []);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError(message(WF_ADMIN_MSG.NAME_REQUIRED));
      return;
    }
    setError(null);
    onSave({
      name: name.trim(),
      isDefault,
      stagingRoleId: stagingRoleId || undefined,
      steps,
    });
  };

  return (
    <form className="perc-workflow-editor" onSubmit={handleSubmit} data-testid="perc-workflow-editor">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
        <h3>
          {workflow ? message(WF_ADMIN_MSG.EDIT_WORKFLOW) : message(WF_ADMIN_MSG.CREATE_WORKFLOW)}
        </h3>
        <div>
          <button type="button" onClick={onCancel} style={{ marginRight: "8px" }}>
            {message(WF_ADMIN_MSG.CANCEL)}
          </button>
          <button type="submit" className="perc-button-primary" data-testid="save-workflow-button">
            {message(WF_ADMIN_MSG.SAVE)}
          </button>
        </div>
      </div>

      {error && (
        <div style={{ color: "#d9534f", marginBottom: "12px", padding: "8px", background: "#fdf7f7", border: "1px solid #d9534f" }}>
          {error}
        </div>
      )}

      <div style={{ background: "#f9f9f9", padding: "16px", borderRadius: "4px", marginBottom: "20px" }}>
        <div style={{ marginBottom: "12px" }}>
          <label style={{ display: "block", fontWeight: 600, marginBottom: "4px" }}>
            {message(WF_ADMIN_MSG.WORKFLOW_NAME)}
          </label>
          <input
            type="text"
            value={name}
            disabled={!!workflow} // Name is primary key; disabled on edit
            onChange={(e) => setName(e.target.value)}
            style={{ width: "100%", maxWidth: "400px", padding: "8px" }}
            data-testid="workflow-name-input"
          />
        </div>

        <div style={{ marginBottom: "12px" }}>
          <label style={{ display: "flex", alignItems: "center", cursor: "pointer" }}>
            <input
              type="checkbox"
              checked={isDefault}
              onChange={(e) => setIsDefault(e.target.checked)}
              style={{ marginRight: "8px" }}
              data-testid="workflow-default-checkbox"
            />
            <span>{message(WF_ADMIN_MSG.MAKE_DEFAULT)}</span>
          </label>
        </div>

        <div>
          <label style={{ display: "block", fontWeight: 600, marginBottom: "4px" }}>
            {message(WF_ADMIN_MSG.STAGING_ROLE)}
          </label>
          <select
            value={stagingRoleId}
            onChange={(e) => setStagingRoleId(e.target.value)}
            style={{ width: "100%", maxWidth: "400px", padding: "8px" }}
            data-testid="workflow-staging-role-select"
          >
            <option value="">-- {message(WF_ADMIN_MSG.NONE)} --</option>
            {availableRoles.map((role) => (
              <option key={role} value={role}>
                {role}
              </option>
            ))}
          </select>
        </div>
      </div>

      <WorkflowStepList steps={steps} availableRoles={availableRoles} onChange={setSteps} />
    </form>
  );
};
