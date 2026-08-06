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
import { LEGACY_PROPERTY_ALIASES } from "./serverValidation";

/**
 * Drivers for which the {@code user → userid} alias applies. These are the
 * drivers whose backend delivery handler reads
 * {@code IPSPubServerDao.PUBLISH_USER_ID_PROPERTY} when authenticating. The
 * alias must NOT be applied for non-matching drivers (e.g. a Local server
 * with an unrelated custom property named {@code user} would otherwise be
 * silently dropped by {@link PSPubServerService.setProperties()}).
 */
const USER_ALIAS_DRIVERS = new Set(["FTP", "FTPS", "SFTP"]);

/**
 * Drivers for which the {@code bucketName → bucketlocation} alias applies.
 * Mirrors the S3 delivery handler, which reads
 * {@code IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY}.
 */
const BUCKET_ALIAS_DRIVERS = new Set(["AMAZONS3", "S3"]);

/**
 * Test whether a property value is absent or only whitespace. Matches the
 * blankness semantics used by {@link validateServerForm} so the read path
 * does not promote a whitespace-only legacy value over a missing canonical
 * value (or vice-versa).
 */
function isBlank(value: unknown): boolean {
  return value == null || String(value).trim() === "";
}

/** UI-editable server form state (flattened). */
export interface ServerEditorModel {
  serverId: string;
  serverName: string;
  serverType: "PRODUCTION" | "STAGING" | string;
  type: "File" | "Database" | string;
  driver: string;
  isDefault: boolean;
  ignoreUnModifiedAssets: boolean;
  publishRelatedItems: boolean;
  properties: Record<string, string>;
}

export function propsToMap(
  props: PublishServerProperty[] | Record<string, string> | undefined,
  driver?: string,
): Record<string, string> {
  if (props == null) {
    return {};
  }
  if (!Array.isArray(props)) {
    const out: Record<string, string> = {};
    for (const [k, v] of Object.entries(props)) {
      out[k] = v == null ? "" : String(v);
    }
    return applyLegacyAliases(out, driver);
  }
  const out: Record<string, string> = {};
  for (const p of props) {
    const key = p.key ?? p.name;
    if (key == null || key === "") {
      continue;
    }
    out[key] = p.value == null ? "" : String(p.value);
  }
  return applyLegacyAliases(out, driver);
}

/**
 * Self-heal publishing-server property records that were written by an
 * earlier draft of the React driver components using non-canonical keys
 * (e.g. {@code user} instead of {@code userid}, {@code bucketName} instead
 * of {@code bucketlocation}). When the canonical key is absent but the
 * legacy alias is present for the matching driver, the value is surfaced
 * under the canonical key so the editor displays it; the legacy alias is
 * then dropped so the next save writes only the canonical key
 * (self-healing the persisted record). The alias is strictly scoped to
 * the driver that owns it — applying it universally would silently delete
 * unrelated custom properties (e.g. a Local server with a property named
 * {@code user}) because {@code PSPubServerService.setProperties()} clears
 * the persisted set and re-adds only what the client sends. See
 * {@link LEGACY_PROPERTY_ALIASES} for the alias table.
 */
function applyLegacyAliases(
  map: Record<string, string>,
  driver?: string,
): Record<string, string> {
  const d = (driver ?? "").toUpperCase();
  if (USER_ALIAS_DRIVERS.has(d) && "userid" in LEGACY_PROPERTY_ALIASES) {
    const legacy = LEGACY_PROPERTY_ALIASES.userid;
    if (isBlank(map["userid"]) && !isBlank(map[legacy])) {
      map["userid"] = map[legacy];
    }
    delete map[legacy];
  }
  if (BUCKET_ALIAS_DRIVERS.has(d) && "bucketlocation" in LEGACY_PROPERTY_ALIASES) {
    const legacy = LEGACY_PROPERTY_ALIASES.bucketlocation;
    if (isBlank(map["bucketlocation"]) && !isBlank(map[legacy])) {
      map["bucketlocation"] = map[legacy];
    }
    delete map[legacy];
  }
  return map;
}

