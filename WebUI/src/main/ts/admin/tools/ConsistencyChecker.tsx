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

import React, { useState, useEffect, useRef } from "react";
import { get, post } from "../../api/client";
import { PATHS } from "../../api/paths";
import { message } from "../../i18n/message";
import { ADMIN_MSG } from "../messages";

export interface ConsistencyIssue {
  issueId: string;
  type: string;
  description: string;
  fixable: boolean;
}

export interface ConsistencyJobStatus {
  jobId: string;
  status: "RUNNING" | "COMPLETE" | "ERROR";
  issues?: ConsistencyIssue[];
}

/**
 * Jackson one-item / WRAP_ROOT lists are objects; never call {@code .map} on
 * {@code issues} until it is an array (#3195).
 */
export function asConsistencyIssues(raw: unknown): ConsistencyIssue[] {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw as ConsistencyIssue[];
  }
  if (typeof raw === "object") {
    const rec = raw as Record<string, unknown>;
    const wrapped =
      rec.issues ?? rec.ConsistencyIssue ?? rec.consistencyIssue;
    if (wrapped != null && wrapped !== raw) {
      return asConsistencyIssues(wrapped);
    }
    if (
      typeof rec.issueId === "string" ||
      typeof rec.description === "string"
    ) {
      return [raw as ConsistencyIssue];
    }
  }
  return [];
}

export const ConsistencyChecker: React.FC = () => {
  const [jobStatus, setJobStatus] = useState<ConsistencyJobStatus | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const isMountedRef = useRef<boolean>(true);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const handleStartCheck = async () => {
    setLoading(true);
    setError(null);
    try {
      const res: ConsistencyJobStatus = await post(PATHS.CONSISTENCY_CHECK, {});
      if (isMountedRef.current) {
        setJobStatus({
          ...res,
          issues: asConsistencyIssues(res.issues),
        });
        pollJobStatus(res.jobId);
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || "Failed to start consistency check.");
        setLoading(false);
      }
    }
  };

  const pollJobStatus = async (jobId: string) => {
    if (!isMountedRef.current) return;
    try {
      const res: ConsistencyJobStatus = await get(`${PATHS.CONSISTENCY_CHECK}/${jobId}`);
      if (isMountedRef.current) {
        setJobStatus({
          ...res,
          issues: asConsistencyIssues(res.issues),
        });
        if (res.status === "RUNNING") {
          setTimeout(() => pollJobStatus(jobId), 1000);
        } else {
          setLoading(false);
          if (res.status === "ERROR") {
            setError("Consistency check reported an error.");
          }
        }
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || "Failed to fetch consistency check status.");
        setLoading(false);
      }
    }
  };

  const handleFixIssue = async (issueId: string) => {
    if (!jobStatus) return;
    try {
      await post(`${PATHS.CONSISTENCY_CHECK}/${jobStatus.jobId}/fix/${issueId}`, {});
      if (isMountedRef.current) {
        setJobStatus((prev) => {
          if (!prev) return null;
          return {
            ...prev,
            issues: prev.issues?.filter((i) => i.issueId !== issueId) || [],
          };
        });
        await pollJobStatus(jobStatus.jobId);
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || "Failed to apply fix for issue.");
      }
    }
  };

  return (
    <div style={{ padding: "16px" }} data-testid="perc-consistency-checker">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
        <div>
          <h2 style={{ fontSize: "18px", fontWeight: 600, margin: 0 }}>System Consistency Checker</h2>
          <p style={{ color: "#64748b", margin: "4px 0 0 0", fontSize: "13px" }}>
            Verify content tree integrity, orphaned asset relationships, and system data consistency.
          </p>
        </div>
        <button
          type="button"
          onClick={handleStartCheck}
          disabled={loading}
          className="perc-button-primary"
          style={{ padding: "8px 16px", borderRadius: "4px", backgroundColor: "#0284c7", color: "#fff", border: "none" }}
          data-testid="start-check-btn"
        >
          {loading ? message(ADMIN_MSG.LOADING) : "Run Consistency Check"}
        </button>
      </div>

      {error && (
        <div style={{ padding: "12px", background: "#fef2f2", color: "#991b1b", borderRadius: "4px", marginBottom: "16px" }}>
          {error}
        </div>
      )}

      {jobStatus && (
        <div style={{ background: "#f8fafc", padding: "16px", borderRadius: "8px", border: "1px solid #e2e8f0" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "12px" }}>
            <span style={{ fontWeight: 600 }}>Status:</span>
            <span
              style={{
                padding: "2px 8px",
                borderRadius: "12px",
                fontSize: "12px",
                fontWeight: 600,
                backgroundColor: jobStatus.status === "COMPLETE" ? "#dcfce7" : "#fef3c7",
                color: jobStatus.status === "COMPLETE" ? "#166534" : "#92400e",
              }}
              data-testid="job-status-badge"
            >
              {jobStatus.status}
            </span>
          </div>

          <h3 style={{ fontSize: "14px", fontWeight: 600, margin: "0 0 12px 0" }}>Reported Issues</h3>

          {asConsistencyIssues(jobStatus.issues).length === 0 ? (
            <p style={{ color: "#166534", margin: 0 }}>No consistency issues found. System is fully aligned.</p>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
              {asConsistencyIssues(jobStatus.issues).map((issue) => (
                <div
                  key={issue.issueId}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    background: "#ffffff",
                    padding: "12px",
                    borderRadius: "6px",
                    border: "1px solid #cbd5e1",
                  }}
                  data-testid={`issue-row-${issue.issueId}`}
                >
                  <div>
                    <span style={{ fontWeight: 600, fontSize: "13px", color: "#334155", marginRight: "8px" }}>
                      [{issue.type}]
                    </span>
                    <span style={{ fontSize: "13px", color: "#475569" }}>{issue.description}</span>
                  </div>
                  {issue.fixable && (
                    <button
                      type="button"
                      onClick={() => handleFixIssue(issue.issueId)}
                      style={{
                        padding: "4px 12px",
                        fontSize: "12px",
                        borderRadius: "4px",
                        backgroundColor: "#16a34a",
                        color: "#ffffff",
                        border: "none",
                        cursor: "pointer",
                      }}
                      data-testid={`fix-issue-btn-${issue.issueId}`}
                    >
                      Apply Fix
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
