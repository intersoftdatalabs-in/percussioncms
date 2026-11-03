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
  LOCAL_FOLDER_MAX_LENGTH,
  LOCAL_FOLDER_TOO_LONG,
  buildLocalFolderBody,
  isLocalDeliveryDriver,
  localFolderHttpFailure,
  validateLocalFolder,
  withUpdatedLocalFolder,
} from "@/publishing/deliveryServerLocalFolder";

const stored: PublishServer = {
  serverId: "3",
  serverName: "local-files",
  properties: [
    { key: "driver", value: "Local" },
    { key: "folder", value: "/kept/local" },
  ],
};

describe("isLocalDeliveryDriver", () => {
  it("accepts Local only", () => {
    expect(isLocalDeliveryDriver("Local")).toBe(true);
    expect(isLocalDeliveryDriver(" local ")).toBe(true);
    expect(isLocalDeliveryDriver("FTP")).toBe(false);
    expect(isLocalDeliveryDriver("FTPS")).toBe(false);
    expect(isLocalDeliveryDriver("SFTP")).toBe(false);
    expect(isLocalDeliveryDriver("AMAZONS3")).toBe(false);
  });
});

describe("validateLocalFolder", () => {
  it("accepts a blank folder so the stored folder can be cleared", () => {
    expect(validateLocalFolder("   ")).toEqual({ ok: true, folder: "" });
  });

  it("rejects an overlong folder", () => {
    expect(validateLocalFolder("/".repeat(LOCAL_FOLDER_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: LOCAL_FOLDER_TOO_LONG,
    });
  });

  it("trims a folder and keeps typed separators", () => {
    expect(validateLocalFolder("  /night-local  ")).toEqual({
      ok: true,
      folder: "/night-local",
    });
    expect(validateLocalFolder("  C:\\publish\\out  ")).toEqual({
      ok: true,
      folder: "C:\\publish\\out",
    });
  });
});

describe("buildLocalFolderBody", () => {
  it("sends only folder", () => {
    const body = buildLocalFolderBody("/night-local");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "folder", value: "/night-local" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("driver");
    expect(json).not.toContain("Local");
    expect(json).not.toContain("serverip");
    expect(json).not.toContain("userid");
    expect(json).not.toContain("password");
    expect(json).not.toContain("port");
    expect(json).not.toContain("local-files");
  });

  it("sends an empty folder to clear it", () => {
    const json = JSON.stringify(buildLocalFolderBody(""));
    expect(json).toContain('"value":""');
    expect(json).not.toContain("/kept/local");
  });
});

describe("withUpdatedLocalFolder", () => {
  it("replaces the folder and leaves driver and name", () => {
    const next = withUpdatedLocalFolder(stored, "/night-local");
    expect(serverProperty(next, "folder")).toBe("/night-local");
    expect(serverProperty(next, "driver")).toBe("Local");
    expect(next.serverName).toBe("local-files");
    expect(serverProperty(stored, "folder")).toBe("/kept/local");
  });

  it("clears the folder without dropping the driver", () => {
    const next = withUpdatedLocalFolder(stored, "");
    expect(serverProperty(next, "folder")).toBe("");
    expect(serverProperty(next, "driver")).toBe("Local");
  });

  it("adds a folder when the property list does not have one", () => {
    const next = withUpdatedLocalFolder(
      { properties: [{ key: "driver", value: "Local" }] },
      "/night-local",
    );
    expect(serverProperty(next, "folder")).toBe("/night-local");
    expect(serverProperty(next, "driver")).toBe("Local");
  });

  it("updates a property map", () => {
    const next = withUpdatedLocalFolder(
      {
        properties: {
          driver: "Local",
          folder: "/kept/local",
        },
      },
      "/night-local",
    );
    expect(serverProperty(next, "folder")).toBe("/night-local");
    expect(serverProperty(next, "driver")).toBe("Local");
  });
});

describe("local folder guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(localFolderHttpFailure(400)).toBe("bad_request");
    expect(localFolderHttpFailure(403)).toBe("forbidden");
    expect(localFolderHttpFailure(409)).toBe("conflict");
    expect(localFolderHttpFailure(500)).toBeNull();
  });
});
