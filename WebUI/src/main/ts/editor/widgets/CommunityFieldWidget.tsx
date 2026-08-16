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
import { listCommunities } from "../../api/developer/assemblyApi";
import type { CommunitySummary } from "../../api/developer/types";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import styles from "../EditorHost.module.css";

export function communityOptionValue(row: CommunitySummary): string {
  if (row.id != null && Number.isFinite(row.id)) {
    return String(row.id);
  }
  const guid = row.guid?.stringValue ?? row.guid?.untypedString ?? "";
  return guid;
}

export interface CommunityFieldWidgetProps {
  name: string;
  value: string;
  readOnly: boolean;
  onChange: (value: string) => void;
  loadCommunities?: () => Promise<CommunitySummary[]>;
}

export function CommunityFieldWidget({
  name,
  value,
  readOnly,
  onChange,
  loadCommunities = listCommunities,
}: CommunityFieldWidgetProps): React.ReactElement {
  const [rows, setRows] = useState<CommunitySummary[]>([]);

  useEffect(() => {
    let cancelled = false;
    void loadCommunities()
      .then((list) => {
        if (!cancelled) {
          setRows(list);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRows([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [loadCommunities]);

  return (
    <select
      className={`${styles.input} ${readOnly ? styles.readonly : ""}`}
      data-testid={`editor-field-${name}`}
      data-editor-kind="community"
      name={name}
      value={value}
      disabled={readOnly}
      onChange={(e) => onChange(e.target.value)}
    >
      <option value="">{message(EDITOR_MSG.COMMUNITY_EMPTY)}</option>
      {rows.map((row) => {
        const id = communityOptionValue(row);
        if (!id) {
          return null;
        }
        return (
          <option key={id} value={id}>
            {row.label || row.name || id}
          </option>
        );
      })}
    </select>
  );
}
