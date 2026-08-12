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
import { get, post } from "../../api/client";
import { asStringArray, parseRoleNameList } from "../../api/jsonList";
import { PATHS } from "../../api/paths";
import { message } from "../../i18n/message";
import { WF_ADMIN_MSG } from "../messages";
import { RoleEditor } from "./RoleEditor";

export interface Role {
  name: string;
  description: string;
  users: string[];
}

export const RolesSection: React.FC = () => {
  const [roles, setRoles] = useState<Role[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [isCreating, setIsCreating] = useState<boolean>(false);

  const loadRoles = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await get<unknown>(PATHS.USER_ROLES);
      const roleNames = parseRoleNameList(res);

      const details = await Promise.all(
        roleNames.map(async (name) => {
          try {
            const r = await post<{ Role?: Role }>(PATHS.ROLES_FIND, {
              psstring: { value: name },
            });
            const role = r?.Role || { name, description: "", users: [] };
            return {
              name: role.name || name,
              description: role.description || "",
              users: asStringArray(role.users),
            };
          } catch {
            return { name, description: "", users: [] };
          }
        })
      );
      setRoles(details);
    } catch (err) {
      setError(message(WF_ADMIN_MSG.ERROR_GENERIC));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadRoles();
  }, []);

  const handleDelete = async (roleName: string) => {
    if (!window.confirm(message(WF_ADMIN_MSG.CONFIRM_DELETE_ROLE).replace("{0}", roleName))) {
      return;
    }
    setError(null);
    try {
      // Validate delete first
      await post(PATHS.ROLE_DELETE_VALIDATE, { Role: { name: roleName, description: "", users: [] } });
      // Perform delete
      await post(PATHS.ROLE_DELETE, { psstring: { value: roleName } });
      loadRoles();
    } catch (err: any) {
      setError(err?.message || message(WF_ADMIN_MSG.DELETE_FAILED));
    }
  };

  if (editingRole || isCreating) {
    return (
      <RoleEditor
        role={editingRole}
        onSave={() => {
          setEditingRole(null);
          setIsCreating(false);
          loadRoles();
        }}
        onCancel={() => {
          setEditingRole(null);
          setIsCreating(false);
        }}
      />
    );
  }

  return (
    <div className="perc-roles-section" data-testid="perc-roles-section" style={{ padding: "20px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <h2>{message(WF_ADMIN_MSG.ROLES_TITLE)}</h2>
        <button
          type="button"
          className="perc-button-primary"
          onClick={() => setIsCreating(true)}
          data-testid="create-role-button"
        >
          {message(WF_ADMIN_MSG.CREATE_ROLE)}
        </button>
      </div>

      {isLoading ? (
        <div>{message(WF_ADMIN_MSG.LOADING)}</div>
      ) : error ? (
        <div style={{ color: "#d9534f" }}>{error}</div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "16px" }}>
          {roles.map((role) => (
            <div
              key={role.name}
              data-testid={`role-card-${role.name}`}
              style={{
                background: "#ffffff",
                border: "1px solid #e2e8f0",
                borderRadius: "8px",
                padding: "16px",
                boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
                display: "flex",
                flexDirection: "column",
                justifyContent: "space-between",
              }}
            >
              <div>
                <h4 style={{ margin: "0 0 8px 0", color: "#1a202c" }}>{role.name}</h4>
                <p style={{ margin: "0 0 12px 0", color: "#718096", fontSize: "14px" }}>
                  {role.description || "No description provided."}
                </p>
                <div style={{ fontSize: "13px", color: "#4a5568" }}>
                  <strong>Users ({role.users.length}):</strong>{" "}
                  {role.users.join(", ") || "None"}
                </div>
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px", marginTop: "16px" }}>
                <button
                  type="button"
                  onClick={() => setEditingRole(role)}
                  data-testid={`edit-role-${role.name}`}
                >
                  {message(WF_ADMIN_MSG.EDIT)}
                </button>
                <button
                  type="button"
                  onClick={() => handleDelete(role.name)}
                  data-testid={`delete-role-${role.name}`}
                  style={{ color: "#d9534f" }}
                >
                  {message(WF_ADMIN_MSG.DELETE)}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
