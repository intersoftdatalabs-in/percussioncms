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
import { isOracleDeliveryDriver } from "./deliveryServerDatabaseSid";

/**
 * PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE is VARCHAR(255). Keep in step with
 * {@code PSDatabaseDeliverySchemaUpdate.MAX_ORACLE_SCHEMA_LENGTH}.
 */
export const ORACLE_SCHEMA_MAX_LENGTH = 255;

export const ORACLE_SCHEMA_PROPERTY = "schema";

export const ORACLE_SCHEMA_REQUIRED = "Oracle schema is required";

export const ORACLE_SCHEMA_TOO_LONG =
  "Oracle schema must be 255 characters or fewer";

export const ORACLE_SCHEMA_DRIVER_ONLY =
  "Oracle schema can be set only on an Oracle delivery server.";

export type OracleSchemaResult =
  | { ok: true; schema: string }
  | { ok: false; error: string };

export type OracleSchemaHttpFailure = "bad_request" | "forbidden" | "conflict";

export { isOracleDeliveryDriver };

/**
 * Blank (after trim) is rejected and does not clear the stored schema.
 * Overlong text is rejected before any update request.
 */
export function validateOracleSchema(raw: string): OracleSchemaResult {
  const schema = raw.trim();
  if (!schema) {
    return { ok: false, error: ORACLE_SCHEMA_REQUIRED };
  }
  if (schema.length > ORACLE_SCHEMA_MAX_LENGTH) {
    return { ok: false, error: ORACLE_SCHEMA_TOO_LONG };
  }
  return { ok: true, schema };
}

/**
 * Schema-only delivery-server PUT body. Host, port, userid, password, driver,
 * SID, and database name are omitted so the server leaves them stored.
 */
export function buildOracleSchemaBody(schema: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: ORACLE_SCHEMA_PROPERTY, value: schema }],
    },
  };
}

/** Replace only schema. Other properties, including password and SID, stay. */
export function withUpdatedOracleSchema(
  server: PublishServer,
  schema: string,
): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === ORACLE_SCHEMA_PROPERTY) {
        found = true;
        return { ...property, key: ORACLE_SCHEMA_PROPERTY, value: schema };
      }
      return property;
    });
    if (!found) {
      next.push({ key: ORACLE_SCHEMA_PROPERTY, value: schema });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return {
      ...server,
      properties: { ...props, [ORACLE_SCHEMA_PROPERTY]: schema },
    };
  }
  return {
    ...server,
    properties: [{ key: ORACLE_SCHEMA_PROPERTY, value: schema }],
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function oracleSchemaHttpFailure(
  status: number,
): OracleSchemaHttpFailure | null {
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
