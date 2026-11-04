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
 * {@code PSDatabaseDeliverySidUpdate.MAX_ORACLE_SID_LENGTH}.
 */
export const ORACLE_SID_MAX_LENGTH = 255;

export const ORACLE_SID_PROPERTY = "sid";

export const ORACLE_SID_REQUIRED = "Oracle SID is required";

export const ORACLE_SID_TOO_LONG = "Oracle SID must be 255 characters or fewer";

export const ORACLE_SID_DRIVER_ONLY =
  "Oracle SID can be set only on an Oracle delivery server.";

export type OracleSidResult =
  | { ok: true; sid: string }
  | { ok: false; error: string };

export type OracleSidHttpFailure = "bad_request" | "forbidden" | "conflict";

export function isOracleDeliveryDriver(driver: string): boolean {
  return driver.trim().toUpperCase() === "ORACLE";
}

/**
 * Blank (after trim) is rejected and does not clear the stored SID.
 * Overlong text is rejected before any update request.
 */
export function validateOracleSid(raw: string): OracleSidResult {
  const sid = raw.trim();
  if (!sid) {
    return { ok: false, error: ORACLE_SID_REQUIRED };
  }
  if (sid.length > ORACLE_SID_MAX_LENGTH) {
    return { ok: false, error: ORACLE_SID_TOO_LONG };
  }
  return { ok: true, sid };
}

/**
 * SID-only delivery-server PUT body. Host, port, userid, password, driver,
 * schema, and database name are omitted so the server leaves them stored.
 */
export function buildOracleSidBody(sid: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: ORACLE_SID_PROPERTY, value: sid }],
    },
  };
}

/** Replace only sid. Other properties, including password and schema, stay. */
export function withUpdatedOracleSid(
  server: PublishServer,
  sid: string,
): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === ORACLE_SID_PROPERTY) {
        found = true;
        return { ...property, key: ORACLE_SID_PROPERTY, value: sid };
      }
      return property;
    });
    if (!found) {
      next.push({ key: ORACLE_SID_PROPERTY, value: sid });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return {
      ...server,
      properties: { ...props, [ORACLE_SID_PROPERTY]: sid },
    };
  }
  return {
    ...server,
    properties: [{ key: ORACLE_SID_PROPERTY, value: sid }],
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function oracleSidHttpFailure(
  status: number,
): OracleSidHttpFailure | null {
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
