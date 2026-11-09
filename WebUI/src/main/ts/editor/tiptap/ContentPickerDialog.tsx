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

/**
 * React content-picker lookup for the TipTap adv link/image dialogs
 * (#5900 / parent #5622).
 *
 * <p>The classic {@code ContentBrowserDialog.jsp} cannot run in the React
 * editor host, so the adv dialogs fell back to manual URL entry for managed
 * content. This dialog restores browse/search parity on the React REST
 * surface: folder browse through pathmanagement
 * ({@code findChildren}, same rows as the related-item picker) and
 * free-text search through searchmanagement ({@code searchExtended}),
 * starting at the CMS {@code /Sites} and {@code /Assets} roots.</p>
 *
 * <p>Picking a row returns the selection ({@code contentId} + repository
 * path); the hosting dialog resolves the managed-link reference through the
 * render-link preview and fills the same stored attributes classic wrote.
 * Manual entry stays available in the host dialog — the picker never
 * replaces it.</p>
 */

import React, { useEffect, useState } from "react";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import styles from "../EditorHost.module.css";
import { findChildren } from "../../api/contentExplorer/pathApi";
import { searchExtended } from "../../api/contentExplorer/searchApi";
import type {
  PSPathItem,
  PSSearchResults,
} from "../../api/contentExplorer/types";
import {
  relatedFolderRows,
  type RelatedFolderRow,
} from "../editorRelatedFolderPick";
import {
  CONTENT_PICKER_DEFAULT_ROOT,
  CONTENT_PICKER_ROOTS,
  CONTENT_PICKER_SEARCH_MAX_RESULTS,
  contentPickerSearchRows,
} from "./tiptapContentPicker";

/** Item the picker hands to the hosting adv dialog. */
export interface ContentPickerSelection {
  contentId: number;
  path: string;
  name: string;
}

export interface ContentPickerDialogProps {
  /** Field name, used to scope test ids. */
  name: string;
  /** Hosting dialog: link or image (labels the picker title). */
  mode: "link" | "image";
  /** Called with the picked item; the host resolves the managed reference. */
  onPick: (selection: ContentPickerSelection) => void;
  onClose: () => void;
  /** Overridable for tests. Defaults to pathmanagement {@code findChildren}. */
  listFolder?: (path: string) => Promise<PSPathItem[]>;
  /** Overridable for tests. Defaults to searchmanagement {@code searchExtended}. */
  searchContent?: (query: string) => Promise<PSSearchResults>;
}

function defaultSearch(query: string): Promise<PSSearchResults> {
  return searchExtended({
    query,
    startIndex: 1,
    maxResults: CONTENT_PICKER_SEARCH_MAX_RESULTS,
  });
}

