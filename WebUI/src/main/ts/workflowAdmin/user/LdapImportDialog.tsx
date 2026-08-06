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

import React, { useEffect, useState, useRef } from "react";
import { get, post } from "../../api/client";
import { PATHS } from "../../api/paths";
import { message } from "../../i18n/message";
import { WF_ADMIN_MSG } from "../messages";

interface LdapImportDialogProps {
  onClose: () => void;
}

interface ExternalUser {
  name: string;
}

export const LdapImportDialog: React.FC<LdapImportDialogProps> = ({ onClose }) => {
  const [isEnabled, setIsEnabled] = useState<boolean>(false);
  const [statusChecked, setStatusChecked] = useState<boolean>(false);
  const [query, setQuery] = useState<string>("");
  const [searchResults, setSearchResults] = useState<ExternalUser[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  
  const isMountedRef = useRef<boolean>(true);

  useEffect(() => {
    isMountedRef.current = true;
    const checkStatus = async () => {
      try {
        const statusRes = await get<{ DirectoryServiceStatus: { status: string } }>(
          PATHS.USER_LDAP_STATUS
        );
        if (isMountedRef.current) {
          setIsEnabled(statusRes?.DirectoryServiceStatus?.status === "ENABLED");
          setStatusChecked(true);
        }
      } catch {
        if (isMountedRef.current) {
          setIsEnabled(false);
          setStatusChecked(true);
        }
      }
    };
    checkStatus();

    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;
    setIsLoading(true);
    setError(null);
    setSuccessMsg(null);
    try {
      const res = await get<ExternalUser[]>(
        `${PATHS.USER_LDAP_FIND}/${encodeURIComponent(query.trim())}`
      );
      if (isMountedRef.current) {
        setSearchResults(res || []);
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || "LDAP Search failed.");
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  };

  const handleToggleSelect = (name: string) => {
    setSelectedUsers((prev) =>
      prev.includes(name) ? prev.filter((u) => u !== name) : [...prev, name]
    );
  };

  const handleImport = async () => {
    if (selectedUsers.length === 0) return;
    setIsLoading(true);
    setError(null);
    setSuccessMsg(null);

    const payload = {
      ImportUsers: {
        externalUsers: selectedUsers.map((name) => ({ name })),
      },
    };

    try {
      await post(PATHS.USER_LDAP_IMPORT, payload);
      if (isMountedRef.current) {
        setSuccessMsg(message(WF_ADMIN_MSG.IMPORT_SUCCESS));
        setSelectedUsers([]);
        setSearchResults([]);
        setQuery("");
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || "Import failed.");
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  };

  return (
    <div
      className="perc-ldap-dialog-overlay"
      data-testid="perc-ldap-dialog-overlay"
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: "rgba(0, 0, 0, 0.5)",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        zIndex: 1000,
      }}
    >
      <div
        style={{
          background: "#fff",
          padding: "24px",
          borderRadius: "8px",
          width: "100%",
          maxWidth: "500px",
          boxShadow: "0 10px 25px rgba(0,0,0,0.1)",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
          <h3 style={{ margin: 0 }}>{message(WF_ADMIN_MSG.LDAP_IMPORT)}</h3>
          <button type="button" onClick={onClose} style={{ background: "none", border: "none", fontSize: "20px", cursor: "pointer" }}>
            &times;
          </button>
        </div>

        {!statusChecked ? (
          <div>{message(WF_ADMIN_MSG.LOADING)}</div>
        ) : !isEnabled ? (
          <div style={{ color: "#d9534f", padding: "12px", background: "#fdf7f7", borderRadius: "4px", border: "1px solid #d9534f" }}>
            {message(WF_ADMIN_MSG.LDAP_STATUS_DISABLED)}
          </div>
        ) : (
          <div>
            <form onSubmit={handleSearch} style={{ display: "flex", gap: "8px", marginBottom: "16px" }}>
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={message(WF_ADMIN_MSG.LDAP_SEARCH_PLACEHOLDER)}
                style={{ flex: 1, padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
                data-testid="ldap-search-input"
              />
              <button type="submit" className="perc-button-primary" data-testid="ldap-search-submit">
                {message(WF_ADMIN_MSG.LDAP_SEARCH_BTN)}
              </button>
            </form>

            {error && (
              <div style={{ color: "#d9534f", marginBottom: "12px", fontSize: "14px" }}>{error}</div>
            )}
            {successMsg && (
              <div style={{ color: "#15803d", marginBottom: "12px", fontSize: "14px" }}>{successMsg}</div>
            )}

            {isLoading ? (
              <div>{message(WF_ADMIN_MSG.LOADING)}</div>
            ) : (
              <div style={{ border: "1px solid #e2e8f0", borderRadius: "6px", maxHeight: "200px", overflowY: "auto", marginBottom: "16px" }}>
                {searchResults.length === 0 ? (
                  <div style={{ padding: "12px", color: "#94a3b8", textAlign: "center" }}>{message(WF_ADMIN_MSG.NO_USERS_FOUND)}</div>
                ) : (
                  searchResults.map((user) => (
                    <label
                      key={user.name}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "8px",
                        padding: "8px 12px",
                        borderBottom: "1px solid #f1f5f9",
                        cursor: "pointer",
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={selectedUsers.includes(user.name)}
                        onChange={() => handleToggleSelect(user.name)}
                        data-testid={`ldap-select-${user.name}`}
                      />
                      <span>{user.name}</span>
                    </label>
                  ))
                )}
              </div>
            )}

            <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
              <button type="button" onClick={onClose}>
                {message(WF_ADMIN_MSG.CANCEL)}
              </button>
              <button
                type="button"
                className="perc-button-primary"
                onClick={handleImport}
                disabled={selectedUsers.length === 0}
                data-testid="ldap-import-submit"
              >
                {message(WF_ADMIN_MSG.IMPORT)} ({selectedUsers.length})
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
