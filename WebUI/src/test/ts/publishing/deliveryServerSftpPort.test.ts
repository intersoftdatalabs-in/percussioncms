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
  SFTP_PORT_MAX_LENGTH,
  SFTP_PORT_NOT_NUMERIC,
  SFTP_PORT_REQUIRED,
  SFTP_PORT_TOO_LONG,
  buildSftpPortBody,
  sftpPortHttpFailure,
  validateSftpPort,
  withUpdatedSftpPort,
} from "@/publishing/deliveryServerSftpPort";
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

describe("validateSftpPort", () => {
  it("rejects a blank port", () => {
    expect(validateSftpPort("   ")).toEqual({ ok: false, error: SFTP_PORT_REQUIRED });
  });

  it("rejects a non-numeric port", () => {
    for (const bad of ["21a", "21.5", "-1", "22 22"]) {
      expect(validateSftpPort(bad)).toEqual({ ok: false, error: SFTP_PORT_NOT_NUMERIC });
    }
  });

  it("rejects an overlong port", () => {
    expect(validateSftpPort("9".repeat(SFTP_PORT_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: SFTP_PORT_TOO_LONG,
    });
  });

  it("trims a port that fits", () => {
    expect(validateSftpPort("  2222  ")).toEqual({ ok: true, port: "2222" });
  });
});

describe("buildSftpPortBody", () => {
  it("sends only port", () => {
    const body = buildSftpPortBody("2222");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "port", value: "2222" }] },
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

describe("withUpdatedSftpPort", () => {
  it("replaces the port and leaves host, user, folder, and secrets", () => {
    const next = withUpdatedSftpPort(stored, "2222");
    expect(serverProperty(next, "port")).toBe("2222");
    expect(serverProperty(next, "serverip")).toBe("sftp.example");
    expect(serverProperty(next, "userid")).toBe("sftpuser");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-passphrase");
    expect(serverProperty(next, "privateKey")).toBe("stored-private-key");
    expect(serverProperty(stored, "port")).toBe("22");
  });
});

describe("sftp port guards", () => {
  it("accepts only the SFTP driver", () => {
    expect(isSftpDeliveryDriver("SFTP")).toBe(true);
    expect(isSftpDeliveryDriver(" sftp ")).toBe(true);
    expect(isSftpDeliveryDriver("FTP")).toBe(false);
    expect(isSftpDeliveryDriver("FTPS")).toBe(false);
    expect(isSftpDeliveryDriver("Local")).toBe(false);
  });

  it("maps 400, 403, and 409", () => {
    expect(sftpPortHttpFailure(400)).toBe("bad_request");
    expect(sftpPortHttpFailure(403)).toBe("forbidden");
    expect(sftpPortHttpFailure(409)).toBe("conflict");
    expect(sftpPortHttpFailure(500)).toBeNull();
  });
});
