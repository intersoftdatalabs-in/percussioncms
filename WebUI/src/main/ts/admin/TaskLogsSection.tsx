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

import React, { useEffect, useState, useRef } from "react";
import { get, del } from "../api/client";
import { PATHS } from "../api/paths";
import { message } from "../i18n/message";
import { ADMIN_MSG } from "./messages";

export interface TaskLog {
  id: string;
  taskId: string;
  /** Owning schedule name (#6160: server-joined, null when the schedule was deleted). */
  taskName?: string | null;
  startTime: number;
  endTime: number;
  success: boolean;
  problemDescription?: string;
  serverName?: string;
}

/**
 * Elapsed wall-clock between start/end epoch millis as HH:mm:ss (#6160: mirrors
 * the retired TaskDetailLog.jsp "Elapsed" field). Null-safe: "N/A" when either
 * bound is missing or the interval is negative.
 */
export function formatLogElapsed(startTime?: number | null, endTime?: number | null): string {
  if (startTime == null || endTime == null || endTime < startTime) {
    return "N/A";
  }
  const totalSeconds = Math.floor((endTime - startTime) / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
}

export const TaskLogsSection: React.FC = () => {
  const [logs, setLogs] = useState<TaskLog[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const isMountedRef = useRef<boolean>(true);

  const loadLogs = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await get<TaskLog[]>(`${PATHS.SCHEDULED_TASKS}/logs`);
      if (isMountedRef.current) {
        setLogs(res || []);
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || message(ADMIN_MSG.ERROR_GENERIC));
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    isMountedRef.current = true;
    loadLogs();
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const handlePurgeLogs = async () => {
    if (!window.confirm(message(ADMIN_MSG.CONFIRM_PURGE_LOGS))) {
      return;
    }
    setError(null);
    try {
      await del(`${PATHS.SCHEDULED_TASKS}/logs`);
      if (isMountedRef.current) {
        setLogs([]);
      }
    } catch (err: any) {
      if (isMountedRef.current) {
        setError(err?.message || "Failed to purge logs.");
      }
    }
  };

  if (isLoading) {
    return <div style={{ padding: "20px" }}>{message(ADMIN_MSG.LOADING)}</div>;
  }

  return (
    <div className="perc-task-logs-section" data-testid="perc-task-logs-section">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <h2>{message(ADMIN_MSG.TAB_LOGS)}</h2>
        <button
          type="button"
          onClick={handlePurgeLogs}
          className="perc-button-secondary"
          style={{ color: "#ef4444", borderColor: "#fecaca" }}
          data-testid="purge-logs-btn"
        >
          {message(ADMIN_MSG.PURGE_LOGS)}
        </button>
      </div>

      {error && (
        <div style={{ color: "#ef4444", marginBottom: "16px" }} data-testid="logs-error">
          {error}
        </div>
      )}

      <div style={{ overflowX: "auto" }}>
        <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "600px" }}>
          <thead>
            <tr style={{ borderBottom: "2px solid #e2e8f0", textAlign: "left", color: "#475569" }}>
              <th style={{ padding: "12px 16px", width: "48px" }}>
                <span className="sr-only">{message(ADMIN_MSG.LOG_SHOW_DETAILS)}</span>
              </th>
              <th style={{ padding: "12px 16px" }}>{message(ADMIN_MSG.LOG_TIME)}</th>
              <th style={{ padding: "12px 16px" }}>{message(ADMIN_MSG.STATUS)}</th>
              <th style={{ padding: "12px 16px" }}>{message(ADMIN_MSG.SERVER_NAME)}</th>
              <th style={{ padding: "12px 16px" }}>{message(ADMIN_MSG.MESSAGE)}</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ padding: "20px", textAlign: "center", color: "#94a3b8" }}>
                  No execution logs available.
                </td>
              </tr>
            ) : (
              logs.map((log) => {
                const expanded = expandedId === log.id;
                return (
                  <React.Fragment key={log.id}>
                    <tr style={{ borderBottom: expanded ? "none" : "1px solid #e2e8f0" }} data-testid={`log-row-${log.id}`}>
                      <td style={{ padding: "12px 16px" }}>
                        <button
                          type="button"
                          onClick={() => setExpandedId(expanded ? null : log.id)}
                          aria-expanded={expanded}
                          aria-label={
                            expanded
                              ? message(ADMIN_MSG.LOG_HIDE_DETAILS)
                              : message(ADMIN_MSG.LOG_SHOW_DETAILS)
                          }
                          data-testid={`log-expand-${log.id}`}
                          style={{
                            background: "none",
                            border: "1px solid #cbd5e1",
                            borderRadius: "4px",
                            cursor: "pointer",
                            color: "#475569",
                            width: "28px",
                            height: "28px",
                            lineHeight: "1",
                          }}
                        >
                          {expanded ? "−" : "+"}
                        </button>
                      </td>
                      <td style={{ padding: "12px 16px" }}>
                        {log.startTime ? new Date(log.startTime).toLocaleString() : "N/A"}
                      </td>
                      <td style={{ padding: "12px 16px" }}>
                        <span
                          style={{
                            padding: "2px 8px",
                            borderRadius: "12px",
                            fontSize: "12px",
                            fontWeight: 600,
                            backgroundColor: log.success ? "#dcfce7" : "#fee2e2",
                            color: log.success ? "#15803d" : "#b91c1c",
                          }}
                        >
                          {log.success ? "Success" : "Failed"}
                        </span>
                      </td>
                      <td style={{ padding: "12px 16px" }}>{log.serverName || "N/A"}</td>
                      <td style={{ padding: "12px 16px", color: log.success ? "#475569" : "#ef4444" }}>
                        {log.problemDescription || "Completed successfully."}
                      </td>
                    </tr>
                    {expanded && (
                      <tr style={{ borderBottom: "1px solid #e2e8f0" }} data-testid={`log-detail-${log.id}`}>
                        <td />
                        <td colSpan={4} style={{ padding: "12px 16px 20px 16px", backgroundColor: "#f8fafc" }}>
                          <dl style={{ margin: 0, display: "grid", gridTemplateColumns: "160px 1fr", rowGap: "8px" }}>
                            <dt style={{ fontWeight: 600, color: "#475569" }}>{message(ADMIN_MSG.TASK_NAME)}</dt>
                            <dd style={{ margin: 0 }} data-testid={`log-detail-taskname-${log.id}`}>
                              {log.taskName || log.taskId}
                            </dd>
                            <dt style={{ fontWeight: 600, color: "#475569" }}>{message(ADMIN_MSG.LOG_START_TIME)}</dt>
                            <dd style={{ margin: 0 }}>
                              {log.startTime ? new Date(log.startTime).toLocaleString() : "N/A"}
                            </dd>
                            <dt style={{ fontWeight: 600, color: "#475569" }}>{message(ADMIN_MSG.LOG_END_TIME)}</dt>
                            <dd style={{ margin: 0 }}>
                              {log.endTime ? new Date(log.endTime).toLocaleString() : "N/A"}
                            </dd>
                            <dt style={{ fontWeight: 600, color: "#475569" }}>{message(ADMIN_MSG.LOG_ELAPSED)}</dt>
                            <dd style={{ margin: 0 }} data-testid={`log-detail-elapsed-${log.id}`}>
                              {formatLogElapsed(log.startTime, log.endTime)}
                            </dd>
                            <dt style={{ fontWeight: 600, color: "#475569" }}>{message(ADMIN_MSG.STATUS)}</dt>
                            <dd style={{ margin: 0 }}>{log.success ? "Success" : "Failed"}</dd>
                            <dt style={{ fontWeight: 600, color: "#475569" }}>{message(ADMIN_MSG.SERVER_NAME)}</dt>
                            <dd style={{ margin: 0 }}>{log.serverName || "N/A"}</dd>
                            <dt style={{ fontWeight: 600, color: "#475569" }}>{message(ADMIN_MSG.LOG_DETAIL_MESSAGE)}</dt>
                            <dd style={{ margin: 0, whiteSpace: "pre-wrap" }} data-testid={`log-detail-message-${log.id}`}>
                              {log.problemDescription || "Completed successfully."}
                            </dd>
                          </dl>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
