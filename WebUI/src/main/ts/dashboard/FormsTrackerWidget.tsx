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

interface FormSubmission {
  id: string;
  formName: string;
  formId?: string;
  submissions: number;
  successCount?: number;
  errorCount?: number;
  lastSubmission?: string;
  createdAt?: string;
  submissionRate?: string;
  status?: string;
}

interface FormsTrackerData {
  forms?: FormSubmission[];
  items?: FormSubmission[];
  trackedForms?: FormSubmission[];
  data?: FormSubmission[];
  [key: string]: unknown;
}

export interface FormsTrackerWidgetProps {
  title?: string;
  refreshInterval?: number;
  maxForms?: number;
}

/**
 * FormsTrackerWidget displays form submission metrics and tracking.
 * Shows form name, total submissions, success/error counts, and trends.
 * Fetches data from the forms tracker REST endpoint and updates periodically.
 */
export const FormsTrackerWidget: React.FC<FormsTrackerWidgetProps> = ({
  title = 'Form Tracker',
  refreshInterval = 25000,
  maxForms = 10,
}) => {
  const [forms, setForms] = useState<FormSubmission[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchForms = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Fetch forms tracker data
        const response = await get<FormsTrackerData>('/services/forms/tracker');

        // Handle response format
        let formArray: FormSubmission[] = [];
        if (response.forms && Array.isArray(response.forms)) {
          formArray = response.forms;
        } else if (response.items && Array.isArray(response.items)) {
          formArray = response.items;
        } else if (response.trackedForms && Array.isArray(response.trackedForms)) {
          formArray = response.trackedForms;
        } else if (response.data && Array.isArray(response.data)) {
          formArray = response.data;
        } else if (Array.isArray(response)) {
          formArray = response as FormSubmission[];
        }

        // Limit to maxForms
        setForms(formArray.slice(0, maxForms));
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load forms tracker data';
        setError(errorMessage);
        console.error('FormsTrackerWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchForms();

    // Set up refresh interval if specified
    const interval =
      refreshInterval > 0 ? setInterval(fetchForms, refreshInterval) : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval, maxForms]);

  const getStatusIcon = (f: FormSubmission): string => {
    const errorRate = f.errorCount ? (f.errorCount / f.submissions) * 100 : 0;
    if (errorRate > 20) return '⚠️';
    if (f.status?.toLowerCase() === 'inactive') return '⏸️';
    return '📋';
  };

  const getStatusColor = (f: FormSubmission): string => {
    const errorRate = f.errorCount ? (f.errorCount / f.submissions) * 100 : 0;
    if (errorRate > 20) return '#ff5722';
    if (f.status?.toLowerCase() === 'inactive') return '#999';
    return '#4caf50';
  };

  const formatDate = (timestamp: string | undefined): string => {
    if (!timestamp) return '';
    try {
      const date = new Date(timestamp);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffHours = Math.floor(diffMs / 3600000);
      const diffDays = Math.floor(diffMs / 86400000);

      if (diffHours < 1) return 'just now';
      if (diffHours < 24) return `${diffHours}h ago`;
      if (diffDays < 7) return `${diffDays}d ago`;
      return date.toLocaleDateString();
    } catch {
      return timestamp;
    }
  };

  const calculateSuccessRate = (f: FormSubmission): string => {
    if (!f.submissions || f.submissions === 0) return '0%';
    if (!f.errorCount) return '100%';
    const successCount = f.submissions - (f.errorCount || 0);
    const rate = ((successCount / f.submissions) * 100).toFixed(0);
    return `${rate}%`;
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading form tracker...</p>
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

    if (!forms || forms.length === 0) {
      return (
        <div style={styles.widgetContent}>
          <p>No tracked forms</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' } as React.CSSProperties}>
          {forms.map((form, index) => (
            <div
              key={form.id || index}
              style={{
                padding: '10px',
                backgroundColor: '#fafafa',
                border: `1px solid ${getStatusColor(form)}33`,
                borderLeft: `3px solid ${getStatusColor(form)}`,
                borderRadius: '3px',
                transition: 'all 0.2s ease',
              } as React.CSSProperties}
              onMouseEnter={(e) => {
                const el = e.currentTarget as HTMLDivElement;
                el.style.backgroundColor = '#f0f7ff';
              }}
              onMouseLeave={(e) => {
                const el = e.currentTarget as HTMLDivElement;
                el.style.backgroundColor = '#fafafa';
              }}
            >
              <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-start', marginBottom: '6px' } as React.CSSProperties}>
                <div style={{ fontSize: '1.1em', flexShrink: 0 }}>
                  {getStatusIcon(form)}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: '600', fontSize: '0.9em', color: '#333', marginBottom: '2px' }}>
                    {form.formName}
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '8px', fontSize: '0.75em', color: '#666' } as React.CSSProperties}>
                    <div>📊 {form.submissions} submissions</div>
                    {form.successCount !== undefined && (
                      <div style={{ color: '#4caf50' }}>✓ {form.successCount} success</div>
                    )}
                    <div>Success Rate: <span style={{ fontWeight: '600', color: getStatusColor(form) }}>{calculateSuccessRate(form)}</span></div>
                    {form.errorCount !== undefined && form.errorCount > 0 && (
                      <div style={{ color: '#c33' }}>✗ {form.errorCount} errors</div>
                    )}
                  </div>
                  {form.lastSubmission && (
                    <div style={{ fontSize: '0.7em', color: '#999', marginTop: '4px' }}>
                      Last: {formatDate(form.lastSubmission)}
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

export default FormsTrackerWidget;
