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

import React, { useEffect, useRef } from "react";
import { useDialogEscape } from "../architecture/useDialogEscape";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export interface RecycleConfirmDialogProps {
  onConfirm: () => void;
  onCancel: () => void;
}

/** One confirm for Content → Recycle selected (#4857). */
export function RecycleConfirmDialog({
  onConfirm,
  onCancel,
}: RecycleConfirmDialogProps): React.ReactElement {
  const rootRef = useRef<HTMLDivElement>(null);
  const okRef = useRef<HTMLButtonElement>(null);

  useDialogEscape(true, false, onCancel);

  useEffect(() => {
    const root = rootRef.current;
    okRef.current?.focus();
    if (!root) {
      return;
    }
    const focusables = (): HTMLElement[] =>
      Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE));
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key !== "Tab") {
        return;
      }
      const list = focusables();
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
      aria-labelledby="explorer-recycle-confirm-title"
      data-testid="explorer-recycle-confirm"
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
          id="explorer-recycle-confirm-title"
          style={{ fontSize: 16, margin: "0 0 12px" }}
        >
          {message(EXPLORER_MSG.CONFIRM_DELETE_TITLE)}
        </h2>
        <p style={{ margin: "0 0 16px", fontSize: 14 }}>
          {message(EXPLORER_MSG.MULTI_RECYCLE_CONFIRM)}
        </p>
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button
            type="button"
            data-testid="explorer-recycle-cancel"
            onClick={onCancel}
          >
            {message(EXPLORER_MSG.CONFIRM_CANCEL)}
          </button>
          <button type="submit" ref={okRef} data-testid="explorer-recycle-ok">
            {message(EXPLORER_MSG.CONFIRM_OK)}
          </button>
        </div>
      </form>
    </div>
  );
}
