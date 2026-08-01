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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * ReducedAction set bar/menu for the modern Content Explorer (US1 / T020).
 *
 * <p>Implements the FR-010a intermediate hard-cut action surface
 * (open / preview / create folder / rename / move / copy / delete with
 * confirm). The full configuration-driven menu set (FR-010) expands,
 * not replaces, this surface in US3.</p>
 *
 * <p>Each handler returns a {@link Promise} so the parent shell can render
 * a "saving" indicator and propagate server errors. Destructive actions
 * (delete) require an explicit confirmation prompt before invoking the
 * server call.</p>
 */

import React, { useCallback, useState } from "react";
import { formatApiError } from "../api/client";
import { addNewFolder, deleteItem, moveItem, renameFolder } from "../api/contentExplorer/pathApi";
import type { PSPathItem } from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import { canAdmin, canWrite, isFolder } from "./selection";
import { actionButtonStyle, actionsBarStyle } from "./styles";
import { EXPLORER_MSG } from "./messages";

export type ReducedActionKey =
  | "open"
  | "preview"
  | "createFolder"
  | "rename"
  | "move"
  | "copy"
  | "delete";

export interface ReducedActionHandlers {
  onOpen: (item: PSPathItem) => void | Promise<void>;
  onPreview: (item: PSPathItem) => void | Promise<void>;
  onCreateFolder: (parent: PSPathItem, name: string) => Promise<void>;
  onRename: (item: PSPathItem, newName: string) => Promise<void>;
  onMove: (item: PSPathItem, targetPath: string) => Promise<void>;
  onCopy: (item: PSPathItem, targetPath: string) => Promise<void>;
  onDelete: (item: PSPathItem) => Promise<void>;
  /**
   * Optional prompt helper (defaults to {@link window.prompt} / confirm).
   * Hosts may override to provide a richer dialog.
   */
  prompt?: (msg: string, def?: string) => string | null;
  confirm?: (msg: string) => boolean;
}

export interface ReducedActionsProps {
  /** Selected item (null disables item-scoped actions). */
  item: PSPathItem | null;
  /** Currently open folder (parent for create-folder). */
  folder: PSPathItem | null;
  /** Map of action -> handler implementations. */
  handlers: ReducedActionHandlers;
  /** Set true to render all buttons disabled (e.g. during a server call). */
  busy?: boolean;
  /**
   * When true, the Preview button is rendered disabled. US1 ships without
   * a preview pane (lands in US3 / T055); hosts without an explicit preview
   * handler should pass {@link hasPreviewHandler}={false} to gray the button
   * instead of letting it no-op silently.
   */
  hasPreviewHandler?: boolean;
  /** Fires when an action returns a server error so the shell can surface it. */
  onError?: (message: string) => void;
}

const defaultPrompt = (msg: string, def?: string): string | null => {
  if (typeof window === "undefined") return def ?? null;
  return window.prompt(msg, def ?? "");
};

const defaultConfirm = (msg: string): boolean => {
  if (typeof window === "undefined") return false;
  return window.confirm(msg);
};

