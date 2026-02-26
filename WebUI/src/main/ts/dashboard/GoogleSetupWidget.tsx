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

interface GoogleService {
  name: string;
  enabled: boolean;
  connected?: boolean;
  lastSync?: string;
  status?: string;
}

interface GoogleSetup {
  accountConnected: boolean;
  email?: string;
  services: GoogleService[];
  lastUpdate?: string;
  syncStatus?: string;
}

interface GoogleSetupData {
  setup?: GoogleSetup;
  data?: GoogleSetup;
  google?: GoogleSetup;
  [key: string]: unknown;
}

export interface GoogleSetupWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * GoogleSetupWidget displays Google integration status and configuration.
 * Shows connected services, sync status, and account information.
 */
export const GoogleSetupWidget: React.FC<GoogleSetupWidgetProps> = ({
  title = 'Google Setup',
  refreshInterval,
}) => {
  const [setup, setSetup] = useState<GoogleSetup | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSetup = async () => {
      setIsLoading(true);
      try {
        const response = await get<GoogleSetupData>('/services/google/setup');
        let setupData: GoogleSetup | null = null;

        if (response.setup) {
          setupData = response.setup;
        } else if (response.data) {
          setupData = response.data;
        } else if (response.google) {
          setupData = response.google;
        } else if (typeof response === 'object' && !('setup' in response) && !('data' in response) && !('google' in response)) {
          setupData = {
            accountConnected: (response as Record<string, unknown>).accountConnected as boolean,
            email: (response as Record<string, unknown>).email as string | undefined,
            services: (response as Record<string, unknown>).services as GoogleService[] | undefined,
            lastUpdate: (response as Record<string, unknown>).lastUpdate as string | undefined,
            syncStatus: (response as Record<string, unknown>).syncStatus as string | undefined,
          } as GoogleSetup;
        }

        setSetup(setupData);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load Google setup data';
        setError(errorMessage);
      } finally {
        setIsLoading(false);
      }
    };

    fetchSetup();
    if (refreshInterval) {
      const interval = setInterval(fetchSetup, refreshInterval * 1000);
      return () => clearInterval(interval);
    }
  }, [refreshInterval]);

  const getStatusColor = (connected?: boolean): string => {
    return connected ? '#28a745' : '#dc3545';
  };

  const getStatusIcon = (connected?: boolean): string => {
    return connected ? '✓' : '✕';
  };

  if (isLoading) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading}>Loading Google setup data...</div>
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

  if (!setup) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetContent}>No Google setup data available</div>
      </div>
    );
  }

  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' } as React.CSSProperties}>
          {/* Account Connection Status */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' } as React.CSSProperties}>
            <div
              style={{
                fontSize: '1.2em',
                fontWeight: '700',
                color: getStatusColor(setup.accountConnected),
              }}
            >
              {getStatusIcon(setup.accountConnected)}
            </div>
            <div>
              <div style={{ fontSize: '0.85em', fontWeight: '600', color: '#333' }}>
                Account {setup.accountConnected ? 'Connected' : 'Not Connected'}
              </div>
              {setup.email && (
                <div style={{ fontSize: '0.75em', color: '#666' }}>{setup.email}</div>
              )}
            </div>
          </div>

          {/* Services Section */}
          {setup.services && setup.services.length > 0 && (
            <div style={{ borderTop: '1px solid #eee', paddingTop: '8px' }}>
              <div style={{ fontSize: '0.85em', fontWeight: '600', marginBottom: '6px', color: '#333' }}>
                Services ({setup.services.length})
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' } as React.CSSProperties}>
                {setup.services.map((svc, idx) => (
                  <div
                    key={idx}
                    style={{
                      fontSize: '0.8em',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      padding: '4px',
                      backgroundColor: svc.connected ? '#f0f8f0' : '#fff5f5',
                      borderRadius: '3px',
                    }}
                  >
                    <span style={{ color: getStatusColor(svc.connected) }}>
                      {getStatusIcon(svc.connected)}
                    </span>
                    <span style={{ flex: 1 }}>{svc.name}</span>
                    {svc.enabled && (
                      <span style={{ fontSize: '0.7em', color: '#999' }}>Enabled</span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Sync Status */}
          {setup.syncStatus && (
            <div style={{ fontSize: '0.8em', color: '#666', padding: '6px', backgroundColor: '#f9f9f9', borderRadius: '3px' }}>
              <span style={{ fontWeight: '600' }}>Sync Status:</span> {setup.syncStatus}
            </div>
          )}

          {/* Last Update */}
          {setup.lastUpdate && (
            <div style={{ fontSize: '0.7em', color: '#999', marginTop: '4px', paddingTop: '8px', borderTop: '1px solid #eee' }}>
              Last updated: {setup.lastUpdate}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
