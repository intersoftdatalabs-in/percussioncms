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
  fetchCookieConsentTotals,
  type CookieConsentTotals,
} from "../api/dashboard/deliveryGadgetsApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface CookieConsentWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **Cookie Consent** gadget — consent entry totals (DTS-backed).
 *
 * <p>{@code GET /services/delivery/consent/log/totals}.
 * Invented {@code /services/compliance/cookie-consent} does not exist.</p>
 */
export const CookieConsentWidget: React.FC<CookieConsentWidgetProps> = ({
  title,
  refreshInterval = 60000,
}) => {
  const heading = title ?? message(MSG.GADGET_COOKIE_CONSENT);
  const [totals, setTotals] = useState<CookieConsentTotals | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      setTotals(await fetchCookieConsentTotals());
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) return;
      setError(
        formatApiError(
          err,
          "Failed to load cookie consent totals (delivery tier may be offline)",
        ),
      );
      setTotals(null);
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
      <div style={styles.widget} data-testid="cookie-consent-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetLoading}>Loading cookie consent...</div>
      </div>
    );
  }
  if (error) {
    return (
      <div style={styles.widget} data-testid="cookie-consent-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetError} data-testid="cookie-consent-error">
          {error}
        </div>
      </div>
    );
  }
  if (!totals || (totals.bySite.length === 0 && totals.grandTotal === 0)) {
    return (
      <div style={styles.widget} data-testid="cookie-consent-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetContent} data-testid="cookie-consent-empty">
          No cookie consent log entries found.
        </div>
      </div>
    );
  }

  return (
    <div style={styles.widget} data-testid="cookie-consent-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      <div style={styles.widgetContent} data-testid="cookie-consent-list">
        <div style={{ fontWeight: 600, marginBottom: "8px" }}>
          Total entries:{" "}
          <span style={{ color: "#007ea8" }}>{totals.grandTotal}</span>
        </div>
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {totals.bySite.slice(0, 15).map((row) => (
            <li
              key={row.site}
              style={{
                display: "flex",
                justifyContent: "space-between",
                padding: "6px 0",
                borderBottom: "1px solid #eee",
                fontSize: "0.9em",
              }}
            >
              <span>{row.site}</span>
              <strong style={{ color: "#007ea8" }}>{row.total}</strong>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default CookieConsentWidget;
