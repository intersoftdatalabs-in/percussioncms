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

import { cleanup, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { ArchitectureShell } from "../../../main/ts/architecture/ArchitectureShell";

describe("ArchitectureShell (#3094)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  afterEach(() => {
    cleanup();
  });

  it("renders shell chrome and empty/in-progress state", () => {
    render(<ArchitectureShell embedded />);
    const shell = screen.getByTestId("perc-architecture-shell");
    expect(shell).toBeTruthy();
    expect(shell.getAttribute("data-embedded")).toBe("true");
    expect(shell.getAttribute("data-site")).toBe("");
    expect(screen.getByTestId("architecture-shell-title").textContent).toMatch(
      /Architecture/i,
    );
    expect(screen.getByTestId("architecture-empty-state")).toBeTruthy();
    expect(screen.getByTestId("architecture-empty-title").textContent).toMatch(
      /coming soon/i,
    );
    expect(screen.getByTestId("architecture-site-hint").textContent).toMatch(
      /No site selected/i,
    );
  });

  it("surfaces optional site context from props", () => {
    render(<ArchitectureShell embedded initialSite="Corporate Investments" />);
    expect(
      screen.getByTestId("perc-architecture-shell").getAttribute("data-site"),
    ).toBe("Corporate Investments");
    expect(screen.getByTestId("architecture-site-hint").textContent).toContain(
      "Corporate Investments",
    );
  });
});
