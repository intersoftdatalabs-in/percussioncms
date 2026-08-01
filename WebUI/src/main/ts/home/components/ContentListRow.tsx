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

import React from "react";
import type { ContentListItem } from "../../api/home/types";
import { isBookmarkableItem } from "../../api/home/homeApi";
import { MKD_LANG_IGNORE_ATTR } from "../../i18n/mkdLangIgnore";
import { message, MSG } from "../../i18n/message";
import {
  actionButtonStyle,
  itemActionsStyle,
  itemLabelStyle,
  itemMetaStyle,
  itemPrimaryStyle,
  listItemStyle,
} from "../home.styles";

export type BookmarkMode = "add" | "remove" | "toggle" | "none";

export interface ContentListRowProps {
  item: ContentListItem;
  index?: number;
  /** Fallback label when name/title/path/id are empty */
  fallbackLabel?: string;
  onOpenItem?: (item: ContentListItem) => void;
  /** Bookmark control mode. Default none. */
  bookmarkMode?: BookmarkMode;
  isBookmarked?: boolean;
  bookmarkPending?: boolean;
  onBookmark?: (item: ContentListItem) => void;
  /** Extra action slot (e.g. open folder) replaces default Open when provided */
  primaryAction?: React.ReactNode;
  testIdPrefix?: string;
}

function itemLabel(item: ContentListItem, index: number, fallback?: string): string {
  return (
    item.name ||
    item.title ||
    item.path ||
    item.id ||
    fallback ||
    `item-${index}`
  );
}

/**
 * Shared Home list row: primary label, optional path meta, Open + bookmark actions.
 */
export function ContentListRow({
  item,
  index = 0,
  fallbackLabel,
  onOpenItem,
  bookmarkMode = "none",
  isBookmarked = false,
  bookmarkPending = false,
  onBookmark,
  primaryAction,
  testIdPrefix = "home-item",
}: ContentListRowProps): React.ReactElement {
  const label = String(itemLabel(item, index, fallbackLabel));
  const path =
    item.path != null && String(item.path).trim()
      ? String(item.path).trim()
      : "";
  const status =
    item.status != null && String(item.status).trim()
      ? String(item.status).trim()
      : "";
  const metaParts = [path, status].filter(Boolean);
  const canBookmark =
    bookmarkMode !== "none" && isBookmarkableItem(item) && onBookmark != null;

  let bookmarkLabel = message(MSG.BOOKMARK_ADD);
  if (bookmarkMode === "remove" || (bookmarkMode === "toggle" && isBookmarked)) {
    bookmarkLabel = message(MSG.BOOKMARK_REMOVE);
  }

  const bookmarkTitle =
    bookmarkMode === "remove" || (bookmarkMode === "toggle" && isBookmarked)
      ? message(MSG.BOOKMARK_REMOVE)
      : message(MSG.BOOKMARK_ADD);

  return (
    <li
      style={listItemStyle}
      data-testid={`${testIdPrefix}-row`}
      data-item-id={item.id ?? undefined}
      data-bookmarked={isBookmarked ? "true" : "false"}
    >
      <div
        style={itemLabelStyle}
        {...{ [MKD_LANG_IGNORE_ATTR]: "1" as const }}
      >
        <span style={itemPrimaryStyle}>{label}</span>
        {metaParts.length > 0 && (
          <span style={itemMetaStyle} data-testid={`${testIdPrefix}-meta`}>
            {metaParts.join(" · ")}
          </span>
        )}
      </div>
      <div style={itemActionsStyle}>
        {canBookmark && (
          <button
            type="button"
            style={actionButtonStyle(
              bookmarkMode === "remove" || isBookmarked ? "danger" : "secondary",
            )}
            data-testid={`${testIdPrefix}-bookmark`}
            aria-label={bookmarkTitle}
            title={bookmarkTitle}
            aria-pressed={
              bookmarkMode === "toggle" ? isBookmarked : undefined
            }
            disabled={bookmarkPending}
            onClick={() => onBookmark?.(item)}
          >
            {bookmarkPending ? message(MSG.LOADING) : bookmarkLabel}
          </button>
        )}
        {primaryAction ?? (
          <button
            type="button"
            style={actionButtonStyle("primary")}
            data-testid={`${testIdPrefix}-open`}
            onClick={() => onOpenItem?.(item)}
          >
            {message(MSG.OPEN_ITEM)}
          </button>
        )}
      </div>
    </li>
  );
}
