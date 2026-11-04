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
  buildOracleSidBody,
  ORACLE_SID_DRIVER_ONLY,
  ORACLE_SID_REQUIRED,
  isOracleDeliveryDriver,
  oracleSidHttpFailure,
  validateOracleSid,
  withUpdatedOracleSid,
} from "../deliveryServerDatabaseSid";
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
    const kind = oracleSidHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, ORACLE_SID_REQUIRED);
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
 * Set the SID on an Oracle delivery server. The PUT body is only sid. Host,
 * port, userid, name, driver, and default flag stay visible and are not sent.
 * Password is never rendered.
 */
export function DatabaseDeliverySidPanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [notOracle, setNotOracle] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setNotOracle(false);
    setDraft("");
    setError(null);
    setDirty(false);
  }, [siteId, serverId, setDirty]);

  if (!serverId) {
    return null;
  }

  const oracle = loaded != null && isOracleDeliveryDriver(serverDriver(loaded));
  const driver = oracle && loaded ? serverDriver(loaded) : "";
  const host = oracle && loaded ? serverProperty(loaded, "server") : "";
  const port = oracle && loaded ? serverProperty(loaded, "port") : "";
  const user = oracle && loaded ? serverProperty(loaded, "userid") : "";
  const sid = oracle && loaded ? serverProperty(loaded, "sid") : "";
  const name = oracle && loaded ? deliveryServerDisplayName(loaded) : "";
  const defaultFlag =
    oracle && loaded ? (isDefaultDeliveryServer(loaded) ? "yes" : "no") : "";

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotOracle(false);
    try {
      const raw = await getServer(siteId, serverId);
      const server = unwrapPublishServer(raw);
      if (!isOracleDeliveryDriver(serverDriver(server))) {
        setLoaded(null);
        setEditing(false);
        setNotOracle(true);
        setDirty(false);
        return;
      }
      setLoaded(server);
      setDraft(serverProperty(server, "sid"));
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
    setDraft(sid);
    setEditing(false);
  }

  async function save(): Promise<void> {
    if (busy || !oracle) {
      return;
    }
    const validated = validateOracleSid(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildOracleSidBody(validated.sid));
      setDirty(false);
      setEditing(false);
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isOracleDeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
          setDraft(serverProperty(refreshed, "sid"));
          return;
        }
      } catch {
        // PUT succeeded. Keep the previous host, port, and user and show the new SID.
      }
      if (loaded) {
        const next = withUpdatedOracleSid(loaded, validated.sid);
        setLoaded(next);
        setDraft(validated.sid);
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="db-sid-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>Oracle SID</h3>
      {oracle && (
        <div data-testid="db-sid-summary">
          <p>
            Name: <span data-testid="db-sid-name">{shown(name)}</span>
          </p>
          <p>
            Driver: <span data-testid="db-sid-driver">{shown(driver)}</span>
          </p>
          <p>
            Host: <span data-testid="db-sid-host">{shown(host)}</span>
          </p>
          <p>
            Port: <span data-testid="db-sid-port">{shown(port)}</span>
          </p>
          <p>
            User ID: <span data-testid="db-sid-user">{shown(user)}</span>
          </p>
          <p>
            SID: <span data-testid="db-sid-current">{shown(sid)}</span>
          </p>
          <p>
            Default:{" "}
            <span data-testid="db-sid-default">{shown(defaultFlag)}</span>
          </p>
        </div>
      )}
      {notOracle && (
        <p data-testid="db-sid-not-oracle" role="status">
          {ORACLE_SID_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="db-sid-form">
          <div style={formRowStyle}>
            <label htmlFor="db-sid-input">* SID</label>
            <input
              id="db-sid-input"
              data-testid="db-sid-input"
              value={draft}
              autoComplete="off"
              onChange={(event) => {
                setDraft(event.target.value);
                setDirty(event.target.value.trim() !== sid.trim());
              }}
            />
          </div>
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert" data-testid="db-sid-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="db-sid-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set Oracle SID
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="db-sid-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save Oracle SID
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="db-sid-cancel"
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
