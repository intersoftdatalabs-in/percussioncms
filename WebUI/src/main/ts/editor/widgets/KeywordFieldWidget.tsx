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

import React, { useEffect, useMemo, useState } from "react";
import { listKeywords } from "../../api/developer/keywordsApi";
import type { KeywordChoiceSummary, KeywordSummary } from "../../api/developer/types";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import styles from "../EditorHost.module.css";

export interface KeywordOption {
  value: string;
  label: string;
}

export function keywordChoicesForField(
  keywords: KeywordSummary[],
  fieldName: string,
): KeywordOption[] {
  const want = fieldName.trim().toLowerCase();
  const match = keywords.find((k) => {
    const value = (k.value ?? "").trim().toLowerCase();
    const label = (k.label ?? "").trim().toLowerCase();
    return value === want || label === want;
  });
  const source: KeywordChoiceSummary[] = match?.choices?.length
    ? match.choices
    : keywords.flatMap((k) => k.choices ?? []);
  const seen = new Set<string>();
  const options: KeywordOption[] = [];
  for (const choice of source) {
    const value = String(choice.value ?? "").trim();
    if (!value || seen.has(value)) {
      continue;
    }
    seen.add(value);
    options.push({ value, label: String(choice.label ?? value) });
  }
  return options;
}

export interface KeywordFieldWidgetProps {
  name: string;
  value: string;
  readOnly: boolean;
  onChange: (value: string) => void;
  loadKeywords?: () => Promise<KeywordSummary[]>;
}

export function KeywordFieldWidget({
  name,
  value,
  readOnly,
  onChange,
  loadKeywords = () => listKeywords(true),
}: KeywordFieldWidgetProps): React.ReactElement {
  const [keywords, setKeywords] = useState<KeywordSummary[]>([]);

  useEffect(() => {
    let cancelled = false;
    void loadKeywords()
      .then((rows) => {
        if (!cancelled) {
          setKeywords(rows);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setKeywords([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [loadKeywords]);

  const options = useMemo(
    () => keywordChoicesForField(keywords, name),
    [keywords, name],
  );

  return (
    <select
      className={`${styles.input} ${readOnly ? styles.readonly : ""}`}
      data-testid={`editor-field-${name}`}
      data-editor-kind="keyword"
      name={name}
      value={value}
      disabled={readOnly}
      onChange={(e) => onChange(e.target.value)}
    >
      <option value="">{message(EDITOR_MSG.KEYWORD_EMPTY)}</option>
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </select>
  );
}
