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
 * {@code PSLocalDeliveryFolderUpdate.MAX_LOCAL_FOLDER_LENGTH}.
 */
export const LOCAL_FOLDER_MAX_LENGTH = 255;

export const LOCAL_FOLDER_PROPERTY = "folder";

export const LOCAL_FOLDER_TOO_LONG = "Local folder must be 255 characters or fewer";

export const LOCAL_FOLDER_DRIVER_ONLY =
  "Local folder can be set only on a Local delivery server.";

export type LocalFolderResult = { ok: true; folder: string } | { ok: false; error: string };

export type LocalFolderHttpFailure = "bad_request" | "forbidden" | "conflict";

/** Local file driver only. FTP, FTPS, SFTP, and S3 stay on their own panels. */
export function isLocalDeliveryDriver(driver: string): boolean {
  return driver.trim().toUpperCase() === "LOCAL";
}

/**
 * Blank (after trim) is allowed and clears the stored folder. Overlong text is
 * rejected before any update request. The value is stored as typed text; it is
 * not rewritten with a host filesystem separator.
 */
export function validateLocalFolder(raw: string): LocalFolderResult {
  const folder = raw.trim();
  if (folder.length > LOCAL_FOLDER_MAX_LENGTH) {
    return { ok: false, error: LOCAL_FOLDER_TOO_LONG };
  }
  return { ok: true, folder };
}

/**
 * Folder-only delivery-server PUT body. Driver, name, and FTP host-style
 * properties are omitted so the server leaves them stored. An empty folder
 * clears it.
 */
export function buildLocalFolderBody(folder: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: LOCAL_FOLDER_PROPERTY, value: folder }],
    },
  };
}

/** Replace only folder. Driver, name, and other properties stay on the object. */
export function withUpdatedLocalFolder(server: PublishServer, folder: string): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === LOCAL_FOLDER_PROPERTY) {
        found = true;
        return { ...property, key: LOCAL_FOLDER_PROPERTY, value: folder };
      }
      return property;
    });
    if (!found) {
      next.push({ key: LOCAL_FOLDER_PROPERTY, value: folder });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [LOCAL_FOLDER_PROPERTY]: folder } };
  }
  return { ...server, properties: [{ key: LOCAL_FOLDER_PROPERTY, value: folder }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function localFolderHttpFailure(status: number): LocalFolderHttpFailure | null {
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
