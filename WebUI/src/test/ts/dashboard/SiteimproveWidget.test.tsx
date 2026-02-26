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

import { describe, test, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { SiteimproveWidget } from '@/dashboard/SiteimproveWidget';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('SiteimproveWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('should display loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolves
    );

    render(<SiteimproveWidget />);
    screen.getByText('Loading Siteimprove data...');
  });

  test('should display connected status', async () => {
    const mockData = {
      siteimprove: {
        integrated: true,
        accessibility: { level: 'AA', status: 'Good' },
        quality: { score: 85, status: 'Good' },
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<SiteimproveWidget />);

    await waitFor(() => {
      screen.getByText(/Connected/);
    });
  });

  test('should display disconnected status', async () => {
    const mockData = {
      siteimprove: {
        integrated: false,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<SiteimproveWidget />);

    await waitFor(() => {
      screen.getByText(/Not Connected/);
    });
  });

  test('should display accessibility level', async () => {
    const mockData = {
      data: {
        accessibility: { level: 'AAA', status: 'Excellent' },
        integrated: true,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<SiteimproveWidget />);

    await waitFor(() => {
      screen.getByText('AAA');
      screen.getByText(/Excellent/);
    });
  });

  test('should display quality score', async () => {
    const mockData = {
      analytics: {
        quality: { score: 92, status: 'Excellent' },
        integrated: true,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<SiteimproveWidget />);

    await waitFor(() => {
      screen.getByText('Quality Score');
      screen.getByText('92%');
    });
  });

  test('should handle accessibility level A', async () => {
    const mockData = {
      siteimprove: {
        accessibility: { level: 'A', status: 'Passing' },
        integrated: true,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<SiteimproveWidget />);

    await waitFor(() => {
      screen.getByText('A');
    });
  });

  test('should display account ID when available', async () => {
    const mockData = {
      siteimprove: {
        integrated: true,
        accountId: 'ACC12345',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<SiteimproveWidget />);

    await waitFor(() => {
      screen.getByText(/Account: ACC12345/);
    });
  });

  test('should display last checked timestamp', async () => {
    const mockData = {
      siteimprove: {
        integrated: true,
        lastChecked: '2025-02-26 10:15:00',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<SiteimproveWidget />);

    await waitFor(() => {
      screen.getByText(/Last checked: 2025-02-26/);
    });
  });

  test('should handle error response', async () => {
    vi.mocked(clientModule.get).mockRejectedValueOnce(new Error('API Error'));

    render(<SiteimproveWidget />);

    await waitFor(() => {
      screen.getByText('API Error');
    });
  });

  test('should use custom title', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      siteimprove: { integrated: true },
    });

    render(<SiteimproveWidget title="Custom Title" />);

    await waitFor(() => {
      screen.getByText('Custom Title');
    });
  });

  test('should call correct API endpoint', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      siteimprove: { integrated: true },
    });

    render(<SiteimproveWidget />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledWith('/services/siteimprove/metrics');
    });
  });

  test('should handle inline object response format', async () => {
    const mockData = {
      integrated: true,
      accessibility: { level: 'AA' },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<SiteimproveWidget />);

    await waitFor(() => {
      screen.getByText('Connected');
    });
  });

  test('should handle empty response', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({});

    render(<SiteimproveWidget />);

    await waitFor(() => {
      screen.getByText('No Siteimprove data available');
    });
  });
});
