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
import { deleteSite } from "../../api/developer/sitesApi";
import { message, MSG } from "../../i18n/message";
import { buttonStyle, errorStyle, primaryButtonStyle } from "../publishing.styles";

function deleteErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return message(MSG.PUBLISH_DELETE_SITE_FORBIDDEN);
    }
    if (err.status === 404) {
      return message(MSG.PUBLISH_DELETE_SITE_NOT_FOUND);
    }
    if (err.status === 409) {
      return message(MSG.PUBLISH_DELETE_SITE_CONFLICT);
    }
  }
  return message(MSG.PUBLISH_DELETE_SITE_ERROR);
}

/**
 * Confirm before DELETE /services/sites/{name}. Cancel does not call the server.
 * Failures stay on the workspace; success leaves via onDeleted.
 */
export function DeleteSitePanel({
  siteName,
  onCancel,
  onDeleted,
}: {
  siteName: string;
  onCancel: () => void;
  onDeleted: () => void;
}): React.ReactElement {
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inflight = useRef(false);
  const ready = !busy && siteName.trim().length > 0;

  async function handleDelete(): Promise<void> {
    if (!ready || inflight.current) {
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    try {
      await deleteSite(siteName.trim());
      onDeleted();
    } catch (err: unknown) {
      setError(deleteErrorMessage(err));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  return (
    <div
      data-testid="publish-site-delete"
      role="dialog"
      aria-label={message(MSG.PUBLISH_DELETE_SITE)}
    >
      <p data-testid="publish-site-delete-prompt">{message(MSG.PUBLISH_DELETE_SITE_CONFIRM)}</p>
      {error ? (
        <p role="alert" data-testid="publish-site-delete-error" style={errorStyle}>
          {error}
        </p>
      ) : null}
      <div style={{ display: "flex", gap: 8 }}>
        <button
          type="button"
          data-testid="publish-site-delete-confirm"
          style={primaryButtonStyle}
          disabled={!ready}
          onClick={() => void handleDelete()}
        >
          {message(MSG.PUBLISH_DELETE_SITE_SAVE)}
        </button>
        <button
          type="button"
          data-testid="publish-site-delete-cancel"
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
