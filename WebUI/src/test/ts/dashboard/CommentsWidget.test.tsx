/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { CommentsWidget } from "@/dashboard/CommentsWidget";
import * as api from "@/api/dashboard/deliveryGadgetsApi";
import { MSG } from "@/i18n/message";

vi.mock("@/api/dashboard/deliveryGadgetsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/deliveryGadgetsApi")>();
  return { ...actual, fetchDefaultPagesWithComments: vi.fn() };
});

describe("CommentsWidget", () => {
  beforeEach(() => {
    vi.mocked(api.fetchDefaultPagesWithComments).mockReset();
    delete (window as { I18N?: unknown }).I18N;
  });

  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  it("localizes default title via I18N when key resolves (GH-1835)", async () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) =>
        k === MSG.GADGET_COMMENTS ? "टिप्पणियाँ" : k,
    };
    vi.mocked(api.fetchDefaultPagesWithComments).mockResolvedValue({
      site: "Demo",
      pages: [],
    });
    render(<CommentsWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("comments-widget")).toBeDefined();
    });
    const titleEl = screen.getByTestId("comments-widget").querySelector("div");
    expect(titleEl?.textContent).toBe("टिप्पणियाँ");
    expect(titleEl?.textContent).not.toBe("COMMENTS");
  });

  it("lists pages with comments", async () => {
    vi.mocked(api.fetchDefaultPagesWithComments).mockResolvedValue({
      site: "Demo",
      pages: [
        {
          id: "1",
          pageLinkTitle: "Home",
          commentCount: 3,
          newCount: 1,
          approvedCount: 2,
        },
      ],
    });
    render(<CommentsWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("comments-widget-list")).toBeDefined();
    });
    expect(screen.getByText("Home")).toBeDefined();
  });
});
