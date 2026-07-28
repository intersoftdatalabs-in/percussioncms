/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { IframeWidget } from "@/dashboard/IframeWidget";

describe("IframeWidget", () => {
  it("shows shell without src", () => {
    render(<IframeWidget />);
    expect(screen.getByTestId("iframe-widget")).toBeDefined();
    expect(screen.getByText(/Not available in React Home/i)).toBeDefined();
  });

  it("embeds when src provided", () => {
    render(<IframeWidget src="https://example.com" title="Embed" />);
    const iframe = screen.getByTitle("Embed") as HTMLIFrameElement;
    expect(iframe.src).toContain("example.com");
  });
});
