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

import React, { useEffect, useState } from "react";
import { isSessionRedirectError } from "../../api/client";
import {
  createPageAndPath,
  fetchAllBlogs,
  formatApiError,
} from "../../api/home/homeApi";
import type { BlogSummary } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import {
  actionButtonStyle,
  errorStyle,
  formRowStyle,
} from "../home.styles";
import {
  sanitizeFileNameInput,
  titleToBlogFileName,
} from "./filenameUtils";

export interface BlogWizardProps {
  onBack: () => void;
}

/** Classic blog post create = createPage with blog template + folder. */
export function BlogWizard({ onBack }: BlogWizardProps): React.ReactElement {
  const [blogs, setBlogs] = useState<BlogSummary[]>([]);
  const [blogKey, setBlogKey] = useState("");
  const [title, setTitle] = useState("");
  const [fileName, setFileName] = useState("");
  const [autofill, setAutofill] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAllBlogs()
      .then((all) => {
        setBlogs(all);
        if (all.length === 1) {
          setBlogKey(blogOptionKey(all[0], 0));
        }
      })
      .catch((err: unknown) => {
        if (isSessionRedirectError(err)) {
          return;
        }
        setError(formatApiError(err, message(MSG.ERROR_GENERIC)));
      })
      .finally(() => setLoading(false));
  }, []);

  const selected = blogs.find(
    (b, i) => blogOptionKey(b, i) === blogKey,
  );

  const onTitleChange = (v: string) => {
    setTitle(v);
    if (autofill) {
      setFileName(titleToBlogFileName(v));
    }
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!selected || !title.trim() || !fileName.trim()) {
      setError(message(MSG.CREATE_VALIDATION));
      return;
    }
    if (!selected.templateId || !selected.folderPath) {
      setError(message(MSG.ERROR_GENERIC));
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
        templateId: selected.templateId,
        folderPath: selected.folderPath,
      });
      window.location.href = `/cm/app/?view=editor&path=${encodeURIComponent(fullPath)}`;
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, message(MSG.CREATE_NOT_AUTHORIZED)));
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return (
      <p role="status" data-testid="blog-wizard-loading">
        {message(MSG.LOADING)}
      </p>
    );
  }
  if (blogs.length === 0) {
    return (
      <div data-testid="blog-wizard-empty">
        <button type="button" style={actionButtonStyle("ghost")} onClick={onBack}>
          {message(MSG.CREATE_BACK)}
        </button>
        <p>{message(MSG.CREATE_NO_BLOGS)}</p>
      </div>
    );
  }

  return (
    <form data-testid="blog-wizard" onSubmit={onSubmit}>
      <p>
        <button type="button" style={actionButtonStyle("ghost")} onClick={onBack}>
          {message(MSG.CREATE_BACK)}
        </button>
      </p>
      <h2 style={{ fontSize: "1.1rem" }}>{message(MSG.CREATE_TYPE_BLOG)}</h2>
      <p style={{ color: "#555", fontSize: "0.9rem" }}>
        {message(MSG.CREATE_BLOG_HINT)}
      </p>

      <div style={formRowStyle}>
        <label htmlFor="bw-blog">{message(MSG.CREATE_BLOG)}</label>
        <select
          id="bw-blog"
          data-testid="blog-wizard-blog"
          value={blogKey}
          onChange={(e) => setBlogKey(e.target.value)}
          required
          disabled={blogs.length === 1}
        >
          <option value="">{message(MSG.CREATE_SELECT)}</option>
          {blogs.map((b, i) => (
            <option key={blogOptionKey(b, i)} value={blogOptionKey(b, i)}>
              {b.site ? `${b.site} / ${b.title}` : b.title}
            </option>
          ))}
        </select>
      </div>

      <div style={formRowStyle}>
        <label htmlFor="bw-title">{message(MSG.CREATE_TITLE)}</label>
        <input
          id="bw-title"
          data-testid="blog-wizard-title"
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          required
        />
      </div>

      <div style={formRowStyle}>
        <label htmlFor="bw-file">{message(MSG.CREATE_FILENAME)}</label>
        <input
          id="bw-file"
          data-testid="blog-wizard-filename"
          value={fileName}
          onChange={(e) => {
            setAutofill(false);
            setFileName(sanitizeFileNameInput(e.target.value));
          }}
          required
        />
      </div>

      {error && (
        <p role="alert" style={errorStyle} data-testid="blog-wizard-error">
          {error}
        </p>
      )}

      <button
        type="submit"
        data-testid="blog-wizard-submit"
        style={actionButtonStyle("primary")}
        disabled={busy}
      >
        {message(MSG.CREATE_SUBMIT)}
      </button>
    </form>
  );
}

function blogOptionKey(b: BlogSummary, index: number): string {
  return `${b.site ?? ""}|${b.folderPath}|${b.templateId}|${index}`;
}
