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

import { describe, expect, it } from "vitest";
import {
  S3_SECRET_KEY_MAX_LENGTH,
  S3_SECRET_KEY_REQUIRED,
  S3_SECRET_KEY_TOO_LONG,
  buildS3SecretKeyBody,
  redactS3SecretKeyBodyForLog,
  s3SecretKeyHttpFailure,
  validateS3SecretKey,
} from "@/publishing/deliveryServerS3SecretKey";

const ACCESS_KEY = "AKIATESTACCESS";
const SECRET_KEY = "secret-s3-key";

describe("validateS3SecretKey", () => {
  it("rejects a blank secret key", () => {
    expect(validateS3SecretKey("   ")).toEqual({
      ok: false,
      error: S3_SECRET_KEY_REQUIRED,
    });
  });

  it("rejects an overlong secret key", () => {
    expect(validateS3SecretKey("k".repeat(S3_SECRET_KEY_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: S3_SECRET_KEY_TOO_LONG,
    });
  });

  it("trims a secret key", () => {
    expect(validateS3SecretKey("  new-secret-key  ")).toEqual({
      ok: true,
      secretKey: "new-secret-key",
    });
  });
});

describe("buildS3SecretKeyBody", () => {
  it("sends only the encoded secret key and omits other secrets", () => {
    const body = buildS3SecretKeyBody(SECRET_KEY);
    expect(body).toEqual({
      serverInfo: {
        properties: [{ key: "securitykey", value: btoa(SECRET_KEY) }],
      },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain(ACCESS_KEY);
    expect(json).not.toContain("accesskey");
    expect(json).not.toContain("bucketlocation");
    expect(json).not.toContain("region");
    expect(json).not.toContain("driver");
  });

  it("never sends the plain secret key on the wire", () => {
    const json = JSON.stringify(buildS3SecretKeyBody(SECRET_KEY));
    expect(json).not.toContain(SECRET_KEY);
  });
});

describe("redactS3SecretKeyBodyForLog", () => {
  it("redacts the secret key value", () => {
    const redacted = JSON.stringify(
      redactS3SecretKeyBodyForLog(buildS3SecretKeyBody(SECRET_KEY)),
    );
    expect(redacted).not.toContain(btoa(SECRET_KEY));
    expect(redacted).toContain("[REDACTED]");
  });
});

describe("s3 secret key guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(s3SecretKeyHttpFailure(400)).toBe("bad_request");
    expect(s3SecretKeyHttpFailure(403)).toBe("forbidden");
    expect(s3SecretKeyHttpFailure(409)).toBe("conflict");
    expect(s3SecretKeyHttpFailure(500)).toBeNull();
  });
});
