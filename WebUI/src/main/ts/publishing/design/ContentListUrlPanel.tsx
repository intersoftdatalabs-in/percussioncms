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
import { storedItemFilterLabel } from "../contentListItemFilter";
import { mapContentListSaveError } from "../contentListSaveErrors";
import {
  buildContentListUrlBody,
  CONTENT_LIST_URL_MODERN,
  validateContentListUrl,
} from "../contentListUrl";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";
import { isLegacyContentList } from "./designLegacyTypes";

export interface ContentListUrlPanelProps {
  contentList: ContentListSummary;
  /** Called only after the legacy URL update succeeds. */
  onSaved: (url: string) => void;
  onCancel: () => void;
}

/**
 * Set the legacy URL on one legacy content list. Name, description, type, and
 * item filter are shown and are not written. A modern list does not use this
 * form. Uses {@code updateContentList}. The list shows the new URL only after
 * save succeeds.
 */
export function ContentListUrlPanel({
  contentList,
  onSaved,
  onCancel,
}: ContentListUrlPanelProps): React.ReactElement {
  const [url, setUrl] = useState(contentList.url ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSave(): Promise<void> {
    if (!contentList.contentListId || saving) {
      return;
    }
    if (!isLegacyContentList(contentList.listType)) {
      setError(CONTENT_LIST_URL_MODERN);
      return;
    }
    const validated = validateContentListUrl(url);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await updateContentList(
        contentList.contentListId,
        buildContentListUrlBody(validated.url),
      );
      onSaved(validated.url);
    } catch (e) {
      setError(mapContentListSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div data-testid="contentlist-url-form">
      <h3>Legacy content list URL</h3>
      <p>
        Name:{" "}
        <span data-testid="contentlist-url-name">{contentList.name ?? ""}</span>
      </p>
      <p>
        Description:{" "}
        <span data-testid="contentlist-url-description">
          {contentList.description ?? ""}
        </span>
      </p>
      <p>
        Type:{" "}
        <span data-testid="contentlist-url-type">
          {contentList.listType ?? ""}
        </span>
      </p>
      <p>
        Item filter:{" "}
        <span data-testid="contentlist-url-filter">
          {storedItemFilterLabel(contentList)}
        </span>
      </p>
      <div style={formRowStyle}>
        <label htmlFor="contentlist-url-input">Legacy URL</label>
        <input
          id="contentlist-url-input"
          data-testid="contentlist-url-input"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
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
          data-testid="contentlist-url-save"
          disabled={saving}
          onClick={() => void handleSave()}
        >
          Save legacy URL
        </button>
        <button
          type="button"
          style={buttonStyle}
          data-testid="contentlist-url-cancel"
          disabled={saving}
          onClick={onCancel}
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
