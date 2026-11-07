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
  SFTP_USERNAME_MAX_LENGTH,
  SFTP_USERNAME_REQUIRED,
  SFTP_USERNAME_TOO_LONG,
  buildSftpUsernameBody,
  sftpUsernameHttpFailure,
  validateSftpUsername,
  withUpdatedSftpUsername,
} from "@/publishing/deliveryServerSftpUsername";
import { isSftpDeliveryDriver } from "@/publishing/deliveryServerSftpPrivateKey";
import { serverProperty } from "@/publishing/deliveryServerFtpHost";

const stored: PublishServer = {
  serverId: "9",
  serverName: "sftp-prod",
  properties: [
    { key: "driver", value: "SFTP" },
    { key: "serverip", value: "sftp.example" },
    { key: "port", value: "22" },
    { key: "userid", value: "sftpuser" },
    { key: "folder", value: "/kept/folder" },
    { key: "password", value: "secret-passphrase" },
    { key: "privateKey", value: "stored-private-key" },
  ],
};

describe("validateSftpUsername", () => {
  it("rejects a blank username", () => {
    expect(validateSftpUsername("   ")).toEqual({ ok: false, error: SFTP_USERNAME_REQUIRED });
  });

  it("rejects an overlong username", () => {
    expect(validateSftpUsername("u".repeat(SFTP_USERNAME_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: SFTP_USERNAME_TOO_LONG,
    });
  });

  it("trims a username that fits", () => {
    expect(validateSftpUsername("  nightsftp  ")).toEqual({ ok: true, username: "nightsftp" });
  });
});

describe("buildSftpUsernameBody", () => {
  it("sends only userid", () => {
    const body = buildSftpUsernameBody("nightsftp");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "userid", value: "nightsftp" }] },
    });
    expect(JSON.stringify(body)).not.toContain("secret-passphrase");
    expect(JSON.stringify(body)).not.toContain("stored-private-key");
    expect(JSON.stringify(body)).not.toContain("sftp.example");
    expect(JSON.stringify(body)).not.toContain("sftpuser");
    expect(JSON.stringify(body)).not.toContain("/kept/folder");
    expect(JSON.stringify(body)).not.toContain("password");
    expect(JSON.stringify(body)).not.toContain("privateKey");
    expect(JSON.stringify(body)).not.toContain("driver");
  });
});

describe("withUpdatedSftpUsername", () => {
  it("replaces the username and leaves host, port, folder, and secrets", () => {
    const next = withUpdatedSftpUsername(stored, "nightsftp");
    expect(serverProperty(next, "userid")).toBe("nightsftp");
    expect(serverProperty(next, "serverip")).toBe("sftp.example");
    expect(serverProperty(next, "port")).toBe("22");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-passphrase");
    expect(serverProperty(next, "privateKey")).toBe("stored-private-key");
    expect(serverProperty(stored, "userid")).toBe("sftpuser");
  });
});

describe("sftp username guards", () => {
  it("accepts only the SFTP driver", () => {
    expect(isSftpDeliveryDriver("SFTP")).toBe(true);
    expect(isSftpDeliveryDriver(" sftp ")).toBe(true);
    expect(isSftpDeliveryDriver("FTP")).toBe(false);
    expect(isSftpDeliveryDriver("FTPS")).toBe(false);
    expect(isSftpDeliveryDriver("Local")).toBe(false);
  });

  it("maps 400, 403, and 409", () => {
    expect(sftpUsernameHttpFailure(400)).toBe("bad_request");
    expect(sftpUsernameHttpFailure(403)).toBe("forbidden");
    expect(sftpUsernameHttpFailure(409)).toBe("conflict");
    expect(sftpUsernameHttpFailure(500)).toBeNull();
  });
});
