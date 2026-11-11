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

import type { PublishServer, PublishServerProperty } from "./types";
import {
  DATABASE_HOST_MAX_LENGTH,
  DATABASE_HOST_PROPERTY,
  DATABASE_HOST_REQUIRED,
  DATABASE_HOST_TOO_LONG,
} from "./deliveryServerDatabaseHost";
import {
  DATABASE_PASSWORD_MAX_LENGTH,
  DATABASE_PASSWORD_PROPERTY,
  DATABASE_PASSWORD_REQUIRED,
  DATABASE_PASSWORD_TOO_LONG,
} from "./deliveryServerDatabasePassword";
import {
  DATABASE_USER_MAX_LENGTH,
  DATABASE_USER_PROPERTY,
  DATABASE_USER_REQUIRED,
  DATABASE_USER_TOO_LONG,
} from "./deliveryServerDatabaseUser";

export type DatabaseConnectionResult =
  | { ok: true; host: string; userId: string; password: string }
  | { ok: false; error: string };

export type DatabaseConnectionHttpFailure =
  | "bad_request"
  | "forbidden"
  | "conflict";

/**
 * The host and user id are rejected blank (after trim) and do not clear the
 * stored values. The password is rejected blank and never echoes the masked
 * placeholder: the input always opens empty. Overlong values are rejected
 * before any update request. Length limits stay in step with
 * {@code PSDatabaseDeliveryConnectionUpdate} via the single-field constants.
 */
export function validateDatabaseConnection(raw: {
  host: string;
  userId: string;
  password: string;
}): DatabaseConnectionResult {
  const host = raw.host.trim();
  if (!host) {
    return { ok: false, error: DATABASE_HOST_REQUIRED };
  }
  if (host.length > DATABASE_HOST_MAX_LENGTH) {
    return { ok: false, error: DATABASE_HOST_TOO_LONG };
  }
  const userId = raw.userId.trim();
  if (!userId) {
    return { ok: false, error: DATABASE_USER_REQUIRED };
  }
  if (userId.length > DATABASE_USER_MAX_LENGTH) {
    return { ok: false, error: DATABASE_USER_TOO_LONG };
  }
  if (!raw.password || !raw.password.trim()) {
    return { ok: false, error: DATABASE_PASSWORD_REQUIRED };
  }
  if (raw.password.length > DATABASE_PASSWORD_MAX_LENGTH) {
    return { ok: false, error: DATABASE_PASSWORD_TOO_LONG };
  }
  return { ok: true, host, userId, password: raw.password };
}

/**
 * Connection-triple delivery-server PUT body. Only server, userid, and
 * password travel; port, database/sid, driver, and name are omitted so the
 * server leaves them stored. The password travels base64-encoded (product
 * norm, see modelToSaveBody); plain text is kept only when encoding fails
 * (non-latin1), which the server also accepts.
 */
export function buildDatabaseConnectionBody(
  host: string,
  userId: string,
  password: string,
): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  let secret = password;
  try {
    secret = btoa(password);
  } catch {
    /* keep plain if btoa fails (non-latin1); the server decodes defensively */
  }
  return {
    serverInfo: {
      properties: [
        { key: DATABASE_HOST_PROPERTY, value: host },
        { key: DATABASE_USER_PROPERTY, value: userId },
        { key: DATABASE_PASSWORD_PROPERTY, value: secret },
      ],
    },
  };
}

/**
 * Replace only server and userid on the cached object. The typed secret is
 * never written into loaded state: the summary carries the fixed
 * never-shown status and GET responses carry only a masked placeholder.
 */
export function withUpdatedDatabaseConnection(
  server: PublishServer,
  host: string,
  userId: string,
): PublishServer {
  const props = server.properties;
  const keys = new Set([DATABASE_HOST_PROPERTY, DATABASE_USER_PROPERTY]);
  if (Array.isArray(props)) {
    const seen = new Set<string>();
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === DATABASE_HOST_PROPERTY) {
        seen.add(key);
        return { ...property, key: DATABASE_HOST_PROPERTY, value: host };
      }
      if (key === DATABASE_USER_PROPERTY) {
        seen.add(key);
        return { ...property, key: DATABASE_USER_PROPERTY, value: userId };
      }
      return property;
    });
    for (const key of keys) {
      if (!seen.has(key)) {
        next.push({
          key,
          value: key === DATABASE_HOST_PROPERTY ? host : userId,
        });
      }
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return {
      ...server,
      properties: {
        ...props,
        [DATABASE_HOST_PROPERTY]: host,
        [DATABASE_USER_PROPERTY]: userId,
      },
    };
  }
  return {
    ...server,
    properties: [
      { key: DATABASE_HOST_PROPERTY, value: host },
      { key: DATABASE_USER_PROPERTY, value: userId },
    ],
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function databaseConnectionHttpFailure(
  status: number,
): DatabaseConnectionHttpFailure | null {
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
