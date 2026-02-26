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

import React, { useEffect, useState } from 'react';
import { get } from '../api/client';
import { styles } from './dashboard.styles';

interface Variable {
  name: string;
  value: string;
  scope?: string;
  type?: string;
}

interface Variables {
  variables?: Variable[];
  items?: Variable[];
  count?: number;
  lastModified?: string;
}

interface VariablesData {
  variables?: Variables;
  data?: Variables;
  admin?: Variables;
  [key: string]: unknown;
}

export interface GlobalVariablesWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * GlobalVariablesWidget displays system-wide global variables and configuration.
 * Shows variable names, values, scopes, and modification dates.
 */
export const GlobalVariablesWidget: React.FC<GlobalVariablesWidgetProps> = ({
  title = 'Global Variables',
  refreshInterval,
}) => {
  const [variables, setVariables] = useState<Variable[]>([]);
  const [count, setCount] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchVariables = async () => {
      setIsLoading(true);
      try {
        const response = await get<VariablesData>('/services/admin/variables');
        let vars: Variable[] = [];
        let varCount = 0;

        if (response.variables) {
          vars = response.variables.variables || response.variables.items || [];
          varCount = response.variables.count || vars.length;
        } else if (response.data) {
          vars = response.data.variables || response.data.items || [];
          varCount = response.data.count || vars.length;
        } else if (response.admin) {
          vars = response.admin.variables || response.admin.items || [];
          varCount = response.admin.count || vars.length;
        } else if (Array.isArray(response.variables)) {
          vars = response.variables;
          varCount = vars.length;
        }

        setVariables(vars);
        setCount(varCount);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load variables';
        setError(errorMessage);
      } finally {
        setIsLoading(false);
      }
    };

    fetchVariables();
    if (refreshInterval) {
      const interval = setInterval(fetchVariables, refreshInterval * 1000);
      return () => clearInterval(interval);
    }
  }, [refreshInterval]);

  if (isLoading) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading}>Loading variables...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetError}>{error}</div>
      </div>
    );
  }

  if (!variables || variables.length === 0) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetContent}>No variables available</div>
      </div>
    );
  }

  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' } as React.CSSProperties}>
          {/* Count Summary */}
          <div style={{ fontSize: '0.85em', fontWeight: '600', color: '#333', marginBottom: '4px' }}>
            Total Variables: <span style={{ color: '#007ea8' }}>{count}</span>
          </div>

          {/* Variables List */}
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '4px',
              maxHeight: '300px',
              overflowY: 'auto' as React.CSSProperties['overflowY'],
            }}
          >
            {variables.slice(0, 10).map((variable, idx) => (
              <div
                key={idx}
                style={{
                  fontSize: '0.75em',
                  padding: '6px',
                  backgroundColor: '#f9f9f9',
                  borderLeft: '2px solid #007ea8',
                  borderRadius: '2px',
                }}
              >
                <div style={{ fontWeight: '600', color: '#333', marginBottom: '2px' }}>
                  {variable.name}
                </div>
                <div style={{ color: '#666', fontSize: '0.9em', wordBreak: 'break-word' }}>
                  {variable.value.substring(0, 100)}{variable.value.length > 100 ? '...' : ''}
                </div>
                {variable.scope && (
                  <div style={{ fontSize: '0.8em', color: '#999', marginTop: '2px' }}>
                    Scope: {variable.scope}
                  </div>
                )}
              </div>
            ))}
            {variables.length > 10 && (
              <div style={{ fontSize: '0.75em', color: '#999', fontStyle: 'italic', textAlign: 'center' }}>
                +{variables.length - 10} more variables
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
