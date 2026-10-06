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
import { LOCATION_SCHEME_NAME_MAX_LENGTH } from "@/publishing/locationSchemeRename";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

const updateScheme = vi.fn();
const getScheme = vi.fn();
const listContexts = vi.fn();
const listSchemesForContext = vi.fn();
const createScheme = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  listContexts: (...args: unknown[]) => listContexts(...args),
  listSchemesForContext: (...args: unknown[]) => listSchemesForContext(...args),
  createContext: vi.fn(),
  updateContext: vi.fn(),
  deleteContext: vi.fn(),
  createScheme: (...args: unknown[]) => createScheme(...args),
  updateScheme: (...args: unknown[]) => updateScheme(...args),
  deleteScheme: vi.fn(),
  getScheme: (...args: unknown[]) => getScheme(...args),
}));

const GENERATOR =
  "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation";

const source = {
  schemeId: "11",
  name: "Article",
  generator: GENERATOR,
  description: "Pages",
  contentTypeId: 4,
  templateId: 8,
  contextId: "3",
  parameters: [{ name: "path", type: "String", value: "$sys.site.path", sequence: 0 }],
};

const other = {
  schemeId: "12",
  name: "Brief",
  generator: "legacy-gen",
  description: "Short",
  contentTypeId: 5,
  templateId: 9,
  contextId: "3",
};

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <ContextsPanel />
    </DirtyFormProvider>,
  );
}

async function openRename(): Promise<void> {
  listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
  listSchemesForContext.mockResolvedValue([source, other]);
  getScheme.mockResolvedValue(source);
  renderPanel();
  await waitFor(() => expect(screen.getAllByTestId("location-scheme-rename").length).toBe(2));
  fireEvent.click(screen.getAllByTestId("location-scheme-rename")[0]);
  await waitFor(() => expect(screen.getByTestId("scheme-rename")).toBeTruthy());
}

