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
import { propsToMap } from "./serverFormModel";

/**
 * PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE is VARCHAR(255). Keep in step with
 * {@code PSDatabaseDeliveryHostUpdate.MAX_DATABASE_HOST_LENGTH}.
 */
export const DATABASE_HOST_MAX_LENGTH = 255;

export const DATABASE_HOST_PROPERTY = "server";

export const DATABASE_HOST_REQUIRED = "Database host is required";

export const DATABASE_HOST_TOO_LONG = "Database host must be 255 characters or fewer";

export const DATABASE_HOST_DRIVER_ONLY =
  "Database host can be set only on a database delivery server.";

const DATABASE_DRIVERS = new Set(["MYSQL", "MYSQL8", "MSSQL", "ORACLE"]);

export type DatabaseHostResult =
  | { ok: true; host: string }
  | { ok: false; error: string };

export type DatabaseHostHttpFailure = "bad_request" | "forbidden" | "conflict";

export function isDatabaseDeliveryDriver(driver: string): boolean {
  return DATABASE_DRIVERS.has(driver.trim().toUpperCase());
}

/**
 * Blank (after trim) is rejected and does not clear the stored host.
 * Overlong text is rejected before any update request.
 */
export function validateDatabaseHost(raw: string): DatabaseHostResult {
  const host = raw.trim();
  if (!host) {
    return { ok: false, error: DATABASE_HOST_REQUIRED };
  }
  if (host.length > DATABASE_HOST_MAX_LENGTH) {
    return { ok: false, error: DATABASE_HOST_TOO_LONG };
  }
  return { ok: true, host };
}

/**
 * Host-only delivery-server PUT body. Port, user, database/sid, password, and
 * driver are omitted so the server leaves them stored.
 */
export function buildDatabaseHostBody(host: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: DATABASE_HOST_PROPERTY, value: host }],
    },
  };
}

/** Replace only server. Other properties, including password, stay on the object. */
export function withUpdatedDatabaseHost(
  server: PublishServer,
  host: string,
): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === DATABASE_HOST_PROPERTY) {
        found = true;
        return { ...property, key: DATABASE_HOST_PROPERTY, value: host };
      }
      return property;
    });
    if (!found) {
      next.push({ key: DATABASE_HOST_PROPERTY, value: host });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [DATABASE_HOST_PROPERTY]: host } };
  }
  return { ...server, properties: [{ key: DATABASE_HOST_PROPERTY, value: host }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function databaseHostHttpFailure(
  status: number,
): DatabaseHostHttpFailure | null {
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

export function databaseNameOrSid(server: PublishServer, driver: string): string {
  const map = propsToMap(server.properties);
  if (driver.trim().toUpperCase() === "ORACLE") {
    return map.sid ?? "";
  }
  return map.database ?? "";
}
