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
  buildContentListGeneratorBody,
  validateContentListGenerator,
} from "../contentListGenerator";
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

export interface ContentListGeneratorPanelProps {
  contentList: ContentListSummary;
  /** Called only after the generator update succeeds. */
  onSaved: (generator: string) => void;
  onCancel: () => void;
}

/**
 * Set the generator on one modern content list. Name, description, type, and
 * item filter are shown and are not written. A legacy list URL is not part of
 * this form. Uses {@code updateContentList}.
 */
export function ContentListGeneratorPanel({
  contentList,
  onSaved,
  onCancel,
}: ContentListGeneratorPanelProps): React.ReactElement {
  const [generator, setGenerator] = useState(contentList.generator ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSave(): Promise<void> {
    if (!contentList.contentListId || saving) {
      return;
    }
    if (isLegacyContentList(contentList.listType)) {
      setError("A legacy content list does not use a generator");
      return;
    }
    const validated = validateContentListGenerator(generator);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await updateContentList(
        contentList.contentListId,
        buildContentListGeneratorBody(validated.generator),
      );
      onSaved(validated.generator);
    } catch (e) {
      setError(mapContentListSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div data-testid="contentlist-generator-form">
      <h3>Content list generator</h3>
      <p>
        Name:{" "}
        <span data-testid="contentlist-generator-name">
          {contentList.name ?? ""}
        </span>
      </p>
      <p>
        Description:{" "}
        <span data-testid="contentlist-generator-description">
          {contentList.description ?? ""}
        </span>
      </p>
      <p>
        Type:{" "}
        <span data-testid="contentlist-generator-type">
          {contentList.listType ?? ""}
        </span>
      </p>
      <p>
        Item filter:{" "}
        <span data-testid="contentlist-generator-filter">
          {storedItemFilterLabel(contentList)}
        </span>
      </p>
      <div style={formRowStyle}>
        <label htmlFor="contentlist-generator-input">Generator</label>
        <input
          id="contentlist-generator-input"
          data-testid="contentlist-generator-input"
          value={generator}
          onChange={(e) => setGenerator(e.target.value)}
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
          data-testid="contentlist-generator-save"
          disabled={saving}
          onClick={() => void handleSave()}
        >
          Save generator
        </button>
        <button
          type="button"
          style={buttonStyle}
          data-testid="contentlist-generator-cancel"
          disabled={saving}
          onClick={onCancel}
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
