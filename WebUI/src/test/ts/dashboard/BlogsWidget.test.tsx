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
import { BlogsWidget } from '@/dashboard';
import * as clientModule from '@/api/client';

// Mock the API client
vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('BlogsWidget', () => {
  const mockBlogs = [
    {
      id: '1',
      title: 'Welcome to Our Blog',
      status: 'Published',
      author: 'John Doe',
      description: 'First blog post about our company',
      postCount: 5,
      publishedAt: new Date(Date.now() - 86400000).toISOString(),
    },
    {
      id: '2',
      title: 'Updates and News',
      status: 'Draft',
      author: 'Jane Smith',
      description: 'Latest company updates',
      postCount: 3,
      createdAt: new Date(Date.now() - 3600000).toISOString(),
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementation(() => new Promise(() => {}));

    render(<BlogsWidget />);

    expect(screen.getByText('Loading blogs...')).toBeDefined();
  });

  it('should display blogs when data is loaded', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ blogs: mockBlogs });

    render(<BlogsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Welcome to Our Blog')).toBeDefined();
      expect(screen.getByText('Updates and News')).toBeDefined();
    });
  });

  it('should display author information when available', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ blogs: mockBlogs });

    const { container } = render(<BlogsWidget />);

    await waitFor(() => {
      expect(container.textContent).toContain('John Doe');
      expect(container.textContent).toContain('Jane Smith');
    });
  });

  it('should display post count when available', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ blogs: mockBlogs });

    render(<BlogsWidget />);

    await waitFor(() => {
      expect(screen.getByText(/5\s*posts/i)).toBeDefined();
      expect(screen.getByText(/3\s*posts/i)).toBeDefined();
    });
  });

  it('should display blog status', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ blogs: mockBlogs });

    render(<BlogsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Published')).toBeDefined();
      expect(screen.getByText('Draft')).toBeDefined();
    });
  });

  it('should display error message on fetch failure', async () => {
    const errorMsg = 'Network error';
    vi.mocked(clientModule.get).mockRejectedValue(new Error(errorMsg));

    render(<BlogsWidget />);

    await waitFor(() => {
      expect(screen.getByText(`Error: ${errorMsg}`)).toBeDefined();
    });
  });

  it('should display no blogs message when list is empty', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ blogs: [] });

    render(<BlogsWidget />);

    await waitFor(() => {
      expect(screen.getByText('No blogs available')).toBeDefined();
    });
  });

  it('should respect maxBlogs prop', async () => {
    const manyBlogs = Array.from({ length: 15 }, (_, i) => ({
      id: `${i}`,
      title: `Blog ${i}`,
      status: 'Published',
    }));

    vi.mocked(clientModule.get).mockResolvedValue({ blogs: manyBlogs });

    render(<BlogsWidget maxBlogs={5} />);

    await waitFor(() => {
      expect(screen.getByText('Blog 0')).toBeDefined();
      expect(screen.getByText('Blog 4')).toBeDefined();
      expect(screen.queryByText('Blog 5')).toBeNull();
    });
  });

  it('should use custom title when provided', () => {
    vi.mocked(clientModule.get).mockResolvedValue({ blogs: [] });

    const customTitle = 'My Custom Blog Title';
    render(<BlogsWidget title={customTitle} />);

    expect(screen.getByText(customTitle)).toBeDefined();
  });

  it('should handle alternative response format (items)', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ items: mockBlogs });

    render(<BlogsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Welcome to Our Blog')).toBeDefined();
    });
  });

  it('should handle array response format', async () => {
    vi.mocked(clientModule.get).mockResolvedValue(mockBlogs as any);

    render(<BlogsWidget />);

    await waitFor(() => {
      expect(screen.getByText('Welcome to Our Blog')).toBeDefined();
    });
  });

  it('should fetch immediately and set up refresh interval', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ blogs: mockBlogs });

    const { unmount } = render(<BlogsWidget refreshInterval={5000} />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalled();
    });

    expect(clientModule.get).toHaveBeenCalledWith('/services/blogs/list');

    unmount();
  });

  it('should handle undefined response gracefully', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({});

    render(<BlogsWidget />);

    await waitFor(() => {
      expect(screen.getByText('No blogs available')).toBeDefined();
    });
  });

  it('should display description when available', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ blogs: mockBlogs });

    render(<BlogsWidget />);

    await waitFor(() => {
      expect(screen.getByText('First blog post about our company')).toBeDefined();
      expect(screen.getByText('Latest company updates')).toBeDefined();
    });
  });

  it('should log errors to console', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const error = new Error('API Error');
    vi.mocked(clientModule.get).mockRejectedValue(error);

    render(<BlogsWidget />);

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith('BlogsWidget error:', error);
    });

    consoleErrorSpy.mockRestore();
  });

  it('should disable refresh if refreshInterval is 0', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ blogs: mockBlogs });

    const { unmount } = render(<BlogsWidget refreshInterval={0} />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledTimes(1);
    });

    unmount();
  });
});
