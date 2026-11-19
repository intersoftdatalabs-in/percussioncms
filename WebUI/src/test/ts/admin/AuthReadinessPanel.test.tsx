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
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthReadinessPanel } from "../../../main/ts/admin/tools/AuthReadinessPanel";
import * as api from "../../../main/ts/api/auth/authReadinessApi";
import * as mfaApi from "../../../main/ts/api/auth/authMfaApi";
import {
  AuthReadinessForbiddenError,
  StepUpRequiredError,
} from "../../../main/ts/api/auth/authReadinessApi";
import type { AuthReadinessReport } from "../../../main/ts/api/auth/types";

vi.mock("../../../main/ts/api/auth/authReadinessApi", async () => {
  const actual = await vi.importActual<
    typeof import("../../../main/ts/api/auth/authReadinessApi")
  >("../../../main/ts/api/auth/authReadinessApi");
  return {
    ...actual,
    fetchAuthReadinessReport: vi.fn(),
    sendAuthMailTest: vi.fn(),
    initiateAdminRecovery: vi.fn(),
  };
});

vi.mock("../../../main/ts/api/auth/authMfaApi", async () => {
  const actual = await vi.importActual<
    typeof import("../../../main/ts/api/auth/authMfaApi")
  >("../../../main/ts/api/auth/authMfaApi");
  return {
    ...actual,
    verifyMfaStepUp: vi.fn(),
  };
});

function sampleReport(): AuthReadinessReport {
  return {
    generatedAt: "2026-11-19T00:00:00Z",
    mfaMode: "optional",
    passwordMinLength: 12,
    mailConfigured: true,
    mailSource: "SERVER_PROPERTIES",
    clusterKeyFingerprint: "sha256:0123456789ab",
    overallStatus: "WARN",
    infraChecks: [
      {
        code: "AUTH_MAIL",
        label: "Transactional Auth Mail",
        status: "PASS",
        summary: "Configured via server.properties",
        remediation: "",
      },
      {
        code: "PUBLIC_BASE_URL",
        label: "Public Base URL",
        status: "WARN",
        summary: "auth.publicBaseUrl is not set.",
        remediation: "Set https:// URL.",
      },
    ],
    accountSummary: {
      totalAccounts: 3,
      internalAccounts: 2,
      directoryAccounts: 1,
      readyCount: 1,
      missingEmailCount: 1,
      malformedEmailCount: 0,
      duplicateEmailCount: 0,
      unverifiedEmailCount: 1,
      mfaEnrolledCount: 1,
      totpEnrolledCount: 1,
      passkeyEnrolledCount: 0,
    },
    accountIssues: [
      {
        userName: "alice",
        providerType: "INTERNAL",
        email: "",
        emailVerified: false,
        mfaEnrollmentState: "NONE",
        totpCount: 0,
        passkeyCount: 0,
        recoveryCodeCount: 0,
        issues: ["MISSING_EMAIL", "NO_MFA_ENROLLED"],
      },
      {
        userName: "bob",
        providerType: "DIRECTORY",
        email: "bob@example.com",
        emailVerified: false,
        mfaEnrollmentState: "OPTIONAL",
        totpCount: 1,
        passkeyCount: 0,
        recoveryCodeCount: 5,
        issues: ["UNVERIFIED_EMAIL"],
      },
    ],
  };
}

