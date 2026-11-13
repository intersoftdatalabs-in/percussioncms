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
 * {@code PSFtpsDeliveryPortUpdate.MAX_FTPS_PORT_LENGTH}.
 */
export const FTPS_PORT_MAX_LENGTH = 255;

export const FTPS_PORT_PROPERTY = "port";

export const FTPS_PORT_REQUIRED = "FTPS port is required";

export const FTPS_PORT_NOT_NUMERIC = "FTPS port must contain only digits";

export const FTPS_PORT_TOO_LONG = "FTPS port must be 255 characters or fewer";

export const FTPS_PORT_DRIVER_ONLY =
  "FTPS port can be set only on an FTPS delivery server.";

export type FtpsPortResult = { ok: true; port: string } | { ok: false; error: string };

export type FtpsPortHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * Blank (after trim) is rejected and does not clear the stored port.
 * Non-numeric text and overlong text are rejected before any update request.
 */
export function validateFtpsPort(raw: string): FtpsPortResult {
  const port = raw.trim();
  if (!port) {
    return { ok: false, error: FTPS_PORT_REQUIRED };
  }
  if (port.length > FTPS_PORT_MAX_LENGTH) {
    return { ok: false, error: FTPS_PORT_TOO_LONG };
  }
  if (!/^[0-9]+$/.test(port)) {
    return { ok: false, error: FTPS_PORT_NOT_NUMERIC };
  }
  return { ok: true, port };
}

/**
 * Port-only delivery-server PUT body. Host, user id, folder,
 * driver, and name are omitted so the server leaves them stored.
 */
export function buildFtpsPortBody(port: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: FTPS_PORT_PROPERTY, value: port }],
    },
  };
}

/** Replace only port. Other properties, including secrets, stay on the object. */
export function withUpdatedFtpsPort(server: PublishServer, port: string): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === FTPS_PORT_PROPERTY) {
        found = true;
        return { ...property, key: FTPS_PORT_PROPERTY, value: port };
      }
      return property;
    });
    if (!found) {
      next.push({ key: FTPS_PORT_PROPERTY, value: port });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [FTPS_PORT_PROPERTY]: port } };
  }
  return { ...server, properties: [{ key: FTPS_PORT_PROPERTY, value: port }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function ftpsPortHttpFailure(status: number): FtpsPortHttpFailure | null {
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
