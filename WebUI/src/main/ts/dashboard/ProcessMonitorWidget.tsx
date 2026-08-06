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

import React, { useCallback, useEffect, useState } from "react";
import { isSessionRedirectError } from "../api/client";
import {
  fetchProcessMonitors,
  type ProcessMonitorRow,
} from "../api/dashboard/gadgetApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface ProcessMonitorWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **Process Monitor** gadget.
 *
 * <p>Server: {@code GET /services/sitemanage/monitor/all}
 * (classic {@code PROCESS_STATUS_ALL}). Not {@code /services/monitor/all}.</p>
 */
export const ProcessMonitorWidget: React.FC<ProcessMonitorWidgetProps> = ({
  title,
  refreshInterval = 30000,
}) => {
  const heading = title ?? message(MSG.GADGET_PROCESS_MONITOR);
  const [monitors, setMonitors] = useState<ProcessMonitorRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      setMonitors(await fetchProcessMonitors());
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Failed to load process monitor"));
      setMonitors([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    if (refreshInterval <= 0) {
      return;
    }
    const id = window.setInterval(() => void load(), refreshInterval);
    return () => window.clearInterval(id);
  }, [load, refreshInterval]);

  const getStatusIcon = (monitor: ProcessMonitorRow): string => {
    const status = monitor.status?.toString().toLowerCase();
    if (status === "running" || status === "active" || status === "ok") {
      return "✅";
    }
    if (status === "paused" || status === "idle") {
      return "⏸️";
    }
    if (status === "error" || status === "failed") {
      return "❌";
    }
    return "📊";
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading} data-testid="process-monitor-loading">
          <p>Loading process monitor...</p>
        </div>
      );
    }
    if (error) {
      return (
        <div style={styles.widgetError} data-testid="process-monitor-error">
          <p>Error: {error}</p>
        </div>
      );
    }
    if (monitors.length === 0) {
      return (
        <div style={styles.widgetContent} data-testid="process-monitor-empty">
          <p>No monitors available</p>
        </div>
      );
    }
    return (
      <div style={styles.widgetContent} data-testid="process-monitor-list">
        {monitors.map((monitor, index) => (
          <div
            key={monitor.designator || index}
            style={{
              padding: "10px 0",
              borderBottom: "1px solid #e0e0e0",
              display: "flex",
              gap: "12px",
              alignItems: "center",
            }}
          >
            <div style={{ fontSize: "1.2em", minWidth: "28px", textAlign: "center" }}>
              {getStatusIcon(monitor)}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontWeight: 500, color: "#333", fontSize: "0.95em" }}>
                {monitor.name}
              </div>
              {monitor.message ? (
                <div style={{ fontSize: "0.8em", color: "#666", marginTop: "2px" }}>
                  {monitor.message}
                </div>
              ) : null}
            </div>
            {monitor.status ? (
              <div
                style={{
                  fontSize: "0.8em",
                  padding: "4px 8px",
                  borderRadius: "4px",
                  backgroundColor: "#f0f0f0",
                  color: "#555",
                  whiteSpace: "nowrap",
                }}
              >
                {monitor.status}
              </div>
            ) : null}
          </div>
        ))}
      </div>
    );
  };

  return (
    <div style={styles.widget} data-testid="process-monitor-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      {renderContent()}
    </div>
  );
};

export default ProcessMonitorWidget;
