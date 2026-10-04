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
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ContextsPanel } from "@/publishing/design/ContextsPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { LOCATION_SCHEME_NAME_MAX_LENGTH } from "@/publishing/locationSchemeCopy";

const createScheme = vi.fn();
const getScheme = vi.fn();
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
  getScheme: (...args: unknown[]) => getScheme(...args),
}));

const GENERATOR =
  "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation";

const sourceRow = {
  schemeId: "11",
  name: "Article",
  generator: GENERATOR,
  contextId: "3",
};

const fullSource = {
  ...sourceRow,
  description: "Pages",
  contentTypeId: 4,
  templateId: 8,
  parameters: [
    { name: "path", type: "String", value: "$sys.site.path", sequence: 0 },
  ],
};

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <ContextsPanel />
    </DirtyFormProvider>,
  );
}

async function openCopy(): Promise<void> {
  listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
  listSchemesForContext.mockResolvedValue([sourceRow]);
  renderPanel();
  await waitFor(() => expect(screen.getByTestId("location-scheme-copy")).toBeTruthy());
  fireEvent.click(screen.getByTestId("location-scheme-copy"));
  await waitFor(() => expect(screen.getByTestId("scheme-copy")).toBeTruthy());
}

describe("ContextsPanel location scheme copy", () => {
  beforeEach(() => {
    createScheme.mockReset();
    getScheme.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
  });

  it("creates a copy and lists the new name only after success", async () => {
    let resolveCreate: (value: unknown) => void = () => undefined;
    createScheme.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    getScheme.mockResolvedValue(fullSource);
    await openCopy();
    expect(screen.getByLabelText(/New name/i)).toHaveValue("Article copy");
    const listCallsBefore = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getByTestId("location-scheme-copy-submit"));
    await waitFor(() => expect(getScheme).toHaveBeenCalledWith("11"));
    await waitFor(() => expect(createScheme).toHaveBeenCalledTimes(1));
    expect(screen.getByTestId("scheme-copy")).toBeTruthy();
    expect(screen.queryByTestId("contexts-panel")).toBeNull();
    expect(listSchemesForContext.mock.calls.length).toBe(listCallsBefore);
    expect(createScheme).toHaveBeenCalledWith("3", {
      name: "Article copy",
      generator: GENERATOR,
      description: "Pages",
      contentTypeId: 4,
      templateId: 8,
      contextId: "3",
      copy: true,
      parameters: [
        { name: "path", type: "String", value: "$sys.site.path", sequence: 0 },
      ],
    });
    listSchemesForContext.mockResolvedValueOnce([
      sourceRow,
      { schemeId: "12", name: "Article copy", generator: GENERATOR },
    ]);
    resolveCreate({ schemeId: "12", name: "Article copy" });
    await waitFor(() => expect(screen.queryByTestId("scheme-copy")).toBeNull());
    expect(screen.getByTestId("contexts-panel")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Article copy" })).toBeTruthy();
  });

  it("rejects a blank name before any request", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("location-scheme-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    expect(getScheme).not.toHaveBeenCalled();
    expect(createScheme).not.toHaveBeenCalled();
    expect(screen.getByTestId("scheme-copy")).toBeTruthy();
  });

  it("rejects an overlong name before any request", async () => {
    await openCopy();
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "n".repeat(LOCATION_SCHEME_NAME_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("location-scheme-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Location scheme name must be 50 characters or fewer",
    );
    expect(getScheme).not.toHaveBeenCalled();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("cancel does not call the server", async () => {
    await openCopy();
    fireEvent.click(screen.getByTestId("location-scheme-copy-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-copy")).toBeNull());
    expect(getScheme).not.toHaveBeenCalled();
    expect(createScheme).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Article copy" })).toBeNull();
  });

  it.each([
    [400, "Bad Request", "name and generator are required"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Location scheme name already exists"],
  ])("HTTP %s does not claim the scheme was copied", async (status, statusText, message) => {
    getScheme.mockResolvedValue(fullSource);
    createScheme.mockRejectedValue({ status, statusText, body: { message } });
    await openCopy();
    const listCallsBefore = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getByTestId("location-scheme-copy-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.getByTestId("scheme-copy")).toBeTruthy();
    expect(listSchemesForContext.mock.calls.length).toBe(listCallsBefore);
    expect(screen.queryByRole("button", { name: "Article copy" })).toBeNull();
  });

  it("lists the created scheme when the refresh after success fails", async () => {
    let failRefresh = false;
    getScheme.mockResolvedValue(fullSource);
    createScheme.mockResolvedValue({ schemeId: "12", name: "Article copy" });
    listSchemesForContext.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [sourceRow];
    });
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("location-scheme-copy")).toBeTruthy());
    fireEvent.click(screen.getByTestId("location-scheme-copy"));
    await waitFor(() => expect(screen.getByTestId("scheme-copy")).toBeTruthy());
    failRefresh = true;
    fireEvent.click(screen.getByTestId("location-scheme-copy-submit"));
    await waitFor(() => expect(screen.queryByTestId("scheme-copy")).toBeNull());
    expect(screen.getByRole("button", { name: "Article copy" })).toBeTruthy();
  });
});
