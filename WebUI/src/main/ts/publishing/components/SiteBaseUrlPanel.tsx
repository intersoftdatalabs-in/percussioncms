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

import React, { useEffect, useRef, useState } from "react";
import { isApiError } from "../../api/client";
import { getSite, updateSite } from "../../api/developer/sitesApi";
import { message, MSG } from "../../i18n/message";
import {
  baseUrlsMatch,
  isValidSiteBaseUrl,
  siteBaseUrlHttpFailure,
} from "../siteBaseUrl";
import { buttonStyle, errorStyle, primaryButtonStyle } from "../publishing.styles";

function saveErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    const kind = siteBaseUrlHttpFailure(err.status);
    if (kind === "bad_request") {
      return message(MSG.PUBLISH_SITE_BASE_URL_BAD);
    }
    if (kind === "forbidden") {
      return message(MSG.PUBLISH_SITE_BASE_URL_FORBIDDEN);
    }
    if (kind === "conflict") {
      return message(MSG.PUBLISH_SITE_BASE_URL_CONFLICT);
    }
  }
  return message(MSG.PUBLISH_SITE_BASE_URL_ERROR);
}

/**
 * Edit the open site base URL via PUT /services/sites/{nameOrId}.
 * Cancel, an unchanged value, and an empty or non-http(s) URL do not write
 * and do not claim success.
 */
export function SiteBaseUrlPanel({
  siteName,
}: {
  siteName: string;
}): React.ReactElement {
  const [saved, setSaved] = useState("");
  const [draft, setDraft] = useState("");
  const [editing, setEditing] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inflight = useRef(false);
  const key = siteName.trim();

  useEffect(() => {
    let cancelled = false;
    if (!key) {
      setSaved("");
      setDraft("");
      return;
    }
    getSite(key)
      .then((site) => {
        if (cancelled) {
          return;
        }
        const text = site.baseUrl ?? "";
        setSaved(text);
        setDraft(text);
      })
      .catch(() => {
        if (!cancelled) {
          setSaved("");
          setDraft("");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [key]);

  function closeWithoutWrite(): void {
    setDraft(saved);
    setEditing(false);
    setError(null);
  }

  async function handleSave(): Promise<void> {
    if (!key || inflight.current || busy) {
      return;
    }
    const next = draft.trim();
    if (!isValidSiteBaseUrl(next)) {
      setNotice(null);
      setError(message(MSG.PUBLISH_SITE_BASE_URL_INVALID));
      return;
    }
    if (baseUrlsMatch(saved, next)) {
      closeWithoutWrite();
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await updateSite(key, { name: key, baseUrl: next });
      const text = updated.baseUrl ?? next;
      setSaved(text);
      setDraft(text);
      setEditing(false);
      setNotice(message(MSG.PUBLISH_SITE_BASE_URL_SAVED));
    } catch (err: unknown) {
      setError(saveErrorMessage(err));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const shown = saved.trim() ? saved.trim() : "—";

  return (
    <div data-testid="publish-site-base-url-panel" style={{ marginBottom: 16 }}>
      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
        <span>{message(MSG.PUBLISH_SITE_BASE_URL)}</span>
        <span data-testid="publish-site-base-url">{shown}</span>
        {!editing ? (
          <button
            type="button"
            style={buttonStyle}
            data-testid="publish-site-base-url-edit"
            onClick={() => {
              setDraft(saved);
              setEditing(true);
              setError(null);
              setNotice(null);
            }}
          >
            {message(MSG.PUBLISH_EDIT_SITE_BASE_URL)}
          </button>
        ) : null}
      </div>
      {notice && !editing ? (
        <p data-testid="publish-site-base-url-saved" style={{ color: "#276749" }}>
          {notice}
        </p>
      ) : null}
      {editing ? (
        <div data-testid="publish-site-base-url-form">
          <label>
            <span className="sr-only">{message(MSG.PUBLISH_SITE_BASE_URL)}</span>
            <input
              data-testid="publish-site-base-url-input"
              value={draft}
              disabled={busy}
              onChange={(e) => setDraft(e.target.value)}
              style={{ display: "block", width: "100%", maxWidth: 480, margin: "8px 0" }}
            />
          </label>
          {error ? (
            <p role="alert" data-testid="publish-site-base-url-error" style={errorStyle}>
              {error}
            </p>
          ) : null}
          <div style={{ display: "flex", gap: 8 }}>
            <button
              type="button"
              data-testid="publish-site-base-url-save"
              style={primaryButtonStyle}
              disabled={busy || !key}
              onClick={() => void handleSave()}
            >
              {message(MSG.PUBLISH_SAVE)}
            </button>
            <button
              type="button"
              data-testid="publish-site-base-url-cancel"
              style={buttonStyle}
              disabled={busy}
              onClick={closeWithoutWrite}
            >
              {message(MSG.PUBLISH_CREATE_SITE_CANCEL)}
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
