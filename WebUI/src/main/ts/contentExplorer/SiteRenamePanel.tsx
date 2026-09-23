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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Content Explorer rename-site panel (#4764). Posts public REST
 * {@code POST /services/sites/{name}/rename}. Cancel does not call the server.
 */

import React, { useState } from "react";
import { isApiError } from "../api/client";
import { renameSite } from "../api/developer/sitesApi";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";

export interface SiteRenamePanelProps {
  siteName: string;
  submit?: (siteName: string, newName: string) => Promise<{ name?: string }>;
  onRenamed: (newName: string) => void;
  onCancel: () => void;
}

function errorForStatus(status: number): string {
  if (status === 400) return message(EXPLORER_MSG.SITE_RENAME_ERROR_400);
  if (status === 403) return message(EXPLORER_MSG.SITE_RENAME_ERROR_403);
  if (status === 409) return message(EXPLORER_MSG.SITE_RENAME_ERROR_409);
  return message(EXPLORER_MSG.SITE_RENAME_ERROR_GENERIC);
}

export function SiteRenamePanel(props: SiteRenamePanelProps): React.JSX.Element {
  const { siteName, onRenamed, onCancel } = props;
  const submit = props.submit ?? renameSite;
  const [name, setName] = useState(siteName);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: React.FormEvent): Promise<void> {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const saved = await submit(siteName, name);
      const next = (saved.name && saved.name.trim()) || name.trim();
      onRenamed(next);
    } catch (err) {
      if (isApiError(err)) {
        setError(errorForStatus(err.status));
      } else {
        setError(message(EXPLORER_MSG.SITE_RENAME_ERROR_GENERIC));
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <form
      data-testid="site-rename-form"
      aria-label={message(EXPLORER_MSG.SITE_RENAME_PANEL_REGION)}
      onSubmit={(e) => {
        void onSubmit(e);
      }}
    >
      <label htmlFor="site-rename-name">
        {message(EXPLORER_MSG.SITE_RENAME_NAME_LABEL)}
        <input
          id="site-rename-name"
          data-testid="site-rename-name"
          value={name}
          disabled={busy}
          onChange={(e) => setName(e.target.value)}
        />
      </label>
      {error ? (
        <p role="alert" data-testid="site-rename-error">
          {error}
        </p>
      ) : null}
      <button type="submit" data-testid="site-rename-submit" disabled={busy}>
        {message(EXPLORER_MSG.SITE_RENAME_SUBMIT)}
      </button>
      <button
        type="button"
        data-testid="site-rename-cancel"
        disabled={busy}
        onClick={onCancel}
      >
        {message(EXPLORER_MSG.SITE_RENAME_CANCEL)}
      </button>
    </form>
  );
}
