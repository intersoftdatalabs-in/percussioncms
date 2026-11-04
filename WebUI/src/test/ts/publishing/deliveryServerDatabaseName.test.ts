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
  DATABASE_NAME_MAX_LENGTH,
  DATABASE_NAME_REQUIRED,
  DATABASE_NAME_TOO_LONG,
  buildDatabaseNameBody,
  databaseNameHttpFailure,
  isMysqlOrMssqlDeliveryDriver,
  validateDatabaseName,
  withUpdatedDatabaseName,
} from "@/publishing/deliveryServerDatabaseName";

const stored: PublishServer = {
  serverId: "9",
  serverName: "mysql-prod",
  isDefault: false,
  properties: [
    { key: "driver", value: "MYSQL" },
    { key: "server", value: "old.example" },
    { key: "port", value: "3306" },
    { key: "userid", value: "cmsdb" },
    { key: "database", value: "rxdb" },
    { key: "sid", value: "ORCL" },
    { key: "password", value: "secret-pass" },
  ],
};

describe("validateDatabaseName", () => {
  it("rejects a blank database name", () => {
    expect(validateDatabaseName("   ")).toEqual({
      ok: false,
      error: DATABASE_NAME_REQUIRED,
    });
  });

  it("rejects an overlong database name", () => {
    expect(validateDatabaseName("d".repeat(DATABASE_NAME_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: DATABASE_NAME_TOO_LONG,
    });
  });

  it("trims a database name", () => {
    expect(validateDatabaseName("  rxnew  ")).toEqual({
      ok: true,
      databaseName: "rxnew",
    });
  });
});

describe("isMysqlOrMssqlDeliveryDriver", () => {
  it("allows MYSQL and MSSQL and rejects Oracle, FTP, and Local", () => {
    expect(isMysqlOrMssqlDeliveryDriver("MYSQL")).toBe(true);
    expect(isMysqlOrMssqlDeliveryDriver(" mysql8 ")).toBe(true);
    expect(isMysqlOrMssqlDeliveryDriver("MSSQL")).toBe(true);
    expect(isMysqlOrMssqlDeliveryDriver("Oracle")).toBe(false);
    expect(isMysqlOrMssqlDeliveryDriver("FTP")).toBe(false);
    expect(isMysqlOrMssqlDeliveryDriver("Local")).toBe(false);
  });
});

describe("buildDatabaseNameBody", () => {
  it("sends only database", () => {
    const body = buildDatabaseNameBody("rxnew");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "database", value: "rxnew" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("old.example");
    expect(json).not.toContain("password");
    expect(json).not.toContain("userid");
    expect(json).not.toContain("cmsdb");
    expect(json).not.toContain("ORCL");
    expect(json).not.toContain("sid");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("3306");
  });
});

describe("withUpdatedDatabaseName", () => {
  it("replaces the database name and leaves host, port, user, sid, and password", () => {
    const next = withUpdatedDatabaseName(stored, "rxnew");
    expect(serverProperty(next, "database")).toBe("rxnew");
    expect(serverProperty(next, "server")).toBe("old.example");
    expect(serverProperty(next, "port")).toBe("3306");
    expect(serverProperty(next, "userid")).toBe("cmsdb");
    expect(serverProperty(next, "sid")).toBe("ORCL");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "database")).toBe("rxdb");
  });
});

describe("database name guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(databaseNameHttpFailure(400)).toBe("bad_request");
    expect(databaseNameHttpFailure(403)).toBe("forbidden");
    expect(databaseNameHttpFailure(409)).toBe("conflict");
    expect(databaseNameHttpFailure(500)).toBeNull();
  });
});
