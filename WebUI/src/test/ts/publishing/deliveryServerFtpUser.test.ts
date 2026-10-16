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
  FTP_USER_MAX_LENGTH,
  FTP_USER_REQUIRED,
  FTP_USER_TOO_LONG,
  buildFtpUserBody,
  ftpUserHttpFailure,
  validateFtpUser,
  withUpdatedUser,
} from "@/publishing/deliveryServerFtpUser";

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

describe("validateFtpUser", () => {
  it("rejects a blank user id", () => {
    expect(validateFtpUser("   ")).toEqual({ ok: false, error: FTP_USER_REQUIRED });
  });

  it("rejects an overlong user id", () => {
    expect(validateFtpUser("u".repeat(FTP_USER_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: FTP_USER_TOO_LONG,
    });
  });

  it("trims a user id", () => {
    expect(validateFtpUser("  cmsftp  ")).toEqual({ ok: true, userId: "cmsftp" });
  });
});

describe("buildFtpUserBody", () => {
  it("sends only userid", () => {
    const body = buildFtpUserBody("cmsftp");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "userid", value: "cmsftp" }] },
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
    expect(json).not.toContain("port");
  });
});

describe("withUpdatedUser", () => {
  it("replaces the user id and leaves host, port, folder, and password", () => {
    const next = withUpdatedUser(stored, "cmsftp");
    expect(serverProperty(next, "userid")).toBe("cmsftp");
    expect(serverProperty(next, "serverip")).toBe("old.example");
    expect(serverProperty(next, "port")).toBe("2121");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "userid")).toBe("ftpuser");
  });
});

describe("ftp user guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(ftpUserHttpFailure(400)).toBe("bad_request");
    expect(ftpUserHttpFailure(403)).toBe("forbidden");
    expect(ftpUserHttpFailure(409)).toBe("conflict");
    expect(ftpUserHttpFailure(500)).toBeNull();
  });
});
