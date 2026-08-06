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
import {
  contentItemId,
  fetchRecentItems,
  formatApiError,
} from "../../api/home/homeApi";
import type { ContentListItem } from "../../api/home/types";
import { isSessionRedirectError } from "../../api/client";
import { message, MSG } from "../../i18n/message";
import { ContentListRow } from "../components/ContentListRow";
import { useBookmarks } from "../hooks/useBookmarks";
import {
  emptyStateStyle,
  errorStyle,
  listStyle,
  sectionHintStyle,
} from "../home.styles";

export interface RecentSectionProps {
  onOpenItem?: (item: ContentListItem) => void;
}

export function RecentSection({
  onOpenItem,
}: RecentSectionProps): React.ReactElement {
  const [items, setItems] = useState<ContentListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const bookmarks = useBookmarks(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchRecentItems("item")
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
      <p role="status" data-testid="home-recent-loading">
        {message(MSG.LOADING)}
      </p>
    );
  }
  if (error) {
    return (
      <p role="alert" style={errorStyle} data-testid="home-recent-error">
        {error}
      </p>
    );
  }
  if (items.length === 0) {
    return (
      <div data-testid="home-recent-empty-block">
        <p style={sectionHintStyle}>{message(MSG.RECENT_HINT)}</p>
        <p style={emptyStateStyle} data-testid="home-recent-empty">
          {message(MSG.RECENT_EMPTY)}
        </p>
      </div>
    );
  }

  return (
    <div data-testid="home-recent-section">
      <p style={sectionHintStyle}>{message(MSG.RECENT_HINT)}</p>
      {bookmarks.error && (
        <p role="alert" style={errorStyle} data-testid="home-recent-bookmark-error">
          {bookmarks.error}
        </p>
      )}
      <ul
        style={listStyle}
        aria-label={message(MSG.SECTION_RECENT)}
        data-testid="home-recent-list"
      >
        {items.map((item, index) => {
          const id = contentItemId(item);
          return (
            <ContentListRow
              key={String(id ?? item.path ?? index)}
              item={item}
              index={index}
              onOpenItem={onOpenItem}
              bookmarkMode="toggle"
              isBookmarked={bookmarks.isBookmarked(item)}
              bookmarkPending={id != null && bookmarks.pendingIds.has(id)}
              onBookmark={(row) => {
                void bookmarks.toggleBookmark(row);
              }}
              testIdPrefix="home-recent"
            />
          );
        })}
      </ul>
    </div>
  );
}
