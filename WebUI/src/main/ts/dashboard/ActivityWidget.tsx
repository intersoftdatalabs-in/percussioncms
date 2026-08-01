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
  fetchContentActivity,
  resolveDefaultActivityPath,
  type ActivityDurationType,
  type ContentActivityRow,
} from "../api/dashboard/gadgetApi";
import { formatApiError } from "../api/home/homeApi";
import { message, MSG } from "../i18n/message";
import { styles } from "./dashboard.styles";

export interface ActivityWidgetProps {
  title?: string;
  /** CMS path (default: first site or /Sites/). */
  path?: string;
  durationType?: ActivityDurationType;
  /** Duration length (default 30). */
  duration?: number;
  refreshInterval?: number;
}

/**
 * Classic **Activity** gadget — content activity metrics by path/duration.
 *
 * <p>Server: {@code POST /services/activitymanagement/activity/contentactivity}
 * with {@code ContentActivityRequest}. Not a user-action timeline.</p>
 */
export const ActivityWidget: React.FC<ActivityWidgetProps> = ({
  title,
  path: pathProp,
  durationType = "days",
  duration = 30,
  refreshInterval = 60000,
}) => {
  const [rows, setRows] = useState<ContentActivityRow[]>([]);
  const [resolvedPath, setResolvedPath] = useState<string>(pathProp ?? "");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const path =
        pathProp?.trim() || (await resolveDefaultActivityPath());
      setResolvedPath(path);
      const data = await fetchContentActivity(path, durationType, duration);
      setRows(data);
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Failed to load content activity"));
      setRows([]);
    } finally {
      setIsLoading(false);
    }
  }, [pathProp, durationType, duration]);

  useEffect(() => {
    void load();
    if (refreshInterval <= 0) {
      return;
    }
    const id = window.setInterval(() => void load(), refreshInterval);
    return () => window.clearInterval(id);
  }, [load, refreshInterval]);

  const heading = title ?? message(MSG.GADGET_ACTIVITY);

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading} data-testid="activity-widget-loading">
          <p>{message(MSG.ACTIVITY_LOADING)}</p>
        </div>
      );
    }

    if (error) {
      return (
        <div style={styles.widgetError} data-testid="activity-widget-error">
          <p>{message(MSG.ERROR_GENERIC)}: {error}</p>
        </div>
      );
    }

    if (rows.length === 0) {
      return (
        <div style={styles.widgetContent} data-testid="activity-widget-empty">
          <p>{message(MSG.ACTIVITY_EMPTY)}</p>
          {resolvedPath ? (
            <p style={{ fontSize: "0.85em", color: "#666" }}>
              {message(MSG.ACTIVITY_PATH)}: {resolvedPath} · last {duration} {durationType}
            </p>
          ) : null}
        </div>
      );
    }

    return (
      <div style={styles.widgetContent} data-testid="activity-widget-list">
        <p style={{ fontSize: "0.85em", color: "#666", marginTop: 0 }}>
          {resolvedPath} · last {duration} {durationType}
        </p>
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {rows.map((row, index) => (
            <li
              key={`${row.path ?? row.name}-${index}`}
              style={{
                padding: "10px 0",
                borderBottom: "1px solid #e0e0e0",
              }}
            >
              <div style={{ fontWeight: 600, color: "#333" }}>{row.name}</div>
              {row.siteName && row.siteName !== row.name ? (
                <div style={{ fontSize: "0.8em", color: "#666" }}>
                  {message(MSG.ACTIVITY_SITE)}: {row.siteName}
                </div>
              ) : null}
              <div
                style={{
                  display: "flex",
                  flexWrap: "wrap",
                  gap: "8px 12px",
                  marginTop: "6px",
                  fontSize: "0.85em",
                  color: "#444",
                }}
              >
                <span>{message(MSG.ACTIVITY_PUBLISHED)}: {row.publishedItems}</span>
                <span>{message(MSG.ACTIVITY_PENDING)}: {row.pendingItems}</span>
                <span>{message(MSG.ACTIVITY_NEW)}: {row.newItems}</span>
                <span>{message(MSG.ACTIVITY_UPDATED)}: {row.updatedItems}</span>
                <span>{message(MSG.ACTIVITY_ARCHIVED)}: {row.archivedItems}</span>
              </div>
            </li>
          ))}
        </ul>
      </div>
    );
  };

  return (
    <div style={styles.widget} data-testid="activity-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      {renderContent()}
    </div>
  );
};

export default ActivityWidget;
