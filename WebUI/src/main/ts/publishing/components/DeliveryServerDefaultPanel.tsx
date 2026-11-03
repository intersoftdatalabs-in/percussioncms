/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
  buildDeliveryServerDefaultBody,
  DELIVERY_SERVER_ALREADY_DEFAULT,
  DELIVERY_SERVER_DEFAULT_REQUIRED,
  deliveryServerDefaultHttpFailure,
  isDefaultDeliveryServer,
  shouldSkipDefaultWrite,
  unwrapPublishServer,
  withUpdatedDeliveryServerDefault,
} from "../deliveryServerDefault";
import { deliveryServerDisplayName } from "../deliveryServerRename";
import { useDirtyForm } from "../dirtyFormContext";
import type { PublishServer } from "../types";
import {
  buttonStyle,
  errorStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

function saveErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    const kind = deliveryServerDefaultHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, DELIVERY_SERVER_DEFAULT_REQUIRED);
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
 * Mark an existing delivery server as the site default. The PUT body is only
 * isDefault: true. Name, driver, type, and properties stay. Password is never
 * rendered. A server that is already default is not a write. Other servers lose
 * the default badge only after success.
 */
export function DeliveryServerDefaultPanel({
  siteId,
  serverId,
  onDefaulted,
}: {
  siteId: string | number;
  serverId: string;
  onDefaulted?: () => void;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
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
  const alreadyDefault = loaded ? shouldSkipDefaultWrite(loaded) : false;

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const server = unwrapPublishServer(await getServer(siteId, serverId));
      setLoaded(server);
      if (shouldSkipDefaultWrite(server)) {
        setEditing(false);
        setDirty(false);
        setError(DELIVERY_SERVER_ALREADY_DEFAULT);
        return;
      }
      setEditing(true);
      setDirty(true);
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
    setEditing(false);
  }

  async function save(): Promise<void> {
    if (busy || !loaded) {
      return;
    }
    if (shouldSkipDefaultWrite(loaded)) {
      setError(DELIVERY_SERVER_ALREADY_DEFAULT);
      setEditing(false);
      setDirty(false);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildDeliveryServerDefaultBody());
      setDirty(false);
      setEditing(false);
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        setLoaded(refreshed);
      } catch {
        setLoaded(withUpdatedDeliveryServerDefault(loaded, true));
      }
      onDefaulted?.();
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="delivery-server-default-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>Mark delivery server as default</h3>
      {loaded && (
        <div data-testid="delivery-server-default-summary">
          <p>
            Name:{" "}
            <span data-testid="delivery-server-default-name">{shown(name)}</span>
          </p>
          <p>
            Driver:{" "}
            <span data-testid="delivery-server-default-driver">{shown(driver)}</span>
          </p>
          <p>
            Type: <span data-testid="delivery-server-default-type">{shown(type)}</span>
          </p>
          <p>
            Default:{" "}
            <span data-testid="delivery-server-default-flag">{shown(defaultFlag)}</span>
          </p>
          <p>
            Folder:{" "}
            <span data-testid="delivery-server-default-folder">{shown(folder)}</span>
          </p>
        </div>
      )}
      {editing && (
        <p data-testid="delivery-server-default-form">
          Mark this server as the site default? Other servers lose the default
          badge only after a successful save.
        </p>
      )}
      {error && (
        <p
          style={errorStyle}
          role="alert"
          data-testid="delivery-server-default-error"
        >
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="delivery-server-default-open"
            disabled={busy || alreadyDefault}
            onClick={() => void open()}
          >
            Mark as default
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="delivery-server-default-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save default
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="delivery-server-default-cancel"
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
