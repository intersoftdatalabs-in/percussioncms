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

import React, { useRef } from "react";
import {
  useDialogEscape,
  useDialogFocusTrap,
} from "../architecture/useDialogEscape";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

export interface RestoreConfirmDialogProps {
  onConfirm: () => void;
  onCancel: () => void;
  /**
   * Single-item restore names the recycled row so the operator can verify
   * the round-trip target before confirming (#6470). Absent keeps the
   * multi-select confirm text.
   */
  itemName?: string;
}

/** One confirm for Content → Restore selected (#4884). */
export function RestoreConfirmDialog({
  onConfirm,
  onCancel,
  itemName,
}: RestoreConfirmDialogProps): React.ReactElement {
  const rootRef = useRef<HTMLDivElement>(null);
  const okRef = useRef<HTMLButtonElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);

  useDialogEscape(true, false, onCancel);

  // Bulk confirm: initial focus lands on Cancel, not Restore, so an
  // accidental Enter cannot confirm (#7003 contract, #7037).
  useDialogFocusTrap(true, rootRef, cancelRef);

  return (
    <div
      ref={rootRef}
      role="dialog"
      aria-modal="true"
      aria-labelledby="explorer-restore-confirm-title"
      data-testid="explorer-restore-confirm"
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
          onConfirm();
        }}
      >
        <h2
          id="explorer-restore-confirm-title"
          style={{ fontSize: 16, margin: "0 0 12px" }}
        >
          {message(EXPLORER_MSG.ACTION_RESTORE)}
        </h2>
        <p style={{ margin: "0 0 16px", fontSize: 14 }}>
          {itemName
            ? `${message(EXPLORER_MSG.CONFIRM_RESTORE_SINGLE)} ${itemName}`
            : message(EXPLORER_MSG.MULTI_RESTORE_CONFIRM)}
        </p>
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button
            type="button"
            ref={cancelRef}
            data-testid="explorer-restore-cancel"
            onClick={onCancel}
          >
            {message(EXPLORER_MSG.CONFIRM_CANCEL)}
          </button>
          <button type="submit" ref={okRef} data-testid="explorer-restore-ok">
            {message(EXPLORER_MSG.CONFIRM_OK)}
          </button>
        </div>
      </form>
    </div>
  );
}
