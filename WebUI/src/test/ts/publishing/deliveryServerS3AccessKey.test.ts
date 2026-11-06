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
import {
  S3_ACCESS_KEY_MAX_LENGTH,
  S3_ACCESS_KEY_REQUIRED,
  S3_ACCESS_KEY_TOO_LONG,
  buildS3AccessKeyBody,
  redactS3AccessKeyBodyForLog,
  s3AccessKeyHttpFailure,
  validateS3AccessKey,
} from "@/publishing/deliveryServerS3AccessKey";

const ACCESS_KEY = "AKIATESTACCESS";
const SECRET_KEY = "secret-s3-key";

describe("validateS3AccessKey", () => {
  it("rejects a blank access key", () => {
    expect(validateS3AccessKey("   ")).toEqual({
      ok: false,
      error: S3_ACCESS_KEY_REQUIRED,
    });
  });

  it("rejects an overlong access key", () => {
    expect(validateS3AccessKey("k".repeat(S3_ACCESS_KEY_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: S3_ACCESS_KEY_TOO_LONG,
    });
  });

  it("trims an access key", () => {
    expect(validateS3AccessKey("  AKIANEWKEY  ")).toEqual({
      ok: true,
      accessKey: "AKIANEWKEY",
    });
  });
});

describe("buildS3AccessKeyBody", () => {
  it("sends only the encoded access key and omits other secrets", () => {
    const body = buildS3AccessKeyBody(ACCESS_KEY);
    expect(body).toEqual({
      serverInfo: {
        properties: [{ key: "accesskey", value: btoa(ACCESS_KEY) }],
      },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain(SECRET_KEY);
    expect(json).not.toContain("securitykey");
    expect(json).not.toContain("bucketlocation");
    expect(json).not.toContain("region");
    expect(json).not.toContain("driver");
  });

  it("never sends the plain access key on the wire", () => {
    const json = JSON.stringify(buildS3AccessKeyBody(ACCESS_KEY));
    expect(json).not.toContain(ACCESS_KEY);
  });
});

describe("redactS3AccessKeyBodyForLog", () => {
  it("redacts the access key value", () => {
    const redacted = JSON.stringify(
      redactS3AccessKeyBodyForLog(buildS3AccessKeyBody(ACCESS_KEY)),
    );
    expect(redacted).not.toContain(btoa(ACCESS_KEY));
    expect(redacted).toContain("[REDACTED]");
  });
});

describe("s3 access key guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(s3AccessKeyHttpFailure(400)).toBe("bad_request");
    expect(s3AccessKeyHttpFailure(403)).toBe("forbidden");
    expect(s3AccessKeyHttpFailure(409)).toBe("conflict");
    expect(s3AccessKeyHttpFailure(500)).toBeNull();
  });
});
