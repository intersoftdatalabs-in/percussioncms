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

interface FrameworkModule {
  name: string;
  version?: string;
  enabled?: boolean;
  status?: string;
}

interface FrameworkConfig {
  frameworkVersion: string;
  modules?: FrameworkModule[];
  enabledModules?: number;
  totalModules?: number;
  lastChecked?: string;
}

interface FrameworkData {
  framework?: FrameworkConfig;
  data?: FrameworkConfig;
  config?: FrameworkConfig;
  [key: string]: unknown;
}

export interface SitewideFrameworkWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * SitewideFrameworkWidget displays framework configuration and status.
 * Shows framework version, active modules, and system health information.
 */
export const SitewideFrameworkWidget: React.FC<SitewideFrameworkWidgetProps> = ({
  title = 'Framework Configuration',
  refreshInterval,
}) => {
  const [config, setConfig] = useState<FrameworkConfig | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchConfig = async () => {
      setIsLoading(true);
      try {
        const response = await get<FrameworkData>('/services/framework/config');
        let frameworkConfig: FrameworkConfig | null = null;

        if (response.framework) {
          frameworkConfig = response.framework;
        } else if (response.data) {
          frameworkConfig = response.data;
        } else if (response.config) {
          frameworkConfig = response.config;
        } else if (typeof response === 'object' && 'frameworkVersion' in response) {
          frameworkConfig = {
            frameworkVersion: (response as Record<string, unknown>).frameworkVersion as string,
            modules: (response as Record<string, unknown>).modules as FrameworkModule[] | undefined,
            enabledModules: (response as Record<string, unknown>).enabledModules as number | undefined,
            totalModules: (response as Record<string, unknown>).totalModules as number | undefined,
            lastChecked: (response as Record<string, unknown>).lastChecked as string | undefined,
          };
        }

        setConfig(frameworkConfig);
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to load framework configuration';
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
        <div style={styles.widgetLoading}>Loading framework configuration...</div>
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
        <div style={styles.widgetContent}>No framework configuration available</div>
      </div>
    );
  }

  const moduleStats = config.modules
    ? {
        total: config.totalModules || config.modules.length,
        enabled: config.enabledModules || config.modules.filter((m) => m.enabled !== false).length,
      }
    : {
        total: config.totalModules || 0,
        enabled: config.enabledModules || 0,
      };

  const healthStatus = moduleStats.enabled === moduleStats.total ? 'healthy' : 'degraded';

  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' } as React.CSSProperties}>
          {/* Version and Status */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' } as React.CSSProperties}>
            <div>
              <div style={{ fontSize: '0.75em', color: '#666', marginBottom: '2px' }}>Framework Version</div>
              <div style={{ fontSize: '1.1em', fontWeight: '600', color: '#333' }}>
                v{config.frameworkVersion}
              </div>
            </div>
            <div
              style={{
                padding: '6px 12px',
                backgroundColor: healthStatus === 'healthy' ? '#d4f4dd' : '#fff3cd',
                color: healthStatus === 'healthy' ? '#0f5132' : '#664d03',
                borderRadius: '3px',
                fontSize: '0.8em',
                fontWeight: '600',
                textTransform: 'capitalize',
              }}
            >
              {healthStatus}
            </div>
          </div>

          {/* Module Statistics */}
          {(moduleStats.total > 0 || config.modules) && (
            <div style={{ borderTop: '1px solid #eee', paddingTop: '8px' }}>
              <div style={{ display: 'flex', gap: '12px', justifyContent: 'space-between' } as React.CSSProperties}>
                <div style={{ flex: 1, padding: '8px', backgroundColor: '#f0f0ff', borderRadius: '3px' }}>
                  <div style={{ fontSize: '0.75em', color: '#666', marginBottom: '2px' }}>Total Modules</div>
                  <div style={{ fontSize: '1.2em', fontWeight: '600', color: '#333' }}>
                    {moduleStats.total}
                  </div>
                </div>
                <div style={{ flex: 1, padding: '8px', backgroundColor: '#f0f8f0', borderRadius: '3px' }}>
                  <div style={{ fontSize: '0.75em', color: '#666', marginBottom: '2px' }}>Enabled</div>
                  <div style={{ fontSize: '1.2em', fontWeight: '600', color: '#0f5132' }}>
                    {moduleStats.enabled}
                  </div>
                </div>
              </div>

              {/* Module List */}
              {config.modules && config.modules.length > 0 && (
                <div style={{ marginTop: '8px', paddingTop: '8px', borderTop: '1px solid #eee' }}>
                  <div style={{ fontSize: '0.85em', fontWeight: '600', marginBottom: '6px', color: '#333' }}>
                    Active Modules
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' } as React.CSSProperties}>
                    {config.modules.slice(0, 4).map((module, idx) => (
                      <div
                        key={idx}
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
                        <span style={{ fontWeight: '500' }}>{module.name}</span>
                        <span
                          style={{
                            fontSize: '0.85em',
                            padding: '2px 6px',
                            backgroundColor: module.enabled !== false ? '#d4f4dd' : '#f8d7da',
                            color: module.enabled !== false ? '#0f5132' : '#721c24',
                            borderRadius: '2px',
                          }}
                        >
                          {module.enabled !== false ? '✓' : '✕'}
                        </span>
                      </div>
                    ))}
                    {config.modules.length > 4 && (
                      <div
                        style={{
                          fontSize: '0.75em',
                          color: '#999',
                          fontStyle: 'italic',
                          textAlign: 'center',
                          padding: '4px',
                        }}
                      >
                        +{config.modules.length - 4} more modules
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Last Checked */}
          {config.lastChecked && (
            <div style={{ fontSize: '0.7em', color: '#999', marginTop: '4px', paddingTop: '8px', borderTop: '1px solid #eee' }}>
              Last checked: {config.lastChecked}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
