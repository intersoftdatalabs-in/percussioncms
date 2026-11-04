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
  MSSQL_OWNER_MAX_LENGTH,
  MSSQL_OWNER_REQUIRED,
  MSSQL_OWNER_TOO_LONG,
  buildMssqlOwnerBody,
  isMssqlDeliveryDriver,
  mssqlOwnerHttpFailure,
  validateMssqlOwner,
  withUpdatedMssqlOwner,
} from "@/publishing/deliveryServerDatabaseOwner";

const stored: PublishServer = {
  serverId: "9",
  serverName: "sql-prod",
  isDefault: false,
  properties: [
    { key: "driver", value: "MSSQL" },
    { key: "server", value: "old-sql.example" },
    { key: "port", value: "1433" },
    { key: "userid", value: "cmsdb" },
    { key: "database", value: "rxdb" },
    { key: "owner", value: "dbo" },
    { key: "password", value: "secret-pass" },
  ],
};

describe("validateMssqlOwner", () => {
  it("rejects a blank owner", () => {
    expect(validateMssqlOwner("   ")).toEqual({
      ok: false,
      error: MSSQL_OWNER_REQUIRED,
    });
  });

  it("rejects an overlong owner", () => {
    expect(validateMssqlOwner("o".repeat(MSSQL_OWNER_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: MSSQL_OWNER_TOO_LONG,
    });
  });

  it("trims an owner", () => {
    expect(validateMssqlOwner("  sa  ")).toEqual({
      ok: true,
      owner: "sa",
    });
  });
});

describe("isMssqlDeliveryDriver", () => {
  it("allows MSSQL and rejects MYSQL, Oracle, FTP, and Local", () => {
    expect(isMssqlDeliveryDriver("MSSQL")).toBe(true);
    expect(isMssqlDeliveryDriver(" mssql ")).toBe(true);
    expect(isMssqlDeliveryDriver("MYSQL")).toBe(false);
    expect(isMssqlDeliveryDriver("Oracle")).toBe(false);
    expect(isMssqlDeliveryDriver("FTP")).toBe(false);
    expect(isMssqlDeliveryDriver("Local")).toBe(false);
  });
});

describe("buildMssqlOwnerBody", () => {
  it("sends only owner", () => {
    const body = buildMssqlOwnerBody("sa");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "owner", value: "sa" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("old-sql.example");
    expect(json).not.toContain("password");
    expect(json).not.toContain("userid");
    expect(json).not.toContain("cmsdb");
    expect(json).not.toContain("database");
    expect(json).not.toContain("sid");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("1433");
  });
});

describe("withUpdatedMssqlOwner", () => {
  it("replaces the owner and leaves host, port, user, database, and password", () => {
    const next = withUpdatedMssqlOwner(stored, "sa");
    expect(serverProperty(next, "owner")).toBe("sa");
    expect(serverProperty(next, "server")).toBe("old-sql.example");
    expect(serverProperty(next, "port")).toBe("1433");
    expect(serverProperty(next, "userid")).toBe("cmsdb");
    expect(serverProperty(next, "database")).toBe("rxdb");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "owner")).toBe("dbo");
  });
});

describe("mssql owner guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(mssqlOwnerHttpFailure(400)).toBe("bad_request");
    expect(mssqlOwnerHttpFailure(403)).toBe("forbidden");
    expect(mssqlOwnerHttpFailure(409)).toBe("conflict");
    expect(mssqlOwnerHttpFailure(500)).toBeNull();
  });
});
