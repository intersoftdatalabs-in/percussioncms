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

interface BulkUploadJob {
  id: string;
  name: string;
  status: 'pending' | 'in-progress' | 'completed' | 'failed';
  progress?: number;
  filesCount?: number;
  successCount?: number;
  failureCount?: number;
  createdAt?: string;
  targetFolder?: string;
}

interface BulkUploadData {
  jobs?: BulkUploadJob[];
  activeJob?: BulkUploadJob;
  recentJobs?: BulkUploadJob[];
  [key: string]: unknown;
}

export interface BulkUploadWidgetProps {
  title?: string;
  refreshInterval?: number;
  maxJobs?: number;
}

/**
 * BulkUploadWidget displays the status of bulk file upload operations.
 * Shows recent and active upload jobs with progress, success/failure counts, and target folder information.
 * Fetches data from the bulk upload REST endpoint and updates in real time.
 */
export const BulkUploadWidget: React.FC<BulkUploadWidgetProps> = ({
  title = 'Bulk Upload Status',
  refreshInterval = 10000,
  maxJobs = 5,
}) => {
  const [jobs, setJobs] = useState<BulkUploadJob[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchJobs = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Fetch bulk upload job status
        const response = await get<BulkUploadData>('/services/bulk-upload/jobs');

        // Handle response format
        let jobArray: BulkUploadJob[] = [];
        if (response.jobs && Array.isArray(response.jobs)) {
          jobArray = response.jobs;
        } else if (response.recentJobs && Array.isArray(response.recentJobs)) {
          jobArray = response.recentJobs;
        } else if (response.activeJob) {
          jobArray = [response.activeJob];
        } else if (Array.isArray(response)) {
          jobArray = response as BulkUploadJob[];
        }

        // Limit to maxJobs
        setJobs(jobArray.slice(0, maxJobs));
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load bulk upload status';
        setError(errorMessage);
        console.error('BulkUploadWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchJobs();

    // Set up refresh interval if specified
    const interval =
      refreshInterval > 0 ? setInterval(fetchJobs, refreshInterval) : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval, maxJobs]);

  const getStatusIcon = (status: string): string => {
    switch (status?.toLowerCase()) {
      case 'pending':
        return '⏳';
      case 'in-progress':
        return '⚙️';
      case 'completed':
        return '✅';
      case 'failed':
        return '❌';
      default:
        return '📤';
    }
  };

  const getStatusColor = (status: string): string => {
    switch (status?.toLowerCase()) {
      case 'completed':
        return '#4caf50';
      case 'in-progress':
        return '#2196f3';
      case 'pending':
        return '#ff9800';
      case 'failed':
        return '#f44336';
      default:
        return '#999';
    }
  };

  const formatTime = (timestamp: string): string => {
    try {
      const date = new Date(timestamp);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / 60000);
      const diffHours = Math.floor(diffMs / 3600000);

      if (diffMins < 1) return 'just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      if (diffHours < 24) return `${diffHours}h ago`;
      return date.toLocaleDateString();
    } catch {
      return timestamp;
    }
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading bulk upload status...</p>
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

    if (!jobs || jobs.length === 0) {
      return (
        <div style={styles.widgetContent}>
          <p>No bulk upload jobs</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' } as React.CSSProperties}>
          {jobs.map((job, index) => (
            <div
              key={job.id || index}
              style={{
                padding: '10px',
                backgroundColor: '#f9f9f9',
                borderLeft: `4px solid ${getStatusColor(job.status)}`,
                borderRadius: '2px',
              } as React.CSSProperties}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' } as React.CSSProperties}>
                <div style={{ fontSize: '1.2em', marginTop: '2px' }}>
                  {getStatusIcon(job.status)}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 500, color: '#333', fontSize: '0.9em', marginBottom: '2px' }}>
                    {job.name}
                  </div>
                  <div style={{ fontSize: '0.8em', color: '#666', marginBottom: '4px' }}>
                    {job.targetFolder && <span>{job.targetFolder}</span>}
                    {job.createdAt && <span> • {formatTime(job.createdAt)}</span>}
                  </div>
                  {job.progress !== undefined && job.status === 'in-progress' && (
                    <div
                      style={{
                        height: '6px',
                        backgroundColor: '#e0e0e0',
                        borderRadius: '3px',
                        overflow: 'hidden',
                      } as React.CSSProperties}
                    >
                      <div
                        style={{
                          height: '100%',
                          backgroundColor: '#2196f3',
                          width: `${Math.min(job.progress, 100)}%`,
                          transition: 'width 0.3s ease',
                        } as React.CSSProperties}
                      />
                    </div>
                  )}
                </div>
              </div>
              <div style={{ display: 'flex', gap: '12px', marginTop: '6px', fontSize: '0.8em' } as React.CSSProperties}>
                {job.filesCount !== undefined && (
                  <span style={{ color: '#666' }}>
                    📁 {job.filesCount} {job.filesCount === 1 ? 'file' : 'files'}
                  </span>
                )}
                {job.successCount !== undefined && (
                  <span style={{ color: '#4caf50' }}>
                    ✅ {job.successCount} uploaded
                  </span>
                )}
                {job.failureCount !== undefined && job.failureCount > 0 && (
                  <span style={{ color: '#f44336' }}>
                    ❌ {job.failureCount} failed
                  </span>
                )}
                {job.progress !== undefined && job.status === 'in-progress' && (
                  <span style={{ color: '#2196f3', fontWeight: 'bold' }}>
                    {Math.round(job.progress)}%
                  </span>
                )}
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

export default BulkUploadWidget;
