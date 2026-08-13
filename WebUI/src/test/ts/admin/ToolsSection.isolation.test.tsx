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
import { ToolsSection } from "../../../main/ts/admin/tools/ToolsSection";

vi.mock("../../../main/ts/admin/tools/SecurityAuditLogViewer", () => ({
  SecurityAuditLogViewer: () => {
    throw new Error("map is not a function");
  },
}));

vi.mock("../../../main/ts/admin/tools/ConsistencyChecker", () => ({
  ConsistencyChecker: () => (
    <div data-testid="mock-consistency">Consistency</div>
  ),
}));

describe("ToolsSection isolation (#3195)", () => {
  it("keeps tools tablist when Security Audit Log throws; other tool still works", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => undefined);
    render(<ToolsSection />);

    expect(screen.getByTestId("perc-tools-section")).toBeDefined();
    expect(screen.getByTestId("perc-tools-tablist")).toBeDefined();
    expect(screen.getByTestId("admin-section-error")).toBeDefined();
    expect(screen.queryByTestId("route-error")).toBeNull();

    fireEvent.click(screen.getByTestId("tool-tab-consistency"));
    expect(screen.getByTestId("mock-consistency")).toBeDefined();
    spy.mockRestore();
  });
});
