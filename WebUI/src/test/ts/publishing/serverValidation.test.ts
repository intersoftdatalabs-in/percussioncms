/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import {
  requiredFieldsForDriver,
  validateServerForm,
} from "@/publishing/serverValidation";

describe("requiredFieldsForDriver", () => {
  it("requires name for LOCAL", () => {
    expect(requiredFieldsForDriver("LOCAL")).toContain("serverName");
  });

  it("requires FTP remote fields", () => {
    const f = requiredFieldsForDriver("FTP");
    expect(f).toEqual(
      expect.arrayContaining(["serverip", "userid", "password", "port"]),
    );
  });

  it("requires S3 fields using product lowercase keys", () => {
    expect(requiredFieldsForDriver("AMAZONS3")).toEqual(
      expect.arrayContaining([
        "serverName",
        "bucketlocation",
        "region",
        "accesskey",
        "securitykey",
      ]),
    );
  });
});

describe("validateServerForm", () => {
  it("fails incomplete Local", () => {
    expect(validateServerForm({ serverName: "", driver: "LOCAL" }).valid).toBe(
      false,
    );
  });

  it("passes Local with name", () => {
    expect(
      validateServerForm({ serverName: "Local1", driver: "LOCAL" }).valid,
    ).toBe(true);
  });

  it("fails FTP missing host", () => {
    const r = validateServerForm({
      serverName: "ftp1",
      driver: "FTP",
      properties: { userid: "u", password: "p", port: "21" },
    });
    expect(r.valid).toBe(false);
    expect(r.missing).toContain("serverip");
  });

  it("passes S3 when form uses canonical bucketlocation key", () => {
    const r = validateServerForm({
      serverName: "s3prod",
      driver: "AMAZONS3",
      properties: {
        accesskey: "AKIA",
        securitykey: "secret",
        bucketlocation: "my-bucket",
        region: "us-east-1",
      },
    });
    expect(r.valid).toBe(true);
    expect(r.missing).toEqual([]);
  });

  it("fails S3 when bucket is stored under the legacy bucketName key", () => {
    const r = validateServerForm({
      serverName: "s3prod",
      driver: "AMAZONS3",
      properties: {
        accesskey: "AKIA",
        securitykey: "secret",
        bucketName: "my-bucket",
        region: "us-east-1",
      },
    });
    expect(r.valid).toBe(false);
    expect(r.missing).toContain("bucketlocation");
  });

  it("requires SFTP userid (canonical) and rejects the legacy 'user' key", () => {
    const required = requiredFieldsForDriver("SFTP");
    expect(required).toEqual(
      expect.arrayContaining(["serverName", "serverip", "userid", "port"]),
    );
    expect(required).not.toContain("user");
    const legacy = validateServerForm({
      serverName: "sftp1",
      driver: "SFTP",
      properties: { serverip: "h", port: "22", user: "alice", password: "p" },
    });
    expect(legacy.valid).toBe(false);
    expect(legacy.missing).toContain("userid");
    const canonical = validateServerForm({
      serverName: "sftp1",
      driver: "SFTP",
      properties: { serverip: "h", port: "22", userid: "alice", password: "p" },
    });
    expect(canonical.valid).toBe(true);
    expect(canonical.missing).toEqual([]);
  });

  it("requires MySQL canonical fields and rejects 'schema' / 'sid'", () => {
    const ok = validateServerForm({
      serverName: "mysql1",
      driver: "MYSQL",
      properties: {
        driver: "MYSQL",
        server: "db.local",
        port: "3306",
        database: "appdb",
        userid: "alice",
        password: "p",
      },
    });
    expect(ok.valid).toBe(true);

    const wrong = validateServerForm({
      serverName: "mysql1",
      driver: "MYSQL",
      properties: {
        driver: "MYSQL",
        server: "db.local",
        port: "3306",
        database: "appdb",
        userid: "alice",
        password: "p",
        schema: "dbo",
        sid: "orcl",
      },
    });
    expect(wrong.missing).not.toContain("schema");
    expect(wrong.missing).not.toContain("sid");
  });

  it("requires MSSQL canonical fields including owner", () => {
    const ok = validateServerForm({
      serverName: "mssql1",
      driver: "MSSQL",
      properties: {
        driver: "MSSQL",
        server: "db.local",
        port: "1433",
        database: "appdb",
        userid: "alice",
        owner: "dbo",
        password: "p",
      },
    });
    expect(ok.valid).toBe(true);

    const missingOwner = validateServerForm({
      serverName: "mssql1",
      driver: "MSSQL",
      properties: {
        driver: "MSSQL",
        server: "db.local",
        port: "1433",
        database: "appdb",
        userid: "alice",
        password: "p",
        schema: "dbo",
      },
    });
    expect(missingOwner.valid).toBe(false);
    expect(missingOwner.missing).toContain("owner");
  });

  it("requires Oracle canonical fields (sid, schema) and rejects database", () => {
    const ok = validateServerForm({
      serverName: "ora1",
      driver: "ORACLE",
      properties: {
        driver: "ORACLE",
        server: "db.local",
        port: "1521",
        userid: "alice",
        sid: "orcl",
        schema: "APP",
        password: "p",
      },
    });
    expect(ok.valid).toBe(true);

    const missingSid = validateServerForm({
      serverName: "ora1",
      driver: "ORACLE",
      properties: {
        driver: "ORACLE",
        server: "db.local",
        port: "1521",
        userid: "alice",
        schema: "APP",
        password: "p",
        database: "X",
      },
    });
    expect(missingSid.valid).toBe(false);
    expect(missingSid.missing).toContain("sid");
    expect(missingSid.missing).not.toContain("database");
  });

  it("treats whitespace-only values as missing", () => {
    const r = validateServerForm({
      serverName: "ftp1",
      driver: "FTP",
      properties: {
        serverip: "h",
        port: "21",
        userid: "   ",
        password: "p",
      },
    });
    expect(r.valid).toBe(false);
    expect(r.missing).toContain("userid");
  });
});
