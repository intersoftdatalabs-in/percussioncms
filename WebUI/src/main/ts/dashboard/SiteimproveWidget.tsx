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
  fetchSiteimproveStatus,
  type SiteimproveStatus,
} from "../api/dashboard/deliveryGadgetsApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";

export interface SiteimproveWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **Siteimprove** gadget — integration status (token + site config).
 *
 * <p>Real APIs under {@code /services/integrations/siteimprove/*}.
 * Invented {@code /services/siteimprove/metrics} does not exist.</p>
 */
export const SiteimproveWidget: React.FC<SiteimproveWidgetProps> = ({
  title = "Siteimprove",
  refreshInterval = 120000,
}) => {
  const [status, setStatus] = useState<SiteimproveStatus | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      setStatus(await fetchSiteimproveStatus());
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) return;
      setError(formatApiError(err, "Failed to load Siteimprove status"));
      setStatus(null);
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
      <div style={styles.widget} data-testid="siteimprove-widget">
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading}>Loading Siteimprove...</div>
      </div>
    );
  }
  if (error) {
    return (
      <div style={styles.widget} data-testid="siteimprove-widget">
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetError} data-testid="siteimprove-error">
          {error}
        </div>
      </div>
    );
  }

  const ok = status?.hasToken;
  return (
    <div style={styles.widget} data-testid="siteimprove-widget">
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent} data-testid="siteimprove-status">
        <div style={{ marginBottom: "8px" }}>
          <strong>API token:</strong>{" "}
          {ok ? (
            <span style={{ color: "#28a745" }}>
              Configured
              {status?.tokenPreview ? ` (${status.tokenPreview})` : ""}
            </span>
          ) : (
            <span style={{ color: "#dc3545" }}>Not configured</span>
          )}
        </div>
        <div style={{ marginBottom: "8px", fontSize: "0.9em" }}>
          <strong>Site:</strong> {status?.siteName || "—"}
        </div>
        <div style={{ fontSize: "0.9em" }}>
          <strong>Publish config:</strong>{" "}
          {status?.siteConfigPresent ? (
            <span style={{ color: "#28a745" }}>Present</span>
          ) : (
            <span style={{ color: "#999" }}>None for this site</span>
          )}
        </div>
        <p style={{ fontSize: "0.8em", color: "#666", marginTop: "12px" }}>
          Live accessibility/quality scores are not exposed by a CMS metrics
          REST endpoint. Configure Siteimprove token and per-site publish
          settings in Admin / classic integration UI.
        </p>
      </div>
    </div>
  );
};

export default SiteimproveWidget;
