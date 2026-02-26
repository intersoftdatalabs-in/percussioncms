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

interface CookieConsent {
  id?: string;
  status: string;
  consentRate?: number;
  consentedUsers?: number;
  totalUsers?: number;
  lastUpdated?: string;
  complianceScore?: number;
  categories?: string[];
}

interface CookieConsentData {
  consent?: CookieConsent;
  status?: CookieConsent;
  data?: CookieConsent;
  [key: string]: unknown;
}

export interface CookieConsentWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * CookieConsentWidget displays GDPR/compliance cookie consent status.
 * Shows consent rates, compliance scores, and consent category breakdown.
 */
export const CookieConsentWidget: React.FC<CookieConsentWidgetProps> = ({
  title = 'Cookie Consent',
  refreshInterval = 30000,
}) => {
  const [consent, setConsent] = useState<CookieConsent | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchConsent = async () => {
      try {
        setIsLoading(true);
        setError(null);

        const response = await get<CookieConsentData>('/services/compliance/cookie-consent');

        let consentData: CookieConsent | null = null;
        if (response.consent) {
          consentData = response.consent;
        } else if (response.status && typeof response.status === 'object') {
          consentData = response.status;
        } else if (response.data) {
          consentData = response.data;
        } else if (typeof response === 'object' && !('consent' in response) && !('status' in response) && !('data' in response)) {
          // If response is a plain object with the expected properties
          consentData = {
            status: (response as Record<string, unknown>).status as string,
            complianceScore: (response as Record<string, unknown>).complianceScore as number | undefined,
            consentRate: (response as Record<string, unknown>).consentRate as number | undefined,
            categories: (response as Record<string, unknown>).categories as string[] | undefined,
          };
        }

        setConsent(consentData);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load cookie consent data';
        setError(errorMessage);
        console.error('CookieConsentWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchConsent();

    const interval =
      refreshInterval > 0 ? setInterval(fetchConsent, refreshInterval) : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval]);

  const getStatusIcon = (status: string): string => {
    const s = status?.toLowerCase() || '';
    if (s.includes('compliant') || s.includes('active')) return '✅';
    if (s.includes('warning') || s.includes('partial')) return '⚠️';
    return '❌';
  };

  const getStatusColor = (status: string): string => {
    const s = status?.toLowerCase() || '';
    if (s.includes('compliant') || s.includes('active')) return '#4caf50';
    if (s.includes('warning') || s.includes('partial')) return '#ff9800';
    return '#c33';
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading compliance data...</p>
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

    if (!consent) {
      return (
        <div style={styles.widgetContent}>
          <p>No compliance data available</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' } as React.CSSProperties}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' } as React.CSSProperties}>
            <div style={{ fontSize: '1.5em' }}>{getStatusIcon(consent.status)}</div>
            <div>
              <div style={{ fontSize: '0.9em', fontWeight: '600', color: getStatusColor(consent.status) }}>
                {consent.status}
              </div>
            </div>
          </div>

          {(consent.consentRate || consent.complianceScore) && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' } as React.CSSProperties}>
              {consent.consentRate !== undefined && (
                <div>
                  <div style={{ fontSize: '0.75em', color: '#999', marginBottom: '2px' }}>
                    User Consent Rate
                  </div>
                  <div style={{ fontSize: '1.2em', fontWeight: '600', color: '#333' }}>
                    {consent.consentRate}%
                  </div>
                </div>
              )}
              {consent.complianceScore !== undefined && (
                <div>
                  <div style={{ fontSize: '0.75em', color: '#999', marginBottom: '2px' }}>
                    Compliance Score
                  </div>
                  <div style={{ fontSize: '1.2em', fontWeight: '600', color: getStatusColor(consent.status) }}>
                    {consent.complianceScore}/100
                  </div>
                </div>
              )}
            </div>
          )}

          {consent.consentedUsers && consent.totalUsers && (
            <div style={{ fontSize: '0.8em', color: '#666', padding: '8px', backgroundColor: '#f5f5f5', borderRadius: '3px' }}>
              📊 {consent.consentedUsers} of {consent.totalUsers} users consented
            </div>
          )}

          {consent.categories && consent.categories.length > 0 && (
            <div>
              <div style={{ fontSize: '0.75em', color: '#999', marginBottom: '4px', fontWeight: '600' }}>
                Consent Categories:
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' } as React.CSSProperties}>
                {consent.categories.map((cat, i) => (
                  <div
                    key={i}
                    style={{
                      padding: '4px 8px',
                      fontSize: '0.7em',
                      backgroundColor: '#e3f2fd',
                      color: '#1976d2',
                      borderRadius: '3px',
                    }}
                  >
                    {cat}
                  </div>
                ))}
              </div>
            </div>
          )}

          {consent.lastUpdated && (
            <div style={{ fontSize: '0.7em', color: '#999', marginTop: '4px' }}>
              Updated: {consent.lastUpdated}
            </div>
          )}
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

export default CookieConsentWidget;