describe("AuthReadinessPanel", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders summary, infra checks, and filters account issues", async () => {
    vi.mocked(api.fetchAuthReadinessReport).mockResolvedValue(sampleReport());

    render(<AuthReadinessPanel />);

    await waitFor(() => {
      expect(screen.getByTestId("auth-readiness-summary")).toBeDefined();
    });
    expect(screen.getByTestId("auth-infra-row-AUTH_MAIL")).toBeDefined();
    expect(screen.getByTestId("auth-infra-row-PUBLIC_BASE_URL")).toBeDefined();
    expect(screen.getByTestId("auth-account-row-alice")).toBeDefined();
    expect(screen.getByTestId("auth-account-row-bob")).toBeDefined();

    fireEvent.change(screen.getByTestId("auth-readiness-issue-filter"), {
      target: { value: "MISSING_EMAIL" },
    });

    expect(screen.getByTestId("auth-account-row-alice")).toBeDefined();
    expect(screen.queryByTestId("auth-account-row-bob")).toBeNull();
  });

  it("sends test auth email and displays status feedback", async () => {
    vi.mocked(api.fetchAuthReadinessReport).mockResolvedValue(sampleReport());
    vi.mocked(api.sendAuthMailTest).mockResolvedValue({
      sent: true,
      recipient: "ops@example.com",
      source: "SERVER_PROPERTIES",
      message: "Test authentication email sent successfully.",
    });

    render(<AuthReadinessPanel />);

    await waitFor(() => {
      expect(screen.getByTestId("auth-readiness-mail-test-input")).toBeDefined();
    });

    fireEvent.change(screen.getByTestId("auth-readiness-mail-test-input"), {
      target: { value: "ops@example.com" },
    });
    fireEvent.click(screen.getByTestId("auth-readiness-mail-test-submit"));

    await waitFor(() => {
      expect(screen.getByTestId("auth-readiness-mail-test-result").textContent).toContain(
        "sent successfully",
      );
    });
    expect(api.sendAuthMailTest).toHaveBeenCalledWith("ops@example.com");
  });

  it("shows forbidden error banner on 403", async () => {
    vi.mocked(api.fetchAuthReadinessReport).mockRejectedValue(
      new AuthReadinessForbiddenError(),
    );

    render(<AuthReadinessPanel />);

    await waitFor(() => {
      expect(screen.getByTestId("auth-readiness-error")).toBeDefined();
    });
    expect(screen.getByTestId("auth-readiness-error").textContent).toMatch(
      /admin/i,
    );
  });

  it("initiates admin break-glass recovery with inline step-up retry", async () => {
    vi.mocked(api.fetchAuthReadinessReport).mockResolvedValue(sampleReport());
    vi.mocked(api.initiateAdminRecovery)
      .mockRejectedValueOnce(new StepUpRequiredError())
      .mockResolvedValueOnce({
        targetUserName: "alice",
        recoveryMode: "LINK_SENT",
        linkSent: true,
        mfaReset: false,
        message: "Recovery link sent to alice's verified email.",
      });
    vi.mocked(mfaApi.verifyMfaStepUp).mockResolvedValue({
      verified: true,
      expiresAtEpochMs: 1795065000000,
      message: "Step-up verification succeeded.",
    });

    render(<AuthReadinessPanel />);

    await waitFor(() => {
      expect(screen.getByTestId("auth-account-recover-alice")).toBeDefined();
    });

    fireEvent.click(screen.getByTestId("auth-account-recover-alice"));
    const userInput = screen.getByTestId(
      "auth-admin-recovery-user",
    ) as HTMLInputElement;
    expect(userInput.value).toBe("alice");

    fireEvent.change(screen.getByTestId("auth-admin-recovery-note"), {
      target: { value: "Verified identity on support call #4421" },
    });
    fireEvent.click(screen.getByTestId("auth-admin-recovery-submit"));

    await waitFor(() => {
      expect(
        screen.getByTestId("auth-admin-recovery-stepup-input"),
      ).toBeDefined();
    });

    fireEvent.change(screen.getByTestId("auth-admin-recovery-stepup-input"), {
      target: { value: "123456" },
    });
    fireEvent.click(screen.getByTestId("auth-admin-recovery-stepup-submit"));

    await waitFor(() => {
      expect(
        screen.getByTestId("auth-admin-recovery-status").textContent,
      ).toContain("LINK_SENT");
    });

    expect(mfaApi.verifyMfaStepUp).toHaveBeenCalledWith("123456", false);
    expect(api.initiateAdminRecovery).toHaveBeenCalledTimes(2);
    expect(api.initiateAdminRecovery).toHaveBeenCalledWith({
      targetUserName: "alice",
      verificationNote: "Verified identity on support call #4421",
      resetImmediately: false,
    });
  });
});
