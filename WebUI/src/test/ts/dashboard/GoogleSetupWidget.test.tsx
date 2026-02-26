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
import { GoogleSetupWidget } from '@/dashboard/GoogleSetupWidget';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('GoogleSetupWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('should display loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );

    render(<GoogleSetupWidget />);
    screen.getByText(/loading google setup/i);
  });

  test('should display connected account status', async () => {
    const mockSetup = {
      setup: {
        accountConnected: true,
        email: 'user@example.com',
        services: [],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockSetup);

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      screen.getByText('Account Connected');
      screen.getByText('user@example.com');
    });
  });

  test('should display disconnected account status', async () => {
    const mockSetup = {
      data: {
        accountConnected: false,
        services: [],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockSetup);

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      screen.getByText('Account Not Connected');
    });
  });

  test('should display configured Google services', async () => {
    const mockSetup = {
      google: {
        accountConnected: true,
        email: 'admin@company.com',
        services: [
          { name: 'Google Analytics', enabled: true, connected: true },
          { name: 'Google Search Console', enabled: true, connected: true },
          { name: 'Google Ads', enabled: false, connected: false },
        ],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockSetup);

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      screen.getByText('Services (3)');
      screen.getByText('Google Analytics');
      screen.getByText('Google Search Console');
    });
  });

  test('should display sync status', async () => {
    const mockSetup = {
      setup: {
        accountConnected: true,
        services: [],
        syncStatus: 'Last synced 2 hours ago',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockSetup);

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      screen.getByText(/Sync Status:/);
      screen.getByText('Last synced 2 hours ago');
    });
  });

  test('should display error message on fetch failure', async () => {
    vi.mocked(clientModule.get).mockRejectedValueOnce(new Error('API error'));

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      screen.getByText('API error');
    });
  });

  test('should display no data message when response is empty', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({});

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      screen.getByText(/no google setup data/i);
    });
  });

  test('should support custom title prop', async () => {
    const mockSetup = { setup: { accountConnected: true, services: [] } };
    vi.mocked(clientModule.get).mockResolvedValueOnce(mockSetup);

    render(<GoogleSetupWidget title="Google Integration Status" />);

    await waitFor(() => {
      screen.getByText('Google Integration Status');
    });
  });

  test('should handle different response format variations', async () => {
    const mockSetup = {
      accountConnected: true,
      email: 'test@test.com',
      services: [],
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockSetup);

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      screen.getByText('Account Connected');
    });
  });

  test('should call API with correct endpoint', async () => {
    const mockSetup = { setup: { accountConnected: true, services: [] } };
    vi.mocked(clientModule.get).mockResolvedValueOnce(mockSetup);

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      expect(vi.mocked(clientModule.get)).toHaveBeenCalledWith('/services/google/setup');
    });
  });

  test('should display last update timestamp', async () => {
    const mockSetup = {
      setup: {
        accountConnected: true,
        services: [],
        lastUpdate: '2024-02-26T10:30:00Z',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockSetup);

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      screen.getByText(/Last updated:/);
    });
  });

  test('should display service count in header', async () => {
    const mockSetup = {
      setup: {
        accountConnected: true,
        services: [
          { name: 'Analytics', enabled: true, connected: true },
          { name: 'Search Console', enabled: true, connected: true },
        ],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockSetup);

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      screen.getByText('Services (2)');
    });
  });

  test('should set up refresh interval when refreshInterval prop is provided', async () => {
    const mockSetup = { setup: { accountConnected: true, services: [] } };
    vi.mocked(clientModule.get).mockResolvedValue(mockSetup);

    vi.useFakeTimers();

    render(<GoogleSetupWidget refreshInterval={30} />);

    await waitFor(() => {
      expect(vi.mocked(clientModule.get)).toHaveBeenCalledTimes(1);
    });

    vi.advanceTimersByTime(30000);

    await waitFor(() => {
      expect(vi.mocked(clientModule.get)).toHaveBeenCalledTimes(2);
    });

    vi.useRealTimers();
  });

  test('should not set up refresh interval when not provided', async () => {
    const mockSetup = { setup: { accountConnected: true, services: [] } };
    vi.mocked(clientModule.get).mockResolvedValue(mockSetup);

    render(<GoogleSetupWidget />);

    await waitFor(() => {
      expect(vi.mocked(clientModule.get)).toHaveBeenCalledTimes(1);
    });

    await new Promise(resolve => setTimeout(resolve, 100));
    expect(vi.mocked(clientModule.get)).toHaveBeenCalledTimes(1);
  });
});
