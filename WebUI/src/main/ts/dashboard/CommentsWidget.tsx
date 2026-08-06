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
  fetchDefaultPagesWithComments,
  type PageCommentsSummary,
} from "../api/dashboard/deliveryGadgetsApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface CommentsWidgetProps {
  title?: string;
  refreshInterval?: number;
  maxComments?: number;
}

/**
 * Classic **Comments** gadget — pages with comments (DTS-backed).
 *
 * <p>{@code GET /services/delivery/comment/pageswithcomments/{site}}.
 * Invented {@code /services/comments/latest} does not exist.</p>
 */
export const CommentsWidget: React.FC<CommentsWidgetProps> = ({
  title,
  refreshInterval = 60000,
  maxComments = 12,
}) => {
  const heading = title ?? message(MSG.GADGET_COMMENTS);
  const [site, setSite] = useState<string | null>(null);
  const [pages, setPages] = useState<PageCommentsSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const result = await fetchDefaultPagesWithComments(maxComments);
      setSite(result.site);
      setPages(result.pages.slice(0, maxComments));
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) return;
      setError(
        formatApiError(
          err,
          "Failed to load comments (delivery tier may be offline)",
        ),
      );
      setPages([]);
    } finally {
      setIsLoading(false);
    }
  }, [maxComments]);

  useEffect(() => {
    void load();
    if (refreshInterval <= 0) return;
    const id = window.setInterval(() => void load(), refreshInterval);
    return () => window.clearInterval(id);
  }, [load, refreshInterval]);

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading} data-testid="comments-widget-loading">
          <p>Loading comments...</p>
        </div>
      );
    }
    if (error) {
      return (
        <div style={styles.widgetError} data-testid="comments-widget-error">
          <p>Error: {error}</p>
        </div>
      );
    }
    if (!site) {
      return (
        <div style={styles.widgetContent} data-testid="comments-widget-empty">
          <p>No sites available.</p>
        </div>
      );
    }
    if (pages.length === 0) {
      return (
        <div style={styles.widgetContent} data-testid="comments-widget-empty">
          <p>No pages with comments for {site}.</p>
        </div>
      );
    }
    return (
      <div style={styles.widgetContent} data-testid="comments-widget-list">
        <p style={{ fontSize: "0.85em", color: "#666", marginTop: 0 }}>
          Site: {site}
        </p>
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {pages.map((p) => (
            <li
              key={p.id ?? p.path ?? p.pagePath}
              style={{
                padding: "8px 0",
                borderBottom: "1px solid #e0e0e0",
              }}
            >
              <div style={{ fontWeight: 500 }}>
                {p.pageLinkTitle || p.path || p.pagePath || "Page"}
              </div>
              <div style={{ fontSize: "0.8em", color: "#666" }}>
                {p.commentCount} comments · {p.newCount} new · {p.approvedCount}{" "}
                approved
              </div>
            </li>
          ))}
        </ul>
      </div>
    );
  };

  return (
    <div style={styles.widget} data-testid="comments-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      {renderContent()}
    </div>
  );
};

export default CommentsWidget;
