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
import { WF_ADMIN_MSG } from "../messages";

export interface WorkflowStep {
  name: string;
  roleNames: string[];
  position: number;
}

interface WorkflowStepListProps {
  steps: WorkflowStep[];
  availableRoles: string[];
  onChange: (steps: WorkflowStep[]) => void;
}

export const WorkflowStepList: React.FC<WorkflowStepListProps> = ({
  steps,
  availableRoles,
  onChange,
}) => {
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [stepName, setStepName] = useState("");
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);

  const handleOpenAdd = () => {
    setEditingIndex(-1);
    setStepName("");
    setSelectedRoles([]);
  };

  const handleOpenEdit = (index: number) => {
    const s = steps[index];
    setEditingIndex(index);
    setStepName(s.name);
    setSelectedRoles([...s.roleNames]);
  };

  const handleSaveStep = () => {
    if (!stepName.trim()) return;
    const updated = [...steps];
    if (editingIndex === -1) {
      updated.push({
        name: stepName.trim(),
        roleNames: selectedRoles,
        position: updated.length + 1,
      });
    } else if (editingIndex !== null && editingIndex >= 0) {
      updated[editingIndex] = {
        ...updated[editingIndex],
        name: stepName.trim(),
        roleNames: selectedRoles,
      };
    }
    onChange(updated);
    setEditingIndex(null);
  };

  const handleDeleteStep = (index: number) => {
    const updated = steps.filter((_, i) => i !== index).map((s, i) => ({ ...s, position: i + 1 }));
    onChange(updated);
  };

  const handleMove = (index: number, direction: "up" | "down") => {
    const targetIndex = direction === "up" ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= steps.length) return;
    const updated = [...steps];
    const temp = updated[index];
    updated[index] = updated[targetIndex];
    updated[targetIndex] = temp;
    onChange(updated.map((s, i) => ({ ...s, position: i + 1 })));
  };

  const toggleRole = (role: string) => {
    if (selectedRoles.includes(role)) {
      setSelectedRoles(selectedRoles.filter((r) => r !== role));
    } else {
      setSelectedRoles([...selectedRoles, role]);
    }
  };

  return (
    <div className="perc-workflow-step-list" data-testid="perc-workflow-step-list">
      <div className="perc-step-list-header" style={{ display: "flex", justifyContent: "space-between", marginBottom: "12px" }}>
        <h4>{message(WF_ADMIN_MSG.SECTION_STEPS)}</h4>
        <button
          type="button"
          className="perc-button-primary"
          onClick={handleOpenAdd}
          data-testid="add-step-button"
        >
          {message(WF_ADMIN_MSG.ADD_STEP)}
        </button>
      </div>

      <table className="perc-table" style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr style={{ borderBottom: "2px solid #ccc", textAlign: "left" }}>
            <th style={{ padding: "8px" }}>{message(WF_ADMIN_MSG.TABLE_HASH)}</th>
            <th style={{ padding: "8px" }}>{message(WF_ADMIN_MSG.STEP_NAME)}</th>
            <th style={{ padding: "8px" }}>{message(WF_ADMIN_MSG.STEP_ROLES)}</th>
            <th style={{ padding: "8px", textAlign: "right" }}>{message(WF_ADMIN_MSG.TABLE_ACTIONS)}</th>
          </tr>
        </thead>
        <tbody>
          {steps.length === 0 ? (
            <tr>
              <td colSpan={4} style={{ padding: "16px", textAlign: "center", color: "#666" }}>
                {message(WF_ADMIN_MSG.NO_STEPS_DEFINED)}
              </td>
            </tr>
          ) : (
            steps.map((step, idx) => (
              <tr key={step.name || `step-${idx}`} style={{ borderBottom: "1px solid #eee" }} data-testid={`step-row-${idx}`}>
                <td style={{ padding: "8px" }}>{idx + 1}</td>
                <td style={{ padding: "8px", fontWeight: 600 }}>{step.name}</td>
                <td style={{ padding: "8px" }}>
                  {step.roleNames.length > 0 ? step.roleNames.join(", ") : <em>{message(WF_ADMIN_MSG.NONE)}</em>}
                </td>
                <td style={{ padding: "8px", textAlign: "right" }}>
                  <button
                    type="button"
                    disabled={idx === 0}
                    onClick={() => handleMove(idx, "up")}
                    style={{ marginRight: "4px" }}
                    aria-label="Move Up"
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    disabled={idx === steps.length - 1}
                    onClick={() => handleMove(idx, "down")}
                    style={{ marginRight: "8px" }}
                    aria-label="Move Down"
                  >
                    ↓
                  </button>
                  <button
                    type="button"
                    onClick={() => handleOpenEdit(idx)}
                    style={{ marginRight: "4px" }}
                  >
                    {message(WF_ADMIN_MSG.EDIT)}
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDeleteStep(idx)}
                    style={{ color: "#d9534f" }}
                  >
                    {message(WF_ADMIN_MSG.DELETE)}
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>

      {editingIndex !== null && (
        <div
          className="perc-dialog-backdrop"
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(0,0,0,0.4)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
          data-testid="step-edit-dialog"
        >
          <div style={{ background: "#fff", padding: "24px", borderRadius: "8px", width: "400px" }}>
            <h3>{editingIndex === -1 ? message(WF_ADMIN_MSG.ADD_STEP_TITLE) : message(WF_ADMIN_MSG.EDIT_STEP_TITLE)}</h3>
            <div style={{ marginBottom: "12px" }}>
              <label style={{ display: "block", marginBottom: "4px" }}>
                {message(WF_ADMIN_MSG.STEP_NAME)}
              </label>
              <input
                type="text"
                value={stepName}
                onChange={(e) => setStepName(e.target.value)}
                style={{ width: "100%", padding: "8px" }}
                data-testid="step-name-input"
              />
            </div>
            <div style={{ marginBottom: "16px" }}>
              <label style={{ display: "block", marginBottom: "4px" }}>
                {message(WF_ADMIN_MSG.STEP_ROLES)}
              </label>
              <div style={{ maxHeight: "150px", overflowY: "auto", border: "1px solid #ccc", padding: "8px" }}>
                {availableRoles.length === 0 ? (
                  <span style={{ color: "#666" }}>{message(WF_ADMIN_MSG.NO_ROLES_AVAILABLE)}</span>
                ) : (
                  availableRoles.map((role) => (
                    <label key={role} style={{ display: "block", marginBottom: "4px" }}>
                      <input
                        type="checkbox"
                        checked={selectedRoles.includes(role)}
                        onChange={() => toggleRole(role)}
                        style={{ marginRight: "8px" }}
                      />
                      {role}
                    </label>
                  ))
                )}
              </div>
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
              <button type="button" onClick={() => setEditingIndex(null)}>
                {message(WF_ADMIN_MSG.CANCEL)}
              </button>
              <button type="button" className="perc-button-primary" onClick={handleSaveStep} data-testid="save-step-button">
                {message(WF_ADMIN_MSG.SAVE)}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
