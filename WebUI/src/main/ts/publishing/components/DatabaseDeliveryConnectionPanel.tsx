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
  buildDatabaseConnectionBody,
  databaseConnectionHttpFailure,
  validateDatabaseConnection,
  withUpdatedDatabaseConnection,
} from "../deliveryServerDatabaseConnection";
import {
  databaseNameOrSid,
  isDatabaseDeliveryDriver,
} from "../deliveryServerDatabaseHost";
import { DATABASE_PASSWORD_NEVER_SHOWN } from "../deliveryServerDatabasePassword";
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
    const kind = databaseConnectionHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, message(MSG.PUBLISH_DELIVERY_DATABASE_HOST_REQUIRED));
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
 * Set the host, user id, and password on a database delivery server in one
 * save. The PUT body carries only server, userid, and password (base64).
 * Port, database/sid, name, driver, and default flag stay visible and are
 * not sent. The stored secret is never rendered: the password input always
 * opens empty and the typed secret is cleared after a successful save, even
 * though GET responses carry a masked placeholder.
 */
export function DatabaseDeliveryConnectionPanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [notDatabase, setNotDatabase] = useState(false);
  const [hostDraft, setHostDraft] = useState("");
  const [userDraft, setUserDraft] = useState("");
  const [secretDraft, setSecretDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setNotDatabase(false);
    setHostDraft("");
    setUserDraft("");
    setSecretDraft("");
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

  function markDirty(nextHost: string, nextUser: string, nextSecret: string): void {
    setDirty(
      nextHost.trim() !== host.trim() ||
        nextUser.trim() !== user.trim() ||
        nextSecret.length > 0,
    );
  }

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
      setHostDraft(serverProperty(server, "server"));
      setUserDraft(serverProperty(server, "userid"));
      setSecretDraft("");
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
    setHostDraft(host);
    setUserDraft(user);
    setSecretDraft("");
    setEditing(false);
  }

  async function save(): Promise<void> {
    if (busy || !database) {
      return;
    }
    const validated = validateDatabaseConnection({
      host: hostDraft,
      userId: userDraft,
      password: secretDraft,
    });
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(
        siteId,
        serverId,
        buildDatabaseConnectionBody(
          validated.host,
          validated.userId,
          validated.password,
        ),
      );
      setDirty(false);
      setEditing(false);
      // Never write the typed secret into loaded state: the summary shows a
      // fixed status and GET responses carry only a masked placeholder.
      setSecretDraft("");
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isDatabaseDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
          setHostDraft(serverProperty(refreshed, "server"));
          setUserDraft(serverProperty(refreshed, "userid"));
          return;
        }
      } catch {
        // PUT succeeded. Keep the previous port and database/sid and show the new host and user.
      }
      if (loaded) {
        const next = withUpdatedDatabaseConnection(
          loaded,
          validated.host,
          validated.userId,
        );
        setLoaded(next);
        setHostDraft(validated.host);
        setUserDraft(validated.userId);
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="db-conn-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>{message(MSG.PUBLISH_DELIVERY_TITLE_DB_CONNECTION)}</h3>
      {database && (
        <div data-testid="db-conn-summary">
          <p>
            {message(MSG.PUBLISH_DELIVERY_SUMMARY_NAME)}: <span data-testid="db-conn-name">{shown(name)}</span>
          </p>
          <p>
            {message(MSG.PUBLISH_DRIVER)}: <span data-testid="db-conn-driver">{shown(driver)}</span>
          </p>
          <p>
            {message(MSG.PUBLISH_DELIVERY_SUMMARY_HOST)}: <span data-testid="db-conn-host">{shown(host)}</span>
          </p>
          <p>
            {message(MSG.PUBLISH_DELIVERY_SUMMARY_PORT)}: <span data-testid="db-conn-port">{shown(port)}</span>
          </p>
          <p>
            {message(MSG.PUBLISH_DELIVERY_SUMMARY_USER_ID)}: <span data-testid="db-conn-user">{shown(user)}</span>
          </p>
          <p>
            {message(MSG.PUBLISH_DELIVERY_SUMMARY_DATABASE_SID)}:{" "}
            <span data-testid="db-conn-database">{shown(databaseOrSid)}</span>
          </p>
          <p>
            {message(MSG.PUBLISH_DELIVERY_SUMMARY_DEFAULT)}: <span data-testid="db-conn-default">{shown(defaultFlag)}</span>
          </p>
          <p data-testid="db-conn-password-status" role="status">
            Password: {message(MSG.PUBLISH_DELIVERY_DATABASE_PASSWORD_NEVER_SHOWN)}
          </p>
        </div>
      )}
      {notDatabase && (
        <p data-testid="db-conn-not-database" role="status">
          {message(MSG.PUBLISH_DELIVERY_DATABASE_CONNECTION_DRIVER_ONLY)}
        </p>
      )}
      {editing && (
        <div data-testid="db-conn-form">
          <div style={formRowStyle}>
            <label htmlFor="db-conn-host-input">{message(MSG.PUBLISH_DELIVERY_LABEL_HOST)}</label>
            <input
              id="db-conn-host-input"
              data-testid="db-conn-host-input"
              value={hostDraft}
              autoComplete="off"
              onChange={(event) => {
                const next = event.target.value;
                setHostDraft(next);
                markDirty(next, userDraft, secretDraft);
              }}
            />
          </div>
          <div style={formRowStyle}>
            <label htmlFor="db-conn-user-input">{message(MSG.PUBLISH_DELIVERY_LABEL_USER_ID)}</label>
            <input
              id="db-conn-user-input"
              data-testid="db-conn-user-input"
              value={userDraft}
              autoComplete="off"
              onChange={(event) => {
                const next = event.target.value;
                setUserDraft(next);
                markDirty(hostDraft, next, secretDraft);
              }}
            />
          </div>
          <div style={formRowStyle}>
            <label htmlFor="db-conn-password-input">{message(MSG.PUBLISH_DELIVERY_LABEL_NEW_PASSWORD)}</label>
            <input
              id="db-conn-password-input"
              data-testid="db-conn-password-input"
              type="password"
              autoComplete="new-password"
              value={secretDraft}
              onChange={(event) => {
                const next = event.target.value;
                setSecretDraft(next);
                markDirty(hostDraft, userDraft, next);
              }}
            />
          </div>
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="db-conn-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="db-conn-open"
            disabled={busy}
            onClick={() => void open()}
          >
            {message(MSG.PUBLISH_DELIVERY_ACTION_SET_CONNECTION)}
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="db-conn-save"
              disabled={busy}
              onClick={() => void save()}
            >
              {message(MSG.PUBLISH_DELIVERY_ACTION_SAVE_CONNECTION)}
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="db-conn-cancel"
              disabled={busy}
              onClick={cancel}
            >
              {message(MSG.PUBLISH_DELIVERY_ACTION_CANCEL)}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
