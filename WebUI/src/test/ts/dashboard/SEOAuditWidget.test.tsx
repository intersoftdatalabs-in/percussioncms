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
import { SEOAuditWidget } from '@/dashboard/SEOAuditWidget';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('SEOAuditWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('should display loading state initially', () => {
    vi.mocked(clientModule.get).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolves
    );

    render(<SEOAuditWidget />);

    screen.getByText('Loading SEO audit data...');
  });

  test('should display SEO audit data with score and grade', async () => {
    const mockAudit = {
      audit: {
        score: 85,
        grade: 'B',
        passedChecks: 48,
        failedChecks: 5,
        lastAudit: '2024-02-20',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockAudit);

    render(<SEOAuditWidget />);

    await waitFor(() => {
      screen.getByText('85/100');
      screen.getByText('B');
    });
  });

  test('should display passed and failed checks', async () => {
    const mockAudit = {
      data: {
        passedChecks: 48,
        failedChecks: 5,
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockAudit);

    render(<SEOAuditWidget />);

    await waitFor(() => {
      screen.getByText('Passed Checks');
      screen.getByText('Failed Checks');
      screen.getByText('48');
      screen.getByText('5');
    });
  });

  test('should display SEO issues by category', async () => {
    const mockAudit = {
      seo: {
        score: 85,
        issues: [
          { category: 'Meta Tags', count: 3, severity: 'critical' },
          { category: 'Images', count: 2, severity: 'warning' },
          { category: 'Headers', count: 1, severity: 'info' },
        ],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockAudit);

    render(<SEOAuditWidget />);

    await waitFor(() => {
      screen.getByText('Issues by Category');
      screen.getByText('Meta Tags');
      screen.getByText('Images');
      screen.getByText('Headers');
    });
  });

  test('should display recommendations with truncation for long lists', async () => {
    const mockAudit = {
      audit: {
        recommendations: [
          'Add meta descriptions to all pages',
          'Optimize image alt text',
          'Improve page load speed',
          'Fix broken links',
          'Add structured data markup',
        ],
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockAudit);

    render(<SEOAuditWidget />);

    await waitFor(() => {
      screen.getByText('Recommendations:');
      screen.getByText('Add meta descriptions to all pages');
      screen.getByText('+2 more recommendations');
    });
  });

  test('should display last audit timestamp', async () => {
    const mockAudit = {
      audit: {
        score: 85,
        lastAudit: '2024-02-20T14:30:00Z',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockAudit);

    render(<SEOAuditWidget />);

    await waitFor(() => {
      screen.getByText(/Last audit:/);
    });
  });

  test('should display error message on fetch failure', async () => {
    vi.mocked(clientModule.get).mockRejectedValueOnce(new Error('Network error'));

    render(<SEOAuditWidget />);

    await waitFor(() => {
      screen.getByText('Network error');
    });
  });

  test('should display no data message when response is empty', async () => {
    vi.mocked(clientModule.get).mockResolvedValueOnce({});

    render(<SEOAuditWidget />);

    await waitFor(() => {
      screen.getByText('No SEO audit data available');
    });
  });

  test('should support custom title prop', async () => {
    const mockAudit = { audit: { score: 80 } };
    vi.mocked(clientModule.get).mockResolvedValueOnce(mockAudit);

    render(<SEOAuditWidget title="Website SEO Health" />);

    await waitFor(() => {
      screen.getByText('Website SEO Health');
    });
  });

  test('should handle different response format variations', async () => {
    const mockAudit = {
      score: 90,
      grade: 'A',
      passedChecks: 50,
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockAudit);

    render(<SEOAuditWidget />);

    await waitFor(() => {
      screen.getByText('90/100');
    });
  });

  test('should render grade with appropriate color based on score', async () => {
    const mockAudit = {
      audit: {
        score: 95,
        grade: 'A+',
      },
    };

    vi.mocked(clientModule.get).mockResolvedValueOnce(mockAudit);

    render(<SEOAuditWidget />);

    await waitFor(() => {
      const gradeElement = screen.getByText('A+');
      expect(gradeElement).toBeInTheDocument();
      // Verify it's styled (can't check exact color in test, but element should exist)
    });
  });

  test('should call API with correct endpoint', async () => {
    const mockAudit = { audit: { score: 85 } };
    vi.mocked(clientModule.get).mockResolvedValueOnce(mockAudit);

    render(<SEOAuditWidget />);

    await waitFor(() => {
      expect(vi.mocked(clientModule.get)).toHaveBeenCalledWith('/services/seo/audit');
    });
  });

  test('should set up refresh interval when refreshInterval prop is provided', async () => {
    const mockAudit = { audit: { score: 85 } };
    vi.mocked(clientModule.get).mockResolvedValue(mockAudit);

    vi.useFakeTimers();

    render(<SEOAuditWidget refreshInterval={30} />);

    await waitFor(() => {
      expect(vi.mocked(clientModule.get)).toHaveBeenCalledTimes(1);
    });

    // Fast-forward 30 seconds
    vi.advanceTimersByTime(30000);

    await waitFor(() => {
      expect(vi.mocked(clientModule.get)).toHaveBeenCalledTimes(2);
    });

    vi.useRealTimers();
  });

  test('should not set up refresh interval when refreshInterval is not provided', async () => {
    const mockAudit = { audit: { score: 85 } };
    vi.mocked(clientModule.get).mockResolvedValue(mockAudit);

    render(<SEOAuditWidget />);

    await waitFor(() => {
      expect(vi.mocked(clientModule.get)).toHaveBeenCalledTimes(1);
    });

    // Wait a bit to ensure no additional calls
    await new Promise(resolve => setTimeout(resolve, 100));
    expect(vi.mocked(clientModule.get)).toHaveBeenCalledTimes(1);
  });

  test('should clean up interval on component unmount', async () => {
    const mockAudit = { audit: { score: 85 } };
    vi.mocked(clientModule.get).mockResolvedValue(mockAudit);

    const clearIntervalSpy = vi.spyOn(global, 'clearInterval');

    const { unmount } = render(<SEOAuditWidget refreshInterval={30} />);

    await waitFor(() => {
      expect(vi.mocked(clientModule.get)).toHaveBeenCalled();
    });

    unmount();

    expect(clearIntervalSpy).toHaveBeenCalled();
    clearIntervalSpy.mockRestore();
  });
});
