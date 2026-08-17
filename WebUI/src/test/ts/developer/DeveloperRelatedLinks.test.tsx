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

import React from "react";
import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { DeveloperRelatedLinks } from "../../../main/ts/developer/DeveloperRelatedLinks";

const bootstrapState = {
  isAdmin: true,
  isDesigner: true,
  isWidgetBuilderActive: true,
};

vi.mock("../../../main/ts/app/bootstrap/BootstrapContext", () => ({
  useSpaBootstrap: () => bootstrapState,
}));

describe("DeveloperRelatedLinks (#3514)", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
    bootstrapState.isAdmin = true;
    bootstrapState.isDesigner = true;
    bootstrapState.isWidgetBuilderActive = true;
  });

  afterEach(() => {
    cleanup();
  });

  it("links Design and Widget Builder as Developer sub-entries, not top-nav", () => {
    render(
      <MemoryRouter basename="/cm/app" initialEntries={["/cm/app/developer"]}>
        <DeveloperRelatedLinks />
      </MemoryRouter>,
    );
    const design = screen.getByTestId("developer-design-library-link");
    const wb = screen.getByTestId("developer-widget-builder-link");
    expect(design.getAttribute("href") || "").toMatch(/\/design$/);
    expect(wb.getAttribute("href") || "").toMatch(/\/widget-builder$/);
    expect(design.textContent).toMatch(/Design/i);
    expect(wb.textContent).toMatch(/Widget Builder/i);
  });

  it("hides Widget Builder when the feature is inactive", () => {
    bootstrapState.isWidgetBuilderActive = false;
    render(
      <MemoryRouter basename="/cm/app" initialEntries={["/cm/app/developer"]}>
        <DeveloperRelatedLinks />
      </MemoryRouter>,
    );
    expect(screen.getByTestId("developer-design-library-link")).toBeTruthy();
    expect(screen.queryByTestId("developer-widget-builder-link")).toBeNull();
  });
});
