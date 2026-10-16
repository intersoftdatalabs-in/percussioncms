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
 * {@code PSFtpDeliveryPortUpdate.MAX_FTP_PORT_LENGTH}.
 */
export const FTP_PORT_MAX_LENGTH = 255;

export const FTP_PORT_PROPERTY = "port";

export const FTP_PORT_REQUIRED = "FTP port is required";

export const FTP_PORT_NOT_NUMERIC = "FTP port must contain only digits";

export const FTP_PORT_TOO_LONG = "FTP port must be 255 characters or fewer";

export const FTP_PORT_DRIVER_ONLY =
  "FTP port can be set only on an FTP delivery server.";

export type FtpPortResult = { ok: true; port: string } | { ok: false; error: string };

export type FtpPortHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * Blank (after trim) is rejected and does not clear the stored port.
 * Non-numeric text and overlong text are rejected before any update request.
 */
export function validateFtpPort(raw: string): FtpPortResult {
  const port = raw.trim();
  if (!port) {
    return { ok: false, error: FTP_PORT_REQUIRED };
  }
  if (port.length > FTP_PORT_MAX_LENGTH) {
    return { ok: false, error: FTP_PORT_TOO_LONG };
  }
  if (!/^[0-9]+$/.test(port)) {
    return { ok: false, error: FTP_PORT_NOT_NUMERIC };
  }
  return { ok: true, port };
}

/**
 * Port-only delivery-server PUT body. Host, user, folder, password, and driver
 * are omitted so the server leaves them stored.
 */
export function buildFtpPortBody(port: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: FTP_PORT_PROPERTY, value: port }],
    },
  };
}

/** Replace only port. Other properties, including password, stay on the object. */
export function withUpdatedPort(server: PublishServer, port: string): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === FTP_PORT_PROPERTY) {
        found = true;
        return { ...property, key: FTP_PORT_PROPERTY, value: port };
      }
      return property;
    });
    if (!found) {
      next.push({ key: FTP_PORT_PROPERTY, value: port });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [FTP_PORT_PROPERTY]: port } };
  }
  return { ...server, properties: [{ key: FTP_PORT_PROPERTY, value: port }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function ftpPortHttpFailure(status: number): FtpPortHttpFailure | null {
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
