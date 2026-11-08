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
  SFTP_PASSPHRASE_MAX_LENGTH,
  SFTP_PASSPHRASE_PROPERTY,
  SFTP_PASSPHRASE_REQUIRED,
  SFTP_PASSPHRASE_TOO_LONG,
  buildSftpPassphraseBody,
  redactSftpPassphraseBodyForLog,
  sftpPassphraseHttpFailure,
  validateSftpPassphrase,
} from "@/publishing/deliveryServerSftpPassphrase";

const PASSPHRASE = "sftp-passphrase";
const PRIVATE_KEY = "stored-private-key";

describe("validateSftpPassphrase", () => {
  it("rejects a blank passphrase", () => {
    expect(validateSftpPassphrase("   ")).toEqual({
      ok: false,
      error: SFTP_PASSPHRASE_REQUIRED,
    });
  });

  it("rejects an overlong passphrase", () => {
    expect(validateSftpPassphrase("p".repeat(SFTP_PASSPHRASE_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: SFTP_PASSPHRASE_TOO_LONG,
    });
  });

  it("trims a passphrase", () => {
    expect(validateSftpPassphrase("  new-passphrase  ")).toEqual({
      ok: true,
      passphrase: "new-passphrase",
    });
  });
});

describe("buildSftpPassphraseBody", () => {
  it("sends only the encoded password property and omits other secrets", () => {
    const body = buildSftpPassphraseBody(PASSPHRASE);
    expect(body).toEqual({
      serverInfo: {
        properties: [{ key: SFTP_PASSPHRASE_PROPERTY, value: btoa(PASSPHRASE) }],
      },
    });
    expect(SFTP_PASSPHRASE_PROPERTY).toBe("password");
    const json = JSON.stringify(body);
    expect(json).not.toContain(PRIVATE_KEY);
    expect(json).not.toContain("privateKey");
    expect(json).not.toContain("serverip");
    expect(json).not.toContain("userid");
    expect(json).not.toContain("driver");
  });

  it("never sends the plain passphrase on the wire", () => {
    const json = JSON.stringify(buildSftpPassphraseBody(PASSPHRASE));
    expect(json).not.toContain(PASSPHRASE);
  });
});

describe("sftpPassphraseHttpFailure", () => {
  it("maps 400, 403, and 409 and passes through other statuses", () => {
    expect(sftpPassphraseHttpFailure(400)).toBe("bad_request");
    expect(sftpPassphraseHttpFailure(403)).toBe("forbidden");
    expect(sftpPassphraseHttpFailure(409)).toBe("conflict");
    expect(sftpPassphraseHttpFailure(200)).toBeNull();
    expect(sftpPassphraseHttpFailure(500)).toBeNull();
  });
});

describe("redactSftpPassphraseBodyForLog", () => {
  it("redacts the passphrase value", () => {
    const redacted = JSON.stringify(
      redactSftpPassphraseBodyForLog(buildSftpPassphraseBody(PASSPHRASE)),
    );
    expect(redacted).not.toContain(PASSPHRASE);
    expect(redacted).not.toContain(btoa(PASSPHRASE));
    expect(redacted).toContain("[REDACTED]");
  });
});
