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

import React, { useEffect, useState } from "react";
import { formatApiError, isApiError } from "../../api/client";
import { getServer, updateServer } from "../../api/publishing/serversApi";
import { message, MSG } from "../../i18n/message";
import {
  buildSftpPassphraseBody,
  SFTP_PASSPHRASE_DRIVER_ONLY,
  SFTP_PASSPHRASE_NEVER_SHOWN,
  SFTP_PASSPHRASE_REQUIRED,
  sftpPassphraseHttpFailure,
  validateSftpPassphrase,
} from "../deliveryServerSftpPassphrase";
import { isSftpDeliveryDriver } from "../deliveryServerSftpPrivateKey";
import {
  isDefaultDeliveryServer,
  deliveryServerDisplayName,
} from "../deliveryServerRename";
import {
  serverDriver,
  serverProperty,
  unwrapPublishServer,
} from "../deliveryServerFtpHost";
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
    const kind = sftpPassphraseHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, SFTP_PASSPHRASE_REQUIRED);
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
 * Set the key passphrase on an SFTP delivery server. The PUT body is only the
 * passphrase (`password` property, base64-encoded). Host, port, user id, folder,
 * private key, driver, and name stay visible and are not sent. The stored
 * passphrase is never rendered: the input always opens empty and is cleared
 * after a successful save, even though GET responses carry the stored
 * properties. The stored private key is left untouched by this save.
 */
export function SftpDeliveryPassphrasePanel({
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
  const driver = sftp && loaded ? serverDriver(loaded) : "";
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
      setDraft("");
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
    setDraft("");
    setEditing(false);
  }

  async function save(): Promise<void> {
    if (busy || !sftp) {
      return;
    }
    const validated = validateSftpPassphrase(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildSftpPassphraseBody(validated.passphrase));
      setDirty(false);
      setEditing(false);
      // Never write the typed passphrase into loaded state: the summary shows a
      // fixed status and the stored passphrase is never rendered.
      setDraft("");
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isSftpDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
        }
      } catch {
        // PUT succeeded. Host, port, user id, folder, name, driver, and default stay as shown.
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="sftp-passphrase-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>SFTP passphrase</h3>
      {sftp && (
        <div data-testid="sftp-passphrase-summary">
          <p>
            Name: <span data-testid="sftp-passphrase-name">{shown(name)}</span>
          </p>
          <p>
            Driver: <span data-testid="sftp-passphrase-driver">{shown(driver)}</span>
          </p>
          <p>
            Host: <span data-testid="sftp-passphrase-host">{shown(host)}</span>
          </p>
          <p>
            Port: <span data-testid="sftp-passphrase-port">{shown(port)}</span>
          </p>
          <p>
            User ID: <span data-testid="sftp-passphrase-user">{shown(user)}</span>
          </p>
          <p>
            Folder: <span data-testid="sftp-passphrase-folder">{shown(folder)}</span>
          </p>
          <p>
            Default:{" "}
            <span data-testid="sftp-passphrase-default">{shown(defaultFlag)}</span>
          </p>
          <p data-testid="sftp-passphrase-status" role="status">
            Passphrase: {SFTP_PASSPHRASE_NEVER_SHOWN}
          </p>
        </div>
      )}
      {notSftp && (
        <p data-testid="sftp-passphrase-not-sftp" role="status">
          {SFTP_PASSPHRASE_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="sftp-passphrase-form">
          <div style={formRowStyle}>
            <label htmlFor="sftp-passphrase-input">* New passphrase</label>
            <input
              id="sftp-passphrase-input"
              data-testid="sftp-passphrase-input"
              type="password"
              autoComplete="new-password"
              value={draft}
              onChange={(event) => {
                setDraft(event.target.value);
                setDirty(event.target.value.length > 0);
              }}
            />
          </div>
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="sftp-passphrase-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="sftp-passphrase-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set SFTP passphrase
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="sftp-passphrase-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save SFTP passphrase
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="sftp-passphrase-cancel"
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
