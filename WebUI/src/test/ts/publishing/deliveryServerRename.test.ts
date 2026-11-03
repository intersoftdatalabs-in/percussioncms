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
  DELIVERY_SERVER_NAME_DUPLICATE,
  DELIVERY_SERVER_NAME_INVALID,
  DELIVERY_SERVER_NAME_MAX_LENGTH,
  DELIVERY_SERVER_NAME_REQUIRED,
  DELIVERY_SERVER_NAME_TOO_LONG,
  buildDeliveryServerRenameBody,
  deliveryServerRenameHttpFailure,
  validateDeliveryServerRenameName,
  withUpdatedDeliveryServerName,
} from "@/publishing/deliveryServerRename";

const stored: PublishServer = {
  serverId: "9",
  serverName: "ftp-prod",
  type: "File",
  isDefault: true,
  properties: [
    { key: "driver", value: "FTP" },
    { key: "serverip", value: "old.example" },
    { key: "port", value: "2121" },
    { key: "userid", value: "ftpuser" },
    { key: "folder", value: "/kept/folder" },
    { key: "password", value: "secret-pass" },
  ],
};

describe("validateDeliveryServerRenameName", () => {
  it("rejects a blank name", () => {
    expect(validateDeliveryServerRenameName("   ", "ftp-prod", [])).toEqual({
      ok: false,
      error: DELIVERY_SERVER_NAME_REQUIRED,
    });
  });

  it("rejects an overlong name", () => {
    expect(
      validateDeliveryServerRenameName(
        "n".repeat(DELIVERY_SERVER_NAME_MAX_LENGTH + 1),
        "ftp-prod",
        [],
      ),
    ).toEqual({ ok: false, error: DELIVERY_SERVER_NAME_TOO_LONG });
  });

  it("rejects invalid characters", () => {
    expect(validateDeliveryServerRenameName("has space", "ftp-prod", [])).toEqual({
      ok: false,
      error: DELIVERY_SERVER_NAME_INVALID,
    });
  });

  it("rejects a duplicate name on the same site", () => {
    expect(
      validateDeliveryServerRenameName("Stage", "ftp-prod", ["ftp-prod", "stage"]),
    ).toEqual({ ok: false, error: DELIVERY_SERVER_NAME_DUPLICATE });
  });

  it("allows the current name and trims a valid rename", () => {
    expect(
      validateDeliveryServerRenameName("  ftp-prod  ", "ftp-prod", ["ftp-prod", "stage"]),
    ).toEqual({ ok: true, name: "ftp-prod" });
    expect(
      validateDeliveryServerRenameName("  Night-FTP  ", "ftp-prod", ["ftp-prod", "stage"]),
    ).toEqual({ ok: true, name: "Night-FTP" });
  });
});

describe("buildDeliveryServerRenameBody", () => {
  it("sends only serverName", () => {
    const body = buildDeliveryServerRenameBody("Night-FTP");
    expect(body).toEqual({ serverInfo: { serverName: "Night-FTP" } });
    expect(JSON.stringify(body)).not.toContain("secret-pass");
    expect(JSON.stringify(body)).not.toContain("password");
    expect(JSON.stringify(body)).not.toContain("driver");
    expect(JSON.stringify(body)).not.toContain("isDefault");
    expect(JSON.stringify(body)).not.toContain("properties");
    expect(JSON.stringify(body)).not.toContain("/kept/folder");
  });
});

describe("withUpdatedDeliveryServerName", () => {
  it("replaces the name and leaves driver, folder, default, and password", () => {
    const next = withUpdatedDeliveryServerName(stored, "Night-FTP");
    expect(next.serverName).toBe("Night-FTP");
    expect(next.isDefault).toBe(true);
    expect(next.type).toBe("File");
    expect(serverProperty(next, "driver")).toBe("FTP");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(stored.serverName).toBe("ftp-prod");
  });
});

describe("delivery server rename http guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(deliveryServerRenameHttpFailure(400)).toBe("bad_request");
    expect(deliveryServerRenameHttpFailure(403)).toBe("forbidden");
    expect(deliveryServerRenameHttpFailure(409)).toBe("conflict");
    expect(deliveryServerRenameHttpFailure(500)).toBeNull();
  });
});