export function ContentPickerDialog({
  name,
  mode,
  onPick,
  onClose,
  listFolder = findChildren,
  searchContent = defaultSearch,
}: ContentPickerDialogProps): React.ReactElement {
  const [path, setPath] = useState(CONTENT_PICKER_DEFAULT_ROOT);
  const [rows, setRows] = useState<RelatedFolderRow[]>([]);
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);
  const [query, setQuery] = useState("");
  const [searching, setSearching] = useState(false);
  const [searchFailed, setSearchFailed] = useState(false);
  const [searched, setSearched] = useState(false);
  const [hits, setHits] = useState<RelatedFolderRow[]>([]);
  const [picked, setPicked] = useState<ContentPickerSelection | null>(null);

  async function loadFolder(next: string): Promise<void> {
    const target = next.trim() || CONTENT_PICKER_DEFAULT_ROOT;
    setPath(target);
    setBusy(true);
    setFailed(false);
    try {
      const children = await listFolder(target);
      setRows(relatedFolderRows(children));
    } catch {
      setRows([]);
      setFailed(true);
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    void loadFolder(CONTENT_PICKER_DEFAULT_ROOT);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function runSearch(): Promise<void> {
    const text = query.trim();
    if (!text) {
      return;
    }
    setSearching(true);
    setSearchFailed(false);
    setSearched(true);
    try {
      const results = await searchContent(text);
      setHits(contentPickerSearchRows(results.children));
    } catch {
      setHits([]);
      setSearchFailed(true);
    } finally {
      setSearching(false);
    }
  }

  function selectRow(row: RelatedFolderRow): void {
    if (row.folder || row.contentId == null) {
      void loadFolder(row.path);
      return;
    }
    setPicked({ contentId: row.contentId, path: row.path, name: row.name });
  }

  function rowButton(
    row: RelatedFolderRow,
    testId: string,
  ): React.ReactElement {
    const selected =
      picked != null &&
      !row.folder &&
      picked.contentId === row.contentId &&
      picked.path === row.path;
    return (
      <li key={row.key}>
        <button
          type="button"
          data-testid={testId}
          data-path={row.path}
          aria-pressed={selected}
          className={styles.tiptapLinkApply}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => selectRow(row)}
        >
          {row.folder ? `▸ ${row.name}` : `${row.name}`}
        </button>
      </li>
    );
  }

  return (
    <span
      role="dialog"
      aria-label={message(EDITOR_MSG.TIPTAP_PERC_PICK_TITLE)}
      data-testid={`tiptap-content-picker-${name}`}
      data-picker-mode={mode}
      className={`${styles.tiptapLinkEditor} ${styles.tiptapPercDialog}`}
    >
      <span role="group" aria-label={message(EDITOR_MSG.TIPTAP_PERC_PICK_PATH)}>
        {CONTENT_PICKER_ROOTS.map((root) => (
          <button
            key={root}
            type="button"
            data-testid={`tiptap-content-picker-root-${name}`}
            data-root={root}
            aria-pressed={path === root || path.startsWith(`${root}/`)}
            className={styles.tiptapLinkApply}
            disabled={busy}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => {
              setPicked(null);
              void loadFolder(root);
            }}
          >
            {root.replace(/^\//, "")}
          </button>
        ))}
        <input
          data-testid={`tiptap-content-picker-path-${name}`}
          aria-label={message(EDITOR_MSG.TIPTAP_PERC_PICK_PATH)}
          className={styles.tiptapLinkInput}
          value={path}
          onChange={(e) => setPath(e.target.value)}
        />
        <button
          type="button"
          data-testid={`tiptap-content-picker-list-${name}`}
          className={styles.tiptapLinkApply}
          disabled={busy}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => {
            setPicked(null);
            void loadFolder(path);
          }}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_LIST)}
        </button>
      </span>
      {busy ? (
        <div
          className={styles.status}
          data-testid={`tiptap-content-picker-loading-${name}`}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_LOADING)}
        </div>
      ) : null}
      {failed ? (
        <span
          role="alert"
          className={styles.tiptapLinkError}
          data-testid={`tiptap-content-picker-error-${name}`}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_FAILED)}
        </span>
      ) : null}
      {!busy && !failed && rows.length === 0 ? (
        <div
          className={styles.status}
          data-testid={`tiptap-content-picker-empty-${name}`}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_EMPTY)}
        </div>
      ) : null}
      {rows.length > 0 ? (
        <ul data-testid={`tiptap-content-picker-browse-${name}`}>
          {rows.map((row) =>
            rowButton(row, `tiptap-content-picker-row-${name}`),
          )}
        </ul>
      ) : null}
      <span
        role="group"
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_PICK_SEARCH)}
      >
        <input
          data-testid={`tiptap-content-picker-search-${name}`}
          aria-label={message(EDITOR_MSG.TIPTAP_PERC_PICK_QUERY)}
          className={styles.tiptapLinkInput}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              void runSearch();
            }
          }}
        />
        <button
          type="button"
          data-testid={`tiptap-content-picker-search-button-${name}`}
          className={styles.tiptapLinkApply}
          disabled={searching || !query.trim()}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => void runSearch()}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_SEARCH)}
        </button>
      </span>
      {searching ? (
        <div
          className={styles.status}
          data-testid={`tiptap-content-picker-searching-${name}`}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_LOADING)}
        </div>
      ) : null}
      {searchFailed ? (
        <span
          role="alert"
          className={styles.tiptapLinkError}
          data-testid={`tiptap-content-picker-search-error-${name}`}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_FAILED)}
        </span>
      ) : null}
      {searched && !searching && !searchFailed && hits.length === 0 ? (
        <div
          className={styles.status}
          data-testid={`tiptap-content-picker-search-empty-${name}`}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_EMPTY)}
        </div>
      ) : null}
      {hits.length > 0 ? (
        <ul data-testid={`tiptap-content-picker-results-${name}`}>
          {hits.map((row) =>
            rowButton(row, `tiptap-content-picker-hit-${name}`),
          )}
        </ul>
      ) : null}
      <button
        type="button"
        data-testid={`tiptap-content-picker-use-${name}`}
        className={styles.tiptapLinkApply}
        disabled={picked == null}
        onMouseDown={(e) => e.preventDefault()}
        onClick={() => {
          if (picked != null) {
            onPick(picked);
          }
        }}
      >
        {message(EDITOR_MSG.TIPTAP_PERC_PICK_USE)}
      </button>
      <button
        type="button"
        data-testid={`tiptap-content-picker-close-${name}`}
        className={styles.tiptapLinkApply}
        onMouseDown={(e) => e.preventDefault()}
        onClick={onClose}
      >
        <span aria-hidden="true">✕</span>
        <span className={styles.tiptapSrOnly}>
          {message(EDITOR_MSG.TIPTAP_PERC_CLOSE)}
        </span>
      </button>
    </span>
  );
}
