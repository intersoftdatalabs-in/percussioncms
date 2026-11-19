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

import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MfaSecurityPanel } from "../../../main/ts/profile/MfaSecurityPanel";
import type { ProfileMfaStatus } from "../../../main/ts/api/auth";

function makeStatus(overrides: Partial<ProfileMfaStatus> = {}): ProfileMfaStatus {
  return {
    userName: "Admin",
    mfaMode: "optional",
    mfaRequired: false,
    totpEnabled: false,
    passkeyCount: 0,
    recoveryCodesRemaining: 0,
    canDisableTotp: true,
    stepUpActive: false,
    stepUpExpiresAt: null,
    ...overrides,
  };
}

describe("MfaSecurityPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  it("enrolls TOTP and displays initial recovery codes", async () => {
    const sampleKey = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
    const loadMfaStatus = vi
      .fn()
      .mockResolvedValueOnce(makeStatus())
      .mockResolvedValueOnce(
        makeStatus({ totpEnabled: true, recoveryCodesRemaining: 2 }),
      );
    const startTotpEnrollment = vi.fn().mockResolvedValue({
      totpId: "totp-1",
      otpauthUri: `otpauth://totp/Percussion:Admin?secret=${sampleKey}`,
      manualSecret: sampleKey,
      issuer: "Percussion CMS",
      accountName: "Admin",
      digits: 6,
      periodSeconds: 30,
    });
    const confirmTotpEnrollment = vi.fn().mockResolvedValue({
      enrolled: true,
      recoveryCodes: ["AAAA1-BBBB2", "CCCC3-DDDD4"],
      message: "Authenticator enabled",
    });

    render(
      <MfaSecurityPanel
        loadMfaStatus={loadMfaStatus}
        startTotpEnrollment={startTotpEnrollment}
        confirmTotpEnrollment={confirmTotpEnrollment}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-mfa-enroll-start")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("perc-profile-mfa-enroll-start"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-mfa-qr")).toBeTruthy();
      expect(screen.getByTestId("perc-profile-mfa-manual-secret").textContent).toBe(
        sampleKey,
      );
    });

    fireEvent.change(screen.getByTestId("perc-profile-mfa-enroll-code"), {
      target: { value: "123456" },
    });
    fireEvent.click(screen.getByTestId("perc-profile-mfa-enroll-confirm"));

    await waitFor(() => {
      expect(confirmTotpEnrollment).toHaveBeenCalledWith("totp-1", "123456");
      expect(
        screen.getByTestId("perc-profile-mfa-recovery-codes").textContent,
      ).toContain("AAAA1-BBBB2");
    });
  });

  it("enforces last-factor guard visually when canDisableTotp is false", async () => {
    const loadMfaStatus = vi.fn().mockResolvedValue(
      makeStatus({
        mfaRequired: true,
        mfaMode: "required",
        totpEnabled: true,
        canDisableTotp: false,
        recoveryCodesRemaining: 5,
      }),
    );

    render(<MfaSecurityPanel loadMfaStatus={loadMfaStatus} />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-mfa-disable")).toBeDisabled();
      expect(screen.getByTestId("perc-profile-mfa-last-factor-hint")).toBeTruthy();
    });
  });

  it("prompts inline step-up on HTTP 428 when disabling TOTP and retries after verification", async () => {
    const loadMfaStatus = vi
      .fn()
      .mockResolvedValueOnce(
        makeStatus({ totpEnabled: true, canDisableTotp: true, recoveryCodesRemaining: 6 }),
      )
      .mockResolvedValueOnce(makeStatus({ totpEnabled: false }));
    const disableTotp = vi
      .fn()
      .mockRejectedValueOnce({
        status: 428,
        statusText: "Precondition Required",
        body: { error: "STEP_UP_REQUIRED", message: "Verify MFA first" },
      })
      .mockResolvedValueOnce({ success: true, message: "TOTP disabled" });
    const verifyStepUp = vi.fn().mockResolvedValue({
      verified: true,
      expiresAtEpochMs: Date.now() + 60_000,
      message: "Verified",
    });

    render(
      <MfaSecurityPanel
        loadMfaStatus={loadMfaStatus}
        disableTotp={disableTotp}
        verifyStepUp={verifyStepUp}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-mfa-disable")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("perc-profile-mfa-disable"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-mfa-stepup")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("perc-profile-mfa-stepup-input"), {
      target: { value: "654321" },
    });
    fireEvent.click(screen.getByTestId("perc-profile-mfa-stepup-submit"));

    await waitFor(() => {
      expect(verifyStepUp).toHaveBeenCalledWith("654321", false);
      expect(disableTotp).toHaveBeenCalledTimes(2);
    });
  });

  it("retries regenerateRecoveryCodes after HTTP 428 step-up verification", async () => {
    const loadMfaStatus = vi.fn().mockResolvedValue(
      makeStatus({ totpEnabled: true, recoveryCodesRemaining: 3 }),
    );
    const regenerateRecoveryCodes = vi
      .fn()
      .mockRejectedValueOnce({
        status: 428,
        statusText: "Precondition Required",
        body: { error: "STEP_UP_REQUIRED", message: "Step-up required" },
      })
      .mockResolvedValueOnce({
        recoveryCodes: ["NEW11-CODE2", "NEW33-CODE4"],
        message: "Regenerated",
      });
    const verifyStepUp = vi.fn().mockResolvedValue({
      verified: true,
      expiresAtEpochMs: Date.now() + 60_000,
      message: "Verified",
    });

    render(
      <MfaSecurityPanel
        loadMfaStatus={loadMfaStatus}
        regenerateRecoveryCodes={regenerateRecoveryCodes}
        verifyStepUp={verifyStepUp}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-mfa-regen-codes")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("perc-profile-mfa-regen-codes"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-mfa-stepup")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("perc-profile-mfa-stepup-input"), {
      target: { value: "999888" },
    });
    fireEvent.click(screen.getByTestId("perc-profile-mfa-stepup-submit"));

    await waitFor(() => {
      expect(regenerateRecoveryCodes).toHaveBeenCalledTimes(2);
      expect(
        screen.getByTestId("perc-profile-mfa-recovery-codes").textContent,
      ).toContain("NEW11-CODE2");
    });
  });
});
