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

/**
 * Create regular section dialog for Architecture (#3096).
 * Section-link / external-link / convert-folder are Slice E.
 */

import React, { useEffect, useState } from "react";
import { fetchTemplatesForSite } from "../api/home/homeApi";
import type { TemplateSummary } from "../api/home/types";
import { formatApiError, isSessionRedirectError } from "../api/client";
import {
  sanitizeFileNameInput,
  titleToPageFileName,
} from "../home/create/filenameUtils";
import { catalogColors } from "../developer/catalogStyles";
import {
  validateSectionFolderName,
  validateSectionTitle,
} from "../api/architecture/sectionMutations";
import { ARCH_MSG } from "./messages";
import { useDialogEscape } from "./useDialogEscape";

export interface CreateSectionDialogProps {
  open: boolean;
  siteName: string;
  parentTitle: string;
  busy: boolean;
  onCancel: () => void;
  onSubmit: (input: {
    title: string;
    urlName: string;
    templateId: string;
  }) => void;
}

const overlayStyle: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  background: "rgba(15, 23, 42, 0.45)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  zIndex: 1000,
  padding: 16,
};

const panelStyle: React.CSSProperties = {
  background: "#fff",
  borderRadius: 8,
  border: `1px solid ${catalogColors.headerBorder}`,
  maxWidth: 440,
  width: "100%",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
};

const fieldStyle: React.CSSProperties = {
  display: "block",
  width: "100%",
  marginTop: 4,
  marginBottom: 12,
  padding: "0.4rem 0.5rem",
  fontSize: "0.95rem",
  boxSizing: "border-box",
};

