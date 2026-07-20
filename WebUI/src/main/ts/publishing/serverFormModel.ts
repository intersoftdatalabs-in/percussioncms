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

import type { PublishServer, PublishServerProperty } from "./types";

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
): Record<string, string> {
  if (props == null) {
    return {};
  }
  if (!Array.isArray(props)) {
    const out: Record<string, string> = {};
    for (const [k, v] of Object.entries(props)) {
      out[k] = v == null ? "" : String(v);
    }
    return out;
  }
  const out: Record<string, string> = {};
  for (const p of props) {
    const key = p.key ?? p.name;
    if (key == null || key === "") {
      continue;
    }
    out[key] = p.value == null ? "" : String(p.value);
  }
  return out;
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
  const map = propsToMap(root.properties);
  const driver = map.driver || "Local";
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
