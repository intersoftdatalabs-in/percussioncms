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

/** Page size sent as execute {@code maxResults} (#4930). */
export const VIEW_RESULTS_PAGE_SIZE = 50;

export type ViewRunStatus =
  | { kind: "loading"; label: string }
  | {
      kind: "ready";
      label: string;
      results: ViewExecuteResult;
      /** 1-based execute startIndex for this page. Defaults to results.startIndex or 1. */
      startIndex?: number;
    }
  | {
      kind: "error";
      label: string;
      message: string;
      httpStatus?: number;
      /** Page that stayed visible when a later execute failed. */
      startIndex?: number;
      retained?: ViewExecuteResult;
    };

export interface ViewResultsPanelProps {
  status: ViewRunStatus;
  onOpen?: (result: PSItemProperties) => void;
  onReveal?: (result: PSItemProperties) => void;
  onRetry?: () => void;
  onNextPage?: () => void;
  onPreviousPage?: () => void;
}

export function viewResultsStartIndex(
  status: ViewRunStatus,
  results?: ViewExecuteResult,
): number {
  if (status.kind === "ready" || status.kind === "error") {
    if (typeof status.startIndex === "number" && status.startIndex >= 1) {
      return status.startIndex;
    }
  }
  const fromPayload = results?.startIndex;
  if (typeof fromPayload === "number" && fromPayload >= 1) {
    return fromPayload;
  }
  return 1;
}

/**
 * Next is offered when this page is full or {@code totalCount} says more rows remain.
 * An empty page is the end (not an error), so Next is hidden.
 */
export function viewResultsHasNextPage(
  startIndex: number,
  results: ViewExecuteResult | null | undefined,
  pageSize: number = VIEW_RESULTS_PAGE_SIZE,
): boolean {
  const len = Array.isArray(results?.children) ? results.children.length : 0;
  if (len === 0) {
    return false;
  }
  const total = results?.totalCount;
  if (typeof total === "number") {
    return startIndex + len - 1 < total;
  }
  return len >= pageSize;
}

export function viewResultsHasPreviousPage(startIndex: number): boolean {
  return startIndex > 1;
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
  onNextPage,
  onPreviousPage,
}: ViewResultsPanelProps): React.ReactElement {
  const shown =
    status.kind === "ready"
      ? status.results
      : status.kind === "error"
        ? status.retained
        : undefined;
  const startIndex = viewResultsStartIndex(status, shown);
  return (
    <section
      style={listStyle}
      data-testid="explorer-view-results"
      data-start-index={status.kind === "loading" ? undefined : String(startIndex)}
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
      {status.kind !== "loading" &&
      shown &&
      toViewResultRows(shown).length > 0 ? (
        <ViewResultsPager
          startIndex={startIndex}
          results={shown}
          onNextPage={onNextPage}
          onPreviousPage={onPreviousPage}
        />
      ) : null}
      {status.kind === "ready" && toViewResultRows(status.results).length === 0 ? (
        <ViewResultsPager
          startIndex={startIndex}
          results={status.results}
          onNextPage={onNextPage}
          onPreviousPage={onPreviousPage}
        />
      ) : null}
    </section>
  );
}

function ViewResultsPager(props: {
  startIndex: number;
  results: ViewExecuteResult;
  onNextPage?: () => void;
  onPreviousPage?: () => void;
}): React.ReactElement | null {
  const showPrev =
    props.onPreviousPage != null && viewResultsHasPreviousPage(props.startIndex);
  const showNext =
    props.onNextPage != null &&
    viewResultsHasNextPage(props.startIndex, props.results);
  if (!showPrev && !showNext) {
    return null;
  }
  return (
    <nav
      data-testid="explorer-view-results-pager"
      aria-label={message(EXPLORER_MSG.VIEWS_PAGE_REGION)}
      style={{ display: "flex", gap: 8, padding: "8px 12px" }}
    >
      {showPrev ? (
        <button
          type="button"
          data-testid="explorer-view-results-previous"
          onClick={() => props.onPreviousPage?.()}
        >
          {message(EXPLORER_MSG.VIEWS_PAGE_PREVIOUS)}
        </button>
      ) : null}
      {showNext ? (
        <button
          type="button"
          data-testid="explorer-view-results-next"
          onClick={() => props.onNextPage?.()}
        >
          {message(EXPLORER_MSG.VIEWS_PAGE_NEXT)}
        </button>
      ) : null}
    </nav>
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
    const retained = toViewResultRows(status.retained);
    return (
      <div>
        <div role="alert" style={{ color: "#a00", padding: 12 }}>
          <p
            data-testid="explorer-view-results-error"
            data-http-status={
              status.httpStatus != null ? String(status.httpStatus) : undefined
            }
            style={{ margin: "0 0 8px 0" }}
          >
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
        {retained.length > 0 ? (
          <ViewResultList
            label={status.label}
            rows={retained}
            onOpen={onOpen}
            onReveal={onReveal}
          />
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
    <ViewResultList
      label={status.label}
      rows={rows}
      onOpen={onOpen}
      onReveal={onReveal}
    />
  );
}

function ViewResultList(props: {
  label: string;
  rows: PSItemProperties[];
  onOpen?: (result: PSItemProperties) => void;
  onReveal?: (result: PSItemProperties) => void;
}): React.ReactElement {
  const { label, rows, onOpen, onReveal } = props;
  return (
    <ul
      data-testid="explorer-view-results-list"
      style={{ listStyle: "none", padding: "8px 12px", margin: 0 }}
      aria-label={label}
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
