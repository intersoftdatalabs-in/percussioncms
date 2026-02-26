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

interface IframeConfig {
  url?: string;
  title?: string;
  height?: number;
  sandbox?: string;
  allowFullscreen?: boolean;
}

interface IframeData {
  iframe?: IframeConfig;
  data?: IframeConfig;
  config?: IframeConfig;
  [key: string]: unknown;
}

export interface IframeWidgetProps {
  title?: string;
  iframeUrl?: string;
  iframeHeight?: number;
  refreshInterval?: number;
}

/**
 * IframeWidget displays external content via iframe.
 * Supports customizable URL and height with security attributes.
 */
export const IframeWidget: React.FC<IframeWidgetProps> = ({
  title = 'External Content',
  iframeUrl,
  iframeHeight = 400,
  refreshInterval,
}) => {
  const [config, setConfig] = useState<IframeConfig | null>(null);
  const [isLoading, setIsLoading] = useState(!iframeUrl);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // If iframeUrl is provided directly, use it
    if (iframeUrl) {
      setConfig({
        url: iframeUrl,
        title: title,
        height: iframeHeight,
      });
      setIsLoading(false);
      return;
    }

    // Otherwise fetch from API
    const fetchConfig = async () => {
      setIsLoading(true);
      try {
        const response = await get<IframeData>('/services/embed/iframe');
        let iframeConfig: IframeConfig | null = null;

        if (response.iframe) {
          iframeConfig = response.iframe;
        } else if (response.data) {
          iframeConfig = response.data;
        } else if (response.config) {
          iframeConfig = response.config;
        } else if (typeof response === 'object' && 'url' in response) {
          iframeConfig = {
            url: (response as Record<string, unknown>).url as string | undefined,
            title: (response as Record<string, unknown>).title as string | undefined,
            height: (response as Record<string, unknown>).height as number | undefined,
            sandbox: (response as Record<string, unknown>).sandbox as string | undefined,
            allowFullscreen: (response as Record<string, unknown>).allowFullscreen as boolean | undefined,
          };
        }

        setConfig(iframeConfig);
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to load iframe configuration';
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
  }, [iframeUrl, title, iframeHeight, refreshInterval]);

  if (isLoading) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading}>Loading external content...</div>
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

  if (!config || !config.url) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetContent}>No iframe URL configured</div>
      </div>
    );
  }

  const height = config.height || iframeHeight || 400;
  const sandbox = config.sandbox || 'allow-scripts allow-same-origin allow-popups';
  const allowFullscreen = config.allowFullscreen !== false;

  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{config.title || title}</div>
      <div style={{ width: '100%', overflow: 'hidden' }}>
        <iframe
          src={config.url}
          title={config.title || title}
          height={height}
          width="100%"
          sandbox={sandbox}
          allowFullScreen={allowFullscreen}
          style={{
            border: 'none',
            borderRadius: '3px',
          }}
        />
      </div>
    </div>
  );
};
