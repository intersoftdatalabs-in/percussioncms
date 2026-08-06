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

/**
 * Client-side required-field matrix for publish server drivers (Minuet parity).
 * Final authority remains server-side validation.
 */

export type DriverFamily =
  | "LOCAL"
  | "FTP"
  | "FTPS"
  | "SFTP"
  | "AMAZONS3"
  | "DATABASE";

export interface ServerFormValues {
  serverName?: string;
  serverType?: string;
  type?: string;
  driver?: string;
  /** Flattened driver property map */
  properties?: Record<string, string>;
}

/**
 * Canonical publish-server property keys. These match the constants on the
 * backend {@code com.percussion.services.pubserver.IPSPubServerDao} (e.g.
 * {@code PUBLISH_USER_ID_PROPERTY = "userid"},
 * {@code PUBLISH_AS3_BUCKET_PROPERTY = "bucketlocation"}) and the legacy
 * Minuet {@code percName} attributes in {@code cm/.../propEditor.jsp}. The
 * React driver-field components write these keys on save; the backend
 * handlers (FTP/FTPS/SFTP, S3, JDBC) read them on login. Writing the wrong
 * key silently breaks publish connectivity (e.g. FTP logs in with an empty
 * username).
 */
const FILE_REMOTE = ["serverip", "userid", "password", "port"] as const;
/** Product / Minuet property keys (IPSPubServerDao AS3 keys are lowercase). */
const S3_FIELDS = ["accesskey", "securitykey", "bucketlocation", "region"] as const;

/**
 * Legacy aliases that earlier drafts of the React driver-field components
 * wrote to the DB instead of the canonical key. Servers saved before this
 * fix landed may have their credential stored under the alias and appear
 * blank in the modern UI; {@link propsToMap} falls back to the alias so the
 * value is surfaced for the next save (which writes it under the canonical
 * key and self-heals the record).
 */
export const LEGACY_PROPERTY_ALIASES: Readonly<Record<string, string>> =
  Object.freeze({
    userid: "user",
    bucketlocation: "bucketName",
  });

export function requiredFieldsForDriver(driver: string): string[] {
  const d = (driver || "").toUpperCase();
  if (d === "LOCAL" || d === "FILESYSTEM" || d === "") {
    return ["serverName"];
  }
  if (d === "FTP" || d === "FTPS") {
    return ["serverName", ...FILE_REMOTE];
  }
  if (d === "SFTP") {
    return ["serverName", "serverip", "userid", "port"];
  }
  if (d === "AMAZONS3" || d === "S3") {
    return ["serverName", ...S3_FIELDS];
  }
  // Database drivers: property keys mirror IPSPubServerDao.PUBLISH_*_PROPERTY
  // (PUBLISH_DATABASE_NAME_PROPERTY="database",
  //  PUBLISH_OWNER_PROPERTY="owner", PUBLISH_SCHEMA_PROPERTY="schema",
  //  PUBLISH_SID_PROPERTY="sid") and the legacy Minuet propEditor.jsp.
  if (d.includes("MYSQL") || d === "MYSQL8") {
    return ["serverName", "driver", "server", "port", "userid", "database", "password"];
  }
  if (d.includes("MSSQL")) {
    return [
      "serverName",
      "driver",
      "server",
      "port",
      "userid",
      "database",
      "owner",
      "password",
    ];
  }
  if (d.includes("ORACLE")) {
    return [
      "serverName",
      "driver",
      "server",
      "port",
      "userid",
      "sid",
      "schema",
      "password",
    ];
  }
  if (d === "DATABASE") {
    return ["serverName", "driver", "server", "port", "userid", "database", "password"];
  }
  return ["serverName"];
}

export function validateServerForm(
  values: ServerFormValues,
): { valid: boolean; missing: string[] } {
  const name = (values.serverName ?? "").trim();
  const props = values.properties ?? {};
  const driver = (values.driver ?? props.driver ?? "LOCAL").toString();
  const required = requiredFieldsForDriver(driver);
  const missing: string[] = [];
  for (const field of required) {
    if (field === "serverName") {
      if (!name) {
        missing.push("serverName");
      }
      continue;
    }
    const v = props[field];
    if (v == null || String(v).trim() === "") {
      missing.push(field);
    }
  }
  return { valid: missing.length === 0, missing };
}
