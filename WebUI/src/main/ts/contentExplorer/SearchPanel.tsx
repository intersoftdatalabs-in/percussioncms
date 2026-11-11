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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Search panel for the modern Content Explorer (US5 / T068 + #2506 saved-search picker).
 *
 * <p>Renders a server-backed free-text search input + results list, and a
 * saved/design-search picker fed by {@code GET /services/searches} that
 * executes via {@code POST /services/searches/{idOrName}/execute}.</p>
 *
 * <p>The {@link SearchPanelProps.onOpen} callback receives the selected result
 * for the host (typically the explorer shell) to navigate to the item;
 * the {@link SearchPanelProps.onReveal} callback asks the host to
 * select the parent folder in the tree. Free-text results come from the
 * sitemanage extended-search REST endpoint via the typed
 * {@link searchExtended} client; saved searches use the design-search
 * execute façade. The panel defers all transport so the host can stub
 * it in tests.</p>
 *
 * <p>State machine (results):</p>
 * <ul>
 *   <li>{@code idle} — initial; submit the form or run a saved search.</li>
 *   <li>{@code loading} — fetch in flight.</li>
 *   <li>{@code ready} — has results (possibly empty).</li>
 *   <li>{@code invalid} — client gate (U+0000 in the query); no POST (#5575).</li>
 *   <li>{@code error} — fetch rejected / server error; Retry re-issues
 *       the last free-text query or last saved-search execute.</li>
 * </ul>
 */

import React, { useEffect, useMemo, useState } from "react";
import {
  searchExtended,
  type PSItemProperties,
  type PSSearchCriteria,
  type PSSearchResults,
} from "../api/contentExplorer/searchApi";
import {
  deleteSearch as deleteSearchApi,
  executeSearch as executeSearchApi,
  listExplorerSavedSearches,
  saveSearch as saveSearchApi,
} from "../api/developer/searchesApi";
import type {
  SearchDef,
  SearchExecuteRequest,
  SearchExecuteResult,
} from "../api/developer/types";
import { formatApiError } from "../api/client";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import { toRepositorySearchFolderPath } from "./folderPath";
import { searchQueryContainsNul } from "./searchQuery";

export interface SearchPanelProps {
  /** Optional initial query (e.g. URL hash / deep-link). */
  initialQuery?: string;
  /** Optional initial criteria (folder scope + sortColumn + maxResults). */
  initialCriteria?: PSSearchCriteria;
  /** Override for the free-text search transport (default: {@link searchExtended}). */
  search?: (criteria: PSSearchCriteria) => Promise<PSSearchResults>;
  /**
   * Override for the saved-search catalog load
   * (default: {@link listExplorerSavedSearches}, includeViews).
   */
  listSavedSearches?: () => Promise<SearchDef[]>;
  /**
   * Override for design-search execute
   * (default: {@link executeSearchApi}). Return value is normalized to
   * {@link PSSearchResults} by the panel when callers return the wire
   * {@link SearchExecuteResult} shape — inject a function that already
   * returns {@link PSSearchResults} for simple test stubs.
   */
  executeSavedSearch?: (
    idOrName: string,
    request?: SearchExecuteRequest | null,
  ) => Promise<PSSearchResults | SearchExecuteResult>;
  /**
   * Override for saved-search rename (default: PUT display label via
   * {@link saveSearchApi}). The catalog name key is not renamed on PUT —
   * rename edits the display label only.
   */
  renameSavedSearch?: (idOrName: string, label: string) => Promise<SearchDef>;
  /**
   * Override for saved-search delete (default: {@link deleteSearchApi}).
   * On success the row is dropped from the catalog; on 400/403/404/409 the
   * rows are kept and the error is shown.
   */
  deleteSavedSearch?: (idOrName: string) => Promise<void>;
  /** Triggered when the user clicks "Open" on a result. */
  onOpen?: (result: PSItemProperties) => void;
  /** Triggered when the user clicks "Reveal in folder" on a result. */
  onReveal?: (result: PSItemProperties) => void;
  /** ARIA label override; defaults to "Search". */
  ariaLabel?: string;
  className?: string;
}

type Status =
  | { kind: "idle"; query: string }
  | { kind: "loading"; query: string }
  | { kind: "ready"; query: string; results: PSSearchResults }
  | { kind: "error"; query: string; message: string }
  | { kind: "invalid"; query: string; message: string };

type CatalogStatus =
  | { kind: "loading" }
  | { kind: "ready"; items: SearchDef[] }
  | { kind: "empty" }
  | { kind: "error"; message: string };

/** Last results request so Retry can re-issue free-text or saved execute. */
type LastRun =
  | { mode: "query"; query: string }
  | { mode: "saved"; idOrName: string; label: string };

function searchKey(def: SearchDef): string {
  return (def.name || def.guid?.stringValue || (def.id != null ? String(def.id) : "")).trim();
}

function searchLabel(def: SearchDef): string {
  return (def.label || def.name || searchKey(def) || "—").trim();
}

/**
 * Custom URL views are listed with searches but execute on the Views tree.
 * Custom URL searches ({@code CustomSearch}) are runnable here (#4929).
 */
function isCustomViewSavedSearch(def: SearchDef | null | undefined): boolean {
  if (def?.customSearch !== true) {
    return false;
  }
  return (def.type ?? "").trim().toLowerCase() === "view";
}

/**
 * Map design-search execute wire (or already-normalized results) into the
 * Explorer {@link PSSearchResults} shape used by the results list.
 */
export function toSearchResults(
  payload: PSSearchResults | SearchExecuteResult | null | undefined,
): PSSearchResults {
  if (payload == null) {
    return { children: [], totalCount: 0, startIndex: 1 };
  }
  const children = Array.isArray(payload.children)
    ? payload.children.map((row) => ({
        id: row.id,
        name: row.name,
        title: row.title,
        folderPath: row.folderPath,
        type: row.type,
      }))
    : [];
  const startIndex =
    typeof payload.startIndex === "number" ? payload.startIndex : 1;
  const totalCount =
    typeof payload.totalCount === "number" ? payload.totalCount : children.length;
  return { children, totalCount, startIndex };
}

/**
 * Default saved-search rename: PUT the display label only. The catalog name
 * key is not renamed on PUT (REST-GAPS-02 / #6075), so the picker-visible
 * label is what changes. Other stored fields are omitted and left unchanged.
 */
export async function defaultRenameSavedSearch(
  idOrName: string,
  label: string,
): Promise<SearchDef> {
  const key = (idOrName ?? "").trim();
  if (!key) {
    throw new Error("Search id or name is required");
  }
  return saveSearchApi(key, { name: key, label: label.trim() });
}

/** Saved-search manage dialog mode (rename label or confirm delete). */
type ManageMode = "rename" | "delete";

/**
 * Pure catalog update for a renamed row: refresh the display label by key,
 * keep every other row (and the catalog key) untouched.
 */
export function renameCatalogRow(
  prev: CatalogStatus,
  key: string,
  label: string,
): CatalogStatus {
  if (prev.kind !== "ready") return prev;
  return {
    kind: "ready",
    items: prev.items.map((d) =>
      searchKey(d) === key ? { ...d, label } : d,
    ),
  };
}

/**
 * Pure catalog update for a deleted row: drop the row by key, keep the rest.
 */
export function deleteCatalogRow(prev: CatalogStatus, key: string): CatalogStatus {
  if (prev.kind !== "ready") return prev;
  return {
    kind: "ready",
    items: prev.items.filter((d) => searchKey(d) !== key),
  };
}

/** Transport + state contract for the saved-search manage hook (#6075). */
export interface SavedSearchManageApi {
  manageMode: ManageMode | null;
  renameDraft: string;
  manageBusy: boolean;
  manageError: string | null;
  manageNotice: string | null;
  setRenameDraft: (value: string) => void;
  openManage: (mode: ManageMode) => void;
  closeManage: () => void;
  handleRenameSave: () => void;
  handleDeleteConfirm: () => void;
}

/**
 * Saved-search rename/delete state machine (#6075 / parent #4530).
 *
 * <p>Rename PUTs the display label only (the catalog name key is not renamed
 * on PUT); delete confirms before DELETE. Cancel closes without transport.
 * Errors stay on the dialog; 400/403/404/409 keep every catalog row because
 * the catalog is only merged after the transport resolves.</p>
 */
export function useSavedSearchManage(opts: {
  selectedKey: string;
  selectedLabel: string;
  setCatalog: React.Dispatch<React.SetStateAction<CatalogStatus>>;
  setSelectedSaved: React.Dispatch<React.SetStateAction<string>>;
  renameSavedSearch: (idOrName: string, label: string) => Promise<SearchDef>;
  deleteSavedSearch: (idOrName: string) => Promise<void>;
}): SavedSearchManageApi {
  const {
    selectedKey,
    selectedLabel,
    setCatalog,
    setSelectedSaved,
    renameSavedSearch,
    deleteSavedSearch,
  } = opts;
  const [manageMode, setManageMode] = useState<ManageMode | null>(null);
  const [renameDraft, setRenameDraft] = useState("");
  const [manageBusy, setManageBusy] = useState(false);
  const [manageError, setManageError] = useState<string | null>(null);
  const [manageNotice, setManageNotice] = useState<string | null>(null);

  function openManage(mode: ManageMode): void {
    if (!selectedKey) return;
    setManageError(null);
    setManageNotice(null);
    setRenameDraft(selectedLabel);
    setManageMode(mode);
  }

  function closeManage(): void {
    if (manageBusy) return;
    setManageMode(null);
    setManageError(null);
  }

  async function handleRenameSave(): Promise<void> {
    if (!selectedKey || manageBusy) return;
    const next = renameDraft.trim();
    if (!next) {
      // Blank refused client-side: no PUT, rows untouched (#6075).
      setManageError(message(EXPLORER_MSG.SEARCH_SAVED_RENAME_EMPTY));
      return;
    }
    setManageBusy(true);
    setManageError(null);
    try {
      const updated = await renameSavedSearch(selectedKey, next);
      // Server echo preferred, draft as fallback; key never changes on PUT.
      const label = (updated.label ?? "").trim() || next;
      setCatalog((prev) => renameCatalogRow(prev, selectedKey, label));
      setManageNotice(message(EXPLORER_MSG.SEARCH_SAVED_RENAMED));
      setManageMode(null);
    } catch (err: unknown) {
      setManageError(
        formatApiError(err, message(EXPLORER_MSG.SEARCH_SAVED_RENAME_ERROR)),
      );
    } finally {
      setManageBusy(false);
    }
  }

  async function handleDeleteConfirm(): Promise<void> {
    if (!selectedKey || manageBusy) return;
    setManageBusy(true);
    setManageError(null);
    try {
      await deleteSavedSearch(selectedKey);
      setCatalog((prev) => deleteCatalogRow(prev, selectedKey));
      setSelectedSaved((prev) => (prev === selectedKey ? "" : prev));
      setManageNotice(message(EXPLORER_MSG.SEARCH_SAVED_DELETED));
      setManageMode(null);
    } catch (err: unknown) {
      setManageError(
        formatApiError(err, message(EXPLORER_MSG.SEARCH_SAVED_DELETE_ERROR)),
      );
    } finally {
      setManageBusy(false);
    }
  }

  return {
    manageMode,
    renameDraft,
    manageBusy,
    manageError,
    manageNotice,
    setRenameDraft,
    openManage,
    closeManage,
    handleRenameSave: () => void handleRenameSave(),
    handleDeleteConfirm: () => void handleDeleteConfirm(),
  };
}

export function SearchPanel(props: SearchPanelProps): React.JSX.Element {
  const {
    initialQuery = "",
    initialCriteria = {},
    search = searchExtended,
    listSavedSearches = listExplorerSavedSearches,
    executeSavedSearch = executeSearchApi,
    renameSavedSearch = defaultRenameSavedSearch,
    deleteSavedSearch = deleteSearchApi,
    onOpen,
    onReveal,
    ariaLabel,
    className,
  } = props;
  const [status, setStatus] = useState<Status>({
    kind: "idle",
    query: initialQuery,
  });
  const [draft, setDraft] = useState(initialQuery);
  const [catalog, setCatalog] = useState<CatalogStatus>({ kind: "loading" });
  const [selectedSaved, setSelectedSaved] = useState("");
  const [lastRun, setLastRun] = useState<LastRun | null>(null);
  const [catalogEpoch, setCatalogEpoch] = useState(0);

  useEffect(() => {
    setDraft(initialQuery);
    if (initialQuery) {
      void runSearch(initialQuery);
    }
    // We intentionally exclude `runSearch` from deps — it captures
    // initialCriteria once via the closure below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialQuery]);

  useEffect(() => {
    let cancelled = false;
    setCatalog({ kind: "loading" });
    listSavedSearches()
      .then((items) => {
        if (cancelled) return;
        const list = Array.isArray(items) ? items : [];
        if (list.length === 0) {
          setCatalog({ kind: "empty" });
          return;
        }
        setCatalog({ kind: "ready", items: list });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setCatalog({
          kind: "error",
          message: formatApiError(err, message(EXPLORER_MSG.SEARCH_SAVED_ERROR)),
        });
      });
    return () => {
      cancelled = true;
    };
  }, [listSavedSearches, catalogEpoch]);

  const sortedCatalog = useMemo(() => {
    if (catalog.kind !== "ready") return [];
    return [...catalog.items].sort((a, b) =>
      searchLabel(a).localeCompare(searchLabel(b), undefined, {
        sensitivity: "base",
      }),
    );
  }, [catalog]);

  const selectedDef =
    catalog.kind === "ready"
      ? sortedCatalog.find((d) => searchKey(d) === selectedSaved)
      : undefined;

  async function runSearch(query: string): Promise<void> {
    // U+0000 stays in the panel and is not POSTed (#5575).
    if (searchQueryContainsNul(query)) {
      setStatus({
        kind: "invalid",
        query,
        message: message(EXPLORER_MSG.SEARCH_QUERY_NUL),
      });
      return;
    }
    const trimmed = query.trim();
    if (trimmed.length === 0) {
      setStatus({ kind: "idle", query: "" });
      return;
    }
    setLastRun({ mode: "query", query: trimmed });
    setStatus({ kind: "loading", query: trimmed });
    try {
      const results = await search({
        ...initialCriteria,
        query: trimmed,
        startIndex: initialCriteria.startIndex ?? 1,
        maxResults: initialCriteria.maxResults ?? 25,
        folderPath: toRepositorySearchFolderPath(initialCriteria.folderPath),
      });
      setStatus({ kind: "ready", query: trimmed, results });
    } catch (err: unknown) {
      setStatus({
        kind: "error",
        query: trimmed,
        message: formatApiError(err, message(EXPLORER_MSG.SEARCH_ERROR)),
      });
    }
  }

  async function runSavedSearch(idOrName: string, label: string): Promise<void> {
    const key = idOrName.trim();
    if (!key) {
      return;
    }
    const display = (label || key).trim();
    setLastRun({ mode: "saved", idOrName: key, label: display });
    setStatus({ kind: "loading", query: display });
    try {
      const request: SearchExecuteRequest = {
        folderPath: toRepositorySearchFolderPath(initialCriteria.folderPath),
        startIndex: initialCriteria.startIndex ?? 1,
        maxResults: initialCriteria.maxResults ?? 25,
        sortColumn: initialCriteria.sortColumn,
        sortOrder: initialCriteria.sortOrder,
      };
      const raw = await executeSavedSearch(key, request);
      const results = toSearchResults(raw);
      setStatus({ kind: "ready", query: display, results });
    } catch (err: unknown) {
      setStatus({
        kind: "error",
        query: display,
        message: formatApiError(err, message(EXPLORER_MSG.SEARCH_ERROR)),
      });
    }
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>): void {
    e.preventDefault();
    void runSearch(draft);
  }

  function handleRunSaved(): void {
    if (!selectedDef) return;
    if (isCustomViewSavedSearch(selectedDef)) {
      setStatus({
        kind: "error",
        query: searchLabel(selectedDef),
        message: message(EXPLORER_MSG.SEARCH_SAVED_CUSTOM_UNSUPPORTED),
      });
      return;
    }
    void runSavedSearch(searchKey(selectedDef), searchLabel(selectedDef));
  }

  function handleRetry(): void {
    if (lastRun?.mode === "saved") {
      void runSavedSearch(lastRun.idOrName, lastRun.label);
      return;
    }
    if (lastRun?.mode === "query") {
      void runSearch(lastRun.query);
      return;
    }
    if (status.kind === "error") {
      void runSearch(status.query);
    }
  }

  /** Rename/delete state machine for the selected saved search (#6075). */
  const manage = useSavedSearchManage({
    selectedKey: selectedSaved,
    selectedLabel: selectedDef ? searchLabel(selectedDef) : "",
    setCatalog,
    setSelectedSaved,
    renameSavedSearch,
    deleteSavedSearch,
  });

  return (
    <section
      role="search"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.SEARCH_TITLE)}
      className={className}
      data-testid="search-panel"
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <form onSubmit={handleSubmit} style={{ display: "flex", gap: 8 }}>
        <label htmlFor="search-panel-input" style={{ display: "none" }}>
          {message(EXPLORER_MSG.SEARCH_TITLE)}
        </label>
        <input
          id="search-panel-input"
          type="search"
          data-testid="search-panel-input"
          placeholder={message(EXPLORER_MSG.SEARCH_PLACEHOLDER)}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          style={{ flex: 1, padding: "4px 8px" }}
        />
        <button
          type="submit"
          data-testid="search-panel-submit"
          disabled={status.kind === "loading" || draft.trim().length === 0}
        >
          {message(EXPLORER_MSG.SEARCH_SUBMIT)}
        </button>
      </form>

      <SavedSearchPicker
        catalog={catalog}
        sortedItems={sortedCatalog}
        selectedKey={selectedSaved}
        onSelectKey={setSelectedSaved}
        onRun={handleRunSaved}
        onRetryCatalog={() => setCatalogEpoch((n) => n + 1)}
        runDisabled={
          status.kind === "loading" ||
          !selectedSaved ||
          isCustomViewSavedSearch(selectedDef)
        }
        selectedIsCustom={isCustomViewSavedSearch(selectedDef)}
        manageDisabled={
          manage.manageBusy ||
          !selectedSaved ||
          isCustomViewSavedSearch(selectedDef)
        }
        onRename={() => manage.openManage("rename")}
        onDelete={() => manage.openManage("delete")}
      />
      {manage.manageNotice ? (
        <p
          role="status"
          data-testid="search-panel-saved-manage-notice"
          style={{ marginTop: 8, color: "#276749" }}
        >
          {manage.manageNotice}
        </p>
      ) : null}
      <ManageSavedSearchDialog
        mode={manage.manageMode}
        busy={manage.manageBusy}
        error={manage.manageError}
        currentLabel={selectedDef ? searchLabel(selectedDef) : ""}
        renameDraft={manage.renameDraft}
        onRenameDraft={manage.setRenameDraft}
        onSaveRename={manage.handleRenameSave}
        onConfirmDelete={manage.handleDeleteConfirm}
        onClose={manage.closeManage}
      />

      <SearchStatusView
        status={status}
        onOpen={onOpen}
        onReveal={onReveal}
        onRetry={handleRetry}
      />
    </section>
  );
}

function SavedSearchPicker(props: {
  catalog: CatalogStatus;
  sortedItems: SearchDef[];
  selectedKey: string;
  onSelectKey: (key: string) => void;
  onRun: () => void;
  onRetryCatalog: () => void;
  runDisabled: boolean;
  selectedIsCustom: boolean;
  /** Rename/delete stay disabled without a runnable selection (#6075). */
  manageDisabled: boolean;
  onRename: () => void;
  onDelete: () => void;
}): React.JSX.Element {
  const {
    catalog,
    sortedItems,
    selectedKey,
    onSelectKey,
    onRun,
    onRetryCatalog,
    runDisabled,
    selectedIsCustom,
    manageDisabled,
    onRename,
    onDelete,
  } = props;

  if (catalog.kind === "loading") {
    return (
      <p
        role="status"
        aria-live="polite"
        data-testid="search-panel-saved-loading"
        style={{ marginTop: 10, color: "#666" }}
      >
        {message(EXPLORER_MSG.SEARCH_SAVED_LOADING)}
      </p>
    );
  }

  if (catalog.kind === "error") {
    return (
      <div role="alert" style={{ marginTop: 10, color: "#a00" }}>
        <p data-testid="search-panel-saved-error" style={{ margin: "0 0 6px 0" }}>
          {message(EXPLORER_MSG.SEARCH_SAVED_ERROR)}: {catalog.message}
        </p>
        <button
          type="button"
          data-testid="search-panel-saved-retry"
          onClick={onRetryCatalog}
        >
          {message(EXPLORER_MSG.SEARCH_SAVED_RETRY)}
        </button>
      </div>
    );
  }

  if (catalog.kind === "empty") {
    return (
      <p
        role="status"
        data-testid="search-panel-saved-empty"
        style={{ marginTop: 10, color: "#595959" }}
      >
        {message(EXPLORER_MSG.SEARCH_SAVED_EMPTY)}
      </p>
    );
  }

  return (
    <div
      data-testid="search-panel-saved-picker"
      style={{
        display: "flex",
        flexWrap: "wrap",
        gap: 8,
        alignItems: "center",
        marginTop: 10,
      }}
    >
      <label
        htmlFor="search-panel-saved-select"
        style={{ fontSize: 13, color: "#333" }}
      >
        {message(EXPLORER_MSG.SEARCH_SAVED_LABEL)}
      </label>
      <select
        id="search-panel-saved-select"
        data-testid="search-panel-saved-select"
        value={selectedKey}
        onChange={(e) => onSelectKey(e.target.value)}
        style={{ flex: "1 1 160px", minWidth: 140, padding: "4px 6px" }}
        aria-label={message(EXPLORER_MSG.SEARCH_SAVED_LABEL)}
      >
        <option value="">{message(EXPLORER_MSG.SEARCH_SAVED_PLACEHOLDER)}</option>
        {sortedItems.map((def, idx) => {
          const key = searchKey(def);
          if (!key) return null;
          return (
            <option key={`${key}-${idx}`} value={key}>
              {searchLabel(def)}
              {isCustomViewSavedSearch(def)
                ? " (view URL)"
                : def.customSearch
                  ? " (URL)"
                  : ""}
            </option>
          );
        })}
      </select>
      <button
        type="button"
        data-testid="search-panel-saved-run"
        disabled={runDisabled}
        onClick={onRun}
        title={
          selectedIsCustom
            ? message(EXPLORER_MSG.SEARCH_SAVED_CUSTOM_UNSUPPORTED)
            : undefined
        }
      >
        {message(EXPLORER_MSG.SEARCH_SAVED_RUN)}
      </button>
      <button
        type="button"
        data-testid="search-panel-saved-rename"
        disabled={manageDisabled}
        onClick={onRename}
        title={
          selectedIsCustom
            ? message(EXPLORER_MSG.SEARCH_SAVED_CUSTOM_UNSUPPORTED)
            : undefined
        }
      >
        {message(EXPLORER_MSG.SEARCH_SAVED_RENAME)}
      </button>
      <button
        type="button"
        data-testid="search-panel-saved-delete"
        disabled={manageDisabled}
        onClick={onDelete}
        title={
          selectedIsCustom
            ? message(EXPLORER_MSG.SEARCH_SAVED_CUSTOM_UNSUPPORTED)
            : undefined
        }
      >
        {message(EXPLORER_MSG.SEARCH_SAVED_DELETE)}
      </button>
    </div>
  );
}

/**
 * Rename / delete dialog for the selected saved search (#6075).
 * Rename edits the display label (PUT); delete confirms before DELETE.
 * Cancel closes without transport. Errors stay on the dialog; rows are kept.
 */
function ManageSavedSearchDialog(props: {
  mode: ManageMode | null;
  busy: boolean;
  error: string | null;
  currentLabel: string;
  renameDraft: string;
  onRenameDraft: (value: string) => void;
  onSaveRename: () => void;
  onConfirmDelete: () => void;
  onClose: () => void;
}): React.JSX.Element {
  const {
    mode,
    busy,
    error,
    currentLabel,
    renameDraft,
    onRenameDraft,
    onSaveRename,
    onConfirmDelete,
    onClose,
  } = props;
  if (mode == null) {
    return <></>;
  }
  const renameSaveDisabled = busy || renameDraft.trim().length === 0;
  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={
        mode === "rename"
          ? message(EXPLORER_MSG.SEARCH_SAVED_RENAME_TITLE)
          : message(EXPLORER_MSG.SEARCH_SAVED_DELETE_TITLE)
      }
      data-testid="search-panel-saved-manage-dialog"
      style={{
        border: "1px solid #999",
        borderRadius: 4,
        padding: 12,
        marginTop: 10,
        background: "#fafafa",
      }}
    >
      {mode === "rename" ? (
        <>
          <p data-testid="search-panel-saved-manage-title" style={{ margin: "0 0 8px 0" }}>
            {message(EXPLORER_MSG.SEARCH_SAVED_RENAME_TITLE)}: {currentLabel}
          </p>
          <label
            htmlFor="search-panel-saved-rename-input"
            style={{ display: "block", fontSize: 13, color: "#333", marginBottom: 4 }}
          >
            {message(EXPLORER_MSG.SEARCH_SAVED_RENAME_NEW_LABEL)}
          </label>
          <input
            id="search-panel-saved-rename-input"
            type="text"
            data-testid="search-panel-saved-rename-input"
            value={renameDraft}
            onChange={(e) => onRenameDraft(e.target.value)}
            style={{ width: "100%", padding: "4px 8px", boxSizing: "border-box" }}
          />
        </>
      ) : (
        <p data-testid="search-panel-saved-manage-title" style={{ margin: "0 0 8px 0" }}>
          {message(EXPLORER_MSG.SEARCH_SAVED_DELETE_CONFIRM)} {currentLabel}
        </p>
      )}
      {error ? (
        <p
          role="alert"
          data-testid="search-panel-saved-manage-error"
          style={{ color: "#a00", margin: "8px 0 0 0" }}
        >
          {error}
        </p>
      ) : null}
      <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
        {mode === "rename" ? (
          <button
            type="button"
            data-testid="search-panel-saved-rename-save"
            disabled={renameSaveDisabled}
            onClick={onSaveRename}
          >
            {message(EXPLORER_MSG.SEARCH_SAVED_RENAME_SAVE)}
          </button>
        ) : (
          <button
            type="button"
            data-testid="search-panel-saved-delete-confirm"
            disabled={busy}
            onClick={onConfirmDelete}
          >
            {message(EXPLORER_MSG.SEARCH_SAVED_DELETE_DELETE)}
          </button>
        )}
        <button
          type="button"
          data-testid="search-panel-saved-manage-cancel"
          disabled={busy}
          onClick={onClose}
        >
          {mode === "rename"
            ? message(EXPLORER_MSG.SEARCH_SAVED_RENAME_CANCEL)
            : message(EXPLORER_MSG.SEARCH_SAVED_DELETE_CANCEL)}
        </button>
      </div>
    </div>
  );
}

function SearchStatusView(props: {
  status: Status;
  onOpen?: (r: PSItemProperties) => void;
  onReveal?: (r: PSItemProperties) => void;
  /** Re-issues the failed search. Optional to keep the helper pure; the
   * parent {@link SearchPanel} wires its own retry closure
   * here so the parent stays the single owner of the transport.
   *
   * <p>(Plain-text reference; {@code SearchStatusView.onRetry} is a
   * destructured prop parameter rather than a property of the function,
   * so it cannot be the target of a {@code {@link ...}} tag.)</p>
   */
  onRetry?: () => void;
}): React.JSX.Element {
  const { status, onOpen, onReveal, onRetry } = props;
  if (status.kind === "loading") {
    return (
      <p
        role="status"
        aria-live="polite"
        data-testid="search-panel-loading"
        style={{ marginTop: 8 }}
      >
        {message(EXPLORER_MSG.SEARCH_LOADING)}
      </p>
    );
  }
  if (status.kind === "invalid") {
    return (
      <div role="alert" style={{ color: "#a00", marginTop: 8 }}>
        <p data-testid="search-panel-query-error">{status.message}</p>
      </div>
    );
  }
  if (status.kind === "error") {
    return (
      <div role="alert" style={{ color: "#a00", marginTop: 8 }}>
        <p data-testid="search-panel-error">
          {message(EXPLORER_MSG.SEARCH_ERROR)}: {status.message}
        </p>
        <button
          type="button"
          data-testid="search-panel-retry"
          disabled={!onRetry}
          onClick={() => onRetry?.()}
        >
          {message(EXPLORER_MSG.RETRY)}
        </button>
      </div>
    );
  }
  if (status.kind === "ready") {
    if (status.results.children.length === 0) {
      return (
        <p
          role="status"
          aria-live="polite"
          data-testid="search-panel-empty"
          style={{ marginTop: 8, color: "#595959" }}
        >
          {message(EXPLORER_MSG.SEARCH_EMPTY)}
        </p>
      );
    }
    return (
      <ul
        data-testid="search-panel-results"
        style={{ listStyle: "none", padding: 0, margin: "8px 0 0 0" }}
        aria-label={`Search results for "${status.query}"`}
      >
        {status.results.children.map((r, idx) => (
          <li
            key={`${r.id ?? r.title ?? "row"}-${idx}`}
            data-testid="search-panel-result-row"
            data-item-type={r.type ?? ""}
            style={{
              display: "flex",
              gap: 8,
              alignItems: "center",
              borderBottom: "1px solid #eee",
              padding: "4px 0",
            }}
          >
            <span style={{ flex: 1 }} data-mkd-lang-ignore="1">
              <strong>{r.title ?? r.name ?? r.id}</strong>
              <small style={{ marginLeft: 6, color: "#595959" }}>
                {r.folderPath ?? r.type}
              </small>
            </span>
            <button
              type="button"
              data-testid={`search-panel-open-${r.id ?? idx}`}
              onClick={() => onOpen?.(r)}
              disabled={!onOpen}
            >
              {message(EXPLORER_MSG.SEARCH_OPEN)}
            </button>
            <button
              type="button"
              data-testid={`search-panel-reveal-${r.id ?? idx}`}
              onClick={() => onReveal?.(r)}
              disabled={!onReveal}
            >
              {message(EXPLORER_MSG.SEARCH_REVEAL)}
            </button>
          </li>
        ))}
      </ul>
    );
  }
  return <></>;
}
