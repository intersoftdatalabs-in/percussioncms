/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { DependencyViewer } from "../../../main/ts/contentExplorer/views/DependencyViewer";
import { renderA11yGate } from "./a11y";

describe("DependencyViewer", () => {
  it("renders the 6 dimension rows for a page item", () => {
    render(<DependencyViewer item={{ id: "p-1", type: "page", folderPath: "/Sites/Foo" }} aaLinkCount={2} />);
    expect(screen.getByTestId("dependency-viewer")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-outgoing")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-incoming")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-aa")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-taxonomy")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-local")).toBeTruthy();
    expect(screen.getByTestId("dependency-row-reverse")).toBeTruthy();
  });

  it("shows the AA count when known", () => {
    render(<DependencyViewer item={{ id: "x", folderPath: "/p" }} aaLinkCount={5} />);
    const row = screen.getByTestId("dependency-row-aa");
    expect(row.textContent).toContain("Active Assembly links");
    expect(row.textContent).toContain("5 AA links");
  });

  it("labels non-AA dimensions as \u2014 (per the rest gap)", () => {
    render(<DependencyViewer item={{ id: "x", folderPath: "/p" }} aaLinkCount={0} />);
    expect(screen.getByTestId("dependency-row-outgoing").textContent).toMatch(/\u2014/);
    expect(screen.getByTestId("dependency-row-taxonomy").textContent).toMatch(/\u2014/);
  });

  it("shows the client-side preview banner", () => {
    render(<DependencyViewer item={{ id: "x" }} />);
    expect(screen.getByTestId("dependency-client-side-preview")).toBeTruthy();
  });

  it("passes the zero serious/critical axe-core gate", async () => {
    const { container } = render(
      <DependencyViewer item={{ id: "x", folderPath: "/p" }} aaLinkCount={3} />
    );
    await renderA11yGate(container);
  });
});
