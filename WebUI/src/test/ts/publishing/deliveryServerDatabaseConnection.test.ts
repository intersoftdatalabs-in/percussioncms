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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it } from "vitest";
import type { PublishServer } from "@/publishing/types";
import {
  DATABASE_HOST_MAX_LENGTH,
  DATABASE_HOST_REQUIRED,
  DATABASE_HOST_TOO_LONG,
} from "@/publishing/deliveryServerDatabaseHost";
import {
  DATABASE_PASSWORD_MAX_LENGTH,
  DATABASE_PASSWORD_REQUIRED,
  DATABASE_PASSWORD_TOO_LONG,
} from "@/publishing/deliveryServerDatabasePassword";
import {
  DATABASE_USER_MAX_LENGTH,
  DATABASE_USER_REQUIRED,
  DATABASE_USER_TOO_LONG,
} from "@/publishing/deliveryServerDatabaseUser";
import {
  buildDatabaseConnectionBody,
  databaseConnectionHttpFailure,
  validateDatabaseConnection,
  withUpdatedDatabaseConnection,
} from "@/publishing/deliveryServerDatabaseConnection";

const NEW_PASSWORD = "NightDbConn1!";

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

function valid() {
  return { host: "db.new.example", userId: "cmsnew", password: NEW_PASSWORD };
}

describe("validateDatabaseConnection", () => {
  it("rejects a blank host", () => {
    expect(
      validateDatabaseConnection({ ...valid(), host: "   " }),
    ).toEqual({ ok: false, error: DATABASE_HOST_REQUIRED });
  });

  it("rejects an overlong host", () => {
    expect(
      validateDatabaseConnection({
        ...valid(),
        host: "h".repeat(DATABASE_HOST_MAX_LENGTH + 1),
      }),
    ).toEqual({ ok: false, error: DATABASE_HOST_TOO_LONG });
  });

  it("rejects a blank user id", () => {
    expect(
      validateDatabaseConnection({ ...valid(), userId: "  " }),
    ).toEqual({ ok: false, error: DATABASE_USER_REQUIRED });
  });

  it("rejects an overlong user id", () => {
    expect(
      validateDatabaseConnection({
        ...valid(),
        userId: "u".repeat(DATABASE_USER_MAX_LENGTH + 1),
      }),
    ).toEqual({ ok: false, error: DATABASE_USER_TOO_LONG });
  });

  it("rejects a blank password without echoing it", () => {
    expect(
      validateDatabaseConnection({ ...valid(), password: "   " }),
    ).toEqual({ ok: false, error: DATABASE_PASSWORD_REQUIRED });
  });

  it("rejects an overlong password", () => {
    expect(
      validateDatabaseConnection({
        ...valid(),
        password: "p".repeat(DATABASE_PASSWORD_MAX_LENGTH + 1),
      }),
    ).toEqual({ ok: false, error: DATABASE_PASSWORD_TOO_LONG });
  });

  it("trims host and user id and keeps the typed password", () => {
    expect(
      validateDatabaseConnection({
        host: "  db.new.example  ",
        userId: "  cmsnew  ",
        password: NEW_PASSWORD,
      }),
    ).toEqual({
      ok: true,
      host: "db.new.example",
      userId: "cmsnew",
      password: NEW_PASSWORD,
    });
  });
});

describe("buildDatabaseConnectionBody", () => {
  it("sends only server, userid, and base64 password", () => {
    const body = buildDatabaseConnectionBody(
      "db.new.example",
      "cmsnew",
      NEW_PASSWORD,
    );
    expect(body.serverInfo.properties).toHaveLength(3);
    expect(body.serverInfo.properties[0]).toEqual({
      key: "server",
      value: "db.new.example",
    });
    expect(body.serverInfo.properties[1]).toEqual({
      key: "userid",
      value: "cmsnew",
    });
    expect(body.serverInfo.properties[2].key).toBe("password");
    const sent = String(body.serverInfo.properties[2].value ?? "");
    expect(sent).not.toContain(NEW_PASSWORD);
    expect(Buffer.from(sent, "base64").toString("utf8")).toBe(NEW_PASSWORD);
    const json = JSON.stringify(body);
    expect(json).not.toContain(NEW_PASSWORD);
    expect(json).not.toContain("secret-pass");
    expect(json).not.toContain("3306");
    expect(json).not.toContain("rxdb");
    expect(json).not.toContain("driver");
    expect(json).not.toContain("serverip");
  });
});

describe("withUpdatedDatabaseConnection", () => {
  it("replaces host and user id and never writes the secret", () => {
    const next = withUpdatedDatabaseConnection(
      stored,
      "db.new.example",
      "cmsnew",
    );
    const map = new Map(
      (next.properties as { key: string; value: string }[]).map((p) => [
        p.key,
        p.value,
      ]),
    );
    expect(map.get("server")).toBe("db.new.example");
    expect(map.get("userid")).toBe("cmsnew");
    expect(map.get("password")).toBe("secret-pass");
    expect(map.get("port")).toBe("3306");
    expect(map.get("database")).toBe("rxdb");
    expect(map.get("driver")).toBe("MYSQL");
  });
});

describe("databaseConnectionHttpFailure", () => {
  it("maps 400, 403, and 409", () => {
    expect(databaseConnectionHttpFailure(400)).toBe("bad_request");
    expect(databaseConnectionHttpFailure(403)).toBe("forbidden");
    expect(databaseConnectionHttpFailure(409)).toBe("conflict");
    expect(databaseConnectionHttpFailure(500)).toBeNull();
  });
});
