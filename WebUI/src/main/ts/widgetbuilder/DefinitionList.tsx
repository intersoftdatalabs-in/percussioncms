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

import React from "react";
import { message } from "../i18n/message";
import type { WidgetSummary } from "./types";

const K = {
  EMPTY: "perc.ui.widgetbuilder.modern@Empty",
  NEW: "perc.ui.widgetbuilder.modern@New",
  EDIT: "perc.ui.widgetbuilder.modern@Edit",
  DELETE: "perc.ui.widgetbuilder.modern@Delete",
  DEPLOY: "perc.ui.widgetbuilder.modern@Deploy",
};

export interface DefinitionListProps {
  summaries: WidgetSummary[];
  onNew: () => void;
  onEdit: (id: number | string) => void;
  onDelete: (id: number | string) => void;
  onDeploy: (id: number | string) => void;
}

export function DefinitionList({
  summaries,
  onNew,
  onEdit,
  onDelete,
  onDeploy,
}: DefinitionListProps): React.ReactElement {
  return (
    <div data-testid="wb-definition-list">
      <div style={{ marginBottom: 12 }}>
        <button type="button" onClick={onNew}>
          {message(K.NEW)}
        </button>
      </div>
      {summaries.length === 0 ? (
        <p>{message(K.EMPTY)}</p>
      ) : (
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr>
              <th align="left">Label</th>
              <th align="left">Prefix</th>
              <th align="left">Version</th>
              <th align="left">Actions</th>
            </tr>
          </thead>
          <tbody>
            {summaries.map((s) => {
              const id = s.widgetId ?? "";
              return (
                <tr key={String(id)} style={{ borderTop: "1px solid #eee" }}>
                  <td style={{ padding: 8 }}>{s.label ?? id}</td>
                  <td style={{ padding: 8 }}>{s.prefix}</td>
                  <td style={{ padding: 8 }}>{s.version}</td>
                  <td style={{ padding: 8, display: "flex", gap: 8 }}>
                    <button type="button" onClick={() => onEdit(id)}>
                      {message(K.EDIT)}
                    </button>
                    <button type="button" onClick={() => onDeploy(id)}>
                      {message(K.DEPLOY)}
                    </button>
                    <button type="button" onClick={() => onDelete(id)}>
                      {message(K.DELETE)}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}
