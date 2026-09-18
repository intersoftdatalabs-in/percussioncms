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
import { ItemPublishingHistoryPanel } from "../publishing/components/ItemPublishingHistoryPanel";
import { EXPLORER_MSG } from "./messages";

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export interface PublishingHistoryDialogProps {
  itemId: string;
  onClose: () => void;
}

/**
 * Explorer host for {@link ItemPublishingHistoryPanel} (PublishingShell
 * #4536). Classic Finder used {@code PercPublishingHistoryDialog}.
 */
export function PublishingHistoryDialog({
  itemId,
  onClose,
}: PublishingHistoryDialogProps): React.ReactElement {
  const rootRef = useRef<HTMLDivElement>(null);
  const closeRef = useRef<HTMLButtonElement>(null);

  useDialogEscape(true, false, onClose);

  useEffect(() => {
    const root = rootRef.current;
    closeRef.current?.focus();
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
      aria-label={message(EXPLORER_MSG.PUBLISHING_HISTORY_TITLE)}
      data-testid="explorer-publishing-history-dialog"
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(15,23,42,0.45)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 40,
        padding: 24,
      }}
    >
      <div
        style={{
          background: "#fff",
          color: "#0f172a",
          minWidth: 320,
          maxWidth: 880,
          width: "100%",
          maxHeight: "90vh",
          overflow: "auto",
          padding: 16,
          borderRadius: 8,
          boxShadow: "0 8px 24px rgba(0,0,0,0.18)",
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            marginBottom: 8,
          }}
        >
          <button
            ref={closeRef}
            type="button"
            data-testid="explorer-publishing-history-close"
            onClick={onClose}
          >
            {message(EXPLORER_MSG.CONFIRM_CANCEL)}
          </button>
        </div>
        <ItemPublishingHistoryPanel
          itemId={itemId}
          currentSection="status"
        />
      </div>
    </div>
  );
}
