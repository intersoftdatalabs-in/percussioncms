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

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { EffectivenessWidget } from '@/dashboard';
import * as clientModule from '@/api/client';

// Mock the API client
vi.mock('@/api/client', () => ({
  post: vi.fn(),
}));

describe('EffectivenessWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should render the widget title', () => {
    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue({ metrics: [] });

    render(<EffectivenessWidget title="Custom Metrics" />);

    expect(screen.getByText('Custom Metrics')).toBeInTheDocument();
    mockPost.mockRestore();
  });

  it('should use default title when not provided', () => {
    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue({ metrics: [] });

    render(<EffectivenessWidget />);

    expect(screen.getByText('Effectiveness Metrics')).toBeInTheDocument();
    mockPost.mockRestore();
  });

  it('should display loading state initially', () => {
    const mockPost = vi.spyOn(clientModule, 'post').mockImplementation(
      () => new Promise(() => {}), // Never resolves
    );

    render(<EffectivenessWidget />);

    expect(screen.getByText('Loading effectiveness metrics...')).toBeInTheDocument();
    mockPost.mockRestore();
  });

  it('should display error message on API failure', async () => {
    const mockPost = vi
      .spyOn(clientModule, 'post')
      .mockRejectedValue(new Error('API failed'));

    render(<EffectivenessWidget />);

    await waitFor(() => {
      expect(screen.getByText(/Error: API failed/)).toBeInTheDocument();
    });

    mockPost.mockRestore();
  });

  it('should display "No effectiveness metrics available" when empty', async () => {
    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue({ metrics: [] });

    render(<EffectivenessWidget />);

    await waitFor(() => {
      expect(screen.getByText('No effectiveness metrics available')).toBeInTheDocument();
    });

    mockPost.mockRestore();
  });

  it('should display metrics correctly', async () => {
    const mockMetrics = {
      metrics: [
        {
          name: 'Page Load Time',
          value: 2.4,
          unit: 's',
          trend: 'down',
          percentage: 95,
          target: 2.0,
        },
        {
          name: 'Content Publish Rate',
          value: 156,
          unit: 'items/day',
          trend: 'up',
          percentage: 102,
          target: 150,
        },
        {
          name: 'System Uptime',
          value: 99.8,
          unit: '%',
          trend: 'stable',
          percentage: 99.8,
        },
      ],
    };

    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue(mockMetrics);

    render(<EffectivenessWidget />);

    await waitFor(() => {
      expect(screen.getByText('Page Load Time')).toBeInTheDocument();
      expect(screen.getByText('Content Publish Rate')).toBeInTheDocument();
      expect(screen.getByText('System Uptime')).toBeInTheDocument();
    });

    mockPost.mockRestore();
  });

  it('should display metric values and units', async () => {
    const mockMetrics = {
      metrics: [
        {
          name: 'Response Time',
          value: 145,
          unit: 'ms',
          trend: 'down',
          percentage: 92,
        },
      ],
    };

    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue(mockMetrics);

    render(<EffectivenessWidget />);

    await waitFor(() => {
      expect(screen.getByText('145')).toBeInTheDocument();
      expect(screen.getByText('ms')).toBeInTheDocument();
    });

    mockPost.mockRestore();
  });

  it('should display percentage and target values', async () => {
    const mockMetrics = {
      metrics: [
        {
          name: 'Conversion Rate',
          value: 3.2,
          percentage: 85,
          target: 3.5,
        },
      ],
    };

    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue(mockMetrics);

    render(<EffectivenessWidget />);

    await waitFor(() => {
      expect(screen.getByText('85%')).toBeInTheDocument();
      expect(screen.getByText(/Target: 3.5/)).toBeInTheDocument();
    });

    mockPost.mockRestore();
  });

  it('should handle array response format', async () => {
    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue([
      {
        name: 'Array Format Metric',
        value: 42,
        trend: 'up',
      },
    ]);

    render(<EffectivenessWidget />);

    await waitFor(() => {
      expect(screen.getByText('Array Format Metric')).toBeInTheDocument();
    });

    mockPost.mockRestore();
  });

  it('should call POST endpoint with correct path', async () => {
    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue({ metrics: [] });

    render(<EffectivenessWidget />);

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/services/activity/effectiveness', {});
    });

    mockPost.mockRestore();
  });

  it('should set up refresh interval', async () => {
    vi.useFakeTimers();
    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue({ metrics: [] });

    const { unmount } = render(<EffectivenessWidget refreshInterval={5000} />);

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    // Fast-forward by refresh interval
    vi.advanceTimersByTime(5000);

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledTimes(2);
    });

    unmount();
    mockPost.mockRestore();
  });

  it('should not set up refresh interval when refreshInterval is 0', async () => {
    vi.useFakeTimers();
    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue({ metrics: [] });

    const { unmount } = render(<EffectivenessWidget refreshInterval={0} />);

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    // Advance time significantly
    vi.advanceTimersByTime(10000);

    // Should still only be called once since refreshInterval is 0
    expect(mockPost).toHaveBeenCalledTimes(1);

    unmount();
    mockPost.mockRestore();
  });

  it('should handle metrics without unit', async () => {
    const mockMetrics = {
      metrics: [
        {
          name: 'Page Views',
          value: 5432,
          percentage: 88,
        },
      ],
    };

    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue(mockMetrics);

    render(<EffectivenessWidget />);

    await waitFor(() => {
      expect(screen.getByText('5432')).toBeInTheDocument();
    });

    mockPost.mockRestore();
  });

  it('should handle metrics without percentage or target', async () => {
    const mockMetrics = {
      metrics: [
        {
          name: 'Simple Metric',
          value: 99,
          trend: 'stable',
        },
      ],
    };

    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue(mockMetrics);

    render(<EffectivenessWidget />);

    await waitFor(() => {
      expect(screen.getByText('Simple Metric')).toBeInTheDocument();
      expect(screen.getByText('99')).toBeInTheDocument();
    });

    mockPost.mockRestore();
  });

  it('should clear interval on unmount', async () => {
    vi.useFakeTimers();
    const mockPost = vi.spyOn(clientModule, 'post').mockResolvedValue({ metrics: [] });

    const { unmount } = render(<EffectivenessWidget refreshInterval={5000} />);

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    unmount();

    // Advance time after unmount
    vi.advanceTimersByTime(5000);

    // Should still only be called once since interval was cleared
    expect(mockPost).toHaveBeenCalledTimes(1);

    mockPost.mockRestore();
  });
});
