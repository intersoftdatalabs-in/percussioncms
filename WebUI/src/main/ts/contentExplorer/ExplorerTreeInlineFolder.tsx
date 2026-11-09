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
 * Inline new-folder row for the modern Content Explorer tree (#5882).
 *
 * <p>Operators type a name directly at the selected tree location: Enter
 * commits, Escape cancels and leaves the tree unchanged. Blank,
 * NUL-bearing, and illegal names are refused client-side (no POST).
 * A server 409 keeps the row open with the conflict named and does not
 * refresh the tree. Pure validation/formatting helpers live here so
 * {@code ExplorerTree} stays under the cognitive-complexity gate
 * (#5793).</p>
 */

import React, { useState } from "react";
import { isApiError } from "../api/client";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import {
  folderNameContainsNul,
  isValidExplorerFolderName,
} from "./folderName";
import { formatCreateFolderError } from "./createFolderErrors";
import { nodeRowStyle } from "./styles";

/** Why an inline new-folder name must not be sent. */
export type InlineNewFolderFieldReason = "blank" | "nul" | "invalid";

/**
 * Client checks before an inline new-folder commit. Blank (including
 * whitespace-only) is refused before any POST (#5882); NUL and illegal
 * segments match the Create Folder dialog gates.
 */
export function validateInlineNewFolderName(
  name: string | null | undefined,
): InlineNewFolderFieldReason | null {
  if (folderNameContainsNul(name ?? "")) {
    return "nul";
  }
  const next = String(name ?? "").trim();
  if (!next) {
    return "blank";
  }
  if (!isValidExplorerFolderName(next)) {
    return "invalid";
  }
  return null;
}

/** Inline field error text for a client-side refusal (row stays open). */
export function formatInlineNewFolderFieldError(
  reason: InlineNewFolderFieldReason,
): string {
  if (reason === "blank") {
    return message(EXPLORER_MSG.TREE_INLINE_NEW_FOLDER_BLANK);
  }
  if (reason === "nul") {
    return message(EXPLORER_MSG.ACTION_CREATE_FOLDER_NUL);
  }
  return message(EXPLORER_MSG.ACTION_CREATE_FOLDER_INVALID);
}

/**
 * Inline commit error text for a failed POST. A 409 names the conflicting
 * folder and the row stays open with no tree refresh (#5882). Other
 * failures reuse the Create Folder dialog mapping.
 */
export function formatInlineNewFolderCommitError(
  err: unknown,
  name: string,
): string {
  if (isApiError(err) && err.status === 409) {
    return `${String(name ?? "").trim()}: ${message(EXPLORER_MSG.ACTION_CREATE_FOLDER_CONFLICT)}`;
  }
  return formatCreateFolderError(err);
}

export interface ExplorerTreeInlineNewFolderRowProps {
  /** Tree depth of the parent the row is created under. */
  depth: number;
  /** Server error (or conflict) to show under the field; row stays open. */
  error: string | null;
  /** Commit in flight — field and Enter are disabled. */
  busy: boolean;
  /** Fires on Enter with the typed name. */
  onCommit: (name: string) => void;
  /** Fires on Escape — the tree is left unchanged. */
  onCancel: () => void;
}

/**
 * Render state for one open inline row, threaded from
 * {@code ExplorerTree} through {@code ExplorerTreeNodeView} to the
 * matching parent (#5882). A single object prop keeps the recursion
 * signature stable.
 */
export interface ExplorerTreeInlineCreateState {
  /** Canonical key of the parent the row is created under. */
  parentKey: string;
  /** Server error (or conflict) to show under the field; row stays open. */
  error: string | null;
  /** Commit in flight — field and Enter are disabled. */
  busy: boolean;
  /** Fires on Enter with the typed name. */
  onCommit: (name: string) => void;
  /** Fires on Escape — the tree is left unchanged. */
  onCancel: () => void;
}

/**
 * One editable row rendered as a child of the selected tree location.
 * Enter submits the form; Escape cancels without a POST.
 */
export function ExplorerTreeInlineNewFolderRow({
  depth,
  error,
  busy,
  onCommit,
  onCancel,
}: ExplorerTreeInlineNewFolderRowProps): React.ReactElement {
  const [value, setValue] = useState("");
  const label = message(EXPLORER_MSG.TREE_INLINE_NEW_FOLDER_LABEL);
  // The error is a live region WITHOUT an owned role: role=alert (or
  // status) inside role=tree trips axe aria-required-children (only
  // treeitem/group may be owned). aria-describedby + aria-invalid tie
  // the text to the field; aria-live announces it.
  const errorId = "tree-inline-new-folder-error-text";
  return (
    // treeitem: the tree may only own treeitem/group (axe
    // aria-required-children) — a bare textbox or live region owned by
    // role=tree is a critical violation, so the editor row (input +
    // error) lives inside a peer treeitem and ownership stops here.
    // The row is excluded from type-ahead focus in ExplorerTree.
    <div
      data-testid="tree-inline-new-folder"
      role="treeitem"
      aria-label={label}
      aria-selected={false}
    >
      <div style={nodeRowStyle(false, depth)}>
        <span aria-hidden="true">▸</span>
        <form
          style={{ display: "flex", flex: 1 }}
          onSubmit={(e) => {
            e.preventDefault();
            if (!busy) {
              onCommit(value);
            }
          }}
        >
          <input
            data-testid="tree-inline-new-folder-input"
            aria-label={label}
            placeholder={label}
            autoFocus
            disabled={busy}
            value={value}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? errorId : undefined}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Escape") {
                e.preventDefault();
                e.stopPropagation();
                onCancel();
              }
            }}
            style={{ flex: 1, fontSize: "0.9rem" }}
          />
        </form>
      </div>
      {error ? (
        <div
          id={errorId}
          aria-live="polite"
          data-testid="tree-inline-new-folder-error"
          style={{ ...nodeRowStyle(false, depth), color: "#b3261e" }}
        >
          {error}
        </div>
      ) : null}
    </div>
  );
}
