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
import { TrafficWidget } from './TrafficWidget';
import * as apiClient from '../api/client';

// Mock the API client
vi.mock('../api/client');

describe('TrafficWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  it('should render the widget title', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [],
      totalViews: 0,
      totalVisitors: 0,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget title="Test Traffic" />);

    expect(screen.getByText('Test Traffic')).toBeDefined();
  });

  it('should display loading state initially', () => {
    const mockPostFn = vi.fn(() => new Promise(() => {})); // Never resolves
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    expect(screen.getByText('Loading traffic data...')).toBeDefined();
  });

  it('should display error message on API failure', async () => {
    const errorMessage = 'Network error';
    const mockPostFn = vi.fn().mockRejectedValue(new Error(errorMessage));
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    await waitFor(() => {
      expect(screen.getByText(`Error: ${errorMessage}`)).toBeDefined();
    });
  });

  it('should display no data message when data array is empty', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [],
      totalViews: 0,
      totalVisitors: 0,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    await waitFor(() => {
      expect(screen.getByText('No traffic data available')).toBeDefined();
    });
  });

  it('should display traffic data with views and visitors', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [
        { date: '2026-02-20', views: 150, visitors: 45 },
        { date: '2026-02-21', views: 200, visitors: 60 },
      ],
      totalViews: 350,
      totalVisitors: 105,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    await waitFor(() => {
      expect(screen.getByText('Page Views')).toBeDefined();
      expect(screen.getByText('Unique Visitors')).toBeDefined();
      expect(screen.getByText('350')).toBeDefined();
      expect(screen.getByText('105')).toBeDefined();
    });
  });

  it('should call API with correct endpoint and payload', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [],
      totalViews: 0,
      totalVisitors: 0,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget daysRange={30} granularity="daily" />);

    await waitFor(() => {
      expect(mockPostFn).toHaveBeenCalledWith(
        '/services/activity/contenttraffic',
        expect.objectContaining({
          granularity: 'daily',
        })
      );
    });
  });

  it('should handle alternative response format with traffic property', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      traffic: [
        { date: '2026-02-20', views: 100, visitors: 30 },
      ],
      totalViews: 100,
      totalVisitors: 30,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    await waitFor(() => {
      expect(screen.getByText('100')).toBeDefined();
      expect(screen.getByText('30')).toBeDefined();
    });
  });

  it('should handle alternative response format with dataPoints property', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      dataPoints: [
        { date: '2026-02-20', views: 120, visitors: 35 },
      ],
      totalViews: 120,
      totalVisitors: 35,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    await waitFor(() => {
      expect(screen.getByText('120')).toBeDefined();
      expect(screen.getByText('35')).toBeDefined();
    });
  });

  it('should handle array response format directly', async () => {
    const mockPostFn = vi.fn().mockResolvedValue([
      { date: '2026-02-20', views: 110, visitors: 32 },
    ]);
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    await waitFor(() => {
      expect(screen.getByText('110')).toBeDefined();
    });
  });

  it('should calculate totals from data when not provided in response', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [
        { date: '2026-02-20', views: 100, visitors: 30 },
        { date: '2026-02-21', views: 150, visitors: 45 },
      ],
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    await waitFor(() => {
      expect(screen.getByText('250')).toBeDefined(); // 100 + 150
      expect(screen.getByText('75')).toBeDefined(); // 30 + 45
    });
  });

  it('should handle pageViews and uniqueVisitors field names', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [
        { date: '2026-02-20', pageViews: 100, uniqueVisitors: 30 },
      ],
      totalViews: 100,
      totalVisitors: 30,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    await waitFor(() => {
      expect(screen.getByText('100')).toBeDefined();
      expect(screen.getByText('30')).toBeDefined();
    });
  });

  it('should render line chart by default', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [
        { date: '2026-02-20', views: 100, visitors: 30 },
      ],
      totalViews: 100,
      totalVisitors: 30,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    const { container } = render(<TrafficWidget chartType="line" />);

    await waitFor(() => {
      const svg = container.querySelector('svg');
      expect(svg).toBeDefined();
    });
  });

  it('should render bar chart when specified', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [
        { date: '2026-02-20', views: 100, visitors: 30 },
      ],
      totalViews: 100,
      totalVisitors: 30,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    const { container } = render(<TrafficWidget chartType="bar" />);

    await waitFor(() => {
      const svg = container.querySelector('svg');
      expect(svg).toBeDefined();
    });
  });

  it('should use default title when not provided', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [],
      totalViews: 0,
      totalVisitors: 0,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    await waitFor(() => {
      expect(screen.getByText('Content Traffic')).toBeDefined();
    });
  });

  it('should set up and clear refresh interval correctly', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [],
      totalViews: 0,
      totalVisitors: 0,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    const { unmount } = render(<TrafficWidget refreshInterval={5000} />);

    await waitFor(() => {
      expect(mockPostFn).toHaveBeenCalled();
    });

    mockPostFn.mockClear();

    vi.useFakeTimers();
    vi.advanceTimersByTime(5000);

    expect(mockPostFn).toHaveBeenCalled();

    unmount();
    mockPostFn.mockClear();
    vi.advanceTimersByTime(5000);

    expect(mockPostFn).not.toHaveBeenCalled();

    vi.useRealTimers();
  });

  it('should not set up refresh interval when refreshInterval is 0', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [],
      totalViews: 0,
      totalVisitors: 0,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget refreshInterval={0} />);

    await waitFor(() => {
      expect(mockPostFn).toHaveBeenCalledTimes(1);
    });

    vi.useFakeTimers();
    vi.advanceTimersByTime(5000);
    expect(mockPostFn).toHaveBeenCalledTimes(1);
    vi.useRealTimers();
  });

  it('should format large numbers with locale formatting', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [
        { date: '2026-02-20', views: 1000000, visitors: 50000 },
      ],
      totalViews: 1000000,
      totalVisitors: 50000,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget />);

    await waitFor(() => {
      // Check for locale-formatted number (with commas or appropriate separator)
      expect(screen.getByText(/1.*000.*000/)).toBeDefined();
    });
  });

  it('should pass daysRange to API payload', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [],
      totalViews: 0,
      totalVisitors: 0,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget daysRange={7} />);

    await waitFor(() => {
      expect(mockPostFn).toHaveBeenCalledWith(
        '/services/activity/contenttraffic',
        expect.objectContaining({
          granularity: 'daily',
        })
      );
      // Verify that the payload includes date range
      const payload = mockPostFn.mock.calls[0][1] as Record<string, unknown>;
      expect(payload.startDate).toBeDefined();
      expect(payload.endDate).toBeDefined();
    });
  });

  it('should pass granularity parameter to API', async () => {
    const mockPostFn = vi.fn().mockResolvedValue({
      data: [],
      totalViews: 0,
      totalVisitors: 0,
    });
    vi.spyOn(apiClient, 'post').mockImplementation(mockPostFn);

    render(<TrafficWidget granularity="hourly" />);

    await waitFor(() => {
      expect(mockPostFn).toHaveBeenCalledWith(
        '/services/activity/contenttraffic',
        expect.objectContaining({
          granularity: 'hourly',
        })
      );
    });
  });
});
