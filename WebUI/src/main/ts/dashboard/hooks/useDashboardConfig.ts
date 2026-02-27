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

import { useEffect, useState } from 'react';
import { get, put } from '../../api/client';

export interface WidgetConfig {
  widgetKey: string;
  widgetType: string;
  position: {
    column: 'left' | 'right';
    order: number;
  };
  settings?: Record<string, unknown>;
}

export interface DashboardConfig {
  userId: string;
  widgets: WidgetConfig[];
  theme?: string;
  refreshInterval?: number;
  createdAt: string;
  updatedAt: string;
}

export interface UseDashboardConfigResult {
  config: DashboardConfig | null;
  isLoading: boolean;
  error: string | null;
  saveConfig: (newConfig: DashboardConfig) => Promise<void>;
  addWidget: (widget: WidgetConfig) => Promise<void>;
  removeWidget: (widgetKey: string) => Promise<void>;
  updateWidget: (widgetKey: string, updates: Partial<WidgetConfig>) => Promise<void>;
  reorderWidget: (widgetKey: string, column: 'left' | 'right', order: number) => Promise<void>;
}

/**
 * Hook for loading and managing user dashboard configuration.
 *
 * <p>Fetches the user's persisted dashboard configuration from the backend
 * and provides methods to update widgets, save preferences, and manage layout.</p>
 *
 * @param userId - The current user's ID (required to load their config)
 * @param autoRefresh - Whether to automatically refresh config on mount (default: true)
 * @returns Dashboard configuration state and manipulation methods
 */
export const useDashboardConfig = (
  userId?: string,
  autoRefresh: boolean = true
): UseDashboardConfigResult => {
  const [config, setConfig] = useState<DashboardConfig | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Load dashboard config from server
  const loadConfig = async () => {
    if (!userId) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);

      const response = await get<DashboardConfig>(
        `/services/dashboardmanagement/dashboard/${userId}`
      );

      setConfig(response);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Failed to load dashboard configuration';
      setError(errorMessage);
      console.error('useDashboardConfig error:', err);
      // Return default config on error
      setConfig(null);
    } finally {
      setIsLoading(false);
    }
  };

  // Load config on mount if user ID is provided
  useEffect(() => {
    if (autoRefresh && userId) {
      loadConfig();
    } else {
      setIsLoading(false);
    }
  }, [userId, autoRefresh]);

  // Save entire config to server
  const saveConfig = async (newConfig: DashboardConfig) => {
    if (!userId) {
      throw new Error('User ID is required to save dashboard configuration');
    }

    try {
      const response = await put<DashboardConfig>(
        `/services/dashboardmanagement/dashboard/${userId}`,
        newConfig
      );

      setConfig(response);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Failed to save dashboard configuration';
      setError(errorMessage);
      throw err;
    }
  };

  // Add a single widget to the config
  const addWidget = async (widget: WidgetConfig) => {
    if (!config) {
      throw new Error('Configuration not loaded');
    }

    const newConfig: DashboardConfig = {
      ...config,
      widgets: [...config.widgets, widget],
      updatedAt: new Date().toISOString(),
    };

    await saveConfig(newConfig);
  };

  // Remove a widget from the config
  const removeWidget = async (widgetKey: string) => {
    if (!config) {
      throw new Error('Configuration not loaded');
    }

    const newConfig: DashboardConfig = {
      ...config,
      widgets: config.widgets.filter((w) => w.widgetKey !== widgetKey),
      updatedAt: new Date().toISOString(),
    };

    await saveConfig(newConfig);
  };

  // Update a specific widget's settings
  const updateWidget = async (widgetKey: string, updates: Partial<WidgetConfig>) => {
    if (!config) {
      throw new Error('Configuration not loaded');
    }

    const newConfig: DashboardConfig = {
      ...config,
      widgets: config.widgets.map((w) =>
        w.widgetKey === widgetKey ? { ...w, ...updates } : w
      ),
      updatedAt: new Date().toISOString(),
    };

    await saveConfig(newConfig);
  };

  // Reorder a widget in the layout
  const reorderWidget = async (widgetKey: string, column: 'left' | 'right', order: number) => {
    if (!config) {
      throw new Error('Configuration not loaded');
    }

    const newConfig: DashboardConfig = {
      ...config,
      widgets: config.widgets.map((w) =>
        w.widgetKey === widgetKey
          ? {
              ...w,
              position: { column, order },
            }
          : w
      ),
      updatedAt: new Date().toISOString(),
    };

    await saveConfig(newConfig);
  };

  return {
    config,
    isLoading,
    error,
    saveConfig,
    addWidget,
    removeWidget,
    updateWidget,
    reorderWidget,
  };
};

export default useDashboardConfig;
