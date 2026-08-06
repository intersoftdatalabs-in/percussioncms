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

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { isSessionRedirectError } from "../../api/client";
import {
  contentItemId,
  fetchFolderChildren,
  fetchSites,
  formatApiError,
  isBookmarkableItem,
} from "../../api/home/homeApi";
import type { ContentListItem, SiteSummary } from "../../api/home/types";
import {
  cmsPathSegments,
  normalizeCmsPath,
  parentCmsPath,
} from "../create/filenameUtils";
import { message, MSG } from "../../i18n/message";
import { ContentListRow } from "../components/ContentListRow";
import { useBookmarks } from "../hooks/useBookmarks";
import {
  actionButtonStyle,
  emptyStateStyle,
  errorStyle,
  itemActionsStyle,
  listItemStyle,
  listStyle,
  sectionHintStyle,
} from "../home.styles";

export interface LibrarySectionProps {
  isAdmin?: boolean;
  onOpenItem?: (item: ContentListItem) => void;
}

export function LibrarySection({
  isAdmin = false,
  onOpenItem,
}: LibrarySectionProps): React.ReactElement {
  const [sites, setSites] = useState<SiteSummary[]>([]);
  const [path, setPath] = useState<string | null>(null);
  const [children, setChildren] = useState<ContentListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const bookmarks = useBookmarks(true);

  const onApiError = useCallback((err: unknown): void => {
    if (isSessionRedirectError(err)) {
      return;
    }
    setError(formatApiError(err, message(MSG.ERROR_GENERIC)));
  }, []);

  const loadSites = useCallback(() => {
    setLoading(true);
    fetchSites()
      .then((list) => {
        setSites(list);
        setError(null);
      })
      .catch(onApiError)
      .finally(() => setLoading(false));
  }, [onApiError]);

  useEffect(() => {
    loadSites();
  }, [loadSites]);

  const loadFolder = useCallback(
    (folderPath: string) => {
      const normalized = normalizeCmsPath(folderPath);
      setPath(normalized);
      setLoading(true);
      fetchFolderChildren(normalized)
        .then((list) => {
          setChildren(list);
          setError(null);
        })
        .catch(onApiError)
        .finally(() => setLoading(false));
    },
    [onApiError],
  );

  const openSite = (siteName: string) => {
    loadFolder(`/Sites/${siteName}`);
  };

  const openFolder = (folderPath: string) => {
    loadFolder(folderPath);
  };

  const goUp = () => {
    if (!path) {
      return;
    }
    const parent = parentCmsPath(path);
    if (parent == null) {
      setPath(null);
      setChildren([]);
      setError(null);
      return;
    }
    loadFolder(parent);
  };

  const segments = useMemo(
    () => (path ? cmsPathSegments(path) : []),
    [path],
  );

  /** Navigate to breadcrumb index (0 = Sites root → site list). */
  const goToSegment = (index: number) => {
    if (index < 0) {
      setPath(null);
      setChildren([]);
      setError(null);
      return;
    }
    // index 0 is "Sites" — not a browsable folder alone; require site name
    if (index === 0 && segments[0]?.toLowerCase() === "sites") {
      setPath(null);
      setChildren([]);
      setError(null);
      return;
    }
    const next = `/${segments.slice(0, index + 1).join("/")}`;
    // /Sites alone → site list
    if (/^\/Sites$/i.test(next)) {
      setPath(null);
      setChildren([]);
      setError(null);
      return;
    }
    loadFolder(next);
  };

  if (loading && !path && sites.length === 0) {
    return (
      <p role="status" data-testid="home-library-loading">
        {message(MSG.LOADING)}
      </p>
    );
  }
  if (error) {
    return (
      <p role="alert" style={errorStyle} data-testid="home-library-error">
        {error}
      </p>
    );
  }

  if (!path) {
    if (sites.length === 0) {
      return (
        <p style={emptyStateStyle} data-testid="home-library-empty">
          {isAdmin
            ? message(MSG.NO_SITES_ADMIN)
            : message(MSG.LIBRARY_EMPTY)}
        </p>
      );
    }
    return (
      <div data-testid="home-library-sites">
        <p style={sectionHintStyle}>{message(MSG.LIBRARY_HELP)}</p>
        <ul
          style={listStyle}
          aria-label={message(MSG.SECTION_LIBRARY)}
          data-testid="home-library-site-list"
        >
          {sites.map((site) => (
            <li
              key={String(site.id ?? site.siteId ?? site.name)}
              style={listItemStyle}
            >
              <span style={{ fontWeight: 600 }}>{site.name}</span>
              <div style={itemActionsStyle}>
                <button
                  type="button"
                  style={actionButtonStyle("primary")}
                  data-testid="home-library-open-site"
                  onClick={() => openSite(site.name)}
                >
                  {message(MSG.OPEN_ITEM)}
                </button>
              </div>
            </li>
          ))}
        </ul>
      </div>
    );
  }

  return (
    <div data-testid="home-library-folder">
      <nav
        style={{
          ...sectionHintStyle,
          display: "flex",
          flexWrap: "wrap",
          alignItems: "center",
          gap: 6,
        }}
        aria-label={message(MSG.LIBRARY_BREADCRUMB)}
        data-testid="home-library-breadcrumb"
      >
        <button
          type="button"
          style={actionButtonStyle("ghost")}
          data-testid="home-library-up"
          onClick={goUp}
        >
          ← {message(MSG.LIBRARY_UP)}
        </button>
        <button
          type="button"
          style={actionButtonStyle("ghost")}
          data-testid="home-library-back"
          onClick={() => {
            setPath(null);
            setChildren([]);
            setError(null);
          }}
        >
          {message(MSG.SECTION_LIBRARY)}
        </button>
        {segments.map((seg, i) => (
          <React.Fragment key={`${seg}-${i}`}>
            <span aria-hidden="true">/</span>
            <button
              type="button"
              style={{
                ...actionButtonStyle("ghost"),
                fontWeight: i === segments.length - 1 ? 600 : 400,
              }}
              data-testid={`home-library-crumb-${i}`}
              onClick={() => goToSegment(i)}
            >
              {seg}
            </button>
          </React.Fragment>
        ))}
      </nav>
      <p style={{ margin: "0 0 12px", fontSize: "0.8rem", color: "#666" }}>
        <code data-testid="home-library-path">{path}</code>
      </p>
      {bookmarks.error && (
        <p role="alert" style={errorStyle} data-testid="home-library-bookmark-error">
          {bookmarks.error}
        </p>
      )}
      {loading ? (
        <p role="status">{message(MSG.LOADING)}</p>
      ) : children.length === 0 ? (
        <p style={emptyStateStyle} data-testid="home-library-folder-empty">
          {message(MSG.LIBRARY_FOLDER_EMPTY)}
        </p>
      ) : (
        <ul style={listStyle} data-testid="home-library-children">
          {children.map((child, index) => {
            const name = child.name || child.title || `item-${index}`;
            const childPath =
              (child.path as string) ||
              `${path}/${name}`.replace(/\/+/g, "/");
            const isFolder =
              child.folder === true ||
              child.type === "Folder" ||
              String(child.type).toLowerCase() === "folder";
            const id = contentItemId(child);
            const rowItem: ContentListItem = {
              ...child,
              path: child.path ?? childPath,
            };

            if (isFolder) {
              return (
                <ContentListRow
                  key={String(id ?? childPath)}
                  item={{ ...rowItem, folder: true }}
                  index={index}
                  testIdPrefix="home-library"
                  primaryAction={
                    <button
                      type="button"
                      style={actionButtonStyle("primary")}
                      data-testid="home-library-open-folder"
                      onClick={() => openFolder(childPath)}
                    >
                      {message(MSG.OPEN_ITEM)}
                    </button>
                  }
                />
              );
            }

            return (
              <ContentListRow
                key={String(id ?? childPath)}
                item={rowItem}
                index={index}
                onOpenItem={onOpenItem}
                bookmarkMode={isBookmarkableItem(rowItem) ? "toggle" : "none"}
                isBookmarked={bookmarks.isBookmarked(rowItem)}
                bookmarkPending={id != null && bookmarks.pendingIds.has(id)}
                onBookmark={(row) => {
                  void bookmarks.toggleBookmark(row);
                }}
                testIdPrefix="home-library"
              />
            );
          })}
        </ul>
      )}
    </div>
  );
}
