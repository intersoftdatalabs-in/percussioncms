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

import { cleanup, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ExplorerRoute } from "../../../../main/ts/app/routes/ExplorerRoute";

vi.mock("../../../../main/ts/registry", () => ({
  loadComponent: async (name: string) => {
    if (name !== "ContentExplorerShell") {
      throw new Error(`unexpected component: ${name}`);
    }
    return function FakeExplorerShell(): React.ReactElement {
      return <div data-testid="content-explorer-shell">explorer</div>;
    };
  },
}));

describe("ExplorerRoute loading chrome (#3332)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("replaces explorer-route-loading with the shell after lazy load", async () => {
    render(
      <MemoryRouter initialEntries={["/explorer"]}>
        <Routes>
          <Route path="/explorer" element={<ExplorerRoute />} />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("content-explorer-shell")).toBeTruthy();
    });
    expect(screen.queryByTestId("explorer-route-loading")).toBeNull();
  });
});
