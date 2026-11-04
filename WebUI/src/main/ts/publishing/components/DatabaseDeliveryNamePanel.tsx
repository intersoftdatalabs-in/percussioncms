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
  buildDatabaseNameBody,
  DATABASE_NAME_DRIVER_ONLY,
  DATABASE_NAME_REQUIRED,
  databaseNameHttpFailure,
  isMysqlOrMssqlDeliveryDriver,
  validateDatabaseName,
  withUpdatedDatabaseName,
} from "../deliveryServerDatabaseName";
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
    const kind = databaseNameHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, DATABASE_NAME_REQUIRED);
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
 * Set the database name on a MySQL or MSSQL delivery server. The PUT body is
 * only database. Host, port, userid, name, driver, default flag, and Oracle SID
 * stay visible and are not sent. Password is never rendered.
 */
export function DatabaseDeliveryNamePanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [notMysqlMssql, setNotMysqlMssql] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setNotMysqlMssql(false);
    setDraft("");
    setError(null);
    setDirty(false);
  }, [siteId, serverId, setDirty]);

  if (!serverId) {
    return null;
  }

  const mysqlMssql =
    loaded != null && isMysqlOrMssqlDeliveryDriver(serverDriver(loaded));
  const driver = mysqlMssql && loaded ? serverDriver(loaded) : "";
  const host = mysqlMssql && loaded ? serverProperty(loaded, "server") : "";
  const port = mysqlMssql && loaded ? serverProperty(loaded, "port") : "";
  const user = mysqlMssql && loaded ? serverProperty(loaded, "userid") : "";
  const databaseName =
    mysqlMssql && loaded ? serverProperty(loaded, "database") : "";
  const name = mysqlMssql && loaded ? deliveryServerDisplayName(loaded) : "";
  const defaultFlag =
    mysqlMssql && loaded ? (isDefaultDeliveryServer(loaded) ? "yes" : "no") : "";

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotMysqlMssql(false);
    try {
      const raw = await getServer(siteId, serverId);
      const server = unwrapPublishServer(raw);
      if (!isMysqlOrMssqlDeliveryDriver(serverDriver(server))) {
        setLoaded(null);
        setEditing(false);
        setNotMysqlMssql(true);
        setDirty(false);
        return;
      }
      setLoaded(server);
      setDraft(serverProperty(server, "database"));
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
    setDraft(databaseName);
    setEditing(false);
  }

  async function save(): Promise<void> {
    if (busy || !mysqlMssql) {
      return;
    }
    const validated = validateDatabaseName(draft);
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
        buildDatabaseNameBody(validated.databaseName),
      );
      setDirty(false);
      setEditing(false);
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isMysqlOrMssqlDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
          setDraft(serverProperty(refreshed, "database"));
          return;
        }
      } catch {
        // PUT succeeded. Keep the previous host, port, and user and show the new name.
      }
      if (loaded) {
        const next = withUpdatedDatabaseName(loaded, validated.databaseName);
        setLoaded(next);
        setDraft(validated.databaseName);
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="db-dbname-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>Database name</h3>
      {mysqlMssql && (
        <div data-testid="db-dbname-summary">
          <p>
            Name: <span data-testid="db-dbname-name">{shown(name)}</span>
          </p>
          <p>
            Driver: <span data-testid="db-dbname-driver">{shown(driver)}</span>
          </p>
          <p>
            Host: <span data-testid="db-dbname-host">{shown(host)}</span>
          </p>
          <p>
            Port: <span data-testid="db-dbname-port">{shown(port)}</span>
          </p>
          <p>
            User ID: <span data-testid="db-dbname-user">{shown(user)}</span>
          </p>
          <p>
            Database:{" "}
            <span data-testid="db-dbname-current">{shown(databaseName)}</span>
          </p>
          <p>
            Default:{" "}
            <span data-testid="db-dbname-default">{shown(defaultFlag)}</span>
          </p>
        </div>
      )}
      {notMysqlMssql && (
        <p data-testid="db-dbname-not-database" role="status">
          {DATABASE_NAME_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="db-dbname-form">
          <div style={formRowStyle}>
            <label htmlFor="db-dbname-input">* Database</label>
            <input
              id="db-dbname-input"
              data-testid="db-dbname-input"
              value={draft}
              autoComplete="off"
              onChange={(event) => {
                setDraft(event.target.value);
                setDirty(event.target.value.trim() !== databaseName.trim());
              }}
            />
          </div>
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="db-dbname-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="db-dbname-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set database name
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="db-dbname-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save database name
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="db-dbname-cancel"
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
