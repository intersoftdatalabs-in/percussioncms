/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

interface ActivityEntry {
  id: string;
  timestamp: string;
  type: string;
  description: string;
  user?: string;
  contentName?: string;
}

interface ActivityData {
  entries: ActivityEntry[];
  totalCount: number;
}

export interface ActivityWidgetProps {
  title?: string;
  maxEntries?: number;
  refreshInterval?: number;
}

/**
 * ActivityWidget displays recent content activity and user actions.
 * Fetches data from the activity REST endpoint and shows a timeline of recent changes.
 */
export const ActivityWidget: React.FC<ActivityWidgetProps> = ({
  title = 'Recent Activity',
  maxEntries = 10,
  refreshInterval = 60000,
}) => {
  const [data, setData] = useState<ActivityData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchActivity = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Call activity REST endpoint
        const response = await get<ActivityData>(
          `/services/activity/contentactivity?limit=${maxEntries}`
        );

        setData(response);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load activity';
        setError(errorMessage);
        console.error('ActivityWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchActivity();

    // Set up refresh interval if specified
    const interval =
      refreshInterval > 0
        ? setInterval(fetchActivity, refreshInterval)
        : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [maxEntries, refreshInterval]);

  const getActivityIcon = (type: string): string => {
    const iconMap: Record<string, string> = {
      publish: '📤',
      revise: '✏️',
      create: '📝',
      delete: '🗑️',
      update: '🔄',
      approve: '✅',
      reject: '❌',
      comment: '💬',
    };
    return iconMap[type.toLowerCase()] || '📌';
  };

  const formatTime = (timestamp: string): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading activity...</p>
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

    if (!data || !data.entries || data.entries.length === 0) {
      return (
        <div style={styles.widgetContent}>
          <p>No recent activity</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <div style={{ listStyle: 'none', padding: 0, margin: 0 } as React.CSSProperties}>
          {data.entries.map((entry, index) => (
            <div
              key={entry.id || index}
              style={{
                padding: '10px 0',
                borderBottom: '1px solid #e0e0e0',
                display: 'flex',
                gap: '10px',
                alignItems: 'flex-start',
              } as React.CSSProperties}
            >
              <div style={{ fontSize: '1.2em', minWidth: '24px' }}>
                {getActivityIcon(entry.type)}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontWeight: 500, color: '#333', fontSize: '0.95em' }}>
                  {entry.description}
                </div>
                <div
                  style={{
                    fontSize: '0.8em',
                    color: '#666',
                    marginTop: '2px',
                  } as React.CSSProperties}
                >
                  {entry.contentName && (
                    <div>{entry.contentName}</div>
                  )}
                  <div style={{ color: '#999' }}>
                    {entry.user && `by ${entry.user} • `}
                    {formatTime(entry.timestamp)}
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

export default ActivityWidget;
