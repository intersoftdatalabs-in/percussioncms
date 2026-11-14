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
  buildFtpsUserBody,
  FTPS_USER_DRIVER_ONLY,
  FTPS_USER_REQUIRED,
  ftpsUserHttpFailure,
  validateFtpsUser,
  withUpdatedFtpsUser,
} from "../deliveryServerFtpsUser";
import { isFtpsDeliveryDriver } from "../deliveryServerFtpsHost";
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
    const kind = ftpsUserHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, FTPS_USER_REQUIRED);
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
 * Set the user id on an FTPS delivery server. The PUT body is only userid.
 * Host, port, and folder stay visible and are not sent. The passphrase and
 * private key are never rendered.
 */
export function FtpsDeliveryUserPanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [notFtps, setNotFtps] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setNotFtps(false);
    setDraft("");
    setError(null);
    setDirty(false);
  }, [siteId, serverId, setDirty]);

  if (!serverId) {
    return null;
  }

  const ftps = loaded != null && isFtpsDeliveryDriver(serverDriver(loaded));
  const host = ftps && loaded ? serverProperty(loaded, "serverip") : "";
  const port = ftps && loaded ? serverProperty(loaded, "port") : "";
  const user = ftps && loaded ? serverProperty(loaded, "userid") : "";
  const folder = ftps && loaded ? serverProperty(loaded, "folder") : "";
  const name = ftps && loaded ? deliveryServerDisplayName(loaded) : "";
  const defaultFlag =
    ftps && loaded ? (isDefaultDeliveryServer(loaded) ? "yes" : "no") : "";

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotFtps(false);
    try {
      const raw = await getServer(siteId, serverId);
      const server = unwrapPublishServer(raw);
      if (!isFtpsDeliveryDriver(serverDriver(server))) {
        setLoaded(null);
        setEditing(false);
        setNotFtps(true);
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
    if (busy || !ftps) {
      return;
    }
    const validated = validateFtpsUser(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildFtpsUserBody(validated.userId));
      setDirty(false);
      setEditing(false);
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isFtpsDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
          setDraft(serverProperty(refreshed, "userid"));
          return;
        }
      } catch {
        // PUT succeeded. Keep the previous host, port, and folder and show the new user id.
      }
      if (loaded) {
        const next = withUpdatedFtpsUser(loaded, validated.userId);
        setLoaded(next);
        setDraft(validated.userId);
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="ftps-user-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>FTPS user id</h3>
      {ftps && (
        <div data-testid="ftps-user-summary">
          <p>
            Name: <span data-testid="ftps-user-name">{shown(name)}</span>
          </p>
          <p>
            Host: <span data-testid="ftps-user-host">{shown(host)}</span>
          </p>
          <p>
            Port: <span data-testid="ftps-user-port">{shown(port)}</span>
          </p>
          <p>
            User ID: <span data-testid="ftps-user-current">{shown(user)}</span>
          </p>
          <p>
            Folder: <span data-testid="ftps-user-folder">{shown(folder)}</span>
          </p>
          <p>
            Default: <span data-testid="ftps-user-default">{shown(defaultFlag)}</span>
          </p>
        </div>
      )}
      {notFtps && (
        <p data-testid="ftps-user-not-ftps" role="status">
          {FTPS_USER_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="ftps-user-form">
          <div style={formRowStyle}>
            <label htmlFor="ftps-user-input">* User ID</label>
            <input
              id="ftps-user-input"
              data-testid="ftps-user-input"
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
        <p style={errorStyle} role="alert" data-testid="ftps-user-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="ftps-user-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set user id
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="ftps-user-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save user id
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="ftps-user-cancel"
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
