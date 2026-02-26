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
import { CookieConsentWidget } from '@/dashboard';
import * as clientModule from '@/api/client';

vi.mock('@/api/client', () => ({
  get: vi.fn(),
}));

describe('CookieConsentWidget', () => {
  const mockConsent = {
    status: 'Compliant',
    consentRate: 92,
    complianceScore: 95,
    consentedUsers: 9200,
    totalUsers: 10000,
    lastUpdated: '2025-01-15',
    categories: ['Analytics', 'Marketing', 'Essential'],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render loading state', () => {
    vi.mocked(clientModule.get).mockImplementation(() => new Promise(() => {}));
    render(<CookieConsentWidget />);
    expect(screen.getByText('Loading compliance data...')).toBeDefined();
  });

  it('should display consent data when loaded', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ consent: mockConsent });
    render(<CookieConsentWidget />);
    await waitFor(() => {
      expect(screen.getByText('Compliant')).toBeDefined();
      expect(screen.getByText('92%')).toBeDefined();
    });
  });

  it('should display compliance score', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ consent: mockConsent });
    render(<CookieConsentWidget />);
    await waitFor(() => {
      expect(screen.getByText('95/100')).toBeDefined();
    });
  });

  it('should display consent categories', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ consent: mockConsent });
    render(<CookieConsentWidget />);
    await waitFor(() => {
      expect(screen.getByText('Analytics')).toBeDefined();
      expect(screen.getByText('Marketing')).toBeDefined();
      expect(screen.getByText('Essential')).toBeDefined();
    });
  });

  it('should handle error', async () => {
    vi.mocked(clientModule.get).mockRejectedValue(new Error('API error'));
    render(<CookieConsentWidget />);
    await waitFor(() => {
      expect(screen.getByText(/Error:/)).toBeDefined();
    });
  });

  it('should display no data message when empty', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({});
    render(<CookieConsentWidget />);
    await waitFor(() => {
      expect(screen.getByText('No compliance data available')).toBeDefined();
    });
  });

  it('should handle status response format', async () => {
    vi.mocked(clientModule.get).mockResolvedValue({ status: mockConsent });
    render(<CookieConsentWidget />);
    await waitFor(() => {
      expect(screen.getByText('Compliant')).toBeDefined();
    });
  });

  it('should handle custom title', () => {
    vi.mocked(clientModule.get).mockResolvedValue({ consent: mockConsent });
    render(<CookieConsentWidget title="GDPR Compliance" />);
    expect(screen.getByText('GDPR Compliance')).toBeDefined();
  });

  it('should log errors to console', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const error = new Error('API Error');
    vi.mocked(clientModule.get).mockRejectedValue(error);
    render(<CookieConsentWidget />);
    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith('CookieConsentWidget error:', error);
    });
    consoleErrorSpy.mockRestore();
  });
});
