/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
import { renderHook, waitFor } from '@testing-library/react';
import { useDashboardConfig, type WidgetConfig } from '@/dashboard/hooks/useDashboardConfig';
import * as clientModule from '@/api/client';

// Mock the API client
vi.mock('@/api/client', () => ({
  get: vi.fn(),
  put: vi.fn(),
}));

describe('useDashboardConfig hook', () => {
  const mockConfig = {
    userId: 'test-user',
    widgets: [
      {
        widgetKey: 'welcome',
        widgetType: 'WelcomeWidget',
        position: { column: 'left' as const, order: 0 },
      },
      {
        widgetKey: 'workflow',
        widgetType: 'WorkflowStatusWidget',
        position: { column: 'right' as const, order: 0 },
      },
    ],
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-25T00:00:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should load dashboard config on mount', async () => {
    vi.mocked(clientModule.get).mockResolvedValue(mockConfig);

    const { result } = renderHook(() => useDashboardConfig('test-user'));

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.config).toEqual(mockConfig);
    expect(clientModule.get).toHaveBeenCalledWith('/services/dashboardmanagement/dashboard/test-user');
  });

  it('should handle loading error', async () => {
    const error = new Error('Failed to load config');
    vi.mocked(clientModule.get).mockRejectedValue(error);

    const { result } = renderHook(() => useDashboardConfig('test-user'));

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.error).toBe('Failed to load config');
    expect(result.current.config).toBeNull();
  });

  it('should not load config if userId is not provided', async () => {
    const { result } = renderHook(() => useDashboardConfig(undefined));

    expect(result.current.isLoading).toBe(false);
    expect(clientModule.get).not.toHaveBeenCalled();
  });

  it('should add a widget to config', async () => {
    vi.mocked(clientModule.get).mockResolvedValue(mockConfig);
    vi.mocked(clientModule.put).mockResolvedValue({
      ...mockConfig,
      widgets: [
        ...mockConfig.widgets,
        {
          widgetKey: 'activity',
          widgetType: 'ActivityWidget',
          position: { column: 'left' as const, order: 1 },
        },
      ],
    });

    const { result } = renderHook(() => useDashboardConfig('test-user'));

    await waitFor(() => {
      expect(result.current.config).toBeDefined();
    });

    const newWidget = {
      widgetKey: 'activity',
      widgetType: 'ActivityWidget',
      position: { column: 'left' as const, order: 1 },
    };

    await result.current.addWidget(newWidget);

    expect(clientModule.put).toHaveBeenCalled();
    expect(result.current.config?.widgets.length).toBe(3);
  });

  it('should remove a widget from config', async () => {
    vi.mocked(clientModule.get).mockResolvedValue(mockConfig);
    vi.mocked(clientModule.put).mockResolvedValue({
      ...mockConfig,
      widgets: mockConfig.widgets.filter((w) => w.widgetKey !== 'workflow'),
    });

    const { result } = renderHook(() => useDashboardConfig('test-user'));

    await waitFor(() => {
      expect(result.current.config).toBeDefined();
    });

    await result.current.removeWidget('workflow');

    expect(clientModule.put).toHaveBeenCalled();
    expect(result.current.config?.widgets.length).toBe(1);
  });

  it('should update widget settings', async () => {
    vi.mocked(clientModule.get).mockResolvedValue(mockConfig);
    const updatedConfig = {
      ...mockConfig,
      widgets: mockConfig.widgets.map((w) =>
        w.widgetKey === 'workflow'
          ? { ...w, settings: { refreshInterval: 60000 } }
          : w
      ),
    };
    vi.mocked(clientModule.put).mockResolvedValue(updatedConfig);

    const { result } = renderHook(() => useDashboardConfig('test-user'));

    await waitFor(() => {
      expect(result.current.config).toBeDefined();
    });

    await result.current.updateWidget('workflow', {
      settings: { refreshInterval: 60000 },
    });

    expect(clientModule.put).toHaveBeenCalled();
    const updatedWidget = result.current.config?.widgets.find(
      (w: WidgetConfig) => w.widgetKey === 'workflow'
    );
    expect(updatedWidget?.settings).toEqual({ refreshInterval: 60000 });
  });

  it('should reorder a widget', async () => {
    vi.mocked(clientModule.get).mockResolvedValue(mockConfig);
    const reorderedConfig = {
      ...mockConfig,
      widgets: mockConfig.widgets.map((w) =>
        w.widgetKey === 'welcome' ? { ...w, position: { column: 'right' as const, order: 1 } } : w
      ),
    };
    vi.mocked(clientModule.put).mockResolvedValue(reorderedConfig);

    const { result } = renderHook(() => useDashboardConfig('test-user'));

    await waitFor(() => {
      expect(result.current.config).toBeDefined();
    });

    await result.current.reorderWidget('welcome', 'right', 1);

    expect(clientModule.put).toHaveBeenCalled();
    const movedWidget = result.current.config?.widgets.find(
      (w: WidgetConfig) => w.widgetKey === 'welcome'
    );
    expect(movedWidget?.position.column).toBe('right');
    expect(movedWidget?.position.order).toBe(1);
  });

  it('should throw error when adding widget without config loaded', async () => {
    const { result } = renderHook(() => useDashboardConfig());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    const newWidget = {
      widgetKey: 'test',
      widgetType: 'TestWidget',
      position: { column: 'left' as const, order: 0 },
    };

    await expect(result.current.addWidget(newWidget)).rejects.toThrow(
      'Configuration not loaded'
    );
  });

  it('should skip config load if autoRefresh is false', async () => {
    const { result } = renderHook(() => useDashboardConfig('test-user', false));

    expect(result.current.isLoading).toBe(false);
    expect(clientModule.get).not.toHaveBeenCalled();
  });
});
