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

import type { PublishServer } from "@/publishing/types";
import {
  SFTP_HOST_MAX_LENGTH,
  SFTP_HOST_REQUIRED,
  SFTP_HOST_TOO_LONG,
  buildSftpHostBody,
  sftpHostHttpFailure,
  validateSftpHost,
  withUpdatedSftpHost,
} from "@/publishing/deliveryServerSftpHost";
import { isSftpDeliveryDriver } from "@/publishing/deliveryServerSftpPrivateKey";
import { serverProperty } from "@/publishing/deliveryServerFtpHost";

const stored: PublishServer = {
  serverId: "9",
  serverName: "sftp-prod",
  properties: [
    { key: "driver", value: "SFTP" },
    { key: "serverip", value: "old.example" },
    { key: "port", value: "22" },
    { key: "userid", value: "sftpuser" },
    { key: "folder", value: "/kept/folder" },
    { key: "password", value: "secret-passphrase" },
    { key: "privateKey", value: "stored-private-key" },
  ],
};

describe("validateSftpHost", () => {
  it("rejects a blank host", () => {
    expect(validateSftpHost("   ")).toEqual({ ok: false, error: SFTP_HOST_REQUIRED });
  });

  it("rejects an overlong host", () => {
    expect(validateSftpHost("h".repeat(SFTP_HOST_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: SFTP_HOST_TOO_LONG,
    });
  });

  it("trims a host that fits", () => {
    expect(validateSftpHost("  sftp.new.example  ")).toEqual({
      ok: true,
      host: "sftp.new.example",
    });
  });
});

describe("buildSftpHostBody", () => {
  it("sends only serverip", () => {
    const body = buildSftpHostBody("sftp.new.example");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "serverip", value: "sftp.new.example" }] },
    });
    expect(JSON.stringify(body)).not.toContain("secret-passphrase");
    expect(JSON.stringify(body)).not.toContain("stored-private-key");
    expect(JSON.stringify(body)).not.toContain("22");
    expect(JSON.stringify(body)).not.toContain("sftpuser");
    expect(JSON.stringify(body)).not.toContain("/kept/folder");
    expect(JSON.stringify(body)).not.toContain("password");
    expect(JSON.stringify(body)).not.toContain("privateKey");
    expect(JSON.stringify(body)).not.toContain("driver");
  });
});

describe("withUpdatedSftpHost", () => {
  it("replaces the host and leaves port, user, folder, and secrets", () => {
    const next = withUpdatedSftpHost(stored, "sftp.new.example");
    expect(serverProperty(next, "serverip")).toBe("sftp.new.example");
    expect(serverProperty(next, "port")).toBe("22");
    expect(serverProperty(next, "userid")).toBe("sftpuser");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-passphrase");
    expect(serverProperty(next, "privateKey")).toBe("stored-private-key");
    expect(serverProperty(stored, "serverip")).toBe("old.example");
  });
});

describe("sftp host guards", () => {
  it("accepts only the SFTP driver", () => {
    expect(isSftpDeliveryDriver("SFTP")).toBe(true);
    expect(isSftpDeliveryDriver(" sftp ")).toBe(true);
    expect(isSftpDeliveryDriver("FTP")).toBe(false);
    expect(isSftpDeliveryDriver("FTPS")).toBe(false);
    expect(isSftpDeliveryDriver("Local")).toBe(false);
  });

  it("maps 400, 403, and 409", () => {
    expect(sftpHostHttpFailure(400)).toBe("bad_request");
    expect(sftpHostHttpFailure(403)).toBe("forbidden");
    expect(sftpHostHttpFailure(409)).toBe("conflict");
    expect(sftpHostHttpFailure(500)).toBeNull();
  });
});
