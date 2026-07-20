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

/**
 * Search panel for the modern Content Explorer (US5 / T068).
 *
 * <p>Renders a server-backed search input + results list. The
 * {@link SearchPanelProps.onOpen} callback receives the selected result
 * for the host (typically the explorer shell) to navigate to the item;
 * the {@link SearchPanelProps.onReveal} callback asks the host to
 * select the parent folder in the tree. Results come from the
 * sitemanage extended-search REST endpoint via the typed
 * {@link searchExtended} client; the panel defers all transport so
 * the host can stub it in tests.</p>
 *
 * <p>State machine:</p>
 * <ul>
 *   <li>{@code idle} — initial; submit the form to search.</li>
 *   <li>{@code loading} — fetch in flight.</li>
 *   <li>{@code ready} — has results (possibly empty).</li>
 *   <li>{@code error} — fetch rejected / server error; the
 *       {@code SearchStatusView} renders a Retry button that re-issues
 *       the last query. The handler is wired by the parent
 *       {@link SearchPanel} (which owns the transport); see the
 *       {@code onRetry} prop description in the local
 *       {@code SearchStatusView} function component below.</li>
 * </ul>
 */

import React, { useEffect, useState } from "react";
import {
  searchExtended,
  type PSItemProperties,
  type PSSearchCriteria,
  type PSSearchResults,
} from "../api/contentExplorer/searchApi";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

export interface SearchPanelProps {
  /** Optional initial query (e.g. URL hash / deep-link). */
  initialQuery?: string;
  /** Optional initial criteria (folder scope + sortColumn + maxResults). */
  initialCriteria?: PSSearchCriteria;
  /** Override for the search transport (default: {@link searchExtended}). */
  search?: (criteria: PSSearchCriteria) => Promise<PSSearchResults>;
  /** Triggered when the user clicks "Open" on a result. */
  onOpen?: (result: PSItemProperties) => void;
  /** Triggered when the user clicks "Reveal in folder" on a result. */
  onReveal?: (result: PSItemProperties) => void;
  /** ARIA label override; defaults to "Search". */
  ariaLabel?: string;
  className?: string;
}

type Status =
  | { kind: "idle"; query: string }
  | { kind: "loading"; query: string }
  | { kind: "ready"; query: string; results: PSSearchResults }
  | { kind: "error"; query: string; message: string };

export function SearchPanel(props: SearchPanelProps): React.JSX.Element {
  const {
    initialQuery = "",
    initialCriteria = {},
    search = searchExtended,
    onOpen,
    onReveal,
    ariaLabel,
    className,
  } = props;
  const [status, setStatus] = useState<Status>({
    kind: "idle",
    query: initialQuery,
  });
  const [draft, setDraft] = useState(initialQuery);

  useEffect(() => {
    setDraft(initialQuery);
    if (initialQuery) {
      void runSearch(initialQuery);
    }
    // We intentionally exclude `runSearch` from deps — it captures
    // initialCriteria once via the closure below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialQuery]);

  async function runSearch(query: string): Promise<void> {
    const trimmed = query.trim();
    if (trimmed.length === 0) {
      setStatus({ kind: "idle", query: "" });
      return;
    }
    setStatus({ kind: "loading", query: trimmed });
    try {
      const results = await search({
        ...initialCriteria,
        query: trimmed,
        startIndex: initialCriteria.startIndex ?? 0,
        maxResults: initialCriteria.maxResults ?? 25,
      });
      setStatus({ kind: "ready", query: trimmed, results });
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : String(err ?? "unknown");
      setStatus({ kind: "error", query: trimmed, message: msg });
    }
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>): void {
    e.preventDefault();
    void runSearch(draft);
  }

  return (
    <section
      role="search"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.SEARCH_TITLE)}
      className={className}
      data-testid="search-panel"
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <form onSubmit={handleSubmit} style={{ display: "flex", gap: 8 }}>
        <label htmlFor="search-panel-input" style={{ display: "none" }}>
          {message(EXPLORER_MSG.SEARCH_TITLE)}
        </label>
        <input
          id="search-panel-input"
          type="search"
          data-testid="search-panel-input"
          placeholder={message(EXPLORER_MSG.SEARCH_PLACEHOLDER)}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          style={{ flex: 1, padding: "4px 8px" }}
        />
        <button
          type="submit"
          data-testid="search-panel-submit"
          disabled={status.kind === "loading" || draft.trim().length === 0}
        >
          {message(EXPLORER_MSG.SEARCH_SUBMIT)}
        </button>
      </form>
      <SearchStatusView
        status={status}
        onOpen={onOpen}
        onReveal={onReveal}
        onRetry={() => void runSearch(status.kind === "error" ? status.query : draft.trim())}
      />
    </section>
  );
}

