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
  ORACLE_SCHEMA_MAX_LENGTH,
  ORACLE_SCHEMA_REQUIRED,
  ORACLE_SCHEMA_TOO_LONG,
  buildOracleSchemaBody,
  isOracleDeliveryDriver,
  oracleSchemaHttpFailure,
  validateOracleSchema,
  withUpdatedOracleSchema,
} from "@/publishing/deliveryServerDatabaseSchema";

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

describe("validateOracleSchema", () => {
  it("rejects a blank schema", () => {
    expect(validateOracleSchema("   ")).toEqual({
      ok: false,
      error: ORACLE_SCHEMA_REQUIRED,
    });
  });

  it("rejects an overlong schema", () => {
    expect(
      validateOracleSchema("s".repeat(ORACLE_SCHEMA_MAX_LENGTH + 1)),
    ).toEqual({
      ok: false,
      error: ORACLE_SCHEMA_TOO_LONG,
    });
  });

  it("trims a schema", () => {
    expect(validateOracleSchema("  APP  ")).toEqual({
      ok: true,
      schema: "APP",
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

describe("buildOracleSchemaBody", () => {
  it("sends only schema", () => {
    const body = buildOracleSchemaBody("APP");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "schema", value: "APP" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("old-ora.example");
    expect(json).not.toContain("password");
    expect(json).not.toContain("userid");
    expect(json).not.toContain("cmsora");
    expect(json).not.toContain("database");
    expect(json).not.toContain("sid");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("1521");
  });
});

describe("withUpdatedOracleSchema", () => {
  it("replaces the schema and leaves host, port, user, SID, and password", () => {
    const next = withUpdatedOracleSchema(stored, "APP");
    expect(serverProperty(next, "schema")).toBe("APP");
    expect(serverProperty(next, "server")).toBe("old-ora.example");
    expect(serverProperty(next, "port")).toBe("1521");
    expect(serverProperty(next, "userid")).toBe("cmsora");
    expect(serverProperty(next, "sid")).toBe("ORCL");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "schema")).toBe("SYSTEM");
  });
});

describe("oracle schema guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(oracleSchemaHttpFailure(400)).toBe("bad_request");
    expect(oracleSchemaHttpFailure(403)).toBe("forbidden");
    expect(oracleSchemaHttpFailure(409)).toBe("conflict");
    expect(oracleSchemaHttpFailure(500)).toBeNull();
  });
});
