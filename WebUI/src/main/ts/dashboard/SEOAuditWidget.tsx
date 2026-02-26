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

interface SEOIssue {
  category: string;
  count: number;
  severity: 'critical' | 'warning' | 'info';
}

interface SEOAudit {
  score?: number;
  grade?: string;
  lastAudit?: string;
  passedChecks?: number;
  failedChecks?: number;
  issues?: SEOIssue[];
  recommendations?: string[];
}

interface SEOAuditData {
  audit?: SEOAudit;
  data?: SEOAudit;
  seo?: SEOAudit;
  [key: string]: unknown;
}

export interface SEOAuditWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * SEOAuditWidget displays SEO audit results, scores, and recommendations.
 * Shows SEO health metrics, critical issues, and improvement suggestions.
 */
export const SEOAuditWidget: React.FC<SEOAuditWidgetProps> = ({
  title = 'SEO Audit',
  refreshInterval,
}) => {
  const [audit, setAudit] = useState<SEOAudit | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchAudit = async () => {
      setIsLoading(true);
      try {
        const response = await get<SEOAuditData>('/services/seo/audit');
        let auditData: SEOAudit | null = null;

        if (response.audit) {
          auditData = response.audit;
        } else if (response.data) {
          auditData = response.data;
        } else if (response.seo) {
          auditData = response.seo;
        } else if (typeof response === 'object' && !('audit' in response) && !('data' in response) && !('seo' in response)) {
          auditData = {
            score: (response as Record<string, unknown>).score as number | undefined,
            grade: (response as Record<string, unknown>).grade as string | undefined,
            lastAudit: (response as Record<string, unknown>).lastAudit as string | undefined,
            passedChecks: (response as Record<string, unknown>).passedChecks as number | undefined,
            failedChecks: (response as Record<string, unknown>).failedChecks as number | undefined,
            issues: (response as Record<string, unknown>).issues as SEOIssue[] | undefined,
            recommendations: (response as Record<string, unknown>).recommendations as string[] | undefined,
          };
        }

        setAudit(auditData);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load SEO audit data';
        setError(errorMessage);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAudit();
    if (refreshInterval) {
      const interval = setInterval(fetchAudit, refreshInterval * 1000);
      return () => clearInterval(interval);
    }
  }, [refreshInterval]);

  const getGradeColor = (grade?: string): string => {
    if (!grade) return '#999';
    switch (grade.toUpperCase()) {
      case 'A':
      case 'A+':
        return '#28a745';
      case 'B':
        return '#6c757d';
      case 'C':
      case 'D':
        return '#ffc107';
      default:
        return '#dc3545';
    }
  };

  const getSeverityIcon = (severity: string): string => {
    switch (severity) {
      case 'critical':
        return '🔴';
      case 'warning':
        return '🟡';
      default:
        return '🔵';
    }
  };

  if (isLoading) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading}>Loading SEO audit data...</div>
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

  if (!audit) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetContent}>No SEO audit data available</div>
      </div>
    );
  }

  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' } as React.CSSProperties}>
          {/* Score Section */}
          {audit.score !== undefined && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' } as React.CSSProperties}>
              <div style={{ fontSize: '2em', fontWeight: '700', color: getGradeColor(audit.grade), minWidth: '60px', textAlign: 'center' }}>
                {audit.score}/100
              </div>
              {audit.grade && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px' } as React.CSSProperties}>
                  <div style={{ fontSize: '1.5em', fontWeight: '700', color: getGradeColor(audit.grade) }}>
                    {audit.grade}
                  </div>
                  <div style={{ fontSize: '0.8em', color: '#666' }}>Overall Grade</div>
                </div>
              )}
            </div>
          )}

          {/* Checks Section */}
          {(audit.passedChecks !== undefined || audit.failedChecks !== undefined) && (
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'space-between' } as React.CSSProperties}>
              {audit.passedChecks !== undefined && (
                <div style={{ flex: 1, padding: '8px', backgroundColor: '#f0f8f0', borderRadius: '3px' }}>
                  <div style={{ fontSize: '0.75em', color: '#666', marginBottom: '2px' }}>
                    Passed Checks
                  </div>
                  <div style={{ fontSize: '1.2em', fontWeight: '600', color: '#28a745' }}>
                    {audit.passedChecks}
                  </div>
                </div>
              )}
              {audit.failedChecks !== undefined && (
                <div style={{ flex: 1, padding: '8px', backgroundColor: '#fff5f5', borderRadius: '3px' }}>
                  <div style={{ fontSize: '0.75em', color: '#666', marginBottom: '2px' }}>
                    Failed Checks
                  </div>
                  <div style={{ fontSize: '1.2em', fontWeight: '600', color: '#dc3545' }}>
                    {audit.failedChecks}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Issues Section */}
          {audit.issues && audit.issues.length > 0 && (
            <div style={{ borderTop: '1px solid #eee', paddingTop: '8px' }}>
              <div style={{ fontSize: '0.85em', fontWeight: '600', marginBottom: '6px', color: '#333' }}>
                Issues by Category
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' } as React.CSSProperties}>
                {audit.issues.map((issue, idx) => (
                  <div key={idx} style={{ fontSize: '0.8em', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span>{getSeverityIcon(issue.severity)}</span>
                    <span style={{ flex: 1 }}>{issue.category}</span>
                    <span style={{ fontWeight: '600', color: '#666' }}>{issue.count}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Last Audit */}
          {audit.lastAudit && (
            <div style={{ fontSize: '0.7em', color: '#999', marginTop: '4px', paddingTop: '8px', borderTop: '1px solid #eee' }}>
              Last audit: {audit.lastAudit}
            </div>
          )}

          {/* Recommendations */}
          {audit.recommendations && audit.recommendations.length > 0 && (
            <div style={{ fontSize: '0.75em', color: '#666', padding: '8px', backgroundColor: '#f9f9f9', borderRadius: '3px', marginTop: '4px' }}>
              <div style={{ fontWeight: '600', marginBottom: '4px' }}>Recommendations:</div>
              <ul style={{ margin: '0', paddingLeft: '16px' }}>
                {audit.recommendations.slice(0, 3).map((rec, idx) => (
                  <li key={idx} style={{ marginBottom: '2px' }}>
                    {rec}
                  </li>
                ))}
                {audit.recommendations.length > 3 && (
                  <li style={{ color: '#999', fontStyle: 'italic' }}>
                    +{audit.recommendations.length - 3} more recommendations
                  </li>
                )}
              </ul>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
