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
import {
  updateContentList,
  type ContentListSummary,
} from "../../api/publishing/designApi";
import {
  buildContentListDescriptionBody,
  validateContentListDescription,
} from "../contentListDescription";
import { storedItemFilterLabel } from "../contentListItemFilter";
import { mapContentListSaveError } from "../contentListSaveErrors";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";
import { isLegacyContentList } from "./designLegacyTypes";

export interface ContentListDescriptionPanelProps {
  contentList: ContentListSummary;
  /** Called only after the description update succeeds. */
  onSaved: (description: string) => void;
  onCancel: () => void;
}

/**
 * Set or clear one content list's description. Name, type, generator or legacy
 * URL, and item filter are shown and are not written. Uses
 * {@code updateContentList}.
 */
export function ContentListDescriptionPanel({
  contentList,
  onSaved,
  onCancel,
}: ContentListDescriptionPanelProps): React.ReactElement {
  const [description, setDescription] = useState(contentList.description ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const legacy = isLegacyContentList(contentList.listType);
  const sourceLabel = legacy ? "Legacy URL" : "Generator";
  const sourceValue = legacy
    ? (contentList.url ?? "")
    : (contentList.generator ?? "");

  async function handleSave(): Promise<void> {
    if (!contentList.contentListId || saving) {
      return;
    }
    const validated = validateContentListDescription(description);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await updateContentList(
        contentList.contentListId,
        buildContentListDescriptionBody(validated.description),
      );
      onSaved(validated.description);
    } catch (e) {
      setError(mapContentListSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div data-testid="contentlist-description-form">
      <h3>Content list description</h3>
      <p>
        Name:{" "}
        <span data-testid="contentlist-description-name">
          {contentList.name ?? ""}
        </span>
      </p>
      <p>
        Type:{" "}
        <span data-testid="contentlist-description-type">
          {contentList.listType ?? ""}
        </span>
      </p>
      <p>
        {sourceLabel}:{" "}
        <span data-testid="contentlist-description-source">{sourceValue}</span>
      </p>
      <p>
        Item filter:{" "}
        <span data-testid="contentlist-description-filter">
          {storedItemFilterLabel(contentList)}
        </span>
      </p>
      <div style={formRowStyle}>
        <label htmlFor="contentlist-description-input">Description</label>
        <input
          id="contentlist-description-input"
          data-testid="contentlist-description-input"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
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
          data-testid="contentlist-description-save"
          disabled={saving}
          onClick={() => void handleSave()}
        >
          Save description
        </button>
        <button
          type="button"
          style={buttonStyle}
          data-testid="contentlist-description-cancel"
          disabled={saving}
          onClick={onCancel}
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
