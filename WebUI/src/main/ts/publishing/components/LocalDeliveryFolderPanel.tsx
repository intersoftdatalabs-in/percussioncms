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

import React, { useEffect, useState } from "react";
import { formatApiError, isApiError } from "../../api/client";
import { getServer, updateServer } from "../../api/publishing/serversApi";
import { message, MSG } from "../../i18n/message";
import { serverDriver, serverProperty, unwrapPublishServer } from "../deliveryServerFtpHost";
import {
  buildLocalFolderBody,
  isLocalDeliveryDriver,
  LOCAL_FOLDER_DRIVER_ONLY,
  LOCAL_FOLDER_TOO_LONG,
  localFolderHttpFailure,
  validateLocalFolder,
  withUpdatedLocalFolder,
} from "../deliveryServerLocalFolder";
import { useDirtyForm } from "../dirtyFormContext";
import type { PublishServer } from "../types";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

function saveErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    const kind = localFolderHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, LOCAL_FOLDER_TOO_LONG);
    }
    if (kind === "forbidden") {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (kind === "conflict") {
      return formatApiError(err, message(MSG.PUBLISH_SERVER_NAME_CONFLICT));
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

function shown(value: string): string {
  const text = value.trim();
  return text ? text : "—";
}

/**
 * Set the folder on a Local (file) delivery server. The PUT body is only folder.
 * Driver and name stay. Host-style FTP properties are not shown and are not sent.
 * A blank folder clears the stored folder. The saved folder is shown only after a
 * successful save (or after load). FTP servers keep the FTP folder panel.
 */
export function LocalDeliveryFolderPanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [notLocal, setNotLocal] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setNotLocal(false);
    setDraft("");
    setError(null);
    setDirty(false);
  }, [siteId, serverId, setDirty]);

  if (!serverId) {
    return null;
  }

  const local = loaded != null && isLocalDeliveryDriver(serverDriver(loaded));
  const folder = local && loaded ? serverProperty(loaded, "folder") : "";

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotLocal(false);
    try {
      const raw = await getServer(siteId, serverId);
      const server = unwrapPublishServer(raw);
      if (!isLocalDeliveryDriver(serverDriver(server))) {
        setLoaded(null);
        setEditing(false);
        setNotLocal(true);
        setDirty(false);
        return;
      }
      setLoaded(server);
      setDraft(serverProperty(server, "folder"));
      setEditing(true);
      setDirty(false);
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  function cancel(): void {
    if (busy) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setDraft(folder);
    setEditing(false);
  }

  async function save(): Promise<void> {
    if (busy || !local) {
      return;
    }
    const validated = validateLocalFolder(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildLocalFolderBody(validated.folder));
      setDirty(false);
      setEditing(false);
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isLocalDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
          setDraft(serverProperty(refreshed, "folder"));
          return;
        }
      } catch {
        // PUT succeeded. Show the new folder without changing driver or name.
      }
      if (loaded) {
        const next = withUpdatedLocalFolder(loaded, validated.folder);
        setLoaded(next);
        setDraft(validated.folder);
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="local-folder-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>Local folder</h3>
      {local && (
        <div data-testid="local-folder-summary">
          <p>
            Folder: <span data-testid="local-folder-current">{shown(folder)}</span>
          </p>
        </div>
      )}
      {notLocal && (
        <p data-testid="local-folder-not-local" role="status">
          {LOCAL_FOLDER_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="local-folder-form">
          <div style={formRowStyle}>
            <label htmlFor="local-folder-input">Folder</label>
            <input
              id="local-folder-input"
              data-testid="local-folder-input"
              value={draft}
              autoComplete="off"
              onChange={(event) => {
                setDraft(event.target.value);
                setDirty(event.target.value.trim() !== folder.trim());
              }}
            />
          </div>
          <p data-testid="local-folder-optional">Leave blank to clear the folder.</p>
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="local-folder-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="local-folder-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set folder
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="local-folder-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save folder
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="local-folder-cancel"
              disabled={busy}
              onClick={cancel}
            >
              Cancel
            </button>
          </>
        )}
      </div>
    </div>
  );
}
