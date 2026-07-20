/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
  startTime: number;
  endTime: number;
  success: boolean;
  problemDescription?: string;
  serverName?: string;
}

export const TaskLogsSection: React.FC = () => {
  const [logs, setLogs] = useState<TaskLog[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

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
              <th style={{ padding: "12px 16px" }}>{message(ADMIN_MSG.LOG_TIME)}</th>
              <th style={{ padding: "12px 16px" }}>{message(ADMIN_MSG.STATUS)}</th>
              <th style={{ padding: "12px 16px" }}>{message(ADMIN_MSG.SERVER_NAME)}</th>
              <th style={{ padding: "12px 16px" }}>{message(ADMIN_MSG.MESSAGE)}</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 ? (
              <tr>
                <td colSpan={4} style={{ padding: "20px", textAlign: "center", color: "#94a3b8" }}>
                  No execution logs available.
                </td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} style={{ borderBottom: "1px solid #e2e8f0" }} data-testid={`log-row-${log.id}`}>
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
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
