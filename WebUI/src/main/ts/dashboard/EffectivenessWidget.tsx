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
import { post } from '../api/client';
import { styles } from './dashboard.styles';

interface EffectivenessMetric {
  name: string;
  value: number;
  unit?: string;
  trend?: 'up' | 'down' | 'stable';
  target?: number;
  percentage?: number;
}

interface EffectivenessData {
  metrics?: EffectivenessMetric[];
  timestamp?: string;
  period?: string;
  [key: string]: unknown;
}

export interface EffectivenessWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * EffectivenessWidget displays performance metrics and efficiency measurements.
 * Fetches data from the effectiveness REST endpoint and shows key performance indicators.
 */
export const EffectivenessWidget: React.FC<EffectivenessWidgetProps> = ({
  title = 'Effectiveness Metrics',
  refreshInterval = 60000,
}) => {
  const [metrics, setMetrics] = useState<EffectivenessMetric[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchMetrics = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Fetch effectiveness metrics from the activity service
        const response = await post<EffectivenessData>('/services/activity/effectiveness', {});

        // Handle both possible response structures
        let metricArray: EffectivenessMetric[] = [];
        if (response.metrics && Array.isArray(response.metrics)) {
          metricArray = response.metrics;
        } else if (Array.isArray(response)) {
          metricArray = response as EffectivenessMetric[];
        }

        setMetrics(metricArray);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load effectiveness metrics';
        setError(errorMessage);
        console.error('EffectivenessWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchMetrics();

    // Set up refresh interval if specified
    const interval =
      refreshInterval > 0 ? setInterval(fetchMetrics, refreshInterval) : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval]);

  const getTrendIcon = (metric: EffectivenessMetric): string => {
    const trend = metric.trend?.toLowerCase();
    if (trend === 'up') return '📈';
    if (trend === 'down') return '📉';
    return '➡️';
  };

  const getPercentageColor = (metric: EffectivenessMetric): string => {
    if (!metric.percentage && !metric.target) return '#666';
    const percentage = metric.percentage || 0;
    if (percentage >= 90) return '#4caf50'; // Green
    if (percentage >= 70) return '#ff9800'; // Orange
    return '#f44336'; // Red
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading effectiveness metrics...</p>
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

    if (!metrics || metrics.length === 0) {
      return (
        <div style={styles.widgetContent}>
          <p>No effectiveness metrics available</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' } as React.CSSProperties}>
          {metrics.map((metric, index) => (
            <div
              key={metric.name || index}
              style={{
                padding: '10px',
                borderLeft: `4px solid ${getPercentageColor(metric)}`,
                backgroundColor: '#f9f9f9',
                borderRadius: '2px',
              } as React.CSSProperties}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' } as React.CSSProperties}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 500, color: '#333', fontSize: '0.9em' }}>
                    {metric.name}
                  </div>
                  <div style={{ fontSize: '1.3em', fontWeight: '700', color: '#007ea8', marginTop: '4px' } as React.CSSProperties}>
                    {metric.value}
                    {metric.unit && <span style={{ fontSize: '0.8em', marginLeft: '4px' }}>{metric.unit}</span>}
                  </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '4px', marginLeft: '12px' } as React.CSSProperties}>
                  <div style={{ fontSize: '1.4em' }}>{getTrendIcon(metric)}</div>
                  {metric.percentage !== undefined && (
                    <div
                      style={{
                        fontSize: '0.8em',
                        fontWeight: 'bold',
                        color: getPercentageColor(metric),
                      } as React.CSSProperties}
                    >
                      {Math.round(metric.percentage)}%
                    </div>
                  )}
                  {metric.target !== undefined && (
                    <div style={{ fontSize: '0.75em', color: '#999' }}>
                      Target: {metric.target}
                    </div>
                  )}
                </div>
              </div>
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

export default EffectivenessWidget;
