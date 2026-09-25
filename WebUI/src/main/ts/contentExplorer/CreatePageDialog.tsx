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
import { listContentTypes } from "../api/developer/contentTypesApi";
import { message } from "../i18n/message";
import { createPageFieldMessage, formatCreatePageError } from "./createPageErrors";
import {
  pageContentTypeChoices,
  validateCreatePageFields,
  type PageContentTypeChoice,
} from "./createPageInFolder";
import { EXPLORER_MSG } from "./messages";

export interface CreatePageDialogProps {
  busy?: boolean;
  loadTypes?: () => Promise<PageContentTypeChoice[]>;
  onCancel: () => void;
  onCreate: (name: string, contentType: string) => Promise<void>;
}

async function defaultLoadTypes(): Promise<PageContentTypeChoice[]> {
  return pageContentTypeChoices(await listContentTypes());
}

/**
 * Name + page content type for the folder that is already selected.
 * Cancel does not call {@link CreatePageDialogProps.onCreate}.
 */
export function CreatePageDialog({
  busy,
  loadTypes = defaultLoadTypes,
  onCancel,
  onCreate,
}: CreatePageDialogProps): React.ReactElement {
  const [name, setName] = useState("");
  const [contentType, setContentType] = useState("");
  const [types, setTypes] = useState<PageContentTypeChoice[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let cancelled = false;
    void loadTypes()
      .then((rows) => {
        if (cancelled) {
          return;
        }
        setTypes(rows);
        setContentType(rows[0]?.name ?? "");
        if (rows.length === 0) {
          setLoadError(message(EXPLORER_MSG.ACTION_CREATE_PAGE_NO_TYPE));
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setLoadError(formatCreatePageError(err));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [loadTypes]);

  const locked = busy || submitting;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="explorer-create-page-title"
      data-testid="explorer-create-page"
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(15,23,42,0.45)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 40,
      }}
    >
      <form
        style={{
          background: "#fff",
          color: "#0f172a",
          minWidth: 320,
          maxWidth: 440,
          padding: 16,
          borderRadius: 8,
        }}
        onSubmit={(e) => {
          e.preventDefault();
          const reason = validateCreatePageFields(name, contentType);
          if (reason) {
            setError(createPageFieldMessage(reason));
            return;
          }
          setSubmitting(true);
          setError(null);
          void onCreate(name.trim(), contentType.trim())
            .catch((err: unknown) => {
              setError(formatCreatePageError(err));
            })
            .finally(() => setSubmitting(false));
        }}
      >
        <h2 id="explorer-create-page-title" style={{ fontSize: 16, margin: "0 0 12px" }}>
          {message(EXPLORER_MSG.ACTION_CREATE_PAGE)}
        </h2>
        <label style={{ display: "block", fontSize: 13 }}>
          {message(EXPLORER_MSG.ACTION_CREATE_PAGE_NAME)}
          <input
            data-testid="explorer-create-page-name"
            value={name}
            disabled={locked}
            onChange={(ev) => setName(ev.target.value)}
            style={{ display: "block", width: "100%", marginTop: 6, padding: 6 }}
          />
        </label>
        <label style={{ display: "block", fontSize: 13, marginTop: 12 }}>
          {message(EXPLORER_MSG.ACTION_CREATE_PAGE_TYPE)}
          <select
            data-testid="explorer-create-page-type"
            value={contentType}
            disabled={locked || types.length === 0}
            onChange={(ev) => setContentType(ev.target.value)}
            style={{ display: "block", width: "100%", marginTop: 6, padding: 6 }}
          >
            {types.length === 0 ? (
              <option value="">{message(EXPLORER_MSG.ACTION_CREATE_PAGE_NO_TYPE)}</option>
            ) : null}
            {types.map((t) => (
              <option key={t.name} value={t.name}>
                {t.label}
              </option>
            ))}
          </select>
        </label>
        {loadError || error ? (
          <p role="alert" data-testid="explorer-create-page-error" style={{ color: "#991b1b" }}>
            {error ?? loadError}
          </p>
        ) : null}
        <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 16 }}>
          <button
            type="button"
            data-testid="explorer-create-page-cancel"
            disabled={locked}
            onClick={onCancel}
          >
            {message(EXPLORER_MSG.CONFIRM_CANCEL)}
          </button>
          <button type="submit" data-testid="explorer-create-page-confirm" disabled={locked}>
            {message(EXPLORER_MSG.CONFIRM_OK)}
          </button>
        </div>
      </form>
    </div>
  );
}
