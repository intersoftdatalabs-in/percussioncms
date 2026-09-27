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
import {
  copyManagedSite,
  suggestCopySiteName,
} from "../../api/architecture/siteAdminApi";
import { isApiError } from "../../api/client";
import {
  isValidSiteName,
  normalizeSiteName,
} from "../../api/developer/sitesApi";
import { message, MSG } from "../../i18n/message";
import { buttonStyle, errorStyle, primaryButtonStyle } from "../publishing.styles";

function namesMatch(left: string, right: string | undefined): boolean {
  return normalizeSiteName(left).toLowerCase() === (right ?? "").trim().toLowerCase();
}

function copyErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 409) {
      return message(MSG.PUBLISH_CREATE_SITE_DUPLICATE);
    }
    if (err.status === 400) {
      return message(MSG.PUBLISH_CREATE_SITE_INVALID);
    }
    if (err.status === 403) {
      return message(MSG.PUBLISH_COPY_SITE_FORBIDDEN);
    }
  }
  return message(MSG.PUBLISH_COPY_SITE_ERROR);
}

/**
 * Copy the open site to a new name. The source site is not renamed.
 * Cancel, blank, and duplicate names do not call the server.
 */
export function CopySitePanel({
  sourceName,
  existingNames,
  onCancel,
  onCopied,
}: {
  sourceName: string;
  existingNames: readonly string[];
  onCancel: () => void;
  onCopied: (copyName: string) => void;
}): React.ReactElement {
  const [name, setName] = useState(() => suggestCopySiteName(sourceName));
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inflight = useRef(false);
  const normalized = normalizeSiteName(name);
  const sameAsSource = namesMatch(normalized, sourceName);
  const ready = !busy && isValidSiteName(name) && !sameAsSource;

  async function handleCopy(): Promise<void> {
    if (!ready || inflight.current) {
      return;
    }
    if (
      sameAsSource ||
      existingNames.some((existing) => namesMatch(normalized, existing))
    ) {
      setError(message(MSG.PUBLISH_CREATE_SITE_DUPLICATE));
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    try {
      await copyManagedSite({ srcSite: sourceName, copySite: normalized });
      onCopied(normalized);
    } catch (err: unknown) {
      setError(copyErrorMessage(err));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div
      data-testid="publish-site-copy"
      role="dialog"
      aria-label={message(MSG.PUBLISH_COPY_SITE)}
    >
      {error ? (
        <p role="alert" data-testid="publish-site-copy-error" style={errorStyle}>
          {error}
        </p>
      ) : null}
      <label htmlFor="publish-site-copy-name">{message(MSG.PUBLISH_CREATE_SITE_NAME)}</label>
      <input
        id="publish-site-copy-name"
        data-testid="publish-site-copy-name"
        value={name}
        disabled={busy}
        autoComplete="off"
        onChange={(e) => setName(e.target.value)}
        style={{ padding: "6px 10px", minWidth: 200, display: "block", margin: "6px 0 12px" }}
      />
      <div style={{ display: "flex", gap: 8 }}>
        <button
          type="button"
          data-testid="publish-site-copy-save"
          style={primaryButtonStyle}
          disabled={!ready}
          onClick={() => void handleCopy()}
        >
          {message(MSG.PUBLISH_COPY_SITE_SAVE)}
        </button>
        <button
          type="button"
          data-testid="publish-site-copy-cancel"
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
