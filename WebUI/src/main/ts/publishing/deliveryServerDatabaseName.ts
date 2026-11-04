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
 * {@code PSDatabaseDeliveryDatabaseNameUpdate.MAX_DATABASE_NAME_LENGTH}.
 */
export const DATABASE_NAME_MAX_LENGTH = 255;

export const DATABASE_NAME_PROPERTY = "database";

export const DATABASE_NAME_REQUIRED = "Database name is required";

export const DATABASE_NAME_TOO_LONG = "Database name must be 255 characters or fewer";

export const DATABASE_NAME_DRIVER_ONLY =
  "Database name can be set only on a MySQL or MSSQL delivery server.";

const MYSQL_MSSQL_DRIVERS = new Set(["MYSQL", "MYSQL8", "MSSQL"]);

export type DatabaseNameResult =
  | { ok: true; databaseName: string }
  | { ok: false; error: string };

export type DatabaseNameHttpFailure = "bad_request" | "forbidden" | "conflict";

export function isMysqlOrMssqlDeliveryDriver(driver: string): boolean {
  return MYSQL_MSSQL_DRIVERS.has(driver.trim().toUpperCase());
}

/**
 * Blank (after trim) is rejected and does not clear the stored database name.
 * Overlong text is rejected before any update request.
 */
export function validateDatabaseName(raw: string): DatabaseNameResult {
  const databaseName = raw.trim();
  if (!databaseName) {
    return { ok: false, error: DATABASE_NAME_REQUIRED };
  }
  if (databaseName.length > DATABASE_NAME_MAX_LENGTH) {
    return { ok: false, error: DATABASE_NAME_TOO_LONG };
  }
  return { ok: true, databaseName };
}

/**
 * Database-name-only delivery-server PUT body. Host, port, userid, password,
 * driver, and Oracle SID are omitted so the server leaves them stored.
 */
export function buildDatabaseNameBody(databaseName: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: DATABASE_NAME_PROPERTY, value: databaseName }],
    },
  };
}

/** Replace only database. Other properties, including password and SID, stay. */
export function withUpdatedDatabaseName(
  server: PublishServer,
  databaseName: string,
): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === DATABASE_NAME_PROPERTY) {
        found = true;
        return { ...property, key: DATABASE_NAME_PROPERTY, value: databaseName };
      }
      return property;
    });
    if (!found) {
      next.push({ key: DATABASE_NAME_PROPERTY, value: databaseName });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return {
      ...server,
      properties: { ...props, [DATABASE_NAME_PROPERTY]: databaseName },
    };
  }
  return {
    ...server,
    properties: [{ key: DATABASE_NAME_PROPERTY, value: databaseName }],
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function databaseNameHttpFailure(
  status: number,
): DatabaseNameHttpFailure | null {
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
