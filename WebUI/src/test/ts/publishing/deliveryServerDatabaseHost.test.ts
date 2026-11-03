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
  DATABASE_HOST_MAX_LENGTH,
  DATABASE_HOST_REQUIRED,
  DATABASE_HOST_TOO_LONG,
  buildDatabaseHostBody,
  databaseHostHttpFailure,
  databaseNameOrSid,
  isDatabaseDeliveryDriver,
  validateDatabaseHost,
  withUpdatedDatabaseHost,
} from "@/publishing/deliveryServerDatabaseHost";
import { serverProperty } from "@/publishing/deliveryServerFtpHost";

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
    { key: "password", value: "secret-pass" },
  ],
};

describe("validateDatabaseHost", () => {
  it("rejects a blank host", () => {
    expect(validateDatabaseHost("   ")).toEqual({
      ok: false,
      error: DATABASE_HOST_REQUIRED,
    });
  });

  it("rejects an overlong host", () => {
    expect(validateDatabaseHost("h".repeat(DATABASE_HOST_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: DATABASE_HOST_TOO_LONG,
    });
  });

  it("trims a host that fits", () => {
    expect(validateDatabaseHost("  db.new.example  ")).toEqual({
      ok: true,
      host: "db.new.example",
    });
  });
});

describe("buildDatabaseHostBody", () => {
  it("sends only server", () => {
    const body = buildDatabaseHostBody("db.new.example");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "server", value: "db.new.example" }] },
    });
    expect(JSON.stringify(body)).not.toContain("secret-pass");
    expect(JSON.stringify(body)).not.toContain("3306");
    expect(JSON.stringify(body)).not.toContain("cmsdb");
    expect(JSON.stringify(body)).not.toContain("rxdb");
    expect(JSON.stringify(body)).not.toContain("password");
    expect(JSON.stringify(body)).not.toContain("driver");
    expect(JSON.stringify(body)).not.toContain("serverip");
  });
});

describe("withUpdatedDatabaseHost", () => {
  it("replaces the host and leaves port, user, database, and password", () => {
    const next = withUpdatedDatabaseHost(stored, "db.new.example");
    expect(serverProperty(next, "server")).toBe("db.new.example");
    expect(serverProperty(next, "port")).toBe("3306");
    expect(serverProperty(next, "userid")).toBe("cmsdb");
    expect(serverProperty(next, "database")).toBe("rxdb");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "server")).toBe("old.example");
  });
});

describe("database host guards", () => {
  it("accepts only database drivers", () => {
    expect(isDatabaseDeliveryDriver("MYSQL")).toBe(true);
    expect(isDatabaseDeliveryDriver(" mysql8 ")).toBe(true);
    expect(isDatabaseDeliveryDriver("MSSQL")).toBe(true);
    expect(isDatabaseDeliveryDriver("Oracle")).toBe(true);
    expect(isDatabaseDeliveryDriver("FTP")).toBe(false);
    expect(isDatabaseDeliveryDriver("Local")).toBe(false);
  });

  it("maps 400, 403, and 409", () => {
    expect(databaseHostHttpFailure(400)).toBe("bad_request");
    expect(databaseHostHttpFailure(403)).toBe("forbidden");
    expect(databaseHostHttpFailure(409)).toBe("conflict");
    expect(databaseHostHttpFailure(500)).toBeNull();
  });

  it("reads database name or Oracle SID", () => {
    expect(databaseNameOrSid(stored, "MYSQL")).toBe("rxdb");
    const oracle: PublishServer = {
      properties: [
        { key: "driver", value: "Oracle" },
        { key: "sid", value: "ORCL" },
      ],
    };
    expect(databaseNameOrSid(oracle, "Oracle")).toBe("ORCL");
  });
});
