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

import React, { useEffect, useRef, useState } from "react";
import { useDialogEscape } from "../architecture/useDialogEscape";
import type { FolderWorkflowChoice } from "../api/contentExplorer/folderWorkflowApi";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export interface SetFolderWorkflowDialogProps {
  choices: FolderWorkflowChoice[];
  currentId: string;
  busy: boolean;
  error: string;
  /** When true, one save writes every checked folder (#5179). */
  multi?: boolean;
  onSave: (workflowId: string) => void;
  onCancel: () => void;
}

export function SetFolderWorkflowDialog({
  choices,
  currentId,
  busy,
  error,
  multi = false,
  onSave,
  onCancel,
}: SetFolderWorkflowDialogProps): React.ReactElement {
  const initial =
    choices.find((row) => row.id !== currentId)?.id ?? choices[0]?.id ?? "";
  const [value, setValue] = useState(initial);
  const rootRef = useRef<HTMLDivElement>(null);
  const selectRef = useRef<HTMLSelectElement>(null);

  useDialogEscape(true, busy, onCancel);

  useEffect(() => {
    const root = rootRef.current;
    selectRef.current?.focus();
    if (!root) {
      return;
    }
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key !== "Tab") {
        return;
      }
      const list = Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE));
      if (list.length === 0) {
        return;
      }
      const first = list[0];
      const last = list[list.length - 1];
      if (ev.shiftKey && document.activeElement === first) {
        ev.preventDefault();
        last.focus();
      } else if (!ev.shiftKey && document.activeElement === last) {
        ev.preventDefault();
        first.focus();
      }
    };
    root.addEventListener("keydown", onKey);
    return () => root.removeEventListener("keydown", onKey);
  }, []);

  return (
    <div
      ref={rootRef}
      role="dialog"
      aria-modal="true"
      aria-labelledby="explorer-set-folder-workflow-title"
      data-testid="explorer-set-folder-workflow-dialog"
      data-current-workflow-id={currentId}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(15,23,42,0.45)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 40,
      }}
    >
      <form
        style={{
          background: "#fff",
          color: "#0f172a",
          minWidth: 280,
          maxWidth: 420,
          padding: 16,
          borderRadius: 8,
          boxShadow: "0 8px 24px rgba(0,0,0,0.18)",
        }}
        onSubmit={(e) => {
          e.preventDefault();
          if (!busy && value) {
            onSave(value);
          }
        }}
      >
        <h2 id="explorer-set-folder-workflow-title" style={{ fontSize: 16, margin: "0 0 12px" }}>
          {message(EXPLORER_MSG.SET_FOLDER_WORKFLOW_TITLE)}
        </h2>
        {multi ? (
          <p
            data-testid="explorer-set-folder-workflow-multi"
            style={{ fontSize: 13, margin: "0 0 12px" }}
          >
            {message(EXPLORER_MSG.SET_FOLDER_WORKFLOW_MULTI_NOTE)}
          </p>
        ) : null}
        <label style={{ display: "block", fontSize: 13 }}>
          {message(EXPLORER_MSG.SET_FOLDER_WORKFLOW_LABEL)}
          <select
            ref={selectRef}
            data-testid="explorer-set-folder-workflow-select"
            value={value}
            disabled={busy}
            onChange={(e) => setValue(e.target.value)}
            style={{ display: "block", width: "100%", marginTop: 6, padding: 6 }}
          >
            {choices.map((row) => (
              <option key={row.id} value={row.id}>
                {row.name}
                {row.id === currentId
                  ? ` (${message(EXPLORER_MSG.SET_FOLDER_WORKFLOW_CURRENT)})`
                  : ""}
              </option>
            ))}
          </select>
        </label>
        {error ? (
          <p data-testid="explorer-set-folder-workflow-dialog-error" role="alert">
            {error}
          </p>
        ) : null}
        <div style={{ display: "flex", gap: 8, marginTop: 16, justifyContent: "flex-end" }}>
          <button
            type="button"
            data-testid="explorer-set-folder-workflow-cancel"
            disabled={busy}
            onClick={onCancel}
          >
            {message(EXPLORER_MSG.SET_FOLDER_WORKFLOW_CANCEL)}
          </button>
          <button
            type="submit"
            data-testid="explorer-set-folder-workflow-save"
            disabled={busy || !value}
          >
            {message(EXPLORER_MSG.SET_FOLDER_WORKFLOW_SAVE)}
          </button>
        </div>
      </form>
    </div>
  );
}
