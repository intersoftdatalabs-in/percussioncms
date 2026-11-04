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
  buildDatabaseUserBody,
  DATABASE_USER_DRIVER_ONLY,
  DATABASE_USER_REQUIRED,
  databaseUserHttpFailure,
  validateDatabaseUser,
  withUpdatedDatabaseUser,
} from "../deliveryServerDatabaseUser";
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
    const kind = databaseUserHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, DATABASE_USER_REQUIRED);
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
 * Set the user id on a database delivery server. The PUT body is only userid.
 * Host, port, database/sid, name, driver, and default flag stay visible and
 * are not sent. Password is never rendered.
 */
export function DatabaseDeliveryUserPanel({
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
  const name = database && loaded ? deliveryServerDisplayName(loaded) : "";
  const defaultFlag =
    database && loaded ? (isDefaultDeliveryServer(loaded) ? "yes" : "no") : "";

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
    if (busy || !database) {
      return;
    }
    const validated = validateDatabaseUser(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildDatabaseUserBody(validated.userId));
      setDirty(false);
      setEditing(false);
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isDatabaseDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
          setDraft(serverProperty(refreshed, "userid"));
          return;
        }
      } catch {
        // PUT succeeded. Keep the previous host, port, and database/sid and show the new user id.
      }
      if (loaded) {
        const next = withUpdatedDatabaseUser(loaded, validated.userId);
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
    <div data-testid="db-user-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>Database user id</h3>
      {database && (
        <div data-testid="db-user-summary">
          <p>
            Name: <span data-testid="db-user-name">{shown(name)}</span>
          </p>
          <p>
            Driver: <span data-testid="db-user-driver">{shown(driver)}</span>
          </p>
          <p>
            Host: <span data-testid="db-user-host">{shown(host)}</span>
          </p>
          <p>
            Port: <span data-testid="db-user-port">{shown(port)}</span>
          </p>
          <p>
            User ID: <span data-testid="db-user-current">{shown(user)}</span>
          </p>
          <p>
            Database / SID:{" "}
            <span data-testid="db-user-database">{shown(databaseOrSid)}</span>
          </p>
          <p>
            Default: <span data-testid="db-user-default">{shown(defaultFlag)}</span>
          </p>
        </div>
      )}
      {notDatabase && (
        <p data-testid="db-user-not-database" role="status">
          {DATABASE_USER_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="db-user-form">
          <div style={formRowStyle}>
            <label htmlFor="db-user-input">* User ID</label>
            <input
              id="db-user-input"
              data-testid="db-user-input"
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
        <p style={errorStyle} role="alert" data-testid="db-user-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="db-user-open"
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
              data-testid="db-user-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save user id
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="db-user-cancel"
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
