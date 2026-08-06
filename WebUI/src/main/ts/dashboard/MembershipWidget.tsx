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
  fetchDefaultMembershipUsers,
  type MembershipUser,
} from "../api/dashboard/deliveryGadgetsApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";
import { message, MSG } from "../i18n/message";

export interface MembershipWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **Membership** gadget (deprecated registry group).
 *
 * <p>{@code GET /services/delivery/membership/admin/users/{site}}.
 * Invented {@code /services/membership/list} does not exist.</p>
 */
export const MembershipWidget: React.FC<MembershipWidgetProps> = ({
  title,
  refreshInterval = 60000,
}) => {
  const heading = title ?? message(MSG.GADGET_MEMBERSHIP);
  const [site, setSite] = useState<string | null>(null);
  const [users, setUsers] = useState<MembershipUser[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const result = await fetchDefaultMembershipUsers();
      setSite(result.site);
      setUsers(result.users);
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) return;
      setError(
        formatApiError(
          err,
          "Failed to load membership (delivery membership service may be offline)",
        ),
      );
      setUsers([]);
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
      <div style={styles.widget} data-testid="membership-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetLoading}>Loading membership...</div>
      </div>
    );
  }
  if (error) {
    return (
      <div style={styles.widget} data-testid="membership-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetError} data-testid="membership-error">
          {error}
        </div>
      </div>
    );
  }
  if (!site) {
    return (
      <div style={styles.widget} data-testid="membership-widget">
        <div style={styles.widgetTitle}>{heading}</div>
        <div style={styles.widgetContent}>No sites available.</div>
      </div>
    );
  }

  return (
    <div style={styles.widget} data-testid="membership-widget">
      <div style={styles.widgetTitle}>{heading}</div>
      <div style={styles.widgetContent} data-testid="membership-list">
        <div style={{ fontSize: "0.85em", color: "#666", marginBottom: "8px" }}>
          Site: {site} · {users.length} member{users.length === 1 ? "" : "s"}
        </div>
        {users.length === 0 ? (
          <p data-testid="membership-empty">No members for this site.</p>
        ) : (
          <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
            {users.slice(0, 20).map((u) => (
              <li
                key={u.email}
                style={{
                  padding: "6px 0",
                  borderBottom: "1px solid #eee",
                  fontSize: "0.9em",
                }}
              >
                <div style={{ fontWeight: 500 }}>{u.email}</div>
                <div style={{ fontSize: "0.8em", color: "#666" }}>
                  {[u.status, u.groups].filter(Boolean).join(" · ") || "—"}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
};

export default MembershipWidget;
