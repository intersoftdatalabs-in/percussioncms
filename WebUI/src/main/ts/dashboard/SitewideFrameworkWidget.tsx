/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useCallback, useEffect, useState } from "react";
import { isSessionRedirectError } from "../api/client";
import {
  fetchThemeSummaries,
  type ThemeSummary,
} from "../api/dashboard/shellGadgetsApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface SitewideFrameworkWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **Sitewide Framework** gadget — theme summaries from Design APIs.
 * {@code GET /services/pagemanagement/theme/summary/all}
 */
export const SitewideFrameworkWidget: React.FC<SitewideFrameworkWidgetProps> = ({
  title,
  refreshInterval = 0,
}) => {
  const heading = title ?? message(MSG.GADGET_SITEWIDE_FRAMEWORK);
  const [themes, setThemes] = useState<ThemeSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      setThemes(await fetchThemeSummaries());
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) return;
      setError(formatApiError(err, "Failed to load themes"));
      setThemes([]);
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
      <div style={styles.widget} data-testid="sitewide-framework-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetLoading}>Loading themes…</div>
      </div>
    );
  }
  if (error) {
    return (
      <div style={styles.widget} data-testid="sitewide-framework-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetError}>{error}</div>
      </div>
    );
  }

  return (
    <div style={styles.widget} data-testid="sitewide-framework-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      <div style={styles.widgetContent} data-testid="sitewide-framework-list">
        <p style={{ fontSize: "0.85em", color: "#666", marginTop: 0 }}>
          Installed themes ({themes.length}). Open Design to edit CSS / regions.
        </p>
        {themes.length === 0 ? (
          <p data-testid="sitewide-framework-empty">No themes found.</p>
        ) : (
          <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
            {themes.map((t) => (
              <li
                key={t.name}
                style={{
                  display: "flex",
                  gap: 10,
                  padding: "8px 0",
                  borderBottom: "1px solid #eee",
                  alignItems: "center",
                }}
              >
                {t.thumbUrl ? (
                  <img
                    src={t.thumbUrl}
                    alt=""
                    width={40}
                    height={40}
                    style={{ objectFit: "cover", borderRadius: 4 }}
                  />
                ) : (
                  <div
                    style={{
                      width: 40,
                      height: 40,
                      background: "#e0e0e0",
                      borderRadius: 4,
                    }}
                  />
                )}
                <div>
                  <div style={{ fontWeight: 600, fontSize: "0.9em" }}>{t.name}</div>
                  {t.cssFilePath ? (
                    <div style={{ fontSize: "0.75em", color: "#666" }}>
                      {t.cssFilePath}
                    </div>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
        )}
        <p style={{ fontSize: "0.8em", marginTop: 12 }}>
          <a href="/cm/app/?view=design" style={{ color: "#007ea8" }}>
            Open Design
          </a>
        </p>
      </div>
    </div>
  );
};

export default SitewideFrameworkWidget;
