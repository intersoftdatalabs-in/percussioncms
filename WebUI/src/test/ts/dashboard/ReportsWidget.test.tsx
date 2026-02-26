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
import { ReportsWidget } from './ReportsWidget';
import * as apiClient from '../api/client';

// Mock the API client
vi.mock('../api/client');

describe('ReportsWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  it('should render the widget title', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget title="Test Reports" />);

    expect(screen.getByText('Test Reports')).toBeDefined();
  });

  it('should display loading state initially', () => {
    const mockGetFn = vi.fn(() => new Promise(() => {})); // Never resolves
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    expect(screen.getByText('Loading reports...')).toBeDefined();
  });

  it('should display error message on API failure', async () => {
    const errorMessage = 'API Error';
    const mockGetFn = vi.fn().mockRejectedValue(new Error(errorMessage));
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText(`Error: ${errorMessage}`)).toBeDefined();
    });
  });

  it('should display no reports message when reports array is empty', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText('No reports available')).toBeDefined();
    });
  });

  it('should display reports with name and type', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [
        {
          id: '1',
          name: 'Content Traffic Report',
          type: 'Traffic',
          category: 'Traffic',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Content Traffic Report')).toBeDefined();
      expect(screen.getByText('Traffic')).toBeDefined();
    });
  });

  it('should display report description when available', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [
        {
          id: '1',
          name: 'Test Report',
          type: 'Analytics',
          description: 'Analyze content traffic patterns',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Analyze content traffic patterns')).toBeDefined();
    });
  });

  it('should display correct icon for traffic category', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [
        {
          id: '1',
          name: 'Test Report',
          type: 'Traffic',
          category: 'Traffic',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText(/📊/)).toBeDefined();
    });
  });

  it('should display correct icon for content category', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [
        {
          id: '1',
          name: 'Test Report',
          type: 'Content',
          category: 'Content',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText(/📄/)).toBeDefined();
    });
  });

  it('should display correct icon for asset category', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [
        {
          id: '1',
          name: 'Test Report',
          type: 'Assets',
          category: 'Asset',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText(/🖼️/)).toBeDefined();
    });
  });

  it('should display last run date when available', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [
        {
          id: '1',
          name: 'Test Report',
          type: 'Custom',
          lastRun: new Date().toISOString(),
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText(/Last run:/)).toBeDefined();
    });
  });

  it('should call API with correct endpoint', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(mockGetFn).toHaveBeenCalledWith('/services/reports/list');
    });
  });

  it('should handle alternative response format with availableReports', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      availableReports: [
        {
          id: '1',
          name: 'Available Report',
          type: 'Analytics',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Available Report')).toBeDefined();
    });
  });

  it('should handle alternative response format with items', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      items: [
        {
          id: '1',
          name: 'Item Report',
          type: 'Report',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Item Report')).toBeDefined();
    });
  });

  it('should handle array response format directly', async () => {
    const mockGetFn = vi.fn().mockResolvedValue([
      {
        id: '1',
        name: 'Direct Array Report',
        type: 'Custom',
      },
    ]);
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Direct Array Report')).toBeDefined();
    });
  });

  it('should respect maxReports prop to limit displayed reports', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [
        { id: '1', name: 'Report 1', type: 'Type1' },
        { id: '2', name: 'Report 2', type: 'Type2' },
        { id: '3', name: 'Report 3', type: 'Type3' },
        { id: '4', name: 'Report 4', type: 'Type4' },
        { id: '5', name: 'Report 5', type: 'Type5' },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget maxReports={3} />);

    await waitFor(() => {
      expect(screen.getByText('Report 1')).toBeDefined();
      expect(screen.getByText('Report 3')).toBeDefined();
      expect(screen.queryByText('Report 5')).toBeNull();
    });
  });

  it('should use default title when not provided', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Available Reports')).toBeDefined();
    });
  });

  it('should set up and clear refresh interval correctly', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({ reports: [] });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    const { unmount } = render(<ReportsWidget refreshInterval={5000} />);

    await waitFor(() => {
      expect(mockGetFn).toHaveBeenCalled();
    });

    mockGetFn.mockClear();

    vi.useFakeTimers();
    vi.advanceTimersByTime(5000);

    expect(mockGetFn).toHaveBeenCalled();

    unmount();
    mockGetFn.mockClear();
    vi.advanceTimersByTime(5000);

    expect(mockGetFn).not.toHaveBeenCalled();

    vi.useRealTimers();
  });

  it('should not set up refresh interval when refreshInterval is 0', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({ reports: [] });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget refreshInterval={0} />);

    await waitFor(() => {
      expect(mockGetFn).toHaveBeenCalledTimes(1);
    });

    vi.useFakeTimers();
    vi.advanceTimersByTime(5000);
    expect(mockGetFn).toHaveBeenCalledTimes(1);
    vi.useRealTimers();
  });

  it('should display multiple reports with proper spacing', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [
        { id: '1', name: 'Report One', type: 'Traffic' },
        { id: '2', name: 'Report Two', type: 'Analytics' },
        { id: '3', name: 'Report Three', type: 'SEO' },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Report One')).toBeDefined();
      expect(screen.getByText('Report Two')).toBeDefined();
      expect(screen.getByText('Report Three')).toBeDefined();
    });
  });

  it('should format report type from camelCase to readable text', async () => {
    const mockGetFn = vi.fn().mockResolvedValue({
      reports: [
        {
          id: '1',
          name: 'Test Report',
          type: 'ContentTraffic',
        },
      ],
    });
    vi.spyOn(apiClient, 'get').mockImplementation(mockGetFn);

    render(<ReportsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Content Traffic')).toBeDefined();
    });
  });
});
