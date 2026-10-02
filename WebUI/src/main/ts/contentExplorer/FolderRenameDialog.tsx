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

import React, { useState } from "react";
import { message } from "../i18n/message";
import {
  folderRenameFieldReason,
  type FolderRenameFieldReason,
} from "./folderName";
import { EXPLORER_MSG } from "./messages";
import { formatRenameItemError } from "./renameItemErrors";

export interface FolderRenameDialogProps {
  currentName: string;
  /** Other names in the open folder (the current name is ignored). */
  takenNames?: readonly string[];
  busy?: boolean;
  onCancel: () => void;
  /** Called only after client checks pass. Rejected promises stay in the dialog. */
  onRename: (newName: string) => Promise<void>;
}

function fieldMessage(reason: FolderRenameFieldReason): string | null {
  if (reason === "blank") return message(EXPLORER_MSG.FOLDER_RENAME_BLANK);
  if (reason === "invalid") return message(EXPLORER_MSG.FOLDER_RENAME_INVALID);
  if (reason === "collision") return message(EXPLORER_MSG.FOLDER_RENAME_COLLISION);
  return null;
}

/**
 * Confirm/cancel rename for one selected folder (#5038).
 * Cancel and rejected names do not call {@link FolderRenameDialogProps.onRename}.
 */
export function FolderRenameDialog({
  currentName,
  takenNames = [],
  busy,
  onCancel,
  onRename,
}: FolderRenameDialogProps): React.ReactElement {
  const [name, setName] = useState(currentName);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const locked = busy || submitting;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="explorer-folder-rename-title"
      data-testid="explorer-folder-rename"
    >
      <form
        onSubmit={(event) => {
          event.preventDefault();
          const reason = folderRenameFieldReason(currentName, name, takenNames);
          if (reason === "unchanged") {
            onCancel();
            return;
          }
          if (reason) {
            setError(fieldMessage(reason));
            return;
          }
          setSubmitting(true);
          setError(null);
          void onRename(name.trim())
            .catch((err: unknown) => {
              setError(formatRenameItemError(err));
            })
            .finally(() => setSubmitting(false));
        }}
      >
        <h2 id="explorer-folder-rename-title">
          {message(EXPLORER_MSG.FOLDER_RENAME_TITLE)}
        </h2>
        <label htmlFor="folder-rename-name">
          {message(EXPLORER_MSG.FOLDER_RENAME_NAME)}
          <input
            id="folder-rename-name"
            data-testid="folder-rename-name"
            value={name}
            disabled={locked}
            onChange={(event) => setName(event.target.value)}
          />
        </label>
        {error ? (
          <p role="alert" data-testid="folder-rename-error">
            {error}
          </p>
        ) : null}
        <button type="submit" data-testid="folder-rename-submit" disabled={locked}>
          {message(EXPLORER_MSG.FOLDER_RENAME_SUBMIT)}
        </button>
        <button
          type="button"
          data-testid="folder-rename-cancel"
          disabled={locked}
          onClick={onCancel}
        >
          {message(EXPLORER_MSG.FOLDER_RENAME_CANCEL)}
        </button>
      </form>
    </div>
  );
}
