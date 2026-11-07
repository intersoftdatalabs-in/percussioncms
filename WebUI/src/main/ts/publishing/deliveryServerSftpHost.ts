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
 * {@code PSSftpDeliveryHostUpdate.MAX_SFTP_HOST_LENGTH}.
 */
export const SFTP_HOST_MAX_LENGTH = 255;

export const SFTP_HOST_PROPERTY = "serverip";

export const SFTP_HOST_REQUIRED = "SFTP host is required";

export const SFTP_HOST_TOO_LONG =
  "SFTP host must be 255 characters or fewer";

export const SFTP_HOST_DRIVER_ONLY =
  "SFTP host can be set only on an SFTP delivery server.";

export type SftpHostResult = { ok: true; host: string } | { ok: false; error: string };

export type SftpHostHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * Blank (after trim) is rejected and does not clear the stored host.
 * Overlong text is rejected before any update request.
 */
export function validateSftpHost(raw: string): SftpHostResult {
  const host = raw.trim();
  if (!host) {
    return { ok: false, error: SFTP_HOST_REQUIRED };
  }
  if (host.length > SFTP_HOST_MAX_LENGTH) {
    return { ok: false, error: SFTP_HOST_TOO_LONG };
  }
  return { ok: true, host };
}

/**
 * Host-only delivery-server PUT body. Port, user id, folder, passphrase,
 * private key, driver, and name are omitted so the server leaves them stored.
 */
export function buildSftpHostBody(host: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: SFTP_HOST_PROPERTY, value: host }],
    },
  };
}

/** Replace only serverip. Other properties, including secrets, stay on the object. */
export function withUpdatedSftpHost(server: PublishServer, host: string): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === SFTP_HOST_PROPERTY) {
        found = true;
        return { ...property, key: SFTP_HOST_PROPERTY, value: host };
      }
      return property;
    });
    if (!found) {
      next.push({ key: SFTP_HOST_PROPERTY, value: host });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [SFTP_HOST_PROPERTY]: host } };
  }
  return { ...server, properties: [{ key: SFTP_HOST_PROPERTY, value: host }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function sftpHostHttpFailure(status: number): SftpHostHttpFailure | null {
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
