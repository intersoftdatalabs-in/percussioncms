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
 * {@code PSAmazonS3DeliverySecretKeyUpdate.MAX_S3_SECRET_KEY_LENGTH}.
 */
export const S3_SECRET_KEY_MAX_LENGTH = 255;

export const S3_SECRET_KEY_PROPERTY = "securitykey";

export const S3_SECRET_KEY_REQUIRED = "S3 secret key is required";

export const S3_SECRET_KEY_TOO_LONG =
  "S3 secret key must be 255 characters or fewer";

export const S3_SECRET_KEY_DRIVER_ONLY =
  "S3 secret key can be set only on an Amazon S3 delivery server.";

/** The stored secret is never shown. The summary carries this fixed status instead. */
export const S3_SECRET_KEY_NEVER_SHOWN = "Stored secret key is never shown.";

export type S3SecretKeyResult =
  | { ok: true; secretKey: string }
  | { ok: false; error: string };

export type S3SecretKeyHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * A blank (after trim) secret key is rejected and does not clear the stored
 * key. Overlong text is rejected before any update request. The accepted value
 * is sent as typed (base64-encoded by {@link #buildS3SecretKeyBody}).
 */
export function validateS3SecretKey(raw: string): S3SecretKeyResult {
  const secretKey = raw.trim();
  if (!secretKey) {
    return { ok: false, error: S3_SECRET_KEY_REQUIRED };
  }
  if (secretKey.length > S3_SECRET_KEY_MAX_LENGTH) {
    return { ok: false, error: S3_SECRET_KEY_TOO_LONG };
  }
  return { ok: true, secretKey };
}

/**
 * Secret-key-only delivery-server PUT body. Bucket, region, access key,
 * driver, and name are omitted so the server leaves them stored. The key
 * travels base64-encoded (product norm, see modelToSaveBody); plain text is
 * kept only when encoding fails (non-latin1), which the server also accepts.
 */
export function buildS3SecretKeyBody(secretKey: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  let value = secretKey;
  try {
    value = btoa(secretKey);
  } catch {
    /* keep plain if btoa fails (non-latin1); the server decodes defensively */
  }
  return {
    serverInfo: {
      properties: [{ key: S3_SECRET_KEY_PROPERTY, value }],
    },
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function s3SecretKeyHttpFailure(
  status: number,
): S3SecretKeyHttpFailure | null {
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
 * Render-safe snapshot of a secret-key PUT body for debug/error paths: the
 * secret value is always redacted, never the encoded (or plain) key.
 *
 * The wire shape carries secrets as `{ key: "securitykey", value }` entries,
 * which the generic map-key redactor does not see, so secret-bearing entries
 * are redacted explicitly before delegating.
 */
export function redactS3SecretKeyBodyForLog(body: unknown): unknown {
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
