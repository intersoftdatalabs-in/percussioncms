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

import React, { useCallback, useEffect, useState } from "react";
import { isSessionRedirectError } from "../../api/client";
import {
  contentItemId,
  fetchMyContent,
  formatApiError,
  removeFromMyPages,
} from "../../api/home/homeApi";
import type { ContentListItem } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import { ContentListRow } from "../components/ContentListRow";
import {
  emptyStateStyle,
  errorStyle,
  listStyle,
  sectionHintStyle,
} from "../home.styles";

export interface BookmarksSectionProps {
  onOpenItem?: (item: ContentListItem) => void;
}

/**
 * Classic CUI "My Bookmarks" — bookmarked content via item/mycontent,
 * with remove via removefrommypages.
 */
export function BookmarksSection({
  onOpenItem,
}: BookmarksSectionProps): React.ReactElement {
  const [items, setItems] = useState<ContentListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [pendingIds, setPendingIds] = useState<Set<string>>(() => new Set());

  const load = useCallback(() => {
    setLoading(true);
    return fetchMyContent()
      .then((list) => {
        setItems(list);
        setError(null);
      })
      .catch((err: unknown) => {
        if (isSessionRedirectError(err)) {
          return;
        }
        setError(formatApiError(err, message(MSG.ERROR_GENERIC)));
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

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

  const onRemove = async (item: ContentListItem) => {
    const id = contentItemId(item);
    if (!id) {
      setActionError(message(MSG.BOOKMARK_NEEDS_ID));
      return;
    }
    setPendingIds((prev) => new Set(prev).add(id));
    setActionError(null);
    try {
      await removeFromMyPages(id);
      setItems((prev) => prev.filter((row) => contentItemId(row) !== id));
    } catch (err: unknown) {
      if (!isSessionRedirectError(err)) {
        setActionError(formatApiError(err, message(MSG.ERROR_GENERIC)));
      }
    } finally {
      setPendingIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }
  };

  if (loading) {
    return (
      <p role="status" data-testid="home-bookmarks-loading">
        {message(MSG.LOADING)}
      </p>
    );
  }
  if (error) {
    return (
      <div data-testid="home-bookmarks-error-block">
        <p role="alert" style={errorStyle} data-testid="home-bookmarks-error">
          {error}
        </p>
        <button type="button" onClick={() => void load()}>
          {message(MSG.RETRY)}
        </button>
      </div>
    );
  }
  if (items.length === 0) {
    return (
      <div data-testid="home-bookmarks-empty-block">
        <p style={sectionHintStyle}>{message(MSG.BOOKMARKS_HINT)}</p>
        <p style={emptyStateStyle} data-testid="home-bookmarks-empty">
          {message(MSG.BOOKMARKS_EMPTY)}
        </p>
      </div>
    );
  }

  return (
    <div data-testid="home-bookmarks-section">
      <p style={sectionHintStyle}>{message(MSG.BOOKMARKS_HINT)}</p>
      {actionError && (
        <p role="alert" style={errorStyle} data-testid="home-bookmarks-action-error">
          {actionError}
        </p>
      )}
      <ul
        style={listStyle}
        aria-label={message(MSG.SECTION_BOOKMARKS)}
        data-testid="home-bookmarks-list"
      >
        {items.map((item, index) => {
          const id = contentItemId(item);
          return (
            <ContentListRow
              key={String(id ?? item.path ?? index)}
              item={item}
              index={index}
              fallbackLabel={`bookmark-${index}`}
              onOpenItem={onOpenItem}
              bookmarkMode="remove"
              isBookmarked
              bookmarkPending={id != null && pendingIds.has(id)}
              onBookmark={onRemove}
              testIdPrefix="home-bookmarks"
            />
          );
        })}
      </ul>
    </div>
  );
}
