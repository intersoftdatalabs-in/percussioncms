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

import type { PublishServerProperty } from "./types";
import { isSecretPropertyKey, redactSecretsForLog } from "./serverSecrets";

/**
 * PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE is VARCHAR(255). Keep in step with
 * {@code PSFtpDeliveryPasswordUpdate.MAX_FTP_PASSWORD_LENGTH}.
 */
export const FTP_PASSWORD_MAX_LENGTH = 255;

export const FTP_PASSWORD_PROPERTY = "password";

export const FTP_PASSWORD_REQUIRED = "FTP password is required";

export const FTP_PASSWORD_TOO_LONG = "FTP password must be 255 characters or fewer";

export const FTP_PASSWORD_DRIVER_ONLY =
  "FTP password can be set only on an FTP delivery server.";

/** The stored secret is never shown. The summary carries this fixed status instead. */
export const FTP_PASSWORD_NEVER_SHOWN = "Stored password is never shown.";

export type FtpPasswordResult = { ok: true; password: string } | { ok: false; error: string };

export type FtpPasswordHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * An empty or whitespace-only password is rejected and does not clear the stored secret.
 * Overlong input is rejected before any update request. The publish-server wire layer
 * ({@code PSPublishServerProperty#setValue}) trims surrounding whitespace, so a padded
 * password is stored trimmed — the same norm as the full-server edit. The accepted value
 * is sent as typed (base64-encoded by {@link #buildFtpPasswordBody}).
 */
export function validateFtpPassword(raw: string): FtpPasswordResult {
  if (!raw || !raw.trim()) {
    return { ok: false, error: FTP_PASSWORD_REQUIRED };
  }
  if (raw.length > FTP_PASSWORD_MAX_LENGTH) {
    return { ok: false, error: FTP_PASSWORD_TOO_LONG };
  }
  return { ok: true, password: raw };
}

/**
 * Password-only delivery-server PUT body. Host, port, user id, folder, driver,
 * and name are omitted so the server leaves them stored. The password travels
 * base64-encoded (product norm, see modelToSaveBody); plain text is kept only
 * when encoding fails (non-latin1), which the server also accepts.
 */
export function buildFtpPasswordBody(password: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  let value = password;
  try {
    value = btoa(password);
  } catch {
    /* keep plain if btoa fails (non-latin1); the server decodes defensively */
  }
  return {
    serverInfo: {
      properties: [{ key: FTP_PASSWORD_PROPERTY, value }],
    },
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function ftpPasswordHttpFailure(status: number): FtpPasswordHttpFailure | null {
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

/**
 * Render-safe snapshot of a password PUT body for debug/error paths: the secret
 * value is always redacted, never the encoded (or plain) password.
 *
 * The wire shape carries secrets as `{ key: "password", value }` entries, which
 * the generic map-key redactor does not see, so secret-bearing entries are
 * redacted explicitly before delegating.
 */
export function redactFtpPasswordBodyForLog(body: unknown): unknown {
  if (body !== null && typeof body === "object" && !Array.isArray(body)) {
    const root = body as Record<string, unknown>;
    const serverInfo = root.serverInfo;
    if (serverInfo !== null && typeof serverInfo === "object" && !Array.isArray(serverInfo)) {
      const info = serverInfo as Record<string, unknown>;
      if (Array.isArray(info.properties)) {
        return {
          ...root,
          serverInfo: {
            ...info,
            properties: info.properties.map((property: unknown) => {
              if (
                property !== null &&
                typeof property === "object" &&
                !Array.isArray(property) &&
                isSecretPropertyKey(
                  String((property as Record<string, unknown>).key ?? ""),
                )
              ) {
                return { ...(property as Record<string, unknown>), value: "[REDACTED]" };
              }
              return redactSecretsForLog(property);
            }),
          },
        };
      }
    }
  }
  return redactSecretsForLog(body);
}
