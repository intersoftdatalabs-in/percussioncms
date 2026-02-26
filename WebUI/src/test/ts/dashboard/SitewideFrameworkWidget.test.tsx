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
import { SitewideFrameworkWidget } from '@/dashboard/SitewideFrameworkWidget';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('SitewideFrameworkWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('should display loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolves
    );

    render(<SitewideFrameworkWidget />);
    screen.getByText('Loading framework configuration...');
  });

  test('should display framework version', async () => {
    const mockConfig = {
      framework: {
        frameworkVersion: '8.1.6',
        totalModules: 5,
        enabledModules: 5,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText('Framework Version');
      screen.getByText('v8.1.6');
    });
  });

  test('should display healthy status when all modules enabled', async () => {
    const mockConfig = {
      framework: {
        frameworkVersion: '8.1.6',
        totalModules: 5,
        enabledModules: 5,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText('healthy');
    });
  });

  test('should display degraded status when modules disabled', async () => {
    const mockConfig = {
      framework: {
        frameworkVersion: '8.1.6',
        totalModules: 5,
        enabledModules: 3,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText('degraded');
    });
  });

  test('should display module statistics', async () => {
    const mockConfig = {
      framework: {
        frameworkVersion: '8.1.6',
        totalModules: 8,
        enabledModules: 7,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText('Total Modules');
      screen.getByText('8');
      screen.getByText('Enabled');
      screen.getByText('7');
    });
  });

  test('should display module list with status', async () => {
    const mockConfig = {
      framework: {
        frameworkVersion: '8.1.6',
        modules: [
          { name: 'Core', enabled: true },
          { name: 'Content', enabled: true },
          { name: 'Analytics', enabled: false },
        ],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText('Core');
      screen.getByText('Content');
      screen.getByText('Analytics');
    });
  });

  test('should handle data response format', async () => {
    const mockConfig = {
      data: {
        frameworkVersion: '8.1.6',
        totalModules: 3,
        enabledModules: 3,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText('v8.1.6');
    });
  });

  test('should handle config response format', async () => {
    const mockConfig = {
      config: {
        frameworkVersion: '8.1.5',
        totalModules: 5,
        enabledModules: 5,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText('v8.1.5');
    });
  });

  test('should handle error response', async () => {
    vi.mocked(clientModule.get).mockRejectedValueOnce(new Error('API Error'));

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText('API Error');
    });
  });

  test('should use custom title when provided', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      framework: { frameworkVersion: '8.1.6' },
    });

    render(<SitewideFrameworkWidget title="Custom Framework" />);

    await waitFor(() => {
      screen.getByText('Custom Framework');
    });
  });

  test('should call correct API endpoint', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      framework: { frameworkVersion: '8.1.6' },
    });

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledWith('/services/framework/config');
    });
  });

  test('should handle inline object response format', async () => {
    const mockConfig = {
      frameworkVersion: '8.1.6',
      totalModules: 4,
      enabledModules: 4,
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText('v8.1.6');
    });
  });

  test('should display last checked timestamp', async () => {
    const mockConfig = {
      framework: {
        frameworkVersion: '8.1.6',
        lastChecked: '2025-02-26 11:30:00',
        totalModules: 5,
        enabledModules: 5,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText(/Last checked: 2025-02-26/);
    });
  });

  test('should display modules truncation indicator', async () => {
    const mockConfig = {
      framework: {
        frameworkVersion: '8.1.6',
        modules: Array.from({ length: 8 }, (_, i) => ({
          name: `Module${i}`,
          enabled: true,
        })),
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<SitewideFrameworkWidget />);

    await waitFor(() => {
      screen.getByText(/\+4 more modules/);
    });
  });
});
