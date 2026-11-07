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
 * {@code PSSftpDeliveryPortUpdate.MAX_SFTP_PORT_LENGTH}.
 */
export const SFTP_PORT_MAX_LENGTH = 255;

export const SFTP_PORT_PROPERTY = "port";

export const SFTP_PORT_REQUIRED = "SFTP port is required";

export const SFTP_PORT_NOT_NUMERIC = "SFTP port must contain only digits";

export const SFTP_PORT_TOO_LONG = "SFTP port must be 255 characters or fewer";

export const SFTP_PORT_DRIVER_ONLY =
  "SFTP port can be set only on an SFTP delivery server.";

export type SftpPortResult = { ok: true; port: string } | { ok: false; error: string };

export type SftpPortHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * Blank (after trim) is rejected and does not clear the stored port.
 * Non-numeric text and overlong text are rejected before any update request.
 */
export function validateSftpPort(raw: string): SftpPortResult {
  const port = raw.trim();
  if (!port) {
    return { ok: false, error: SFTP_PORT_REQUIRED };
  }
  if (port.length > SFTP_PORT_MAX_LENGTH) {
    return { ok: false, error: SFTP_PORT_TOO_LONG };
  }
  if (!/^[0-9]+$/.test(port)) {
    return { ok: false, error: SFTP_PORT_NOT_NUMERIC };
  }
  return { ok: true, port };
}

/**
 * Port-only delivery-server PUT body. Host, user id, folder, passphrase,
 * private key, driver, and name are omitted so the server leaves them stored.
 */
export function buildSftpPortBody(port: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: SFTP_PORT_PROPERTY, value: port }],
    },
  };
}

/** Replace only port. Other properties, including secrets, stay on the object. */
export function withUpdatedSftpPort(server: PublishServer, port: string): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === SFTP_PORT_PROPERTY) {
        found = true;
        return { ...property, key: SFTP_PORT_PROPERTY, value: port };
      }
      return property;
    });
    if (!found) {
      next.push({ key: SFTP_PORT_PROPERTY, value: port });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [SFTP_PORT_PROPERTY]: port } };
  }
  return { ...server, properties: [{ key: SFTP_PORT_PROPERTY, value: port }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function sftpPortHttpFailure(status: number): SftpPortHttpFailure | null {
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
