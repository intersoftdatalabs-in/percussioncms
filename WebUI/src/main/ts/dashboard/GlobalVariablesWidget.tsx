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
  fetchGlobalVariables,
  type GlobalVariableEntry,
} from "../api/dashboard/gadgetApi";
import { formatApiError } from "../api/home/homeApi";
import { styles } from "./dashboard.styles";

export interface GlobalVariablesWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * Classic **Global Variables** gadget.
 *
 * <p>Server: {@code GET /services/metadatamanagement/metadata/percglobalvariables}.
 * Invented path {@code /services/admin/variables} does not exist.</p>
 */
export const GlobalVariablesWidget: React.FC<GlobalVariablesWidgetProps> = ({
  title = "Global Variables",
  refreshInterval = 0,
}) => {
  const [variables, setVariables] = useState<GlobalVariableEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      setVariables(await fetchGlobalVariables());
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setError(formatApiError(err, "Failed to load global variables"));
      setVariables([]);
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

  if (isLoading) {
    return (
      <div style={styles.widget} data-testid="global-variables-widget">
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading} data-testid="global-variables-loading">
          Loading variables...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.widget} data-testid="global-variables-widget">
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetError} data-testid="global-variables-error">
          {error}
        </div>
      </div>
    );
  }

  if (variables.length === 0) {
    return (
      <div style={styles.widget} data-testid="global-variables-widget">
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetContent} data-testid="global-variables-empty">
          No variables available
        </div>
      </div>
    );
  }

  return (
    <div style={styles.widget} data-testid="global-variables-widget">
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent} data-testid="global-variables-list">
        <div style={{ fontSize: "0.85em", fontWeight: 600, marginBottom: "8px" }}>
          Total Variables: <span style={{ color: "#007ea8" }}>{variables.length}</span>
        </div>
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "4px",
            maxHeight: "300px",
            overflowY: "auto",
          }}
        >
          {variables.slice(0, 20).map((variable) => (
            <div
              key={variable.name}
              style={{
                fontSize: "0.75em",
                padding: "6px",
                backgroundColor: "#f9f9f9",
                borderLeft: "2px solid #007ea8",
                borderRadius: "2px",
              }}
            >
              <div style={{ fontWeight: 600, color: "#333", marginBottom: "2px" }}>
                {variable.name}
              </div>
              <div
                style={{
                  color: "#666",
                  wordBreak: "break-word",
                }}
              >
                {variable.value.length > 120
                  ? `${variable.value.slice(0, 120)}…`
                  : variable.value}
              </div>
            </div>
          ))}
          {variables.length > 20 ? (
            <div style={{ fontSize: "0.75em", color: "#999", textAlign: "center" }}>
              +{variables.length - 20} more variables
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
};

export default GlobalVariablesWidget;