export function CreateSectionDialog({
  open,
  siteName,
  parentTitle,
  busy,
  onCancel,
  onSubmit,
}: CreateSectionDialogProps): React.ReactElement | null {
  const [title, setTitle] = useState("");
  const [urlName, setUrlName] = useState("");
  /** When true, stop auto-mirroring title → URL so manual URL edits stick. */
  const [urlNameTouched, setUrlNameTouched] = useState(false);
  const [templateId, setTemplateId] = useState("");
  const [templates, setTemplates] = useState<TemplateSummary[]>([]);
  const [templatesLoading, setTemplatesLoading] = useState(false);
  const [templatesError, setTemplatesError] = useState<string | null>(null);
  const [localError, setLocalError] = useState<string | null>(null);

  useDialogEscape(open, busy, onCancel);

  useEffect(() => {
    if (!open) {
      return;
    }
    setTitle("");
    setUrlName("");
    setUrlNameTouched(false);
    setTemplateId("");
    setLocalError(null);
    setTemplatesError(null);
    setTemplates([]);
    let cancelled = false;
    setTemplatesLoading(true);
    void (async () => {
      try {
        const list = await fetchTemplatesForSite(siteName);
        if (cancelled) return;
        setTemplates(list);
        setTemplateId(list[0]?.id ?? "");
      } catch (err) {
        if (cancelled) return;
        if (isSessionRedirectError(err)) return;
        setTemplatesError(
          formatApiError(err, ARCH_MSG.CREATE_TEMPLATE_EMPTY),
        );
      } finally {
        if (!cancelled) setTemplatesLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [open, siteName]);

  if (!open) {
    return null;
  }

  const onTitleChange = (v: string) => {
    setTitle(v);
    setLocalError(null);
    if (!urlNameTouched) {
      const base = titleToPageFileName(v).replace(/\.html$/i, "");
      setUrlName(sanitizeFileNameInput(base));
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    const titleErr = validateSectionTitle(title);
    if (titleErr) {
      setLocalError(titleErr);
      return;
    }
    const urlErr = validateSectionFolderName(urlName);
    if (urlErr) {
      setLocalError(urlErr);
      return;
    }
    if (!templateId.trim()) {
      setLocalError(ARCH_MSG.CREATE_TEMPLATE_EMPTY);
      return;
    }
    onSubmit({
      title: title.trim(),
      urlName: urlName.trim(),
      templateId: templateId.trim(),
    });
  };

  return (
    <div
      style={overlayStyle}
      data-testid="architecture-create-dialog"
      role="presentation"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="architecture-create-title"
        style={panelStyle}
        onClick={(e) => e.stopPropagation()}
      >
        <h2
          id="architecture-create-title"
          style={{ marginTop: 0, marginBottom: 8, fontSize: "1.1rem" }}
        >
          {ARCH_MSG.CREATE_DIALOG_TITLE}
        </h2>
        <p
          style={{
            margin: "0 0 12px",
            color: catalogColors.muted,
            fontSize: "0.9rem",
          }}
          data-testid="architecture-create-parent"
        >
          {ARCH_MSG.CREATE_PARENT_LABEL}: {parentTitle}
        </p>
        <form onSubmit={handleSubmit}>
          <label
            htmlFor="architecture-create-title-input"
            style={{ display: "block", fontSize: "0.9rem" }}
          >
            {ARCH_MSG.CREATE_TITLE_LABEL}
            <input
              id="architecture-create-title-input"
              data-testid="architecture-create-title-input"
              value={title}
              onChange={(e) => onTitleChange(e.target.value)}
              required
              disabled={busy}
              style={fieldStyle}
              autoFocus
            />
          </label>
          <label
            htmlFor="architecture-create-url-input"
            style={{ display: "block", fontSize: "0.9rem" }}
          >
            {ARCH_MSG.CREATE_URL_LABEL}
            <input
              id="architecture-create-url-input"
              data-testid="architecture-create-url-input"
              value={urlName}
              onChange={(e) => {
                setUrlNameTouched(true);
                setLocalError(null);
                setUrlName(sanitizeFileNameInput(e.target.value));
              }}
              required
              disabled={busy}
              style={fieldStyle}
            />
          </label>
          <label
            htmlFor="architecture-create-template-select"
            style={{ display: "block", fontSize: "0.9rem" }}
          >
            {ARCH_MSG.CREATE_TEMPLATE_LABEL}
            {templatesLoading ? (
              <span
                data-testid="architecture-create-templates-loading"
                style={{
                  display: "block",
                  marginTop: 4,
                  marginBottom: 12,
                  color: catalogColors.muted,
                }}
              >
                {ARCH_MSG.CREATE_TEMPLATE_LOADING}
              </span>
            ) : (
              <select
                id="architecture-create-template-select"
                data-testid="architecture-create-template-select"
                value={templateId}
                onChange={(e) => {
                  setLocalError(null);
                  setTemplateId(e.target.value);
                }}
                required
                disabled={busy || templates.length === 0}
                style={fieldStyle}
              >
                {templates.length === 0 ? (
                  <option value="">{ARCH_MSG.CREATE_TEMPLATE_EMPTY}</option>
                ) : (
                  templates.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name || t.id}
                    </option>
                  ))
                )}
              </select>
            )}
          </label>
          {templatesError || localError ? (
            <p
              role="alert"
              data-testid="architecture-create-error"
              style={{ color: catalogColors.error, fontSize: "0.9rem" }}
            >
              {localError || templatesError}
            </p>
          ) : null}
          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              gap: 8,
              marginTop: 8,
            }}
          >
            <button
              type="button"
              data-testid="architecture-create-cancel"
              onClick={onCancel}
              disabled={busy}
              style={{
                padding: "0.4rem 0.85rem",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: 4,
                background: "#fff",
                cursor: busy ? "not-allowed" : "pointer",
              }}
            >
              {ARCH_MSG.CREATE_CANCEL}
            </button>
            <button
              type="submit"
              data-testid="architecture-create-submit"
              disabled={
                busy || templatesLoading || templates.length === 0
              }
              style={{
                padding: "0.4rem 0.85rem",
                border: `1px solid ${catalogColors.accent}`,
                borderRadius: 4,
                background: catalogColors.accent,
                color: "#fff",
                cursor:
                  busy || templatesLoading || templates.length === 0
                    ? "not-allowed"
                    : "pointer",
              }}
            >
              {busy ? ARCH_MSG.ACTION_BUSY : ARCH_MSG.CREATE_SUBMIT}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CreateSectionDialog;
