/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

const FILE_REMOTE = ["serverip", "user", "password", "port"] as const;
/** Product / Minuet property keys (IPSPubServerDao AS3 keys are lowercase). */
const S3_FIELDS = ["accesskey", "securitykey", "bucketName", "region"] as const;
const DB_FIELDS = ["driver", "server", "database", "user", "password"] as const;

export function requiredFieldsForDriver(driver: string): string[] {
  const d = (driver || "").toUpperCase();
  if (d === "LOCAL" || d === "FILESYSTEM" || d === "") {
    return ["serverName"];
  }
  if (d === "FTP" || d === "FTPS") {
    return ["serverName", ...FILE_REMOTE];
  }
  if (d === "SFTP") {
    return ["serverName", "serverip", "user", "port"];
  }
  if (d === "AMAZONS3" || d === "S3") {
    return ["serverName", ...S3_FIELDS];
  }
  if (
    d.includes("MSSQL") ||
    d.includes("MYSQL") ||
    d.includes("ORACLE") ||
    d === "DATABASE"
  ) {
    return ["serverName", ...DB_FIELDS];
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
