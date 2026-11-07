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
  SFTP_PRIVATE_KEY_MAX_LENGTH,
  SFTP_PRIVATE_KEY_REQUIRED,
  SFTP_PRIVATE_KEY_TOO_LONG,
  buildSftpPrivateKeyBody,
  isSftpDeliveryDriver,
  redactSftpPrivateKeyBodyForLog,
  sftpPrivateKeyHttpFailure,
  validateSftpPrivateKey,
} from "@/publishing/deliveryServerSftpPrivateKey";

const PASSPHRASE = "sftp-passphrase";
const PRIVATE_KEY = "stored-private-key";

describe("isSftpDeliveryDriver", () => {
  it("matches SFTP case-insensitively", () => {
    expect(isSftpDeliveryDriver("SFTP")).toBe(true);
    expect(isSftpDeliveryDriver("sftp")).toBe(true);
    expect(isSftpDeliveryDriver("  SFTP  ")).toBe(true);
  });

  it("rejects FTP, S3, Local, and database drivers", () => {
    expect(isSftpDeliveryDriver("FTP")).toBe(false);
    expect(isSftpDeliveryDriver("FTPS")).toBe(false);
    expect(isSftpDeliveryDriver("AMAZONS3")).toBe(false);
    expect(isSftpDeliveryDriver("Local")).toBe(false);
    expect(isSftpDeliveryDriver("Oracle")).toBe(false);
    expect(isSftpDeliveryDriver(undefined)).toBe(false);
    expect(isSftpDeliveryDriver(null)).toBe(false);
  });
});

describe("validateSftpPrivateKey", () => {
  it("rejects a blank private key", () => {
    expect(validateSftpPrivateKey("   ")).toEqual({
      ok: false,
      error: SFTP_PRIVATE_KEY_REQUIRED,
    });
  });

  it("rejects an overlong private key", () => {
    expect(validateSftpPrivateKey("k".repeat(SFTP_PRIVATE_KEY_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: SFTP_PRIVATE_KEY_TOO_LONG,
    });
  });

  it("trims a private key", () => {
    expect(validateSftpPrivateKey("  new-private-key  ")).toEqual({
      ok: true,
      privateKey: "new-private-key",
    });
  });
});

describe("buildSftpPrivateKeyBody", () => {
  it("sends only the encoded private key and omits other secrets", () => {
    const body = buildSftpPrivateKeyBody(PRIVATE_KEY);
    expect(body).toEqual({
      serverInfo: {
        properties: [{ key: "privateKey", value: btoa(PRIVATE_KEY) }],
      },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain(PASSPHRASE);
    expect(json).not.toContain("password");
    expect(json).not.toContain("serverip");
    expect(json).not.toContain("userid");
    expect(json).not.toContain("driver");
  });

  it("never sends the plain private key on the wire", () => {
    const json = JSON.stringify(buildSftpPrivateKeyBody(PRIVATE_KEY));
    expect(json).not.toContain(PRIVATE_KEY);
  });
});

describe("sftpPrivateKeyHttpFailure", () => {
  it("maps 400, 403, and 409 and passes through other statuses", () => {
    expect(sftpPrivateKeyHttpFailure(400)).toBe("bad_request");
    expect(sftpPrivateKeyHttpFailure(403)).toBe("forbidden");
    expect(sftpPrivateKeyHttpFailure(409)).toBe("conflict");
    expect(sftpPrivateKeyHttpFailure(200)).toBeNull();
    expect(sftpPrivateKeyHttpFailure(500)).toBeNull();
  });
});

describe("redactSftpPrivateKeyBodyForLog", () => {
  it("redacts the private key value", () => {
    const redacted = JSON.stringify(
      redactSftpPrivateKeyBodyForLog(buildSftpPrivateKeyBody(PRIVATE_KEY)),
    );
    expect(redacted).not.toContain(PRIVATE_KEY);
    expect(redacted).not.toContain(btoa(PRIVATE_KEY));
    expect(redacted).toContain("[REDACTED]");
  });
});
