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

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { CommentsWidget } from '@/dashboard';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('CommentsWidget', () => {
  const mockComments = [
    {
      id: '1',
      author: 'Alice Smith',
      content: 'Great article! Very informative content.',
      page: 'Getting Started Guide',
      status: 'Approved',
      approved: true,
      createdAt: new Date(Date.now() - 3600000).toISOString(),
    },
    {
      id: '2',
      author: 'Bob Johnson',
      text: 'This helped me solve my problem.',
      post: 'Installation Guide',
      status: 'Pending',
      approved: false,
      timestamp: new Date(Date.now() - 7200000).toISOString(),
    },
    {
      id: '3',
      email: 'charlie@example.com',
      content: 'Spam content will be blocked',
      status: 'Spam',
      createdAt: new Date(Date.now() - 86400000).toISOString(),
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementation(() => new Promise(() => {}));

    render(<CommentsWidget />);

    expect(screen.getByText('Loading comments...')).toBeDefined();
  });

  it('should display comments when data is loaded', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ comments: mockComments });

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeDefined();
      expect(screen.getByText('Bob Johnson')).toBeDefined();
    });
  });

  it('should display comment content excerpts', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ comments: mockComments });

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText(/Great article/)).toBeDefined();
      expect(screen.getByText(/This helped me/)).toBeDefined();
    });
  });

  it('should display comment status', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ comments: mockComments });

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Approved')).toBeDefined();
      expect(screen.getByText('Pending')).toBeDefined();
      expect(screen.getByText('Spam')).toBeDefined();
    });
  });

  it('should display page or post reference when available', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ comments: mockComments });

    const { container } = render(<CommentsWidget />);

    await waitFor(() => {
      expect(container.textContent).toContain('Getting Started Guide');
      expect(container.textContent).toContain('Installation Guide');
    });
  });

  it('should display error message on fetch failure', async () => {
    const errorMsg = 'Network error';
    vi.mocked(clientModule.get).mockRejectedValue(new Error(errorMsg));

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText(`Error: ${errorMsg}`)).toBeDefined();
    });
  });

  it('should display no comments message when list is empty', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ comments: [] });

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText('No comments yet')).toBeDefined();
    });
  });

  it('should respect maxComments prop', async () => {
    const manyComments = Array.from({ length: 15 }, (_, i) => ({
      id: `${i}`,
      author: `Author ${i}`,
      content: `Comment ${i}`,
    }));

    vi.mocked(clientModule.get).mockResolvedValue({ comments: manyComments });

    render(<CommentsWidget maxComments={5} />);

    await waitFor(() => {
      expect(screen.getByText('Author 0')).toBeDefined();
      expect(screen.queryByText('Author 5')).toBeNull();
    });
  });

  it('should use custom title when provided', () => {
    vi.mocked(clientModule.get).mockResolvedValue({ comments: [] });

    const customTitle = 'Site Feedback';
    render(<CommentsWidget title={customTitle} />);

    expect(screen.getByText(customTitle)).toBeDefined();
  });

  it('should handle alternative response format (items)', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ items: mockComments });

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeDefined();
    });
  });

  it('should handle data response format', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ data: mockComments });

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeDefined();
    });
  });

  it('should handle array response format', async () => {
    vi.mocked(clientModule.get).mockResolvedValue(mockComments as any);

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeDefined();
    });
  });

  it('should fetch immediately on mount', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ comments: mockComments });

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledWith('/services/comments/latest');
    });
  });

  it('should display email when author is not available', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ comments: mockComments });

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText('charlie@example.com')).toBeDefined();
    });
  });

  it('should truncate long comment content', async () => {
    const longComment = {
      id: '1',
      author: 'Test User',
      content: 'A'.repeat(200),
      createdAt: new Date().toISOString(),
    };

    vi.mocked(clientModule.get).mockResolvedValue({ comments: [longComment] });

    render(<CommentsWidget />);

    const { container } = render(<CommentsWidget />);
    await waitFor(() => {
      const text = container.textContent;
      expect(text?.includes('A'.repeat(80))).toBeTruthy();
      expect(text?.includes('A'.repeat(81))).toBeFalsy();
    });
  });

  it('should handle undefined response gracefully', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({});

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText('No comments yet')).toBeDefined();
    });
  });

  it('should log errors to console', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const error = new Error('API Error');
    vi.mocked(clientModule.get).mockRejectedValue(error);

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith('CommentsWidget error:', error);
    });

    consoleErrorSpy.mockRestore();
  });

  it('should format recent timestamps as relative time', async () => {
    const recentComment = {
      id: '1',
      author: 'Fresh User',
      content: 'Just posted',
      createdAt: new Date(Date.now() - 60000).toISOString(), // 1 minute ago
    };

    vi.mocked(clientModule.get).mockResolvedValue({ comments: [recentComment] });

    render(<CommentsWidget />);

    await waitFor(() => {
      expect(screen.getByText(/ago/)).toBeDefined();
    });
  });

  it('should disable refresh if refreshInterval is 0', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ comments: mockComments });

    const { unmount } = render(<CommentsWidget refreshInterval={0} />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledTimes(1);
    });

    unmount();
  });
});
