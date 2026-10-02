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
import { headContentForSave, headContentMatch, siteAdditionalHeadHttpFailure } from "../siteAdditionalHeadEdit";
import { buttonStyle, errorStyle, primaryButtonStyle } from "../publishing.styles";

function saveErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    const kind = siteAdditionalHeadHttpFailure(err.status);
    if (kind === "bad_request") {
      return message(MSG.PUBLISH_SITE_ADDITIONAL_HEAD_BAD);
    }
    if (kind === "forbidden") {
      return message(MSG.PUBLISH_SITE_ADDITIONAL_HEAD_FORBIDDEN);
    }
    if (kind === "conflict") {
      return message(MSG.PUBLISH_SITE_ADDITIONAL_HEAD_CONFLICT);
    }
  }
  return message(MSG.PUBLISH_SITE_ADDITIONAL_HEAD_ERROR);
}

/**
 * Edit the open site additional head content via PUT /services/sites/{nameOrId}.
 * The markup is shown as text and is not inserted into the document.
 * Cancel and an unchanged (including blank) close do not write.
 */
export function SiteAdditionalHeadPanel({
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
        const text = site.siteAdditionalHeadContent ?? "";
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
    if (headContentMatch(saved, draft)) {
      closeWithoutWrite();
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = headContentForSave(draft);
      const updated = await updateSite(key, {
        name: key,
        siteAdditionalHeadContent: next,
      });
      const text = updated.siteAdditionalHeadContent ?? next;
      setSaved(text);
      setDraft(text);
      setEditing(false);
      setNotice(message(MSG.PUBLISH_SITE_ADDITIONAL_HEAD_SAVED));
    } catch (err: unknown) {
      setError(saveErrorMessage(err));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const shown = saved.trim() ? saved.trim() : "—";

  return (
    <div data-testid="publish-site-additional-head-panel" style={{ marginBottom: 16 }}>
      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
        <span>{message(MSG.PUBLISH_SITE_ADDITIONAL_HEAD)}</span>
        <pre
          data-testid="publish-site-additional-head"
          style={{ margin: 0, whiteSpace: "pre-wrap", fontFamily: "inherit" }}
        >
          {shown}
        </pre>
        {!editing ? (
          <button
            type="button"
            style={buttonStyle}
            data-testid="publish-site-additional-head-edit"
            onClick={() => {
              setDraft(saved);
              setEditing(true);
              setError(null);
              setNotice(null);
            }}
          >
            {message(MSG.PUBLISH_EDIT_SITE_ADDITIONAL_HEAD)}
          </button>
        ) : null}
      </div>
      {notice && !editing ? (
        <p data-testid="publish-site-additional-head-saved" style={{ color: "#276749" }}>
          {notice}
        </p>
      ) : null}
      {editing ? (
        <div data-testid="publish-site-additional-head-form">
          <label>
            <span className="sr-only">{message(MSG.PUBLISH_SITE_ADDITIONAL_HEAD)}</span>
            <textarea
              data-testid="publish-site-additional-head-input"
              value={draft}
              disabled={busy}
              rows={4}
              spellCheck={false}
              onChange={(e) => setDraft(e.target.value)}
              style={{ display: "block", width: "100%", maxWidth: 640, margin: "8px 0" }}
            />
          </label>
          {error ? (
            <p role="alert" data-testid="publish-site-additional-head-error" style={errorStyle}>
              {error}
            </p>
          ) : null}
          <div style={{ display: "flex", gap: 8 }}>
            <button
              type="button"
              data-testid="publish-site-additional-head-save"
              style={primaryButtonStyle}
              disabled={busy || !key}
              onClick={() => void handleSave()}
            >
              {message(MSG.PUBLISH_SAVE)}
            </button>
            <button
              type="button"
              data-testid="publish-site-additional-head-cancel"
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
