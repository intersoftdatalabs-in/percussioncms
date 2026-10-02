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
  canonicalReplaceHttpFailure,
  canonicalReplaceLabel,
  parseCanonicalReplace,
} from "../siteCanonicalReplace";
import { buttonStyle, errorStyle, primaryButtonStyle } from "../publishing.styles";

function saveErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    const kind = canonicalReplaceHttpFailure(err.status);
    if (kind === "bad_request") {
      return message(MSG.PUBLISH_SITE_CANONICAL_REPLACE_BAD);
    }
    if (kind === "forbidden") {
      return message(MSG.PUBLISH_SITE_CANONICAL_REPLACE_FORBIDDEN);
    }
    if (kind === "conflict") {
      return message(MSG.PUBLISH_SITE_CANONICAL_REPLACE_CONFLICT);
    }
  }
  return message(MSG.PUBLISH_SITE_CANONICAL_REPLACE_ERROR);
}

/**
 * Toggle whether canonical URLs replace the rendered location via PUT
 * /services/sites/{nameOrId}. Cancel and an unchanged value do not write.
 * Success is shown only after PUT returns the saved boolean.
 */
export function SiteCanonicalReplacePanel({
  siteName,
}: {
  siteName: string;
}): React.ReactElement {
  const [saved, setSaved] = useState<boolean | null>(null);
  const [draft, setDraft] = useState(true);
  const [editing, setEditing] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inflight = useRef(false);
  const key = siteName.trim();

  useEffect(() => {
    let cancelled = false;
    if (!key) {
      setSaved(null);
      setDraft(true);
      return;
    }
    getSite(key)
      .then((site) => {
        if (cancelled) {
          return;
        }
        const value = parseCanonicalReplace(site.canonicalReplace);
        setSaved(value);
        setDraft(value ?? true);
      })
      .catch(() => {
        if (!cancelled) {
          setSaved(null);
          setDraft(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [key]);

  function closeWithoutWrite(): void {
    setDraft(saved ?? true);
    setEditing(false);
    setError(null);
  }

  async function handleSave(): Promise<void> {
    if (!key || inflight.current || busy) {
      return;
    }
    if (saved !== null && saved === draft) {
      closeWithoutWrite();
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await updateSite(key, { name: key, canonicalReplace: draft });
      const value = parseCanonicalReplace(updated.canonicalReplace);
      const next = value ?? draft;
      setSaved(next);
      setDraft(next);
      setEditing(false);
      setNotice(message(MSG.PUBLISH_SITE_CANONICAL_REPLACE_SAVED));
    } catch (err: unknown) {
      setError(saveErrorMessage(err));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div data-testid="publish-site-canonical-replace-panel" style={{ marginBottom: 16 }}>
      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
        <span>{message(MSG.PUBLISH_SITE_CANONICAL_REPLACE)}</span>
        <span data-testid="publish-site-canonical-replace">{canonicalReplaceLabel(saved)}</span>
        {!editing ? (
          <button
            type="button"
            style={buttonStyle}
            data-testid="publish-site-canonical-replace-edit"
            onClick={() => {
              setDraft(saved ?? true);
              setEditing(true);
              setError(null);
              setNotice(null);
            }}
          >
            {message(MSG.PUBLISH_EDIT_SITE_CANONICAL_REPLACE)}
          </button>
        ) : null}
      </div>
      {notice && !editing ? (
        <p data-testid="publish-site-canonical-replace-saved" style={{ color: "#276749" }}>
          {notice}
        </p>
      ) : null}
      {editing ? (
        <div data-testid="publish-site-canonical-replace-form">
          <label style={{ display: "flex", gap: 8, alignItems: "center", margin: "8px 0" }}>
            <input
              type="checkbox"
              data-testid="publish-site-canonical-replace-input"
              checked={draft}
              disabled={busy}
              onChange={(e) => setDraft(e.target.checked)}
            />
            <span>{message(MSG.PUBLISH_SITE_CANONICAL_REPLACE)}</span>
          </label>
          {error ? (
            <p role="alert" data-testid="publish-site-canonical-replace-error" style={errorStyle}>
              {error}
            </p>
          ) : null}
          <div style={{ display: "flex", gap: 8 }}>
            <button
              type="button"
              data-testid="publish-site-canonical-replace-save"
              style={primaryButtonStyle}
              disabled={busy || !key}
              onClick={() => void handleSave()}
            >
              {message(MSG.PUBLISH_SAVE)}
            </button>
            <button
              type="button"
              data-testid="publish-site-canonical-replace-cancel"
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
