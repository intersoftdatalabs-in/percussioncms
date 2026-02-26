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

interface Blog {
  id: string;
  title: string;
  status: string;
  author?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
  publishedAt?: string;
  postCount?: number;
}

interface BlogsData {
  blogs?: Blog[];
  items?: Blog[];
  [key: string]: unknown;
}

export interface BlogsWidgetProps {
  title?: string;
  refreshInterval?: number;
  maxBlogs?: number;
}

/**
 * BlogsWidget displays a list of blogs and their status.
 * Shows blog title, author, post count, and current status.
 * Fetches data from the blogs REST endpoint and updates periodically.
 */
export const BlogsWidget: React.FC<BlogsWidgetProps> = ({
  title = 'Blogs',
  refreshInterval = 30000,
  maxBlogs = 10,
}) => {
  const [blogs, setBlogs] = useState<Blog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchBlogs = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Fetch available blogs
        const response = await get<BlogsData>('/services/blogs/list');

        // Handle response format
        let blogArray: Blog[] = [];
        if (response.blogs && Array.isArray(response.blogs)) {
          blogArray = response.blogs;
        } else if (response.items && Array.isArray(response.items)) {
          blogArray = response.items;
        } else if (Array.isArray(response)) {
          blogArray = response as Blog[];
        }

        // Limit to maxBlogs
        setBlogs(blogArray.slice(0, maxBlogs));
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load blogs';
        setError(errorMessage);
        console.error('BlogsWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchBlogs();

    // Set up refresh interval if specified
    const interval =
      refreshInterval > 0 ? setInterval(fetchBlogs, refreshInterval) : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval, maxBlogs]);

  const getStatusColor = (status: string): string => {
    const s = status?.toLowerCase() || '';
    if (s.includes('published') || s.includes('live')) return '#4caf50';
    if (s.includes('draft')) return '#ff9800';
    if (s.includes('archived')) return '#9e9e9e';
    return '#2196f3';
  };

  const getStatusIcon = (status: string): string => {
    const s = status?.toLowerCase() || '';
    if (s.includes('published') || s.includes('live')) return '📰';
    if (s.includes('draft')) return '✎';
    if (s.includes('archived')) return '📦';
    return '📑';
  };

  const formatDate = (timestamp: string): string => {
    try {
      const date = new Date(timestamp);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffDays = Math.floor(diffMs / 86400000);

      if (diffDays === 0) return 'Today';
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 7) return `${diffDays}d ago`;
      if (diffDays < 30) return `${Math.floor(diffDays / 7)}w ago`;
      return date.toLocaleDateString();
    } catch {
      return timestamp;
    }
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading blogs...</p>
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

    if (!blogs || blogs.length === 0) {
      return (
        <div style={styles.widgetContent}>
          <p>No blogs available</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' } as React.CSSProperties}>
          {blogs.map((blog, index) => (
            <div
              key={blog.id || index}
              style={{
                padding: '12px',
                backgroundColor: '#fafafa',
                border: '1px solid #e8e8e8',
                borderRadius: '4px',
                cursor: 'pointer',
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
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' } as React.CSSProperties}>
                <div style={{ fontSize: '1.3em' }}>
                  {getStatusIcon(blog.status)}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: '600', color: '#333', fontSize: '0.95em', marginBottom: '2px' }}>
                    {blog.title}
                  </div>
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginBottom: '4px', fontSize: '0.8em' } as React.CSSProperties}>
                    <span style={{ color: getStatusColor(blog.status), fontWeight: '500' }}>
                      {blog.status}
                    </span>
                    {blog.postCount !== undefined && (
                      <span style={{ color: '#999' }}>
                        • {blog.postCount} posts
                      </span>
                    )}
                  </div>
                  {blog.author && (
                    <div style={{ fontSize: '0.75em', color: '#999', marginBottom: '4px' }}>
                      👤 By {blog.author}
                    </div>
                  )}
                  {blog.description && (
                    <div style={{ fontSize: '0.8em', color: '#666', marginBottom: '6px', lineHeight: '1.3' }}>
                      {blog.description}
                    </div>
                  )}
                  <div style={{ display: 'flex', gap: '12px', fontSize: '0.75em', color: '#999' } as React.CSSProperties}>
                    {blog.publishedAt && <span>📅 Published: {formatDate(blog.publishedAt)}</span>}
                    {blog.updatedAt && !blog.publishedAt && <span>Updated: {formatDate(blog.updatedAt)}</span>}
                    {blog.createdAt && !blog.publishedAt && !blog.updatedAt && <span>Created: {formatDate(blog.createdAt)}</span>}
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

export default BlogsWidget;
