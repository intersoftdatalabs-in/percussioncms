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
import { propsToMap } from "./serverFormModel";

/**
 * PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE is VARCHAR(255). Keep in step with
 * {@code PSFtpDeliveryHostUpdate.MAX_FTP_HOST_LENGTH}.
 */
export const FTP_HOST_MAX_LENGTH = 255;

export const FTP_HOST_PROPERTY = "serverip";

export const FTP_HOST_REQUIRED = "FTP host is required";

export const FTP_HOST_TOO_LONG = "FTP host must be 255 characters or fewer";

export const FTP_HOST_DRIVER_ONLY =
  "FTP host can be set only on an FTP delivery server.";

export type FtpHostResult = { ok: true; host: string } | { ok: false; error: string };

export type FtpHostHttpFailure = "bad_request" | "forbidden" | "conflict";

/** Normalize a publish-server payload that may nest under serverInfo. */
export function unwrapPublishServer(
  raw: PublishServer | { serverInfo?: PublishServer } | null | undefined,
): PublishServer {
  if (raw == null) {
    return {};
  }
  const nested = (raw as { serverInfo?: PublishServer }).serverInfo;
  return nested ?? (raw as PublishServer);
}

export function serverDriver(server: PublishServer): string {
  return (propsToMap(server.properties).driver ?? "").trim();
}

/** FTP only. FTPS, SFTP, S3, and Local are not this slice. */
export function isFtpDeliveryDriver(driver: string): boolean {
  return driver.trim().toUpperCase() === "FTP";
}

export function serverProperty(server: PublishServer, key: string): string {
  return propsToMap(server.properties)[key] ?? "";
}

/**
 * Blank (after trim) is rejected and does not clear the stored host.
 * Overlong text is rejected before any update request.
 */
export function validateFtpHost(raw: string): FtpHostResult {
  const host = raw.trim();
  if (!host) {
    return { ok: false, error: FTP_HOST_REQUIRED };
  }
  if (host.length > FTP_HOST_MAX_LENGTH) {
    return { ok: false, error: FTP_HOST_TOO_LONG };
  }
  return { ok: true, host };
}

/**
 * Host-only delivery-server PUT body. Port, user, folder, password, and driver
 * are omitted so the server leaves them stored.
 */
export function buildFtpHostBody(host: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: FTP_HOST_PROPERTY, value: host }],
    },
  };
}

/** Replace only serverip. Other properties, including password, stay on the object. */
export function withUpdatedHost(server: PublishServer, host: string): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === FTP_HOST_PROPERTY) {
        found = true;
        return { ...property, key: FTP_HOST_PROPERTY, value: host };
      }
      return property;
    });
    if (!found) {
      next.push({ key: FTP_HOST_PROPERTY, value: host });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return { ...server, properties: { ...props, [FTP_HOST_PROPERTY]: host } };
  }
  return { ...server, properties: [{ key: FTP_HOST_PROPERTY, value: host }] };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function ftpHostHttpFailure(status: number): FtpHostHttpFailure | null {
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
