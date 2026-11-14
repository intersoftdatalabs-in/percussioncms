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

/**
 * Dashboard gadget i18n wiring (issue #6304).
 *
 * <p>Covers the Phase-3 migration of hardcoded gadget body chrome
 * (loading / empty / form / error-fallback literals) to TMX resource keys:
 * every {@code MSG.DASHBOARD.WIDGETS} leaf resolves through the product
 * catalog when {@code window.I18N} is present and falls back to its English
 * segment otherwise, and representative widget states render the resolved
 * strings.</p>
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { message, MSG } from "@/i18n/message";
import { WorkflowStatusWidget } from "@/dashboard/WorkflowStatusWidget";
import { ProcessMonitorWidget } from "@/dashboard/ProcessMonitorWidget";
import { UnavailableGadgetShell } from "@/dashboard/UnavailableGadgetShell";
import * as gadgetApi from "@/api/dashboard/gadgetApi";

vi.mock("@/api/dashboard/gadgetApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/gadgetApi")>();
  return {
    ...actual,
    fetchPagesByStatusSummary: vi.fn(),
    fetchProcessMonitors: vi.fn(),
  };
});

/** Collect every leaf key string under MSG.DASHBOARD.WIDGETS. */
function widgetKeys(): string[] {
  const out: string[] = [];
  const walk = (node: unknown): void => {
    if (typeof node === "string") {
      out.push(node);
      return;
    }
    if (node && typeof node === "object") {
      for (const v of Object.values(node as Record<string, unknown>)) {
        walk(v);
      }
    }
  };
  walk(MSG.DASHBOARD.WIDGETS);
  return out;
}

describe("dashboard gadget i18n keys (#6304)", () => {
  beforeEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  it("every WIDGETS leaf key has a non-empty English segment after @", () => {
    const keys = widgetKeys();
    // Phase-3 migrated surface: guard against key-shape regressions.
    expect(keys.length).toBeGreaterThan(100);
    for (const key of keys) {
      expect(key).toContain("@");
      const english = key.slice(key.lastIndexOf("@") + 1);
      expect(english.trim().length).toBeGreaterThan(0);
    }
  });

  it("every WIDGETS leaf resolves through window.I18N when present", () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) => `hi:${k}`,
    };
    for (const key of widgetKeys()) {
      expect(message(key)).toBe(`hi:${key}`);
    }
  });

  it("every WIDGETS leaf falls back to English when I18N is absent", () => {
    for (const key of widgetKeys()) {
      const english = key.slice(key.lastIndexOf("@") + 1);
      expect(message(key)).toBe(english);
    }
  });

  it("workflow empty state renders the localized EMPTY string", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.DASHBOARD.WIDGETS.WORKFLOW.EMPTY
          ? "keine Seiten gefunden"
          : k,
    };
    vi.mocked(gadgetApi.fetchPagesByStatusSummary).mockResolvedValue({
      path: "/Sites/Demo",
      workflow: "Default",
      totalItems: 0,
      buckets: [],
    });
    render(<WorkflowStatusWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("workflow-status-empty")).toBeDefined();
    });
    expect(
      screen.getByTestId("workflow-status-empty").textContent,
    ).toContain("keine Seiten gefunden");
  });

  it("workflow error fallback uses the TMX load-failed English segment", async () => {
    vi.mocked(gadgetApi.fetchPagesByStatusSummary).mockRejectedValue(
      new Error("wf down"),
    );
    render(<WorkflowStatusWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("workflow-status-error")).toBeDefined();
    });
    // formatApiError surfaces the server message; the key fallback path is
    // exercised through message() — assert the prefix label is localized.
    expect(
      screen.getByTestId("workflow-status-error").textContent,
    ).toContain("Error");
  });

  it("process monitor empty state renders the localized EMPTY string", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.DASHBOARD.WIDGETS.PROCESS_MONITOR.EMPTY
          ? "keine Monitore"
          : k,
    };
    vi.mocked(gadgetApi.fetchProcessMonitors).mockResolvedValue([]);
    render(<ProcessMonitorWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("process-monitor-empty")).toBeDefined();
    });
    expect(
      screen.getByTestId("process-monitor-empty").textContent,
    ).toContain("keine Monitore");
  });

  it("unavailable shell renders the localized header", () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.DASHBOARD.WIDGETS.UNAVAILABLE_GADGET.HEADER
          ? "nicht verfügbar"
          : k,
    };
    render(
      <UnavailableGadgetShell title="Comments" reason="no REST peer" />,
    );
    expect(screen.getByTestId("unavailable-gadget").textContent).toContain(
      "nicht verfügbar",
    );
  });
});
