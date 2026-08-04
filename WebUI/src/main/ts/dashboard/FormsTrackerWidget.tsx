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
  fetchFormsForDefaultSite,
  fetchFormsForSite,
  type FormSummaryRow,
} from "../api/dashboard/gadgetApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface FormsTrackerWidgetProps {
  title?: string;
  /** Site name; when omitted, first site is used. */
  site?: string;
  refreshInterval?: number;
}

/**
 * Classic **Forms Tracker** gadget.
 *
 * <p>Server: {@code GET /services/assetmanagement/asset/forms/{site}}.
 * Invented path {@code /services/forms/tracker} does not exist.</p>
 */
export const FormsTrackerWidget: React.FC<FormsTrackerWidgetProps> = ({
  title,
  site: siteProp,
  refreshInterval = 60000,
}) => {
  const heading = title ?? message(MSG.GADGET_FORM_TRACKER);
  const [forms, setForms] = useState<FormSummaryRow[]>([]);
  const [site, setSite] = useState<string | null>(siteProp ?? null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      if (siteProp?.trim()) {
        const list = await fetchFormsForSite(siteProp.trim());
        setSite(siteProp.trim());
        setForms(list);
      } else {
        const result = await fetchFormsForDefaultSite();
        setSite(result.site);
        setForms(result.forms);
      }
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Failed to load forms"));
      setForms([]);
    } finally {
      setIsLoading(false);
    }
  }, [siteProp]);

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
        <div style={styles.widgetLoading} data-testid="forms-tracker-loading">
          <p>Loading forms...</p>
        </div>
      );
    }
    if (error) {
      return (
        <div style={styles.widgetError} data-testid="forms-tracker-error">
          <p>Error: {error}</p>
        </div>
      );
    }
    if (!site) {
      return (
        <div style={styles.widgetContent} data-testid="forms-tracker-empty">
          <p>No sites available to load forms.</p>
        </div>
      );
    }
    if (forms.length === 0) {
      return (
        <div style={styles.widgetContent} data-testid="forms-tracker-empty">
          <p>No forms for site {site}.</p>
        </div>
      );
    }
    return (
      <div style={styles.widgetContent} data-testid="forms-tracker-list">
        <p style={{ fontSize: "0.85em", color: "#666", marginTop: 0 }}>
          Site: {site} · {forms.length} form{forms.length === 1 ? "" : "s"}
        </p>
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {forms.map((form) => (
            <li
              key={form.id ?? form.name}
              style={{
                padding: "8px 0",
                borderBottom: "1px solid #e0e0e0",
                display: "flex",
                justifyContent: "space-between",
                gap: "12px",
              }}
            >
              <div>
                <div style={{ fontWeight: 500 }}>{form.title || form.name}</div>
                <div style={{ fontSize: "0.8em", color: "#666" }}>
                  {form.state ? `State: ${form.state}` : form.name}
                </div>
              </div>
              <div style={{ textAlign: "right", fontSize: "0.85em", color: "#007ea8" }}>
                <div>
                  <strong>{form.totalSubmissions}</strong> total
                </div>
                <div style={{ color: "#666" }}>{form.newSubmissions} new</div>
              </div>
            </li>
          ))}
        </ul>
      </div>
    );
  };

  return (
    <div style={styles.widget} data-testid="forms-tracker-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      {renderContent()}
    </div>
  );
};

export default FormsTrackerWidget;
