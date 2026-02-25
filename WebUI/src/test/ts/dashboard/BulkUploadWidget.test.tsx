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

import React from 'react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BulkUploadWidget } from './BulkUploadWidget';
import * as apiClient from '../api/client';

// Mock the API client
vi.mock('../api/client');

describe('BulkUploadWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  it('should render the widget title', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget title="Test Upload Status" />);

    expect(screen.getByText('Test Upload Status')).toBeDefined();
  });

  it('should display loading state initially', () => {
    const mockGetFn = vi.fn(() => new Promise(() => {})); // Never resolves
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    expect(screen.getByText('Loading bulk upload status...')).toBeDefined();
  });

  it('should display error message on API failure', async () => {
    const errorMessage = 'Network error';
    const mockGetFn = vi.fn().mockRejectedValue(new Error(errorMessage));
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      expect(screen.getByText(`Error: ${errorMessage}`)).toBeDefined();
    });
  });

  it('should display no jobs message when jobs array is empty', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      expect(screen.getByText('No bulk upload jobs')).toBeDefined();
    });
  });

  it('should display bulk upload jobs with name and status', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [
        {
          id: '1',
          name: 'Images Upload',
          status: 'completed',
          filesCount: 5,
          successCount: 5,
          failureCount: 0,
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      expect(screen.getByText('Images Upload')).toBeDefined();
      expect(screen.getByText(/5 files/)).toBeDefined();
      expect(screen.getByText(/5 uploaded/)).toBeDefined();
    });
  });

  it('should display correct status icon for completed status', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [
        {
          id: '1',
          name: 'Test Upload',
          status: 'completed',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      // The ✅ emoji should be visible
      expect(screen.getByText(/✅/)).toBeDefined();
    });
  });

  it('should display correct status icon for in-progress status', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [
        {
          id: '1',
          name: 'Test Upload',
          status: 'in-progress',
          progress: 50,
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      // The ⚙️ emoji should be visible
      expect(screen.getByText(/⚙️/)).toBeDefined();
    });
  });

  it('should display correct status icon for pending status', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [
        {
          id: '1',
          name: 'Test Upload',
          status: 'pending',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      // The ⏳ emoji should be visible
      expect(screen.getByText(/⏳/)).toBeDefined();
    });
  });

  it('should display correct status icon for failed status', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [
        {
          id: '1',
          name: 'Test Upload',
          status: 'failed',
          filesCount: 3,
          successCount: 1,
          failureCount: 2,
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      // The ❌ emoji should be visible
      expect(screen.getByText(/❌/)).toBeDefined();
      expect(screen.getByText(/2 failed/)).toBeDefined();
    });
  });

  it('should display progress percentage for in-progress jobs', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [
        {
          id: '1',
          name: 'Test Upload',
          status: 'in-progress',
          progress: 75,
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      expect(screen.getByText('75%')).toBeDefined();
    });
  });

  it('should call API with correct endpoint', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      expect(mockGetFn).toHaveBeenCalledWith('/services/bulk-upload/jobs');
    });
  });

  it('should handle alternative response format with recentJobs', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      recentJobs: [
        {
          id: '1',
          name: 'Recent Upload',
          status: 'completed',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      expect(screen.getByText('Recent Upload')).toBeDefined();
    });
  });

  it('should handle alternative response format with activeJob', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      activeJob: {
        id: '1',
        name: 'Active Upload',
        status: 'in-progress',
        progress: 50,
      },
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      expect(screen.getByText('Active Upload')).toBeDefined();
    });
  });

  it('should respect maxJobs prop to limit displayed jobs', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [
        { id: '1', name: 'Job 1', status: 'completed' },
        { id: '2', name: 'Job 2', status: 'completed' },
        { id: '3', name: 'Job 3', status: 'completed' },
        { id: '4', name: 'Job 4', status: 'completed' },
        { id: '5', name: 'Job 5', status: 'completed' },
        { id: '6', name: 'Job 6', status: 'completed' },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget maxJobs={3} />);

    await waitFor(() => {
      expect(screen.getByText('Job 1')).toBeDefined();
      expect(screen.getByText('Job 3')).toBeDefined();
      expect(screen.queryByText('Job 6')).toBeNull();
    });
  });

  it('should display target folder information when available', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      jobs: [
        {
          id: '1',
          name: 'Test Upload',
          status: 'completed',
          targetFolder: '/content/images',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      expect(screen.getByText(/\/content\/images/)).toBeDefined();
    });
  });

  it('should set up and clear refresh interval correctly', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({ jobs: [] });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    const { unmount } = render(<BulkUploadWidget refreshInterval={5000} />);

    await waitFor(() => {
      expect(mockGetFn).toHaveBeenCalled();
    });

    // Clear previous calls to count new ones from interval
    mockGetFn.mockClear();

    // Fast-forward time by refresh interval
    vi.useFakeTimers();
    vi.advanceTimersByTime(5000);

    // Should have been called again from interval
    expect(mockGetFn).toHaveBeenCalled();

    // Unmount and verify interval is cleaned up
    unmount();
    mockGetFn.mockClear();
    vi.advanceTimersByTime(5000);

    // Should not be called after unmount
    expect(mockGetFn).not.toHaveBeenCalled();

    vi.useRealTimers();
  });

  it('should handle array response format directly', async () => {
    const mockGetFn = vi.fn().mockResolvedValue([
      {
        id: '1',
        name: 'Array Format Job',
        status: 'completed',
      },
    ]);
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      expect(screen.getByText('Array Format Job')).toBeDefined();
    });
  });

  it('should use default title when not provided', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({ jobs: [] });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget />);

    await waitFor(() => {
      expect(screen.getByText('Bulk Upload Status')).toBeDefined();
    });
  });

  it('should not set up refresh interval when refreshInterval is 0', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({ jobs: [] });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<BulkUploadWidget refreshInterval={0} />);

    await waitFor(() => {
      expect(mockGetFn).toHaveBeenCalledTimes(1);
    });

    // Wait and verify no additional calls
    vi.useFakeTimers();
    vi.advanceTimersByTime(5000);
    expect(mockGetFn).toHaveBeenCalledTimes(1);
    vi.useRealTimers();
  });
});
