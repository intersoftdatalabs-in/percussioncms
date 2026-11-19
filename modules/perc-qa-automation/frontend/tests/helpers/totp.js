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
 * Zero-dependency RFC 4648 Base32 + RFC 6238 TOTP generator for Playwright QA specs.
 */

"use strict";

const crypto = require("node:crypto");

const BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

/**
 * Decode an RFC 4648 Base32 string (ignoring spaces, hyphens, and '=' padding).
 *
 * @param {string} input Base32-encoded key material
 * @returns {Buffer} decoded raw bytes
 */
function decodeBase32(input) {
  if (typeof input !== "string") {
    throw new Error("Base32 input must be a string.");
  }
  const normalized = input.replace(/[\s=-]/g, "").toUpperCase();
  if (!normalized) {
    throw new Error("Base32 input must not be empty.");
  }

  let bits = 0;
  let value = 0;
  const bytes = [];

  for (let i = 0; i < normalized.length; i += 1) {
    const idx = BASE32_ALPHABET.indexOf(normalized[i]);
    if (idx === -1) {
      throw new Error(`Invalid Base32 character '${normalized[i]}' at index ${i}.`);
    }
    value = (value << 5) | idx;
    bits += 5;
    if (bits >= 8) {
      bits -= 8;
      bytes.push((value >>> bits) & 0xff);
    }
  }

  return Buffer.from(bytes);
}

/**
 * Compute an RFC 4226 / RFC 6238 OTP code for a raw key buffer and integer time-step counter.
 *
 * @param {Buffer} keyBuffer decoded HMAC key bytes
 * @param {number|bigint} counter 64-bit time step
 * @param {number} [digits=6] output digits (1..10)
 * @param {string} [algorithm="sha1"] Node crypto HMAC digest algorithm
 * @returns {string} zero-padded OTP string
 */
function generateTotpWithCounter(keyBuffer, counter, digits = 6, algorithm = "sha1") {
  if (!Buffer.isBuffer(keyBuffer) || keyBuffer.length === 0) {
    throw new Error("keyBuffer must be a non-empty Buffer.");
  }
  const stepDigits = Number.isInteger(digits) && digits >= 1 && digits <= 10 ? digits : 6;
  const counterBuf = Buffer.alloc(8);
  counterBuf.writeBigUInt64BE(BigInt(counter));

  const digest = crypto.createHmac(algorithm, keyBuffer).update(counterBuf).digest();
  const offset = digest[digest.length - 1] & 0x0f;
  const binary =
    ((digest[offset] & 0x7f) << 24) |
    ((digest[offset + 1] & 0xff) << 16) |
    ((digest[offset + 2] & 0xff) << 8) |
    (digest[offset + 3] & 0xff);

  const modulus = 10 ** stepDigits;
  return String(binary % modulus).padStart(stepDigits, "0");
}

/**
 * Compute an RFC 6238 TOTP code from a Base32-encoded key.
 *
 * @param {string} base32Key Base32 TOTP setup key
 * @param {object} [options] optional generation parameters
 * @param {number} [options.digits=6] code length
 * @param {number} [options.periodSeconds=30] time step duration in seconds
 * @param {string} [options.algorithm="sha1"] HMAC digest ("sha1", "sha256", "sha512")
 * @param {number} [options.timestampMs=Date.now()] epoch timestamp in milliseconds
 * @returns {string} zero-padded TOTP code
 */
function generateTotp(base32Key, options = {}) {
  const {
    digits = 6,
    periodSeconds = 30,
    algorithm = "sha1",
    timestampMs = Date.now(),
  } = options;
  const stepSeconds = Number.isFinite(periodSeconds) && periodSeconds > 0 ? periodSeconds : 30;
  const counter = Math.floor(timestampMs / 1000 / stepSeconds);
  const keyBuffer = decodeBase32(base32Key);
  return generateTotpWithCounter(keyBuffer, counter, digits, algorithm);
}

module.exports = {
  decodeBase32,
  generateTotpWithCounter,
  generateTotp,
};
