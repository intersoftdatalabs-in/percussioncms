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
import { WidgetConfigurationWidget } from '@/dashboard/WidgetConfigurationWidget';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('WidgetConfigurationWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('should display loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolves
    );

    render(<WidgetConfigurationWidget />);
    screen.getByText('Loading dashboard configuration...');
  });

  test('should display active and available widget counts', async () => {
    const mockConfig = {
      dashboard: {
        activeWidgets: 12,
        totalWidgets: 21,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText('Active Widgets');
      screen.getByText('12');
      screen.getByText('Available');
      screen.getByText('21');
    });
  });

  test('should display list of installed widgets', async () => {
    const mockConfig = {
      data: {
        activeWidgets: 3,
        totalWidgets: 5,
        widgets: [
          { id: '1', name: 'Blogs', active: true },
          { id: '2', name: 'Comments', active: true },
          { id: '3', name: 'Analytics', active: false },
        ],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText('Blogs');
      screen.getByText('Comments');
      screen.getByText('Analytics');
    });
  });

  test('should show active status for enabled widgets', async () => {
    const mockConfig = {
      dashboard: {
        widgets: [
          { id: '1', name: 'Active Widget', active: true },
        ],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText('Active Widget');
      screen.getByText('Active');
    });
  });

  test('should show inactive status for disabled widgets', async () => {
    const mockConfig = {
      dashboard: {
        widgets: [
          { id: '1', name: 'Inactive Widget', active: false },
        ],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText('Inactive Widget');
      screen.getByText('Inactive');
    });
  });

  test('should display widget categories', async () => {
    const mockConfig = {
      dashboard: {
        categories: ['System', 'Content Management', 'Analytics', 'Compliance'],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText('Categories');
      screen.getByText('System');
      screen.getByText('Content Management');
      screen.getByText('Analytics');
      screen.getByText('Compliance');
    });
  });

  test('should handle more indicator for large widget list', async () => {
    const mockConfig = {
      dashboard: {
        widgets: Array.from({ length: 10 }, (_, i) => ({
          id: `${i}`,
          name: `Widget${i}`,
          active: true,
        })),
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText(/\+4 more widgets/);
    });
  });

  test('should display add widget button when callback provided', async () => {
    const mockAddWidget = vi.fn();
    const mockConfig = {
      dashboard: {
        activeWidgets: 10,
        totalWidgets: 21,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget onAddWidget={mockAddWidget} />);

    await waitFor(() => {
      const button = screen.getByText('+ Add Widget') as HTMLButtonElement;
      expect(button).toBeTruthy();
    });
  });

  test('should display last saved timestamp', async () => {
    const mockConfig = {
      dashboard: {
        activeWidgets: 5,
        lastSaved: '2025-02-26 10:00:00',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText(/Last saved: 2025-02-26/);
    });
  });

  test('should handle error response', async () => {
    vi.mocked(clientModule.get).mockRejectedValueOnce(new Error('API Error'));

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText('API Error');
    });
  });

  test('should use custom title', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      dashboard: { activeWidgets: 5 },
    });

    render(<WidgetConfigurationWidget title="Custom Config" />);

    await waitFor(() => {
      screen.getByText('Custom Config');
    });
  });

  test('should call correct API endpoint', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      dashboard: { activeWidgets: 5 },
    });

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledWith('/services/dashboard/config');
    });
  });

  test('should handle inline object response format', async () => {
    const mockConfig = {
      activeWidgets: 8,
      totalWidgets: 15,
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText('8');
      screen.getByText('15');
    });
  });

  test('should handle config response format', async () => {
    const mockConfig = {
      config: {
        activeWidgets: 7,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText('7');
    });
  });

  test('should handle empty response', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({});

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText('No dashboard configuration available');
    });
  });

  test('should display more categories indicator', async () => {
    const mockConfig = {
      dashboard: {
        categories: Array.from({ length: 12 }, (_, i) => `Category${i}`),
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockConfig);

    render(<WidgetConfigurationWidget />);

    await waitFor(() => {
      screen.getByText(/\+4 more/);
    });
  });
});
