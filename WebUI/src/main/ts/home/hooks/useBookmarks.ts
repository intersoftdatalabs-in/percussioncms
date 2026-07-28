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

import { useCallback, useEffect, useState } from "react";
import { isSessionRedirectError } from "../../api/client";
import {
  addToMyPages,
  contentItemId,
  fetchMyContent,
  formatApiError,
  removeFromMyPages,
} from "../../api/home/homeApi";
import type { ContentListItem } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";

export interface UseBookmarksResult {
  /** Loaded favorite page ids (string guids). */
  bookmarkIds: ReadonlySet<string>;
  /** True while the initial mycontent fetch is in flight. */
  loading: boolean;
  /** Last action/load error, if any. */
  error: string | null;
  /** Clear action error (e.g. after user dismiss). */
  clearError: () => void;
  /** Reload favorites from the server. */
  refresh: () => Promise<void>;
  isBookmarked: (item: ContentListItem | string | null | undefined) => boolean;
  /** Ids currently mid add/remove. */
  pendingIds: ReadonlySet<string>;
  addBookmark: (item: ContentListItem | string) => Promise<boolean>;
  removeBookmark: (item: ContentListItem | string) => Promise<boolean>;
  toggleBookmark: (item: ContentListItem | string) => Promise<boolean>;
}

function resolveId(item: ContentListItem | string | null | undefined): string | null {
  if (item == null) {
    return null;
  }
  if (typeof item === "string") {
    const t = item.trim();
    return t || null;
  }
  return contentItemId(item);
}

/**
 * Shared My Pages / Bookmarks membership for Home sections.
 *
 * Loads {@code item/mycontent} once, then optimistically updates on add/remove
 * so Recent / Search / Library can toggle stars without N+1 isMyPage calls.
 */
export function useBookmarks(enabled = true): UseBookmarksResult {
  const [bookmarkIds, setBookmarkIds] = useState<Set<string>>(() => new Set());
  const [pendingIds, setPendingIds] = useState<Set<string>>(() => new Set());
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!enabled) {
      return;
    }
    setLoading(true);
    try {
      const list = await fetchMyContent();
      const next = new Set<string>();
      for (const row of list) {
        const id = contentItemId(row);
        if (id) {
          next.add(id);
        }
      }
      setBookmarkIds(next);
      setError(null);
    } catch (err: unknown) {
      if (!isSessionRedirectError(err)) {
        setError(formatApiError(err, message(MSG.ERROR_GENERIC)));
      }
    } finally {
      setLoading(false);
    }
  }, [enabled]);

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    fetchMyContent()
      .then((list) => {
        if (cancelled) {
          return;
        }
        const next = new Set<string>();
        for (const row of list) {
          const id = contentItemId(row);
          if (id) {
            next.add(id);
          }
        }
        setBookmarkIds(next);
        setError(null);
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
  }, [enabled]);

  const clearError = useCallback(() => setError(null), []);

  const isBookmarked = useCallback(
    (item: ContentListItem | string | null | undefined) => {
      const id = resolveId(item);
      return id != null && bookmarkIds.has(id);
    },
    [bookmarkIds],
  );

  const withPending = useCallback(async (id: string, work: () => Promise<void>) => {
    setPendingIds((prev) => new Set(prev).add(id));
    try {
      await work();
      setError(null);
      return true;
    } catch (err: unknown) {
      if (!isSessionRedirectError(err)) {
        setError(formatApiError(err, message(MSG.ERROR_GENERIC)));
      }
      return false;
    } finally {
      setPendingIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }
  }, []);

  const addBookmark = useCallback(
    async (item: ContentListItem | string) => {
      const id = resolveId(item);
      if (!id) {
        setError(message(MSG.BOOKMARK_NEEDS_ID));
        return false;
      }
      return withPending(id, async () => {
        await addToMyPages(id);
        setBookmarkIds((prev) => new Set(prev).add(id));
      });
    },
    [withPending],
  );

  const removeBookmark = useCallback(
    async (item: ContentListItem | string) => {
      const id = resolveId(item);
      if (!id) {
        setError(message(MSG.BOOKMARK_NEEDS_ID));
        return false;
      }
      return withPending(id, async () => {
        await removeFromMyPages(id);
        setBookmarkIds((prev) => {
          const next = new Set(prev);
          next.delete(id);
          return next;
        });
      });
    },
    [withPending],
  );

  const toggleBookmark = useCallback(
    async (item: ContentListItem | string) => {
      const id = resolveId(item);
      if (!id) {
        setError(message(MSG.BOOKMARK_NEEDS_ID));
        return false;
      }
      if (bookmarkIds.has(id)) {
        return removeBookmark(id);
      }
      return addBookmark(id);
    },
    [addBookmark, bookmarkIds, removeBookmark],
  );

  return {
    bookmarkIds,
    loading,
    error,
    clearError,
    refresh,
    isBookmarked,
    pendingIds,
    addBookmark,
    removeBookmark,
    toggleBookmark,
  };
}
