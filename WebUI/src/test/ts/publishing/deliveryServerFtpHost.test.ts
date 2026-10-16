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
import {
  FTP_HOST_MAX_LENGTH,
  FTP_HOST_REQUIRED,
  FTP_HOST_TOO_LONG,
  buildFtpHostBody,
  ftpHostHttpFailure,
  isFtpDeliveryDriver,
  serverProperty,
  validateFtpHost,
  withUpdatedHost,
} from "@/publishing/deliveryServerFtpHost";

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

describe("validateFtpHost", () => {
  it("rejects a blank host", () => {
    expect(validateFtpHost("   ")).toEqual({ ok: false, error: FTP_HOST_REQUIRED });
  });

  it("rejects an overlong host", () => {
    expect(validateFtpHost("h".repeat(FTP_HOST_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: FTP_HOST_TOO_LONG,
    });
  });

  it("trims a host that fits", () => {
    expect(validateFtpHost("  ftp.new.example  ")).toEqual({
      ok: true,
      host: "ftp.new.example",
    });
  });
});

describe("buildFtpHostBody", () => {
  it("sends only serverip", () => {
    const body = buildFtpHostBody("ftp.new.example");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "serverip", value: "ftp.new.example" }] },
    });
    expect(JSON.stringify(body)).not.toContain("secret-pass");
    expect(JSON.stringify(body)).not.toContain("2121");
    expect(JSON.stringify(body)).not.toContain("ftpuser");
    expect(JSON.stringify(body)).not.toContain("/kept/folder");
    expect(JSON.stringify(body)).not.toContain("password");
    expect(JSON.stringify(body)).not.toContain("driver");
  });
});

describe("withUpdatedHost", () => {
  it("replaces the host and leaves port, user, folder, and password", () => {
    const next = withUpdatedHost(stored, "ftp.new.example");
    expect(serverProperty(next, "serverip")).toBe("ftp.new.example");
    expect(serverProperty(next, "port")).toBe("2121");
    expect(serverProperty(next, "userid")).toBe("ftpuser");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "serverip")).toBe("old.example");
  });
});

describe("ftp host guards", () => {
  it("accepts only the FTP driver", () => {
    expect(isFtpDeliveryDriver("FTP")).toBe(true);
    expect(isFtpDeliveryDriver(" ftp ")).toBe(true);
    expect(isFtpDeliveryDriver("FTPS")).toBe(false);
    expect(isFtpDeliveryDriver("SFTP")).toBe(false);
    expect(isFtpDeliveryDriver("Local")).toBe(false);
  });

  it("maps 400, 403, and 409", () => {
    expect(ftpHostHttpFailure(400)).toBe("bad_request");
    expect(ftpHostHttpFailure(403)).toBe("forbidden");
    expect(ftpHostHttpFailure(409)).toBe("conflict");
    expect(ftpHostHttpFailure(500)).toBeNull();
  });
});
