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

interface AssetWorkflow {
  name: string;
  count: number;
  percentage?: number;
  icon?: string;
}

interface AssetStatusData {
  workflows?: AssetWorkflow[];
  timestamp?: string;
  [key: string]: unknown;
}

export interface AssetsStatusWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * AssetsStatusWidget displays the distribution of assets across workflow statuses.
 * Shows how many assets are in each workflow stage (Draft, Review, Published, etc.).
 * Fetches data from the asset workflow REST endpoint and updates periodically.
 */
export const AssetsStatusWidget: React.FC<AssetsStatusWidgetProps> = ({
  title = 'Assets By Status',
  refreshInterval = 30000,
}) => {
  const [workflows, setWorkflows] = useState<AssetWorkflow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    const fetchWorkflows = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Fetch asset workflow status from the asset service
        const response = await get<AssetStatusData>('/services/asset/workflow-status');

        // Handle response format
        let workflowArray: AssetWorkflow[] = [];
        if (response.workflows && Array.isArray(response.workflows)) {
          workflowArray = response.workflows;
        } else if (Array.isArray(response)) {
          workflowArray = response as AssetWorkflow[];
        }

        // Calculate total and percentages
        const sum = workflowArray.reduce((acc, w) => acc + (w.count || 0), 0);
        setTotal(sum);

        const withPercentages = workflowArray.map((w) => ({
          ...w,
          percentage: sum > 0 ? Math.round((w.count / sum) * 100) : 0,
        }));

        setWorkflows(withPercentages);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load asset workflow status';
        setError(errorMessage);
        console.error('AssetsStatusWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchWorkflows();

    // Set up refresh interval if specified
    const interval =
      refreshInterval > 0 ? setInterval(fetchWorkflows, refreshInterval) : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval]);

  const getWorkflowIcon = (name: string): string => {
    const nameLower = name?.toLowerCase() || '';
    if (nameLower.includes('draft') || nameLower.includes('new')) return '📝';
    if (nameLower.includes('review') || nameLower.includes('pending')) return '👁️';
    if (nameLower.includes('approved') || nameLower.includes('publish')) return '✅';
    if (nameLower.includes('archived')) return '📦';
    if (nameLower.includes('rejected')) return '❌';
    return '📄';
  };

  const getStatusColor = (percentage: number): string => {
    if (percentage === 0) return '#ccc';
    if (percentage >= 50) return '#4caf50'; // Green
    if (percentage >= 25) return '#ff9800'; // Orange
    return '#f44336'; // Red
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading asset status...</p>
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

    if (!workflows || workflows.length === 0) {
      return (
        <div style={styles.widgetContent}>
          <p>No asset workflow status available</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <div style={{ marginBottom: '8px', fontSize: '0.85em', color: '#666' } as React.CSSProperties}>
          Total Assets: <strong>{total}</strong>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' } as React.CSSProperties}>
          {workflows.map((workflow, index) => (
            <div
              key={workflow.name || index}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '8px',
                backgroundColor: '#f9f9f9',
                borderRadius: '4px',
              } as React.CSSProperties}
            >
              <div style={{ fontSize: '1.2em', minWidth: '24px' }}>
                {workflow.icon || getWorkflowIcon(workflow.name)}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontWeight: 500, color: '#333', fontSize: '0.9em' }}>
                  {workflow.name}
                </div>
                <div
                  style={{
                    height: '6px',
                    backgroundColor: '#e0e0e0',
                    borderRadius: '3px',
                    marginTop: '4px',
                    overflow: 'hidden',
                  } as React.CSSProperties}
                >
                  <div
                    style={{
                      height: '100%',
                      backgroundColor: getStatusColor(workflow.percentage || 0),
                      width: `${workflow.percentage || 0}%`,
                      transition: 'width 0.3s ease',
                    } as React.CSSProperties}
                  />
                </div>
              </div>
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'flex-end',
                  gap: '2px',
                  marginLeft: '8px',
                  whiteSpace: 'nowrap',
                } as React.CSSProperties}
              >
                <div style={{ fontWeight: 'bold', color: '#007ea8', fontSize: '0.95em' }}>
                  {workflow.count}
                </div>
                <div style={{ fontSize: '0.75em', color: '#999' }}>
                  {workflow.percentage}%
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

export default AssetsStatusWidget;
