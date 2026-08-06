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

import React, { useState } from "react";
import { isSessionRedirectError } from "../../api/client";
import {
  contentItemId,
  formatApiError,
  searchContent,
} from "../../api/home/homeApi";
import type { ContentListItem } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import { ContentListRow } from "../components/ContentListRow";
import { useBookmarks } from "../hooks/useBookmarks";
import {
  actionButtonStyle,
  emptyStateStyle,
  errorStyle,
  formRowStyle,
  listStyle,
  searchFormActionsStyle,
  searchInputStyle,
  sectionHintStyle,
} from "../home.styles";

export interface SearchSectionProps {
  onOpenItem?: (item: ContentListItem) => void;
}

/** Format result count for status line (TMX key falls back after @). */
export function formatSearchResultCount(count: number): string {
  if (count === 1) {
    return "1 result";
  }
  return `${count} results`;
}

export function SearchSection({
  onOpenItem,
}: SearchSectionProps): React.ReactElement {
  const [query, setQuery] = useState("");
  const [items, setItems] = useState<ContentListItem[] | null>(null);
  const [lastQuery, setLastQuery] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const bookmarks = useBookmarks(true);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const q = query.trim();
    if (!q) {
      return;
    }
    setLoading(true);
    setError(null);
    // Wire contract: SearchCriteria.query + maxResults (see searchExtended / US5).
    searchContent({ query: q, maxResults: 50, startIndex: 1 })
      .then((list) => {
        setItems(list);
        setLastQuery(q);
      })
      .catch((err: unknown) => {
        if (isSessionRedirectError(err)) {
          return;
        }
        setError(formatApiError(err, message(MSG.ERROR_GENERIC)));
      })
      .finally(() => setLoading(false));
  };

  return (
    <div data-testid="home-search-section">
      <p style={sectionHintStyle}>{message(MSG.SEARCH_HINT)}</p>
      <form onSubmit={onSubmit}>
        <div style={formRowStyle}>
          <label htmlFor="home-search-query">
            {message(MSG.SECTION_SEARCH)}
          </label>
          <input
            id="home-search-query"
            data-testid="home-search-input"
            type="search"
            value={query}
            onChange={(ev) => setQuery(ev.target.value)}
            placeholder={message(MSG.SEARCH_PLACEHOLDER)}
            style={searchInputStyle}
            autoComplete="off"
          />
        </div>
        <div style={searchFormActionsStyle}>
          <button
            type="submit"
            data-testid="home-search-submit"
            style={actionButtonStyle("primary")}
            disabled={loading || !query.trim()}
          >
            {loading ? message(MSG.LOADING) : message(MSG.SEARCH_SUBMIT)}
          </button>
        </div>
      </form>
      {error && (
        <p role="alert" style={errorStyle} data-testid="home-search-error">
          {error}
        </p>
      )}
      {bookmarks.error && (
        <p role="alert" style={errorStyle} data-testid="home-search-bookmark-error">
          {bookmarks.error}
        </p>
      )}
      {items && items.length === 0 && (
        <p style={emptyStateStyle} data-testid="home-search-empty">
          {message(MSG.SEARCH_EMPTY)}
          {lastQuery ? (
            <span data-testid="home-search-empty-query">
              {" "}
              ({lastQuery})
            </span>
          ) : null}
        </p>
      )}
      {items && items.length > 0 && (
        <>
          <p
            role="status"
            style={{ margin: "0 0 10px", color: "#444", fontSize: "0.9rem" }}
            data-testid="home-search-count"
          >
            {formatSearchResultCount(items.length)}
            {lastQuery ? ` · “${lastQuery}”` : ""}
          </p>
          <ul
            style={listStyle}
            aria-label={message(MSG.SECTION_SEARCH)}
            data-testid="home-search-results"
          >
            {items.map((item, index) => {
              const id = contentItemId(item);
              return (
                <ContentListRow
                  key={String(id ?? item.path ?? index)}
                  item={item}
                  index={index}
                  fallbackLabel={`hit-${index}`}
                  onOpenItem={onOpenItem}
                  bookmarkMode="toggle"
                  isBookmarked={bookmarks.isBookmarked(item)}
                  bookmarkPending={id != null && bookmarks.pendingIds.has(id)}
                  onBookmark={(row) => {
                    void bookmarks.toggleBookmark(row);
                  }}
                  testIdPrefix="home-search"
                />
              );
            })}
          </ul>
        </>
      )}
    </div>
  );
}
