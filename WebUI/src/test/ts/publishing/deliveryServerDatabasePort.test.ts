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
  DATABASE_PORT_MAX_LENGTH,
  DATABASE_PORT_NOT_NUMERIC,
  DATABASE_PORT_REQUIRED,
  DATABASE_PORT_TOO_LONG,
  buildDatabasePortBody,
  databasePortHttpFailure,
  validateDatabasePort,
  withUpdatedDatabasePort,
} from "@/publishing/deliveryServerDatabasePort";

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

describe("validateDatabasePort", () => {
  it("rejects a blank port", () => {
    expect(validateDatabasePort("   ")).toEqual({
      ok: false,
      error: DATABASE_PORT_REQUIRED,
    });
  });

  it("rejects a non-numeric port", () => {
    expect(validateDatabasePort("33a")).toEqual({
      ok: false,
      error: DATABASE_PORT_NOT_NUMERIC,
    });
    expect(validateDatabasePort("33.6")).toEqual({
      ok: false,
      error: DATABASE_PORT_NOT_NUMERIC,
    });
    expect(validateDatabasePort("-1")).toEqual({
      ok: false,
      error: DATABASE_PORT_NOT_NUMERIC,
    });
  });

  it("rejects an overlong port", () => {
    expect(validateDatabasePort("9".repeat(DATABASE_PORT_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: DATABASE_PORT_TOO_LONG,
    });
  });

  it("trims a numeric port", () => {
    expect(validateDatabasePort("  3307  ")).toEqual({ ok: true, port: "3307" });
  });
});

describe("buildDatabasePortBody", () => {
  it("sends only port", () => {
    const body = buildDatabasePortBody("3307");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "port", value: "3307" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("old.example");
    expect(json).not.toContain("cmsdb");
    expect(json).not.toContain("rxdb");
    expect(json).not.toContain("password");
    expect(json).not.toContain('"server"');
    expect(json).not.toContain("driver");
    expect(json).not.toContain("3306");
  });
});

describe("withUpdatedDatabasePort", () => {
  it("replaces the port and leaves host, user, database, and password", () => {
    const next = withUpdatedDatabasePort(stored, "3307");
    expect(serverProperty(next, "port")).toBe("3307");
    expect(serverProperty(next, "server")).toBe("old.example");
    expect(serverProperty(next, "userid")).toBe("cmsdb");
    expect(serverProperty(next, "database")).toBe("rxdb");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "port")).toBe("3306");
  });
});

describe("database port guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(databasePortHttpFailure(400)).toBe("bad_request");
    expect(databasePortHttpFailure(403)).toBe("forbidden");
    expect(databasePortHttpFailure(409)).toBe("conflict");
    expect(databasePortHttpFailure(500)).toBeNull();
  });
});
