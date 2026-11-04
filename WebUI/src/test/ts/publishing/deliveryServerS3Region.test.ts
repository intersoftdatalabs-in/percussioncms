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
  S3_REGION_MAX_LENGTH,
  S3_REGION_REQUIRED,
  S3_REGION_TOO_LONG,
  buildS3RegionBody,
  s3RegionHttpFailure,
  validateS3Region,
  withUpdatedS3Region,
} from "@/publishing/deliveryServerS3Region";

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

describe("validateS3Region", () => {
  it("rejects a blank region", () => {
    expect(validateS3Region("   ")).toEqual({
      ok: false,
      error: S3_REGION_REQUIRED,
    });
  });

  it("rejects an overlong region", () => {
    expect(validateS3Region("r".repeat(S3_REGION_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: S3_REGION_TOO_LONG,
    });
  });

  it("trims a region", () => {
    expect(validateS3Region("  us-east-1  ")).toEqual({
      ok: true,
      region: "us-east-1",
    });
  });
});

describe("buildS3RegionBody", () => {
  it("sends only region and omits secrets", () => {
    const body = buildS3RegionBody("us-east-1");
    expect(body).toEqual({
      serverInfo: {
        properties: [{ key: "region", value: "us-east-1" }],
      },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain(SECRET_KEY);
    expect(json).not.toContain(ACCESS_KEY);
    expect(json).not.toContain("securitykey");
    expect(json).not.toContain("accesskey");
    expect(json).not.toContain("bucketlocation");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("old-bucket");
  });
});

describe("withUpdatedS3Region", () => {
  it("replaces the region and leaves bucket, access key, and secret key", () => {
    const next = withUpdatedS3Region(stored, "us-east-1");
    expect(serverProperty(next, "region")).toBe("us-east-1");
    expect(serverProperty(next, "bucketlocation")).toBe("old-bucket");
    expect(serverProperty(next, "accesskey")).toBe(ACCESS_KEY);
    expect(serverProperty(next, "securitykey")).toBe(SECRET_KEY);
    expect(serverProperty(stored, "region")).toBe("us-west-2");
  });
});

describe("s3 region guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(s3RegionHttpFailure(400)).toBe("bad_request");
    expect(s3RegionHttpFailure(403)).toBe("forbidden");
    expect(s3RegionHttpFailure(409)).toBe("conflict");
    expect(s3RegionHttpFailure(500)).toBeNull();
  });
});
