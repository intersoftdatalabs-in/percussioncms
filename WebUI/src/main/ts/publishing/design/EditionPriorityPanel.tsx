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
import { EDITION_PRIORITY_FORM_DEFAULT } from "../editionRename";
import {
  buildEditionPriorityBody,
  validateEditionPriority,
} from "../editionPriority";
import { mapEditionSaveError } from "../editionSaveErrors";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface EditionPriorityPanelProps {
  edition: EditionSummary;
  /** Called only after the priority update succeeds. */
  onSaved: (priority: number) => void;
  onCancel: () => void;
}

/**
 * Set one edition's priority (1–5). Name and comment are shown and are not
 * written. Content-list order is not loaded or changed.
 */
export function EditionPriorityPanel({
  edition,
  onSaved,
  onCancel,
}: EditionPriorityPanelProps): React.ReactElement {
  const stored =
    edition.priority != null && !Number.isNaN(edition.priority)
      ? String(edition.priority)
      : String(EDITION_PRIORITY_FORM_DEFAULT);
  const [priority, setPriority] = useState(stored);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSave(): Promise<void> {
    if (!edition.editionId || saving) {
      return;
    }
    const validated = validateEditionPriority(priority);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await updateEdition(
        edition.editionId,
        buildEditionPriorityBody(validated.priority),
      );
      onSaved(validated.priority);
    } catch (e) {
      setError(mapEditionSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div data-testid="edition-priority-form">
      <h3>Edition priority</h3>
      <p>
        Name:{" "}
        <span data-testid="edition-priority-name">{edition.name ?? ""}</span>
      </p>
      <p>
        Comment:{" "}
        <span data-testid="edition-priority-comment">{edition.comment ?? ""}</span>
      </p>
      <div style={formRowStyle}>
        <label htmlFor="edition-priority-input">Priority (1–5)</label>
        <input
          id="edition-priority-input"
          type="text"
          inputMode="numeric"
          value={priority}
          onChange={(e) => setPriority(e.target.value)}
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
          data-testid="edition-priority-save"
          disabled={saving}
          onClick={() => void handleSave()}
        >
          Save priority
        </button>
        <button
          type="button"
          style={buttonStyle}
          data-testid="edition-priority-cancel"
          disabled={saving}
          onClick={onCancel}
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
