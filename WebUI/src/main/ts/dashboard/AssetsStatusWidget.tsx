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
  fetchAssetsByStatusSummary,
  type PagesByStatusResult,
} from "../api/dashboard/gadgetApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface AssetsStatusWidgetProps {
  title?: string;
  path?: string;
  workflow?: string;
  refreshInterval?: number;
}

/**
 * Classic **Assets By Status** gadget.
 *
 * <p>Uses the same {@code path/item/wfState} API as Pages By Status, rooted
 * under {@code /Assets}. Invented path {@code /services/asset/workflow-status}
 * does not exist.</p>
 */
export const AssetsStatusWidget: React.FC<AssetsStatusWidgetProps> = ({
  title,
  path,
  workflow,
  refreshInterval = 30000,
}) => {
  const heading = title ?? message(MSG.GADGET_ASSETS_BY_STATUS);
  const [data, setData] = useState<PagesByStatusResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const result = await fetchAssetsByStatusSummary({ path, workflow });
      setData(result);
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Failed to load asset status"));
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

  const getStatusColor = (percentage: number): string => {
    if (percentage === 0) return "#ccc";
    if (percentage >= 50) return "#4caf50";
    if (percentage >= 25) return "#ff9800";
    return "#f44336";
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading} data-testid="assets-status-loading">
          <p>Loading asset status...</p>
        </div>
      );
    }
    if (error) {
      return (
        <div style={styles.widgetError} data-testid="assets-status-error">
          <p>Error: {error}</p>
        </div>
      );
    }
    if (!data || data.buckets.length === 0) {
      return (
        <div style={styles.widgetContent} data-testid="assets-status-empty">
          <p>No assets found for this path and workflow.</p>
          {data ? (
            <p style={{ fontSize: "0.85em", color: "#666" }}>
              {data.path} · {data.workflow}
            </p>
          ) : null}
        </div>
      );
    }

    const total = data.totalItems;
    return (
      <div style={styles.widgetContent} data-testid="assets-status-list">
        <div style={{ marginBottom: "8px", fontSize: "0.85em", color: "#666" }}>
          {data.path} · {data.workflow} · Total: <strong>{total}</strong>
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          {data.buckets.map((bucket) => {
            const percentage =
              total > 0 ? Math.round((bucket.count / total) * 100) : 0;
            return (
              <div
                key={bucket.state}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "8px",
                  padding: "8px",
                  backgroundColor: "#f9f9f9",
                  borderRadius: "4px",
                }}
              >
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 500, color: "#333", fontSize: "0.9em" }}>
                    {bucket.state}
                  </div>
                  {bucket.sampleNames.length > 0 ? (
                    <div style={{ fontSize: "0.8em", color: "#666" }}>
                      {bucket.sampleNames.join(", ")}
                      {bucket.count > bucket.sampleNames.length ? "…" : ""}
                    </div>
                  ) : null}
                  <div
                    style={{
                      height: "6px",
                      backgroundColor: "#e0e0e0",
                      borderRadius: "3px",
                      marginTop: "4px",
                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        height: "100%",
                        backgroundColor: getStatusColor(percentage),
                        width: `${percentage}%`,
                      }}
                    />
                  </div>
                </div>
                <div style={{ textAlign: "right", whiteSpace: "nowrap" }}>
                  <div style={{ fontWeight: "bold", color: "#007ea8" }}>
                    {bucket.count}
                  </div>
                  <div style={{ fontSize: "0.75em", color: "#999" }}>
                    {percentage}%
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    );
  };

  return (
    <div style={styles.widget} data-testid="assets-status-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      {renderContent()}
    </div>
  );
};

export default AssetsStatusWidget;
