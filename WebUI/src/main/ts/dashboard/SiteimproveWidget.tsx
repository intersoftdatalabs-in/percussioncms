/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
import { message as i18nMessage, MSG } from "../i18n/message";

export interface SiteimproveWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **Siteimprove** gadget — view/store token + site config status.
 */
export const SiteimproveWidget: React.FC<SiteimproveWidgetProps> = ({
  title,
  refreshInterval = 120000,
}) => {
  const heading = title ?? i18nMessage(MSG.GADGET_SITEIMPROVE);
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
      setError(
        formatApiError(
          err,
          i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.LOAD_FAILED),
        ),
      );
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
      setError(
        i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.VALIDATION_REQUIRED),
      );
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
      setMessage(i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.SAVED));
      setToken("");
      await load();
    } catch (err: unknown) {
      if (!isSessionRedirectError(err)) {
        setError(
          formatApiError(
            err,
            i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.SAVE_FAILED),
          ),
        );
      }
    } finally {
      setBusy(false);
    }
  };

  if (isLoading) {
    return (
      <div style={styles.widget} data-testid="siteimprove-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetLoading}>
          {i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.LOADING)}
        </div>
      </div>
    );
  }

  return (
    <div style={styles.widget} data-testid="siteimprove-widget">
      <div style={styles.widgetTitle}>{heading}</div>
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
          <strong>
            {i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.TOKEN_LABEL)}:
          </strong>{" "}
          {status?.hasToken ? (
            <span style={{ color: "#28a745" }}>
              {i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.CONFIGURED)}
              {status?.tokenPreview ? ` (${status.tokenPreview})` : ""}
            </span>
          ) : (
            <span style={{ color: "#dc3545" }}>
              {i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.NOT_CONFIGURED)}
            </span>
          )}
        </div>
        <div style={{ fontSize: "0.9em", marginBottom: 8 }}>
          <strong>
            {i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.PUBLISH_CONFIG_FOR)}{" "}
            {status?.siteName || siteName || "—"}:
          </strong>{" "}
          {status?.siteConfigPresent ? (
            <span style={{ color: "#28a745" }}>
              {i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.PRESENT)}
            </span>
          ) : (
            <span style={{ color: "#999" }}>
              {i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.NONE)}
            </span>
          )}
        </div>
        <div style={{ borderTop: "1px solid #eee", paddingTop: 10, marginTop: 8 }}>
          <div style={{ fontWeight: 600, fontSize: "0.85em", marginBottom: 6 }}>
            {i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.STORE_CREDENTIALS)}
          </div>
          <label style={{ display: "block", fontSize: "0.8em", marginBottom: 6 }}>
            {i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.SITE_NAME)}
            <input
              value={siteName}
              onChange={(e) => setSiteName(e.target.value)}
              disabled={busy}
              data-testid="siteimprove-site"
              style={{ display: "block", width: "100%", marginTop: 4, padding: 6, boxSizing: "border-box" }}
            />
          </label>
          <label style={{ display: "block", fontSize: "0.8em", marginBottom: 6 }}>
            {i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.API_TOKEN)}
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
            {busy
              ? i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.SAVING) + "…"
              : i18nMessage(MSG.DASHBOARD.WIDGETS.SITEIMPROVE.SAVE_TOKEN)}
          </button>
        </div>
      </div>
    </div>
  );
};

export default SiteimproveWidget;
