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

import { describe, expect, it } from "vitest";
import type { PublishServer } from "@/publishing/types";
import { serverProperty } from "@/publishing/deliveryServerFtpHost";
import {
  S3_BUCKET_MAX_LENGTH,
  S3_BUCKET_REQUIRED,
  S3_BUCKET_TOO_LONG,
  buildS3BucketBody,
  isS3DeliveryDriver,
  s3BucketHttpFailure,
  validateS3Bucket,
  withUpdatedS3Bucket,
} from "@/publishing/deliveryServerS3Bucket";

const ACCESS_KEY = "AKIATESTACCESS";
const SECRET_KEY = "secret-s3-key";

const stored: PublishServer = {
  serverId: "9",
  serverName: "s3-prod",
  isDefault: false,
  properties: [
    { key: "driver", value: "AMAZONS3" },
    { key: "bucketlocation", value: "old-bucket" },
    { key: "region", value: "us-west-2" },
    { key: "accesskey", value: ACCESS_KEY },
    { key: "securitykey", value: SECRET_KEY },
  ],
};

describe("validateS3Bucket", () => {
  it("rejects a blank bucket", () => {
    expect(validateS3Bucket("   ")).toEqual({
      ok: false,
      error: S3_BUCKET_REQUIRED,
    });
  });

  it("rejects an overlong bucket", () => {
    expect(validateS3Bucket("b".repeat(S3_BUCKET_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: S3_BUCKET_TOO_LONG,
    });
  });

  it("trims a bucket", () => {
    expect(validateS3Bucket("  new-bucket  ")).toEqual({
      ok: true,
      bucket: "new-bucket",
    });
  });
});

describe("isS3DeliveryDriver", () => {
  it("allows AMAZONS3 and S3 and rejects FTP, Local, and Oracle", () => {
    expect(isS3DeliveryDriver("AMAZONS3")).toBe(true);
    expect(isS3DeliveryDriver(" amazons3 ")).toBe(true);
    expect(isS3DeliveryDriver("S3")).toBe(true);
    expect(isS3DeliveryDriver("FTP")).toBe(false);
    expect(isS3DeliveryDriver("Local")).toBe(false);
    expect(isS3DeliveryDriver("Oracle")).toBe(false);
  });
});

describe("buildS3BucketBody", () => {
  it("sends only bucketlocation and omits secrets", () => {
    const body = buildS3BucketBody("new-bucket");
    expect(body).toEqual({
      serverInfo: {
        properties: [{ key: "bucketlocation", value: "new-bucket" }],
      },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain(SECRET_KEY);
    expect(json).not.toContain(ACCESS_KEY);
    expect(json).not.toContain("securitykey");
    expect(json).not.toContain("accesskey");
    expect(json).not.toContain("region");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("us-west-2");
  });
});

describe("withUpdatedS3Bucket", () => {
  it("replaces the bucket and leaves region, access key, and secret key", () => {
    const next = withUpdatedS3Bucket(stored, "new-bucket");
    expect(serverProperty(next, "bucketlocation")).toBe("new-bucket");
    expect(serverProperty(next, "region")).toBe("us-west-2");
    expect(serverProperty(next, "accesskey")).toBe(ACCESS_KEY);
    expect(serverProperty(next, "securitykey")).toBe(SECRET_KEY);
    expect(serverProperty(stored, "bucketlocation")).toBe("old-bucket");
  });
});

describe("s3 bucket guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(s3BucketHttpFailure(400)).toBe("bad_request");
    expect(s3BucketHttpFailure(403)).toBe("forbidden");
    expect(s3BucketHttpFailure(409)).toBe("conflict");
    expect(s3BucketHttpFailure(500)).toBeNull();
  });
});
