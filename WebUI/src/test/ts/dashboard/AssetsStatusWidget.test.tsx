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
import { AssetsStatusWidget } from '@/dashboard';
import * as clientModule from '@/api/client';

// Mock the API client
vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('AssetsStatusWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should render the widget title', () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({ workflows: [] });

    render(<AssetsStatusWidget title="Custom Asset Status" />);

    expect(screen.getByText('Custom Asset Status')).toBeInTheDocument();
    mockGet.mockRestore();
  });

  it('should use default title when not provided', () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({ workflows: [] });

    render(<AssetsStatusWidget />);

    expect(screen.getByText('Assets By Status')).toBeInTheDocument();
    mockGet.mockRestore();
  });

  it('should display loading state initially', () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockImplementation(
      () => new Promise(() => {}), // Never resolves
    );

    render(<AssetsStatusWidget />);

    expect(screen.getByText('Loading asset status...')).toBeInTheDocument();
    mockGet.mockRestore();
  });

  it('should display error message on API failure', async () => {
    const mockGet = vi
      .spyOn(clientModule, 'get')
      .mockRejectedValue(new Error('Service unavailable'));

    render(<AssetsStatusWidget />);

    await waitFor(() => {
      expect(screen.getByText(/Error: Service unavailable/)).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should display "No asset workflow status available" when empty', async () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({ workflows: [] });

    render(<AssetsStatusWidget />);

    await waitFor(() => {
      expect(screen.getByText('No asset workflow status available')).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should display workflow statuses with counts', async () => {
    const mockData = {
      workflows: [
        { name: 'Draft', count: 45 },
        { name: 'Review', count: 12 },
        { name: 'Published', count: 238 },
      ],
    };

    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue(mockData);

    render(<AssetsStatusWidget />);

    await waitFor(() => {
      expect(screen.getByText('Draft')).toBeInTheDocument();
      expect(screen.getByText('Review')).toBeInTheDocument();
      expect(screen.getByText('Published')).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should calculate and display total assets', async () => {
    const mockData = {
      workflows: [
        { name: 'Draft', count: 50 },
        { name: 'Published', count: 200 },
      ],
    };

    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue(mockData);

    render(<AssetsStatusWidget />);

    await waitFor(() => {
      expect(screen.getByText(/Total Assets: 250/)).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should calculate percentages correctly', async () => {
    const mockData = {
      workflows: [
        { name: 'Draft', count: 25 },
        { name: 'Published', count: 75 },
      ],
    };

    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue(mockData);

    render(<AssetsStatusWidget />);

    await waitFor(() => {
      expect(screen.getByText('25%')).toBeInTheDocument();
      expect(screen.getByText('75%')).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should display workflow icons based on status name', async () => {
    const mockData = {
      workflows: [
        { name: 'Draft', count: 10 },
        { name: 'Approved', count: 5 },
        { name: 'Archived', count: 2 },
      ],
    };

    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue(mockData);

    render(<AssetsStatusWidget />);

    await waitFor(() => {
      const elements = screen.getAllByText(/Draft|Approved|Archived/);
      expect(elements.length).toBeGreaterThanOrEqual(3);
    });

    mockGet.mockRestore();
  });

  it('should handle array response format', async () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue([
      { name: 'Array Format Status', count: 15 },
    ]);

    render(<AssetsStatusWidget />);

    await waitFor(() => {
      expect(screen.getByText('Array Format Status')).toBeInTheDocument();
    });

    mockGet.mockRestore();
  });

  it('should call API with correct endpoint', async () => {
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({ workflows: [] });

    render(<AssetsStatusWidget />);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/services/asset/workflow-status');
    });

    mockGet.mockRestore();
  });

  it('should set up refresh interval', async () => {
    vi.useFakeTimers();
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({ workflows: [] });

    const { unmount } = render(<AssetsStatusWidget refreshInterval={3000} />);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledTimes(1);
    });

    // Advance by refresh interval
    vi.advanceTimersByTime(3000);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledTimes(2);
    });

    unmount();
    mockGet.mockRestore();
  });

  it('should not refresh when refreshInterval is 0', async () => {
    vi.useFakeTimers();
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({ workflows: [] });

    const { unmount } = render(<AssetsStatusWidget refreshInterval={0} />);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledTimes(1);
    });

    vi.advanceTimersByTime(10000);

    expect(mockGet).toHaveBeenCalledTimes(1);

    unmount();
    mockGet.mockRestore();
  });

  it('should cleanup interval on unmount', async () => {
    vi.useFakeTimers();
    const mockGet = vi.spyOn(clientModule, 'get').mockResolvedValue({ workflows: [] });

    const { unmount } = render(<AssetsStatusWidget refreshInterval={3000} />);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledTimes(1);
    });

    unmount();

    vi.advanceTimersByTime(3000);

    expect(mockGet).toHaveBeenCalledTimes(1);

    mockGet.mockRestore();
  });
});
