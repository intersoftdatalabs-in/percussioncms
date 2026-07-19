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
 * ContentExplorerShell — modern React shell for primary content navigation
 * (feature 992-react-content-explorer US1 / T017).
 *
 * <p>Layout: grid with tree on the left, detail list on the right, and a
 * header carrying the title + the ReducedAction bar. Selection state is
 * lifted here; the tree and list are controlled components.</p>
 *
 * <p>The shell does not own navigation; onOpen delegates to the host
 * (default = {@link openInEditor}) so dialog mounts can override.</p>
 */

import React, { useCallback, useState } from "react";
import type { PSPathItem } from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import { DetailList } from "./DetailList";
import { ExplorerTree } from "./ExplorerTree";
import { EXPLORER_MSG } from "./messages";
import { openInEditor } from "./openInEditor";
import {
  ReducedActions,
  defaultReducedActionHandlers,
  type ReducedActionHandlers,
} from "./ReducedActions";
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
}

export function ContentExplorerShell({
  initialPath = "/",
  onOpenItem = openInEditor,
  actionHandlers,
  onFolderActivated,
}: ContentExplorerShellProps): React.ReactElement {
  const [selection, setSelection] = useState<Selection>(EMPTY_SELECTION);
  const [error, setError] = useState<string | null>(null);

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
    onPreview: actionHandlers?.onPreview,
  };
  // Preview is real only when the host supplies its own handler; default is no-op.
  const hasPreviewHandler = Boolean(actionHandlers?.onPreview);

  const handleSelectFolder = useCallback(
    (path: string, folder: PSPathItem | null) => {
      setSelection({ folderPath: path, item: null });
      if (folder) onFolderActivated?.(path, folder);
    },
    [onFolderActivated],
  );

  const handleActivate = useCallback(
    (path: string, folder: PSPathItem) => {
      setSelection({ folderPath: path, item: null });
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

  return (
    <div
      style={shellStyle}
      role="application"
      aria-label={message(EXPLORER_MSG.TITLE)}
      data-testid="content-explorer-shell"
    >
      <header style={headerStyle}>
        <h1 style={headerTitleStyle}>{message(EXPLORER_MSG.TITLE)}</h1>
        <ReducedActions
          item={selection.item}
          folder={
            selection.item?.type === "folder"
              ? selection.item
              : selection.folderPath
                ? // US1 default: assume WRITE on the active folder so the
                  // user can create sub-folders without first selecting an
                  // item. Server enforces AuthZ; we surface failures via
                  // onError. US4 (ACL) tightens this with real permission
                  // data lifted from the tree.
                  ({
                    id: undefined,
                    path: selection.folderPath,
                    name: selection.folderPath,
                    type: "folder",
                    accessLevel: "WRITE",
                  } as PSPathItem)
                : null
          }
          handlers={handlers}
          hasPreviewHandler={hasPreviewHandler}
          onError={handleActionError}
        />
      </header>
      {error && (
        <div style={{ ...errorStateStyle, gridColumn: "1 / -1" }} role="alert">
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
        folderPath={selection.folderPath}
        selectedItemId={selection.item?.id ?? null}
        onSelectItem={handleSelectItem}
        onActivateItem={handleActivateItem}
      />
    </div>
  );
}

export default ContentExplorerShell;