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
 * ContentExplorerShell — product Explorer route shell (feature 992 + #2400).
 *
 * <p>Composes tree, detail list, reduced actions, server-driven action
 * toolbar, context menu, search panel, and display-format selector so the
 * SPA route approaches Desktop Content Explorer parity.</p>
 */

import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  findActions,
  findAllowedContentTypeMenus,
  mapActionMenusToMenuActions,
} from "../api/contentExplorer/actionMenuApi";
import {
  listDisplayFormats,
  normalizeDisplayFormatColumns,
  type DisplayFormat,
} from "../api/contentExplorer/displayFormatsApi";
import { findItemByPath } from "../api/contentExplorer/pathApi";
import type {
  Clipboard,
  ClipboardItem,
  MenuAction,
  PSItemProperties,
  PSPathItem,
} from "../api/contentExplorer/types";
import { useSpaBootstrap } from "../app/bootstrap/BootstrapContext";
import { message } from "../i18n/message";
import { ActionToolbar } from "./ActionToolbar";
import { ClipboardPanel } from "./clipboard/ClipboardPanel";
import { EMPTY_CLIPBOARD, setClipboard as buildClipboard } from "./clipboard/model";
import { ContextMenu } from "./ContextMenu";
import { resolveCurrentUserIdentities } from "./currentUserIdentities";
import { DetailList } from "./DetailList";
import {
  displayFormatOptionKey,
  toDetailDisplayFormat,
} from "./displayFormatMap";
import { ExplorerTree } from "./ExplorerTree";
import { FolderSecurityPanel } from "./FolderSecurityPanel";
import { EXPLORER_MSG } from "./messages";
import { openInEditor } from "./openInEditor";
import {
  ReducedActions,
  defaultReducedActionHandlers,
  type ReducedActionHandlers,
} from "./ReducedActions";
import { SearchPanel } from "./SearchPanel";
import { EMPTY_SELECTION, type Selection } from "./selection";
import {
  errorStateStyle,
  headerStyle,
  headerTitleStyle,
  shellStyle,
} from "./styles";

export interface ContentExplorerShellProps {
  /** Folder path to display on mount; defaults to product root. */
  initialPath?: string;
  /** Override open behavior (default navigates to the editor). */
  onOpenItem?: (item: PSPathItem) => void;
  /** Override create-folder / rename / move / copy / delete handlers. */
  actionHandlers?: Partial<ReducedActionHandlers>;
  /**
   * Fired when a folder is activated. Allows the host to perform additional
   * work (analytics, deep-link handling, etc.) before the list refreshes.
   */
  onFolderActivated?: (path: string, folder: PSPathItem) => void;
  /** Test seam: override display-format catalog load. */
  loadDisplayFormats?: () => Promise<DisplayFormat[]>;
  /** Test seam: override action menu load. */
  loadMenuActions?: (item: PSPathItem | null) => Promise<MenuAction[]>;
  /**
   * Identities for folder ACL self-lockout (USER name + ROLE names).
   * When omitted, derived from SPA bootstrap ({@code userName}, admin/designer
   * flags). Tests inject an explicit list so lockout gates stay deterministic.
   */
  currentUserIdentities?: ReadonlyArray<string>;
  /**
   * Test seam: resolve a CMS folder id from a path for the security panel.
   * Default uses pathmanagement {@link findItemByPath}.
   */
  resolveFolderId?: (path: string) => Promise<string | undefined>;
}

type ContextMenuState = {
  actions: MenuAction[];
  x: number;
  y: number;
} | null;

/**
 * Map a `PSPathItem` (detail-list / tree row shape) into a `ClipboardItem`
 * (clipboard-panel input shape). Single source of truth so the
 * "Add to clipboard" handler and the `<ClipboardPanel items>` prop
 * never disagree on the kind / name / accessLevel mapping.
 *
 * Returns `null` when the item has no stable id (`item.id` and
 * `item.path` both missing) so the caller can skip it instead of
 * injecting a row that would later fail the paste transport.
 */
