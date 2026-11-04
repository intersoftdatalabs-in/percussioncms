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
 * {@code PSAmazonS3DeliveryRegionUpdate.MAX_S3_REGION_LENGTH}.
 */
export const S3_REGION_MAX_LENGTH = 255;

export const S3_REGION_PROPERTY = "region";

export const S3_REGION_REQUIRED = "S3 region is required";

export const S3_REGION_TOO_LONG = "S3 region must be 255 characters or fewer";

export const S3_REGION_DRIVER_ONLY =
  "S3 region can be set only on an Amazon S3 delivery server.";

export type S3RegionResult =
  | { ok: true; region: string }
  | { ok: false; error: string };

export type S3RegionHttpFailure = "bad_request" | "forbidden" | "conflict";

/**
 * Blank (after trim) is rejected and does not clear the stored region.
 * Overlong text is rejected before any update request.
 */
export function validateS3Region(raw: string): S3RegionResult {
  const region = raw.trim();
  if (!region) {
    return { ok: false, error: S3_REGION_REQUIRED };
  }
  if (region.length > S3_REGION_MAX_LENGTH) {
    return { ok: false, error: S3_REGION_TOO_LONG };
  }
  return { ok: true, region };
}

/**
 * Region-only delivery-server PUT body. Bucket, access key, secret key,
 * driver, and name are omitted so the server leaves them stored.
 */
export function buildS3RegionBody(region: string): {
  serverInfo: { properties: PublishServerProperty[] };
} {
  return {
    serverInfo: {
      properties: [{ key: S3_REGION_PROPERTY, value: region }],
    },
  };
}

/** Replace only region. Other properties, including secrets, stay. */
export function withUpdatedS3Region(
  server: PublishServer,
  region: string,
): PublishServer {
  const props = server.properties;
  if (Array.isArray(props)) {
    let found = false;
    const next = props.map((property) => {
      const key = property.key ?? property.name;
      if (key === S3_REGION_PROPERTY) {
        found = true;
        return { ...property, key: S3_REGION_PROPERTY, value: region };
      }
      return property;
    });
    if (!found) {
      next.push({ key: S3_REGION_PROPERTY, value: region });
    }
    return { ...server, properties: next };
  }
  if (props && typeof props === "object") {
    return {
      ...server,
      properties: { ...props, [S3_REGION_PROPERTY]: region },
    };
  }
  return {
    ...server,
    properties: [{ key: S3_REGION_PROPERTY, value: region }],
  };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function s3RegionHttpFailure(
  status: number,
): S3RegionHttpFailure | null {
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
