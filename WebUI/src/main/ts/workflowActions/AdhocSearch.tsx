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

import React, { useState, useEffect, useRef } from "react";
import { get } from "../api/client";
import { PATHS } from "../api/paths";

interface AdhocSearchProps {
  onSelect: (users: string[]) => void;
  selectedUsers: string[];
}

export const AdhocSearch: React.FC<AdhocSearchProps> = ({
  onSelect,
  selectedUsers,
}) => {
  const [query, setQuery] = useState<string>("");
  const [results, setResults] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const isMountedRef = useRef<boolean>(true);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      return;
    }

    const delayDebounce = setTimeout(async () => {
      setIsLoading(true);
      try {
        // Query users using the endpoint
        const res = await get<{ UserList: { users: string[] } }>(
          `${PATHS.USERS}/names/${encodeURIComponent(query.trim())}`
        );
        if (isMountedRef.current) {
          setResults(res?.UserList?.users || []);
        }
      } catch {
        if (isMountedRef.current) {
          setResults([]);
        }
      } finally {
        if (isMountedRef.current) {
          setIsLoading(false);
        }
      }
    }, 300);

    return () => clearTimeout(delayDebounce);
  }, [query]);

  const handleAdd = (user: string) => {
    if (!selectedUsers.includes(user)) {
      onSelect([...selectedUsers, user]);
    }
    setQuery("");
  };

  const handleRemove = (user: string) => {
    onSelect(selectedUsers.filter((u) => u !== user));
  };

  return (
    <div className="perc-adhoc-search" data-testid="perc-adhoc-search" style={{ marginTop: "12px" }}>
      <label style={{ display: "block", fontWeight: 600, marginBottom: "6px", fontSize: "14px" }}>
        Ad-hoc Reviewers
      </label>
      <div style={{ display: "flex", flexWrap: "wrap", gap: "6px", marginBottom: "8px" }}>
        {selectedUsers.map((user) => (
          <span
            key={user}
            style={{
              background: "#eff6ff",
              color: "#1e40af",
              border: "1px solid #bfdbfe",
              borderRadius: "4px",
              padding: "2px 8px",
              fontSize: "12px",
              display: "inline-flex",
              alignItems: "center",
              gap: "4px",
            }}
          >
            {user}
            <button
              type="button"
              onClick={() => handleRemove(user)}
              style={{ background: "none", border: "none", color: "#1e40af", cursor: "pointer", padding: 0 }}
            >
              &times;
            </button>
          </span>
        ))}
      </div>
      <div style={{ position: "relative" }}>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search users to add..."
          style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e1" }}
          data-testid="adhoc-search-input"
        />
        {isLoading && (
          <div style={{ position: "absolute", right: "10px", top: "10px", fontSize: "12px", color: "#94a3b8" }}>
            Searching...
          </div>
        )}
        {results.length > 0 && (
          <ul
            style={{
              position: "absolute",
              left: 0,
              right: 0,
              top: "100%",
              background: "#fff",
              border: "1px solid #cbd5e1",
              borderRadius: "4px",
              margin: "4px 0 0 0",
              padding: 0,
              listStyle: "none",
              maxHeight: "150px",
              overflowY: "auto",
              zIndex: 10,
              boxShadow: "0 4px 6px rgba(0,0,0,0.05)",
            }}
            data-testid="adhoc-search-results"
          >
            {results.map((user) => (
              <li
                key={user}
                onClick={() => handleAdd(user)}
                style={{ padding: "8px 12px", cursor: "pointer", borderBottom: "1px solid #f1f5f9" }}
              >
                {user}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
};
