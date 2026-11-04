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
  buildMssqlOwnerBody,
  MSSQL_OWNER_DRIVER_ONLY,
  MSSQL_OWNER_REQUIRED,
  isMssqlDeliveryDriver,
  mssqlOwnerHttpFailure,
  validateMssqlOwner,
  withUpdatedMssqlOwner,
} from "../deliveryServerDatabaseOwner";
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
    const kind = mssqlOwnerHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, MSSQL_OWNER_REQUIRED);
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
 * Set the owner on an MSSQL delivery server. The PUT body is only owner. Host,
 * port, userid, database, name, driver, and default flag stay visible and are
 * not sent. Password is never rendered.
 */
export function DatabaseDeliveryOwnerPanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [notMssql, setNotMssql] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setNotMssql(false);
    setDraft("");
    setError(null);
    setDirty(false);
  }, [siteId, serverId, setDirty]);

  if (!serverId) {
    return null;
  }

  const mssql = loaded != null && isMssqlDeliveryDriver(serverDriver(loaded));
  const driver = mssql && loaded ? serverDriver(loaded) : "";
  const host = mssql && loaded ? serverProperty(loaded, "server") : "";
  const port = mssql && loaded ? serverProperty(loaded, "port") : "";
  const user = mssql && loaded ? serverProperty(loaded, "userid") : "";
  const database = mssql && loaded ? serverProperty(loaded, "database") : "";
  const owner = mssql && loaded ? serverProperty(loaded, "owner") : "";
  const name = mssql && loaded ? deliveryServerDisplayName(loaded) : "";
  const defaultFlag =
    mssql && loaded ? (isDefaultDeliveryServer(loaded) ? "yes" : "no") : "";

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotMssql(false);
    try {
      const raw = await getServer(siteId, serverId);
      const server = unwrapPublishServer(raw);
      if (!isMssqlDeliveryDriver(serverDriver(server))) {
        setLoaded(null);
        setEditing(false);
        setNotMssql(true);
        setDirty(false);
        return;
      }
      setLoaded(server);
      setDraft(serverProperty(server, "owner"));
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
    setDraft(owner);
    setEditing(false);
  }

  async function save(): Promise<void> {
    if (busy || !mssql) {
      return;
    }
    const validated = validateMssqlOwner(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildMssqlOwnerBody(validated.owner));
      setDirty(false);
      setEditing(false);
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isMssqlDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
          setDraft(serverProperty(refreshed, "owner"));
          return;
        }
      } catch {
        // PUT succeeded. Keep the previous host, port, user, and database and show the new owner.
      }
      if (loaded) {
        const next = withUpdatedMssqlOwner(loaded, validated.owner);
        setLoaded(next);
        setDraft(validated.owner);
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="db-owner-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>MSSQL owner</h3>
      {mssql && (
        <div data-testid="db-owner-summary">
          <p>
            Name: <span data-testid="db-owner-name">{shown(name)}</span>
          </p>
          <p>
            Driver: <span data-testid="db-owner-driver">{shown(driver)}</span>
          </p>
          <p>
            Host: <span data-testid="db-owner-host">{shown(host)}</span>
          </p>
          <p>
            Port: <span data-testid="db-owner-port">{shown(port)}</span>
          </p>
          <p>
            User ID: <span data-testid="db-owner-user">{shown(user)}</span>
          </p>
          <p>
            Database:{" "}
            <span data-testid="db-owner-database">{shown(database)}</span>
          </p>
          <p>
            Owner: <span data-testid="db-owner-current">{shown(owner)}</span>
          </p>
          <p>
            Default:{" "}
            <span data-testid="db-owner-default">{shown(defaultFlag)}</span>
          </p>
        </div>
      )}
      {notMssql && (
        <p data-testid="db-owner-not-mssql" role="status">
          {MSSQL_OWNER_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="db-owner-form">
          <div style={formRowStyle}>
            <label htmlFor="db-owner-input">* Owner</label>
            <input
              id="db-owner-input"
              data-testid="db-owner-input"
              value={draft}
              autoComplete="off"
              onChange={(event) => {
                setDraft(event.target.value);
                setDirty(event.target.value.trim() !== owner.trim());
              }}
            />
          </div>
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="db-owner-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="db-owner-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set MSSQL owner
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="db-owner-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save MSSQL owner
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="db-owner-cancel"
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
