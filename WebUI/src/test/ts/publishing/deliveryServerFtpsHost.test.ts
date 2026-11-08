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

import type { PublishServer } from "@/publishing/types";
import {
  FTPS_HOST_MAX_LENGTH,
  FTPS_HOST_REQUIRED,
  FTPS_HOST_TOO_LONG,
  buildFtpsHostBody,
  ftpsHostHttpFailure,
  isFtpsDeliveryDriver,
  validateFtpsHost,
  withUpdatedFtpsHost,
} from "@/publishing/deliveryServerFtpsHost";
import { serverProperty } from "@/publishing/deliveryServerFtpHost";

const stored: PublishServer = {
  serverId: "9",
  serverName: "ftps-prod",
  properties: [
    { key: "driver", value: "FTPS" },
    { key: "serverip", value: "old.example" },
    { key: "port", value: "990" },
    { key: "userid", value: "ftpsuser" },
    { key: "folder", value: "/kept/folder" },
    { key: "password", value: "secret-passphrase" },
    { key: "privateKey", value: "stored-private-key" },
  ],
};

describe("validateFtpsHost", () => {
  it("rejects a blank host", () => {
    expect(validateFtpsHost("   ")).toEqual({ ok: false, error: FTPS_HOST_REQUIRED });
  });

  it("rejects an overlong host", () => {
    expect(validateFtpsHost("h".repeat(FTPS_HOST_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: FTPS_HOST_TOO_LONG,
    });
  });

  it("trims a host that fits", () => {
    expect(validateFtpsHost("  ftps.new.example  ")).toEqual({
      ok: true,
      host: "ftps.new.example",
    });
  });
});

describe("buildFtpsHostBody", () => {
  it("sends only serverip", () => {
    const body = buildFtpsHostBody("ftps.new.example");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "serverip", value: "ftps.new.example" }] },
    });
    expect(JSON.stringify(body)).not.toContain("secret-passphrase");
    expect(JSON.stringify(body)).not.toContain("stored-private-key");
    expect(JSON.stringify(body)).not.toContain("990");
    expect(JSON.stringify(body)).not.toContain("ftpsuser");
    expect(JSON.stringify(body)).not.toContain("/kept/folder");
    expect(JSON.stringify(body)).not.toContain("password");
    expect(JSON.stringify(body)).not.toContain("privateKey");
    expect(JSON.stringify(body)).not.toContain("driver");
  });
});

describe("withUpdatedFtpsHost", () => {
  it("replaces the host and leaves port, user, folder, and secrets", () => {
    const next = withUpdatedFtpsHost(stored, "ftps.new.example");
    expect(serverProperty(next, "serverip")).toBe("ftps.new.example");
    expect(serverProperty(next, "port")).toBe("990");
    expect(serverProperty(next, "userid")).toBe("ftpsuser");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-passphrase");
    expect(serverProperty(next, "privateKey")).toBe("stored-private-key");
    expect(serverProperty(stored, "serverip")).toBe("old.example");
  });
});

describe("ftps host guards", () => {
  it("accepts only the FTPS driver", () => {
    expect(isFtpsDeliveryDriver("FTPS")).toBe(true);
    expect(isFtpsDeliveryDriver(" ftps ")).toBe(true);
    expect(isFtpsDeliveryDriver("FTP")).toBe(false);
    expect(isFtpsDeliveryDriver("SFTP")).toBe(false);
    expect(isFtpsDeliveryDriver("Local")).toBe(false);
  });

  it("maps 400, 403, and 409", () => {
    expect(ftpsHostHttpFailure(400)).toBe("bad_request");
    expect(ftpsHostHttpFailure(403)).toBe("forbidden");
    expect(ftpsHostHttpFailure(409)).toBe("conflict");
    expect(ftpsHostHttpFailure(500)).toBeNull();
  });
});