describe("ContextsPanel location scheme rename", () => {
  beforeEach(() => {
    updateScheme.mockReset();
    getScheme.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    createScheme.mockReset();
    listSchemesForContext.mockResolvedValue([source, other]);
    getScheme.mockResolvedValue(source);
  });

  it("shows the new name only after the rename reload and keeps the other fields", async () => {
    let releaseUpdate: (value: unknown) => void = () => undefined;
    updateScheme.mockImplementation(
      () =>
        new Promise((resolve) => {
          releaseUpdate = resolve;
        }),
    );
    let releaseList: ((rows: unknown) => void) | undefined;
    let listed = false;
    listSchemesForContext.mockImplementation(
      () =>
        new Promise((resolve) => {
          if (!listed) {
            resolve([source, other]);
            return;
          }
          releaseList = resolve;
        }),
    );
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);

    renderPanel();
    await waitFor(() => expect(screen.getAllByTestId("location-scheme-rename").length).toBe(2));
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getAllByTestId("location-scheme-rename")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-rename")).toBeTruthy());
    expect(getScheme).toHaveBeenCalledWith("11");
    expect(screen.getByLabelText("* Name")).toHaveValue("Article");
    expect(screen.getByTestId("scheme-rename-generator")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-rename-description")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-rename-content-type")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-rename-template")).toHaveTextContent("8");
    expect(screen.getByTestId("scheme-rename-parameter")).toHaveTextContent("$sys.site.path");

    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "Renamed" },
    });
    listed = true;
    fireEvent.click(screen.getByTestId("location-scheme-rename-submit"));
    await waitFor(() => expect(updateScheme).toHaveBeenCalledTimes(1));
    expect(updateScheme).toHaveBeenCalledWith("11", { name: "Renamed" });
    expect(screen.getByTestId("scheme-rename")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Renamed" })).toBeNull();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(createScheme).not.toHaveBeenCalled();

    releaseUpdate({ ...source, name: "Renamed" });
    await waitFor(() => expect(screen.queryByTestId("scheme-rename")).toBeNull());
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Renamed" })).toBeNull();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([
      { ...source, name: "Renamed" },
      other,
    ]);
    await waitFor(() => expect(screen.getByRole("button", { name: "Renamed" })).toBeTruthy());
    expect(screen.queryByRole("button", { name: "Article" })).toBeNull();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(screen.getByText(new RegExp(GENERATOR))).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("rejects a blank or overlong name without updating", async () => {
    await openRename();
    const loads = listSchemesForContext.mock.calls.length;
    const reads = getScheme.mock.calls.length;
    fireEvent.change(screen.getByLabelText("* Name"), { target: { value: " " } });
    fireEvent.click(screen.getByTestId("location-scheme-rename-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "n".repeat(LOCATION_SCHEME_NAME_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("location-scheme-rename-submit"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Location scheme name must be 50 characters or fewer",
    );
    expect(updateScheme).not.toHaveBeenCalled();
    expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
    expect(getScheme).toHaveBeenCalledTimes(reads);
    expect(screen.getByTestId("scheme-rename-generator")).toHaveTextContent(GENERATOR);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("location-scheme-rename-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-rename")).toBeNull());
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
  });

  it("cancel keeps the old name and does not update", async () => {
    await openRename();
    fireEvent.change(screen.getByLabelText("* Name"), { target: { value: "Nope" } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("location-scheme-rename-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-rename")).toBeNull());
    expect(updateScheme).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Nope" })).toBeNull();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it.each([
    [400, "Bad Request", "Location scheme name must be 50 characters or fewer"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Location scheme name already exists"],
  ])(
    "keeps the previous name when HTTP %s rejects the rename",
    async (status, statusText, message) => {
      updateScheme.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openRename();
      const loads = listSchemesForContext.mock.calls.length;
      fireEvent.change(screen.getByLabelText("* Name"), { target: { value: "Taken" } });
      fireEvent.click(screen.getByTestId("location-scheme-rename-submit"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("scheme-rename")).toBeTruthy();
      expect(screen.getByTestId("scheme-rename-generator")).toHaveTextContent(GENERATOR);
      expect(screen.getByTestId("scheme-rename-description")).toHaveTextContent("Pages");
      expect(screen.getByTestId("scheme-rename-parameter")).toHaveTextContent("$sys.site.path");
      expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("location-scheme-rename-cancel"));
      await waitFor(() => expect(screen.queryByTestId("scheme-rename")).toBeNull());
      expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
      expect(screen.queryByRole("button", { name: "Taken" })).toBeNull();
      expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    },
  );

  it("keeps generator and the other scheme when the reload after success fails", async () => {
    updateScheme.mockResolvedValue({ ...source, name: "Renamed" });
    let failRefresh = false;
    listSchemesForContext.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source, other];
    });
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    renderPanel();
    await waitFor(() => expect(screen.getAllByTestId("location-scheme-rename").length).toBe(2));
    fireEvent.click(screen.getAllByTestId("location-scheme-rename")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-rename")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("* Name"), { target: { value: "Renamed" } });
    failRefresh = true;
    fireEvent.click(screen.getByTestId("location-scheme-rename-submit"));
    await waitFor(() => expect(screen.queryByTestId("scheme-rename")).toBeNull());
    expect(screen.getByRole("button", { name: "Renamed" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Article" })).toBeNull();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(screen.getByText(new RegExp(GENERATOR))).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("still opens rename from the list row when the scheme read fails", async () => {
    getScheme.mockRejectedValue(new Error("read failed"));
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    listSchemesForContext.mockResolvedValue([
      {
        schemeId: "11",
        name: "Article",
        generator: GENERATOR,
        contextId: "3",
      },
    ]);
    updateScheme.mockResolvedValue({ schemeId: "11", name: "Renamed" });
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("location-scheme-rename")).toBeTruthy());
    fireEvent.click(screen.getByTestId("location-scheme-rename"));
    await waitFor(() => expect(screen.getByTestId("scheme-rename")).toBeTruthy());
    expect(screen.getByTestId("scheme-rename-generator")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-rename-description")).toHaveTextContent("");
    fireEvent.change(screen.getByLabelText("* Name"), { target: { value: "Renamed" } });
    fireEvent.click(screen.getByTestId("location-scheme-rename-submit"));
    await waitFor(() => expect(updateScheme).toHaveBeenCalledWith("11", { name: "Renamed" }));
  });
});
