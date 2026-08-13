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
import { AdminSectionErrorBoundary } from "../../../main/ts/admin/AdminSectionErrorBoundary";

function Bomb(): React.ReactElement {
  throw new Error("map is not a function");
}

describe("AdminSectionErrorBoundary", () => {
  it("isolates a child TypeError so siblings stay mounted (#3195)", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => undefined);
    render(
      <div>
        <div data-testid="admin-chrome">Admin chrome</div>
        <AdminSectionErrorBoundary label="System Tools">
          <Bomb />
        </AdminSectionErrorBoundary>
      </div>,
    );
    expect(screen.getByTestId("admin-chrome")).toBeDefined();
    expect(screen.getByTestId("admin-section-error")).toBeDefined();
    expect(screen.getByTestId("admin-section-error").textContent).toMatch(
      /System Tools/i,
    );
    spy.mockRestore();
  });

  it("renders children when they do not throw", () => {
    render(
      <AdminSectionErrorBoundary label="System Tools">
        <div data-testid="ok-child">ok</div>
      </AdminSectionErrorBoundary>,
    );
    expect(screen.getByTestId("ok-child")).toBeDefined();
    expect(screen.queryByTestId("admin-section-error")).toBeNull();
  });
});