function toClipboardItem(item: PSPathItem): ClipboardItem | null {
  const id = item.id ?? item.path;
  if (id == null) return null;
  const kind: ClipboardItem["kind"] =
    item.type === "folder"
      ? "folder"
      : item.category === "asset" || item.type === "asset"
        ? "asset"
        : "page";
  return {
    id,
    path: item.path,
    kind,
    name: item.name ?? item.title ?? item.path,
    sourceAccessLevel: item.accessLevel,
  };
}

const sidePanelStyle: React.CSSProperties = {
  gridColumn: "1 / -1",
  borderTop: "1px solid #ddd",
  padding: 8,
  background: "#fcfcfc",
  maxHeight: "40vh",
  overflow: "auto",
};

const toolRowStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: 8,
  alignItems: "center",
  marginTop: 6,
};

function parseContentId(id: string | undefined): number | null {
  if (!id) return null;
  const n = Number(id);
  return Number.isFinite(n) ? n : null;
}

async function defaultLoadMenuActions(
  item: PSPathItem | null,
): Promise<MenuAction[]> {
  const contentId = parseContentId(item?.id);
  if (contentId != null) {
    const menus = await findAllowedContentTypeMenus([contentId]);
    if (menus.length > 0) {
      return mapActionMenusToMenuActions(menus);
    }
  }
  const menus = await findActions({ item: true });
  return mapActionMenusToMenuActions(menus);
}

/** Stable default — module scope so useEffect deps do not refetch every render. */
async function defaultLoadDisplayFormats(): Promise<DisplayFormat[]> {
  return listDisplayFormats({ validForFolder: true });
}

async function defaultResolveFolderId(
  path: string,
): Promise<string | undefined> {
  try {
    const item = await findItemByPath(path);
    return item?.id != null && String(item.id).length > 0
      ? String(item.id)
      : undefined;
  } catch {
    return undefined;
  }
}

