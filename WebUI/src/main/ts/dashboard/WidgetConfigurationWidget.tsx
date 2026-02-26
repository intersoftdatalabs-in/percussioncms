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

interface Widget {
  id: string;
  name: string;
  category?: string;
  active?: boolean;
}

interface DashboardConfig {
  activeWidgets?: number;
  totalWidgets?: number;
  widgets?: Widget[];
  categories?: string[];
  lastSaved?: string;
}

interface DashboardConfigData {
  dashboard?: DashboardConfig;
  data?: DashboardConfig;
  config?: DashboardConfig;
  [key: string]: unknown;
}

export interface WidgetConfigurationWidgetProps {
  title?: string;
  onAddWidget?: () => void;
  refreshInterval?: number;
}

/**
 * WidgetConfigurationWidget is a meta-widget for dashboard management.
 * Displays active widgets, allows widget configuration and management.
 */
export const WidgetConfigurationWidget: React.FC<WidgetConfigurationWidgetProps> = ({
  title = 'Dashboard Configuration',
  onAddWidget,
  refreshInterval,
}) => {
  const [config, setConfig] = useState<DashboardConfig | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchConfig = async () => {
      setIsLoading(true);
      try {
        const response = await get<DashboardConfigData>('/services/dashboard/config');
        let dashboardConfig: DashboardConfig | null = null;

        if (response.dashboard) {
          dashboardConfig = response.dashboard;
        } else if (response.data) {
          dashboardConfig = response.data;
        } else if (response.config) {
          dashboardConfig = response.config;
        } else if (typeof response === 'object' && ('activeWidgets' in response || 'widgets' in response)) {
          dashboardConfig = {
            activeWidgets: (response as Record<string, unknown>).activeWidgets as number | undefined,
            totalWidgets: (response as Record<string, unknown>).totalWidgets as number | undefined,
            widgets: (response as Record<string, unknown>).widgets as Widget[] | undefined,
            categories: (response as Record<string, unknown>).categories as string[] | undefined,
            lastSaved: (response as Record<string, unknown>).lastSaved as string | undefined,
          };
        }

        setConfig(dashboardConfig);
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to load dashboard configuration';
        setError(errorMessage);
      } finally {
        setIsLoading(false);
      }
    };

    fetchConfig();
    if (refreshInterval) {
      const interval = setInterval(fetchConfig, refreshInterval * 1000);
      return () => clearInterval(interval);
    }
  }, [refreshInterval]);

  if (isLoading) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading}>Loading dashboard configuration...</div>
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

  if (!config) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetContent}>No dashboard configuration available</div>
      </div>
    );
  }

  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' } as React.CSSProperties}>
          {/* Active Widgets Summary */}
          {config.activeWidgets !== undefined && config.totalWidgets !== undefined && (
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'space-between' } as React.CSSProperties}>
              <div style={{ flex: 1, padding: '8px', backgroundColor: '#f0f8ff', borderRadius: '3px' }}>
                <div style={{ fontSize: '0.75em', color: '#666', marginBottom: '2px' }}>Active Widgets</div>
                <div style={{ fontSize: '1.2em', fontWeight: '600', color: '#333' }}>
                  {config.activeWidgets}
                </div>
              </div>
              <div style={{ flex: 1, padding: '8px', backgroundColor: '#f0f0ff', borderRadius: '3px' }}>
                <div style={{ fontSize: '0.75em', color: '#666', marginBottom: '2px' }}>Available</div>
                <div style={{ fontSize: '1.2em', fontWeight: '600', color: '#333' }}>
                  {config.totalWidgets}
                </div>
              </div>
            </div>
          )}

          {/* Widget List */}
          {config.widgets && config.widgets.length > 0 && (
            <div style={{ borderTop: '1px solid #eee', paddingTop: '8px' }}>
              <div style={{ fontSize: '0.85em', fontWeight: '600', marginBottom: '6px', color: '#333' }}>
                Installed Widgets
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' } as React.CSSProperties}>
                {config.widgets.slice(0, 6).map((widget) => (
                  <div
                    key={widget.id}
                    style={{
                      fontSize: '0.75em',
                      padding: '4px 6px',
                      backgroundColor: '#f9f9f9',
                      borderRadius: '2px',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <span style={{ fontWeight: '500' }}>{widget.name}</span>
                    <span
                      style={{
                        fontSize: '0.85em',
                        padding: '2px 6px',
                        backgroundColor: widget.active ? '#d4f4dd' : '#f0f0f0',
                        color: widget.active ? '#0f5132' : '#666',
                        borderRadius: '2px',
                      }}
                    >
                      {widget.active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                ))}
                {config.widgets.length > 6 && (
                  <div
                    style={{
                      fontSize: '0.75em',
                      color: '#999',
                      fontStyle: 'italic',
                      textAlign: 'center',
                      padding: '4px',
                    }}
                  >
                    +{config.widgets.length - 6} more widgets
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Categories */}
          {config.categories && config.categories.length > 0 && (
            <div style={{ borderTop: '1px solid #eee', paddingTop: '8px' }}>
              <div style={{ fontSize: '0.85em', fontWeight: '600', marginBottom: '4px', color: '#333' }}>
                Categories
              </div>
              <div
                style={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: '6px',
                }}
              >
                {config.categories.slice(0, 8).map((category) => (
                  <span
                    key={category}
                    style={{
                      fontSize: '0.75em',
                      padding: '4px 8px',
                      backgroundColor: '#e8f4f8',
                      color: '#0f5132',
                      borderRadius: '12px',
                      fontWeight: '500',
                    }}
                  >
                    {category}
                  </span>
                ))}
                {config.categories.length > 8 && (
                  <span style={{ fontSize: '0.75em', color: '#999' }}>
                    +{config.categories.length - 8} more
                  </span>
                )}
              </div>
            </div>
          )}

          {/* Add Widget Button */}
          {onAddWidget && (
            <div style={{ borderTop: '1px solid #eee', paddingTop: '8px', textAlign: 'center' }}>
              <button
                onClick={onAddWidget}
                style={{
                  padding: '6px 12px',
                  backgroundColor: '#007ea8',
                  color: '#fff',
                  border: 'none',
                  borderRadius: '3px',
                  fontSize: '0.8em',
                  fontWeight: '600',
                  cursor: 'pointer',
                }}
              >
                + Add Widget
              </button>
            </div>
          )}

          {/* Last Saved */}
          {config.lastSaved && (
            <div style={{ fontSize: '0.7em', color: '#999', marginTop: '4px', paddingTop: '8px', borderTop: '1px solid #eee' }}>
              Last saved: {config.lastSaved}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
