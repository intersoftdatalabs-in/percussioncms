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
 * <p>Composes DCE-style top menu bar (Content / View / Help), tree, detail
 * list, reduced actions, server-driven action toolbar (with nested MENU
 * dropdowns and enablement filtering from {@code rest/actions}), item/folder
 * context menu, search panel, IA relationships panel, dependency viewer, and
 * display-format selector so the SPA route approaches Desktop Content Explorer
 * parity (#2400 / #2407 / #2731 / #2768 / #2769 / #2849).</p>
 */

import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
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
import {
  getItemWorkflowTransitions,
  transitionItem,
} from "../api/contentExplorer/itemWorkflowApi";
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
import {
  filterContextMenuActions,
  filterToolbarActions,
} from "./actionEnablement";
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
import { ExplorerMenuBar } from "./ExplorerMenuBar";
import { ExplorerTree } from "./ExplorerTree";
import { FolderSecurityPanel } from "./FolderSecurityPanel";
import type { ExplorerMenuCommandId } from "./menuBarModel";
import { EXPLORER_MSG } from "./messages";
import { openInEditor } from "./openInEditor";
import { openPreviewItem } from "./previewItem";
import {
  ReducedActions,
  defaultReducedActionHandlers,
  type ReducedActionHandlers,
} from "./ReducedActions";
import { SearchPanel, type SearchPanelProps } from "./SearchPanel";
import { EMPTY_SELECTION, type Selection } from "./selection";
import { resolveFolderPathFromSelection } from "./folderPath";
import { resolveSiteNameFromSelection } from "./sitePath";
import {
  errorStateStyle,
  headerStyle,
  headerTitleStyle,
  shellStyle,
} from "./styles";
import { TranslationsPanel } from "./TranslationsPanel";
import { SiteCopyWizard } from "./wizards/SiteCopyWizard";
import { SubfolderCopyWizard } from "./wizards/SubfolderCopyWizard";
import { RelationshipsView } from "./views/RelationshipsView";
import { DependencyViewer } from "./views/DependencyViewer";
import type { PSNodeRelationshipSummary } from "../api/contentExplorer/relationship";
import {
  buildWorkflowTransitionMenu,
  mergeWorkflowMenuActions,
  parseWorkflowTransitionTrigger,
} from "./workflowMenuActions";

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
   * Test seam: override workflow-transition menu enrichment for a selected
   * content item (#2732). Default loads triggers via itemmanagement.
   * Return {@code null} to skip workflow group (folders / no triggers).
   */
  loadWorkflowMenuActions?: (
    item: PSPathItem | null,
  ) => Promise<MenuAction | null>;
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
  /**
   * Test seam: execute a workflow transition. Default uses itemmanagement
   * {@code transitionWithComments}.
   */
  runWorkflowTransition?: (
    itemId: string,
    trigger: string,
  ) => Promise<void>;
  /**
   * Test seam: load consolidated relationship summary for DependencyViewer
   * (#2768). Default uses {@code fetchNodeSummary} via the viewer.
   */
  loadDependencySummary?: (
    itemId: string,
  ) => Promise<PSNodeRelationshipSummary>;
  /**
   * Test / host seam: free-text search transport for the product Search panel
   * (#2850 / #2407). Default {@link SearchPanel} → searchExtended.
   */
  search?: SearchPanelProps["search"];
  /**
   * Test / host seam: saved-search catalog for the product Search panel.
   * Default {@link SearchPanel} → listSearches.
   */
  listSavedSearches?: SearchPanelProps["listSavedSearches"];
  /**
   * Test / host seam: design-search execute for the product Search panel.
   * Default {@link SearchPanel} → executeSearch.
   */
  executeSavedSearch?: SearchPanelProps["executeSavedSearch"];
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

/** True when the selection can receive workflow transitions (not a folder). */
function isWorkflowEligibleItem(item: PSPathItem | null | undefined): boolean {
  if (!item) return false;
  if (item.type === "folder") return false;
  const id = item.id != null ? String(item.id).trim() : "";
  return id.length > 0;
}

/**
 * Load the server action catalog for the current selection (#2849).
 *
 * <p>When a content item is selected, prefer per-content-type menus from
 * {@code POST /actions/find/types}. Otherwise load the full cascading tree
 * from {@code GET /actions/find} (no {@code item=true} filter) so toolbar
 * dropdown parents ({@code MENU}) remain available — {@code item=true}
 * would keep only flat {@code MENUITEM} roots and drop nested chrome.</p>
 *
 * <p>Desktop-only / surface enablement is applied by the shell after load
 * via {@link filterToolbarActions} / {@link filterContextMenuActions}.</p>
 */
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
  const menus = await findActions({});
  return mapActionMenusToMenuActions(menus);
}

/**
 * Load Workflow cascade from itemmanagement allowed transitions.
 * Returns {@code null} when the item is ineligible or has no triggers.
 */
async function defaultLoadWorkflowMenuActions(
  item: PSPathItem | null,
): Promise<MenuAction | null> {
  if (!isWorkflowEligibleItem(item)) {
    return null;
  }
  try {
    const state = await getItemWorkflowTransitions(String(item!.id));
    return buildWorkflowTransitionMenu(state?.transitionTriggers ?? [], {
      groupLabel: message(EXPLORER_MSG.WORKFLOW_MENU_LABEL),
      stateName: state?.stateName,
    });
  } catch {
    // Non-fatal: keep server action menus without workflow group.
    return null;
  }
}

async function defaultRunWorkflowTransition(
  itemId: string,
  trigger: string,
): Promise<void> {
  await transitionItem(itemId, trigger);
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
  loadWorkflowMenuActions = defaultLoadWorkflowMenuActions,
  currentUserIdentities: currentUserIdentitiesProp,
  resolveFolderId = defaultResolveFolderId,
  runWorkflowTransition = defaultRunWorkflowTransition,
  loadDependencySummary,
  search: searchTransport,
  listSavedSearches,
  executeSavedSearch,
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
  const [showTranslations, setShowTranslations] = useState(false);
  /** Content → Site Copy wizard panel (#2767 / parent #2400). */
  const [showSiteCopy, setShowSiteCopy] = useState(false);
  /** Content → Subfolder Copy wizard panel (#2792 / parent #2400). */
  const [showSubfolderCopy, setShowSubfolderCopy] = useState(false);
  const [showRelationships, setShowRelationships] = useState(false);
  const [showDependencies, setShowDependencies] = useState(false);
  const [displayFormats, setDisplayFormats] = useState<DisplayFormat[]>([]);
  const [selectedFormatKey, setSelectedFormatKey] = useState<string>("");
  const [menuActions, setMenuActions] = useState<MenuAction[]>([]);
  const [contextMenu, setContextMenu] = useState<ContextMenuState>(null);
  /**
   * Monotonic generation for context-menu loads. Rapid right-clicks on
   * different rows race two async IIFEs; only the latest generation may
   * call setContextMenu (avoids stale workflow menus at the first item's XY).
   */
  const contextMenuRequestIdRef = useRef(0);
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
    // Product default: Finder-equivalent page/asset preview (#2733). Hosts
    // may still override via actionHandlers.onPreview.
    onPreview:
      actionHandlers?.onPreview ??
      (async (item) => {
        await openPreviewItem(item);
      }),
  };
  // Always true for product shell (built-in openPreviewItem); override still counts.
  const hasPreviewHandler = true;

  const handleRefreshList = useCallback(() => {
    setListEpoch((n) => n + 1);
    setError(null);
  }, []);

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
    async function loadMenus(): Promise<void> {
      try {
        const base = await loadMenuActions(selection.item);
        if (cancelled) return;
        let workflow: MenuAction | null = null;
        try {
          workflow = await loadWorkflowMenuActions(selection.item);
        } catch {
          workflow = null;
        }
        if (cancelled) return;
        // Toolbar surface: drop desktop-only URLs and CONTEXTMENU roots (#2849).
        const merged = mergeWorkflowMenuActions(base ?? [], workflow);
        setMenuActions(filterToolbarActions(merged));
      } catch {
        if (!cancelled) setMenuActions([]);
      }
    }
    void loadMenus();
    return () => {
      cancelled = true;
    };
  }, [selection.item, loadMenuActions, loadWorkflowMenuActions, listEpoch]);

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
      const requestId = ++contextMenuRequestIdRef.current;
      void (async () => {
        try {
          const base = await loadMenuActions(item);
          if (requestId !== contextMenuRequestIdRef.current) return;
          let workflow: MenuAction | null = null;
          try {
            workflow = await loadWorkflowMenuActions(item);
          } catch {
            workflow = null;
          }
          if (requestId !== contextMenuRequestIdRef.current) return;
          // Context-menu surface: keep CONTEXTMENU roots; drop desktop-only (#2849).
          const merged = mergeWorkflowMenuActions(base ?? [], workflow);
          setContextMenu({
            actions: filterContextMenuActions(merged),
            x: clientX,
            y: clientY,
          });
        } catch {
          if (requestId !== contextMenuRequestIdRef.current) return;
          setContextMenu({
            actions: [],
            x: clientX,
            y: clientY,
          });
        }
      })();
    },
    [loadMenuActions, loadWorkflowMenuActions],
  );

  /**
   * Route toolbar / context-menu activations. Workflow transition names are
   * client-tagged ({@code workflow-transition:Trigger}); other actions fall
   * through to list refresh (URL actions already navigated in the child).
   */
  const handleMenuInvoke = useCallback(
    (actionName: string, _action: MenuAction) => {
      const trigger = parseWorkflowTransitionTrigger(actionName);
      if (trigger != null) {
        const item = selection.item;
        const itemId =
          item?.id != null && isWorkflowEligibleItem(item)
            ? String(item.id)
            : "";
        if (!itemId) {
          setError(message(EXPLORER_MSG.WORKFLOW_TRANSITION_FAILED));
          return;
        }
        void (async () => {
          try {
            await runWorkflowTransition(itemId, trigger);
            setError(null);
            setListEpoch((n) => n + 1);
          } catch (err: unknown) {
            const detail =
              err instanceof Error && err.message
                ? err.message
                : message(EXPLORER_MSG.WORKFLOW_TRANSITION_FAILED);
            setError(detail);
          }
        })();
        return;
      }
      setListEpoch((n) => n + 1);
    },
    [selection.item, runWorkflowTransition],
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

  const siteNameForCopy = useMemo(
    () =>
      resolveSiteNameFromSelection(
        selection.folderPath,
        selection.item?.path,
      ),
    [selection.folderPath, selection.item?.path],
  );
  const hasSiteContext = siteNameForCopy != null;
  const sourceFolderPathForCopy = useMemo(
    () =>
      resolveFolderPathFromSelection(
        selection.folderPath,
        selection.item?.path,
        selection.item?.type,
      ),
    [selection.folderPath, selection.item?.path, selection.item?.type],
  );
  const hasFolderContext = sourceFolderPathForCopy != null;
  const hasDependencyItem =
    selection.item != null &&
    selection.item.type !== "folder" &&
    selection.item.id != null &&
    String(selection.item.id).trim().length > 0;

  const handleMenuBarCommand = useCallback(
    (id: ExplorerMenuCommandId) => {
      switch (id) {
        case "content-search":
        case "view-search":
          setShowSearch((v) => !v);
          break;
        case "content-clipboard-add":
          handleAddToClipboard();
          break;
        case "content-site-copy":
          // Only open when a site is in context; menu item is disabled otherwise.
          if (siteNameForCopy) {
            setShowSiteCopy((v) => !v);
          }
          break;
        case "content-subfolder-copy":
          // Only open when a folder is in context; menu item is disabled otherwise.
          if (sourceFolderPathForCopy) {
            setShowSubfolderCopy((v) => !v);
          }
          break;
        case "view-refresh":
          handleRefreshList();
          break;
        case "view-security":
          setShowSecurity((v) => !v);
          break;
        case "view-translations":
          setShowTranslations((v) => !v);
          break;
        case "view-relationships":
          setShowRelationships((v) => !v);
          break;
        case "view-dependencies":
          setShowDependencies((v) => !v);
          break;
        case "view-clipboard":
          setShowClipboard((v) => !v);
          break;
        case "help-explorer":
          // Product help site — open in a new tab when available.
          if (typeof window !== "undefined") {
            window.open(
              "https://percussioncmshelp.intsof.com/",
              "_blank",
              "noopener,noreferrer",
            );
          }
          break;
        case "help-about":
          if (typeof window !== "undefined") {
            window.alert(message(EXPLORER_MSG.MENU_HELP_ABOUT_BODY));
          }
          break;
        default:
          break;
      }
    },
    [
      handleAddToClipboard,
      handleRefreshList,
      siteNameForCopy,
      sourceFolderPathForCopy,
    ],
  );

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
      try {
        const id = await resolveFolderId(path);
        if (!cancelled) setSecurityFolderId(id);
      } catch {
        // Custom injectors may reject; defaultResolveFolderId already swallows.
        // Treat failure as unresolved so security stays read-only hint, not crash.
        if (!cancelled) setSecurityFolderId(undefined);
      }
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
          <ExplorerMenuBar
            showSearch={showSearch}
            showSecurity={showSecurity}
            showTranslations={showTranslations}
            showRelationships={showRelationships}
            showDependencies={showDependencies}
            hasDependencyItem={hasDependencyItem}
            showClipboard={showClipboard}
            showSiteCopy={showSiteCopy}
            showSubfolderCopy={showSubfolderCopy}
            multiSelectedCount={multiSelectedIds.size}
            clipboardItemCount={clipboard.items.length}
            hasSiteContext={hasSiteContext}
            hasFolderContext={hasFolderContext}
            displayFormats={displayFormats}
            selectedFormatKey={selectedFormatKey}
            onSelectFormat={setSelectedFormatKey}
            onCommand={handleMenuBarCommand}
          />
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
              onInvoke={handleMenuInvoke}
            />
          </div>
          {/* Always-visible refresh residual (#2733); View menu also has Refresh (#2731). */}
          <div
            style={toolRowStyle}
            data-testid="explorer-view-tools"
            role="toolbar"
            aria-label={message(EXPLORER_MSG.VIEW_TOOLS_ARIA)}
          >
            <button
              type="button"
              data-testid="explorer-refresh-list"
              aria-label={message(EXPLORER_MSG.ACTION_REFRESH_ARIA)}
              title={message(EXPLORER_MSG.ACTION_REFRESH_ARIA)}
              onClick={handleRefreshList}
            >
              {message(EXPLORER_MSG.ACTION_REFRESH)}
            </button>
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
            search={searchTransport}
            listSavedSearches={listSavedSearches}
            executeSavedSearch={executeSavedSearch}
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
      {showTranslations &&
        selection.item &&
        selection.item.type !== "folder" &&
        parseContentId(selection.item.id) != null && (
          <section
            id="explorer-translations-panel"
            style={sidePanelStyle}
            data-testid="explorer-translations-panel"
            aria-label={message(EXPLORER_MSG.TRANSLATIONS_PANEL_REGION)}
          >
            <TranslationsPanel
              itemId={String(selection.item.id)}
              itemLabel={
                selection.item.name ??
                selection.item.title ??
                selection.item.path
              }
              onCreated={() => {
                setListEpoch((n) => n + 1);
              }}
            />
          </section>
        )}
      {showTranslations &&
        !(
          selection.item &&
          selection.item.type !== "folder" &&
          parseContentId(selection.item.id) != null
        ) && (
          <div
            id="explorer-translations-panel"
            style={sidePanelStyle}
            data-testid="explorer-translations-hint"
            role="status"
            aria-live="polite"
          >
            {message(EXPLORER_MSG.TRANSLATIONS_SELECT_ITEM)}
          </div>
        )}
      {showSiteCopy && siteNameForCopy && (
        <section
          id="explorer-site-copy-panel"
          style={sidePanelStyle}
          data-testid="explorer-site-copy-panel"
          aria-label={message(EXPLORER_MSG.SITE_COPY_PANEL_REGION)}
        >
          {/*
            key remounts wizard when source site changes so initialSource
            seeds the first step (useState does not re-apply props).
          */}
          <SiteCopyWizard
            key={`site-copy-${siteNameForCopy}`}
            initialSource={siteNameForCopy}
            onSettled={(ok) => {
              if (ok) {
                setListEpoch((n) => n + 1);
              }
            }}
          />
        </section>
      )}
      {showSiteCopy && !siteNameForCopy && (
        <div
          id="explorer-site-copy-hint"
          style={sidePanelStyle}
          data-testid="explorer-site-copy-hint"
          role="status"
          aria-live="polite"
        >
          {message(EXPLORER_MSG.SITE_COPY_SELECT_SITE)}
        </div>
      )}
      {showSubfolderCopy && sourceFolderPathForCopy && (
        <section
          id="explorer-subfolder-copy-panel"
          style={sidePanelStyle}
          data-testid="explorer-subfolder-copy-panel"
          aria-label={message(EXPLORER_MSG.SUBFOLDER_COPY_PANEL_REGION)}
        >
          {/*
            key remounts wizard when source folder changes so initialSource
            seeds the first step (useState does not re-apply props).
          */}
          <SubfolderCopyWizard
            key={`subfolder-copy-${sourceFolderPathForCopy}`}
            initialSource={sourceFolderPathForCopy}
            onSettled={(ok) => {
              if (ok) {
                setListEpoch((n) => n + 1);
              }
            }}
          />
        </section>
      )}
      {showSubfolderCopy && !sourceFolderPathForCopy && (
        <div
          id="explorer-subfolder-copy-hint"
          style={sidePanelStyle}
          data-testid="explorer-subfolder-copy-hint"
          role="status"
          aria-live="polite"
        >
          {message(EXPLORER_MSG.SUBFOLDER_COPY_SELECT_FOLDER)}
        </div>
      )}
      {showRelationships &&
        selection.item &&
        selection.item.type !== "folder" &&
        parseContentId(selection.item.id) != null && (
          <section
            id="explorer-relationships-panel"
            style={sidePanelStyle}
            data-testid="explorer-relationships-panel"
            aria-label={message(EXPLORER_MSG.RELATIONSHIPS_PANEL_REGION)}
          >
            <RelationshipsView
              item={{
                id: String(selection.item.id),
                path: selection.item.path,
                folderPath: selection.folderPath || undefined,
              }}
            />
          </section>
        )}
      {showRelationships &&
        !(
          selection.item &&
          selection.item.type !== "folder" &&
          parseContentId(selection.item.id) != null
        ) && (
          <div
            id="explorer-relationships-panel"
            style={sidePanelStyle}
            data-testid="explorer-relationships-hint"
            role="status"
            aria-live="polite"
          >
            {message(EXPLORER_MSG.RELATIONSHIPS_SELECT_ITEM)}
          </div>
        )}
      {showDependencies && hasDependencyItem && (
          <section
            id="explorer-dependencies-panel"
            style={sidePanelStyle}
            data-testid="explorer-dependencies-panel"
            aria-label={message(EXPLORER_MSG.DEPENDENCY_PANEL_REGION)}
          >
            <DependencyViewer
              item={{
                id: String(selection.item!.id),
                path: selection.item!.path,
                folderPath: selection.folderPath ?? undefined,
              }}
              loadServerSummary={loadDependencySummary}
            />
          </section>
        )}
      {showDependencies && !hasDependencyItem && (
          <div
            id="explorer-dependencies-hint"
            style={sidePanelStyle}
            data-testid="explorer-dependencies-hint"
            role="status"
            aria-live="polite"
          >
            {message(EXPLORER_MSG.DEPENDENCY_SELECT_ITEM)}
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
            onInvoke={(actionName, action) => {
              setContextMenu(null);
              handleMenuInvoke(actionName, action);
            }}
          />
        </div>
      )}
    </div>
  );
}

export default ContentExplorerShell;
