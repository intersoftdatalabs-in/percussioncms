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
  fetchNonSeoPages,
  type SeoPageRow,
} from "../api/dashboard/deliveryGadgetsApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface SEOAuditWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **SEO Audit** gadget — non-SEO pages under the default site path.
 *
 * <p>{@code POST /services/pagemanagement/page/nonSEOPages}.
 * Invented {@code /services/seo/audit} does not exist.</p>
 */
export const SEOAuditWidget: React.FC<SEOAuditWidgetProps> = ({
  title,
  refreshInterval = 120000,
}) => {
  const heading = title ?? message(MSG.GADGET_SEO_AUDIT);
  const [rows, setRows] = useState<SeoPageRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      setRows(await fetchNonSeoPages({ severity: "ALL" }));
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) return;
      setError(formatApiError(err, "Failed to load non-SEO pages"));
      setRows([]);
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
      <div style={styles.widget} data-testid="seo-audit-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetLoading}>Loading SEO audit...</div>
      </div>
    );
  }
  if (error) {
    return (
      <div style={styles.widget} data-testid="seo-audit-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetError} data-testid="seo-audit-error">
          {error}
        </div>
      </div>
    );
  }
  if (rows.length === 0) {
    return (
      <div style={styles.widget} data-testid="seo-audit-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetContent} data-testid="seo-audit-empty">
          No non-SEO pages found for the default path and workflow.
        </div>
      </div>
    );
  }

  return (
    <div style={styles.widget} data-testid="seo-audit-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      <div style={styles.widgetContent} data-testid="seo-audit-list">
        <div style={{ fontSize: "0.85em", color: "#666", marginBottom: "8px" }}>
          {rows.length} page{rows.length === 1 ? "" : "s"} with SEO issues
        </div>
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {rows.slice(0, 15).map((row) => (
            <li
              key={row.path}
              style={{
                padding: "8px 0",
                borderBottom: "1px solid #eee",
                fontSize: "0.9em",
              }}
            >
              <div style={{ fontWeight: 500 }}>
                {row.pageName || row.path}
              </div>
              <div style={{ fontSize: "0.8em", color: "#666" }}>{row.path}</div>
              <div style={{ fontSize: "0.8em", color: "#c62828" }}>
                Severity {row.severity}
                {row.issues.length > 0
                  ? ` · ${row.issues.slice(0, 3).join(", ")}`
                  : ""}
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default SEOAuditWidget;
