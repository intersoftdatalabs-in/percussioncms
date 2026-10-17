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
 * {@code PSFtpDeliveryFolderUpdate.MAX_FTP_FOLDER_LENGTH}.
 */
export const FTP_FOLDER_MAX_LENGTH = 255;

export const FTP_FOLDER_PROPERTY = "folder";

export const FTP_FOLDER_TOO_LONG = "FTP folder must be 255 characters or fewer";

export const FTP_FOLDER_DRIVER_ONLY =
  "FTP folder can be set only on an FTP delivery server.";

export type FtpFolderResult = { ok: true; folder: string } | { ok: false; error: string };

export type FtpFolderHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * Blank (after trim) is allowed and clears the stored folder. Overlong text is
 * rejected before any update request. The value is a remote FTP path.
 */
export function validateFtpFolder(raw: string): FtpFolderResult {
  const folder = raw.trim();
  if (folder.length > FTP_FOLDER_MAX_LENGTH) {
    return { ok: false, error: FTP_FOLDER_TOO_LONG };
  }
  return { ok: true, folder };
}

/**
 * Folder-only delivery-server PUT body. Host, port, user, password, and driver
 * are omitted so the server leaves them stored. An empty folder clears it.
 */
export function buildFtpFolderBody(folder: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: FTP_FOLDER_PROPERTY, value: folder }],
    },
  };
}

/** Replace only folder. Other properties, including password, stay on the object. */
export function withUpdatedFolder(server: PublishServer, folder: string): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === FTP_FOLDER_PROPERTY) {
        found = true;
        return { ...property, key: FTP_FOLDER_PROPERTY, value: folder };
      }
      return property;
    });
    if (!found) {
      next.push({ key: FTP_FOLDER_PROPERTY, value: folder });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [FTP_FOLDER_PROPERTY]: folder } };
  }
  return { ...server, properties: [{ key: FTP_FOLDER_PROPERTY, value: folder }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function ftpFolderHttpFailure(status: number): FtpFolderHttpFailure | null {
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
