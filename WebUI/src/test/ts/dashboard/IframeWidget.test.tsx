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
import { IframeWidget } from '@/dashboard/IframeWidget';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('IframeWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('should display loading state when fetching from API', () => {
    vi.mocked(clientModule.get).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolves
    );

    render(<IframeWidget />);
    screen.getByText('Loading external content...');
  });

  test('should render iframe with provided URL', async () => {
    render(<IframeWidget iframeUrl="https://example.com/dashboard" />);

    await waitFor(() => {
      const iframe = screen.getByTitle('External Content') as HTMLIFrameElement;
      expect(iframe.src).toBe('https://example.com/dashboard');
    });
  });

  test('should render iframe with custom title', async () => {
    render(
      <IframeWidget
        iframeUrl="https://example.com/dashboard"
        title="Analytics Dashboard"
      />
    );

    await waitFor(() => {
      const iframe = screen.getByTitle('Analytics Dashboard') as HTMLIFrameElement;
      expect(iframe).toBeTruthy();
    });
  });

  test('should render iframe with custom height', async () => {
    render(<IframeWidget iframeUrl="https://example.com/chart" iframeHeight={600} />);

    await waitFor(() => {
      const iframe = screen.getByTitle('External Content') as HTMLIFrameElement;
      expect(iframe.height).toBe('600');
    });
  });

  test('should fetch and render iframe from API', async () => {
    const mockData = {
      iframe: {
        url: 'https://api.example.com/embedded',
        title: 'API Content',
        height: 500,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<IframeWidget />);

    await waitFor(() => {
      const iframe = screen.getByTitle('API Content') as HTMLIFrameElement;
      expect(iframe.src).toBe('https://api.example.com/embedded');
      expect(iframe.height).toBe('500');
    });
  });

  test('should handle data response format from API', async () => {
    const mockData = {
      data: {
        url: 'https://example.com/report',
        title: 'Report',
        height: 450,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<IframeWidget />);

    await waitFor(() => {
      const iframe = screen.getByTitle('Report') as HTMLIFrameElement;
      expect(iframe.src).toBe('https://example.com/report');
    });
  });

  test('should handle config response format from API', async () => {
    const mockData = {
      config: {
        url: 'https://example.com/config-embed',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<IframeWidget />);

    await waitFor(() => {
      const iframe = screen.getByTitle('External Content') as HTMLIFrameElement;
      expect(iframe.src).toBe('https://example.com/config-embed');
    });
  });

  test('should apply sandbox attribute', async () => {
    render(<IframeWidget iframeUrl="https://example.com/safe" />);

    await waitFor(() => {
      const iframe = screen.getByTitle('External Content') as HTMLIFrameElement;
      expect(iframe.sandbox.contains('allow-scripts')).toBe(true);
    });
  });

  test('should allow fullscreen by default', async () => {
    render(<IframeWidget iframeUrl="https://example.com/fullscreen" />);

    await waitFor(() => {
      const iframe = screen.getByTitle('External Content') as HTMLIFrameElement;
      expect(iframe.allowFullscreen).toBe(true);
    });
  });

  test('should handle error from API', async () => {
    vi.mocked(clientModule.get).mockRejectedValueOnce(new Error('API Error'));

    render(<IframeWidget />);

    await waitFor(() => {
      screen.getByText('API Error');
    });
  });

  test('should handle missing URL', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({});

    render(<IframeWidget />);

    await waitFor(() => {
      screen.getByText('No iframe URL configured');
    });
  });

  test('should prefer direct iframeUrl prop over API', async () => {
    const mockData = {
      iframe: {
        url: 'https://api-url.com',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    const { rerender } = render(
      <IframeWidget iframeUrl="https://direct-url.com" />
    );

    // Should NOT have called the API
    expect(clientModule.get).not.toHaveBeenCalled();

    const iframe = screen.getByTitle('External Content') as HTMLIFrameElement;
    expect(iframe.src).toBe('https://direct-url.com');
  });

  test('should use custom title from widget props', async () => {
    render(
      <IframeWidget
        iframeUrl="https://example.com"
        title="Custom Widget Title"
      />
    );

    await waitFor(() => {
      const iframe = screen.getByTitle('Custom Widget Title') as HTMLIFrameElement;
      expect(iframe).toBeTruthy();
    });
  });

  test('should handle inline object response format', async () => {
    const mockData = {
      url: 'https://example.com/inline',
      title: 'Inline',
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockData);

    render(<IframeWidget />);

    await waitFor(() => {
      const iframe = screen.getByTitle('Inline') as HTMLIFrameElement;
      expect(iframe.src).toBe('https://example.com/inline');
    });
  });

  test('should call correct API endpoint', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({
      iframe: { url: 'https://example.com' },
    });

    render(<IframeWidget />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledWith('/services/embed/iframe');
    });
  });
});
