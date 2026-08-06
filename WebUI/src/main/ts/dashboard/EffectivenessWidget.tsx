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
import { isAnalyticsProviderConfigured } from "../api/dashboard/analyticsApi";
import {
  fetchDefaultEffectiveness,
  type EffectivenessRow,
} from "../api/dashboard/gadgetApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface EffectivenessWidgetProps {
  title?: string;
  refreshInterval?: number;
  /** Duration window in days (classic duration + durationType=days). */
  durationDays?: number;
}

/**
 * Classic **What's Working** (effectiveness) gadget.
 *
 * <p>Server: {@code POST /services/activitymanagement/activity/effectiveness}
 * with {@code EffectivenessRequest}. Requires Google Analytics provider
 * config (server rejects with “Analytics has not been setup yet”).</p>
 */
export const EffectivenessWidget: React.FC<EffectivenessWidgetProps> = ({
  title,
  refreshInterval = 60000,
  durationDays = 30,
}) => {
  const heading = title ?? message(MSG.GADGET_WHATS_WORKING);
  const [path, setPath] = useState("");
  const [rows, setRows] = useState<EffectivenessRow[]>([]);
  const [analyticsOk, setAnalyticsOk] = useState<boolean | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      let configured = false;
      try {
        configured = await isAnalyticsProviderConfigured();
      } catch {
        configured = false;
      }
      setAnalyticsOk(configured);
      if (!configured) {
        setRows([]);
        setPath("");
        setError(null);
        return;
      }
      const result = await fetchDefaultEffectiveness(durationDays);
      setPath(result.path);
      setRows(result.rows);
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Failed to load effectiveness metrics"));
      setRows([]);
    } finally {
      setIsLoading(false);
    }
  }, [durationDays]);

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
          data-testid="effectiveness-widget-loading"
        >
          <p>Loading effectiveness...</p>
        </div>
      );
    }

    if (analyticsOk === false) {
      return (
        <div
          style={styles.widgetContent}
          data-testid="effectiveness-widget-needs-analytics"
        >
          <p style={{ fontWeight: 600, marginTop: 0 }}>
            Google Analytics is not configured
          </p>
          <p style={{ fontSize: "0.9em", color: "#555" }}>
            What&apos;s Working needs a Google Analytics provider and site
            profile. Use the <strong>Google Setup</strong> gadget, then refresh
            this widget.
          </p>
        </div>
      );
    }

    if (error) {
      return (
        <div
          style={styles.widgetError}
          data-testid="effectiveness-widget-error"
        >
          <p>Error: {error}</p>
        </div>
      );
    }

    if (rows.length === 0) {
      return (
        <div
          style={styles.widgetContent}
          data-testid="effectiveness-widget-empty"
        >
          <p>No effectiveness data for this path and duration.</p>
          {path ? (
            <p style={{ fontSize: "0.85em", color: "#666" }}>
              {path} · last {durationDays} days
            </p>
          ) : null}
        </div>
      );
    }

    const max = Math.max(...rows.map((r) => r.effectiveness), 1);

    return (
      <div style={styles.widgetContent} data-testid="effectiveness-widget-list">
        <p style={{ fontSize: "0.85em", color: "#666", marginTop: 0 }}>
          {path} · last {durationDays} days
        </p>
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {rows.map((row) => {
            const pct = Math.round((row.effectiveness / max) * 100);
            return (
              <li
                key={row.name}
                style={{
                  padding: "8px 0",
                  borderBottom: "1px solid #e0e0e0",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    gap: "8px",
                    marginBottom: "4px",
                  }}
                >
                  <span style={{ fontWeight: 500 }}>{row.name}</span>
                  <span style={{ color: "#007ea8", fontWeight: 600 }}>
                    {row.effectiveness.toLocaleString()}
                  </span>
                </div>
                <div
                  style={{
                    height: "6px",
                    backgroundColor: "#e0e0e0",
                    borderRadius: "3px",
                    overflow: "hidden",
                  }}
                >
                  <div
                    style={{
                      height: "100%",
                      width: `${pct}%`,
                      backgroundColor: "#007ea8",
                    }}
                  />
                </div>
              </li>
            );
          })}
        </ul>
      </div>
    );
  };

  return (
    <div style={styles.widget} data-testid="effectiveness-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      {renderContent()}
    </div>
  );
};

export default EffectivenessWidget;
