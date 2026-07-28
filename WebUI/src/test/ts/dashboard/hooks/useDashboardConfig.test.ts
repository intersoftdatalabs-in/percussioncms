/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import {
  DASHBOARD_API,
  mapClassicGadgetUrlToWidgetKey,
  parseDashboardResponse,
  useDashboardConfig,
} from "@/dashboard/hooks/useDashboardConfig";
import * as clientModule from "@/api/client";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
  };
});

describe("useDashboardConfig", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("mapClassicGadgetUrlToWidgetKey maps known names", () => {
    expect(mapClassicGadgetUrlToWidgetKey("/path/blogs.xml")).toBe("blogs");
    expect(mapClassicGadgetUrlToWidgetKey("http://x/workflowStatus.xml")).toBe(
      "workflow",
    );
    expect(
      mapClassicGadgetUrlToWidgetKey(
        "http://www.labpixies.com/campaigns/todo/todo.xml",
      ),
    ).toBeNull();
  });

  it("parseDashboardResponse maps classic Dashboard gadgets", () => {
    const cfg = parseDashboardResponse(
      {
        Dashboard: {
          id: "Admin",
          gadgets: [
            {
              instanceId: 1,
              url: "/cm/gadgets/repository/perc/perc_blog_gadget.xml",
              col: 0,
              row: 0,
            },
            {
              instanceId: 2,
              url: "http://www.labpixies.com/campaigns/todo/todo.xml",
              col: 1,
              row: 0,
            },
          ],
        },
      },
      "Admin",
    );
    expect(cfg.userId).toBe("Admin");
    expect(cfg.widgets).toEqual([
      expect.objectContaining({
        widgetKey: "blogs",
        position: { column: "left", order: 0 },
      }),
    ]);
  });

  it("loads from session dashboard API without userId path segment", async () => {
    vi.mocked(clientModule.get).mockResolvedValue({
      Dashboard: {
        id: "Admin",
        gadgets: [
          {
            url: "/x/perc_blog_gadget.xml",
            col: 0,
            row: 0,
          },
        ],
      },
    });

    const { result } = renderHook(() => useDashboardConfig("Admin"));

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(clientModule.get).toHaveBeenCalledWith(DASHBOARD_API);
    expect(result.current.config?.widgets[0]?.widgetKey).toBe("blogs");
    expect(result.current.error).toBeNull();
  });

  it("soft-fails load error without blocking (null config)", async () => {
    vi.mocked(clientModule.get).mockRejectedValue({
      status: 500,
      statusText: "Error",
      body: "nope",
    });

    const { result } = renderHook(() => useDashboardConfig("Admin"));

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.config).toBeNull();
    expect(result.current.error).toBe("nope");
  });
});
