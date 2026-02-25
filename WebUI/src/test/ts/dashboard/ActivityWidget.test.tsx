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

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { ActivityWidget } from '@/dashboard';
import * as clientModule from '@/api/client';

// Mock the API client
vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('ActivityWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementation(
      () =>
        new Promise((resolve) => {
          // Never resolve to keep loading state
          setTimeout(() => resolve({ entries: [], totalCount: 0 }), 10000);
        })
    );

    render(<ActivityWidget />);
    expect(screen.getByText(/loading activity/i)).toBeInTheDocument();
  });

  it('should render activity entries from API response', async () => {
    const mockData = {
      entries: [
        {
          id: '1',
          timestamp: new Date().toISOString(),
          type: 'publish',
          description: 'Published content',
          user: 'admin',
          contentName: 'Homepage',
        },
        {
          id: '2',
          timestamp: new Date(Date.now() - 3600000).toISOString(),
          type: 'revise',
          description: 'Revised content',
          user: 'editor',
          contentName: 'About Page',
        },
      ],
      totalCount: 2,
    };

    vi.mocked(clientModule.get).mockResolvedValue(mockData);

    render(<ActivityWidget />);

    await waitFor(() => {
      expect(screen.getByText('Published content')).toBeInTheDocument();
      expect(screen.getByText('Revised content')).toBeInTheDocument();
      expect(screen.getByText('Homepage')).toBeInTheDocument();
      expect(screen.getByText('About Page')).toBeInTheDocument();
    });
  });

  it('should render error message on API failure', async () => {
    const error = new Error('Failed to load activity');
    vi.mocked(clientModule.get).mockRejectedValue(error);

    render(<ActivityWidget />);

    await waitFor(() => {
      expect(screen.getByText(/failed to load activity/i)).toBeInTheDocument();
    });
  });

  it('should render no recent activity message when list is empty', async () => {
    const mockData = {
      entries: [],
      totalCount: 0,
    };

    vi.mocked(clientModule.get).mockResolvedValue(mockData);

    render(<ActivityWidget />);

    await waitFor(() => {
      expect(screen.getByText(/no recent activity/i)).toBeInTheDocument();
    });
  });

  it('should call API with max entries parameter', async () => {
    const mockData = {
      entries: [],
      totalCount: 0,
    };

    vi.mocked(clientModule.get).mockResolvedValue(mockData);

    render(<ActivityWidget maxEntries={20} />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledWith(
        '/services/activity/contentactivity?limit=20'
      );
    });
  });

  it('should render custom title', async () => {
    const mockData = {
      entries: [],
      totalCount: 0,
    };

    vi.mocked(clientModule.get).mockResolvedValue(mockData);

    render(<ActivityWidget title="System Activity" />);

    await waitFor(() => {
      expect(screen.getByText('System Activity')).toBeInTheDocument();
    });
  });

  it('should format time correctly', async () => {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 3600000);

    const mockData = {
      entries: [
        {
          id: '1',
          timestamp: oneHourAgo.toISOString(),
          type: 'publish',
          description: 'Published content',
        },
      ],
      totalCount: 1,
    };

    vi.mocked(clientModule.get).mockResolvedValue(mockData);

    render(<ActivityWidget />);

    await waitFor(() => {
      expect(screen.getByText(/1h ago/)).toBeInTheDocument();
    });
  });
});