export function mapToProps(
  map: Record<string, string>,
): PublishServerProperty[] {
  return Object.entries(map).map(([key, value]) => ({ key, value }));
}

export function emptyServerModel(): ServerEditorModel {
  return {
    serverId: "",
    serverName: "",
    serverType: "PRODUCTION",
    type: "File",
    driver: "Local",
    isDefault: false,
    ignoreUnModifiedAssets: false,
    publishRelatedItems: false,
    properties: {
      driver: "Local",
      folder: "",
      HTML: "true",
      XML: "false",
      defaultServerFlag: "false",
      ownServerFlag: "false",
    },
  };
}

/** Normalize API server payload (may nest under serverInfo). */
export function serverToModel(raw: PublishServer | Record<string, unknown>): ServerEditorModel {
  const root = (raw as { serverInfo?: PublishServer }).serverInfo
    ? ((raw as { serverInfo: PublishServer }).serverInfo as PublishServer)
    : (raw as PublishServer);
  // Read the driver BEFORE aliasing so the alias layer can scope the
  // user / bucketName → userid / bucketlocation rewrite to drivers that
  // actually own those property names (FTP/FTPS/SFTP and AMAZONS3/S3).
  const driverFromRoot = (root as { driver?: string }).driver;
  const driverFromProps =
    (Array.isArray(root.properties)
      ? root.properties.find((p) => p?.key === "driver")?.value
      : (root.properties as Record<string, string> | undefined)?.driver) ??
    "";
  const rawMap = propsToMap(root.properties);
  const driver =
    driverFromRoot || driverFromProps || rawMap.driver || "Local";
  const map = propsToMap(root.properties, driver);
  return {
    serverId: String(root.serverId ?? ""),
    serverName: String(root.serverName ?? root.name ?? ""),
    serverType: String(root.serverType ?? "PRODUCTION"),
    type: String(root.type ?? "File"),
    driver,
    isDefault: Boolean(root.isDefault ?? root.defaultServer),
    ignoreUnModifiedAssets: String(map.ignoreUnModifiedAssets) === "true",
    publishRelatedItems: String(map.publishRelatedItems) === "true",
    properties: { ...map, driver },
  };
}

/**
 * Build create/update body matching Minuet `serverInfo` shape.
 * Password fields are base64-encoded when non-empty (product norm).
 */
export function modelToSaveBody(model: ServerEditorModel): {
  serverInfo: Record<string, unknown>;
} {
  const props: Record<string, string> = {
    ...model.properties,
    driver: model.driver,
    ignoreUnModifiedAssets: String(model.ignoreUnModifiedAssets),
    publishRelatedItems: String(model.publishRelatedItems),
  };
  // Encode password-like fields when set (empty means leave unchanged on update).
  for (const key of ["password", "securitykey", "accesskey", "secretKey"]) {
    if (props[key] && props[key].length > 0 && !looksBase64(props[key])) {
      try {
        props[key] = btoa(props[key]);
      } catch {
        /* keep plain if btoa fails (non-latin1) */
      }
    }
  }
  return {
    serverInfo: {
      isDefault: model.isDefault,
      serverId: model.serverId || null,
      serverName: model.serverName.trim(),
      type: model.type,
      isModified: "",
      properties: mapToProps(props),
      serverType: model.serverType,
    },
  };
}

function looksBase64(value: string): boolean {
  return /^[A-Za-z0-9+/]+=*$/.test(value) && value.length % 4 === 0 && value.length >= 4;
}

export const FILE_DRIVERS = [
  "Local",
  "FTP",
  "FTPS",
  "SFTP",
  "AMAZONS3",
] as const;

export const DATABASE_DRIVERS = ["MSSQL", "MYSQL", "Oracle"] as const;
