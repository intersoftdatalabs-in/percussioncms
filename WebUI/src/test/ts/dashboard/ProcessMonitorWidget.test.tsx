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
import { ProcessMonitorWidget } from '@/dashboard';
import * as clientModule from '@/api/client';

// Mock the API client
vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('ProcessMonitorWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should render the widget title', () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({ monitors: [] });

    render(<ProcessMonitorWidget title="System Monitors" />);

    expect(screen.getByText('System Monitors')).toBeInTheDocument();
    mockGet.mockRestore();
  });

  it('should use default title when not provided', () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({ monitors: [] });

    render(<ProcessMonitorWidget />);

    expect(screen.getByText('Process Monitor')).toBeInTheDocument();
    mockGet.mockRestore();
  });

  it('should display loading state initially', () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockImplementation(
      () => new Promise(() => {}), // Never resolves
    );

    render(<ProcessMonitorWidget />);

    expect(screen.getByText('Loading process monitor...')).toBeInTheDocument();
    mockGet.mockRestore();
  });

  it('should display error message on API failure', async () => {
    const mockGet = vi
      .spyOn(clientModule, 'get')
      .mockRejectedValue(new Error('API Error: Connection failed'));

    render(<ProcessMonitorWidget />);

    await waitFor(() => {
      expect(screen.getByText(/Error: API Error: Connection failed/)).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should display "No monitors available" when empty', async () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({ monitors: [] });

    render(<ProcessMonitorWidget />);

    await waitFor(() => {
      expect(screen.getByText('No monitors available')).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should display monitors list correctly', async () => {
    const mockMonitors = {
      monitors: [
        {
          designator: 'monitor_1',
          name: 'Indexing Service',
          status: 'running',
          message: 'Processing 1500 items',
        },
        {
          designator: 'monitor_2',
          name: 'Archive Service',
          status: 'paused',
          message: 'Waiting for approval',
        },
        {
          designator: 'monitor_3',
          name: 'Search Index',
          status: 'error',
          message: 'Connection timeout',
        },
      ],
    };

    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue(mockMonitors);

    render(<ProcessMonitorWidget />);

    await waitFor(() => {
      expect(screen.getByText('Indexing Service')).toBeInTheDocument();
      expect(screen.getByText('Archive Service')).toBeInTheDocument();
      expect(screen.getByText('Search Index')).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should display monitor status and message', async () => {
    const mockMonitors = {
      monitors: [
        {
          designator: 'monitor_1',
          name: 'Test Monitor',
          status: 'running',
          message: 'Processing items',
        },
      ],
    };

    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue(mockMonitors);

    render(<ProcessMonitorWidget />);

    await waitFor(() => {
      expect(screen.getByText('running')).toBeInTheDocument();
      expect(screen.getByText('Processing items')).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should handle response with "monitor" property instead of "monitors"', async () => {
    const mockMonitors = {
      monitor: [
        {
          designator: 'monitor_1',
          name: 'Alternative Format Monitor',
          status: 'active',
        },
      ],
    };

    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue(mockMonitors);

    render(<ProcessMonitorWidget />);

    await waitFor(() => {
      expect(screen.getByText('Alternative Format Monitor')).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should display correct status icons', async () => {
    const mockMonitors = {
      monitors: [
        {designator: 'monitor_running', name: 'Running', status: 'running'},
        {designator: 'monitor_paused', name: 'Paused', status: 'paused'},
        {designator: 'monitor_error', name: 'Error', status: 'error'},
      ],
    };

    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue(mockMonitors);

    render(<ProcessMonitorWidget />);

    await waitFor(() => {
      const monitorElements = screen.getAllByText(/Running|Paused|Error/);
      expect(monitorElements.length).toBeGreaterThanOrEqual(3);
    });

    mockGet.mockRestore();
  });

  it('should call API with correct endpoint', async () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({monitors: []});

    render(<ProcessMonitorWidget />);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/services/monitor/all');
    });

    mockGet.mockRestore();
  });

  it('should set up refresh interval with default duration', async () => {
    vi.useFakeTimers();
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({monitors: []});

    const {unmount} = render(<ProcessMonitorWidget refreshInterval={5000} />);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledTimes(1);
    });

    // Fast-forward by refresh interval
    vi.advanceTimersByTime(5000);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledTimes(2);
    });

    unmount();
    mockGet.mockRestore();
  });

  it('should not set up refresh interval when refreshInterval is 0', async () => {
    vi.useFakeTimers();
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({monitors: []});

    const {unmount} = render(<ProcessMonitorWidget refreshInterval={0} />);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledTimes(1);
    });

    // Advance time significantly
    vi.advanceTimersByTime(10000);

    // Should still only be called once since refreshInterval is 0
    expect(mockGet).toHaveBeenCalledTimes(1);

    unmount();
    mockGet.mockRestore();
  });

  it('should handle monitors without designator by using index', async () => {
    const mockMonitors = {
      monitors: [
        {
          name: 'Monitor Without Designator',
          status: 'running',
        },
      ],
    };

    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue(mockMonitors);

    render(<ProcessMonitorWidget />);

    await waitFor(() => {
      expect(screen.getByText('Monitor Without Designator')).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should display generic monitor title when name is missing', async () => {
    const mockMonitors = {
      monitors: [
        {
          designator: 'monitor_1',
          status: 'running',
        },
      ],
    };

    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue(mockMonitors);

    render(<ProcessMonitorWidget />);

    await waitFor(() => {
      expect(screen.getByText('monitor_1')).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should clear interval on unmount', async () => {
    vi.useFakeTimers();
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({monitors: []});

    const {unmount} = render(<ProcessMonitorWidget refreshInterval={5000} />);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledTimes(1);
    });

    unmount();

    // Advance time after unmount
    vi.advanceTimersByTime(5000);

    // Should still only be called once since interval was cleared
    expect(mockGet).toHaveBeenCalledTimes(1);

    mockGet.mockRestore();
  });
});
