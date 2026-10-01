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
  CANONICAL_DISTS,
  canonicalDistHttpFailure,
  canonicalDistsMatch,
  isValidCanonicalDist,
  normalizeCanonicalDist,
  type CanonicalDist,
} from "../siteCanonicalDist";
import { buttonStyle, errorStyle, primaryButtonStyle } from "../publishing.styles";

function saveErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    const kind = canonicalDistHttpFailure(err.status);
    if (kind === "bad_request") {
      return message(MSG.PUBLISH_SITE_CANONICAL_DIST_BAD);
    }
    if (kind === "forbidden") {
      return message(MSG.PUBLISH_SITE_CANONICAL_DIST_FORBIDDEN);
    }
    if (kind === "conflict") {
      return message(MSG.PUBLISH_SITE_CANONICAL_DIST_CONFLICT);
    }
  }
  return message(MSG.PUBLISH_SITE_CANONICAL_DIST_ERROR);
}

function shownDist(saved: string): string {
  const normalized = normalizeCanonicalDist(saved);
  if (isValidCanonicalDist(normalized)) {
    return normalized;
  }
  return saved.trim() ? saved.trim() : "—";
}

/**
 * Edit the open site canonical distribution (pages/sections) via PUT
 * /services/sites/{nameOrId}. Cancel, an unchanged value, and anything other
 * than pages or sections do not write and do not claim success.
 */
export function SiteCanonicalDistPanel({
  siteName,
}: {
  siteName: string;
}): React.ReactElement {
  const [saved, setSaved] = useState("");
  const [draft, setDraft] = useState<CanonicalDist>("pages");
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
      setDraft("pages");
      return;
    }
    getSite(key)
      .then((site) => {
        if (cancelled) {
          return;
        }
        const text = site.canonicalDist ?? "";
        const normalized = normalizeCanonicalDist(text);
        setSaved(text);
        setDraft(isValidCanonicalDist(normalized) ? normalized : "pages");
      })
      .catch(() => {
        if (!cancelled) {
          setSaved("");
          setDraft("pages");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [key]);

  function closeWithoutWrite(): void {
    const normalized = normalizeCanonicalDist(saved);
    setDraft(isValidCanonicalDist(normalized) ? normalized : "pages");
    setEditing(false);
    setError(null);
  }

  async function handleSave(): Promise<void> {
    if (!key || inflight.current || busy) {
      return;
    }
    if (!isValidCanonicalDist(draft)) {
      setError(message(MSG.PUBLISH_SITE_CANONICAL_DIST_INVALID));
      setNotice(null);
      return;
    }
    if (canonicalDistsMatch(saved, draft)) {
      closeWithoutWrite();
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    const next = normalizeCanonicalDist(draft);
    try {
      const updated = await updateSite(key, { name: key, canonicalDist: next });
      const text = updated.canonicalDist ?? next;
      const normalizedSaved = normalizeCanonicalDist(text);
      setSaved(text);
      setDraft(isValidCanonicalDist(normalizedSaved) ? normalizedSaved : "pages");
      setEditing(false);
      setNotice(message(MSG.PUBLISH_SITE_CANONICAL_DIST_SAVED));
    } catch (err: unknown) {
      setError(saveErrorMessage(err));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div data-testid="publish-site-canonical-dist-panel" style={{ marginBottom: 16 }}>
      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
        <span>{message(MSG.PUBLISH_SITE_CANONICAL_DIST)}</span>
        <span data-testid="publish-site-canonical-dist">{shownDist(saved)}</span>
        {!editing ? (
          <button
            type="button"
            style={buttonStyle}
            data-testid="publish-site-canonical-dist-edit"
            onClick={() => {
              const normalized = normalizeCanonicalDist(saved);
              setDraft(isValidCanonicalDist(normalized) ? normalized : "pages");
              setEditing(true);
              setError(null);
              setNotice(null);
            }}
          >
            {message(MSG.PUBLISH_EDIT_SITE_CANONICAL_DIST)}
          </button>
        ) : null}
      </div>
      {notice && !editing ? (
        <p data-testid="publish-site-canonical-dist-saved" style={{ color: "#276749" }}>
          {notice}
        </p>
      ) : null}
      {editing ? (
        <div data-testid="publish-site-canonical-dist-form">
          <label>
            <span className="sr-only">{message(MSG.PUBLISH_SITE_CANONICAL_DIST)}</span>
            <select
              data-testid="publish-site-canonical-dist-input"
              value={draft}
              disabled={busy}
              onChange={(e) => {
                const value = e.target.value;
                if (isValidCanonicalDist(value)) {
                  setDraft(value);
                }
              }}
              style={{ display: "block", margin: "8px 0" }}
            >
              {CANONICAL_DISTS.map((dist) => (
                <option key={dist} value={dist}>
                  {dist}
                </option>
              ))}
            </select>
          </label>
          {error ? (
            <p role="alert" data-testid="publish-site-canonical-dist-error" style={errorStyle}>
              {error}
            </p>
          ) : null}
          <div style={{ display: "flex", gap: 8 }}>
            <button
              type="button"
              data-testid="publish-site-canonical-dist-save"
              style={primaryButtonStyle}
              disabled={busy || !key}
              onClick={() => void handleSave()}
            >
              {message(MSG.PUBLISH_SAVE)}
            </button>
            <button
              type="button"
              data-testid="publish-site-canonical-dist-cancel"
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
