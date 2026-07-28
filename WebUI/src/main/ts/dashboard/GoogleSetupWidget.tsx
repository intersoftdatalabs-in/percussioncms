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
  deleteAnalyticsProviderConfig,
  fetchGoogleSetupSummary,
} from "../api/dashboard/analyticsApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";

export interface GoogleSetupWidgetProps {
  title?: string;
  refreshInterval?: number;
  /** When true, show a Clear config control (admin). */
  allowDelete?: boolean;
}

/**
 * Classic **Google Setup** gadget — analytics provider status for GA-backed
 * gadgets (Traffic, What's Working / effectiveness).
 *
 * <p>Server:
 * {@code GET /services/analytics/provider/config},
 * {@code GET …/isProfileConfigured/{site}}.
 * Invented path {@code /services/google/setup} does not exist.</p>
 *
 * <p>Full credential upload (JSON key multipart) remains a follow-up;
 * this wave shows status and site profile readiness so operators know why
 * Traffic / Effectiveness fail.</p>
 */
export const GoogleSetupWidget: React.FC<GoogleSetupWidgetProps> = ({
  title = "Google Setup",
  refreshInterval = 0,
  allowDelete = false,
}) => {
  const [summary, setSummary] = useState<
    Awaited<ReturnType<typeof fetchGoogleSetupSummary>> | null
  >(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      setSummary(await fetchGoogleSetupSummary());
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Failed to load Google Analytics setup"));
      setSummary(null);
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

  const onClear = async () => {
    if (
      !window.confirm(
        "Remove stored Google Analytics credentials and site profile mappings?",
      )
    ) {
      return;
    }
    try {
      setBusy(true);
      await deleteAnalyticsProviderConfig();
      await load();
    } catch (err: unknown) {
      if (!isSessionRedirectError(err)) {
        setError(formatApiError(err, "Failed to clear analytics config"));
      }
    } finally {
      setBusy(false);
    }
  };

  if (isLoading) {
    return (
      <div style={styles.widget} data-testid="google-setup-widget">
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading} data-testid="google-setup-loading">
          Loading Google setup data...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.widget} data-testid="google-setup-widget">
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetError} data-testid="google-setup-error">
          {error}
        </div>
      </div>
    );
  }

  const provider = summary?.provider;
  const configured = Boolean(provider?.configured);
  const sites = summary?.sites ?? [];
  const readySites = sites.filter((s) => s.profileConfigured).length;

  return (
    <div style={styles.widget} data-testid="google-setup-widget">
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent} data-testid="google-setup-content">
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          <div
            style={{ display: "flex", alignItems: "center", gap: "8px" }}
            data-testid="google-setup-account"
          >
            <div
              style={{
                fontSize: "1.2em",
                fontWeight: 700,
                color: configured ? "#28a745" : "#dc3545",
              }}
            >
              {configured ? "✓" : "✕"}
            </div>
            <div>
              <div style={{ fontSize: "0.85em", fontWeight: 600, color: "#333" }}>
                {configured
                  ? "Analytics provider configured"
                  : "Analytics provider not configured"}
              </div>
              {provider?.userId ? (
                <div
                  style={{ fontSize: "0.75em", color: "#666" }}
                  data-testid="google-setup-userid"
                >
                  Account: {provider.userId}
                </div>
              ) : null}
            </div>
          </div>

          <div
            style={{
              fontSize: "0.8em",
              color: "#555",
              padding: "8px",
              backgroundColor: "#f5f9fc",
              borderRadius: "4px",
              borderLeft: "3px solid #007ea8",
            }}
          >
            Traffic and What&apos;s Working (effectiveness) use this Google
            Analytics configuration. Without credentials and a site profile
            mapping, those gadgets cannot load data.
          </div>

          {sites.length > 0 ? (
            <div data-testid="google-setup-sites">
              <div
                style={{
                  fontSize: "0.85em",
                  fontWeight: 600,
                  marginBottom: "6px",
                  color: "#333",
                }}
              >
                Site profiles ({readySites}/{sites.length} ready)
              </div>
              <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                {sites.map((s) => (
                  <li
                    key={s.siteName}
                    style={{
                      fontSize: "0.8em",
                      display: "flex",
                      justifyContent: "space-between",
                      gap: "8px",
                      padding: "4px 0",
                      borderBottom: "1px solid #eee",
                    }}
                  >
                    <span>{s.siteName}</span>
                    <span
                      style={{
                        color: s.profileConfigured ? "#28a745" : "#999",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {s.profileConfigured
                        ? s.mapping?.webPropertyId
                          ? `Ready · ${s.mapping.webPropertyId}`
                          : "Ready"
                        : "No profile"}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <div style={{ fontSize: "0.8em", color: "#666" }}>
              No sites found to check profile mappings.
            </div>
          )}

          {!configured ? (
            <div
              style={{ fontSize: "0.8em", color: "#666" }}
              data-testid="google-setup-hint"
            >
              Configure a Google service account and map each site to a GA
              profile (classic Google Setup or Admin). Credential upload from
              this React gadget is a follow-up.
            </div>
          ) : null}

          {allowDelete && configured ? (
            <button
              type="button"
              disabled={busy}
              onClick={() => void onClear()}
              data-testid="google-setup-clear"
              style={{
                alignSelf: "flex-start",
                padding: "6px 12px",
                fontSize: "0.85em",
                cursor: busy ? "default" : "pointer",
              }}
            >
              {busy ? "Clearing…" : "Clear analytics config"}
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
};

export default GoogleSetupWidget;
