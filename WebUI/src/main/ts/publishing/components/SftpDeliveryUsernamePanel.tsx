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
import {
  buildSftpUsernameBody,
  SFTP_USERNAME_DRIVER_ONLY,
  SFTP_USERNAME_REQUIRED,
  sftpUsernameHttpFailure,
  validateSftpUsername,
  withUpdatedSftpUsername,
} from "../deliveryServerSftpUsername";
import { isSftpDeliveryDriver } from "../deliveryServerSftpPrivateKey";
import {
  serverDriver,
  serverProperty,
  unwrapPublishServer,
} from "../deliveryServerFtpHost";
import { isDefaultDeliveryServer, deliveryServerDisplayName } from "../deliveryServerRename";
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
    const kind = sftpUsernameHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, SFTP_USERNAME_REQUIRED);
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
 * Set the username on an SFTP delivery server. The PUT body is only userid.
 * Host, port, and folder stay visible and are not sent. The passphrase
 * and private key are never rendered.
 */
export function SftpDeliveryUsernamePanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [notSftp, setNotSftp] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setNotSftp(false);
    setDraft("");
    setError(null);
    setDirty(false);
  }, [siteId, serverId, setDirty]);

  if (!serverId) {
    return null;
  }

  const sftp = loaded != null && isSftpDeliveryDriver(serverDriver(loaded));
  const host = sftp && loaded ? serverProperty(loaded, "serverip") : "";
  const port = sftp && loaded ? serverProperty(loaded, "port") : "";
  const user = sftp && loaded ? serverProperty(loaded, "userid") : "";
  const folder = sftp && loaded ? serverProperty(loaded, "folder") : "";
  const name = sftp && loaded ? deliveryServerDisplayName(loaded) : "";
  const defaultFlag =
    sftp && loaded ? (isDefaultDeliveryServer(loaded) ? "yes" : "no") : "";

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotSftp(false);
    try {
      const raw = await getServer(siteId, serverId);
      const server = unwrapPublishServer(raw);
      if (!isSftpDeliveryDriver(serverDriver(server))) {
        setLoaded(null);
        setEditing(false);
        setNotSftp(true);
        setDirty(false);
        return;
      }
      setLoaded(server);
      setDraft(serverProperty(server, "userid"));
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
    setDraft(user);
    setEditing(false);
  }

  async function save(): Promise<void> {
    if (busy || !sftp) {
      return;
    }
    const validated = validateSftpUsername(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildSftpUsernameBody(validated.username));
      setDirty(false);
      setEditing(false);
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isSftpDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
          setDraft(serverProperty(refreshed, "userid"));
          return;
        }
      } catch {
        // PUT succeeded. Keep the previous host, port, and folder and show the new username.
      }
      if (loaded) {
        const next = withUpdatedSftpUsername(loaded, validated.username);
        setLoaded(next);
        setDraft(validated.username);
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="sftp-username-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>SFTP username</h3>
      {sftp && (
        <div data-testid="sftp-username-summary">
          <p>
            Name: <span data-testid="sftp-username-name">{shown(name)}</span>
          </p>
          <p>
            Host: <span data-testid="sftp-username-host">{shown(host)}</span>
          </p>
          <p>
            Port: <span data-testid="sftp-username-port">{shown(port)}</span>
          </p>
          <p>
            User ID: <span data-testid="sftp-username-current">{shown(user)}</span>
          </p>
          <p>
            Folder: <span data-testid="sftp-username-folder">{shown(folder)}</span>
          </p>
          <p>
            Default: <span data-testid="sftp-username-default">{shown(defaultFlag)}</span>
          </p>
        </div>
      )}
      {notSftp && (
        <p data-testid="sftp-username-not-sftp" role="status">
          {SFTP_USERNAME_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="sftp-username-form">
          <div style={formRowStyle}>
            <label htmlFor="sftp-username-input">* User ID</label>
            <input
              id="sftp-username-input"
              data-testid="sftp-username-input"
              value={draft}
              autoComplete="off"
              onChange={(event) => {
                setDraft(event.target.value);
                setDirty(event.target.value.trim() !== user.trim());
              }}
            />
          </div>
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="sftp-username-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="sftp-username-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set username
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="sftp-username-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save username
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="sftp-username-cancel"
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
