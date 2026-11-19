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

import React, { useCallback, useEffect, useState } from "react";
import {
  AuthReadinessForbiddenError,
  fetchAuthReadinessReport,
  sendAuthMailTest,
  type AuthCheckStatus,
  type AuthMailTestResult,
  type AuthReadinessReport,
} from "../../api/auth";
import { formatApiError, isSessionRedirectError } from "../../api/client";
import { message } from "../../i18n/message";
import { ADMIN_MSG } from "../messages";

const ISSUE_FILTER_OPTIONS = [
  "ALL",
  "MISSING_EMAIL",
  "MALFORMED_EMAIL",
  "DUPLICATE_EMAIL",
  "UNVERIFIED_EMAIL",
  "NO_MFA_ENROLLED",
] as const;

function statusColors(status: AuthCheckStatus): { bg: string; fg: string; border: string } {
  switch (status) {
    case "PASS":
      return { bg: "#dcfce7", fg: "#166534", border: "#86efac" };
    case "FAIL":
      return { bg: "#fee2e2", fg: "#991b1b", border: "#fca5a5" };
    default:
      return { bg: "#fef3c7", fg: "#92400e", border: "#fcd34d" };
  }
}

const StatusBadge: React.FC<{ status: AuthCheckStatus }> = ({ status }) => {
  const c = statusColors(status);
  return (
    <span
      style={{
        display: "inline-block",
        padding: "2px 8px",
        borderRadius: 9999,
        fontSize: 12,
        fontWeight: 600,
        backgroundColor: c.bg,
        color: c.fg,
        border: `1px solid ${c.border}`,
      }}
    >
      {status}
    </span>
  );
};

