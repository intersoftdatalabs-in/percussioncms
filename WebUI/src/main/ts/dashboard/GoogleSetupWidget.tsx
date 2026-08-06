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
  deleteAnalyticsProviderConfig,
  fetchAnalyticsProfiles,
  fetchGoogleSetupSummary,
  saveAnalyticsSiteMappings,
  testAnalyticsConnection,
  type AnalyticsProfileOption,
} from "../api/dashboard/analyticsApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message as i18nMessage, MSG } from "../i18n/message";

export interface GoogleSetupWidgetProps {
  title?: string;
  refreshInterval?: number;
  /** When true, show a Clear config control (admin). Default true. */
  allowDelete?: boolean;
  /** Show credential upload + site mapping form. Default true. */
  allowConfigure?: boolean;
}

/**
 * Classic **Google Setup** gadget — status, service-account key upload,
 * and site → GA profile mapping for Traffic / What's Working.
 */
export const GoogleSetupWidget: React.FC<GoogleSetupWidgetProps> = ({
  title,
  refreshInterval = 0,
  allowDelete = true,
  allowConfigure = true,
}) => {
  const heading = title ?? i18nMessage(MSG.GADGET_GOOGLE_SETUP);
  const [summary, setSummary] = useState<
    Awaited<ReturnType<typeof fetchGoogleSetupSummary>> | null
  >(null);
  const [profiles, setProfiles] = useState<AnalyticsProfileOption[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [uid, setUid] = useState("");
  const [keyFile, setKeyFile] = useState<File | null>(null);
  /** siteName → profile key */
  const [mappings, setMappings] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const s = await fetchGoogleSetupSummary();
      setSummary(s);
      if (s.provider.userId) {
        setUid(s.provider.userId);
      }
      const nextMaps: Record<string, string> = {};
      for (const p of s.provider.siteProfiles) {
        if (p.mapped && p.rawValue) {
          nextMaps[p.siteName] = p.rawValue;
        }
      }
      for (const site of s.sites) {
        if (site.mapping?.rawValue && !nextMaps[site.siteName]) {
          nextMaps[site.siteName] = site.mapping.rawValue;
        }
      }
      setMappings(nextMaps);

      if (s.provider.configured) {
        try {
          setProfiles(await fetchAnalyticsProfiles());
        } catch {
          setProfiles([]);
        }
      } else {
        setProfiles([]);
      }
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

  const onUploadAndTest = async () => {
    if (!uid.trim()) {
      setError("Enter the service account email (user id).");
      return;
    }
    if (!keyFile) {
      setError("Choose a Google service account JSON key file.");
      return;
    }
    try {
      setBusy(true);
      setError(null);
      setMessage(null);
      await testAnalyticsConnection(uid.trim(), keyFile, keyFile.name);
      setMessage("Connection OK — credentials stored.");
      setKeyFile(null);
      await load();
    } catch (err: unknown) {
      if (!isSessionRedirectError(err)) {
        setError(formatApiError(err, "Test connection failed"));
      }
    } finally {
      setBusy(false);
    }
  };

  const onSaveMappings = async () => {
    try {
      setBusy(true);
      setError(null);
      setMessage(null);
      await saveAnalyticsSiteMappings(mappings);
      setMessage("Site profile mappings saved.");
      await load();
    } catch (err: unknown) {
      if (!isSessionRedirectError(err)) {
        setError(formatApiError(err, "Failed to save site mappings"));
      }
    } finally {
      setBusy(false);
    }
  };

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
      setError(null);
      setMessage(null);
      await deleteAnalyticsProviderConfig();
      setMappings({});
      setProfiles([]);
      setMessage("Analytics configuration cleared.");
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
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetLoading} data-testid="google-setup-loading">
          Loading Google setup data...
        </div>
      </div>
    );
  }

  const provider = summary?.provider;
  const configured = Boolean(provider?.configured);
  const sites = summary?.sites ?? [];
  const readySites = sites.filter((s) => s.profileConfigured).length;

  const fieldStyle: React.CSSProperties = {
    display: "flex",
    flexDirection: "column",
    gap: 4,
    marginBottom: 10,
    fontSize: "0.85em",
  };
  const inputStyle: React.CSSProperties = {
    padding: "6px 8px",
    border: "1px solid #ccc",
    borderRadius: 4,
  };
  const btnStyle: React.CSSProperties = {
    padding: "6px 12px",
    fontSize: "0.85em",
    cursor: busy ? "default" : "pointer",
    marginRight: 8,
    marginTop: 4,
  };

  return (
    <div style={styles.widget} data-testid="google-setup-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      <div style={styles.widgetContent} data-testid="google-setup-content">
        {error ? (
          <div
            style={{ ...styles.widgetError, marginBottom: 12 }}
            data-testid="google-setup-error"
          >
            {error}
          </div>
        ) : null}
        {message ? (
          <div
            style={{
              background: "#e8f5e9",
              color: "#2e7d32",
              padding: 8,
              borderRadius: 4,
              marginBottom: 12,
              fontSize: "0.85em",
            }}
            data-testid="google-setup-message"
          >
            {message}
          </div>
        ) : null}

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
            Traffic and What&apos;s Working use this Google Analytics setup.
            Upload a service account JSON key, then map each site to a GA
            profile/view.
          </div>

          {allowConfigure ? (
            <div
              data-testid="google-setup-configure"
              style={{
                borderTop: "1px solid #eee",
                paddingTop: 12,
              }}
            >
              <div style={{ fontWeight: 600, fontSize: "0.9em", marginBottom: 8 }}>
                Credentials
              </div>
              <label style={fieldStyle}>
                Service account email
                <input
                  type="email"
                  value={uid}
                  onChange={(e) => setUid(e.target.value)}
                  disabled={busy}
                  style={inputStyle}
                  data-testid="google-setup-uid"
                  autoComplete="off"
                />
              </label>
              <label style={fieldStyle}>
                JSON key file
                <input
                  type="file"
                  accept=".json,application/json"
                  disabled={busy}
                  data-testid="google-setup-keyfile"
                  onChange={(e) => {
                    const f = e.target.files?.[0] ?? null;
                    setKeyFile(f);
                  }}
                />
              </label>
              <button
                type="button"
                disabled={busy}
                onClick={() => void onUploadAndTest()}
                style={btnStyle}
                data-testid="google-setup-test"
              >
                {busy ? "Working…" : "Upload & test connection"}
              </button>
            </div>
          ) : null}

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
              {allowConfigure && configured ? (
                <div style={{ marginBottom: 8 }}>
                  {sites.map((s) => (
                    <label
                      key={s.siteName}
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: 4,
                        fontSize: "0.8em",
                        marginBottom: 8,
                      }}
                    >
                      {s.siteName}
                      <select
                        value={mappings[s.siteName] ?? ""}
                        disabled={busy}
                        data-testid={`google-setup-map-${s.siteName}`}
                        onChange={(e) =>
                          setMappings((m) => ({
                            ...m,
                            [s.siteName]: e.target.value,
                          }))
                        }
                        style={inputStyle}
                      >
                        <option value="">— No profile —</option>
                        {profiles.map((p) => (
                          <option key={p.key} value={p.key}>
                            {p.label}
                          </option>
                        ))}
                        {/* Keep current mapping if not in profile list */}
                        {mappings[s.siteName] &&
                        !profiles.some((p) => p.key === mappings[s.siteName]) ? (
                          <option value={mappings[s.siteName]}>
                            {mappings[s.siteName]} (saved)
                          </option>
                        ) : null}
                      </select>
                    </label>
                  ))}
                  <button
                    type="button"
                    disabled={busy || profiles.length === 0}
                    onClick={() => void onSaveMappings()}
                    style={btnStyle}
                    data-testid="google-setup-save-maps"
                  >
                    Save site mappings
                  </button>
                  {profiles.length === 0 && configured ? (
                    <span style={{ fontSize: "0.75em", color: "#666" }}>
                      Load profiles failed — re-upload key or check GA access.
                    </span>
                  ) : null}
                </div>
              ) : (
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
              )}
            </div>
          ) : (
            <div style={{ fontSize: "0.8em", color: "#666" }}>
              No sites found to check profile mappings.
            </div>
          )}

          {allowDelete && configured ? (
            <button
              type="button"
              disabled={busy}
              onClick={() => void onClear()}
              data-testid="google-setup-clear"
              style={{ ...btnStyle, color: "#c62828" }}
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
