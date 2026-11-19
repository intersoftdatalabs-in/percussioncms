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

import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  confirmMyTotpEnrollment,
  disableMyTotp,
  getMyMfaStatus,
  isStepUpRequiredError,
  regenerateMyRecoveryCodes,
  startMyTotpEnrollment,
  verifyMfaStepUp,
} from "../../../../main/ts/api/auth/authMfaApi";
import { del, get, post } from "../../../../main/ts/api/client";

vi.mock("../../../../main/ts/api/client", async () => {
  const actual = await vi.importActual<
    typeof import("../../../../main/ts/api/client")
  >("../../../../main/ts/api/client");
  return {
    ...actual,
    get: vi.fn(),
    post: vi.fn(),
    del: vi.fn(),
  };
});

describe("authMfaApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("unwraps ProfileMfaStatus with and without Jackson root wrapper", async () => {
    vi.mocked(get).mockResolvedValueOnce({
      ProfileMfaStatus: {
        userName: "Admin",
        mfaMode: "required",
        mfaRequired: true,
        totpEnabled: true,
        passkeyCount: 1,
        recoveryCodesRemaining: 8,
        canDisableTotp: true,
        stepUpActive: false,
        stepUpExpiresAt: null,
      },
    });
    const wrapped = await getMyMfaStatus();
    expect(wrapped.totpEnabled).toBe(true);
    expect(wrapped.recoveryCodesRemaining).toBe(8);
    expect(wrapped.passkeyCount).toBe(1);

    vi.mocked(get).mockResolvedValueOnce({
      userName: "editor1",
      mfaMode: "optional",
      mfaRequired: false,
      totpEnabled: false,
      recoveryCodesRemaining: 0,
      canDisableTotp: true,
      stepUpActive: true,
      stepUpExpiresAt: "2026-11-19T12:00:00Z",
    });
    const bare = await getMyMfaStatus();
    expect(bare.userName).toBe("editor1");
    expect(bare.stepUpActive).toBe(true);
    expect(bare.stepUpExpiresAt).toBe("2026-11-19T12:00:00Z");
  });

  it("wraps MfaStepUpRequest, TotpEnrollStartRequest, and TotpEnrollConfirmRequest", async () => {
    const sampleKey = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
    vi.mocked(post)
      .mockResolvedValueOnce({
        MfaStepUpResponse: {
          verified: true,
          expiresAtEpochMs: 123456789,
          message: "Step-up verified",
        },
      })
      .mockResolvedValueOnce({
        TotpEnrollStartResponse: {
          totpId: "totp-1",
          otpauthUri: `otpauth://totp/Percussion:Admin?secret=${sampleKey}`,
          manualSecret: sampleKey,
          issuer: "Percussion CMS",
          accountName: "Admin",
          digits: 6,
          periodSeconds: 30,
        },
      })
      .mockResolvedValueOnce({
        TotpEnrollConfirmResponse: {
          enrolled: true,
          recoveryCodes: ["AAAA-BBBB1", "CCCC-DDDD2"],
          message: "Authenticator enabled",
        },
      });

    const stepUp = await verifyMfaStepUp(" 123456 ", false);
    expect(post).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining("/auth/mfa/step-up"),
      { MfaStepUpRequest: { code: "123456", recoveryCode: false } },
    );
    expect(stepUp.verified).toBe(true);

    const start = await startMyTotpEnrollment("Work Phone");
    expect(post).toHaveBeenNthCalledWith(
      2,
      expect.stringContaining("/auth/mfa/totp/start"),
      { TotpEnrollStartRequest: { label: "Work Phone" } },
    );
    expect(start.totpId).toBe("totp-1");
    expect(start.manualSecret).toBe(sampleKey);

    const confirm = await confirmMyTotpEnrollment("totp-1", " 654321 ");
    expect(post).toHaveBeenNthCalledWith(
      3,
      expect.stringContaining("/auth/mfa/totp/confirm"),
      { TotpEnrollConfirmRequest: { totpId: "totp-1", code: "654321" } },
    );
    expect(confirm.enrolled).toBe(true);
    expect(confirm.recoveryCodes).toEqual(["AAAA-BBBB1", "CCCC-DDDD2"]);
  });

  it("handles disableTotp and regenerateRecoveryCodes", async () => {
    vi.mocked(del).mockResolvedValueOnce({
      MfaActionResult: { success: true, message: "TOTP disabled" },
    });
    vi.mocked(post).mockResolvedValueOnce({
      RecoveryCodesRegenerateResponse: {
        recoveryCodes: ["11111-22222"],
        message: "Generated",
      },
    });

    const disabled = await disableMyTotp();
    expect(del).toHaveBeenCalledWith(expect.stringContaining("/auth/mfa/totp"));
    expect(disabled.success).toBe(true);

    const regen = await regenerateMyRecoveryCodes();
    expect(post).toHaveBeenCalledWith(
      expect.stringContaining("/auth/mfa/recovery-codes/regenerate"),
    );
    expect(regen.recoveryCodes).toEqual(["11111-22222"]);
  });

  it("detects HTTP 428 / STEP_UP_REQUIRED via isStepUpRequiredError", () => {
    expect(
      isStepUpRequiredError({
        status: 428,
        statusText: "Precondition Required",
        body: { error: "STEP_UP_REQUIRED", message: "Verify MFA" },
      }),
    ).toBe(true);
    expect(
      isStepUpRequiredError({
        status: 400,
        statusText: "Bad Request",
        body: { error: "INVALID_REQUEST" },
      }),
    ).toBe(false);
    expect(isStepUpRequiredError(new Error("boom"))).toBe(false);
  });
});
