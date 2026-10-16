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
  FTP_PORT_MAX_LENGTH,
  FTP_PORT_NOT_NUMERIC,
  FTP_PORT_REQUIRED,
  FTP_PORT_TOO_LONG,
  buildFtpPortBody,
  ftpPortHttpFailure,
  validateFtpPort,
  withUpdatedPort,
} from "@/publishing/deliveryServerFtpPort";

const stored: PublishServer = {
  serverId: "9",
  serverName: "ftp-prod",
  properties: [
    { key: "driver", value: "FTP" },
    { key: "serverip", value: "old.example" },
    { key: "port", value: "2121" },
    { key: "userid", value: "ftpuser" },
    { key: "folder", value: "/kept/folder" },
    { key: "password", value: "secret-pass" },
  ],
};

describe("validateFtpPort", () => {
  it("rejects a blank port", () => {
    expect(validateFtpPort("   ")).toEqual({ ok: false, error: FTP_PORT_REQUIRED });
  });

  it("rejects a non-numeric port", () => {
    expect(validateFtpPort("21a")).toEqual({ ok: false, error: FTP_PORT_NOT_NUMERIC });
    expect(validateFtpPort("21.5")).toEqual({ ok: false, error: FTP_PORT_NOT_NUMERIC });
    expect(validateFtpPort("-1")).toEqual({ ok: false, error: FTP_PORT_NOT_NUMERIC });
  });

  it("rejects an overlong port", () => {
    expect(validateFtpPort("9".repeat(FTP_PORT_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: FTP_PORT_TOO_LONG,
    });
  });

  it("trims a numeric port", () => {
    expect(validateFtpPort("  2222  ")).toEqual({ ok: true, port: "2222" });
  });
});

describe("buildFtpPortBody", () => {
  it("sends only port", () => {
    const body = buildFtpPortBody("2222");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "port", value: "2222" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("old.example");
    expect(json).not.toContain("ftpuser");
    expect(json).not.toContain("/kept/folder");
    expect(json).not.toContain("password");
    expect(json).not.toContain("serverip");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("2121");
  });
});

describe("withUpdatedPort", () => {
  it("replaces the port and leaves host, user, folder, and password", () => {
    const next = withUpdatedPort(stored, "2222");
    expect(serverProperty(next, "port")).toBe("2222");
    expect(serverProperty(next, "serverip")).toBe("old.example");
    expect(serverProperty(next, "userid")).toBe("ftpuser");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "port")).toBe("2121");
  });
});

describe("ftp port guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(ftpPortHttpFailure(400)).toBe("bad_request");
    expect(ftpPortHttpFailure(403)).toBe("forbidden");
    expect(ftpPortHttpFailure(409)).toBe("conflict");
    expect(ftpPortHttpFailure(500)).toBeNull();
  });
});
