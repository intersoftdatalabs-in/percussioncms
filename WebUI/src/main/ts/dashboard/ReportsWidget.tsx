/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useCallback, useEffect, useState } from "react";
import { isSessionRedirectError } from "../api/client";
import {
  fetchReportsHubSnapshot,
  type ReportsHubSnapshot,
} from "../api/dashboard/shellGadgetsApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface ReportsWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **Reports** gadget — hub of live CMS/DTS report snapshots
 * (SEO issues, forms, comments, activity) instead of a non-existent list API.
 */
export const ReportsWidget: React.FC<ReportsWidgetProps> = ({
  title,
  refreshInterval = 120000,
}) => {
  const heading = title ?? message(MSG.GADGET_REPORTS);
  const [snap, setSnap] = useState<ReportsHubSnapshot | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      setSnap(await fetchReportsHubSnapshot());
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) return;
      setError(formatApiError(err, "Failed to load report snapshot"));
      setSnap(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    if (refreshInterval <= 0) return;
    const id = window.setInterval(() => void load(), refreshInterval);
    return () => window.clearInterval(id);
  }, [load, refreshInterval]);

  if (isLoading) {
    return (
      <div style={styles.widget} data-testid="reports-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetLoading}>Loading reports…</div>
      </div>
    );
  }
  if (error) {
    return (
      <div style={styles.widget} data-testid="reports-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetError}>{error}</div>
      </div>
    );
  }
  if (!snap) {
    return (
      <div style={styles.widget} data-testid="reports-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetContent}>No report data.</div>
      </div>
    );
  }

  const cards: Array<{ label: string; value: number | string }> = [
    { label: "SEO issue pages", value: snap.seoIssuePages },
    { label: "Forms (site)", value: snap.formsCount },
    { label: "New form submissions", value: snap.formsNewSubmissions },
    { label: "Pages with comments", value: snap.pagesWithComments },
    { label: "Activity folders (30d)", value: snap.activityRows },
    { label: "New items (activity)", value: snap.activityNewItems },
  ];

  return (
    <div style={styles.widget} data-testid="reports-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      <div style={styles.widgetContent} data-testid="reports-hub">
        <p style={{ fontSize: "0.85em", color: "#666", marginTop: 0 }}>
          Snapshot for {snap.path}
        </p>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: 8,
          }}
        >
          {cards.map((c) => (
            <div
              key={c.label}
              style={{
                padding: 10,
                background: "#f5f9fc",
                borderRadius: 4,
                borderLeft: "3px solid #007ea8",
              }}
            >
              <div style={{ fontSize: "0.75em", color: "#666" }}>{c.label}</div>
              <div style={{ fontSize: "1.25em", fontWeight: 700, color: "#007ea8" }}>
                {c.value}
              </div>
            </div>
          ))}
        </div>
        <p style={{ fontSize: "0.8em", color: "#666", marginTop: 12 }}>
          Open SEO Audit, Form Tracker, Comments, or Activity gadgets for
          detail. Partial failures (e.g. DTS offline) show as zeros for that
          metric.
        </p>
      </div>
    </div>
  );
};

export default ReportsWidget;
