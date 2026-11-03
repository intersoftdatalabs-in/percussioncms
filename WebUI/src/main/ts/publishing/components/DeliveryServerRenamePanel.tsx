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
import { serverDriver, serverProperty } from "../deliveryServerFtpHost";
import {
  buildDeliveryServerRenameBody,
  DELIVERY_SERVER_NAME_MAX_LENGTH,
  DELIVERY_SERVER_NAME_REQUIRED,
  deliveryServerDisplayName,
  deliveryServerRenameHttpFailure,
  isDefaultDeliveryServer,
  unwrapPublishServer,
  validateDeliveryServerRenameName,
  withUpdatedDeliveryServerName,
} from "../deliveryServerRename";
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
    const kind = deliveryServerRenameHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, DELIVERY_SERVER_NAME_REQUIRED);
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

function defaultLabel(server: PublishServer): string {
  return isDefaultDeliveryServer(server) ? "Yes" : "No";
}

/**
 * Rename an existing delivery server. The PUT body is only serverName.
 * Driver, type, default flag, folder, and other properties stay. Password is
 * never rendered. The new name is shown only after a successful save.
 */
export function DeliveryServerRenamePanel({
  siteId,
  serverId,
  siblingNames = [],
  onRenamed,
}: {
  siteId: string | number;
  serverId: string;
  siblingNames?: readonly string[];
  onRenamed?: (name: string) => void;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setDraft("");
    setError(null);
    setDirty(false);
  }, [siteId, serverId, setDirty]);

  if (!serverId) {
    return null;
  }

  const name = loaded ? deliveryServerDisplayName(loaded) : "";
  const driver = loaded ? serverDriver(loaded) : "";
  const type = loaded ? String(loaded.type ?? "").trim() : "";
  const folder = loaded ? serverProperty(loaded, "folder") : "";
  const defaultFlag = loaded ? defaultLabel(loaded) : "";

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const server = unwrapPublishServer(await getServer(siteId, serverId));
      setLoaded(server);
      setDraft(deliveryServerDisplayName(server));
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
    setDraft(name);
    setEditing(false);
  }

  async function save(): Promise<void> {
    if (busy) {
      return;
    }
    const validated = validateDeliveryServerRenameName(draft, name, siblingNames);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildDeliveryServerRenameBody(validated.name));
      setDirty(false);
      setEditing(false);
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        setLoaded(refreshed);
        setDraft(deliveryServerDisplayName(refreshed));
      } catch {
        if (loaded) {
          const next = withUpdatedDeliveryServerName(loaded, validated.name);
          setLoaded(next);
          setDraft(validated.name);
        }
      }
      onRenamed?.(validated.name);
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="delivery-server-rename-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>Rename delivery server</h3>
      {loaded && (
        <div data-testid="delivery-server-rename-summary">
          <p>
            Name:{" "}
            <span data-testid="delivery-server-rename-current">{shown(name)}</span>
          </p>
          <p>
            Driver:{" "}
            <span data-testid="delivery-server-rename-driver">{shown(driver)}</span>
          </p>
          <p>
            Type: <span data-testid="delivery-server-rename-type">{shown(type)}</span>
          </p>
          <p>
            Default:{" "}
            <span data-testid="delivery-server-rename-default">{shown(defaultFlag)}</span>
          </p>
          <p>
            Folder:{" "}
            <span data-testid="delivery-server-rename-folder">{shown(folder)}</span>
          </p>
        </div>
      )}
      {editing && (
        <div data-testid="delivery-server-rename-form">
          <div style={formRowStyle}>
            <label htmlFor="delivery-server-rename-input">* Name</label>
            <input
              id="delivery-server-rename-input"
              data-testid="delivery-server-rename-input"
              value={draft}
              autoComplete="off"
              maxLength={DELIVERY_SERVER_NAME_MAX_LENGTH + 1}
              onChange={(event) => {
                setDraft(event.target.value);
                setDirty(event.target.value.trim() !== name);
              }}
            />
          </div>
        </div>
      )}
      {error && (
        <p
          style={errorStyle}
          role="alert"
          data-testid="delivery-server-rename-error"
        >
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="delivery-server-rename-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Rename
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="delivery-server-rename-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save name
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="delivery-server-rename-cancel"
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
