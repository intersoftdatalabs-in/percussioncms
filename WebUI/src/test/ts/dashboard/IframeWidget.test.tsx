/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, it, expect, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import {
  IframeWidget,
  sanitizeEmbedUrl,
} from "@/dashboard/IframeWidget";

describe("sanitizeEmbedUrl", () => {
  it("allows http and https only", () => {
    expect(sanitizeEmbedUrl("https://example.com/x")).toBe(
      "https://example.com/x",
    );
    expect(sanitizeEmbedUrl("http://example.com")).toMatch(/^http:\/\//);
  });

  it("blocks dangerous schemes and garbage", () => {
    expect(sanitizeEmbedUrl("javascript:alert(1)")).toBeNull();
    expect(sanitizeEmbedUrl("data:text/html,<h1>x</h1>")).toBeNull();
    expect(sanitizeEmbedUrl("vbscript:msgbox")).toBeNull();
    expect(sanitizeEmbedUrl("not a url")).toBeNull();
    expect(sanitizeEmbedUrl("")).toBeNull();
  });
});

describe("IframeWidget", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it("prompts for URL when empty", () => {
    render(<IframeWidget />);
    expect(screen.getByTestId("iframe-widget")).toBeDefined();
    expect(screen.getByTestId("iframe-src-input")).toBeDefined();
    expect(screen.getByText(/Enter an https URL/i)).toBeDefined();
  });

  it("loads safe URL into iframe", () => {
    render(<IframeWidget title="Embed" />);
    fireEvent.change(screen.getByTestId("iframe-src-input"), {
      target: { value: "https://example.com/path" },
    });
    fireEvent.click(screen.getByTestId("iframe-src-apply"));
    const iframe = screen.getByTitle("Embed") as HTMLIFrameElement;
    expect(iframe.src).toContain("example.com");
  });

  it("rejects javascript: URLs", () => {
    render(<IframeWidget title="Embed" />);
    fireEvent.change(screen.getByTestId("iframe-src-input"), {
      target: { value: "javascript:alert(1)" },
    });
    fireEvent.click(screen.getByTestId("iframe-src-apply"));
    expect(screen.getByTestId("iframe-src-error")).toBeDefined();
    expect(screen.queryByTitle("Embed")).toBeNull();
  });

  it("uses localized External Content fallback for iframe accessible name when title omitted", () => {
    // Without I18N, message() falls back to text after @ in MSG.GADGET_EXTERNAL_CONTENT
    render(<IframeWidget />);
    fireEvent.change(screen.getByTestId("iframe-src-input"), {
      target: { value: "https://example.com/embed" },
    });
    fireEvent.click(screen.getByTestId("iframe-src-apply"));
    const iframe = screen.getByTitle("External Content") as HTMLIFrameElement;
    expect(iframe.src).toContain("example.com");
    // Widget heading and iframe title stay aligned for a11y
    expect(screen.getByText("External Content")).toBeDefined();
  });
});
