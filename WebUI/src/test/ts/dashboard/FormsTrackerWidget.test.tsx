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
import { FormsTrackerWidget } from '@/dashboard';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('FormsTrackerWidget', () => {
  const mockForms = [
    {
      id: '1',
      formName: 'Contact Form',
      submissions: 150,
      successCount: 145,
      errorCount: 5,
      lastSubmission: new Date(Date.now() - 3600000).toISOString(),
    },
    {
      id: '2',
      formName: 'Newsletter Signup',
      submissions: 340,
      successCount: 335,
      errorCount: 5,
      lastSubmission: new Date(Date.now() - 1800000).toISOString(),
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementation(() => new Promise(() => {}));

    render(<FormsTrackerWidget />);

    expect(screen.getByText('Loading form tracker...')).toBeDefined();
  });

  it('should display forms tracker data when loaded', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ forms: mockForms });

    render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(screen.getByText('Contact Form')).toBeDefined();
      expect(screen.getByText('Newsletter Signup')).toBeDefined();
    });
  });

  it('should display submission counts', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ forms: mockForms });

    const { container } = render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(container.textContent).toContain('150 submissions');
      expect(container.textContent).toContain('340 submissions');
    });
  });

  it('should display error counts when available', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ forms: mockForms });

    const { container } = render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(container.textContent).toContain('5 errors');
    });
  });

  it('should calculate success rate', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ forms: mockForms });

    const { container } = render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(container.textContent).toContain('Success Rate:');
      // Contact form: 145/150 = 96.67% ≈ 97%
      expect(container.textContent).toContain('97%');
    });
  });

  it('should display error message on fetch failure', async () => {
    const errorMsg = 'Connection failed';
    vi.mocked(clientModule.get).mockRejectedValue(new Error(errorMsg));

    render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(screen.getByText(`Error: ${errorMsg}`)).toBeDefined();
    });
  });

  it('should display no forms message when empty', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ forms: [] });

    render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(screen.getByText('No tracked forms')).toBeDefined();
    });
  });

  it('should respect maxForms prop', async () => {
    const manyForms = Array.from({ length: 20 }, (_, i) => ({
      id: `${i}`,
      formName: `Form ${i}`,
      submissions: 100 + i,
    }));

    vi.mocked(clientModule.get).mockResolvedValue({ forms: manyForms });

    render(<FormsTrackerWidget maxForms={5} />);

    await waitFor(() => {
      expect(screen.getByText('Form 0')).toBeDefined();
      expect(screen.queryByText('Form 5')).toBeNull();
    });
  });

  it('should use custom title when provided', () => {
    vi.mocked(clientModule.get).mockResolvedValue({ forms: [] });

    const customTitle = 'Form Submissions';
    render(<FormsTrackerWidget title={customTitle} />);

    expect(screen.getByText(customTitle)).toBeDefined();
  });

  it('should handle items response format', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ items: mockForms });

    render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(screen.getByText('Contact Form')).toBeDefined();
    });
  });

  it('should handle trackedForms response format', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ trackedForms: mockForms });

    render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(screen.getByText('Contact Form')).toBeDefined();
    });
  });

  it('should handle data response format', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ data: mockForms });

    render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(screen.getByText('Contact Form')).toBeDefined();
    });
  });

  it('should handle array response format', async () => {
    vi.mocked(clientModule.get).mockResolvedValue(mockForms as any);

    render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(screen.getByText('Contact Form')).toBeDefined();
    });
  });

  it('should fetch from correct endpoint', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ forms: mockForms });

    render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(clientModule.get).toHaveBeenCalledWith('/services/forms/tracker');
    });
  });

  it('should handle undefined response gracefully', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({});

    render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(screen.getByText('No tracked forms')).toBeDefined();
    });
  });

  it('should log errors to console', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const error = new Error('API Error');
    vi.mocked(clientModule.get).mockRejectedValue(error);

    render(<FormsTrackerWidget />);

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith('FormsTrackerWidget error:', error);
    });

    consoleErrorSpy.mockRestore();
  });
});
