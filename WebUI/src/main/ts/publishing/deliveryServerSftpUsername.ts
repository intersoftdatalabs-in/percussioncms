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
 * {@code PSSftpDeliveryUsernameUpdate.MAX_SFTP_USERNAME_LENGTH}.
 */
export const SFTP_USERNAME_MAX_LENGTH = 255;

export const SFTP_USERNAME_PROPERTY = "userid";

export const SFTP_USERNAME_REQUIRED = "SFTP username is required";

export const SFTP_USERNAME_TOO_LONG = "SFTP username must be 255 characters or fewer";

export const SFTP_USERNAME_DRIVER_ONLY =
  "SFTP username can be set only on an SFTP delivery server.";

export type SftpUsernameResult = { ok: true; username: string } | { ok: false; error: string };

export type SftpUsernameHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * Blank (after trim) is rejected and does not clear the stored username.
 * Overlong text is rejected before any update request.
 */
export function validateSftpUsername(raw: string): SftpUsernameResult {
  const username = raw.trim();
  if (!username) {
    return { ok: false, error: SFTP_USERNAME_REQUIRED };
  }
  if (username.length > SFTP_USERNAME_MAX_LENGTH) {
    return { ok: false, error: SFTP_USERNAME_TOO_LONG };
  }
  return { ok: true, username };
}

/**
 * Username-only delivery-server PUT body. Host, port, folder, passphrase,
 * private key, driver, and name are omitted so the server leaves them stored.
 */
export function buildSftpUsernameBody(username: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: SFTP_USERNAME_PROPERTY, value: username }],
    },
  };
}

/** Replace only userid. Other properties, including secrets, stay on the object. */
export function withUpdatedSftpUsername(server: PublishServer, username: string): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === SFTP_USERNAME_PROPERTY) {
        found = true;
        return { ...property, key: SFTP_USERNAME_PROPERTY, value: username };
      }
      return property;
    });
    if (!found) {
      next.push({ key: SFTP_USERNAME_PROPERTY, value: username });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [SFTP_USERNAME_PROPERTY]: username } };
  }
  return { ...server, properties: [{ key: SFTP_USERNAME_PROPERTY, value: username }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function sftpUsernameHttpFailure(status: number): SftpUsernameHttpFailure | null {
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
