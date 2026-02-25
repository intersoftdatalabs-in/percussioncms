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

interface WorkflowItem {
  id: string;
  name: string;
  state: string;
  count: number;
  progress?: number;
}

interface WorkflowStatusData {
  items: WorkflowItem[];
  totalCount: number;
  lastUpdated: string;
}

export interface WorkflowStatusWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * WorkflowStatusWidget displays the current workflow statuses and task counts.
 * Fetches data from the dashboard management REST endpoint and refreshes automatically.
 */
export const WorkflowStatusWidget: React.FC<WorkflowStatusWidgetProps> = ({
  title = 'Workflow Status',
  refreshInterval = 30000,
}) => {
  const [data, setData] = useState<WorkflowStatusData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchWorkflowStatus = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Call dashboard gadget REST endpoint
        const response = await get<WorkflowStatusData>(
          '/services/dashboardmanagement/gadget/workflow-status'
        );

        setData(response);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load workflow status';
        setError(errorMessage);
        console.error('WorkflowStatusWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchWorkflowStatus();

    // Set up refresh interval if specified
    const interval =
      refreshInterval > 0
        ? setInterval(fetchWorkflowStatus, refreshInterval)
        : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval]);

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading workflow status...</p>
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

    if (!data || !data.items || data.items.length === 0) {
      return (
        <div style={styles.widgetContent}>
          <p>No active workflows</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {data.items.map((item) => (
            <li
              key={item.id}
              style={{
                padding: '8px 0',
                borderBottom: '1px solid #e0e0e0',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              } as React.CSSProperties}
            >
              <div>
                <div style={{ fontWeight: 500, color: '#333' }}>
                  {item.name}
                </div>
                <div style={{ fontSize: '0.85em', color: '#666' }}>
                  State: {item.state}
                </div>
              </div>
              <div
                style={{
                  backgroundColor: '#e8f4f8',
                  padding: '4px 12px',
                  borderRadius: '12px',
                  fontWeight: 600,
                  color: '#007ea8',
                  fontSize: '0.9em',
                }}
              >
                {item.count}
              </div>
            </li>
          ))}
        </ul>
        <div
          style={{
            marginTop: '12px',
            fontSize: '0.8em',
            color: '#999',
            textAlign: 'right',
          }}
        >
          Updated: {new Date(data.lastUpdated).toLocaleTimeString()}
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

export default WorkflowStatusWidget;
