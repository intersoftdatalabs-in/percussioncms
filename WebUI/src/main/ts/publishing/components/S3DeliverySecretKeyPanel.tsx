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
import { isS3DeliveryDriver } from "../deliveryServerS3Bucket";
import {
  buildS3SecretKeyBody,
  S3_SECRET_KEY_DRIVER_ONLY,
  S3_SECRET_KEY_NEVER_SHOWN,
  S3_SECRET_KEY_REQUIRED,
  s3SecretKeyHttpFailure,
  validateS3SecretKey,
} from "../deliveryServerS3SecretKey";
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
    const kind = s3SecretKeyHttpFailure(err.status);
    if (kind === "bad_request") {
      return formatApiError(err, S3_SECRET_KEY_REQUIRED);
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
 * Set the secret key on an Amazon S3 delivery server. The PUT body is only
 * the secret key (base64-encoded). Bucket, region, access key, name, driver,
 * and default flag stay visible and are not sent. The stored secret is never
 * rendered: the input always opens empty and is cleared after a successful
 * save, even though GET responses carry the stored properties.
 */
export function S3DeliverySecretKeyPanel({
  siteId,
  serverId,
}: {
  siteId: string | number;
  serverId: string;
}): React.ReactElement | null {
  const [loaded, setLoaded] = useState<PublishServer | null>(null);
  const [editing, setEditing] = useState(false);
  const [notS3, setNotS3] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setLoaded(null);
    setEditing(false);
    setNotS3(false);
    setDraft("");
    setError(null);
    setDirty(false);
  }, [siteId, serverId, setDirty]);

  if (!serverId) {
    return null;
  }

  const s3 = loaded != null && isS3DeliveryDriver(serverDriver(loaded));
  const driver = s3 && loaded ? serverDriver(loaded) : "";
  const bucket = s3 && loaded ? serverProperty(loaded, "bucketlocation") : "";
  const region = s3 && loaded ? serverProperty(loaded, "region") : "";
  const name = s3 && loaded ? deliveryServerDisplayName(loaded) : "";
  const defaultFlag =
    s3 && loaded ? (isDefaultDeliveryServer(loaded) ? "yes" : "no") : "";

  async function open(): Promise<void> {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotS3(false);
    try {
      const raw = await getServer(siteId, serverId);
      const server = unwrapPublishServer(raw);
      if (!isS3DeliveryDriver(serverDriver(server))) {
        setLoaded(null);
        setEditing(false);
        setNotS3(true);
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
    if (busy || !s3) {
      return;
    }
    const validated = validateS3SecretKey(draft);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateServer(siteId, serverId, buildS3SecretKeyBody(validated.secretKey));
      setDirty(false);
      setEditing(false);
      // Never write the typed secret into loaded state: the summary shows a
      // fixed status and the stored key is never rendered.
      setDraft("");
      try {
        const refreshed = unwrapPublishServer(await getServer(siteId, serverId));
        if (isS3DeliveryDriver(serverDriver(refreshed))) {
          setLoaded(refreshed);
        }
      } catch {
        // PUT succeeded. Bucket, region, name, driver, and default stay as shown.
      }
    } catch (err) {
      setError(saveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="s3-secret-key-panel" style={{ marginBottom: 12 }}>
      <h3 style={{ fontSize: "1rem" }}>Amazon S3 secret key</h3>
      {s3 && (
        <div data-testid="s3-secret-key-summary">
          <p>
            Name: <span data-testid="s3-secret-key-name">{shown(name)}</span>
          </p>
          <p>
            Driver: <span data-testid="s3-secret-key-driver">{shown(driver)}</span>
          </p>
          <p>
            Bucket:{" "}
            <span data-testid="s3-secret-key-bucket">{shown(bucket)}</span>
          </p>
          <p>
            Region:{" "}
            <span data-testid="s3-secret-key-region">{shown(region)}</span>
          </p>
          <p>
            Default:{" "}
            <span data-testid="s3-secret-key-default">{shown(defaultFlag)}</span>
          </p>
          <p data-testid="s3-secret-key-status" role="status">
            Secret key: {S3_SECRET_KEY_NEVER_SHOWN}
          </p>
        </div>
      )}
      {notS3 && (
        <p data-testid="s3-secret-key-not-s3" role="status">
          {S3_SECRET_KEY_DRIVER_ONLY}
        </p>
      )}
      {editing && (
        <div data-testid="s3-secret-key-form">
          <div style={formRowStyle}>
            <label htmlFor="s3-secret-key-input">* New secret key</label>
            <input
              id="s3-secret-key-input"
              data-testid="s3-secret-key-input"
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
        <p style={errorStyle} role="alert" data-testid="s3-secret-key-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        {!editing && (
          <button
            type="button"
            style={buttonStyle}
            data-testid="s3-secret-key-open"
            disabled={busy}
            onClick={() => void open()}
          >
            Set S3 secret key
          </button>
        )}
        {editing && (
          <>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="s3-secret-key-save"
              disabled={busy}
              onClick={() => void save()}
            >
              Save S3 secret key
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="s3-secret-key-cancel"
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