export function ReducedActions({
  item,
  folder,
  handlers,
  busy,
  hasPreviewHandler = false,
  onError,
}: ReducedActionsProps): React.ReactElement {
  const [pending, setPending] = useState<ReducedActionKey | null>(null);

  const isItemFolder = isFolder(item);
  const itemWrite = canWrite(item) || canAdmin(item);
  const folderWrite = canWrite(folder) || canAdmin(folder);

  const runItemAction = useCallback(
    async (key: ReducedActionKey, fn: () => Promise<void> | void) => {
      setPending(key);
      try {
        await fn();
      } catch (err) {
        // handleResponse throws plain ApiError objects — use formatApiError
        // so shells never surface "[object Object]".
        const msg = formatApiError(err, message(EXPLORER_MSG.ERROR_GENERIC));
        onError?.(msg);
      } finally {
        setPending(null);
      }
    },
    [onError],
  );

  const handleOpen = useCallback(() => {
    if (!item) return;
    void runItemAction("open", () => handlers.onOpen(item));
  }, [handlers, item, runItemAction]);

  const handlePreview = useCallback(() => {
    if (!item) return;
    void runItemAction("preview", () => handlers.onPreview(item));
  }, [handlers, item, runItemAction]);

  const handleCreateFolder = useCallback(() => {
    const parent = folder ?? item;
    if (!parent) return;
    const prompt = handlers.prompt ?? defaultPrompt;
    const name = prompt(
      message(EXPLORER_MSG.PROMPT_NEW_FOLDER_NAME),
      "New Folder",
    );
    if (!name) return;
    void runItemAction("createFolder", () => handlers.onCreateFolder(parent, name));
  }, [folder, handlers, item, runItemAction]);

  const handleRename = useCallback(() => {
    if (!item) return;
    const prompt = handlers.prompt ?? defaultPrompt;
    const newName = prompt(
      message(EXPLORER_MSG.PROMPT_NEW_NAME),
      item.name ?? item.path,
    );
    if (!newName || newName === item.name) return;
    void runItemAction("rename", () => handlers.onRename(item, newName));
  }, [handlers, item, runItemAction]);

  const handleMove = useCallback(() => {
    if (!item) return;
    const prompt = handlers.prompt ?? defaultPrompt;
    const target = prompt("Target folder path", item.folderPath ?? "/");
    if (!target) return;
    void runItemAction("move", () => handlers.onMove(item, target));
  }, [handlers, item, runItemAction]);

  const handleCopy = useCallback(() => {
    if (!item) return;
    const prompt = handlers.prompt ?? defaultPrompt;
    const target = prompt("Target folder path", item.folderPath ?? "/");
    if (!target) return;
    void runItemAction("copy", () => handlers.onCopy(item, target));
  }, [handlers, item, runItemAction]);

  const handleDelete = useCallback(() => {
    if (!item) return;
    const confirm = handlers.confirm ?? defaultConfirm;
    const ok = confirm(
      `${message(EXPLORER_MSG.CONFIRM_DELETE_BODY)}\n\n${item.path}`,
    );
    if (!ok) return;
    void runItemAction("delete", () => handlers.onDelete(item));
  }, [handlers, item, runItemAction]);

  const isBusy = busy || pending !== null;

  return (
    <div
      style={actionsBarStyle}
      role="toolbar"
      aria-label={message(EXPLORER_MSG.TITLE)}
      data-testid="reduced-actions"
    >
      <button
        type="button"
        style={actionButtonStyle(!item || isBusy)}
        disabled={!item || isBusy}
        onClick={handleOpen}
        data-testid="action-open"
      >
        {message(EXPLORER_MSG.ACTION_OPEN)}
      </button>
      <button
        type="button"
        style={actionButtonStyle(!item || !hasPreviewHandler || isBusy)}
        disabled={!item || !hasPreviewHandler || isBusy}
        onClick={handlePreview}
        data-testid="action-preview"
      >
        {message(EXPLORER_MSG.ACTION_PREVIEW)}
      </button>
      <button
        type="button"
        style={actionButtonStyle(!folderWrite || isBusy)}
        disabled={!folderWrite || isBusy}
        onClick={handleCreateFolder}
        data-testid="action-create-folder"
      >
        {message(EXPLORER_MSG.ACTION_CREATE_FOLDER)}
      </button>
      <button
        type="button"
        style={actionButtonStyle(!item || !itemWrite || isBusy)}
        disabled={!item || !itemWrite || isBusy}
        onClick={handleRename}
        data-testid="action-rename"
      >
        {message(EXPLORER_MSG.ACTION_RENAME)}
      </button>
      <button
        type="button"
        style={actionButtonStyle(!item || !itemWrite || isBusy)}
        disabled={!item || !itemWrite || isBusy}
        onClick={handleMove}
        data-testid="action-move"
      >
        {message(EXPLORER_MSG.ACTION_MOVE)}
      </button>
      <button
        type="button"
        style={actionButtonStyle(!item || !itemWrite || isBusy)}
        disabled={!item || !itemWrite || isBusy}
        onClick={handleCopy}
        data-testid="action-copy"
      >
        {message(EXPLORER_MSG.ACTION_COPY)}
      </button>
      <button
        type="button"
        style={actionButtonStyle(!item || !itemWrite || !isItemFolder || isBusy)}
        disabled={!item || !itemWrite || !isItemFolder || isBusy}
        onClick={handleDelete}
        data-testid="action-delete"
      >
        {message(EXPLORER_MSG.ACTION_DELETE)}
      </button>
    </div>
  );
}

/**
 * Default handler set wired to {@link pathApi}. The shell uses this when
 * no override is supplied. Each handler maps the UI payload to the
 * server contract and translates failures into thrown errors.
 *
 * <p><strong>Note for hosts</strong>: {@link ReducedActionHandlers.onOpen}
 * is a no-op for folder items (navigation into a folder is owned by the
 * shell via tree/list selection, not the action bar). Hosts that surface
 * the explorer in a context where the user can double-click open a
 * content item must override {@link ReducedActionHandlers.onOpen} for
 * non-folder items (e.g. delegate to {@code openInEditor}).</p>
 *
 * <p>{@link ReducedActionHandlers.onPreview} is a no-op in US1; the
 * preview pane lands in US3. UI grays out the Preview button when no
 * preview handler is supplied.</p>
 */
export function defaultReducedActionHandlers(): ReducedActionHandlers {
  return {
    onOpen: (item) => {
      if (isFolder(item)) {
        // Folder open = navigation is owned by the shell via tree/list
        // selection (clicking a folder in the tree sets selection.folderPath).
        return;
      }
      // Non-folder open: hosts must override onOpen to perform real work
      // (e.g. navigate to the editor). Default is a no-op so the shell
      // can use the default handlers without throwing for folders.
      return;
    },
    onPreview: () => {
      // Preview pane lands in US3 (T055). No-op for US1.
    },
    onCreateFolder: async (parent, name) => {
      await addNewFolder(parent.path, name);
    },
    onRename: async (item, newName) => {
      await renameFolder({ path: item.path, newName });
    },
    onMove: async (item, targetPath) => {
      await moveItem({ sourcePath: item.path, targetPath });
    },
    onCopy: async (item, targetPath) => {
      await moveItem({ sourcePath: item.path, targetPath, copy: true });
    },
    onDelete: async (item) => {
      await deleteItem(item.path);
    },
  };
}