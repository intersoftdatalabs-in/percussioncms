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
  AUTH_MAIL_TEST_RESULT_ROOT,
  AUTH_READINESS_REPORT_ROOT,
  AuthReadinessForbiddenError,
  fetchAuthReadinessReport,
  initiateAdminRecovery,
  sendAuthMailTest,
  StepUpRequiredError,
  unwrapAdminRecoveryResponse,
  unwrapAuthMailTestResult,
  unwrapAuthReadinessReport,
} from "../../../../main/ts/api/auth/authReadinessApi";
import * as client from "../../../../main/ts/api/client";

vi.mock("../../../../main/ts/api/client", async () => {
  const actual = await vi.importActual<typeof import("../../../../main/ts/api/client")>(
    "../../../../main/ts/api/client",
  );
  return {
    ...actual,
    get: vi.fn(),
    post: vi.fn(),
  };
});

describe("authReadinessApi", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("unwraps WRAP_ROOT AuthReadinessReport including one-item Jackson objects", () => {
    const report = unwrapAuthReadinessReport({
      [AUTH_READINESS_REPORT_ROOT]: {
        generatedAt: "2026-11-19T00:00:00Z",
        mfaMode: "optional",
        passwordMinLength: 12,
        mailConfigured: true,
        mailSource: "SERVER_PROPERTIES",
        clusterKeyFingerprint: "sha256:0123456789ab",
        overallStatus: "WARN",
        infraChecks: {
          AuthInfraCheck: {
            code: "AUTH_MAIL",
            label: "Transactional Auth Mail",
            status: "PASS",
            summary: "Configured",
            remediation: "",
          },
        },
        accountSummary: {
          totalAccounts: 2,
          readyCount: 1,
          missingEmailCount: 1,
        },
        accountIssues: {
          AuthAccountReadinessItem: {
            userName: "editor1",
            providerType: "INTERNAL",
            email: "",
            emailVerified: false,
            mfaEnrollmentState: "NONE",
            totpCount: 0,
            passkeyCount: 0,
            recoveryCodeCount: 0,
            issues: ["MISSING_EMAIL", "NO_MFA_ENROLLED"],
          },
        },
      },
    });

    expect(report.overallStatus).toBe("WARN");
    expect(report.clusterKeyFingerprint).toBe("sha256:0123456789ab");
    expect(report.infraChecks).toHaveLength(1);
    expect(report.infraChecks[0]?.code).toBe("AUTH_MAIL");
    expect(report.accountIssues).toHaveLength(1);
    expect(report.accountIssues[0]?.userName).toBe("editor1");
    expect(report.accountIssues[0]?.issues).toEqual(["MISSING_EMAIL", "NO_MFA_ENROLLED"]);
  });

  it("accepts flat AuthReadinessReport shape and handles null safely", () => {
    const flat = unwrapAuthReadinessReport({
      overallStatus: "PASS",
      mfaMode: "required",
      passwordMinLength: 14,
      infraChecks: [],
      accountIssues: [],
    });
    expect(flat.overallStatus).toBe("PASS");
    expect(flat.mfaMode).toBe("required");
    expect(flat.passwordMinLength).toBe(14);

    const empty = unwrapAuthReadinessReport(null);
    expect(empty.infraChecks).toEqual([]);
    expect(empty.accountIssues).toEqual([]);
  });

  it("maps HTTP 403 to AuthReadinessForbiddenError on fetch and test send", async () => {
    vi.mocked(client.get).mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: "Admin role required",
    });

    await expect(fetchAuthReadinessReport()).rejects.toBeInstanceOf(
      AuthReadinessForbiddenError,
    );

    vi.mocked(client.post).mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: "Admin role required",
    });

    await expect(sendAuthMailTest("ops@example.com")).rejects.toBeInstanceOf(
      AuthReadinessForbiddenError,
    );
  });

  it("wraps AuthMailTestRequest on POST and unwraps wrapped/flat AuthMailTestResult", async () => {
    vi.mocked(client.post).mockResolvedValue({
      [AUTH_MAIL_TEST_RESULT_ROOT]: {
        sent: true,
        recipient: "ops@example.com",
        source: "SERVER_PROPERTIES",
        message: "Test authentication email sent successfully.",
      },
    });

    const res = await sendAuthMailTest("  ops@example.com ");
    expect(res.sent).toBe(true);
    expect(res.source).toBe("SERVER_PROPERTIES");
    expect(client.post).toHaveBeenCalledWith(expect.stringContaining("/auth/admin/mail/test"), {
      AuthMailTestRequest: { recipient: "ops@example.com" },
    });

    const flat = unwrapAuthMailTestResult({
      sent: false,
      recipient: "ops@example.com",
      source: "UNCONFIGURED",
      message: "No SMTP host",
    });
    expect(flat.sent).toBe(false);
    expect(flat.message).toBe("No SMTP host");
  });

  it("wraps AdminRecoveryRequest, unwraps AdminRecoveryResponse, and maps 428 to StepUpRequiredError", async () => {
    vi.mocked(client.post).mockResolvedValueOnce({
      AdminRecoveryResponse: {
        issued: true,
        targetUserName: "alice",
        recoveryMode: "RECOVERY_LINK",
        delivery: "EMAIL",
        expiresAt: "2026-11-19T03:00:00Z",
        message: "Break-glass link sent",
      },
    });

    const ok = await initiateAdminRecovery({
      targetUserName: " alice ",
      verificationNote: " Verified via ticket #99 ",
      resetImmediately: false,
    });
    expect(ok.issued).toBe(true);
    expect(ok.recoveryMode).toBe("RECOVERY_LINK");
    expect(client.post).toHaveBeenCalledWith(expect.stringContaining("/auth/admin/recovery"), {
      AdminRecoveryRequest: {
        targetUserName: "alice",
        verificationNote: "Verified via ticket #99",
        resetImmediately: false,
      },
    });

    const unwrapped = unwrapAdminRecoveryResponse({
      issued: true,
      targetUserName: "bob",
      recoveryMode: "IMMEDIATE_MFA_RESET",
      delivery: "DIRECT",
      expiresAt: "",
      message: "Reset immediately",
    });
    expect(unwrapped.recoveryMode).toBe("IMMEDIATE_MFA_RESET");

    vi.mocked(client.post).mockRejectedValueOnce({
      status: 428,
      statusText: "Precondition Required",
      body: { error: "STEP_UP_REQUIRED", message: "Confirm two-step verification." },
    });
    await expect(
      initiateAdminRecovery({
        targetUserName: "alice",
        verificationNote: "Verified ticket #99",
        resetImmediately: true,
      }),
    ).rejects.toBeInstanceOf(StepUpRequiredError);
  });
});
