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
import type { PublishServer } from "@/publishing/types";
import { serverProperty } from "@/publishing/deliveryServerFtpHost";
import {
  SFTP_FOLDER_MAX_LENGTH,
  SFTP_FOLDER_TOO_LONG,
  buildSftpFolderBody,
  sftpFolderHttpFailure,
  validateSftpFolder,
  withUpdatedSftpFolder,
} from "@/publishing/deliveryServerSftpFolder";

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

describe("validateSftpFolder", () => {
  it("accepts a blank folder so the stored folder can be cleared", () => {
    expect(validateSftpFolder("   ")).toEqual({ ok: true, folder: "" });
  });

  it("rejects an overlong folder", () => {
    expect(validateSftpFolder("/".repeat(SFTP_FOLDER_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: SFTP_FOLDER_TOO_LONG,
    });
  });

  it("trims a folder", () => {
    expect(validateSftpFolder("  /remote/new  ")).toEqual({ ok: true, folder: "/remote/new" });
  });
});

describe("buildSftpFolderBody", () => {
  it("sends only folder", () => {
    const body = buildSftpFolderBody("/remote/new");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "folder", value: "/remote/new" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("secret-passphrase");
    expect(json).not.toContain("stored-private-key");
    expect(json).not.toContain("sftp.example");
    expect(json).not.toContain("sftpuser");
    expect(json).not.toContain("password");
    expect(json).not.toContain("privateKey");
    expect(json).not.toContain("serverip");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("userid");
    expect(json).not.toContain("port");
  });

  it("sends an empty folder to clear it", () => {
    const json = JSON.stringify(buildSftpFolderBody(""));
    expect(json).toContain('"value":""');
    expect(json).not.toContain("secret-passphrase");
    expect(json).not.toContain("stored-private-key");
    expect(json).not.toContain("/kept/folder");
  });
});

describe("withUpdatedSftpFolder", () => {
  it("replaces the folder and leaves host, port, user, and secrets", () => {
    const next = withUpdatedSftpFolder(stored, "/remote/new");
    expect(serverProperty(next, "folder")).toBe("/remote/new");
    expect(serverProperty(next, "serverip")).toBe("sftp.example");
    expect(serverProperty(next, "port")).toBe("22");
    expect(serverProperty(next, "userid")).toBe("sftpuser");
    expect(serverProperty(next, "password")).toBe("secret-passphrase");
    expect(serverProperty(next, "privateKey")).toBe("stored-private-key");
    expect(serverProperty(stored, "folder")).toBe("/kept/folder");
  });

  it("clears the folder without dropping the other properties", () => {
    const next = withUpdatedSftpFolder(stored, "");
    expect(serverProperty(next, "folder")).toBe("");
    expect(serverProperty(next, "password")).toBe("secret-passphrase");
    expect(serverProperty(next, "privateKey")).toBe("stored-private-key");
    expect(serverProperty(next, "userid")).toBe("sftpuser");
  });

  it("adds a folder when the property list does not have one", () => {
    const next = withUpdatedSftpFolder(
      { properties: [{ key: "serverip", value: "sftp.example" }] },
      "/remote/new",
    );
    expect(serverProperty(next, "folder")).toBe("/remote/new");
    expect(serverProperty(next, "serverip")).toBe("sftp.example");
  });

  it("updates a property map", () => {
    const next = withUpdatedSftpFolder(
      {
        properties: {
          serverip: "sftp.example",
          folder: "/kept/folder",
          password: "secret-passphrase",
        },
      },
      "/remote/new",
    );
    expect(serverProperty(next, "folder")).toBe("/remote/new");
    expect(serverProperty(next, "password")).toBe("secret-passphrase");
  });
});

describe("sftp folder guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(sftpFolderHttpFailure(400)).toBe("bad_request");
    expect(sftpFolderHttpFailure(403)).toBe("forbidden");
    expect(sftpFolderHttpFailure(409)).toBe("conflict");
    expect(sftpFolderHttpFailure(500)).toBeNull();
  });
});
