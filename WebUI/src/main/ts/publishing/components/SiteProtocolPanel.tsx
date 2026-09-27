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
  isValidSiteProtocol,
  normalizeSiteProtocol,
  protocolsMatch,
  SITE_PROTOCOLS,
  siteProtocolHttpFailure,
  type SiteProtocol,
} from "../siteProtocol";
import { buttonStyle, errorStyle, primaryButtonStyle } from "../publishing.styles";

function saveErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    const kind = siteProtocolHttpFailure(err.status);
    if (kind === "bad_request") {
      return message(MSG.PUBLISH_SITE_PROTOCOL_BAD);
    }
    if (kind === "forbidden") {
      return message(MSG.PUBLISH_SITE_PROTOCOL_FORBIDDEN);
    }
    if (kind === "conflict") {
      return message(MSG.PUBLISH_SITE_PROTOCOL_CONFLICT);
    }
  }
  return message(MSG.PUBLISH_SITE_PROTOCOL_ERROR);
}

function shownProtocol(saved: string): string {
  const normalized = normalizeSiteProtocol(saved);
  if (isValidSiteProtocol(normalized)) {
    return normalized;
  }
  return saved.trim() ? saved.trim() : "—";
}

/**
 * Edit the open site protocol (http/https) via PUT /services/sites/{nameOrId}.
 * Cancel, an unchanged value, and anything other than http or https do not write.
 */
export function SiteProtocolPanel({
  siteName,
}: {
  siteName: string;
}): React.ReactElement {
  const [saved, setSaved] = useState("");
  const [draft, setDraft] = useState<SiteProtocol>("https");
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
      setDraft("https");
      return;
    }
    getSite(key)
      .then((site) => {
        if (cancelled) {
          return;
        }
        const text = site.siteProtocol ?? "";
        const normalized = normalizeSiteProtocol(text);
        setSaved(text);
        setDraft(isValidSiteProtocol(normalized) ? normalized : "https");
      })
      .catch(() => {
        if (!cancelled) {
          setSaved("");
          setDraft("https");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [key]);

  function closeWithoutWrite(): void {
    const normalized = normalizeSiteProtocol(saved);
    setDraft(isValidSiteProtocol(normalized) ? normalized : "https");
    setEditing(false);
    setError(null);
  }

  async function handleSave(): Promise<void> {
    if (!key || inflight.current || busy) {
      return;
    }
    if (!isValidSiteProtocol(draft)) {
      setError(message(MSG.PUBLISH_SITE_PROTOCOL_INVALID));
      setNotice(null);
      return;
    }
    if (protocolsMatch(saved, draft)) {
      closeWithoutWrite();
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    const next = normalizeSiteProtocol(draft);
    try {
      const updated = await updateSite(key, { name: key, siteProtocol: next });
      const text = updated.siteProtocol ?? next;
      const normalizedSaved = normalizeSiteProtocol(text);
      setSaved(text);
      setDraft(isValidSiteProtocol(normalizedSaved) ? normalizedSaved : "https");
      setEditing(false);
      setNotice(message(MSG.PUBLISH_SITE_PROTOCOL_SAVED));
    } catch (err: unknown) {
      setError(saveErrorMessage(err));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div data-testid="publish-site-protocol-panel" style={{ marginBottom: 16 }}>
      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
        <span>{message(MSG.PUBLISH_SITE_PROTOCOL)}</span>
        <span data-testid="publish-site-protocol">{shownProtocol(saved)}</span>
        {!editing ? (
          <button
            type="button"
            style={buttonStyle}
            data-testid="publish-site-protocol-edit"
            onClick={() => {
              const normalized = normalizeSiteProtocol(saved);
              setDraft(isValidSiteProtocol(normalized) ? normalized : "https");
              setEditing(true);
              setError(null);
              setNotice(null);
            }}
          >
            {message(MSG.PUBLISH_EDIT_SITE_PROTOCOL)}
          </button>
        ) : null}
      </div>
      {notice && !editing ? (
        <p data-testid="publish-site-protocol-saved" style={{ color: "#276749" }}>
          {notice}
        </p>
      ) : null}
      {editing ? (
        <div data-testid="publish-site-protocol-form">
          <label>
            <span className="sr-only">{message(MSG.PUBLISH_SITE_PROTOCOL)}</span>
            <select
              data-testid="publish-site-protocol-input"
              value={draft}
              disabled={busy}
              onChange={(e) => {
                const value = e.target.value;
                if (isValidSiteProtocol(value)) {
                  setDraft(value);
                }
              }}
              style={{ display: "block", margin: "8px 0" }}
            >
              {SITE_PROTOCOLS.map((protocol) => (
                <option key={protocol} value={protocol}>
                  {protocol}
                </option>
              ))}
            </select>
          </label>
          {error ? (
            <p role="alert" data-testid="publish-site-protocol-error" style={errorStyle}>
              {error}
            </p>
          ) : null}
          <div style={{ display: "flex", gap: 8 }}>
            <button
              type="button"
              data-testid="publish-site-protocol-save"
              style={primaryButtonStyle}
              disabled={busy || !key}
              onClick={() => void handleSave()}
            >
              {message(MSG.PUBLISH_SAVE)}
            </button>
            <button
              type="button"
              data-testid="publish-site-protocol-cancel"
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
