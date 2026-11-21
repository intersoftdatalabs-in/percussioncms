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
import type { PSPathItem } from "../api/contentExplorer/types";
import { useDialogEscape } from "../architecture/useDialogEscape";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export interface TakedownConfirmDialogProps {
  /** Eligible (non-folder) rows the confirm covers. Single-item uses one row. */
  items: readonly PSPathItem[];
  /** Linked page paths for the confirm list (already capped at ten). */
  linkedPaths: readonly string[];
  onConfirm: () => void;
  onCancel: () => void;
}

function itemLabel(item: PSPathItem): string {
  const name = (item.name ?? "").trim();
  if (name) {
    return name;
  }
  const path = (item.path ?? "").trim();
  if (path) {
    return path;
  }
  return (item.id ?? "").trim() || "item";
}

/**
 * Accessible Take Down confirm (#6996, parent #4530). Replaces the native
 * {@code window.confirm} for single-item and multi-select takedown so the
 * flow is keyboard-trappable with managed focus: {@code alertdialog} chrome,
 * Tab cycles inside the dialog, {@code Escape} cancels, and the shell
 * returns focus to the invoking toolbar control on close.
 *
 * <p>Initial focus lands on Cancel (not the destructive confirm) so an
 * accidental Enter does not unpublish. Multi-select lists every eligible
 * row; linked pages (single-item only) render under the linked heading.</p>
 */
export function TakedownConfirmDialog({
  items,
  linkedPaths,
  onConfirm,
  onCancel,
}: TakedownConfirmDialogProps): React.ReactElement {
  const rootRef = useRef<HTMLDivElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);
  const multi = items.length > 1;

  useDialogEscape(true, false, onCancel);

  useEffect(() => {
    const root = rootRef.current;
    cancelRef.current?.focus();
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

  const body = multi
    ? message(EXPLORER_MSG.CONFIRM_TAKEDOWN_MULTI)
        .split("{count}")
        .join(String(items.length))
    : message(EXPLORER_MSG.CONFIRM_TAKEDOWN);

  return (
    <div
      ref={rootRef}
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="explorer-takedown-confirm-title"
      aria-describedby="explorer-takedown-confirm-body"
      data-testid="explorer-takedown-confirm"
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
          id="explorer-takedown-confirm-title"
          style={{ fontSize: 16, margin: "0 0 12px" }}
        >
          {message(EXPLORER_MSG.TAKEDOWN_DIALOG_TITLE)}
        </h2>
        <p
          id="explorer-takedown-confirm-body"
          style={{ margin: "0 0 16px", fontSize: 14 }}
        >
          {body}
        </p>
        {multi ? (
          <ul
            data-testid="explorer-takedown-confirm-items"
            aria-label={message(EXPLORER_MSG.TAKEDOWN_DIALOG_TITLE)}
            style={{
              margin: "0 0 16px",
              paddingLeft: 20,
              fontSize: 13,
              maxHeight: 160,
              overflowY: "auto",
            }}
          >
            {items.map((item) => (
              <li key={(item.id ?? "").trim() || itemLabel(item)}>
                {itemLabel(item)}
              </li>
            ))}
          </ul>
        ) : null}
        {linkedPaths.length > 0 ? (
          <div style={{ margin: "0 0 16px", fontSize: 13 }}>
            <p
              data-testid="explorer-takedown-confirm-linked-heading"
              style={{ margin: "0 0 4px", fontWeight: 600 }}
            >
              {message(EXPLORER_MSG.CONFIRM_TAKEDOWN_LINKED)}
            </p>
            <ul
              data-testid="explorer-takedown-confirm-linked"
              style={{ margin: 0, paddingLeft: 20 }}
            >
              {linkedPaths.map((path) => (
                <li key={path}>{path}</li>
              ))}
            </ul>
          </div>
        ) : null}
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button
            type="button"
            ref={cancelRef}
            data-testid="explorer-takedown-cancel"
            onClick={onCancel}
          >
            {message(EXPLORER_MSG.CONFIRM_CANCEL)}
          </button>
          <button type="submit" data-testid="explorer-takedown-confirm-ok">
            {message(EXPLORER_MSG.TAKEDOWN_DIALOG_CONFIRM)}
          </button>
        </div>
      </form>
    </div>
  );
}
