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
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import { isSessionRedirectError } from "../api/client";
import { isAnalyticsProviderConfigured } from "../api/dashboard/analyticsApi";
import {
  fetchDefaultContentTraffic,
  type ContentTrafficResult,
  type TrafficGranularity,
} from "../api/dashboard/gadgetApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface TrafficWidgetProps {
  title?: string;
  refreshInterval?: number;
  daysRange?: number;
  granularity?: TrafficGranularity;
}

/**
 * Classic **Traffic** gadget.
 *
 * <p>Server: {@code POST /services/activitymanagement/activity/contenttraffic}
 * with {@code ContentTrafficRequest}. Visits series needs Google Analytics
 * profile mapping (see Google Setup).</p>
 */
export const TrafficWidget: React.FC<TrafficWidgetProps> = ({
  title,
  refreshInterval = 300000,
  daysRange = 30,
  granularity = "DAY",
}) => {
  const heading = title ?? message(MSG.GADGET_TRAFFIC);
  const [result, setResult] = useState<ContentTrafficResult | null>(null);
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
      const data = await fetchDefaultContentTraffic(daysRange, granularity);
      setResult(data);
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Failed to load traffic data"));
      setResult(null);
    } finally {
      setIsLoading(false);
    }
  }, [daysRange, granularity]);

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
        <div style={styles.widgetLoading} data-testid="traffic-widget-loading">
          <p>Loading traffic data...</p>
        </div>
      );
    }
    if (error) {
      return (
        <div style={styles.widgetError} data-testid="traffic-widget-error">
          <p>Error: {error}</p>
          {analyticsOk === false ? (
            <p style={{ fontSize: "0.85em", marginTop: "8px" }}>
              Visits require Google Analytics setup. Open the Google Setup
              gadget and map a profile for this site.
            </p>
          ) : null}
        </div>
      );
    }
    if (!result || result.points.length === 0) {
      return (
        <div style={styles.widgetContent} data-testid="traffic-widget-empty">
          <p>No traffic data for this path and date range.</p>
          {result ? (
            <p style={{ fontSize: "0.85em", color: "#666" }}>
              {result.path} · {result.startDate} – {result.endDate}
            </p>
          ) : null}
        </div>
      );
    }

    const chartData = result.points.map((p) => ({
      name: p.date,
      visits: p.visits,
      livePages: p.livePages,
      newPages: p.newPages,
      pageUpdates: p.pageUpdates,
    }));

    return (
      <div style={styles.widgetContent} data-testid="traffic-widget-list">
        <p style={{ fontSize: "0.85em", color: "#666", marginTop: 0 }}>
          {result.path}
          {result.site ? ` · ${result.site}` : ""} · {result.startDate} –{" "}
          {result.endDate}
        </p>
        {analyticsOk === false ? (
          <p
            style={{
              fontSize: "0.8em",
              color: "#856404",
              background: "#fff3cd",
              padding: "8px",
              borderRadius: "4px",
            }}
            data-testid="traffic-analytics-hint"
          >
            Google Analytics is not configured — visit counts may be empty.
            Use Google Setup to connect a profile.
          </p>
        ) : null}
        <div
          style={{
            display: "flex",
            gap: "12px",
            marginBottom: "12px",
            flexWrap: "wrap",
          }}
        >
          <div
            style={{
              flex: 1,
              minWidth: "100px",
              padding: "10px",
              backgroundColor: "#e3f2fd",
              borderRadius: "4px",
            }}
          >
            <div style={{ fontSize: "0.75em", color: "#666" }}>Visits</div>
            <div style={{ fontSize: "1.25em", fontWeight: 700, color: "#1976d2" }}>
              {result.totalVisits.toLocaleString()}
            </div>
          </div>
          <div
            style={{
              flex: 1,
              minWidth: "100px",
              padding: "10px",
              backgroundColor: "#e8f5e9",
              borderRadius: "4px",
            }}
          >
            <div style={{ fontSize: "0.75em", color: "#666" }}>Live pages Σ</div>
            <div style={{ fontSize: "1.25em", fontWeight: 700, color: "#388e3c" }}>
              {result.totalLivePages.toLocaleString()}
            </div>
          </div>
        </div>
        <div style={{ width: "100%", height: 260 }} data-testid="traffic-chart">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart
              data={chartData}
              margin={{ top: 5, right: 16, left: 0, bottom: 5 }}
            >
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" fontSize={11} />
              <YAxis fontSize={11} />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="visits"
                stroke="#1976d2"
                name="Visits"
                dot={false}
              />
              <Line
                type="monotone"
                dataKey="livePages"
                stroke="#388e3c"
                name="Live pages"
                dot={false}
              />
              <Line
                type="monotone"
                dataKey="newPages"
                stroke="#f57c00"
                name="New pages"
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
    );
  };

  return (
    <div style={styles.widget} data-testid="traffic-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      {renderContent()}
    </div>
  );
};

export default TrafficWidget;
