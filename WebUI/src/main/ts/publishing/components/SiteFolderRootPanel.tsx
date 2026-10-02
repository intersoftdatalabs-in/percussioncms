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
  folderRootsMatch,
  isValidFolderRoot,
  normalizeFolderRoot,
  siteFolderRootHttpFailure,
} from "../siteFolderRoot";
import { buttonStyle, errorStyle, primaryButtonStyle } from "../publishing.styles";

function saveErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    const kind = siteFolderRootHttpFailure(err.status);
    if (kind === "bad_request") {
      return message(MSG.PUBLISH_SITE_FOLDER_ROOT_BAD);
    }
    if (kind === "forbidden") {
      return message(MSG.PUBLISH_SITE_FOLDER_ROOT_FORBIDDEN);
    }
    if (kind === "conflict") {
      return message(MSG.PUBLISH_SITE_FOLDER_ROOT_CONFLICT);
    }
  }
  return message(MSG.PUBLISH_SITE_FOLDER_ROOT_ERROR);
}

/**
 * Edit the open site folder root via PUT /services/sites/{nameOrId}.
 * Cancel, an unchanged path, a blank path, and an unsafe path do not write
 * and do not claim success. The saved path is the site record only.
 */
export function SiteFolderRootPanel({
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
        const text = site.folderRoot ?? "";
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
    const next = normalizeFolderRoot(draft);
    if (!next) {
      setNotice(null);
      setError(message(MSG.PUBLISH_SITE_FOLDER_ROOT_EMPTY));
      return;
    }
    if (!isValidFolderRoot(next)) {
      setNotice(null);
      setError(message(MSG.PUBLISH_SITE_FOLDER_ROOT_INVALID));
      return;
    }
    if (folderRootsMatch(saved, next)) {
      closeWithoutWrite();
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await updateSite(key, { name: key, folderRoot: next });
      const text = updated.folderRoot ?? next;
      setSaved(text);
      setDraft(text);
      setEditing(false);
      setNotice(message(MSG.PUBLISH_SITE_FOLDER_ROOT_SAVED));
    } catch (err: unknown) {
      setError(saveErrorMessage(err));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const shown = saved.trim() ? saved.trim() : "—";

  return (
    <div data-testid="publish-site-folder-root-panel" style={{ marginBottom: 16 }}>
      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
        <span>{message(MSG.PUBLISH_SITE_FOLDER_ROOT)}</span>
        <span data-testid="publish-site-folder-root">{shown}</span>
        {!editing ? (
          <button
            type="button"
            style={buttonStyle}
            data-testid="publish-site-folder-root-edit"
            onClick={() => {
              setDraft(saved);
              setEditing(true);
              setError(null);
              setNotice(null);
            }}
          >
            {message(MSG.PUBLISH_EDIT_SITE_FOLDER_ROOT)}
          </button>
        ) : null}
      </div>
      {notice && !editing ? (
        <p data-testid="publish-site-folder-root-saved" style={{ color: "#276749" }}>
          {notice}
        </p>
      ) : null}
      {editing ? (
        <div data-testid="publish-site-folder-root-form">
          <label>
            <span className="sr-only">{message(MSG.PUBLISH_SITE_FOLDER_ROOT)}</span>
            <input
              data-testid="publish-site-folder-root-input"
              value={draft}
              disabled={busy}
              onChange={(e) => setDraft(e.target.value)}
              style={{ display: "block", width: "100%", maxWidth: 480, margin: "8px 0" }}
            />
          </label>
          {error ? (
            <p role="alert" data-testid="publish-site-folder-root-error" style={errorStyle}>
              {error}
            </p>
          ) : null}
          <div style={{ display: "flex", gap: 8 }}>
            <button
              type="button"
              data-testid="publish-site-folder-root-save"
              style={primaryButtonStyle}
              disabled={busy || !key}
              onClick={() => void handleSave()}
            >
              {message(MSG.PUBLISH_SAVE)}
            </button>
            <button
              type="button"
              data-testid="publish-site-folder-root-cancel"
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
