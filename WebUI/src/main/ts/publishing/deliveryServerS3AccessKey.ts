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
 * {@code PSAmazonS3DeliveryAccessKeyUpdate.MAX_S3_ACCESS_KEY_LENGTH}.
 */
export const S3_ACCESS_KEY_MAX_LENGTH = 255;

export const S3_ACCESS_KEY_PROPERTY = "accesskey";

export const S3_ACCESS_KEY_REQUIRED = "S3 access key is required";

export const S3_ACCESS_KEY_TOO_LONG =
  "S3 access key must be 255 characters or fewer";

export const S3_ACCESS_KEY_DRIVER_ONLY =
  "S3 access key can be set only on an Amazon S3 delivery server.";

/** The stored secret is never shown. The summary carries this fixed status instead. */
export const S3_ACCESS_KEY_NEVER_SHOWN = "Stored access key is never shown.";

export type S3AccessKeyResult =
  | { ok: true; accessKey: string }
  | { ok: false; error: string };

export type S3AccessKeyHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * A blank (after trim) access key is rejected and does not clear the stored
 * key. Overlong text is rejected before any update request. The accepted value
 * is sent as typed (base64-encoded by {@link #buildS3AccessKeyBody}).
 */
export function validateS3AccessKey(raw: string): S3AccessKeyResult {
  const accessKey = raw.trim();
  if (!accessKey) {
    return { ok: false, error: S3_ACCESS_KEY_REQUIRED };
  }
  if (accessKey.length > S3_ACCESS_KEY_MAX_LENGTH) {
    return { ok: false, error: S3_ACCESS_KEY_TOO_LONG };
  }
  return { ok: true, accessKey };
}

/**
 * Access-key-only delivery-server PUT body. Bucket, region, secret key,
 * driver, and name are omitted so the server leaves them stored. The key
 * travels base64-encoded (product norm, see modelToSaveBody); plain text is
 * kept only when encoding fails (non-latin1), which the server also accepts.
 */
export function buildS3AccessKeyBody(accessKey: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  let value = accessKey;
  try {
    value = btoa(accessKey);
  } catch {
    /* keep plain if btoa fails (non-latin1); the server decodes defensively */
  }
  return {
    serverInfo: {
      properties: [{ key: S3_ACCESS_KEY_PROPERTY, value }],
    },
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function s3AccessKeyHttpFailure(
  status: number,
): S3AccessKeyHttpFailure | null {
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
 * Render-safe snapshot of an access-key PUT body for debug/error paths: the
 * secret value is always redacted, never the encoded (or plain) key.
 *
 * The wire shape carries secrets as `{ key: "accesskey", value }` entries,
 * which the generic map-key redactor does not see, so secret-bearing entries
 * are redacted explicitly before delegating.
 */
export function redactS3AccessKeyBodyForLog(body: unknown): unknown {
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
