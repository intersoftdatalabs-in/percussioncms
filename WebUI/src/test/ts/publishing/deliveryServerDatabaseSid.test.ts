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
  ORACLE_SID_MAX_LENGTH,
  ORACLE_SID_REQUIRED,
  ORACLE_SID_TOO_LONG,
  buildOracleSidBody,
  isOracleDeliveryDriver,
  oracleSidHttpFailure,
  validateOracleSid,
  withUpdatedOracleSid,
} from "@/publishing/deliveryServerDatabaseSid";

const stored: PublishServer = {
  serverId: "9",
  serverName: "ora-prod",
  isDefault: false,
  properties: [
    { key: "driver", value: "Oracle" },
    { key: "server", value: "old-ora.example" },
    { key: "port", value: "1521" },
    { key: "userid", value: "cmsora" },
    { key: "sid", value: "ORCL" },
    { key: "schema", value: "SYSTEM" },
    { key: "password", value: "secret-pass" },
  ],
};

describe("validateOracleSid", () => {
  it("rejects a blank SID", () => {
    expect(validateOracleSid("   ")).toEqual({
      ok: false,
      error: ORACLE_SID_REQUIRED,
    });
  });

  it("rejects an overlong SID", () => {
    expect(validateOracleSid("s".repeat(ORACLE_SID_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: ORACLE_SID_TOO_LONG,
    });
  });

  it("trims a SID", () => {
    expect(validateOracleSid("  ORCL2  ")).toEqual({
      ok: true,
      sid: "ORCL2",
    });
  });
});

describe("isOracleDeliveryDriver", () => {
  it("allows Oracle and rejects MYSQL, MSSQL, FTP, and Local", () => {
    expect(isOracleDeliveryDriver("Oracle")).toBe(true);
    expect(isOracleDeliveryDriver(" oracle ")).toBe(true);
    expect(isOracleDeliveryDriver("MYSQL")).toBe(false);
    expect(isOracleDeliveryDriver("MSSQL")).toBe(false);
    expect(isOracleDeliveryDriver("FTP")).toBe(false);
    expect(isOracleDeliveryDriver("Local")).toBe(false);
  });
});

describe("buildOracleSidBody", () => {
  it("sends only sid", () => {
    const body = buildOracleSidBody("ORCL2");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "sid", value: "ORCL2" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("old-ora.example");
    expect(json).not.toContain("password");
    expect(json).not.toContain("userid");
    expect(json).not.toContain("cmsora");
    expect(json).not.toContain("database");
    expect(json).not.toContain("schema");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("1521");
  });
});

describe("withUpdatedOracleSid", () => {
  it("replaces the SID and leaves host, port, user, schema, and password", () => {
    const next = withUpdatedOracleSid(stored, "ORCL2");
    expect(serverProperty(next, "sid")).toBe("ORCL2");
    expect(serverProperty(next, "server")).toBe("old-ora.example");
    expect(serverProperty(next, "port")).toBe("1521");
    expect(serverProperty(next, "userid")).toBe("cmsora");
    expect(serverProperty(next, "schema")).toBe("SYSTEM");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "sid")).toBe("ORCL");
  });
});

describe("oracle SID guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(oracleSidHttpFailure(400)).toBe("bad_request");
    expect(oracleSidHttpFailure(403)).toBe("forbidden");
    expect(oracleSidHttpFailure(409)).toBe("conflict");
    expect(oracleSidHttpFailure(500)).toBeNull();
  });
});
