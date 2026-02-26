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

interface AccessibilityMetric {
  level?: string;
  score?: number;
  status?: string;
}

interface QualityMetric {
  level?: string;
  score?: number;
  status?: string;
}

interface SiteimproveData {
  accessibility?: AccessibilityMetric;
  quality?: QualityMetric;
  integrated?: boolean;
  accountId?: string;
  lastChecked?: string;
}

interface SiteimproveResponse {
  siteimprove?: SiteimproveData;
  data?: SiteimproveData;
  analytics?: SiteimproveData;
  [key: string]: unknown;
}

export interface SiteimproveWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * SiteimproveWidget displays accessibility and quality metrics from Siteimprove.
 * Shows accessibility levels, quality scores, and integration status.
 */
export const SiteimproveWidget: React.FC<SiteimproveWidgetProps> = ({
  title = 'Siteimprove',
  refreshInterval,
}) => {
  const [data, setData] = useState<SiteimproveData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      setIsLoading(true);
      try {
        const response = await get<SiteimproveResponse>('/services/siteimprove/metrics');
        let siteimproveData: SiteimproveData | null = null;

        if (response.siteimprove) {
          siteimproveData = response.siteimprove;
        } else if (response.data) {
          siteimproveData = response.data;
        } else if (response.analytics) {
          siteimproveData = response.analytics;
        } else if (typeof response === 'object' && ('accessibility' in response || 'quality' in response)) {
          siteimproveData = {
            accessibility: (response as Record<string, unknown>).accessibility as AccessibilityMetric | undefined,
            quality: (response as Record<string, unknown>).quality as QualityMetric | undefined,
            integrated: (response as Record<string, unknown>).integrated as boolean | undefined,
            accountId: (response as Record<string, unknown>).accountId as string | undefined,
            lastChecked: (response as Record<string, unknown>).lastChecked as string | undefined,
          };
        }

        setData(siteimproveData);
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to load Siteimprove data';
        setError(errorMessage);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
    if (refreshInterval) {
      const interval = setInterval(fetchData, refreshInterval * 1000);
      return () => clearInterval(interval);
    }
  }, [refreshInterval]);

  if (isLoading) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading}>Loading Siteimprove data...</div>
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

  if (!data) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetContent}>No Siteimprove data available</div>
      </div>
    );
  }

  const getAccessibilityColor = (level?: string): string => {
    if (!level) return '#999';
    const normalized = level.toUpperCase();
    return { 'AAA': '#107c10', 'AA': '#0f7938', 'A': '#f7630c', 'FAIL': '#d13438' }[normalized] || '#999';
  };

  const getQualityColor = (score?: number): string => {
    if (score === undefined) return '#999';
    if (score >= 90) return '#107c10';
    if (score >= 80) return '#0f7938';
    if (score >= 70) return '#f7630c';
    return '#d13438';
  };

  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' } as React.CSSProperties}>
          {/* Integration Status */}
          <div
            style={{
              padding: '8px',
              backgroundColor: data.integrated ? '#d4f4dd' : '#fff3cd',
              color: data.integrated ? '#0f5132' : '#664d03',
              borderRadius: '3px',
              fontSize: '0.8em',
              fontWeight: '600',
              textAlign: 'center',
            }}
          >
            {data.integrated ? '✓ Connected' : '✕ Not Connected'}
          </div>

          {/* Accessibility Score */}
          {data.accessibility && (
            <div style={{ borderTop: '1px solid #eee', paddingTop: '8px' }}>
              <div style={{ fontSize: '0.85em', fontWeight: '600', marginBottom: '4px', color: '#333' }}>
                Accessibility Level
              </div>
              <div
                style={{
                  padding: '8px',
                  backgroundColor: getAccessibilityColor(data.accessibility.level),
                  color: '#fff',
                  borderRadius: '3px',
                  fontSize: '1.1em',
                  fontWeight: '700',
                  textAlign: 'center',
                  letterSpacing: '1px',
                }}
              >
                {data.accessibility.level || 'N/A'}
              </div>
              {data.accessibility.status && (
                <div style={{ fontSize: '0.7em', color: '#666', marginTop: '4px', textAlign: 'center' }}>
                  Status: {data.accessibility.status}
                </div>
              )}
            </div>
          )}

          {/* Quality Score */}
          {data.quality && (
            <div style={{ borderTop: '1px solid #eee', paddingTop: '8px' }}>
              <div style={{ fontSize: '0.85em', fontWeight: '600', marginBottom: '4px', color: '#333' }}>
                Quality Score
              </div>
              <div
                style={{
                  padding: '8px',
                  backgroundColor: getQualityColor(data.quality.score),
                  color: '#fff',
                  borderRadius: '3px',
                  fontSize: '1.1em',
                  fontWeight: '700',
                  textAlign: 'center',
                }}
              >
                {data.quality.score !== undefined ? `${data.quality.score}%` : 'N/A'}
              </div>
              {data.quality.status && (
                <div style={{ fontSize: '0.7em', color: '#666', marginTop: '4px', textAlign: 'center' }}>
                  Status: {data.quality.status}
                </div>
              )}
            </div>
          )}

          {/* Account ID */}
          {data.accountId && (
            <div style={{ fontSize: '0.7em', color: '#999', marginTop: '4px', paddingTop: '8px', borderTop: '1px solid #eee' }}>
              Account: {data.accountId}
            </div>
          )}

          {/* Last Checked */}
          {data.lastChecked && (
            <div style={{ fontSize: '0.7em', color: '#999', marginTop: '2px' }}>
              Last checked: {data.lastChecked}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
