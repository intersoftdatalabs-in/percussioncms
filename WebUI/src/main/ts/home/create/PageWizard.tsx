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
  createPageAndPath,
  fetchFolderChildren,
  fetchSites,
  fetchTemplatesForSite,
  formatApiError,
} from "../../api/home/homeApi";
import type { SiteSummary, TemplateSummary } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import { errorStyle, formRowStyle } from "../home.styles";
import {
  sanitizeFileNameInput,
  titleToPageFileName,
} from "./filenameUtils";

export interface PageWizardProps {
  onBack: () => void;
}

export function PageWizard({ onBack }: PageWizardProps): React.ReactElement {
  const [sites, setSites] = useState<SiteSummary[]>([]);
  const [site, setSite] = useState("");
  const [templates, setTemplates] = useState<TemplateSummary[]>([]);
  const [templateId, setTemplateId] = useState("");
  const [folderPath, setFolderPath] = useState("");
  const [folders, setFolders] = useState<string[]>([]);
  const [title, setTitle] = useState("");
  const [fileName, setFileName] = useState("");
  const [autofill, setAutofill] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchSites()
      .then((list) => {
        setSites(list);
        if (list.length === 1) {
          setSite(list[0].name);
        }
      })
      .catch(() => setError(message(MSG.ERROR_GENERIC)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!site) {
      setTemplates([]);
      setFolders([]);
      setFolderPath("");
      setTemplateId("");
      return;
    }
    setBusy(true);
    Promise.all([
      fetchTemplatesForSite(site),
      fetchFolderChildren(`/Sites/${site}`),
    ])
      .then(([tmpls, children]) => {
        setTemplates(tmpls);
        const folderPaths = children
          .filter(
            (c) =>
              c.folder === true ||
              String(c.type ?? "").toLowerCase().includes("folder"),
          )
          .map((c) => {
            if (c.path) {
              return String(c.path);
            }
            return `/Sites/${site}/${c.name ?? ""}`;
          });
        // Always allow site root as destination
        const root = `/Sites/${site}`;
        setFolders([root, ...folderPaths.filter((p) => p !== root)]);
        setFolderPath(root);
      })
      .catch(() => setError(message(MSG.ERROR_GENERIC)))
      .finally(() => setBusy(false));
  }, [site]);

  const onTitleChange = (v: string) => {
    setTitle(v);
    if (autofill) {
      setFileName(titleToPageFileName(v));
    }
  };

  const onFileChange = (v: string) => {
    setAutofill(false);
    setFileName(sanitizeFileNameInput(v));
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!site || !templateId || !folderPath || !title.trim() || !fileName.trim()) {
      setError(message(MSG.CREATE_VALIDATION));
      return;
    }
    if (fileName.length > 255) {
      setError(message(MSG.CREATE_FILE_TOO_LONG));
      return;
    }
    setBusy(true);
    try {
      const fullPath = await createPageAndPath({
        name: fileName,
        title,
        linkTitle: title,
        templateId,
        folderPath,
      });
      window.location.href = `/cm/app/?view=editor&path=${encodeURIComponent(fullPath)}`;
    } catch (err) {
      setError(
        formatApiError(err, message(MSG.CREATE_NOT_AUTHORIZED)),
      );
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <p role="status">{message(MSG.LOADING)}</p>;
  }

  return (
    <form data-testid="page-wizard" onSubmit={onSubmit}>
      <p>
        <button type="button" onClick={onBack}>
          {message(MSG.CREATE_BACK)}
        </button>
      </p>
      <h2 style={{ fontSize: "1.1rem" }}>{message(MSG.CREATE_TYPE_PAGE)}</h2>

      {sites.length > 1 && (
        <div style={formRowStyle}>
          <label htmlFor="pw-site">{message(MSG.CREATE_SITE)}</label>
          <select
            id="pw-site"
            value={site}
            onChange={(e) => setSite(e.target.value)}
            required
          >
            <option value="">{message(MSG.CREATE_SELECT)}</option>
            {sites.map((s) => (
              <option key={s.name} value={s.name}>
                {s.name}
              </option>
            ))}
          </select>
        </div>
      )}

      <div style={formRowStyle}>
        <label htmlFor="pw-template">{message(MSG.CREATE_TEMPLATE)}</label>
        <select
          id="pw-template"
          value={templateId}
          onChange={(e) => setTemplateId(e.target.value)}
          required
          disabled={!site}
        >
          <option value="">{message(MSG.CREATE_SELECT)}</option>
          {templates.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
      </div>

      <div style={formRowStyle}>
        <label htmlFor="pw-folder">{message(MSG.CREATE_FOLDER)}</label>
        <select
          id="pw-folder"
          value={folderPath}
          onChange={(e) => setFolderPath(e.target.value)}
          required
          disabled={!site}
        >
          {folders.map((f) => (
            <option key={f} value={f}>
              {f}
            </option>
          ))}
        </select>
      </div>

      <div style={formRowStyle}>
        <label htmlFor="pw-title">{message(MSG.CREATE_TITLE)}</label>
        <input
          id="pw-title"
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          required
        />
      </div>

      <div style={formRowStyle}>
        <label htmlFor="pw-file">{message(MSG.CREATE_FILENAME)}</label>
        <input
          id="pw-file"
          value={fileName}
          onChange={(e) => onFileChange(e.target.value)}
          required
        />
      </div>

      {error && (
        <p role="alert" style={errorStyle}>
          {error}
        </p>
      )}

      <button type="submit" disabled={busy}>
        {message(MSG.CREATE_SUBMIT)}
      </button>
    </form>
  );
}
