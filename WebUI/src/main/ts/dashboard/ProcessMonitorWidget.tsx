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

interface Monitor {
  designator: string;
  name: string;
  status?: string;
  message?: string;
  [key: string]: unknown;
}

interface MonitorList {
  monitors?: Monitor[];
  monitor?: Monitor[];
  [key: string]: unknown;
}

export interface ProcessMonitorWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * ProcessMonitorWidget displays the status of system background processes and monitors.
 * Fetches data from the monitor REST endpoint and auto-refreshes.
 */
export const ProcessMonitorWidget: React.FC<ProcessMonitorWidgetProps> = ({
  title = 'Process Monitor',
  refreshInterval = 30000,
}) => {
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchMonitors = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Fetch all monitors from the monitor service
        const response = await get<MonitorList>('/services/monitor/all');

        // Handle both possible response structures
        let monitorArray: Monitor[] = [];
        if (response.monitors && Array.isArray(response.monitors)) {
          monitorArray = response.monitors;
        } else if (response.monitor && Array.isArray(response.monitor)) {
          monitorArray = response.monitor;
        } else if (Array.isArray(response)) {
          monitorArray = response as Monitor[];
        }

        setMonitors(monitorArray);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load process monitor';
        setError(errorMessage);
        console.error('ProcessMonitorWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchMonitors();

    // Set up refresh interval if specified
    const interval =
      refreshInterval > 0
        ? setInterval(fetchMonitors, refreshInterval)
        : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval]);

  const getStatusIcon = (monitor: Monitor): string => {
    const status = monitor.status?.toString().toLowerCase();
    if (status === 'running' || status === 'active' || status === 'ok') {
      return '✅';
    }
    if (status === 'paused' || status === 'idle') {
      return '⏸️';
    }
    if (status === 'error' || status === 'failed') {
      return '❌';
    }
    return '📊';
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading process monitor...</p>
        </div>
      );
    }

    if (error) {
      return (
        <div style={styles.widgetError}>
          <p>Error: {error}</p>
        </div>
      );
    }

    if (!monitors || monitors.length === 0) {
      return (
        <div style={styles.widgetContent}>
          <p>No monitors available</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <div style={{ listStyle: 'none', padding: 0, margin: 0 } as React.CSSProperties}>
          {monitors.map((monitor, index) => (
            <div
              key={monitor.designator || index}
              style={{
                padding: '10px 0',
                borderBottom: '1px solid #e0e0e0',
                display: 'flex',
                gap: '12px',
                alignItems: 'center',
              } as React.CSSProperties}
            >
              <div style={{ fontSize: '1.2em', minWidth: '28px', textAlign: 'center' }}>
                {getStatusIcon(monitor)}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontWeight: 500, color: '#333', fontSize: '0.95em' }}>
                  {monitor.name || monitor.designator || `Monitor ${index + 1}`}
                </div>
                {monitor.message && (
                  <div
                    style={{
                      fontSize: '0.8em',
                      color: '#666',
                      marginTop: '2px',
                    } as React.CSSProperties}
                  >
                    {monitor.message}
                  </div>
                )}
              </div>
              {monitor.status && (
                <div
                  style={{
                    fontSize: '0.8em',
                    padding: '4px 8px',
                    borderRadius: '4px',
                    backgroundColor: '#f0f0f0',
                    color: '#555',
                    whiteSpace: 'nowrap',
                  } as React.CSSProperties}
                >
                  {monitor.status}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{title}</div>
      {renderContent()}
    </div>
  );
};

export default ProcessMonitorWidget;
