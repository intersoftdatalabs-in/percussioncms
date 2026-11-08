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

import type { PublishServer, PublishServerProperty } from "./types";

/**
 * PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE is VARCHAR(255). Keep in step with
 * {@code PSFtpsDeliveryHostUpdate.MAX_FTPS_HOST_LENGTH}.
 */
export const FTPS_HOST_MAX_LENGTH = 255;

export const FTPS_HOST_PROPERTY = "serverip";

export const FTPS_HOST_REQUIRED = "FTPS host is required";

export const FTPS_HOST_TOO_LONG =
  "FTPS host must be 255 characters or fewer";

export const FTPS_HOST_DRIVER_ONLY =
  "FTPS host can be set only on an FTPS delivery server.";

export type FtpsHostResult = { ok: true; host: string } | { ok: false; error: string };

export type FtpsHostHttpFailure = "bad_request" | "forbidden" | "conflict";

/** FTPS only. FTP, SFTP, S3, Local, and database drivers are not this slice. */
export function isFtpsDeliveryDriver(
  driver: string | undefined | null,
): boolean {
  return (driver ?? "").trim().toUpperCase() === "FTPS";
}

/**
 * Blank (after trim) is rejected and does not clear the stored host.
 * Overlong text is rejected before any update request.
 */
export function validateFtpsHost(raw: string): FtpsHostResult {
  const host = raw.trim();
  if (!host) {
    return { ok: false, error: FTPS_HOST_REQUIRED };
  }
  if (host.length > FTPS_HOST_MAX_LENGTH) {
    return { ok: false, error: FTPS_HOST_TOO_LONG };
  }
  return { ok: true, host };
}

/**
 * Host-only delivery-server PUT body. Port, user id, folder, passphrase,
 * private key, driver, and name are omitted so the server leaves them stored.
 */
export function buildFtpsHostBody(host: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: FTPS_HOST_PROPERTY, value: host }],
    },
  };
}

/** Replace only serverip. Other properties, including secrets, stay on the object. */
export function withUpdatedFtpsHost(server: PublishServer, host: string): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === FTPS_HOST_PROPERTY) {
        found = true;
        return { ...property, key: FTPS_HOST_PROPERTY, value: host };
      }
      return property;
    });
    if (!found) {
      next.push({ key: FTPS_HOST_PROPERTY, value: host });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [FTPS_HOST_PROPERTY]: host } };
  }
  return { ...server, properties: [{ key: FTPS_HOST_PROPERTY, value: host }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function ftpsHostHttpFailure(status: number): FtpsHostHttpFailure | null {
  if (status === 400) {
    return "bad_request";
  }
  if (status === 403) {
    return "forbidden";
  }
  if (status === 409) {
    return "conflict";
  }
  return null;
}
