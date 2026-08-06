/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, it, expect, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { UnavailableView } from "@/home/UnavailableView";

describe("UnavailableView", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k) => k,
    };
  });

  it("renders moved/unavailable message", () => {
    render(<UnavailableView detail="/cm/pages/cui/index.html" />);
    expect(screen.getByTestId("unavailable-view")).toBeDefined();
    expect(screen.getByText("Unavailable")).toBeDefined();
    expect(screen.getByText("/cm/pages/cui/index.html")).toBeDefined();
  });
});
