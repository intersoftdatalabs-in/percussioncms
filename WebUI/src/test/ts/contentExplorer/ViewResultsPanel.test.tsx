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

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ViewResultsPanel } from "../../../main/ts/contentExplorer/ViewResultsPanel";
import { renderA11yGate } from "./a11y";

describe("ViewResultsPanel (#3116)", () => {
  it("renders open/reveal rows and invokes host callbacks", async () => {
    const onOpen = vi.fn();
    const onReveal = vi.fn();
    const { container } = render(
      <ViewResultsPanel
        status={{
          kind: "ready",
          label: "View_All",
          results: {
            children: [
              {
                id: "7",
                title: "Welcome",
                folderPath: "/Sites/Demo",
                type: "page",
              },
            ],
            totalCount: 1,
            startIndex: 1,
          },
        }}
        onOpen={onOpen}
        onReveal={onReveal}
      />,
    );
    expect(screen.getByTestId("explorer-view-results-list")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("explorer-view-open-7"));
    fireEvent.click(screen.getByTestId("explorer-view-reveal-7"));
    expect(onOpen.mock.calls[0]?.[0]?.id).toBe("7");
    expect(onReveal.mock.calls[0]?.[0]?.folderPath).toBe("/Sites/Demo");
    await renderA11yGate(container);
  });

  it("shows empty and error states", () => {
    const { rerender } = render(
      <ViewResultsPanel
        status={{ kind: "ready", label: "Empty", results: { children: [] } }}
      />,
    );
    expect(screen.getByTestId("explorer-view-results-empty")).toBeInTheDocument();
    rerender(
      <ViewResultsPanel
        status={{ kind: "error", label: "Broken", message: "nope" }}
        onRetry={() => undefined}
      />,
    );
    expect(screen.getByTestId("explorer-view-results-error")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-view-results-retry")).toBeInTheDocument();
  });
});
