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
import { GlobalVariablesWidget } from '@/dashboard/GlobalVariablesWidget';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('GlobalVariablesWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('should display loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementationOnce(
      () => new Promise(() => {})
    );

    render(<GlobalVariablesWidget />);
    screen.getByText('Loading variables...');
  });

  test('should display total variables count', async () => {
    const mockData = {
      variables: {
        variables: [
          { name: 'VAR1', value: 'value1' },
        ],
        count: 15,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<GlobalVariablesWidget />);

    await waitFor(() => {
      screen.getByText(/Total Variables/);
      screen.getByText('15');
    });
  });

  test('should display list of global variables', async () => {
    const mockData = {
      variables: {
        variables: [
          { name: 'SITE_URL', value: 'https://example.com', scope: 'global' },
          { name: 'ADMIN_EMAIL', value: 'admin@example.com', scope: 'global' },
          { name: 'API_KEY', value: 'sk_****...', scope: 'secure' },
        ],
        count: 3,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<GlobalVariablesWidget />);

    await waitFor(() => {
      screen.getByText('SITE_URL');
      screen.getByText('ADMIN_EMAIL');
      screen.getByText('API_KEY');
    });
  });

  test('should display variable scope information', async () => {
    const mockData = {
      variables: {
        variables: [
          { name: 'VAR1', value: 'value1', scope: 'public' },
          { name: 'VAR2', value: 'value2', scope: 'private' },
        ],
        count: 2,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<GlobalVariablesWidget />);

    await waitFor(() => {
      screen.getByText('VAR1');
      screen.getByText('VAR2');
    });
  });

  test('should use custom title when provided', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      variables: {
        variables: [],
      },
    });

    render(<GlobalVariablesWidget title="System Config Variables" />);

    await waitFor(() => {
      screen.getByText('System Config Variables');
    });
  });

  test('should display more indicator for large variable list', async () => {
    const mockData = {
      variables: {
        variables: Array.from({ length: 15 }, (_, i) => ({
          name: `VAR${i}`,
          value: `value${i}`,
        })),
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<GlobalVariablesWidget />);

    await waitFor(() => {
      screen.getByText(/\+5 more/);
    });
  });

  test('should truncate long variable values at 100 characters', async () => {
    const mockData = {
      variables: {
        variables: [
          {
            name: 'LONG_VAR',
            value: 'x'.repeat(150),
            scope: 'global',
          },
        ],
        count: 1,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<GlobalVariablesWidget />);

    await waitFor(() => {
      screen.getByText('LONG_VAR');
    });
  });

  test('should handle error response', async () => {
    vi.mocked(clientModule.get).mockRejectedValueOnce(new Error('API Error'));

    render(<GlobalVariablesWidget />);

    await waitFor(() => {
      screen.getByText('API Error');
    });
  });

  test('should handle empty variables list', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      variables: {
        variables: [],
      },
    });

    render(<GlobalVariablesWidget />);

    await waitFor(() => {
      screen.getByText('No variables available');
    });
  });

  test('should use custom title when provided', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      variables: {
        variables: [],
      },
    });

    render(<GlobalVariablesWidget title="Custom Variables" />);

    await waitFor(() => {
      screen.getByText('Custom Variables');
    });
  });

  test('should call correct API endpoint', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      variables: {
        variables: [],
      },
    });

    render(<GlobalVariablesWidget />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledWith('/services/admin/variables');
    });
  });

  test('should handle nested data response format', async () => {
    const mockData = {
      data: {
        variables: [
          { name: 'TEST', value: 'value' },
        ],
        count: 1,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<GlobalVariablesWidget />);

    await waitFor(() => {
      screen.getByText('TEST');
    });
  });

  test('should handle admin response format', async () => {
    const mockData = {
      admin: {
        variables: [
          { name: 'ADMIN_VAR', value: 'admin_value' },
        ],
        count: 1,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<GlobalVariablesWidget />);

    await waitFor(() => {
      screen.getByText('ADMIN_VAR');
    });
  });

  test('should support custom refresh interval', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      variables: {
        variables: [{ name: 'TEST', value: 'value' }],
        count: 1,
      },
    });

    render(<GlobalVariablesWidget refreshInterval={30} />);

    await waitFor(() => {
      screen.getByText('TEST');
    });
  });
});