export function ContentExplorerShell({
  initialPath = "/",
  onOpenItem = openInEditor,
  actionHandlers,
  onFolderActivated,
  loadDisplayFormats = defaultLoadDisplayFormats,
  loadMenuActions = defaultLoadMenuActions,
  currentUserIdentities: currentUserIdentitiesProp,
  resolveFolderId = defaultResolveFolderId,
}: ContentExplorerShellProps): React.ReactElement {
  const bootstrap = useSpaBootstrap();
  const currentUserIdentities = useMemo(() => {
    if (currentUserIdentitiesProp) {
      return [...currentUserIdentitiesProp];
    }
    return resolveCurrentUserIdentities({
      userName: bootstrap.userName,
      isAdmin: bootstrap.isAdmin,
      isDesigner: bootstrap.isDesigner,
    });
  }, [
    currentUserIdentitiesProp,
    bootstrap.userName,
    bootstrap.isAdmin,
    bootstrap.isDesigner,
  ]);

  // Seed from initialPath so deep-links and the security/properties panel
  // resolve a folder without requiring an extra tree click (#2410).
  const [selection, setSelection] = useState<Selection>(() =>
    initialPath
      ? { folderPath: initialPath, item: null }
      : EMPTY_SELECTION,
  );
  const [error, setError] = useState<string | null>(null);
  const [showSearch, setShowSearch] = useState(false);
  const [showSecurity, setShowSecurity] = useState(false);
  const [showClipboard, setShowClipboard] = useState(false);
  const [displayFormats, setDisplayFormats] = useState<DisplayFormat[]>([]);
  const [selectedFormatKey, setSelectedFormatKey] = useState<string>("");
  const [menuActions, setMenuActions] = useState<MenuAction[]>([]);
  const [contextMenu, setContextMenu] = useState<ContextMenuState>(null);
  const [listEpoch, setListEpoch] = useState(0);
  const [multiSelectedIds, setMultiSelectedIds] = useState<ReadonlySet<string>>(
    () => new Set<string>(),
  );
  const [multiSelectedItems, setMultiSelectedItems] = useState<
    ReadonlyMap<string, PSPathItem>
  >(() => new Map<string, PSPathItem>());
  const [clipboard, setClipboardState] = useState<Clipboard>(EMPTY_CLIPBOARD);
  const [clipboardMode, setClipboardMode] = useState<"copy" | "cut">("copy");
  /** Folder content id for security/properties (resolved from selection or path). */
  const [securityFolderId, setSecurityFolderId] = useState<string | undefined>(
    undefined,
  );

  const handlers: ReducedActionHandlers = {
    ...defaultReducedActionHandlers(),
    ...actionHandlers,
    onOpen: (item) => {
      if (item.type === "folder" || (item.leaf === false && item.id == null)) {
        setSelection({ folderPath: item.path, item: null });
        return;
      }
      onOpenItem(item);
    },
    onPreview: actionHandlers?.onPreview ?? (() => undefined),
  };
  const hasPreviewHandler = Boolean(actionHandlers?.onPreview);

  useEffect(() => {
    let cancelled = false;
    loadDisplayFormats()
      .then((list) => {
        if (cancelled) return;
        setDisplayFormats(list ?? []);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        // Non-fatal: list still works with default columns.
        console.warn(
          "[ContentExplorerShell] display formats load failed",
          err instanceof Error ? err.message : String(err),
        );
      });
    return () => {
      cancelled = true;
    };
  }, [loadDisplayFormats]);

  useEffect(() => {
    let cancelled = false;
    loadMenuActions(selection.item)
      .then((actions) => {
        if (!cancelled) setMenuActions(actions ?? []);
      })
      .catch(() => {
        if (!cancelled) setMenuActions([]);
      });
    return () => {
      cancelled = true;
    };
  }, [selection.item, loadMenuActions, listEpoch]);

  const selectedFormat = useMemo(() => {
    if (!selectedFormatKey) return null;
    return (
      displayFormats.find(
        (df) => displayFormatOptionKey(df) === selectedFormatKey,
      ) ?? null
    );
  }, [displayFormats, selectedFormatKey]);

  const detailDisplayFormat = useMemo(() => {
    if (!selectedFormat) return undefined;
    return toDetailDisplayFormat(
      normalizeDisplayFormatColumns(selectedFormat.columns),
    );
  }, [selectedFormat]);

  const displayFormatId = selectedFormatKey || null;

  const handleSelectFolder = useCallback(
    (path: string, folder: PSPathItem | null) => {
      setSelection({ folderPath: path, item: null });
      // Reset multi-select when the folder changes: stale ids are not
      // present in the new list so the checkbox column would otherwise
      // show phantom selections until the next user click.
      setMultiSelectedIds(new Set<string>());
      setMultiSelectedItems(new Map<string, PSPathItem>());
      setContextMenu(null);
      if (folder) onFolderActivated?.(path, folder);
    },
    [onFolderActivated],
  );

  const handleActivate = useCallback(
    (path: string, folder: PSPathItem) => {
      setSelection({ folderPath: path, item: null });
      setMultiSelectedIds(new Set<string>());
      setMultiSelectedItems(new Map<string, PSPathItem>());
      setContextMenu(null);
      onFolderActivated?.(path, folder);
    },
    [onFolderActivated],
  );

  const handleSelectItem = useCallback((item: PSPathItem) => {
    setSelection((prev) => ({ ...prev, item }));
  }, []);

  const handleActivateItem = useCallback(
    (item: PSPathItem) => {
      handlers.onOpen(item);
    },
    [handlers],
  );

  const handleToggleSelectItem = useCallback(
    (item: PSPathItem, next: boolean) => {
      const id = item.id ?? item.path;
      if (id == null) return;
      setMultiSelectedIds((prev) => {
        const nextSet = new Set(prev);
        if (next) nextSet.add(id);
        else nextSet.delete(id);
        return nextSet;
      });
      setMultiSelectedItems((prev) => {
        const nextMap = new Map(prev);
        if (next) nextMap.set(id, item);
        else nextMap.delete(id);
        return nextMap;
      });
    },
    [],
  );

  const handleActionError = useCallback((msg: string) => {
    setError(msg);
  }, []);

  const handleAddToClipboard = useCallback(() => {
    if (multiSelectedItems.size === 0) return;
    const items: ClipboardItem[] = [];
    for (const item of multiSelectedItems.values()) {
      const clipboard = toClipboardItem(item);
      if (clipboard == null) continue;
      items.push(clipboard);
    }
    setClipboardState((prev) => buildClipboard(prev, clipboardMode, items));
    setShowClipboard(true);
  }, [multiSelectedItems, clipboardMode]);

  const handleClearClipboard = useCallback(() => {
    setClipboardState(EMPTY_CLIPBOARD);
    setMultiSelectedIds(new Set<string>());
    setMultiSelectedItems(new Map<string, PSPathItem>());
  }, []);

  const handleItemContextMenu = useCallback(
    (item: PSPathItem, clientX: number, clientY: number) => {
      setSelection((prev) => ({ ...prev, item }));
      void loadMenuActions(item).then((actions) => {
        setContextMenu({
          actions: actions ?? [],
          x: clientX,
          y: clientY,
        });
      });
    },
    [loadMenuActions],
  );

  const handleSearchOpen = useCallback((result: PSItemProperties) => {
    openInEditor({
      id: result.id != null ? String(result.id) : undefined,
      name: result.name ?? result.title ?? "",
      // PSItemProperties exposes folderPath (parent); id-open when path empty.
      path: result.folderPath ?? "",
      type: result.type,
    });
  }, []);

  const handleSearchReveal = useCallback((result: PSItemProperties) => {
    const folder = result.folderPath;
    if (folder) {
      setSelection({ folderPath: folder, item: null });
      setShowSearch(false);
    }
  }, []);

  const folderForActions: PSPathItem | null =
    selection.item?.type === "folder"
      ? selection.item
      : selection.folderPath
        ? ({
            id: securityFolderId,
            path: selection.folderPath,
            name: selection.folderPath,
            type: "folder",
            accessLevel: "WRITE",
          } as PSPathItem)
        : null;

  // Resolve the folder id for security/properties: prefer a selected
  // folder row, otherwise look up the active folder path (tree navigation
  // sets item=null and only folderPath). Without path resolution ADMIN
  // could not open ACL for the folder they are browsing (#2410).
  useEffect(() => {
    let cancelled = false;
    async function resolve(): Promise<void> {
      if (selection.item?.type === "folder" && selection.item.id) {
        if (!cancelled) setSecurityFolderId(String(selection.item.id));
        return;
      }
      const path = selection.folderPath;
      if (!path) {
        if (!cancelled) setSecurityFolderId(undefined);
        return;
      }
      const id = await resolveFolderId(path);
      if (!cancelled) setSecurityFolderId(id);
    }
    void resolve();
    return () => {
      cancelled = true;
    };
  }, [selection.item, selection.folderPath, resolveFolderId]);

  return (
    <div
      style={shellStyle}
      role="application"
      aria-label={message(EXPLORER_MSG.TITLE)}
      data-testid="content-explorer-shell"
    >
      <header style={headerStyle}>
        <div style={{ display: "flex", flexDirection: "column", gap: 4, flex: 1 }}>
          <h1 style={headerTitleStyle}>{message(EXPLORER_MSG.TITLE)}</h1>
          <div style={toolRowStyle}>
            <ReducedActions
              item={selection.item}
              folder={folderForActions}
              handlers={handlers}
              hasPreviewHandler={hasPreviewHandler}
              onError={handleActionError}
            />
          </div>
          <div
            style={toolRowStyle}
            data-testid="explorer-server-actions"
            role="group"
            aria-label={message(EXPLORER_MSG.SERVER_ACTIONS_ARIA)}
          >
            <ActionToolbar
              actions={menuActions}
              ariaLabel={message(EXPLORER_MSG.SERVER_ACTIONS_ARIA)}
              onInvoke={() => {
                // Server URL actions navigate; client-only refresh list.
                setListEpoch((n) => n + 1);
              }}
            />
          </div>
          <div
            style={toolRowStyle}
            data-testid="explorer-view-tools"
            role="toolbar"
            aria-label={message(EXPLORER_MSG.VIEW_TOOLS_ARIA)}
          >
            <button
              type="button"
              data-testid="explorer-toggle-search"
              aria-pressed={showSearch}
              aria-expanded={showSearch}
              aria-controls="explorer-search-panel"
              aria-label={message(EXPLORER_MSG.TOGGLE_SEARCH_ARIA)}
              onClick={() => setShowSearch((v) => !v)}
            >
              {message(EXPLORER_MSG.SEARCH_TITLE)}
            </button>
            <button
              type="button"
              data-testid="explorer-toggle-security"
              aria-pressed={showSecurity}
              aria-expanded={showSecurity}
              aria-controls="explorer-security-panel"
              aria-label={message(EXPLORER_MSG.TOGGLE_SECURITY_ARIA)}
              onClick={() => setShowSecurity((v) => !v)}
            >
              {message(EXPLORER_MSG.SECURITY_TITLE)}
            </button>
            <button
              type="button"
              data-testid="explorer-toggle-clipboard"
              aria-pressed={showClipboard}
              aria-expanded={showClipboard}
              aria-controls="explorer-clipboard-panel"
              aria-label={message(EXPLORER_MSG.TOGGLE_CLIPBOARD_ARIA)}
              onClick={() => setShowClipboard((v) => !v)}
              disabled={multiSelectedIds.size === 0 && clipboard.items.length === 0}
            >
              {message(EXPLORER_MSG.CLIPBOARD_TITLE)}
              {multiSelectedIds.size > 0 ? (
                <span
                  data-testid="explorer-multi-select-count"
                  style={{ marginLeft: 6, color: "#888" }}
                >
                  (
                  {multiSelectedIds.size === 1
                    ? message(EXPLORER_MSG.SELECTED_COUNT_SINGULAR)
                    : message(EXPLORER_MSG.SELECTED_COUNT_PLURAL).replace(
                        "{count}",
                        String(multiSelectedIds.size),
                      )}
                  )
                </span>
              ) : null}
            </button>
            <button
              type="button"
              data-testid="explorer-clipboard-add"
              disabled={multiSelectedIds.size === 0}
              onClick={handleAddToClipboard}
              aria-label={message(EXPLORER_MSG.CLIPBOARD_ADD)}
            >
              {message(EXPLORER_MSG.CLIPBOARD_ADD)}
            </button>
            <label
              htmlFor="explorer-display-format"
              style={{ display: "inline-flex", gap: 6, alignItems: "center" }}
            >
              <span id="explorer-display-format-label">
                {message(EXPLORER_MSG.DISPLAY_FORMAT_LABEL)}
              </span>
              <select
                id="explorer-display-format"
                data-testid="explorer-display-format"
                value={selectedFormatKey}
                onChange={(e) => setSelectedFormatKey(e.target.value)}
                aria-labelledby="explorer-display-format-label"
              >
                <option value="">
                  {message(EXPLORER_MSG.DISPLAY_FORMAT_DEFAULT)}
                </option>
                {displayFormats.map((df) => {
                  const key = displayFormatOptionKey(df);
                  if (!key) return null;
                  // Server catalog labels (displayName/label/name) are
                  // CMS design data, not product chrome — not TMX keys.
                  const label = df.displayName || df.label || df.name || key;
                  return (
                    <option key={key} value={key}>
                      {label}
                    </option>
                  );
                })}
              </select>
            </label>
          </div>
        </div>
      </header>
      {error && (
        <div
          style={{ ...errorStateStyle, gridColumn: "1 / -1" }}
          role="alert"
          aria-live="assertive"
        >
          {message(EXPLORER_MSG.ERROR_GENERIC)}: {error}
        </div>
      )}
      <ExplorerTree
        initialPath={initialPath}
        selectedPath={selection.folderPath}
        onSelectFolder={handleSelectFolder}
        onActivate={handleActivate}
      />
      <DetailList
        key={`list-${listEpoch}-${displayFormatId ?? "default"}`}
        folderPath={selection.folderPath}
        selectedItemId={selection.item?.id ?? null}
        onSelectItem={handleSelectItem}
        onActivateItem={handleActivateItem}
        displayFormat={detailDisplayFormat}
        displayFormatId={displayFormatId}
        onItemContextMenu={handleItemContextMenu}
        selectedItemIds={multiSelectedIds}
        onToggleSelectItem={handleToggleSelectItem}
      />
      {showSearch && (
        <section
          id="explorer-search-panel"
          style={sidePanelStyle}
          data-testid="explorer-search-panel"
          aria-label={message(EXPLORER_MSG.SEARCH_PANEL_REGION)}
        >
          <SearchPanel
            onOpen={handleSearchOpen}
            onReveal={handleSearchReveal}
            initialCriteria={
              selection.folderPath
                ? { folderPath: selection.folderPath }
                : undefined
            }
          />
        </section>
      )}
      {showClipboard && (
        <section
          id="explorer-clipboard-panel"
          style={sidePanelStyle}
          data-testid="explorer-clipboard-panel"
          aria-label={message(EXPLORER_MSG.CLIPBOARD_REGION)}
        >
          <ClipboardPanel
            clipboard={clipboard}
            onClipboardChange={setClipboardState}
            items={Array.from(multiSelectedItems.values())
              .map((it) => toClipboardItem(it))
              .filter((it): it is ClipboardItem => it != null)}
            mode={clipboardMode}
            onModeChange={setClipboardMode}
            target={
              selection.folderPath
                ? {
                    path: selection.folderPath,
                    accessLevel:
                      selection.item?.type === "folder"
                        ? selection.item.accessLevel
                        : "WRITE",
                  }
                : undefined
            }
            onPasteSettled={() => {
              // After a fully successful paste the host should refresh the list.
              setListEpoch((n) => n + 1);
              handleClearClipboard();
            }}
          />
        </section>
      )}
      {showSecurity && securityFolderId && (
        <section
          id="explorer-security-panel"
          style={sidePanelStyle}
          data-testid="explorer-security-panel"
          aria-label={message(EXPLORER_MSG.SECURITY_PANEL_REGION)}
        >
          <FolderSecurityPanel
            folderId={securityFolderId}
            currentUserIdentities={currentUserIdentities}
          />
        </section>
      )}
      {showSecurity && !securityFolderId && (
        <div
          id="explorer-security-panel"
          style={sidePanelStyle}
          data-testid="explorer-security-hint"
          role="status"
          aria-live="polite"
        >
          {message(EXPLORER_MSG.SECURITY_SELECT_FOLDER)}
        </div>
      )}
      {contextMenu && (
        <div
          style={{
            position: "fixed",
            left: contextMenu.x,
            top: contextMenu.y,
            zIndex: 1000,
          }}
          data-testid="explorer-context-menu-anchor"
          role="presentation"
        >
          <ContextMenu
            actions={contextMenu.actions}
            onClose={() => setContextMenu(null)}
            onInvoke={() => {
              setContextMenu(null);
              setListEpoch((n) => n + 1);
            }}
          />
        </div>
      )}
    </div>
  );
}

export default ContentExplorerShell;
