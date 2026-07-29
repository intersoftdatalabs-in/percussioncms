/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { IframeWidget } from "@/dashboard/IframeWidget";

describe("IframeWidget", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it("prompts for URL when empty", () => {
    render(<IframeWidget />);
    expect(screen.getByTestId("iframe-widget")).toBeDefined();
    expect(screen.getByTestId("iframe-src-input")).toBeDefined();
    expect(screen.getByText(/Enter a URL to embed/i)).toBeDefined();
  });

  it("loads URL into iframe", () => {
    render(<IframeWidget title="Embed" />);
    fireEvent.change(screen.getByTestId("iframe-src-input"), {
      target: { value: "https://example.com/path" },
    });
    fireEvent.click(screen.getByTestId("iframe-src-apply"));
    const iframe = screen.getByTitle("Embed") as HTMLIFrameElement;
    expect(iframe.src).toContain("example.com");
  });
});
