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
  FTPS_FOLDER_MAX_LENGTH,
  FTPS_FOLDER_TOO_LONG,
  buildFtpsFolderBody,
  ftpsFolderHttpFailure,
  validateFtpsFolder,
  withUpdatedFtpsFolder,
} from "@/publishing/deliveryServerFtpsFolder";

const stored: PublishServer = {
  serverId: "9",
  serverName: "ftps-prod",
  properties: [
    { key: "driver", value: "FTPS" },
    { key: "serverip", value: "old.example" },
    { key: "port", value: "990" },
    { key: "userid", value: "ftpsuser" },
    { key: "folder", value: "/kept/folder" },
    { key: "password", value: "secret-pass" },
  ],
};

describe("validateFtpsFolder", () => {
  it("allows a blank folder to clear the stored folder", () => {
    expect(validateFtpsFolder("   ")).toEqual({ ok: true, folder: "" });
  });

  it("rejects an overlong folder", () => {
    expect(validateFtpsFolder("/".repeat(FTPS_FOLDER_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: FTPS_FOLDER_TOO_LONG,
    });
  });

  it("trims a folder", () => {
    expect(validateFtpsFolder("  /remote/new  ")).toEqual({ ok: true, folder: "/remote/new" });
  });
});

describe("buildFtpsFolderBody", () => {
  it("sends only folder", () => {
    const body = buildFtpsFolderBody("/remote/new");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "folder", value: "/remote/new" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("old.example");
    expect(json).not.toContain("ftpsuser");
    expect(json).not.toContain("/kept/folder");
    expect(json).not.toContain("password");
    expect(json).not.toContain("serverip");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("990");
    expect(json).not.toContain("port");
    expect(json).not.toContain("userid");
  });
});

describe("withUpdatedFtpsFolder", () => {
  it("replaces the folder and leaves host, port, user id, and password", () => {
    const next = withUpdatedFtpsFolder(stored, "/remote/new");
    expect(serverProperty(next, "folder")).toBe("/remote/new");
    expect(serverProperty(next, "serverip")).toBe("old.example");
    expect(serverProperty(next, "port")).toBe("990");
    expect(serverProperty(next, "userid")).toBe("ftpsuser");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "folder")).toBe("/kept/folder");
  });
});

describe("ftps folder guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(ftpsFolderHttpFailure(400)).toBe("bad_request");
    expect(ftpsFolderHttpFailure(403)).toBe("forbidden");
    expect(ftpsFolderHttpFailure(409)).toBe("conflict");
    expect(ftpsFolderHttpFailure(500)).toBeNull();
  });
});
