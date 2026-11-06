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
import {
  buildFtpPasswordBody,
  FTP_PASSWORD_MAX_LENGTH,
  FTP_PASSWORD_PROPERTY,
  ftpPasswordHttpFailure,
  redactFtpPasswordBodyForLog,
  validateFtpPassword,
} from "@/publishing/deliveryServerFtpPassword";

describe("deliveryServerFtpPassword", () => {
  it("rejects an empty or whitespace-only password and keeps overlong input client-side", () => {
    expect(validateFtpPassword("")).toEqual({ ok: false, error: "FTP password is required" });
    expect(validateFtpPassword("   ")).toEqual({
      ok: false,
      error: "FTP password is required",
    });
    expect(validateFtpPassword("p".repeat(FTP_PASSWORD_MAX_LENGTH + 1))).toEqual({
      ok: false,
      error: "FTP password must be 255 characters or fewer",
    });
  });

  it("accepts padded input as typed (the wire layer stores it trimmed)", () => {
    expect(validateFtpPassword("  padded  ")).toEqual({ ok: true, password: "  padded  " });
    expect(validateFtpPassword("N3w-secret!")).toEqual({ ok: true, password: "N3w-secret!" });
  });

  it("builds a password-only PUT body with the secret base64-encoded", () => {
    const body = buildFtpPasswordBody("N3w-secret!");
    expect(body.serverInfo.properties).toHaveLength(1);
    expect(body.serverInfo.properties[0]?.key).toBe(FTP_PASSWORD_PROPERTY);
    const sent = String(body.serverInfo.properties[0]?.value ?? "");
    expect(sent).not.toContain("N3w-secret!");
    expect(atob(sent)).toBe("N3w-secret!");
  });

  it("maps only 400, 403, and 409 to form errors", () => {
    expect(ftpPasswordHttpFailure(400)).toBe("bad_request");
    expect(ftpPasswordHttpFailure(403)).toBe("forbidden");
    expect(ftpPasswordHttpFailure(409)).toBe("conflict");
    expect(ftpPasswordHttpFailure(500)).toBeNull();
  });

  it("redacts the secret in log snapshots", () => {
    const body = buildFtpPasswordBody("N3w-secret!");
    const redacted = redactFtpPasswordBodyForLog(body) as {
      serverInfo: { properties: Array<{ key: string; value: string }> };
    };
    expect(JSON.stringify(redacted)).not.toContain("N3w-secret!");
    expect(redacted.serverInfo.properties[0]?.value).toBe("[REDACTED]");
  });
});
