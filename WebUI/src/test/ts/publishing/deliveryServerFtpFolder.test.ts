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
  FTP_FOLDER_MAX_LENGTH,
  FTP_FOLDER_TOO_LONG,
  buildFtpFolderBody,
  ftpFolderHttpFailure,
  validateFtpFolder,
  withUpdatedFolder,
} from "@/publishing/deliveryServerFtpFolder";

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

describe("validateFtpFolder", () => {
  it("accepts a blank folder so the stored folder can be cleared", () => {
    expect(validateFtpFolder("   ")).toEqual({ ok: true, folder: "" });
  });

  it("rejects an overlong folder", () => {
    expect(validateFtpFolder("/".repeat(FTP_FOLDER_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: FTP_FOLDER_TOO_LONG,
    });
  });

  it("trims a folder", () => {
    expect(validateFtpFolder("  /remote/new  ")).toEqual({ ok: true, folder: "/remote/new" });
  });
});

describe("buildFtpFolderBody", () => {
  it("sends only folder", () => {
    const body = buildFtpFolderBody("/remote/new");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "folder", value: "/remote/new" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("old.example");
    expect(json).not.toContain("ftpuser");
    expect(json).not.toContain("password");
    expect(json).not.toContain("serverip");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("2121");
    expect(json).not.toContain("userid");
    expect(json).not.toContain("port");
  });

  it("sends an empty folder to clear it", () => {
    const json = JSON.stringify(buildFtpFolderBody(""));
    expect(json).toContain('"value":""');
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("/kept/folder");
  });
});

describe("withUpdatedFolder", () => {
  it("replaces the folder and leaves host, port, user, and password", () => {
    const next = withUpdatedFolder(stored, "/remote/new");
    expect(serverProperty(next, "folder")).toBe("/remote/new");
    expect(serverProperty(next, "serverip")).toBe("old.example");
    expect(serverProperty(next, "port")).toBe("2121");
    expect(serverProperty(next, "userid")).toBe("ftpuser");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "folder")).toBe("/kept/folder");
  });

  it("clears the folder without dropping the other properties", () => {
    const next = withUpdatedFolder(stored, "");
    expect(serverProperty(next, "folder")).toBe("");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(next, "userid")).toBe("ftpuser");
  });

  it("adds a folder when the property list does not have one", () => {
    const next = withUpdatedFolder(
      { properties: [{ key: "serverip", value: "old.example" }] },
      "/remote/new",
    );
    expect(serverProperty(next, "folder")).toBe("/remote/new");
    expect(serverProperty(next, "serverip")).toBe("old.example");
  });

  it("updates a property map", () => {
    const next = withUpdatedFolder(
      {
        properties: {
          serverip: "old.example",
          folder: "/kept/folder",
          password: "secret-pass",
        },
      },
      "/remote/new",
    );
    expect(serverProperty(next, "folder")).toBe("/remote/new");
    expect(serverProperty(next, "password")).toBe("secret-pass");
  });
});

describe("ftp folder guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(ftpFolderHttpFailure(400)).toBe("bad_request");
    expect(ftpFolderHttpFailure(403)).toBe("forbidden");
    expect(ftpFolderHttpFailure(409)).toBe("conflict");
    expect(ftpFolderHttpFailure(500)).toBeNull();
  });
});
