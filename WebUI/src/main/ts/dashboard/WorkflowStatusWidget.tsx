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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useCallback, useEffect, useState } from "react";
import { isSessionRedirectError } from "../api/client";
import {
  fetchPagesByStatusSummary,
  type PagesByStatusResult,
} from "../api/dashboard/gadgetApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";

export interface WorkflowStatusWidgetProps {
  /** Display title (classic gadget: Pages By Status). */
  title?: string;
  /** Optional CMS path; defaults to first site. */
  path?: string;
  /** Optional workflow name; defaults to system default workflow label. */
  workflow?: string;
  refreshInterval?: number;
}

/**
 * Classic **Pages By Status** gadget (React key {@code workflow}).
 *
 * <p>Server: {@code POST /services/pathmanagement/path/item/wfState} with
 * {@code ItemByWfStateRequest}. Invented path
 * {@code /dashboardmanagement/gadget/workflow-status} does not exist.</p>
 */
export const WorkflowStatusWidget: React.FC<WorkflowStatusWidgetProps> = ({
  title = "Pages By Status",
  path,
  workflow,
  refreshInterval = 30000,
}) => {
  const [data, setData] = useState<PagesByStatusResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const result = await fetchPagesByStatusSummary({ path, workflow });
      setData(result);
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Failed to load pages by status"));
      setData(null);
    } finally {
      setIsLoading(false);
    }
  }, [path, workflow]);

  useEffect(() => {
    void load();
    if (refreshInterval <= 0) {
      return;
    }
    const id = window.setInterval(() => void load(), refreshInterval);
    return () => window.clearInterval(id);
  }, [load, refreshInterval]);

  const renderContent = () => {
    if (isLoading) {
      return (
        <div
          style={styles.widgetLoading}
          data-testid="workflow-status-loading"
        >
          <p>Loading workflow status...</p>
        </div>
      );
    }

    if (error) {
      return (
        <div style={styles.widgetError} data-testid="workflow-status-error">
          <p>Error: {error}</p>
        </div>
      );
    }

    if (!data || data.buckets.length === 0) {
      return (
        <div style={styles.widgetContent} data-testid="workflow-status-empty">
          <p>No pages found for this path and workflow.</p>
          {data ? (
            <p style={{ fontSize: "0.85em", color: "#666" }}>
              {data.path} · {data.workflow}
            </p>
          ) : null}
        </div>
      );
    }

    return (
      <div style={styles.widgetContent} data-testid="workflow-status-list">
        <p style={{ fontSize: "0.85em", color: "#666", marginTop: 0 }}>
          {data.path} · {data.workflow} · {data.totalItems} page
          {data.totalItems === 1 ? "" : "s"}
        </p>
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {data.buckets.map((bucket) => (
            <li
              key={bucket.state}
              style={{
                padding: "8px 0",
                borderBottom: "1px solid #e0e0e0",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <div>
                <div style={{ fontWeight: 500, color: "#333" }}>
                  {bucket.state}
                </div>
                {bucket.sampleNames.length > 0 ? (
                  <div style={{ fontSize: "0.85em", color: "#666" }}>
                    {bucket.sampleNames.join(", ")}
                    {bucket.count > bucket.sampleNames.length ? "…" : ""}
                  </div>
                ) : null}
              </div>
              <div
                style={{
                  backgroundColor: "#e8f4f8",
                  padding: "4px 12px",
                  borderRadius: "12px",
                  fontWeight: 600,
                  color: "#007ea8",
                  fontSize: "0.9em",
                }}
              >
                {bucket.count}
              </div>
            </li>
          ))}
        </ul>
      </div>
    );
  };

  return (
    <div style={styles.widget} data-testid="workflow-status-widget">
      <div style={styles.widgetTitle}>{title}</div>
      {renderContent()}
    </div>
  );
};

export default WorkflowStatusWidget;
