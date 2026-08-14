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
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DeveloperShell } from "../../../main/ts/developer/DeveloperShell";

vi.mock("../../../main/ts/developer/ContentTypesPanel", () => ({
  ContentTypesPanel: () => (
    <div data-testid="developer-ct-panel">Content types</div>
  ),
}));

vi.mock("../../../main/ts/developer/TemplatesPanel", () => ({
  TemplatesPanel: () => {
    throw new Error("workflowName is not defined");
  },
}));

describe("DeveloperShell Template isolation (#3377)", () => {
  it("keeps Developer shell when TemplatesPanel throws (no RouteErrorBoundary)", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => undefined);
    render(<DeveloperShell initialSection="templates" embedded />);

    expect(screen.getByTestId("perc-developer-shell")).toBeDefined();
    expect(screen.getByTestId("developer-section-error")).toBeDefined();
    expect(screen.getByTestId("developer-section-error").textContent).toMatch(
      /Templates/i,
    );
    expect(screen.queryByTestId("route-error")).toBeNull();
    expect(screen.queryByText(/Unable to load Developer/i)).toBeNull();

    fireEvent.click(screen.getByTestId("tab-developer-content-types"));
    expect(screen.getByTestId("developer-ct-panel")).toBeDefined();
    spy.mockRestore();
  });
});
