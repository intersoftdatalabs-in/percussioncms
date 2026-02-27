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

interface Comment {
  id: string;
  author?: string;
  content?: string;
  text?: string;
  email?: string;
  status?: string;
  createdAt?: string;
  timestamp?: string;
  page?: string;
  post?: string;
  approved?: boolean;
}

interface CommentsData {
  comments?: Comment[];
  items?: Comment[];
  data?: Comment[];
  [key: string]: unknown;
}

export interface CommentsWidgetProps {
  title?: string;
  refreshInterval?: number;
  maxComments?: number;
}

/**
 * CommentsWidget displays the latest comments from site visitors.
 * Shows comment author, content excerpt, page reference, and status.
 * Fetches data from the comments REST endpoint and updates periodically.
 */
export const CommentsWidget: React.FC<CommentsWidgetProps> = ({
  title = 'Latest Comments',
  refreshInterval = 20000,
  maxComments = 8,
}) => {
  const [comments, setComments] = useState<Comment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchComments = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Fetch latest comments
        const response = await get<CommentsData>('/services/comments/latest');

        // Handle response format
        let commentArray: Comment[] = [];
        if (response.comments && Array.isArray(response.comments)) {
          commentArray = response.comments;
        } else if (response.items && Array.isArray(response.items)) {
          commentArray = response.items;
        } else if (response.data && Array.isArray(response.data)) {
          commentArray = response.data;
        } else if (Array.isArray(response)) {
          commentArray = response as Comment[];
        }

        // Limit to maxComments
        setComments(commentArray.slice(0, maxComments));
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load comments';
        setError(errorMessage);
        console.error('CommentsWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchComments();

    // Set up refresh interval if specified
    const interval =
      refreshInterval > 0 ? setInterval(fetchComments, refreshInterval) : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval, maxComments]);

  const getStatusIcon = (comment: Comment): string => {
    if (comment.approved === false || comment.status?.toLowerCase() === 'pending') {
      return '⏳';
    }
    if (comment.status?.toLowerCase() === 'spam') {
      return '🚫';
    }
    return '💬';
  };

  const getStatusDisplay = (comment: Comment): string => {
    if (comment.approved === false || comment.status?.toLowerCase() === 'pending') {
      return 'Pending';
    }
    if (comment.status?.toLowerCase() === 'spam') {
      return 'Spam';
    }
    return 'Approved';
  };

  const truncateText = (text: string | undefined, length: number = 80): string => {
    if (!text) return '';
    return text.length > length ? text.substring(0, length) + '...' : text;
  };

  const formatDate = (timestamp: string | undefined): string => {
    if (!timestamp) return '';
    try {
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
    } catch {
      return timestamp;
    }
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading comments...</p>
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

    if (!comments || comments.length === 0) {
      return (
        <div style={styles.widgetContent}>
          <p>No comments yet</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' } as React.CSSProperties}>
          {comments.map((comment, index) => (
            <div
              key={comment.id || index}
              style={{
                padding: '10px',
                backgroundColor: '#fafafa',
                border: '1px solid #e8e8e8',
                borderRadius: '3px',
                transition: 'all 0.2s ease',
              } as React.CSSProperties}
              onMouseEnter={(e) => {
                const el = e.currentTarget as HTMLDivElement;
                el.style.backgroundColor = '#f0f7ff';
                el.style.borderColor = '#2196f3';
              }}
              onMouseLeave={(e) => {
                const el = e.currentTarget as HTMLDivElement;
                el.style.backgroundColor = '#fafafa';
                el.style.borderColor = '#e8e8e8';
              }}
            >
              <div style={{ display: 'flex', gap: '8px', marginBottom: '4px', alignItems: 'flex-start' } as React.CSSProperties}>
                <div style={{ fontSize: '1.2em', flexShrink: 0 }}>
                  {getStatusIcon(comment)}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: '8px' } as React.CSSProperties}>
                    <div style={{ fontWeight: '600', fontSize: '0.9em', color: '#333' }}>
                      {comment.author || comment.email || 'Anonymous'}
                    </div>
                    <div style={{ fontSize: '0.7em', color: getStatusDisplay(comment) === 'Approved' ? '#4caf50' : getStatusDisplay(comment) === 'Pending' ? '#ff9800' : '#c33', whiteSpace: 'nowrap' }}>
                      {getStatusDisplay(comment)}
                    </div>
                  </div>
                  {(comment.page || comment.post) && (
                    <div style={{ fontSize: '0.75em', color: '#999', marginTop: '2px', marginBottom: '4px' }}>
                      📄 {comment.page || comment.post}
                    </div>
                  )}
                  <div style={{ fontSize: '0.8em', color: '#666', lineHeight: '1.3', marginBottom: '4px' }}>
                    {truncateText(comment.content || comment.text, 80)}
                  </div>
                  <div style={{ fontSize: '0.7em', color: '#999' }}>
                    {formatDate(comment.createdAt || comment.timestamp)}
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

export default CommentsWidget;
