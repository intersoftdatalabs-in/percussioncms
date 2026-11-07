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
  buildSftpPrivateKeyBody,
  isSftpDeliveryDriver,
  SFTP_PRIVATE_KEY_DRIVER_ONLY,
  SFTP_PRIVATE_KEY_NEVER_SHOWN,
  SFTP_PRIVATE_KEY_REQUIRED,
  sftpPrivateKeyHttpFailure,
  validateSftpPrivateKey,
} from "../deliveryServerSftpPrivateKey";
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
    const kind = sftpPrivateKeyHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, SFTP_PRIVATE_KEY_REQUIRED);
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
 * Set the private key on an SFTP delivery server. The PUT body is only the
 * private key (base64-encoded). Host, port, user id, folder, passphrase,
 * driver, and name stay visible and are not sent. The stored key is never
 * rendered: the input always opens empty and is cleared after a successful
 * save, even though GET responses carry the stored properties. The stored
 * passphrase is left untouched by this save.
 */
export function SftpDeliveryPrivateKeyPanel({
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
    const validated = validateSftpPrivateKey(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildSftpPrivateKeyBody(validated.privateKey));
      setDirty(false);
      setEditing(false);
      // Never write the typed key into loaded state: the summary shows a
      // fixed status and the stored key is never rendered.
      setDraft("");
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isSftpDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
        }
      } catch {
        // PUT succeeded. Host, port, user id, name, driver, and default stay as shown.
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="sftp-private-key-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>SFTP private key</h3>
      {sftp && (
        <div data-testid="sftp-private-key-summary">
          <p>
            Name: <span data-testid="sftp-private-key-name">{shown(name)}</span>
          </p>
          <p>
            Driver: <span data-testid="sftp-private-key-driver">{shown(driver)}</span>
          </p>
          <p>
            Host: <span data-testid="sftp-private-key-host">{shown(host)}</span>
          </p>
          <p>
            Port: <span data-testid="sftp-private-key-port">{shown(port)}</span>
          </p>
          <p>
            User ID: <span data-testid="sftp-private-key-user">{shown(user)}</span>
          </p>
          <p>
            Default:{" "}
            <span data-testid="sftp-private-key-default">{shown(defaultFlag)}</span>
          </p>
          <p data-testid="sftp-private-key-status" role="status">
            Private key: {SFTP_PRIVATE_KEY_NEVER_SHOWN}
          </p>
        </div>
      )}
      {notSftp && (
        <p data-testid="sftp-private-key-not-sftp" role="status">
          {SFTP_PRIVATE_KEY_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="sftp-private-key-form">
          <div style={formRowStyle}>
            <label htmlFor="sftp-private-key-input">* New private key</label>
            <input
              id="sftp-private-key-input"
              data-testid="sftp-private-key-input"
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
        <p style={errorStyle} role="alert" data-testid="sftp-private-key-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="sftp-private-key-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set SFTP private key
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="sftp-private-key-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save SFTP private key
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="sftp-private-key-cancel"
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
