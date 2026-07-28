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

import React, { useEffect, useState } from "react";
import { isSessionRedirectError } from "../../api/client";
import { fetchMyContent, formatApiError } from "../../api/home/homeApi";
import type { ContentListItem } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import { errorStyle, listItemStyle, listStyle } from "../home.styles";

export interface BookmarksSectionProps {
  onOpenItem?: (item: ContentListItem) => void;
}

/**
 * Classic CUI "My Bookmarks" — bookmarked content via item/mycontent.
 */
export function BookmarksSection({
  onOpenItem,
}: BookmarksSectionProps): React.ReactElement {
  const [items, setItems] = useState<ContentListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchMyContent()
      .then((list) => {
        if (!cancelled) {
          setItems(list);
          setError(null);
        }
      })
      .catch((err: unknown) => {
        if (cancelled || isSessionRedirectError(err)) {
          return;
        }
        setError(formatApiError(err, message(MSG.ERROR_GENERIC)));
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <p role="status" data-testid="home-bookmarks-loading">
        {message(MSG.LOADING)}
      </p>
    );
  }
  if (error) {
    return (
      <p role="alert" style={errorStyle} data-testid="home-bookmarks-error">
        {error}
      </p>
    );
  }
  if (items.length === 0) {
    return (
      <p data-testid="home-bookmarks-empty">{message(MSG.BOOKMARKS_EMPTY)}</p>
    );
  }

  return (
    <ul
      style={listStyle}
      aria-label={message(MSG.SECTION_BOOKMARKS)}
      data-testid="home-bookmarks-list"
    >
      {items.map((item, index) => {
        const label =
          item.name ||
          item.title ||
          item.path ||
          item.id ||
          `bookmark-${index}`;
        return (
          <li key={String(item.id ?? item.path ?? index)} style={listItemStyle}>
            <span>{String(label)}</span>
            <button type="button" onClick={() => onOpenItem?.(item)}>
              {message(MSG.OPEN_ITEM)}
            </button>
          </li>
        );
      })}
    </ul>
  );
}
