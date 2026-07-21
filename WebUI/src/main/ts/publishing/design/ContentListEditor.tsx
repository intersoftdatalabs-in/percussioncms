/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import React, { useEffect, useState } from "react";
import {
  createContentList,
  deleteContentList,
  updateContentList,
  type ContentListSummary,
} from "../../api/publishing/designApi";
import { message, MSG } from "../../i18n/message";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";
import { isLegacyContentList } from "./designLegacyTypes";

export interface ContentListEditorProps {
  contentList: ContentListSummary | null;
  onSaved: () => void;
  onCancel: () => void;
}

export function ContentListEditor({
  contentList,
  onSaved,
  onCancel,
}: ContentListEditorProps): React.ReactElement {
  const [name, setName] = useState(contentList?.name ?? "");
  const [description, setDescription] = useState(contentList?.description ?? "");
  const [generator, setGenerator] = useState(contentList?.generator ?? "");
  const [url, setUrl] = useState(contentList?.url ?? "");
  const [listType, setListType] = useState(contentList?.listType ?? "modern");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setName(contentList?.name ?? "");
    setDescription(contentList?.description ?? "");
    setGenerator(contentList?.generator ?? "");
    setUrl(contentList?.url ?? "");
    setListType(contentList?.listType ?? "modern");
  }, [contentList]);

  const legacy = isLegacyContentList(listType);

  async function handleSave(): Promise<void> {
    if (!name.trim()) {
      setError("Name is required");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const body: ContentListSummary = {
        name: name.trim(),
        description,
        generator: legacy ? undefined : generator,
        url: legacy ? url : undefined,
        listType,
      };
      if (contentList?.contentListId) {
        await updateContentList(contentList.contentListId, body);
      } else {
        await createContentList(body);
      }
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(): Promise<void> {
    if (!contentList?.contentListId) {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    setSaving(true);
    try {
      await deleteContentList(contentList.contentListId);
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div data-testid="contentlist-editor">
      <h3>
        {contentList?.contentListId ? "Edit content list" : "Create content list"}
      </h3>
      <div style={formRowStyle}>
        <label htmlFor="cl-name">* Name</label>
        <input
          id="cl-name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          disabled={Boolean(contentList?.contentListId)}
        />
      </div>
      <div style={formRowStyle}>
        <label htmlFor="cl-desc">Description</label>
        <input
          id="cl-desc"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      <div style={formRowStyle}>
        <label htmlFor="cl-type">Type</label>
        <select
          id="cl-type"
          value={listType}
          onChange={(e) => setListType(e.target.value)}
          disabled={Boolean(contentList?.contentListId)}
        >
          <option value="modern">Modern</option>
          <option value="legacy">Legacy</option>
        </select>
      </div>
      {legacy ? (
        <div style={formRowStyle}>
          <label htmlFor="cl-url">Legacy URL / resource</label>
          <input
            id="cl-url"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
          />
        </div>
      ) : (
        <div style={formRowStyle}>
          <label htmlFor="cl-gen">Generator</label>
          <input
            id="cl-gen"
            value={generator}
            onChange={(e) => setGenerator(e.target.value)}
          />
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        <button
          type="button"
          style={primaryButtonStyle}
          disabled={saving}
          onClick={() => void handleSave()}
        >
          {message(MSG.PUBLISH_SAVE)}
        </button>
        <button type="button" style={buttonStyle} onClick={onCancel}>
          {message(MSG.PUBLISH_BACK)}
        </button>
        {contentList?.contentListId && (
          <button
            type="button"
            style={buttonStyle}
            disabled={saving}
            onClick={() => void handleDelete()}
          >
            Delete
          </button>
        )}
      </div>
    </div>
  );
}
