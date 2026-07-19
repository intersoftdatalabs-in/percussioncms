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

  it("requires S3 fields", () => {
    expect(requiredFieldsForDriver("AMAZONS3")).toEqual(
      expect.arrayContaining(["bucketName", "region", "accessKey"]),
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
});
