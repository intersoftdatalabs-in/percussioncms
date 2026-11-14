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
  FTPS_USER_MAX_LENGTH,
  FTPS_USER_REQUIRED,
  FTPS_USER_TOO_LONG,
  buildFtpsUserBody,
  ftpsUserHttpFailure,
  validateFtpsUser,
  withUpdatedFtpsUser,
} from "@/publishing/deliveryServerFtpsUser";

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

describe("validateFtpsUser", () => {
  it("rejects a blank user id", () => {
    expect(validateFtpsUser("   ")).toEqual({ ok: false, error: FTPS_USER_REQUIRED });
  });

  it("rejects an overlong user id", () => {
    expect(validateFtpsUser("u".repeat(FTPS_USER_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: FTPS_USER_TOO_LONG,
    });
  });

  it("trims a user id", () => {
    expect(validateFtpsUser("  cmsftps  ")).toEqual({ ok: true, userId: "cmsftps" });
  });
});

describe("buildFtpsUserBody", () => {
  it("sends only userid", () => {
    const body = buildFtpsUserBody("cmsftps");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "userid", value: "cmsftps" }] },
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
  });
});

describe("withUpdatedFtpsUser", () => {
  it("replaces the user id and leaves host, port, folder, and password", () => {
    const next = withUpdatedFtpsUser(stored, "cmsftps");
    expect(serverProperty(next, "userid")).toBe("cmsftps");
    expect(serverProperty(next, "serverip")).toBe("old.example");
    expect(serverProperty(next, "port")).toBe("990");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "userid")).toBe("ftpsuser");
  });
});

describe("ftps user guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(ftpsUserHttpFailure(400)).toBe("bad_request");
    expect(ftpsUserHttpFailure(403)).toBe("forbidden");
    expect(ftpsUserHttpFailure(409)).toBe("conflict");
    expect(ftpsUserHttpFailure(500)).toBeNull();
  });
});
