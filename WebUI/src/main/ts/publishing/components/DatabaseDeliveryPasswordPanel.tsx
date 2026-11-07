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
  databaseNameOrSid,
  isDatabaseDeliveryDriver,
} from "../deliveryServerDatabaseHost";
import {
  buildDatabasePasswordBody,
  DATABASE_PASSWORD_DRIVER_ONLY,
  DATABASE_PASSWORD_NEVER_SHOWN,
  DATABASE_PASSWORD_REQUIRED,
  databasePasswordHttpFailure,
  validateDatabasePassword,
} from "../deliveryServerDatabasePassword";
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
    const kind = databasePasswordHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, DATABASE_PASSWORD_REQUIRED);
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
 * Set the password on a database delivery server. The PUT body is only the password
 * (base64-encoded). Host, port, user id, and database/sid stay visible and are not sent.
 * The stored secret is never rendered: the input always opens empty and is cleared
 * after a successful save, even though GET responses carry a masked placeholder.
 */
export function DatabaseDeliveryPasswordPanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [notDatabase, setNotDatabase] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setNotDatabase(false);
    setDraft("");
    setError(null);
    setDirty(false);
  }, [siteId, serverId, setDirty]);

  if (!serverId) {
    return null;
  }

  const database =
    loaded != null && isDatabaseDeliveryDriver(serverDriver(loaded));
  const driver = database && loaded ? serverDriver(loaded) : "";
  const host = database && loaded ? serverProperty(loaded, "server") : "";
  const port = database && loaded ? serverProperty(loaded, "port") : "";
  const user = database && loaded ? serverProperty(loaded, "userid") : "";
  const databaseOrSid =
    database && loaded ? databaseNameOrSid(loaded, driver) : "";

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotDatabase(false);
    try {
      const raw = await getServer(siteId, serverId);
      const server = unwrapPublishServer(raw);
      if (!isDatabaseDeliveryDriver(serverDriver(server))) {
        setLoaded(null);
        setEditing(false);
        setNotDatabase(true);
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
    if (busy || !database) {
      return;
    }
    const validated = validateDatabasePassword(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildDatabasePasswordBody(validated.password));
      setDirty(false);
      setEditing(false);
      // Never write the typed secret into loaded state: the summary shows a
      // fixed status and GET responses carry only a masked placeholder.
      setDraft("");
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isDatabaseDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
        }
      } catch {
        // PUT succeeded. Host, port, user id, and database/sid shown stay as they were.
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="db-password-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>Database password</h3>
      {database && (
        <div data-testid="db-password-summary">
          <p>
            Host: <span data-testid="db-password-host">{shown(host)}</span>
          </p>
          <p>
            Port: <span data-testid="db-password-port">{shown(port)}</span>
          </p>
          <p>
            User ID: <span data-testid="db-password-user">{shown(user)}</span>
          </p>
          <p>
            Database / SID:{" "}
            <span data-testid="db-password-database">{shown(databaseOrSid)}</span>
          </p>
          <p data-testid="db-password-status" role="status">
            Password: {DATABASE_PASSWORD_NEVER_SHOWN}
          </p>
        </div>
      )}
      {notDatabase && (
        <p data-testid="db-password-not-database" role="status">
          {DATABASE_PASSWORD_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="db-password-form">
          <div style={formRowStyle}>
            <label htmlFor="db-password-input">* New password</label>
            <input
              id="db-password-input"
              data-testid="db-password-input"
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
        <p style={errorStyle} role="alert" data-testid="db-password-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="db-password-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set password
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="db-password-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save password
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="db-password-cancel"
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
