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
import {
  normalizeAdminTool,
  ToolsSection,
} from "../../../main/ts/admin/tools/ToolsSection";

vi.mock("../../../main/ts/admin/tools/SecurityAuditLogViewer", () => ({
  SecurityAuditLogViewer: () => (
    <div data-testid="mock-security-audit">Security Audit</div>
  ),
}));

vi.mock("../../../main/ts/admin/tools/ConsistencyChecker", () => ({
  ConsistencyChecker: () => (
    <div data-testid="mock-consistency">Consistency</div>
  ),
}));

describe("ToolsSection", () => {
  it("normalizeAdminTool defaults to security-audit", () => {
    expect(normalizeAdminTool(undefined)).toBe("security-audit");
    expect(normalizeAdminTool("bogus")).toBe("security-audit");
    expect(normalizeAdminTool("consistency")).toBe("consistency");
  });

  it("defaults to security audit tool", () => {
    render(<ToolsSection />);
    expect(screen.getByTestId("perc-tools-section")).toBeDefined();
    expect(screen.getByTestId("tool-tab-security-audit")).toBeDefined();
    expect(screen.getByTestId("mock-security-audit")).toBeDefined();
  });

  it("switches to consistency checker", () => {
    render(<ToolsSection />);
    fireEvent.click(screen.getByTestId("tool-tab-consistency"));
    expect(screen.getByTestId("mock-consistency")).toBeDefined();
    expect(screen.queryByTestId("mock-security-audit")).toBeNull();
  });
});
