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
import { post } from "../../api/client";
import { PATHS } from "../../api/paths";
import { message } from "../../i18n/message";
import { WF_ADMIN_MSG } from "../messages";
import { Role } from "./RolesSection";

interface RoleEditorProps {
  role: Role | null; // null if creating
  onSave: () => void;
  onCancel: () => void;
}

export const RoleEditor: React.FC<RoleEditorProps> = ({
  role,
  onSave,
  onCancel,
}) => {
  const [name, setName] = useState<string>(role?.name || "");
  const [description, setDescription] = useState<string>(role?.description || "");
  const [assignedUsers, setAssignedUsers] = useState<string[]>(role?.users || []);
  const [availableUsers, setAvailableUsers] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState<boolean>(false);

  useEffect(() => {
    const fetchAvailableUsers = async () => {
      try {
        const payload = {
          Role: {
            name: role?.name || name || "TempRole",
            description,
            users: assignedUsers,
          },
        };
        const res = await post<{ UserList: { users: string[] } }>(
          PATHS.ROLE_AVAILABLE_USERS,
          payload
        );
        setAvailableUsers(res?.UserList?.users || []);
      } catch {
        setAvailableUsers([]);
      }
    };
    fetchAvailableUsers();
  }, [role, assignedUsers]);

  const handleAddUser = (user: string) => {
    setError(null);
    setAssignedUsers((prev) => [...prev, user].sort());
  };

  const handleRemoveUser = async (user: string) => {
    setError(null);
    // Validate remove users if updating
    if (role) {
      try {
        await post(PATHS.ROLE_REMOVE_USERS_VALIDATE, {
          UserList: { users: [user] },
        });
      } catch (err: any) {
        setError(err?.message || "Cannot remove user from this role.");
        return;
      }
    }
    setAssignedUsers((prev) => prev.filter((u) => u !== user));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError(message(WF_ADMIN_MSG.ROLE_NAME_REQUIRED));
      return;
    }
    setIsSaving(true);
    setError(null);

    const payload = {
      Role: {
        name: name.trim(),
        description: description.trim(),
        users: assignedUsers,
        homepage: "Home",
        ...(role ? { oldName: role.name } : {}),
      },
    };

    try {
      if (role) {
        await post(PATHS.ROLE_UPDATE, payload);
      } else {
        await post(PATHS.ROLE_CREATE, payload);
      }
      onSave();
    } catch (err: any) {
      setError(err?.message || message(WF_ADMIN_MSG.ERROR_GENERIC));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <form className="perc-role-editor" onSubmit={handleSubmit} data-testid="perc-role-editor" style={{ padding: "20px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <h3>{role ? message(WF_ADMIN_MSG.EDIT_ROLE) : message(WF_ADMIN_MSG.CREATE_ROLE)}</h3>
        <div style={{ display: "flex", gap: "8px" }}>
          <button type="button" onClick={onCancel} disabled={isSaving}>
            {message(WF_ADMIN_MSG.CANCEL)}
          </button>
          <button type="submit" className="perc-button-primary" disabled={isSaving} data-testid="save-role-button">
            {message(WF_ADMIN_MSG.SAVE)}
          </button>
        </div>
      </div>

      {error && (
        <div
          style={{
            color: "#d9534f",
            marginBottom: "16px",
            padding: "10px",
            background: "#fdf7f7",
            border: "1px solid #d9534f",
            borderRadius: "4px",
          }}
        >
          {error}
        </div>
      )}

      <div style={{ background: "#f8fafc", padding: "20px", borderRadius: "8px", border: "1px solid #e2e8f0", marginBottom: "20px" }}>
        <div style={{ marginBottom: "16px" }}>
          <label style={{ display: "block", fontWeight: 600, marginBottom: "6px" }}>
            {message(WF_ADMIN_MSG.ROLE_NAME)}
          </label>
          <input
            type="text"
            value={name}
            disabled={!!role} // Disabled on edit
            onChange={(e) => setName(e.target.value)}
            style={{ width: "100%", maxWidth: "400px", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
            data-testid="role-name-input"
          />
        </div>

        <div>
          <label style={{ display: "block", fontWeight: 600, marginBottom: "6px" }}>Description</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            style={{ width: "100%", maxWidth: "600px", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
            data-testid="role-description-input"
          />
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "24px" }}>
        {/* Assigned Users list */}
        <div style={{ border: "1px solid #e2e8f0", borderRadius: "8px", background: "#fff", overflow: "hidden" }}>
          <div style={{ background: "#f8fafc", padding: "10px 16px", borderBottom: "1px solid #e2e8f0", fontWeight: 600 }}>
            {message(WF_ADMIN_MSG.ROLE_MEMBERS)} ({assignedUsers.length})
          </div>
          <ul style={{ listStyle: "none", padding: 0, margin: 0, maxHeight: "300px", overflowY: "auto" }} data-testid="assigned-users-list">
            {assignedUsers.length === 0 ? (
              <li style={{ padding: "16px", color: "#94a3b8", textAlign: "center" }}>No users assigned</li>
            ) : (
              assignedUsers.map((user) => (
                <li
                  key={user}
                  style={{
                    padding: "10px 16px",
                    borderBottom: "1px solid #f1f5f9",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <span>{user}</span>
                  <button
                    type="button"
                    onClick={() => handleRemoveUser(user)}
                    style={{
                      padding: "2px 8px",
                      fontSize: "12px",
                      background: "#fef2f2",
                      color: "#b91c1c",
                      border: "1px solid #fee2e2",
                    }}
                    data-testid={`remove-user-${user}`}
                  >
                    Remove
                  </button>
                </li>
              ))
            )}
          </ul>
        </div>

        {/* Available Users list */}
        <div style={{ border: "1px solid #e2e8f0", borderRadius: "8px", background: "#fff", overflow: "hidden" }}>
          <div style={{ background: "#f8fafc", padding: "10px 16px", borderBottom: "1px solid #e2e8f0", fontWeight: 600 }}>
            {message(WF_ADMIN_MSG.AVAILABLE_USERS)} ({availableUsers.length})
          </div>
          <ul style={{ listStyle: "none", padding: 0, margin: 0, maxHeight: "300px", overflowY: "auto" }} data-testid="available-users-list">
            {availableUsers.length === 0 ? (
              <li style={{ padding: "16px", color: "#94a3b8", textAlign: "center" }}>No available users</li>
            ) : (
              availableUsers.map((user) => (
                <li
                  key={user}
                  style={{
                    padding: "10px 16px",
                    borderBottom: "1px solid #f1f5f9",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <span>{user}</span>
                  <button
                    type="button"
                    onClick={() => handleAddUser(user)}
                    style={{
                      padding: "2px 8px",
                      fontSize: "12px",
                      background: "#f0fdf4",
                      color: "#15803d",
                      border: "1px solid #dcfce7",
                    }}
                    data-testid={`add-user-${user}`}
                  >
                    Add
                  </button>
                </li>
              ))
            )}
          </ul>
        </div>
      </div>
    </form>
  );
};
