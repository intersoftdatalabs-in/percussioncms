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
import { MembershipWidget } from '@/dashboard/MembershipWidget';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('MembershipWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('should display loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolves
    );

    render(<MembershipWidget />);
    screen.getByText('Loading membership data...');
  });

  test('should display total members count', async () => {
    const mockData = {
      membership: {
        totalMembers: 42,
        activeMembers: 38,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<MembershipWidget />);

    await waitFor(() => {
      screen.getByText('Total Members');
      screen.getByText('42');
    });
  });

  test('should display active members count', async () => {
    const mockData = {
      membership: {
        totalMembers: 42,
        activeMembers: 38,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<MembershipWidget />);

    await waitFor(() => {
      screen.getByText('Active');
      screen.getByText('38');
    });
  });

  test('should display member list with details', async () => {
    const mockData = {
      data: {
        totalMembers: 3,
        members: [
          {
            id: '1',
            name: 'Alice Johnson',
            email: 'alice@example.com',
            role: 'Admin',
          },
          {
            id: '2',
            name: 'Bob Smith',
            email: 'bob@example.com',
            role: 'Editor',
          },
        ],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<MembershipWidget />);

    await waitFor(() => {
      screen.getByText('Alice Johnson');
      screen.getByText('alice@example.com');
      screen.getByText(/Role: Admin/);
    });
  });

  test('should display member count summary', async () => {
    const mockData = {
      membership: {
        totalMembers: 50,
        activeMembers: 45,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<MembershipWidget />);

    await waitFor(() => {
      screen.getByText('50');
      screen.getByText('45');
    });
  });

  test('should handle error response', async () => {
    vi.mocked(clientModule.get).mockRejectedValueOnce(new Error('API Error'));

    render(<MembershipWidget />);

    await waitFor(() => {
      screen.getByText('API Error');
    });
  });

  test('should handle empty results', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      membership: {
        totalMembers: 0,
        members: [],
      },
    });

    render(<MembershipWidget />);

    await waitFor(() => {
      screen.getByText('0');
    });
  });

  test('should use custom title when provided', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      membership: { totalMembers: 5 },
    });

    render(<MembershipWidget title="Custom Members" />);

    await waitFor(() => {
      screen.getByText('Custom Members');
    });
  });

  test('should call correct API endpoint', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      membership: { totalMembers: 10 },
    });

    render(<MembershipWidget />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledWith('/services/membership/list');
    });
  });

  test('should display last updated timestamp when available', async () => {
    const mockData = {
      membership: {
        totalMembers: 25,
        lastUpdated: '2025-02-26 11:00:00',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<MembershipWidget />);

    await waitFor(() => {
      screen.getByText(/Last updated: 2025-02-26/);
    });
  });

  test('should handle inline object response format', async () => {
    const mockData = {
      totalMembers: 15,
      activeMembers: 12,
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<MembershipWidget />);

    await waitFor(() => {
      screen.getByText('15');
      screen.getByText('12');
    });
  });

  test('should display more indicator for large member list', async () => {
    const mockData = {
      membership: {
        totalMembers: 10,
        members: Array.from({ length: 8 }, (_, i) => ({
          id: `${i}`,
          name: `User ${i}`,
          email: `user${i}@example.com`,
        })),
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<MembershipWidget />);

    await waitFor(() => {
      screen.getByText(/\+3 more members/);
    });
  });
});
