/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useCallback, useEffect, useState } from "react";
import { isSessionRedirectError } from "../api/client";
import { put } from "../api/client";
import { PATHS } from "../api/paths";
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
 * Classic **Siteimprove** gadget — view/store token + site config status.
 */
export const SiteimproveWidget: React.FC<SiteimproveWidgetProps> = ({
  title = "Siteimprove",
  refreshInterval = 120000,
}) => {
  const [status, setStatus] = useState<SiteimproveStatus | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [token, setToken] = useState("");
  const [siteName, setSiteName] = useState("");

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const s = await fetchSiteimproveStatus();
      setStatus(s);
      if (s.siteName) setSiteName(s.siteName);
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

  const onSaveToken = async () => {
    if (!token.trim() || !siteName.trim()) {
      setError("Site name and token are required.");
      return;
    }
    try {
      setBusy(true);
      setError(null);
      setMessage(null);
      // PSSiteImproveCredentials — Jackson may accept flat or rooted body
      await put(PATHS.SITEIMPROVE_TOKEN, {
        SiteimproveCredentials: {
          token: token.trim(),
          siteName: siteName.trim(),
        },
        token: token.trim(),
        siteName: siteName.trim(),
      });
      setMessage("Token saved (validated by server).");
      setToken("");
      await load();
    } catch (err: unknown) {
      if (!isSessionRedirectError(err)) {
        setError(formatApiError(err, "Failed to store Siteimprove token"));
      }
    } finally {
      setBusy(false);
    }
  };

  if (isLoading) {
    return (
      <div style={styles.widget} data-testid="siteimprove-widget">
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading}>Loading Siteimprove...</div>
      </div>
    );
  }

  return (
    <div style={styles.widget} data-testid="siteimprove-widget">
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent} data-testid="siteimprove-status">
        {error ? (
          <div style={{ ...styles.widgetError, marginBottom: 8 }}>{error}</div>
        ) : null}
        {message ? (
          <div
            style={{
              background: "#e8f5e9",
              color: "#2e7d32",
              padding: 8,
              borderRadius: 4,
              marginBottom: 8,
              fontSize: "0.85em",
            }}
          >
            {message}
          </div>
        ) : null}
        <div style={{ marginBottom: "8px" }}>
          <strong>Token on server:</strong>{" "}
          {status?.hasToken ? (
            <span style={{ color: "#28a745" }}>
              Configured
              {status?.tokenPreview ? ` (${status.tokenPreview})` : ""}
            </span>
          ) : (
            <span style={{ color: "#dc3545" }}>Not configured</span>
          )}
        </div>
        <div style={{ fontSize: "0.9em", marginBottom: 8 }}>
          <strong>Publish config for {status?.siteName || siteName || "—"}:</strong>{" "}
          {status?.siteConfigPresent ? (
            <span style={{ color: "#28a745" }}>Present</span>
          ) : (
            <span style={{ color: "#999" }}>None</span>
          )}
        </div>
        <div style={{ borderTop: "1px solid #eee", paddingTop: 10, marginTop: 8 }}>
          <div style={{ fontWeight: 600, fontSize: "0.85em", marginBottom: 6 }}>
            Store credentials
          </div>
          <label style={{ display: "block", fontSize: "0.8em", marginBottom: 6 }}>
            Site name
            <input
              value={siteName}
              onChange={(e) => setSiteName(e.target.value)}
              disabled={busy}
              data-testid="siteimprove-site"
              style={{ display: "block", width: "100%", marginTop: 4, padding: 6, boxSizing: "border-box" }}
            />
          </label>
          <label style={{ display: "block", fontSize: "0.8em", marginBottom: 6 }}>
            API token
            <input
              type="password"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              disabled={busy}
              data-testid="siteimprove-token"
              autoComplete="off"
              style={{ display: "block", width: "100%", marginTop: 4, padding: 6, boxSizing: "border-box" }}
            />
          </label>
          <button
            type="button"
            disabled={busy}
            onClick={() => void onSaveToken()}
            data-testid="siteimprove-save"
            style={{ padding: "6px 12px" }}
          >
            {busy ? "Saving…" : "Validate & save token"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default SiteimproveWidget;
