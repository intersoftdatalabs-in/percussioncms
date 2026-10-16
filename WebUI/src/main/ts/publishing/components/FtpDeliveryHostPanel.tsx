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
  buildFtpHostBody,
  FTP_HOST_DRIVER_ONLY,
  FTP_HOST_REQUIRED,
  ftpHostHttpFailure,
  isFtpDeliveryDriver,
  serverDriver,
  serverProperty,
  unwrapPublishServer,
  validateFtpHost,
  withUpdatedHost,
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
    const kind = ftpHostHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, FTP_HOST_REQUIRED);
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
 * Set the host on an FTP delivery server. The PUT body is only serverip.
 * Port, user, and folder stay visible and are not sent. Password is never rendered.
 */
export function FtpDeliveryHostPanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [notFtp, setNotFtp] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setNotFtp(false);
    setDraft("");
    setError(null);
    setDirty(false);
  }, [siteId, serverId, setDirty]);

  if (!serverId) {
    return null;
  }

  const ftp = loaded != null && isFtpDeliveryDriver(serverDriver(loaded));
  const host = ftp && loaded ? serverProperty(loaded, "serverip") : "";
  const port = ftp && loaded ? serverProperty(loaded, "port") : "";
  const user = ftp && loaded ? serverProperty(loaded, "userid") : "";
  const folder = ftp && loaded ? serverProperty(loaded, "folder") : "";

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotFtp(false);
    try {
      const raw = await getServer(siteId, serverId);
      const server = unwrapPublishServer(raw);
      if (!isFtpDeliveryDriver(serverDriver(server))) {
        setLoaded(null);
        setEditing(false);
        setNotFtp(true);
        setDirty(false);
        return;
      }
      setLoaded(server);
      setDraft(serverProperty(server, "serverip"));
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
    setDraft(host);
    setEditing(false);
  }

  async function save(): Promise<void> {
    if (busy || !ftp) {
      return;
    }
    const validated = validateFtpHost(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildFtpHostBody(validated.host));
      setDirty(false);
      setEditing(false);
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isFtpDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
          setDraft(serverProperty(refreshed, "serverip"));
          return;
        }
      } catch {
        // PUT succeeded. Keep the previous port, user, and folder and show the new host.
      }
      if (loaded) {
        const next = withUpdatedHost(loaded, validated.host);
        setLoaded(next);
        setDraft(validated.host);
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="ftp-host-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>FTP host</h3>
      {ftp && (
        <div data-testid="ftp-host-summary">
          <p>
            Host: <span data-testid="ftp-host-current">{shown(host)}</span>
          </p>
          <p>
            Port: <span data-testid="ftp-host-port">{shown(port)}</span>
          </p>
          <p>
            User ID: <span data-testid="ftp-host-user">{shown(user)}</span>
          </p>
          <p>
            Folder: <span data-testid="ftp-host-folder">{shown(folder)}</span>
          </p>
        </div>
      )}
      {notFtp && (
        <p data-testid="ftp-host-not-ftp" role="status">
          {FTP_HOST_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="ftp-host-form">
          <div style={formRowStyle}>
            <label htmlFor="ftp-host-input">* Host</label>
            <input
              id="ftp-host-input"
              data-testid="ftp-host-input"
              value={draft}
              autoComplete="off"
              onChange={(event) => {
                setDraft(event.target.value);
                setDirty(event.target.value.trim() !== host.trim());
              }}
            />
          </div>
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="ftp-host-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="ftp-host-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set host
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="ftp-host-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save host
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="ftp-host-cancel"
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