function SearchStatusView(props: {
  status: Status;
  onOpen?: (r: PSItemProperties) => void;
  onReveal?: (r: PSItemProperties) => void;
  /** Re-issues the failed search. Optional to keep the helper pure; the
   * parent {@link SearchPanel} wires its own {@code runSearch} closure
   * here so the parent stays the single owner of the transport.
   *
   * <p>(Plain-text reference; {@code SearchStatusView.onRetry} is a
   * destructured prop parameter rather than a property of the function,
   * so it cannot be the target of a {@code {@link ...}} tag.)</p>
   */
  onRetry?: () => void;
}): React.JSX.Element {
  const { status, onOpen, onReveal, onRetry } = props;
  if (status.kind === "loading") {
    return (
      <p
        role="status"
        aria-live="polite"
        data-testid="search-panel-loading"
        style={{ marginTop: 8 }}
      >
        {message(EXPLORER_MSG.SEARCH_LOADING)}
      </p>
    );
  }
  if (status.kind === "error") {
    return (
      <div role="alert" style={{ color: "#a00", marginTop: 8 }}>
        <p data-testid="search-panel-error">
          {message(EXPLORER_MSG.SEARCH_ERROR)}: {status.message}
        </p>
        <button
          type="button"
          data-testid="search-panel-retry"
          disabled={!onRetry}
          onClick={() => onRetry?.()}
        >
          {message(EXPLORER_MSG.RETRY)}
        </button>
      </div>
    );
  }
  if (status.kind === "ready") {
    if (status.results.children.length === 0) {
      return (
        <p
          role="status"
          aria-live="polite"
          data-testid="search-panel-empty"
          style={{ marginTop: 8, color: "#888" }}
        >
          {message(EXPLORER_MSG.SEARCH_EMPTY)}
        </p>
      );
    }
    return (
      <ul
        data-testid="search-panel-results"
        style={{ listStyle: "none", padding: 0, margin: "8px 0 0 0" }}
        aria-label={`Search results for "${status.query}"`}
      >
        {status.results.children.map((r, idx) => (
          <li
            key={`${r.id ?? r.title ?? "row"}-${idx}`}
            data-testid="search-panel-result-row"
            style={{
              display: "flex",
              gap: 8,
              alignItems: "center",
              borderBottom: "1px solid #eee",
              padding: "4px 0",
            }}
          >
            <span style={{ flex: 1 }}>
              <strong>{r.title ?? r.name ?? r.id}</strong>
              <small style={{ marginLeft: 6, color: "#888" }}>
                {r.folderPath ?? r.type}
              </small>
            </span>
            <button
              type="button"
              data-testid={`search-panel-open-${r.id ?? idx}`}
              onClick={() => onOpen?.(r)}
              disabled={!onOpen}
            >
              {message(EXPLORER_MSG.SEARCH_OPEN)}
            </button>
            <button
              type="button"
              data-testid={`search-panel-reveal-${r.id ?? idx}`}
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
  return <></>;
}
