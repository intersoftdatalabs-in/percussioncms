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

import React, { useRef, useState } from "react";
import {
  useDialogEscape,
  useDialogFocusTrap,
} from "../architecture/useDialogEscape";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

export interface CheckinCommentDialogProps {
  /** Confirm with the raw comment. Empty is a check-in with no comment. */
  onConfirm: (comment: string) => void;
  /** Cancel does not check in and does not send the comment. */
  onCancel: () => void;
}

/**
 * Optional revision comment before Explorer checks in one page or asset (#5199).
 * The shell calls the editor check-in only after confirm.
 */
export function CheckinCommentDialog({
  onConfirm,
  onCancel,
}: CheckinCommentDialogProps): React.ReactElement {
  const [comment, setComment] = useState("");
  const rootRef = useRef<HTMLDivElement>(null);
  useDialogEscape(true, false, onCancel);
  useDialogFocusTrap(true, rootRef);

  return (
    <div
      ref={rootRef}
      role="dialog"
      aria-modal="true"
      aria-labelledby="explorer-checkin-comment-title"
      aria-describedby="explorer-checkin-comment-hint"
      data-testid="explorer-checkin-comment"
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
          minWidth: 320,
          maxWidth: 480,
          padding: 16,
          borderRadius: 8,
          boxShadow: "0 8px 24px rgba(0,0,0,0.18)",
        }}
        onSubmit={(event) => {
          event.preventDefault();
          onConfirm(comment);
        }}
      >
        <h2
          id="explorer-checkin-comment-title"
          style={{ fontSize: 16, margin: "0 0 8px" }}
        >
          {message(EXPLORER_MSG.CHECKIN_COMMENT_TITLE)}
        </h2>
        <p id="explorer-checkin-comment-hint" style={{ margin: "0 0 12px", fontSize: 13 }}>
          {message(EXPLORER_MSG.CHECKIN_COMMENT_HINT)}
        </p>
        <label
          htmlFor="explorer-checkin-comment-input"
          style={{ display: "block", fontSize: 13, marginBottom: 12 }}
        >
          {message(EXPLORER_MSG.CHECKIN_COMMENT_LABEL)}
          <textarea
            id="explorer-checkin-comment-input"
            data-testid="explorer-checkin-comment-input"
            value={comment}
            rows={3}
            style={{ display: "block", width: "100%", marginTop: 4, boxSizing: "border-box" }}
            onChange={(event) => setComment(event.target.value)}
          />
        </label>
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button
            type="button"
            data-testid="explorer-checkin-cancel"
            onClick={onCancel}
          >
            {message(EXPLORER_MSG.CHECKIN_CANCEL)}
          </button>
          <button type="submit" data-testid="explorer-checkin-confirm">
            {message(EXPLORER_MSG.CHECKIN_CONFIRM)}
          </button>
        </div>
      </form>
    </div>
  );
}
