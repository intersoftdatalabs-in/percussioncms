/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
  buildDeliveryServerDefaultBody,
  deliveryServerDefaultHttpFailure,
  shouldSkipDefaultWrite,
  withUpdatedDeliveryServerDefault,
} from "@/publishing/deliveryServerDefault";

const stored: PublishServer = {
  serverId: "9",
  serverName: "ftp-prod",
  type: "File",
  isDefault: false,
  properties: [
    { key: "driver", value: "FTP" },
    { key: "serverip", value: "old.example" },
    { key: "port", value: "2121" },
    { key: "userid", value: "ftpuser" },
    { key: "folder", value: "/kept/folder" },
    { key: "password", value: "secret-pass" },
  ],
};

describe("buildDeliveryServerDefaultBody", () => {
  it("sends only isDefault true", () => {
    const body = buildDeliveryServerDefaultBody();
    expect(body).toEqual({ serverInfo: { isDefault: true } });
    expect(JSON.stringify(body)).not.toContain("secret-pass");
    expect(JSON.stringify(body)).not.toContain("password");
    expect(JSON.stringify(body)).not.toContain("driver");
    expect(JSON.stringify(body)).not.toContain("serverName");
    expect(JSON.stringify(body)).not.toContain("properties");
    expect(JSON.stringify(body)).not.toContain("/kept/folder");
  });
});

describe("withUpdatedDeliveryServerDefault", () => {
  it("sets the default flag and leaves name, driver, folder, and password", () => {
    const next = withUpdatedDeliveryServerDefault(stored, true);
    expect(next.isDefault).toBe(true);
    expect(next.defaultServer).toBe(true);
    expect(next.serverName).toBe("ftp-prod");
    expect(next.type).toBe("File");
    expect(serverProperty(next, "driver")).toBe("FTP");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(stored.isDefault).toBe(false);
  });
});

describe("shouldSkipDefaultWrite", () => {
  it("skips when the server is already default", () => {
    expect(shouldSkipDefaultWrite(stored)).toBe(false);
    expect(shouldSkipDefaultWrite({ ...stored, isDefault: true })).toBe(true);
    expect(shouldSkipDefaultWrite({ ...stored, defaultServer: true })).toBe(true);
  });
});

describe("delivery server default http guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(deliveryServerDefaultHttpFailure(400)).toBe("bad_request");
    expect(deliveryServerDefaultHttpFailure(403)).toBe("forbidden");
    expect(deliveryServerDefaultHttpFailure(409)).toBe("conflict");
    expect(deliveryServerDefaultHttpFailure(500)).toBeNull();
  });
});
