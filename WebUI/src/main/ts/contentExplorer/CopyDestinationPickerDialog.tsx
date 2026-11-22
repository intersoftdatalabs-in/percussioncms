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

import React, { useEffect, useRef, useState } from "react";
import {
  useDialogEscape,
  useDialogFocusTrap,
} from "../architecture/useDialogEscape";
import { message } from "../i18n/message";
import { copyDestContainsNul, copyDestIsBlank } from "./copyDest";
import { EXPLORER_MSG } from "./messages";

export interface CopyDestinationPickerDialogProps {
  defaultPath: string;
  /** Server failure (for example HTTP 403/409 from a host pick). Dialog stays open. */
  serverError?: string | null;
  /** Clears a shown {@code serverError} when the author edits the destination. */
  onServerErrorClear?: () => void;
  onPick: (targetFolderPath: string) => void;
  onCancel: () => void;
}

export function CopyDestinationPickerDialog({
  defaultPath,
  serverError = null,
  onServerErrorClear,
  onPick,
  onCancel,
}: CopyDestinationPickerDialogProps): React.ReactElement {
  const [value, setValue] = useState(defaultPath);
  const [error, setError] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const serverErrorRef = useRef<HTMLParagraphElement>(null);

  useDialogEscape(true, false, onCancel);

  useDialogFocusTrap(true, rootRef, inputRef);

  // Keep the destination text selected for overwrite (was inside the trap effect).
  useEffect(() => {
    inputRef.current?.select();
  }, []);

  // A server failure (for example HTTP 409 on a host pick) names the
  // destination inside this dialog, so move keyboard focus to it (#6772).
  useEffect(() => {
    if (serverError) {
      serverErrorRef.current?.focus();
    }
  }, [serverError]);

  const trimmed = value.trim();
  const describedBy = [
    error ? "explorer-copy-dest-error" : null,
    serverError ? "explorer-copy-dest-server-error" : null,
  ]
    .filter((id): id is string => id != null)
    .join(" ");

  return (
    <div
      ref={rootRef}
      role="dialog"
      aria-modal="true"
      aria-labelledby="explorer-copy-dest-title"
      data-testid="explorer-copy-dest-picker"
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
          maxWidth: 480,
          padding: 16,
          borderRadius: 8,
          boxShadow: "0 8px 24px rgba(0,0,0,0.18)",
        }}
        onSubmit={(e) => {
          e.preventDefault();
          // U+0000 stays in the dialog and is not POSTed (#5551).
          if (copyDestContainsNul(value)) {
            setError(message(EXPLORER_MSG.COPY_DEST_NUL));
            return;
          }
          // Blank or whitespace-only dest stays in the dialog (#5553).
          if (copyDestIsBlank(value)) {
            setError(message(EXPLORER_MSG.COPY_DEST_BLANK));
            return;
          }
          setError(null);
          onPick(trimmed);
        }}
      >
        <h2 id="explorer-copy-dest-title" style={{ fontSize: 16, margin: "0 0 12px" }}>
          {message(EXPLORER_MSG.COPY_DEST_TITLE)}
        </h2>
        <label htmlFor="explorer-copy-dest-input" style={{ display: "block", fontSize: 13 }}>
          {message(EXPLORER_MSG.COPY_DEST_LABEL)}
          <input
            ref={inputRef}
            id="explorer-copy-dest-input"
            data-testid="explorer-copy-dest-input"
            value={value}
            aria-invalid={error || serverError ? true : undefined}
            aria-describedby={describedBy ? describedBy : undefined}
            onChange={(e) => {
              setValue(e.target.value);
              setError(null);
              onServerErrorClear?.();
            }}
            style={{ display: "block", width: "100%", marginTop: 6, padding: 6 }}
          />
        </label>
        {serverError ? (
          <p
            id="explorer-copy-dest-server-error"
            ref={serverErrorRef}
            role="alert"
            tabIndex={-1}
            data-testid="explorer-copy-dest-server-error"
            style={{ color: "#b91c1c", fontSize: 13, margin: "8px 0 0" }}
          >
            {serverError}
          </p>
        ) : null}
        {error ? (
          <p
            id="explorer-copy-dest-error"
            role="alert"
            data-testid="explorer-copy-dest-error"
            style={{ color: "#b91c1c", fontSize: 13, margin: "8px 0 0" }}
          >
            {error}
          </p>
        ) : null}
        <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 16 }}>
          <button type="button" data-testid="explorer-copy-dest-cancel" onClick={onCancel}>
            {message(EXPLORER_MSG.CONFIRM_CANCEL)}
          </button>
          <button type="submit" data-testid="explorer-copy-dest-ok">
            {message(EXPLORER_MSG.CONFIRM_OK)}
          </button>
        </div>
      </form>
    </div>
  );
}
