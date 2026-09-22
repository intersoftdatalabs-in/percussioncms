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

const createContext = vi.fn();
const listContexts = vi.fn();
const listSchemesForContext = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  listContexts: (...args: unknown[]) => listContexts(...args),
  listSchemesForContext: (...args: unknown[]) => listSchemesForContext(...args),
  createContext: (...args: unknown[]) => createContext(...args),
  updateContext: vi.fn(),
  deleteContext: vi.fn(),
  createScheme: vi.fn(),
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

describe("ContextsPanel design context save", () => {
  it("creates a context then returns to the list", async () => {
    listContexts.mockResolvedValue([]);
    listSchemesForContext.mockResolvedValue([]);
    createContext.mockResolvedValue({ contextId: "12", name: "NightCtx" });
    renderPanel();
    await waitFor(() => expect(listContexts).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("design-add-context"));
    await waitFor(() => expect(screen.getByTestId("context-editor")).toBeTruthy());
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "NightCtx" },
    });
    fireEvent.click(screen.getByTestId("context-save"));
    await waitFor(() => expect(createContext).toHaveBeenCalled());
    await waitFor(() => expect(screen.queryByTestId("context-editor")).toBeNull());
  });

  it("shows 409 conflict on the context editor", async () => {
    listContexts.mockResolvedValue([]);
    createContext.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Publishing context name already exists" },
    });
    renderPanel();
    await waitFor(() => expect(listContexts).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("design-add-context"));
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "Dup" },
    });
    fireEvent.click(screen.getByTestId("context-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Publishing context name already exists",
    );
  });
});
