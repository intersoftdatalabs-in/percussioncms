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
import type {
  MenuAction,
  PSItemProperties,
  PSPathItem,
} from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import { ActionToolbar } from "./ActionToolbar";
import { ContextMenu } from "./ContextMenu";
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
}

type ContextMenuState = {
  actions: MenuAction[];
  x: number;
  y: number;
} | null;

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

export function ContentExplorerShell({
  initialPath = "/",
  onOpenItem = openInEditor,
  actionHandlers,
  onFolderActivated,
  loadDisplayFormats = () => listDisplayFormats({ validForFolder: true }),
  loadMenuActions = defaultLoadMenuActions,
}: ContentExplorerShellProps): React.ReactElement {
  const [selection, setSelection] = useState<Selection>(EMPTY_SELECTION);
  const [error, setError] = useState<string | null>(null);
  const [showSearch, setShowSearch] = useState(false);
  const [showSecurity, setShowSecurity] = useState(false);
  const [displayFormats, setDisplayFormats] = useState<DisplayFormat[]>([]);
  const [selectedFormatKey, setSelectedFormatKey] = useState<string>("");
  const [menuActions, setMenuActions] = useState<MenuAction[]>([]);
  const [contextMenu, setContextMenu] = useState<ContextMenuState>(null);
  const [listEpoch, setListEpoch] = useState(0);

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
      setContextMenu(null);
      if (folder) onFolderActivated?.(path, folder);
    },
    [onFolderActivated],
  );

  const handleActivate = useCallback(
    (path: string, folder: PSPathItem) => {
      setSelection({ folderPath: path, item: null });
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

  const handleActionError = useCallback((msg: string) => {
    setError(msg);
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
            id: undefined,
            path: selection.folderPath,
            name: selection.folderPath,
            type: "folder",
            accessLevel: "WRITE",
          } as PSPathItem)
        : null;

  const securityFolderId =
    selection.item?.type === "folder"
      ? selection.item.id
      : undefined;

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
      {showSecurity && securityFolderId && (
        <section
          id="explorer-security-panel"
          style={sidePanelStyle}
          data-testid="explorer-security-panel"
          aria-label={message(EXPLORER_MSG.SECURITY_PANEL_REGION)}
        >
          <FolderSecurityPanel
            folderId={securityFolderId}
            currentUserIdentities={[]}
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
