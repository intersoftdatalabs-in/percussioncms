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
import { WorkflowStatusWidget } from '@/dashboard';
import * as clientModule from '@/api/client';

// Mock the API client
vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('WorkflowStatusWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementation(
      () =>
        new Promise((resolve) => {
          // Never resolve to keep loading state
          setTimeout(() => resolve({ items: [], totalCount: 0, lastUpdated: '' }), 10000);
        })
    );

    render(<WorkflowStatusWidget />);
    expect(screen.getByText(/loading workflow status/i)).toBeInTheDocument();
  });

  it('should render workflow items from API response', async () => {
    const mockData = {
      items: [
        {
          id: '1',
          name: 'Content Review',
          state: 'Active',
          count: 5,
        },
        {
          id: '2',
          name: 'Site Publishing',
          state: 'Pending',
          count: 3,
        },
      ],
      totalCount: 2,
      lastUpdated: new Date().toISOString(),
    };

    vi.mocked(clientModule.get).mockResolvedValue(mockData);

    render(<WorkflowStatusWidget />);

    await waitFor(() => {
      expect(screen.getByText('Content Review')).toBeInTheDocument();
      expect(screen.getByText('Site Publishing')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
    });
  });

  it('should render error message on API failure', async () => {
    const error = new Error('Failed to fetch workflow status');
    vi.mocked(clientModule.get).mockRejectedValue(error);

    render(<WorkflowStatusWidget />);

    await waitFor(() => {
      expect(screen.getByText(/failed to fetch workflow status/i)).toBeInTheDocument();
    });
  });

  it('should render no active workflows message when list is empty', async () => {
    const mockData = {
      items: [],
      totalCount: 0,
      lastUpdated: new Date().toISOString(),
    };

    vi.mocked(clientModule.get).mockResolvedValue(mockData);

    render(<WorkflowStatusWidget />);

    await waitFor(() => {
      expect(screen.getByText(/no active workflows/i)).toBeInTheDocument();
    });
  });

  it('should call API with correct endpoint', async () => {
    const mockData = {
      items: [],
      totalCount: 0,
      lastUpdated: new Date().toISOString(),
    };

    vi.mocked(clientModule.get).mockResolvedValue(mockData);

    render(<WorkflowStatusWidget />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledWith(
        '/services/dashboardmanagement/gadget/workflow-status'
      );
    });
  });

  it('should render custom title', async () => {
    const mockData = {
      items: [],
      totalCount: 0,
      lastUpdated: new Date().toISOString(),
    };

    vi.mocked(clientModule.get).mockResolvedValue(mockData);

    render(<WorkflowStatusWidget title="My Custom Title" />);

    await waitFor(() => {
      expect(screen.getByText('My Custom Title')).toBeInTheDocument();
    });
  });
});
