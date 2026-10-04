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
  copyContentList,
  type ContentListSummary,
} from "../../api/publishing/designApi";
import {
  suggestedContentListCopyName,
  validateContentListCopyName,
} from "../contentListCopy";
import { mapContentListSaveError } from "../contentListSaveErrors";
import { useDirtyForm } from "../dirtyFormContext";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

export interface ContentListCopyPanelProps {
  source: ContentListSummary;
  onCopied: (created: ContentListSummary) => Promise<void> | void;
  onCancel: () => void;
}

/**
 * Confirm a new name, then copy one content list. Cancel and a blank name do
 * not call the server. The new row is the caller's job after copy succeeds.
 */
export function ContentListCopyPanel({
  source,
  onCopied,
  onCancel,
}: ContentListCopyPanelProps): React.ReactElement {
  const [name, setName] = useState(suggestedContentListCopyName(source.name));
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  function handleCancel(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    onCancel();
  }

  async function handleCopy(): Promise<void> {
    if (!source.contentListId || saving) {
      return;
    }
    const validated = validateContentListCopyName(name);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const created = await copyContentList({
        sourceContentListId: source.contentListId,
        newName: validated.name,
      });
      setDirty(false);
      await onCopied(created);
    } catch (e) {
      setError(mapContentListSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div data-testid="contentlist-copy">
      <h3>Copy content list</h3>
      <p>Source: {source.name ?? source.contentListId}</p>
      <div style={formRowStyle}>
        <label htmlFor="contentlist-copy-name">* New name</label>
        <input
          id="contentlist-copy-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setDirty(true);
          }}
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
          data-testid="contentlist-copy-submit"
          disabled={saving}
          onClick={() => void handleCopy()}
        >
          Copy content list
        </button>
        <button
          type="button"
          style={buttonStyle}
          data-testid="contentlist-copy-cancel"
          disabled={saving}
          onClick={handleCancel}
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
