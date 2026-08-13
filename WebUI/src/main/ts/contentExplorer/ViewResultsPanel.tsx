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

/**
 * View execute results with Open / Reveal (saved-search peer UX) (#3116).
 */

import React from "react";
import type { PSItemProperties } from "../api/contentExplorer/types";
import type { ViewExecuteResult } from "../api/developer/types";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import { listStyle } from "./styles";

export type ViewRunStatus =
  | { kind: "loading"; label: string }
  | { kind: "ready"; label: string; results: ViewExecuteResult }
  | { kind: "error"; label: string; message: string };

export interface ViewResultsPanelProps {
  status: ViewRunStatus;
  onOpen?: (result: PSItemProperties) => void;
  onReveal?: (result: PSItemProperties) => void;
  onRetry?: () => void;
}

export function toViewResultRows(
  payload: ViewExecuteResult | null | undefined,
): PSItemProperties[] {
  if (payload == null || !Array.isArray(payload.children)) {
    return [];
  }
  return payload.children.map((row) => ({
    id: row.id,
    name: row.name,
    title: row.title,
    folderPath: row.folderPath,
    type: row.type,
  }));
}

export function ViewResultsPanel({
  status,
  onOpen,
  onReveal,
  onRetry,
}: ViewResultsPanelProps): React.ReactElement {
  return (
    <section
      style={listStyle}
      data-testid="explorer-view-results"
      aria-label={message(EXPLORER_MSG.VIEWS_RESULTS_REGION)}
    >
      <header
        style={{
          padding: "8px 12px",
          borderBottom: "1px solid #eee",
          fontSize: 13,
          fontWeight: 600,
        }}
        data-testid="explorer-view-results-heading"
        data-mkd-lang-ignore="1"
      >
        {status.label}
      </header>
      <ViewRunBody
        status={status}
        onOpen={onOpen}
        onReveal={onReveal}
        onRetry={onRetry}
      />
    </section>
  );
}

function ViewRunBody(props: {
  status: ViewRunStatus;
  onOpen?: (result: PSItemProperties) => void;
  onReveal?: (result: PSItemProperties) => void;
  onRetry?: () => void;
}): React.ReactElement {
  const { status, onOpen, onReveal, onRetry } = props;
  if (status.kind === "loading") {
    return (
      <p
        role="status"
        aria-live="polite"
        data-testid="explorer-view-results-loading"
        style={{ padding: 12 }}
      >
        {message(EXPLORER_MSG.VIEWS_RUN_LOADING)}
      </p>
    );
  }
  if (status.kind === "error") {
    return (
      <div role="alert" style={{ color: "#a00", padding: 12 }}>
        <p data-testid="explorer-view-results-error" style={{ margin: "0 0 8px 0" }}>
          {message(EXPLORER_MSG.VIEWS_RUN_ERROR)}: {status.message}
        </p>
        {onRetry ? (
          <button
            type="button"
            data-testid="explorer-view-results-retry"
            onClick={() => onRetry()}
          >
            {message(EXPLORER_MSG.RETRY)}
          </button>
        ) : null}
      </div>
    );
  }
  const rows = toViewResultRows(status.results);
  if (rows.length === 0) {
    return (
      <p
        role="status"
        aria-live="polite"
        data-testid="explorer-view-results-empty"
        style={{ padding: 12, color: "#444" }}
      >
        {message(EXPLORER_MSG.VIEWS_RUN_EMPTY)}
      </p>
    );
  }
  return (
    <ul
      data-testid="explorer-view-results-list"
      style={{ listStyle: "none", padding: "8px 12px", margin: 0 }}
      aria-label={status.label}
    >
      {rows.map((r, idx) => (
        <li
          key={`${r.id ?? r.title ?? "row"}-${idx}`}
          data-testid="explorer-view-result-row"
          style={{
            display: "flex",
            gap: 8,
            alignItems: "center",
            borderBottom: "1px solid #eee",
            padding: "4px 0",
          }}
        >
          <span style={{ flex: 1 }} data-mkd-lang-ignore="1">
            <strong>{r.title ?? r.name ?? r.id}</strong>
            <small style={{ marginLeft: 6, color: "#888" }}>
              {r.folderPath ?? r.type}
            </small>
          </span>
          <button
            type="button"
            data-testid={`explorer-view-open-${r.id ?? idx}`}
            onClick={() => onOpen?.(r)}
            disabled={!onOpen}
          >
            {message(EXPLORER_MSG.SEARCH_OPEN)}
          </button>
          <button
            type="button"
            data-testid={`explorer-view-reveal-${r.id ?? idx}`}
            onClick={() => onReveal?.(r)}
            disabled={!onReveal}
          >
            {message(EXPLORER_MSG.SEARCH_REVEAL)}
          </button>
        </li>
      ))}
    </ul>
  );
}
