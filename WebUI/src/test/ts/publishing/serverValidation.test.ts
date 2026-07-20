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
      expect.arrayContaining(["serverip", "user", "password", "port"]),
    );
  });

  it("requires S3 fields using product lowercase keys", () => {
    expect(requiredFieldsForDriver("AMAZONS3")).toEqual(
      expect.arrayContaining([
        "serverName",
        "bucketName",
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
      properties: { user: "u", password: "p", port: "21" },
    });
    expect(r.valid).toBe(false);
    expect(r.missing).toContain("serverip");
  });

  it("passes S3 when form uses accesskey/securitykey", () => {
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
    expect(r.valid).toBe(true);
    expect(r.missing).toEqual([]);
  });

  it("fails S3 when only camelCase keys present (mismatch with form)", () => {
    const r = validateServerForm({
      serverName: "s3prod",
      driver: "AMAZONS3",
      properties: {
        accessKey: "AKIA",
        secretKey: "secret",
        bucketName: "my-bucket",
        region: "us-east-1",
      },
    });
    expect(r.valid).toBe(false);
    expect(r.missing).toEqual(
      expect.arrayContaining(["accesskey", "securitykey"]),
    );
  });
});
