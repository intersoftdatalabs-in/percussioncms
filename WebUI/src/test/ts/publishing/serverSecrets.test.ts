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
  isSecretPropertyKey,
  redactSecretsForLog,
} from "@/publishing/serverSecrets";

describe("redactSecretsForLog", () => {
  it("redacts password and privateKey", () => {
    const out = redactSecretsForLog({
      serverName: "s1",
      password: "secret",
      privateKey: "KEY",
      nested: { secretKey: "x", region: "us-east-1" },
    }) as Record<string, unknown>;
    expect(out.password).toBe("[REDACTED]");
    expect(out.privateKey).toBe("[REDACTED]");
    expect(out.serverName).toBe("s1");
    expect((out.nested as Record<string, unknown>).secretKey).toBe("[REDACTED]");
    expect((out.nested as Record<string, unknown>).region).toBe("us-east-1");
  });

  it("redacts product S3 lowercase keys (accesskey / securitykey)", () => {
    const out = redactSecretsForLog({
      accesskey: "AKIA",
      securitykey: "sekret",
      bucketName: "b",
    }) as Record<string, unknown>;
    expect(out.accesskey).toBe("[REDACTED]");
    expect(out.securitykey).toBe("[REDACTED]");
    expect(out.bucketName).toBe("b");
  });

  it("identifies secret keys", () => {
    expect(isSecretPropertyKey("password")).toBe(true);
    expect(isSecretPropertyKey("accesskey")).toBe(true);
    expect(isSecretPropertyKey("securitykey")).toBe(true);
    expect(isSecretPropertyKey("serverip")).toBe(false);
  });
});
