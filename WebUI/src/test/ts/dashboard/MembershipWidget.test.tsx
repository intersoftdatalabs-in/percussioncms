/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MembershipWidget } from "@/dashboard/MembershipWidget";
import * as api from "@/api/dashboard/deliveryGadgetsApi";

vi.mock("@/api/dashboard/deliveryGadgetsApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/api/dashboard/deliveryGadgetsApi")>();
  return { ...actual, fetchDefaultMembershipUsers: vi.fn() };
});

describe("MembershipWidget", () => {
  beforeEach(() => {
    vi.mocked(api.fetchDefaultMembershipUsers).mockReset();
  });

  it("lists members", async () => {
    vi.mocked(api.fetchDefaultMembershipUsers).mockResolvedValue({
      site: "Demo",
      users: [{ email: "user@example.com", status: "Active" }],
    });
    render(<MembershipWidget refreshInterval={0} />);
    await waitFor(() => {
      expect(screen.getByTestId("membership-list")).toBeDefined();
    });
    expect(screen.getByText("user@example.com")).toBeDefined();
  });
});
