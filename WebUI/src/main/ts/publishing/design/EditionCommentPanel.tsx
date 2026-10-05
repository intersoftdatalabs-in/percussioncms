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
import { updateEdition, type EditionSummary } from "../../api/publishing/designApi";
import {
  buildEditionCommentBody,
  normalizeEditionComment,
} from "../editionComment";
import { mapEditionSaveError } from "../editionSaveErrors";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface EditionCommentPanelProps {
  edition: EditionSummary;
  /** Called only after the comment update succeeds. */
  onSaved: (comment: string) => void;
  onCancel: () => void;
}

/**
 * Set or clear one edition's comment. Name and priority are shown and are not
 * written. Content-list order is not loaded or changed. Uses {@code updateEdition}.
 */
export function EditionCommentPanel({
  edition,
  onSaved,
  onCancel,
}: EditionCommentPanelProps): React.ReactElement {
  const [comment, setComment] = useState(edition.comment ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const priorityLabel =
    edition.priority != null && !Number.isNaN(edition.priority)
      ? String(edition.priority)
      : "";

  async function handleSave(): Promise<void> {
    if (!edition.editionId || saving) {
      return;
    }
    const next = normalizeEditionComment(comment);
    setSaving(true);
    setError(null);
    try {
      await updateEdition(edition.editionId, buildEditionCommentBody(next));
      onSaved(next);
    } catch (e) {
      setError(mapEditionSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div data-testid="edition-comment-form">
      <h3>Edition comment</h3>
      <p>
        Name:{" "}
        <span data-testid="edition-comment-name">{edition.name ?? ""}</span>
      </p>
      <p>
        Priority:{" "}
        <span data-testid="edition-comment-priority">{priorityLabel}</span>
      </p>
      <div style={formRowStyle}>
        <label htmlFor="edition-comment-input">Comment</label>
        <input
          id="edition-comment-input"
          data-testid="edition-comment-input"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
        />
      </div>
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        <button
          type="button"
          style={primaryButtonStyle}
          data-testid="edition-comment-save"
          disabled={saving}
          onClick={() => void handleSave()}
        >
          Save comment
        </button>
        <button
          type="button"
          style={buttonStyle}
          data-testid="edition-comment-cancel"
          disabled={saving}
          onClick={onCancel}
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
