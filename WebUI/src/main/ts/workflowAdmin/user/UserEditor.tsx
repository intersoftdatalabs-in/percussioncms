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
import { get, post, put } from "../../api/client";
import { PATHS } from "../../api/paths";
import { message } from "../../i18n/message";
import { WF_ADMIN_MSG } from "../messages";
import { User } from "./UsersSection";

interface UserEditorProps {
  user: User | null;
  onSave: () => void;
  onCancel: () => void;
}

export const UserEditor: React.FC<UserEditorProps> = ({
  user,
  onSave,
  onCancel,
}) => {
  const [name, setName] = useState<string>(user?.name || "");
  const [email, setEmail] = useState<string>(user?.email || "");
  const [password, setPassword] = useState<string>("");
  const [confirmPassword, setConfirmPassword] = useState<string>("");
  const [assignedRoles, setAssignedRoles] = useState<string[]>(user?.roles || []);
  const [availableRoles, setAvailableRoles] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState<boolean>(false);

  useEffect(() => {
    const fetchRoles = async () => {
      try {
        const res = await get<{ RoleList: { roles: string[] } }>(PATHS.USER_ROLES);
        setAvailableRoles(res?.RoleList?.roles || []);
      } catch {
        setAvailableRoles([]);
      }
    };
    fetchRoles();
  }, []);

  const handleRoleToggle = (role: string) => {
    setAssignedRoles((prev) =>
      prev.includes(role) ? prev.filter((r) => r !== role) : [...prev, role]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError(message(WF_ADMIN_MSG.USER_NAME_REQUIRED));
      return;
    }
    if ((!user || password) && password !== confirmPassword) {
      setError(message(WF_ADMIN_MSG.PASSWORDS_DONOT_MATCH));
      return;
    }
    setIsSaving(true);
    setError(null);

    const userPayload = {
      User: {
        name: name.trim(),
        email: email.trim(),
        providerType: user?.providerType || "INTERNAL",
        roles: assignedRoles,
        createUser: !user,
        ...(password && !user ? { password } : {}),
      },
    };

    try {
      if (user) {
        // Update user metadata (roles and email)
        await post(PATHS.USER_UPDATE, userPayload);
        // If password fields are filled during edit, perform changepw
        if (password) {
          await put(PATHS.USER_CHANGE_PW, {
            User: {
              name: name.trim(),
              password,
              email: email.trim(),
              providerType: user.providerType,
              roles: assignedRoles,
            },
          });
        }
      } else {
        // Create new internal user
        await post(PATHS.USER_CREATE, userPayload);
      }
      onSave();
    } catch (err: any) {
      setError(err?.message || message(WF_ADMIN_MSG.ERROR_GENERIC));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <form className="perc-user-editor" onSubmit={handleSubmit} data-testid="perc-user-editor" style={{ padding: "20px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <h3>{user ? message(WF_ADMIN_MSG.EDIT_USER) : message(WF_ADMIN_MSG.CREATE_USER)}</h3>
        <div style={{ display: "flex", gap: "8px" }}>
          <button type="button" onClick={onCancel} disabled={isSaving}>
            {message(WF_ADMIN_MSG.CANCEL)}
          </button>
          <button type="submit" className="perc-button-primary" disabled={isSaving} data-testid="save-user-button">
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
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginBottom: "16px" }}>
          <div>
            <label style={{ display: "block", fontWeight: 600, marginBottom: "6px" }}>
              {message(WF_ADMIN_MSG.USER_NAME)}
            </label>
            <input
              type="text"
              value={name}
              disabled={!!user}
              onChange={(e) => setName(e.target.value)}
              style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
              data-testid="user-name-input"
            />
          </div>
          <div>
            <label style={{ display: "block", fontWeight: 600, marginBottom: "6px" }}>
              {message(WF_ADMIN_MSG.EMAIL)}
            </label>
            <input
              type="email"
              value={email}
              disabled={user?.providerType === "DIRECTORY"} // Disabled for imported LDAP users
              onChange={(e) => setEmail(e.target.value)}
              style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
              data-testid="user-email-input"
            />
          </div>
        </div>

        {user?.providerType !== "DIRECTORY" && (
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
            <div>
              <label style={{ display: "block", fontWeight: 600, marginBottom: "6px" }}>
                {message(WF_ADMIN_MSG.PASSWORD)}
              </label>
              <input
                type="password"
                value={password}
                placeholder={user ? message(WF_ADMIN_MSG.PASSWORD_PLACEHOLDER) : ""}
                onChange={(e) => setPassword(e.target.value)}
                style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
                data-testid="user-password-input"
              />
            </div>
            <div>
              <label style={{ display: "block", fontWeight: 600, marginBottom: "6px" }}>
                {message(WF_ADMIN_MSG.CONFIRM_PASSWORD)}
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
                data-testid="user-confirm-password-input"
              />
            </div>
          </div>
        )}
      </div>

      <div style={{ border: "1px solid #e2e8f0", borderRadius: "8px", background: "#fff", overflow: "hidden" }}>
        <div style={{ background: "#f8fafc", padding: "10px 16px", borderBottom: "1px solid #e2e8f0", fontWeight: 600 }}>
          {message(WF_ADMIN_MSG.USER_ROLES)}
        </div>
        <div style={{ padding: "16px", display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: "12px" }}>
          {availableRoles.map((role) => (
            <label key={role} style={{ display: "flex", alignItems: "center", gap: "8px", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={assignedRoles.includes(role)}
                onChange={() => handleRoleToggle(role)}
                data-testid={`role-checkbox-${role}`}
              />
              <span>{role}</span>
            </label>
          ))}
        </div>
      </div>
    </form>
  );
};
