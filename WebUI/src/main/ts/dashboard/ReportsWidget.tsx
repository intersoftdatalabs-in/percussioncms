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

interface Report {
  id: string;
  name: string;
  type: string;
  description?: string;
  category?: string;
  endpoint?: string;
  createdAt?: string;
  lastRun?: string;
}

interface ReportsData {
  reports?: Report[];
  availableReports?: Report[];
  items?: Report[];
  [key: string]: unknown;
}

export interface ReportsWidgetProps {
  title?: string;
  refreshInterval?: number;
  maxReports?: number;
}

/**
 * ReportsWidget displays available reports that users can generate.
 * Shows report name, type, description, and provides quick access to run reports.
 * Fetches data from the reports REST endpoint and updates periodically.
 */
export const ReportsWidget: React.FC<ReportsWidgetProps> = ({
  title = 'Available Reports',
  refreshInterval = 30000,
  maxReports = 10,
}) => {
  const [reports, setReports] = useState<Report[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchReports = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Fetch available reports
        const response = await get<ReportsData>('/services/reports/list');

        // Handle response format
        let reportArray: Report[] = [];
        if (response.reports && Array.isArray(response.reports)) {
          reportArray = response.reports;
        } else if (response.availableReports && Array.isArray(response.availableReports)) {
          reportArray = response.availableReports;
        } else if (response.items && Array.isArray(response.items)) {
          reportArray = response.items;
        } else if (Array.isArray(response)) {
          reportArray = response as Report[];
        }

        // Limit to maxReports
        setReports(reportArray.slice(0, maxReports));
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load reports';
        setError(errorMessage);
        console.error('ReportsWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchReports();

    // Set up refresh interval if specified
    const interval =
      refreshInterval > 0 ? setInterval(fetchReports, refreshInterval) : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval, maxReports]);

  const getCategoryIcon = (category: string): string => {
    const cat = category?.toLowerCase() || '';
    if (cat.includes('content') || cat.includes('page')) return '📄';
    if (cat.includes('asset') || cat.includes('image')) return '🖼️';
    if (cat.includes('traffic') || cat.includes('activity')) return '📊';
    if (cat.includes('seo')) return '🔍';
    if (cat.includes('publish') || cat.includes('workflow')) return '📤';
    if (cat.includes('user') || cat.includes('team')) return '👥';
    return '📋';
  };

  const getTypeLabel = (type: string): string => {
    return type
      ?.replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase())
      .trim() || 'Report';
  };

  const formatDate = (timestamp: string): string => {
    try {
      const date = new Date(timestamp);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffDays = Math.floor(diffMs / 86400000);

      if (diffDays === 0) return 'Today';
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 7) return `${diffDays}d ago`;
      if (diffDays < 30) return `${Math.floor(diffDays / 7)}w ago`;
      return date.toLocaleDateString();
    } catch {
      return timestamp;
    }
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading reports...</p>
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

    if (!reports || reports.length === 0) {
      return (
        <div style={styles.widgetContent}>
          <p>No reports available</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' } as React.CSSProperties}>
          {reports.map((report, index) => (
            <div
              key={report.id || index}
              style={{
                padding: '12px',
                backgroundColor: '#fafafa',
                border: '1px solid #e8e8e8',
                borderRadius: '4px',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              } as React.CSSProperties}
              onMouseEnter={(e) => {
                const el = e.currentTarget as HTMLDivElement;
                el.style.backgroundColor = '#f0f7ff';
                el.style.borderColor = '#2196f3';
              }}
              onMouseLeave={(e) => {
                const el = e.currentTarget as HTMLDivElement;
                el.style.backgroundColor = '#fafafa';
                el.style.borderColor = '#e8e8e8';
              }}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' } as React.CSSProperties}>
                <div style={{ fontSize: '1.3em' }}>
                  {getCategoryIcon(report.category || report.type)}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: '600', color: '#333', fontSize: '0.95em', marginBottom: '2px' }}>
                    {report.name}
                  </div>
                  <div style={{ fontSize: '0.75em', color: '#999', marginBottom: '4px' }}>
                    {getTypeLabel(report.type)}
                  </div>
                  {report.description && (
                    <div style={{ fontSize: '0.8em', color: '#666', marginBottom: '6px', lineHeight: '1.3' }}>
                      {report.description}
                    </div>
                  )}
                  <div style={{ display: 'flex', gap: '12px', fontSize: '0.75em', color: '#999' } as React.CSSProperties}>
                    {report.lastRun && <span>📅 Last run: {formatDate(report.lastRun)}</span>}
                    {report.createdAt && !report.lastRun && <span>Created: {formatDate(report.createdAt)}</span>}
                  </div>
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

export default ReportsWidget;
