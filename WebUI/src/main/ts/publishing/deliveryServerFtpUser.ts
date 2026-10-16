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
 * {@code PSFtpDeliveryUserUpdate.MAX_FTP_USER_LENGTH}.
 */
export const FTP_USER_MAX_LENGTH = 255;

export const FTP_USER_PROPERTY = "userid";

export const FTP_USER_REQUIRED = "FTP user id is required";

export const FTP_USER_TOO_LONG = "FTP user id must be 255 characters or fewer";

export const FTP_USER_DRIVER_ONLY =
  "FTP user id can be set only on an FTP delivery server.";

export type FtpUserResult = { ok: true; userId: string } | { ok: false; error: string };

export type FtpUserHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * Blank (after trim) is rejected and does not clear the stored user id.
 * Overlong text is rejected before any update request.
 */
export function validateFtpUser(raw: string): FtpUserResult {
  const userId = raw.trim();
  if (!userId) {
    return { ok: false, error: FTP_USER_REQUIRED };
  }
  if (userId.length > FTP_USER_MAX_LENGTH) {
    return { ok: false, error: FTP_USER_TOO_LONG };
  }
  return { ok: true, userId };
}

/**
 * User-id-only delivery-server PUT body. Host, port, folder, password, and
 * driver are omitted so the server leaves them stored.
 */
export function buildFtpUserBody(userId: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: FTP_USER_PROPERTY, value: userId }],
    },
  };
}

/** Replace only userid. Other properties, including password, stay on the object. */
export function withUpdatedUser(server: PublishServer, userId: string): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === FTP_USER_PROPERTY) {
        found = true;
        return { ...property, key: FTP_USER_PROPERTY, value: userId };
      }
      return property;
    });
    if (!found) {
      next.push({ key: FTP_USER_PROPERTY, value: userId });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [FTP_USER_PROPERTY]: userId } };
  }
  return { ...server, properties: [{ key: FTP_USER_PROPERTY, value: userId }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function ftpUserHttpFailure(status: number): FtpUserHttpFailure | null {
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
