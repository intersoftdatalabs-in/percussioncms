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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DeveloperSectionErrorBoundary } from "../../../main/ts/developer/DeveloperSectionErrorBoundary";

function Bomb(): React.ReactElement {
  throw new Error("workflowName is not defined");
}

describe("DeveloperSectionErrorBoundary", () => {
  it("isolates a child TypeError so Developer chrome stays mounted (#3377)", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => undefined);
    render(
      <div>
        <div data-testid="perc-developer-shell">Developer chrome</div>
        <DeveloperSectionErrorBoundary label="Templates">
          <Bomb />
        </DeveloperSectionErrorBoundary>
      </div>,
    );
    expect(screen.getByTestId("perc-developer-shell")).toBeDefined();
    expect(screen.getByTestId("developer-section-error")).toBeDefined();
    expect(screen.getByTestId("developer-section-error").textContent).toMatch(
      /Templates/i,
    );
    expect(screen.queryByTestId("route-error")).toBeNull();
    spy.mockRestore();
  });

  it("renders children when they do not throw", () => {
    render(
      <DeveloperSectionErrorBoundary label="Templates">
        <div data-testid="ok-child">ok</div>
      </DeveloperSectionErrorBoundary>,
    );
    expect(screen.getByTestId("ok-child")).toBeDefined();
    expect(screen.queryByTestId("developer-section-error")).toBeNull();
  });
});
