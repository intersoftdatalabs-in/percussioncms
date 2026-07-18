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

import React, { useCallback, useEffect, useState } from "react";
import { fetchFolderChildren, fetchSites } from "../../api/home/homeApi";
import type { ContentListItem, SiteSummary } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import { errorStyle, listItemStyle, listStyle } from "../home.styles";

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

  const loadSites = useCallback(() => {
    setLoading(true);
    fetchSites()
      .then((list) => {
        setSites(list);
        setError(null);
      })
      .catch(() => setError(message(MSG.ERROR_GENERIC)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadSites();
  }, [loadSites]);

  const openSite = (siteName: string) => {
    const folderPath = `/Sites/${siteName}`;
    setPath(folderPath);
    setLoading(true);
    fetchFolderChildren(folderPath)
      .then((list) => {
        setChildren(list);
        setError(null);
      })
      .catch(() => setError(message(MSG.ERROR_GENERIC)))
      .finally(() => setLoading(false));
  };

  const openFolder = (folderPath: string) => {
    setPath(folderPath);
    setLoading(true);
    fetchFolderChildren(folderPath)
      .then((list) => {
        setChildren(list);
        setError(null);
      })
      .catch(() => setError(message(MSG.ERROR_GENERIC)))
      .finally(() => setLoading(false));
  };

  if (loading && !path && sites.length === 0) {
    return <p role="status">{message(MSG.LOADING)}</p>;
  }
  if (error) {
    return (
      <p role="alert" style={errorStyle}>
        {error}
      </p>
    );
  }

  if (!path) {
    if (sites.length === 0) {
      return (
        <p>
          {isAdmin
            ? message(MSG.NO_SITES_ADMIN)
            : message(MSG.LIBRARY_EMPTY)}
        </p>
      );
    }
    return (
      <div>
        <p>{message(MSG.LIBRARY_HELP)}</p>
        <ul style={listStyle} aria-label={message(MSG.SECTION_LIBRARY)}>
          {sites.map((site) => (
            <li key={String(site.id ?? site.siteId ?? site.name)} style={listItemStyle}>
              <button type="button" onClick={() => openSite(site.name)}>
                {site.name}
              </button>
            </li>
          ))}
        </ul>
      </div>
    );
  }

  return (
    <div>
      <p>
        <button type="button" onClick={() => setPath(null)}>
          ← {message(MSG.SECTION_LIBRARY)}
        </button>{" "}
        <code>{path}</code>
      </p>
      {loading ? (
        <p role="status">{message(MSG.LOADING)}</p>
      ) : (
        <ul style={listStyle}>
          {children.map((child, index) => {
            const name = child.name || child.title || `item-${index}`;
            const childPath =
              (child.path as string) ||
              `${path}/${name}`.replace(/\/+/g, "/");
            const isFolder =
              child.folder === true ||
              child.type === "Folder" ||
              String(child.type).toLowerCase() === "folder";
            return (
              <li key={String(child.id ?? childPath)} style={listItemStyle}>
                <span>{name}</span>
                {isFolder ? (
                  <button type="button" onClick={() => openFolder(childPath)}>
                    {message(MSG.OPEN_ITEM)}
                  </button>
                ) : (
                  <button type="button" onClick={() => onOpenItem?.(child)}>
                    {message(MSG.OPEN_ITEM)}
                  </button>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
