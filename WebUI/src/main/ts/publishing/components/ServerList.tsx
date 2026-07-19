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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import { message, MSG } from "../../i18n/message";
import {
  buttonStyle,
  emptyStyle,
  listItemStyle,
  listStyle,
  toolbarStyle,
} from "../publishing.styles";
import type { PublishServer } from "../types";

export interface ServerListProps {
  servers: PublishServer[];
  selectedId: string;
  onSelect: (id: string) => void;
  onRefresh?: () => void;
  onAdd?: () => void;
  emptyMessage?: string;
}

function idOf(s: PublishServer): string {
  return String(s.serverId ?? s.serverName ?? s.name ?? "");
}

function nameOf(s: PublishServer): string {
  return s.serverName ?? s.name ?? idOf(s);
}

export function ServerList({
  servers,
  selectedId,
  onSelect,
  onRefresh,
  onAdd,
  emptyMessage,
}: ServerListProps): React.ReactElement {
  return (
    <div data-testid="publish-server-list">
      <div style={toolbarStyle}>
        {onRefresh && (
          <button type="button" style={buttonStyle} onClick={onRefresh}>
            Refresh
          </button>
        )}
        {onAdd && (
          <button type="button" style={buttonStyle} onClick={onAdd}>
            Add
          </button>
        )}
      </div>
      {servers.length === 0 ? (
        <p style={emptyStyle} data-testid="server-list-empty">
          {emptyMessage ?? message(MSG.PUBLISH_EMPTY_SERVERS)}
        </p>
      ) : (
        <ul style={listStyle}>
          {servers.map((s) => {
            const id = idOf(s);
            const isDefault = Boolean(s.isDefault || s.defaultServer);
            const active = id === selectedId;
            return (
              <li key={id} style={listItemStyle}>
                <button
                  type="button"
                  style={{
                    ...buttonStyle,
                    flex: 1,
                    textAlign: "left",
                    fontWeight: active ? 600 : 400,
                    borderColor: active ? "#0b6" : "#ccc",
                  }}
                  aria-current={active ? "true" : undefined}
                  onClick={() => onSelect(id)}
                >
                  {nameOf(s)}
                  {isDefault ? " ★" : ""}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
