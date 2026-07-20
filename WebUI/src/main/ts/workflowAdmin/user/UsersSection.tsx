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

import React, { useEffect, useState } from "react";
import { get, del } from "../../api/client";
import { PATHS } from "../../api/paths";
import { message } from "../../i18n/message";
import { WF_ADMIN_MSG } from "../messages";
import { UserEditor } from "./UserEditor";
import { LdapImportDialog } from "./LdapImportDialog";

export interface User {
  name: string;
  email?: string;
  providerType: "INTERNAL" | "DIRECTORY";
  roles: string[];
}

export const UsersSection: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [isCreating, setIsCreating] = useState<boolean>(false);
  const [isLdapOpen, setIsLdapOpen] = useState<boolean>(false);

  const loadUsers = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await get<{ UserList: { users: string[] } }>(PATHS.USERS);
      const userNames = res?.UserList?.users || [];

      const details = await Promise.all(
        userNames.map(async (name) => {
          try {
            const u = await get<User>(`${PATHS.USER_FIND}/${encodeURIComponent(name)}`);
            return u;
          } catch {
            return { name, providerType: "INTERNAL" as const, roles: [] };
          }
        })
      );
      setUsers(details);
    } catch (err) {
      setError(message(WF_ADMIN_MSG.ERROR_GENERIC));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleDelete = async (userName: string) => {
    if (!window.confirm(message(WF_ADMIN_MSG.CONFIRM_DELETE_USER).replace("{0}", userName))) {
      return;
    }
    setError(null);
    try {
      await del(`${PATHS.USER_DELETE}/${encodeURIComponent(userName)}`);
      loadUsers();
    } catch (err: any) {
      setError(err?.message || "Failed to delete user.");
    }
  };

  if (editingUser || isCreating) {
    return (
      <UserEditor
        user={editingUser}
        onSave={() => {
          setEditingUser(null);
          setIsCreating(false);
          loadUsers();
        }}
        onCancel={() => {
          setEditingUser(null);
          setIsCreating(false);
        }}
      />
    );
  }

  return (
    <div className="perc-users-section" data-testid="perc-users-section" style={{ padding: "20px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <h2>{message(WF_ADMIN_MSG.USERS_TITLE)}</h2>
        <div style={{ display: "flex", gap: "10px" }}>
          <button
            type="button"
            className="perc-button-secondary"
            onClick={() => setIsLdapOpen(true)}
            data-testid="ldap-import-button"
          >
            {message(WF_ADMIN_MSG.LDAP_IMPORT)}
          </button>
          <button
            type="button"
            className="perc-button-primary"
            onClick={() => setIsCreating(true)}
            data-testid="create-user-button"
          >
            {message(WF_ADMIN_MSG.CREATE_USER)}
          </button>
        </div>
      </div>

      {error && (
        <div style={{ color: "#d9534f", marginBottom: "16px" }}>{error}</div>
      )}

      {isLoading ? (
        <div>{message(WF_ADMIN_MSG.LOADING)}</div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "16px" }}>
          {users.map((user) => (
            <div
              key={user.name}
              data-testid={`user-card-${user.name}`}
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
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
                  <h4 style={{ margin: "0 0 8px 0", color: "#1a202c" }}>{user.name}</h4>
                  <span
                    style={{
                      fontSize: "11px",
                      fontWeight: 600,
                      padding: "2px 6px",
                      borderRadius: "4px",
                      background: user.providerType === "DIRECTORY" ? "#e0f2fe" : "#f1f5f9",
                      color: user.providerType === "DIRECTORY" ? "#0369a1" : "#475569",
                    }}
                  >
                    {user.providerType}
                  </span>
                </div>
                <p style={{ margin: "0 0 12px 0", color: "#64748b", fontSize: "14px" }}>
                  {user.email || "No email address provided."}
                </p>
                <div style={{ fontSize: "13px", color: "#475569" }}>
                  <strong>Roles ({user.roles?.length || 0}):</strong>{" "}
                  {user.roles?.join(", ") || "None"}
                </div>
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px", marginTop: "16px" }}>
                <button
                  type="button"
                  onClick={() => setEditingUser(user)}
                  data-testid={`edit-user-${user.name}`}
                >
                  {message(WF_ADMIN_MSG.EDIT)}
                </button>
                <button
                  type="button"
                  onClick={() => handleDelete(user.name)}
                  data-testid={`delete-user-${user.name}`}
                  style={{ color: "#d9534f" }}
                >
                  {message(WF_ADMIN_MSG.DELETE)}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {isLdapOpen && (
        <LdapImportDialog
          onClose={() => {
            setIsLdapOpen(false);
            loadUsers();
          }}
        />
      )}
    </div>
  );
};
