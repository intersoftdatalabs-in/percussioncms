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
  FTPS_PORT_MAX_LENGTH,
  FTPS_PORT_NOT_NUMERIC,
  FTPS_PORT_REQUIRED,
  FTPS_PORT_TOO_LONG,
  buildFtpsPortBody,
  ftpsPortHttpFailure,
  validateFtpsPort,
  withUpdatedFtpsPort,
} from "@/publishing/deliveryServerFtpsPort";
import { isFtpsDeliveryDriver } from "@/publishing/deliveryServerFtpsHost";
import { serverProperty } from "@/publishing/deliveryServerFtpHost";

const stored: PublishServer = {
  serverId: "9",
  serverName: "ftps-prod",
  properties: [
    { key: "driver", value: "FTPS" },
    { key: "serverip", value: "ftps.example" },
    { key: "port", value: "990" },
    { key: "userid", value: "ftpsuser" },
    { key: "folder", value: "/kept/folder" },
    { key: "password", value: "secret-passphrase" },
    { key: "privateKey", value: "stored-private-key" },
  ],
};

describe("validateFtpsPort", () => {
  it("rejects a blank port", () => {
    expect(validateFtpsPort("   ")).toEqual({ ok: false, error: FTPS_PORT_REQUIRED });
  });

  it("rejects a non-numeric port", () => {
    for (const bad of ["21a", "21.5", "-1", "22 22"]) {
      expect(validateFtpsPort(bad)).toEqual({ ok: false, error: FTPS_PORT_NOT_NUMERIC });
    }
  });

  it("rejects an overlong port", () => {
    expect(validateFtpsPort("9".repeat(FTPS_PORT_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: FTPS_PORT_TOO_LONG,
    });
  });

  it("trims a port that fits", () => {
    expect(validateFtpsPort("  21  ")).toEqual({ ok: true, port: "21" });
  });
});

describe("buildFtpsPortBody", () => {
  it("sends only port", () => {
    const body = buildFtpsPortBody("21");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "port", value: "21" }] },
    });
    expect(JSON.stringify(body)).not.toContain("secret-passphrase");
    expect(JSON.stringify(body)).not.toContain("stored-private-key");
    expect(JSON.stringify(body)).not.toContain("ftps.example");
    expect(JSON.stringify(body)).not.toContain("ftpsuser");
    expect(JSON.stringify(body)).not.toContain("/kept/folder");
    expect(JSON.stringify(body)).not.toContain("password");
    expect(JSON.stringify(body)).not.toContain("privateKey");
    expect(JSON.stringify(body)).not.toContain("driver");
  });
});

describe("withUpdatedFtpsPort", () => {
  it("replaces the port and leaves host, user, folder, and secrets", () => {
    const next = withUpdatedFtpsPort(stored, "21");
    expect(serverProperty(next, "port")).toBe("21");
    expect(serverProperty(next, "serverip")).toBe("ftps.example");
    expect(serverProperty(next, "userid")).toBe("ftpsuser");
    expect(serverProperty(next, "folder")).toBe("/kept/folder");
    expect(serverProperty(next, "password")).toBe("secret-passphrase");
    expect(serverProperty(next, "privateKey")).toBe("stored-private-key");
    expect(serverProperty(stored, "port")).toBe("990");
  });
});

describe("ftps port guards", () => {
  it("accepts only the FTPS driver", () => {
    expect(isFtpsDeliveryDriver("FTPS")).toBe(true);
    expect(isFtpsDeliveryDriver(" ftps ")).toBe(true);
    expect(isFtpsDeliveryDriver("FTP")).toBe(false);
    expect(isFtpsDeliveryDriver("SFTP")).toBe(false);
    expect(isFtpsDeliveryDriver("Local")).toBe(false);
  });

  it("maps 400, 403, and 409", () => {
    expect(ftpsPortHttpFailure(400)).toBe("bad_request");
    expect(ftpsPortHttpFailure(403)).toBe("forbidden");
    expect(ftpsPortHttpFailure(409)).toBe("conflict");
    expect(ftpsPortHttpFailure(500)).toBeNull();
  });
});
