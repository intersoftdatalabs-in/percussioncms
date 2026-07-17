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

import React, { useState } from "react";
import { createPage } from "../../api/home/homeApi";
import { message, MSG } from "../../i18n/message";
import { errorStyle, formRowStyle } from "../home.styles";

export function CreateSection(): React.ReactElement {
  const [name, setName] = useState("");
  const [title, setTitle] = useState("");
  const [folderPath, setFolderPath] = useState("/Sites/");
  const [templateId, setTemplateId] = useState("");
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setStatus(null);
    createPage({
      name,
      title: title || name,
      linkTitle: title || name,
      templateId,
      folderPath,
    })
      .then(() => {
        setStatus(message(MSG.CREATE_PAGE));
      })
      .catch(() => setError(message(MSG.ERROR_GENERIC)))
      .finally(() => setSaving(false));
  };

  return (
    <div>
      <p>{message(MSG.CREATE_HINT)}</p>
      <form onSubmit={onSubmit}>
        <div style={formRowStyle}>
          <label htmlFor="create-page-name">{message(MSG.CREATE_PAGE)} name</label>
          <input
            id="create-page-name"
            required
            value={name}
            onChange={(ev) => setName(ev.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="create-page-title">Title</label>
          <input
            id="create-page-title"
            value={title}
            onChange={(ev) => setTitle(ev.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="create-folder-path">Folder path</label>
          <input
            id="create-folder-path"
            required
            value={folderPath}
            onChange={(ev) => setFolderPath(ev.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="create-template-id">Template id</label>
          <input
            id="create-template-id"
            required
            value={templateId}
            onChange={(ev) => setTemplateId(ev.target.value)}
          />
        </div>
        <button type="submit" disabled={saving}>
          {message(MSG.CREATE_PAGE)}
        </button>
      </form>
      {error && (
        <p role="alert" style={errorStyle}>
          {error}
        </p>
      )}
      {status && <p role="status">{status}</p>}
    </div>
  );
}
