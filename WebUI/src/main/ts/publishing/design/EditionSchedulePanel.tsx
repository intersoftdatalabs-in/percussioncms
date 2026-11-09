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
import { validateScheduleDateRange } from "../../contentExplorer/itemScheduleDates";
import { message } from "../../i18n/message";
import {
  buildEditionScheduleBody,
  editionScheduleInputDefaults,
  editionScheduleServerDates,
  type EditionScheduleDates,
} from "../editionSchedule";
import { mapEditionSaveError } from "../editionSaveErrors";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface EditionSchedulePanelProps {
  edition: EditionSummary;
  /** Called only after the schedule update succeeds. */
  onSaved: (dates: EditionScheduleDates) => void;
  onCancel: () => void;
}

/**
 * Set or clear one edition's embargo/expiry schedule. Name, priority, and
 * comment are shown and are not written. Content-list order is not loaded
 * or changed. Uses a schedule-only {@code updateEdition} body.
 */
export function EditionSchedulePanel({
  edition,
  onSaved,
  onCancel,
}: EditionSchedulePanelProps): React.ReactElement {
  const defaults = editionScheduleInputDefaults(edition);
  const [startLocal, setStartLocal] = useState(defaults.startLocal);
  const [endLocal, setEndLocal] = useState(defaults.endLocal);
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
    const dates = editionScheduleServerDates(startLocal, endLocal);
    const rangeError = validateScheduleDateRange(
      dates.startDate,
      dates.endDate,
    );
    if (rangeError) {
      setError(message(rangeError));
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await updateEdition(
        edition.editionId,
        buildEditionScheduleBody(dates),
      );
      onSaved(dates);
    } catch (e) {
      setError(mapEditionSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div data-testid="edition-schedule-form">
      <h3>Edition schedule</h3>
      <p>
        Name:{" "}
        <span data-testid="edition-schedule-name">{edition.name ?? ""}</span>
      </p>
      <p>
        Priority:{" "}
        <span data-testid="edition-schedule-priority">{priorityLabel}</span>
      </p>
      <p>
        Comment:{" "}
        <span data-testid="edition-schedule-comment">
          {edition.comment ?? ""}
        </span>
      </p>
      <div style={formRowStyle}>
        <label htmlFor="edition-schedule-start-input">
          Embargo (publish from)
        </label>
        <input
          id="edition-schedule-start-input"
          type="datetime-local"
          data-testid="edition-schedule-start-input"
          value={startLocal}
          onChange={(e) => setStartLocal(e.target.value)}
        />
      </div>
      <div style={formRowStyle}>
        <label htmlFor="edition-schedule-end-input">Expiry (until)</label>
        <input
          id="edition-schedule-end-input"
          type="datetime-local"
          data-testid="edition-schedule-end-input"
          value={endLocal}
          onChange={(e) => setEndLocal(e.target.value)}
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
          style={buttonStyle}
          data-testid="edition-schedule-clear"
          disabled={saving}
          onClick={() => {
            setStartLocal("");
            setEndLocal("");
            setError(null);
          }}
        >
          Clear
        </button>
        <button
          type="button"
          style={primaryButtonStyle}
          data-testid="edition-schedule-save"
          disabled={saving}
          onClick={() => void handleSave()}
        >
          Save schedule
        </button>
        <button
          type="button"
          style={buttonStyle}
          data-testid="edition-schedule-cancel"
          disabled={saving}
          onClick={onCancel}
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
