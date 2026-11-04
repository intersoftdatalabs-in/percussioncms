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
 * {@code PSDatabaseDeliveryOwnerUpdate.MAX_MSSQL_OWNER_LENGTH}.
 */
export const MSSQL_OWNER_MAX_LENGTH = 255;

export const MSSQL_OWNER_PROPERTY = "owner";

export const MSSQL_OWNER_REQUIRED = "MSSQL owner is required";

export const MSSQL_OWNER_TOO_LONG = "MSSQL owner must be 255 characters or fewer";

export const MSSQL_OWNER_DRIVER_ONLY =
  "MSSQL owner can be set only on an MSSQL delivery server.";

export type MssqlOwnerResult =
  | { ok: true; owner: string }
  | { ok: false; error: string };

export type MssqlOwnerHttpFailure = "bad_request" | "forbidden" | "conflict";

export function isMssqlDeliveryDriver(driver: string): boolean {
  return driver.trim().toUpperCase() === "MSSQL";
}

/**
 * Blank (after trim) is rejected and does not clear the stored owner.
 * Overlong text is rejected before any update request.
 */
export function validateMssqlOwner(raw: string): MssqlOwnerResult {
  const owner = raw.trim();
  if (!owner) {
    return { ok: false, error: MSSQL_OWNER_REQUIRED };
  }
  if (owner.length > MSSQL_OWNER_MAX_LENGTH) {
    return { ok: false, error: MSSQL_OWNER_TOO_LONG };
  }
  return { ok: true, owner };
}

/**
 * Owner-only delivery-server PUT body. Host, port, userid, password, driver,
 * SID, and database name are omitted so the server leaves them stored.
 */
export function buildMssqlOwnerBody(owner: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: MSSQL_OWNER_PROPERTY, value: owner }],
    },
  };
}

/** Replace only owner. Other properties, including password and database, stay. */
export function withUpdatedMssqlOwner(
  server: PublishServer,
  owner: string,
): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === MSSQL_OWNER_PROPERTY) {
        found = true;
        return { ...property, key: MSSQL_OWNER_PROPERTY, value: owner };
      }
      return property;
    });
    if (!found) {
      next.push({ key: MSSQL_OWNER_PROPERTY, value: owner });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return {
      ...server,
      properties: { ...props, [MSSQL_OWNER_PROPERTY]: owner },
    };
  }
  return {
    ...server,
    properties: [{ key: MSSQL_OWNER_PROPERTY, value: owner }],
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function mssqlOwnerHttpFailure(
  status: number,
): MssqlOwnerHttpFailure | null {
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
