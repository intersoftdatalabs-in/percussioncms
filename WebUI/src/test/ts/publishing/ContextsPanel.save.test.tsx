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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ContextsPanel } from "@/publishing/design/ContextsPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

const createScheme = vi.fn();
const listContexts = vi.fn();
const listSchemesForContext = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  listContexts: (...args: unknown[]) => listContexts(...args),
  listSchemesForContext: (...args: unknown[]) => listSchemesForContext(...args),
  createContext: vi.fn(),
  updateContext: vi.fn(),
  deleteContext: vi.fn(),
  createScheme: (...args: unknown[]) => createScheme(...args),
  updateScheme: vi.fn(),
  deleteScheme: vi.fn(),
  getScheme: vi.fn(),
}));

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <ContextsPanel />
    </DirtyFormProvider>,
  );
}

describe("ContextsPanel location scheme save", () => {
  it("creates a scheme then returns to the list", async () => {
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    listSchemesForContext.mockResolvedValue([]);
    createScheme.mockResolvedValue({ schemeId: "9", name: "NightScheme" });
    renderPanel();
    await waitFor(() => expect(listContexts).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("design-add-location-scheme"));
    await waitFor(() => expect(screen.getByTestId("scheme-editor")).toBeTruthy());
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "NightScheme" },
    });
    fireEvent.change(screen.getByLabelText(/Generator/i), {
      target: {
        value: "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation",
      },
    });
    fireEvent.click(screen.getByTestId("location-scheme-save"));
    await waitFor(() => expect(createScheme).toHaveBeenCalled());
    await waitFor(() => expect(screen.queryByTestId("scheme-editor")).toBeNull());
  });

  it("shows 409 conflict on the scheme editor", async () => {
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    listSchemesForContext.mockResolvedValue([]);
    createScheme.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Location scheme name already exists" },
    });
    renderPanel();
    await waitFor(() => expect(listContexts).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("design-add-location-scheme"));
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "Dup" },
    });
    fireEvent.change(screen.getByLabelText(/Generator/i), {
      target: {
        value: "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation",
      },
    });
    fireEvent.click(screen.getByTestId("location-scheme-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Location scheme name already exists",
    );
  });
});