export const AuthReadinessPanel: React.FC = () => {
  const [report, setReport] = useState<AuthReadinessReport | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [issueFilter, setIssueFilter] = useState<string>("ALL");

  const [recipient, setRecipient] = useState<string>("");
  const [sendingTest, setSendingTest] = useState<boolean>(false);
  const [mailTestResult, setMailTestResult] = useState<AuthMailTestResult | null>(null);
  const [mailTestError, setMailTestError] = useState<string | null>(null);

  const loadReport = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAuthReadinessReport();
      setReport(data);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      if (err instanceof AuthReadinessForbiddenError) {
        setError(message(ADMIN_MSG.AUTH_READINESS_FORBIDDEN));
      } else {
        setError(formatApiError(err, message(ADMIN_MSG.ERROR_GENERIC)));
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadReport();
  }, [loadReport]);

  const onSendTestEmail = async (e: React.FormEvent): Promise<void> => {
    e.preventDefault();
    const target = recipient.trim();
    if (!target) {
      setMailTestError("Recipient email address is required.");
      setMailTestResult(null);
      return;
    }
    setSendingTest(true);
    setMailTestError(null);
    setMailTestResult(null);
    try {
      const res = await sendAuthMailTest(target);
      setMailTestResult(res);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setMailTestError(formatApiError(err, message(ADMIN_MSG.ERROR_GENERIC)));
    } finally {
      setSendingTest(false);
    }
  };

  const filteredIssues = (report?.accountIssues ?? []).filter((item) =>
    issueFilter === "ALL" ? true : item.issues.includes(issueFilter),
  );

  return (
    <div className="perc-auth-readiness" data-testid="perc-auth-readiness">
      <header
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          flexWrap: "wrap",
          gap: 12,
          marginBottom: 16,
        }}
      >
        <div>
          <h2 style={{ margin: "0 0 4px" }}>{message(ADMIN_MSG.AUTH_READINESS_TITLE)}</h2>
          <p style={{ margin: 0, color: "#64748b", fontSize: 14 }}>
            {message(ADMIN_MSG.AUTH_READINESS_DESCRIPTION)}
          </p>
        </div>
        <button
          type="button"
          onClick={() => void loadReport()}
          disabled={loading}
          data-testid="auth-readiness-refresh"
          style={buttonStyle}
        >
          {message(ADMIN_MSG.AUTH_READINESS_REFRESH)}
        </button>
      </header>

      {error && (
        <div
          role="alert"
          data-testid="auth-readiness-error"
          style={{
            padding: 12,
            marginBottom: 16,
            borderRadius: 6,
            background: "#fef2f2",
            border: "1px solid #fecaca",
            color: "#991b1b",
          }}
        >
          {error}
        </div>
      )}

      {loading && !report && (
        <p data-testid="auth-readiness-loading">{message(ADMIN_MSG.LOADING)}</p>
      )}

      {report && (
        <>
          <section
            data-testid="auth-readiness-summary"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))",
              gap: 12,
              marginBottom: 20,
            }}
          >
            <SummaryCard label={message(ADMIN_MSG.AUTH_READINESS_OVERALL_STATUS)}>
              <StatusBadge status={report.overallStatus} />
            </SummaryCard>
            <SummaryCard label={message(ADMIN_MSG.AUTH_READINESS_MFA_MODE)}>
              <code>{report.mfaMode}</code>
            </SummaryCard>
            <SummaryCard label={message(ADMIN_MSG.AUTH_READINESS_PASSWORD_POLICY)}>
              <span>{report.passwordMinLength} chars</span>
            </SummaryCard>
            <SummaryCard label={message(ADMIN_MSG.AUTH_READINESS_KEY_FINGERPRINT)}>
              <code style={{ fontSize: 12 }}>
                {report.clusterKeyFingerprint || "—"}
              </code>
            </SummaryCard>
            <SummaryCard label={message(ADMIN_MSG.AUTH_READINESS_TOTAL_ACCOUNTS)}>
              <span>{report.accountSummary.totalAccounts}</span>
            </SummaryCard>
            <SummaryCard label={message(ADMIN_MSG.AUTH_READINESS_READY_ACCOUNTS)}>
              <span>{report.accountSummary.readyCount}</span>
            </SummaryCard>
            <SummaryCard label={message(ADMIN_MSG.AUTH_READINESS_MFA_ENROLLED)}>
              <span>{report.accountSummary.mfaEnrolledCount}</span>
            </SummaryCard>
            <SummaryCard label={message(ADMIN_MSG.AUTH_READINESS_EMAIL_ISSUES)}>
              <span>
                {report.accountSummary.missingEmailCount +
                  report.accountSummary.malformedEmailCount +
                  report.accountSummary.duplicateEmailCount +
                  report.accountSummary.unverifiedEmailCount}
              </span>
            </SummaryCard>
          </section>

          <section style={{ marginBottom: 24 }}>
            <h3 style={{ margin: "0 0 8px", fontSize: 16 }}>
              {message(ADMIN_MSG.AUTH_READINESS_INFRA_TITLE)}
            </h3>
            <table
              data-testid="auth-readiness-infra-table"
              style={tableStyle}
            >
              <thead>
                <tr>
                  <th style={thStyle}>{message(ADMIN_MSG.AUTH_READINESS_COL_CHECK)}</th>
                  <th style={thStyle}>{message(ADMIN_MSG.AUTH_READINESS_COL_STATUS)}</th>
                  <th style={thStyle}>{message(ADMIN_MSG.AUTH_READINESS_COL_SUMMARY)}</th>
                  <th style={thStyle}>{message(ADMIN_MSG.AUTH_READINESS_COL_REMEDIATION)}</th>
                </tr>
              </thead>
              <tbody>
                {report.infraChecks.map((check) => (
                  <tr key={check.code} data-testid={`auth-infra-row-${check.code}`}>
                    <td style={tdStyle}>
                      <strong>{check.label}</strong>
                    </td>
                    <td style={tdStyle}>
                      <StatusBadge status={check.status} />
                    </td>
                    <td style={tdStyle}>{check.summary || "—"}</td>
                    <td style={tdStyle}>{check.remediation || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>

          <section
            style={{
              marginBottom: 24,
              padding: 12,
              background: "#f8fafc",
              borderRadius: 6,
              border: "1px solid #e2e8f0",
            }}
          >
            <h3 style={{ margin: "0 0 8px", fontSize: 16 }}>
              {message(ADMIN_MSG.AUTH_READINESS_MAIL_TEST_TITLE)}
            </h3>
            <form
              data-testid="auth-readiness-mail-test-form"
              onSubmit={(e) => void onSendTestEmail(e)}
              style={{ display: "flex", flexWrap: "wrap", gap: 8, alignItems: "flex-end" }}
            >
              <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                <label htmlFor="auth-mail-test-recipient" style={{ fontSize: 12, fontWeight: 600 }}>
                  {message(ADMIN_MSG.AUTH_READINESS_MAIL_TEST_LABEL)}
                </label>
                <input
                  id="auth-mail-test-recipient"
                  type="email"
                  value={recipient}
                  onChange={(e) => setRecipient(e.target.value)}
                  placeholder="admin@example.com"
                  data-testid="auth-readiness-mail-test-input"
                  style={{
                    padding: "6px 10px",
                    borderRadius: 4,
                    border: "1px solid #cbd5e1",
                    minWidth: 260,
                  }}
                />
              </div>
              <button
                type="submit"
                disabled={sendingTest}
                data-testid="auth-readiness-mail-test-submit"
                style={buttonStyle}
              >
                {sendingTest
                  ? message(ADMIN_MSG.AUTH_READINESS_MAIL_TEST_SENDING)
                  : message(ADMIN_MSG.AUTH_READINESS_MAIL_TEST_SEND)}
              </button>
            </form>
            {mailTestError && (
              <div
                role="alert"
                data-testid="auth-readiness-mail-test-result"
                style={{ marginTop: 8, color: "#991b1b", fontSize: 13 }}
              >
                {mailTestError}
              </div>
            )}
            {mailTestResult && (
              <div
                role="status"
                data-testid="auth-readiness-mail-test-result"
                style={{
                  marginTop: 8,
                  color: mailTestResult.sent ? "#166534" : "#991b1b",
                  fontSize: 13,
                }}
              >
                {mailTestResult.message} ({mailTestResult.source})
              </div>
            )}
          </section>

          <section>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                flexWrap: "wrap",
                gap: 8,
                marginBottom: 8,
              }}
            >
              <h3 style={{ margin: 0, fontSize: 16 }}>
                {message(ADMIN_MSG.AUTH_READINESS_ACCOUNTS_TITLE)} ({filteredIssues.length})
              </h3>
              <label style={{ fontSize: 13, display: "flex", alignItems: "center", gap: 6 }}>
                <span>{message(ADMIN_MSG.AUTH_READINESS_ISSUE_FILTER)}</span>
                <select
                  value={issueFilter}
                  onChange={(e) => setIssueFilter(e.target.value)}
                  data-testid="auth-readiness-issue-filter"
                  style={{ padding: "4px 8px", borderRadius: 4, border: "1px solid #cbd5e1" }}
                >
                  {ISSUE_FILTER_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>
                      {opt === "ALL" ? message(ADMIN_MSG.AUTH_READINESS_FILTER_ALL) : opt}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            {filteredIssues.length === 0 ? (
              <p data-testid="auth-readiness-accounts-empty" style={{ color: "#64748b" }}>
                {message(ADMIN_MSG.AUTH_READINESS_NO_ISSUES)}
              </p>
            ) : (
              <table data-testid="auth-readiness-accounts-table" style={tableStyle}>
                <thead>
                  <tr>
                    <th style={thStyle}>{message(ADMIN_MSG.AUTH_READINESS_COL_USER)}</th>
                    <th style={thStyle}>{message(ADMIN_MSG.AUTH_READINESS_COL_PROVIDER)}</th>
                    <th style={thStyle}>{message(ADMIN_MSG.AUTH_READINESS_COL_EMAIL)}</th>
                    <th style={thStyle}>{message(ADMIN_MSG.AUTH_READINESS_COL_VERIFIED)}</th>
                    <th style={thStyle}>{message(ADMIN_MSG.AUTH_READINESS_COL_FACTORS)}</th>
                    <th style={thStyle}>{message(ADMIN_MSG.AUTH_READINESS_COL_ISSUES)}</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredIssues.map((item) => (
                    <tr
                      key={item.userName}
                      data-testid={`auth-account-row-${item.userName}`}
                    >
                      <td style={tdStyle}>{item.userName}</td>
                      <td style={tdStyle}>{item.providerType}</td>
                      <td style={tdStyle}>{item.email || "—"}</td>
                      <td style={tdStyle}>{item.emailVerified ? "Yes" : "No"}</td>
                      <td style={tdStyle}>
                        TOTP: {item.totpCount} / Passkey: {item.passkeyCount}
                      </td>
                      <td style={tdStyle}>{item.issues.join(", ")}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </>
      )}
    </div>
  );
};

const SummaryCard: React.FC<{ label: string; children: React.ReactNode }> = ({
  label,
  children,
}) => (
  <div
    style={{
      padding: 12,
      background: "#ffffff",
      border: "1px solid #e2e8f0",
      borderRadius: 6,
    }}
  >
    <div style={{ fontSize: 12, color: "#64748b", marginBottom: 4 }}>{label}</div>
    <div style={{ fontSize: 16, fontWeight: 600 }}>{children}</div>
  </div>
);

const buttonStyle: React.CSSProperties = {
  padding: "6px 14px",
  borderRadius: 4,
  border: "1px solid #0284c7",
  backgroundColor: "#0284c7",
  color: "#ffffff",
  fontWeight: 600,
  cursor: "pointer",
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: 13,
};

const thStyle: React.CSSProperties = {
  textAlign: "left",
  padding: "8px 10px",
  borderBottom: "2px solid #e2e8f0",
  background: "#f8fafc",
};

const tdStyle: React.CSSProperties = {
  padding: "8px 10px",
  borderBottom: "1px solid #e2e8f0",
  verticalAlign: "top",
};
