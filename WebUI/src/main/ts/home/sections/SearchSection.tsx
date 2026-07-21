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

import React, { useState } from "react";
import { searchContent } from "../../api/home/homeApi";
import type { ContentListItem } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import {
  errorStyle,
  formRowStyle,
  listItemStyle,
  listStyle,
} from "../home.styles";

export interface SearchSectionProps {
  onOpenItem?: (item: ContentListItem) => void;
}

export function SearchSection({
  onOpenItem,
}: SearchSectionProps): React.ReactElement {
  const [query, setQuery] = useState("");
  const [items, setItems] = useState<ContentListItem[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    searchContent({ query, searchText: query, maxResults: 50 })
      .then((list) => setItems(list))
      .catch(() => setError(message(MSG.ERROR_GENERIC)))
      .finally(() => setLoading(false));
  };

  return (
    <div>
      <form onSubmit={onSubmit}>
        <div style={formRowStyle}>
          <label htmlFor="home-search-query">
            {message(MSG.SECTION_SEARCH)}
          </label>
          <input
            id="home-search-query"
            type="search"
            value={query}
            onChange={(ev) => setQuery(ev.target.value)}
            placeholder={message(MSG.SEARCH_PLACEHOLDER)}
          />
        </div>
        <button type="submit" disabled={loading || !query.trim()}>
          {message(MSG.SEARCH_SUBMIT)}
        </button>
      </form>
      {loading && <p role="status">{message(MSG.LOADING)}</p>}
      {error && (
        <p role="alert" style={errorStyle}>
          {error}
        </p>
      )}
      {items && items.length === 0 && <p>{message(MSG.SEARCH_EMPTY)}</p>}
      {items && items.length > 0 && (
        <ul style={listStyle} aria-label={message(MSG.SECTION_SEARCH)}>
          {items.map((item, index) => {
            const label =
              item.name || item.title || item.path || item.id || `hit-${index}`;
            return (
              <li
                key={String(item.id ?? item.path ?? index)}
                style={listItemStyle}
              >
                <span>{label}</span>
                <button type="button" onClick={() => onOpenItem?.(item)}>
                  {message(MSG.OPEN_ITEM)}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
