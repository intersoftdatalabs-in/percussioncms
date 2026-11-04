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
  DATABASE_USER_MAX_LENGTH,
  DATABASE_USER_REQUIRED,
  DATABASE_USER_TOO_LONG,
  buildDatabaseUserBody,
  databaseUserHttpFailure,
  validateDatabaseUser,
  withUpdatedDatabaseUser,
} from "@/publishing/deliveryServerDatabaseUser";

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

describe("validateDatabaseUser", () => {
  it("rejects a blank user id", () => {
    expect(validateDatabaseUser("   ")).toEqual({
      ok: false,
      error: DATABASE_USER_REQUIRED,
    });
  });

  it("rejects an overlong user id", () => {
    expect(validateDatabaseUser("u".repeat(DATABASE_USER_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: DATABASE_USER_TOO_LONG,
    });
  });

  it("trims a user id", () => {
    expect(validateDatabaseUser("  cmsnew  ")).toEqual({ ok: true, userId: "cmsnew" });
  });
});

describe("buildDatabaseUserBody", () => {
  it("sends only userid", () => {
    const body = buildDatabaseUserBody("cmsnew");
    expect(body).toEqual({
      serverInfo: { properties: [{ key: "userid", value: "cmsnew" }] },
    });
    const json = JSON.stringify(body);
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("old.example");
    expect(json).not.toContain("rxdb");
    expect(json).not.toContain("password");
    expect(json).not.toContain('"server"');
    expect(json).not.toContain("driver");
    expect(json).not.toContain("3306");
    expect(json).not.toContain("cmsdb");
  });
});

describe("withUpdatedDatabaseUser", () => {
  it("replaces the user id and leaves host, port, database, and password", () => {
    const next = withUpdatedDatabaseUser(stored, "cmsnew");
    expect(serverProperty(next, "userid")).toBe("cmsnew");
    expect(serverProperty(next, "server")).toBe("old.example");
    expect(serverProperty(next, "port")).toBe("3306");
    expect(serverProperty(next, "database")).toBe("rxdb");
    expect(serverProperty(next, "password")).toBe("secret-pass");
    expect(serverProperty(stored, "userid")).toBe("cmsdb");
  });
});

describe("database user guards", () => {
  it("maps 400, 403, and 409", () => {
    expect(databaseUserHttpFailure(400)).toBe("bad_request");
    expect(databaseUserHttpFailure(403)).toBe("forbidden");
    expect(databaseUserHttpFailure(409)).toBe("conflict");
    expect(databaseUserHttpFailure(500)).toBeNull();
  });
});
