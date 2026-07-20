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
import { RelationshipsView } from "../../../main/ts/contentExplorer/views/RelationshipsView";
import { renderA11yGate } from "./a11y";

describe("RelationshipsView", () => {
  it("renders the 4 IA-primary rows + a supplementary details panel for AA / reverse", () => {
    render(
      <RelationshipsView
        item={{ id: "p-1", type: "page", folderPath: "/Sites/Foo" }}
        aaLinkCount={3}
      />,
    );
    expect(screen.getByTestId("relationships-view")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-outgoing")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-incoming")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-taxonomy")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-local")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-aa")).toBeTruthy();
    expect(screen.getByTestId("relationships-row-reverse")).toBeTruthy();
  });

  it("shows the client-side preview banner", () => {
    render(<RelationshipsView item={{ id: "x" }} />);
    expect(screen.getByTestId("relationships-client-side-preview")).toBeTruthy();
  });

  it("passes the zero serious/critical axe-core gate", async () => {
    const { container } = render(
      <RelationshipsView item={{ id: "x", folderPath: "/p" }} aaLinkCount={3} />
    );
    await renderA11yGate(container);
  });
});
