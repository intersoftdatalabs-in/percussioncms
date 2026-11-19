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

/**
 * Unit tests for the zero-dependency RFC 6238 TOTP Playwright helper.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  decodeBase32,
  generateTotpWithCounter,
  generateTotp,
} = require("../helpers/totp");

// RFC 6238 Appendix B standard public SHA-1 test vector ("12345678901234567890").
// mkd-code-review[secrets.heuristic]: adjudicated FP (issue #53, precedent #5946) - RFC 6238 Appendix B public test vector, not a credential.
const RFC6238_SHA1_VECTOR_B32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

describe("totp Playwright helper (RFC 6238)", () => {
  it("matches RFC 6238 Appendix B SHA-1 8-digit and 6-digit test vectors", () => {
    const vectors = [
      { epochSec: 59, expected8: "94287082", expected6: "287082" },
      { epochSec: 1111111109, expected8: "07081804", expected6: "081804" },
      { epochSec: 1111111111, expected8: "14050471", expected6: "050471" },
      { epochSec: 1234567890, expected8: "89005924", expected6: "005924" },
    ];

    for (const v of vectors) {
      assert.equal(
        generateTotp(RFC6238_SHA1_VECTOR_B32, {
          digits: 8,
          periodSeconds: 30,
          algorithm: "sha1",
          timestampMs: v.epochSec * 1000,
        }),
        v.expected8,
      );
      assert.equal(
        generateTotp(RFC6238_SHA1_VECTOR_B32, {
          digits: 6,
          periodSeconds: 30,
          algorithm: "sha1",
          timestampMs: v.epochSec * 1000,
        }),
        v.expected6,
      );
    }
  });

  it("normalizes Base32 whitespace, hyphens, lowercase, and '=' padding", () => {
    const raw = decodeBase32("  gezd gnbv-gy3t qojq= ");
    assert.equal(raw.toString("ascii"), "1234567890");
    assert.equal(
      generateTotpWithCounter(Buffer.from("12345678901234567890", "ascii"), 1, 8, "sha1"),
      "94287082",
    );
  });

  it("rejects empty or invalid Base32 inputs", () => {
    assert.throws(() => decodeBase32(""), /must not be empty/i);
    assert.throws(() => decodeBase32("ABCD1EFG"), /Invalid Base32 character/i);
  });
});
