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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useState } from "react";
import { fetchFolderChildren } from "../../api/home/homeApi";
import type { FolderChild } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  listItemStyle,
  listStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface SiteRootBrowserProps {
  /** Starting CMS path (e.g. site folder root) */
  rootPath: string;
  onSelectPath?: (path: string) => void;
}

/**
 * Folder browser for scheme path selection — reuses pathmanagement folder API
 * (same as Home library), not a new engine.
 */
export function SiteRootBrowser({
  rootPath,
  onSelectPath,
}: SiteRootBrowserProps): React.ReactElement {
  const [path, setPath] = useState(rootPath || "/");
  const [children, setChildren] = useState<FolderChild[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setPath(rootPath || "/");
  }, [rootPath]);

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetchFolderChildren(path)
      .then(setChildren)
      .catch(() => {
        setChildren([]);
        setError(message(MSG.PUBLISH_ERROR));
      })
      .finally(() => setLoading(false));
  }, [path]);

  function goUp(): void {
    const trimmed = path.replace(/\/+$/, "");
    const idx = trimmed.lastIndexOf("/");
    if (idx <= 0) {
      setPath("/");
      return;
    }
    setPath(trimmed.slice(0, idx) || "/");
  }

  function openChild(child: FolderChild): void {
    const childPath =
      child.path ??
      (path.endsWith("/") ? `${path}${child.name}` : `${path}/${child.name}`);
    if (child.folder || child.type === "folder" || child.type === "Folder") {
      setPath(childPath);
    }
    onSelectPath?.(childPath);
  }

  return (
    <div data-testid="site-root-browser">
      <div style={toolbarStyle}>
        <button type="button" style={buttonStyle} onClick={goUp}>
          Up
        </button>
        <code style={{ fontSize: "0.85rem" }}>{path}</code>
        {onSelectPath && (
          <button
            type="button"
            style={buttonStyle}
            onClick={() => onSelectPath(path)}
          >
            Use this path
          </button>
        )}
      </div>
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      {!loading && children.length === 0 && (
        <p style={emptyStyle}>Empty folder or path not found.</p>
      )}
      <ul style={listStyle}>
        {children.map((c) => (
          <li key={c.path ?? c.name} style={listItemStyle}>
            <button type="button" style={buttonStyle} onClick={() => openChild(c)}>
              {c.folder || c.type === "folder" ? "📁 " : "📄 "}
              {c.name}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
