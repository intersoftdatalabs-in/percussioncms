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

import React, { useRef, useState } from "react";
import { isApiError } from "../../api/client";
import {
  isValidSiteName,
  normalizeSiteName,
  renameSite,
} from "../../api/developer/sitesApi";
import { message, MSG } from "../../i18n/message";
import { buttonStyle, errorStyle, primaryButtonStyle } from "../publishing.styles";

function namesMatch(left: string, right: string | undefined): boolean {
  return normalizeSiteName(left).toLowerCase() === (right ?? "").trim().toLowerCase();
}

function renameErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 409) {
      return message(MSG.PUBLISH_CREATE_SITE_DUPLICATE);
    }
    if (err.status === 400) {
      return message(MSG.PUBLISH_CREATE_SITE_INVALID);
    }
    if (err.status === 403) {
      return message(MSG.PUBLISH_RENAME_SITE_FORBIDDEN);
    }
  }
  return message(MSG.PUBLISH_RENAME_SITE_ERROR);
}

export function RenameSitePanel({
  currentName,
  existingNames,
  onCancel,
  onRenamed,
}: {
  currentName: string;
  existingNames: readonly string[];
  onCancel: () => void;
  onRenamed: (newName: string) => void;
}): React.ReactElement {
  const [name, setName] = useState(currentName);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inflight = useRef(false);
  const normalized = normalizeSiteName(name);
  const unchanged = namesMatch(normalized, currentName);
  const ready = !busy && isValidSiteName(name) && !unchanged;

  async function handleRename(): Promise<void> {
    if (!ready || inflight.current) {
      return;
    }
    if (
      existingNames.some(
        (existing) => !namesMatch(currentName, existing) && namesMatch(normalized, existing),
      )
    ) {
      setError(message(MSG.PUBLISH_CREATE_SITE_DUPLICATE));
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    try {
      const saved = await renameSite(currentName, normalized);
      const savedName = normalizeSiteName(saved.name ?? "") || normalized;
      onRenamed(savedName);
    } catch (err: unknown) {
      setError(renameErrorMessage(err));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div
      data-testid="publish-site-rename"
      role="dialog"
      aria-label={message(MSG.PUBLISH_RENAME_SITE)}
    >
      {error ? (
        <p role="alert" data-testid="publish-site-rename-error" style={errorStyle}>
          {error}
        </p>
      ) : null}
      <label htmlFor="publish-site-rename-name">{message(MSG.PUBLISH_CREATE_SITE_NAME)}</label>
      <input
        id="publish-site-rename-name"
        data-testid="publish-site-rename-name"
        value={name}
        disabled={busy}
        autoComplete="off"
        onChange={(e) => setName(e.target.value)}
        style={{ padding: "6px 10px", minWidth: 200, display: "block", margin: "6px 0 12px" }}
      />
      <div style={{ display: "flex", gap: 8 }}>
        <button
          type="button"
          data-testid="publish-site-rename-save"
          style={primaryButtonStyle}
          disabled={!ready}
          onClick={() => void handleRename()}
        >
          {message(MSG.PUBLISH_RENAME_SITE_SAVE)}
        </button>
        <button
          type="button"
          data-testid="publish-site-rename-cancel"
          style={buttonStyle}
          disabled={busy}
          onClick={onCancel}
        >
          {message(MSG.PUBLISH_CREATE_SITE_CANCEL)}
        </button>
      </div>
    </div>
  );
}
