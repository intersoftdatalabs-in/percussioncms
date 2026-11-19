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

import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  getMyEmailStatus,
  requestMyEmailChange,
  resendMyEmailVerification,
  unwrapProfileEmailActionResult,
  unwrapProfileEmailStatus,
} from "../../../../main/ts/api/auth/authProfileApi";
import { get, post } from "../../../../main/ts/api/client";

vi.mock("../../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
}));

describe("authProfileApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("unwraps root-wrapped ProfileEmailStatus", async () => {
    vi.mocked(get).mockResolvedValue({
      ProfileEmailStatus: {
        userName: "editor1",
        email: "old@example.com",
        emailVerified: true,
        emailVerifiedAt: "2026-11-18T10:00:00Z",
        pendingEmail: "pending@example.com",
        emailEditable: true,
        emailRequired: true,
        providerType: "INTERNAL",
      },
    });

    const status = await getMyEmailStatus();
    expect(status.userName).toBe("editor1");
    expect(status.email).toBe("old@example.com");
    expect(status.emailVerified).toBe(true);
    expect(status.pendingEmail).toBe("pending@example.com");
    expect(status.emailEditable).toBe(true);
    expect(status.emailRequired).toBe(true);
  });

  it("wraps ProfileEmailChangeRequest and unwraps ProfileEmailActionResult", async () => {
    vi.mocked(post).mockResolvedValue({
      ProfileEmailActionResult: {
        verificationSent: true,
        appliedImmediately: false,
        email: "old@example.com",
        pendingEmail: "next@example.com",
        emailVerified: true,
        message: "Verification sent",
      },
    });

    const res = await requestMyEmailChange("  next@example.com  ");
    expect(post).toHaveBeenCalledWith(expect.stringContaining("/auth/profile/email/change"), {
      ProfileEmailChangeRequest: { email: "next@example.com" },
    });
    expect(res.verificationSent).toBe(true);
    expect(res.pendingEmail).toBe("next@example.com");

    const resend = await resendMyEmailVerification();
    expect(post).toHaveBeenCalledWith(expect.stringContaining("/auth/profile/email/resend"));
    expect(resend.message).toBe("Verification sent");
  });

  it("handles bare objects cleanly in unwrap helpers", () => {
    const bareStatus = unwrapProfileEmailStatus({
      userName: "ldap.user",
      email: "ldap@example.com",
      emailVerified: false,
      emailEditable: false,
    });
    expect(bareStatus.emailEditable).toBe(false);
    expect(bareStatus.emailVerifiedAt).toBeNull();

    const bareAction = unwrapProfileEmailActionResult({
      verificationSent: true,
      message: "Sent",
    });
    expect(bareAction.verificationSent).toBe(true);
    expect(bareAction.message).toBe("Sent");
  });
});
