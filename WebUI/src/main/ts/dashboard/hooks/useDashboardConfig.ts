/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import { useCallback, useEffect, useState } from "react";
import { get, isSessionRedirectError } from "../../api/client";
import type { ApiError } from "../../api/client";

export interface WidgetConfig {
  widgetKey: string;
  widgetType: string;
  position: {
    column: "left" | "right";
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

/**
 * Dashboard config mutations are **session-local** only: they update React
 * state and do not POST/PUT to the server. Classic dashboard persist expects
 * PSDashboard + Shindig gadget URLs; a first-class React layout persist is
 * not wired yet. Callers may still `await` these methods for a stable API.
 */
export interface UseDashboardConfigResult {
  config: DashboardConfig | null;
  isLoading: boolean;
  /** Soft error — UI should still show default gadgets. */
  error: string | null;
  /** Session-local only — no server I/O (see interface note). */
  saveConfig: (newConfig: DashboardConfig) => Promise<void>;
  /** Session-local only — no server I/O. */
  addWidget: (widget: WidgetConfig) => Promise<void>;
  /** Session-local only — no server I/O. */
  removeWidget: (widgetKey: string) => Promise<void>;
  /** Session-local only — no server I/O. */
  updateWidget: (widgetKey: string, updates: Partial<WidgetConfig>) => Promise<void>;
  /** Session-local only — no server I/O. */
  reorderWidget: (
    widgetKey: string,
    column: "left" | "right",
    order: number,
  ) => Promise<void>;
}

/** Live dashboard REST root (session user is implied — no path userId). */
export const DASHBOARD_API = "/services/dashboardmanagement/dashboard";

/**
 * Map classic Shindig gadget URL file name → React widget key when known.
 * Unknown legacy gadgets are skipped (React registry has no peer).
 */
export function mapClassicGadgetUrlToWidgetKey(url: string): string | null {
  if (!url) {
    return null;
  }
  const file = url.split("/").pop()?.toLowerCase() ?? "";
  // Product / common mappings (expand as React widgets are completed)
  if (file.includes("blog")) return "blogs";
  if (file.includes("workflow")) return "workflow";
  if (file.includes("activity") || file.includes("recent")) return "activity";
  if (file.includes("welcome")) return "welcome";
  if (file.includes("comment")) return "comments";
  if (file.includes("form")) return "forms-tracker";
  if (file.includes("traffic")) return "traffic";
  if (file.includes("report")) return "reports";
  if (file.includes("process") || file.includes("monitor")) return "process-monitor";
  if (file.includes("asset")) return "assets-status";
  if (file.includes("membership")) return "membership";
  if (file.includes("siteimprove")) return "siteimprove";
  if (file.includes("seo")) return "seo-audit";
  if (file.includes("cookie")) return "cookie-consent";
  if (file.includes("google")) return "google-setup";
  // External / third-party Shindig URLs (labpixies, etc.) → generic iframe
  if (file.endsWith(".xml") || url.includes("labpixies") || url.startsWith("http")) {
    return null; // no React peer for pure Shindig XML gadgets
  }
  return null;
}

/**
 * Normalize server dashboard payload (classic PSDashboard wire) to DashboardConfig.
 */
export function parseDashboardResponse(
  data: unknown,
  userId: string,
): DashboardConfig {
  const now = new Date().toISOString();
  const empty: DashboardConfig = {
    userId,
    widgets: [],
    createdAt: now,
    updatedAt: now,
  };

  if (!data || typeof data !== "object") {
    return empty;
  }

  const root = data as Record<string, unknown>;
  // Already modern shape
  if (Array.isArray(root.widgets)) {
    return {
      userId: String(root.userId ?? userId),
      widgets: root.widgets as WidgetConfig[],
      theme: root.theme ? String(root.theme) : undefined,
      refreshInterval:
        typeof root.refreshInterval === "number" ? root.refreshInterval : undefined,
      createdAt: String(root.createdAt ?? now),
      updatedAt: String(root.updatedAt ?? now),
    };
  }

  const dash =
    root.Dashboard && typeof root.Dashboard === "object"
      ? (root.Dashboard as Record<string, unknown>)
      : root;

  const gadgetsRaw = dash.gadgets;
  let gadgets: unknown[] = [];
  if (Array.isArray(gadgetsRaw)) {
    gadgets = gadgetsRaw;
  } else if (gadgetsRaw && typeof gadgetsRaw === "object") {
    // single gadget
    gadgets = [gadgetsRaw];
  }

  const widgets: WidgetConfig[] = [];
  const seen = new Set<string>();
  for (const g of gadgets) {
    if (!g || typeof g !== "object") continue;
    const og = g as Record<string, unknown>;
    const url = og.url != null ? String(og.url) : "";
    const key = mapClassicGadgetUrlToWidgetKey(url);
    if (!key || seen.has(key)) continue;
    seen.add(key);
    // Classic dashboard is a two-column layout (col 0 = left, any other = right).
    // Server col values outside {0,1} are rare; map non-zero to right until we
    // support multi-column React gadgets.
    const col = Number(og.col ?? 0) === 0 ? "left" : "right";
    const order = Number(og.row ?? widgets.length);
    widgets.push({
      widgetKey: key,
      widgetType: key,
      position: { column: col, order },
      settings: {},
    });
  }

  return {
    userId: String(dash.id ?? userId),
    widgets,
    createdAt: now,
    updatedAt: now,
  };
}

function formatErr(err: unknown): string {
  if (isSessionRedirectError(err)) {
    return "Session expired";
  }
  if (err && typeof err === "object" && "status" in err) {
    const api = err as ApiError;
    if (typeof api.body === "string" && api.body.trim()) {
      return api.body;
    }
    return `HTTP ${api.status}`;
  }
  if (err instanceof Error) {
    return err.message;
  }
  return "Failed to load dashboard configuration";
}

/**
 * Load and manage dashboard configuration for Home Gadgets.
 *
 * <p>Server API is {@code GET/POST /services/dashboardmanagement/dashboard}
 * (session user — <strong>not</strong> {@code /dashboard/{userId}}).</p>
 */
export const useDashboardConfig = (
  userId?: string,
  autoRefresh = true,
): UseDashboardConfigResult => {
  const [config, setConfig] = useState<DashboardConfig | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(userId && autoRefresh));
  const [error, setError] = useState<string | null>(null);

  const loadConfig = useCallback(async () => {
    if (!userId) {
      setIsLoading(false);
      return;
    }
    try {
      setIsLoading(true);
      setError(null);
      const response = await get<unknown>(DASHBOARD_API);
      setConfig(parseDashboardResponse(response, userId));
    } catch (err: unknown) {
      if (isSessionRedirectError(err)) {
        return;
      }
      // Soft-fail: Gadgets section must still render DEFAULT_GADGETS
      console.error("useDashboardConfig error:", err);
      setError(formatErr(err));
      setConfig(null);
    } finally {
      setIsLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    if (autoRefresh && userId) {
      void loadConfig();
    } else {
      setIsLoading(false);
    }
  }, [userId, autoRefresh, loadConfig]);

  /**
   * Session-local only: updates React state; does not call the server.
   * Classic POST expects PSDashboard + Shindig URLs; React persist is deferred.
   */
  const saveConfig = async (newConfig: DashboardConfig) => {
    if (!userId) {
      throw new Error("User ID is required to save dashboard configuration");
    }
    setConfig(newConfig);
  };

  /** Session-local only — no server I/O. */
  const addWidget = async (widget: WidgetConfig) => {
    const base: DashboardConfig =
      config ??
      ({
        userId: userId ?? "",
        widgets: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      } as DashboardConfig);

    const newConfig: DashboardConfig = {
      ...base,
      widgets: [...base.widgets, widget],
      updatedAt: new Date().toISOString(),
    };
    setConfig(newConfig);
  };

  /** Session-local only — no server I/O. */
  const removeWidget = async (widgetKey: string) => {
    if (!config) return;
    setConfig({
      ...config,
      widgets: config.widgets.filter((w) => w.widgetKey !== widgetKey),
      updatedAt: new Date().toISOString(),
    });
  };

  /** Session-local only — no server I/O. */
  const updateWidget = async (
    widgetKey: string,
    updates: Partial<WidgetConfig>,
  ) => {
    if (!config) return;
    setConfig({
      ...config,
      widgets: config.widgets.map((w) =>
        w.widgetKey === widgetKey ? { ...w, ...updates } : w,
      ),
      updatedAt: new Date().toISOString(),
    });
  };

  /** Session-local only — no server I/O. */
  const reorderWidget = async (
    widgetKey: string,
    column: "left" | "right",
    order: number,
  ) => {
    if (!config) return;
    setConfig({
      ...config,
      widgets: config.widgets.map((w) =>
        w.widgetKey === widgetKey
          ? { ...w, position: { column, order } }
          : w,
      ),
      updatedAt: new Date().toISOString(),
    });
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
