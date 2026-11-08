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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { PublishServerProperty } from "./types";
import { isSecretPropertyKey, redactSecretsForLog } from "./serverSecrets";

/**
 * PSX_PUBSERVER_PROPERTIES.PROPERTYVALUE is VARCHAR(255). Keep in step with
 * {@code PSSftpDeliveryPassphraseUpdate.MAX_SFTP_PASSPHRASE_LENGTH}.
 */
export const SFTP_PASSPHRASE_MAX_LENGTH = 255;

/**
 * The SFTP key passphrase travels as the `password` property — the same wire
 * key the FTP-password-only path uses. The stored driver selects the SFTP
 * passphrase apply on the server, so this body must stay passphrase-only.
 */
export const SFTP_PASSPHRASE_PROPERTY = "password";

export const SFTP_PASSPHRASE_REQUIRED = "SFTP passphrase is required";

export const SFTP_PASSPHRASE_TOO_LONG =
  "SFTP passphrase must be 255 characters or fewer";

export const SFTP_PASSPHRASE_DRIVER_ONLY =
  "SFTP passphrase can be set only on an SFTP delivery server.";

/** The stored passphrase is never shown. The summary carries this fixed status instead. */
export const SFTP_PASSPHRASE_NEVER_SHOWN = "Stored passphrase is never shown.";

export type SftpPassphraseResult =
  | { ok: true; passphrase: string }
  | { ok: false; error: string };

export type SftpPassphraseHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * A blank (after trim) passphrase is rejected and does not clear the stored
 * passphrase. Overlong text is rejected before any update request. The accepted value
 * is sent as typed (base64-encoded by {@link #buildSftpPassphraseBody}).
 */
export function validateSftpPassphrase(raw: string): SftpPassphraseResult {
  const passphrase = raw.trim();
  if (!passphrase) {
    return { ok: false, error: SFTP_PASSPHRASE_REQUIRED };
  }
  if (passphrase.length > SFTP_PASSPHRASE_MAX_LENGTH) {
    return { ok: false, error: SFTP_PASSPHRASE_TOO_LONG };
  }
  return { ok: true, passphrase };
}

/**
 * Passphrase-only delivery-server PUT body. Host, port, user id, folder,
 * private key, driver, and name are omitted so the server leaves them stored.
 * The passphrase travels base64-encoded (product norm, see modelToSaveBody); plain
 * text is kept only when encoding fails (non-latin1), which the server also
 * accepts.
 */
export function buildSftpPassphraseBody(passphrase: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  let value = passphrase;
  try {
    value = btoa(passphrase);
  } catch {
    /* keep plain if btoa fails (non-latin1); the server decodes defensively */
  }
  return {
    serverInfo: {
      properties: [{ key: SFTP_PASSPHRASE_PROPERTY, value }],
    },
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function sftpPassphraseHttpFailure(
  status: number,
): SftpPassphraseHttpFailure | null {
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
 * Render-safe snapshot of a passphrase PUT body for debug/error paths: the secret
 * value is always redacted, never the encoded (or plain) passphrase.
 *
 * The wire shape carries secrets as `{ key: "password", value }` entries,
 * which the generic map-key redactor does not see, so secret-bearing entries
 * are redacted explicitly before delegating.
 */
export function redactSftpPassphraseBodyForLog(body: unknown): unknown {
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
