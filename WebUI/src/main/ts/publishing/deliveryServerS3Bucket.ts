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
 * {@code PSAmazonS3DeliveryBucketUpdate.MAX_S3_BUCKET_LENGTH}.
 */
export const S3_BUCKET_MAX_LENGTH = 255;

export const S3_BUCKET_PROPERTY = "bucketlocation";

export const S3_BUCKET_REQUIRED = "S3 bucket is required";

export const S3_BUCKET_TOO_LONG = "S3 bucket must be 255 characters or fewer";

export const S3_BUCKET_DRIVER_ONLY =
  "S3 bucket can be set only on an Amazon S3 delivery server.";

export type S3BucketResult =
  | { ok: true; bucket: string }
  | { ok: false; error: string };

export type S3BucketHttpFailure = "bad_request" | "forbidden" | "conflict";

export function isS3DeliveryDriver(driver: string | undefined | null): boolean {
  const normalized = (driver ?? "").trim().toUpperCase();
  return normalized === "AMAZONS3" || normalized === "S3";
}

/**
 * Blank (after trim) is rejected and does not clear the stored bucket.
 * Overlong text is rejected before any update request.
 */
export function validateS3Bucket(raw: string): S3BucketResult {
  const bucket = raw.trim();
  if (!bucket) {
    return { ok: false, error: S3_BUCKET_REQUIRED };
  }
  if (bucket.length > S3_BUCKET_MAX_LENGTH) {
    return { ok: false, error: S3_BUCKET_TOO_LONG };
  }
  return { ok: true, bucket };
}

/**
 * Bucket-only delivery-server PUT body. Region, access key, secret key,
 * driver, and name are omitted so the server leaves them stored.
 */
export function buildS3BucketBody(bucket: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: S3_BUCKET_PROPERTY, value: bucket }],
    },
  };
}

/** Replace only bucketlocation. Other properties, including secrets, stay. */
export function withUpdatedS3Bucket(
  server: PublishServer,
  bucket: string,
): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === S3_BUCKET_PROPERTY) {
        found = true;
        return { ...property, key: S3_BUCKET_PROPERTY, value: bucket };
      }
      return property;
    });
    if (!found) {
      next.push({ key: S3_BUCKET_PROPERTY, value: bucket });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return {
      ...server,
      properties: { ...props, [S3_BUCKET_PROPERTY]: bucket },
    };
  }
  return {
    ...server,
    properties: [{ key: S3_BUCKET_PROPERTY, value: bucket }],
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function s3BucketHttpFailure(
  status: number,
): S3BucketHttpFailure | null {
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
