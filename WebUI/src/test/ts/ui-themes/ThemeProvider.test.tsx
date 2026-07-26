/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import { describe, it, expect, beforeEach } from "vitest";
import React from "react";
import { render, screen, cleanup } from "@testing-library/react";
import { ThemeProvider, useTheme } from "../../../main/ts/ui-themes/ThemeProvider";
import { BrandBar, BrandFooter } from "../../../main/ts/ui-themes/components/Branding";
import { intersoftTheme } from "../../../main/ts/ui-themes/intersoft/intersoftTheme";

function CaptureTheme({ testId }: { testId: string }): React.ReactElement {
  const t = useTheme();
  return React.createElement(
    "div",
    { "data-testid": testId, "data-id": t.meta.id },
  );
}

describe("ThemeProvider", () => {
  beforeEach(() => cleanup());

  it("injects the intersoft theme by default and sets data-perc-theme", () => {
    render(
      React.createElement(
        ThemeProvider,
        { "data-testid": "scope" },
        React.createElement(CaptureTheme, { testId: "child" }),
      ),
    );
    const scope = screen.getByTestId("scope");
    expect(scope.getAttribute("data-perc-theme")).toBe("intersoft");
    expect(screen.getByTestId("child").getAttribute("data-id")).toBe("intersoft");
  });

  it("writes CSS custom properties for the active theme on the scope element", () => {
    render(
      React.createElement(
        ThemeProvider,
        { "data-testid": "scope" },
        React.createElement(CaptureTheme, { testId: "child" }),
      ),
    );
    const scope = screen.getByTestId("scope") as unknown as HTMLElement;
    const inline = (scope.getAttribute("style") ?? "").toLowerCase();
    expect(inline).toContain("--color-brand-500");
    expect(inline).toContain("--color-accent");
    expect(inline).toContain("--font-font-family");
  });

  it("respects an explicit theme override (tests/preview)", () => {
    const custom = {
      ...intersoftTheme,
      meta: { ...intersoftTheme.meta, id: "test" },
    };
    render(
      React.createElement(
        ThemeProvider,
        { theme: custom, "data-testid": "scope" },
        React.createElement(CaptureTheme, { testId: "child" }),
      ),
    );
    expect(screen.getByTestId("scope").getAttribute("data-perc-theme")).toBe("test");
    expect(screen.getByTestId("child").getAttribute("data-id")).toBe("test");
  });

  it("falls back to the active theme when useTheme is called outside a provider", () => {
    render(
      React.createElement(CaptureTheme, { testId: "orphan" }),
    );
    expect(screen.getByTestId("orphan").getAttribute("data-id")).toBe("intersoft");
  });
});

describe("BrandBar / BrandFooter", () => {
  beforeEach(() => cleanup());

  it("renders the brand logo and product name in the header", () => {
    render(React.createElement(BrandBar));
    const logo = screen.getByTestId("perc-brand-logo");
    expect(logo.getAttribute("src")).toBe(
      "/cm/themes/intersoft/brand/intersoft-logo-horizontal.png",
    );
    expect(screen.getByTestId("perc-brand-product").textContent).toBe(
      "Percussion CMS",
    );
    expect(screen.getByTestId("perc-brand-tagline").textContent).toContain(
      "Intelligent",
    );
  });

  it("renders a footer that credits Intersoft Data Labs and links to intsof.com", () => {
    render(React.createElement(BrandFooter));
    const footer = screen.getByTestId("perc-brand-footer");
    expect(footer.textContent).toContain("Powered by");
    const link = footer.querySelector("a");
    expect(link?.getAttribute("href")).toBe("https://intsof.com/");
    expect(link?.textContent).toBe("Intersoft Data Labs");
  });

  it("renders the BrandBar in the document banner role", () => {
    render(React.createElement(BrandBar));
    const bar = screen.getByTestId("perc-brand-bar");
    expect(bar.tagName.toLowerCase()).toBe("header");
  });
});
