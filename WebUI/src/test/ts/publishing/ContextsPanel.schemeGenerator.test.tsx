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
import { LOCATION_SCHEME_GENERATOR_MAX_LENGTH } from "@/publishing/locationSchemeGenerator";
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
const NEXT = "sys_Changed";

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

async function openGenerator(): Promise<void> {
  listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
  listSchemesForContext.mockResolvedValue([source, other]);
  getScheme.mockResolvedValue(source);
  renderPanel();
  await waitFor(() =>
    expect(screen.getAllByTestId("location-scheme-generator").length).toBe(2),
  );
  fireEvent.click(screen.getAllByTestId("location-scheme-generator")[0]);
  await waitFor(() => expect(screen.getByTestId("scheme-generator")).toBeTruthy());
}

describe("ContextsPanel location scheme generator", () => {
  beforeEach(() => {
    updateScheme.mockReset();
    getScheme.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    createScheme.mockReset();
    listSchemesForContext.mockResolvedValue([source, other]);
    getScheme.mockResolvedValue(source);
  });

  it("shows the new generator only after the save reload and keeps the other fields", async () => {
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
    await waitFor(() =>
      expect(screen.getAllByTestId("location-scheme-generator").length).toBe(2),
    );
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getAllByTestId("location-scheme-generator")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-generator")).toBeTruthy());
    expect(getScheme).toHaveBeenCalledWith("11");
    expect(screen.getByLabelText("* Generator")).toHaveValue(GENERATOR);
    expect(screen.getByTestId("scheme-generator-name")).toHaveTextContent("Article");
    expect(screen.getByTestId("scheme-generator-description")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-generator-content-type")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-generator-template")).toHaveTextContent("8");
    expect(screen.getByTestId("scheme-generator-parameter")).toHaveTextContent(
      "$sys.site.path",
    );

    fireEvent.change(screen.getByLabelText("* Generator"), {
      target: { value: `  ${NEXT}  ` },
    });
    listed = true;
    fireEvent.click(screen.getByTestId("location-scheme-generator-save"));
    await waitFor(() => expect(updateScheme).toHaveBeenCalledTimes(1));
    expect(updateScheme).toHaveBeenCalledWith("11", { generator: NEXT });
    expect(screen.getByTestId("scheme-generator")).toBeTruthy();
    expect(screen.queryByTestId("scheme-list-generator-11")).toBeNull();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(createScheme).not.toHaveBeenCalled();

    releaseUpdate({ ...source, generator: NEXT });
    await waitFor(() => expect(screen.queryByTestId("scheme-generator")).toBeNull());
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-list-generator-12")).toHaveTextContent("legacy-gen");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([
      { ...source, generator: NEXT },
      other,
    ]);
    await waitFor(() =>
      expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(NEXT),
    );
    expect(screen.getByTestId("scheme-list-generator-11")).not.toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-list-generator-12")).toHaveTextContent("legacy-gen");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("rejects a blank or overlong generator without updating", async () => {
    await openGenerator();
    const loads = listSchemesForContext.mock.calls.length;
    const reads = getScheme.mock.calls.length;
    fireEvent.change(screen.getByLabelText("* Generator"), { target: { value: " " } });
    fireEvent.click(screen.getByTestId("location-scheme-generator-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Location scheme generator is required",
    );
    fireEvent.change(screen.getByLabelText("* Generator"), {
      target: { value: "g".repeat(LOCATION_SCHEME_GENERATOR_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("location-scheme-generator-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Location scheme generator must be 255 characters or fewer",
    );
    expect(updateScheme).not.toHaveBeenCalled();
    expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
    expect(getScheme).toHaveBeenCalledTimes(reads);
    expect(screen.getByTestId("scheme-generator-name")).toHaveTextContent("Article");
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("location-scheme-generator-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-generator")).toBeNull());
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
  });

  it("cancel keeps the old generator and does not update", async () => {
    await openGenerator();
    fireEvent.change(screen.getByLabelText("* Generator"), { target: { value: "Nope" } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("location-scheme-generator-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-generator")).toBeNull());
    expect(updateScheme).not.toHaveBeenCalled();
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.queryByText("Nope")).toBeNull();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it.each([
    [400, "Bad Request", "Location scheme generator must be 255 characters or fewer"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Location scheme name already exists"],
  ])(
    "keeps the previous generator when HTTP %s rejects the save",
    async (status, statusText, message) => {
      updateScheme.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openGenerator();
      const loads = listSchemesForContext.mock.calls.length;
      fireEvent.change(screen.getByLabelText("* Generator"), { target: { value: "Taken" } });
      fireEvent.click(screen.getByTestId("location-scheme-generator-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("scheme-generator")).toBeTruthy();
      expect(screen.getByTestId("scheme-generator-name")).toHaveTextContent("Article");
      expect(screen.getByTestId("scheme-generator-description")).toHaveTextContent("Pages");
      expect(screen.getByTestId("scheme-generator-parameter")).toHaveTextContent(
        "$sys.site.path",
      );
      expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("location-scheme-generator-cancel"));
      await waitFor(() => expect(screen.queryByTestId("scheme-generator")).toBeNull());
      expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
      expect(screen.queryByText("Taken")).toBeNull();
      expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    },
  );

  it("keeps the name and the other scheme when the reload after success fails", async () => {
    updateScheme.mockResolvedValue({ ...source, generator: NEXT });
    let failRefresh = false;
    listSchemesForContext.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source, other];
    });
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    renderPanel();
    await waitFor(() =>
      expect(screen.getAllByTestId("location-scheme-generator").length).toBe(2),
    );
    fireEvent.click(screen.getAllByTestId("location-scheme-generator")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-generator")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("* Generator"), { target: { value: NEXT } });
    failRefresh = true;
    fireEvent.click(screen.getByTestId("location-scheme-generator-save"));
    await waitFor(() => expect(screen.queryByTestId("scheme-generator")).toBeNull());
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(NEXT);
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(screen.getByTestId("scheme-list-generator-12")).toHaveTextContent("legacy-gen");
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("still opens the generator form from the list row when the scheme read fails", async () => {
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
    updateScheme.mockResolvedValue({ schemeId: "11", generator: NEXT });
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("location-scheme-generator")).toBeTruthy());
    fireEvent.click(screen.getByTestId("location-scheme-generator"));
    await waitFor(() => expect(screen.getByTestId("scheme-generator")).toBeTruthy());
    expect(screen.getByTestId("scheme-generator-name")).toHaveTextContent("Article");
    expect(screen.getByTestId("scheme-generator-description")).toHaveTextContent("");
    expect(screen.getByLabelText("* Generator")).toHaveValue(GENERATOR);
    fireEvent.change(screen.getByLabelText("* Generator"), { target: { value: NEXT } });
    fireEvent.click(screen.getByTestId("location-scheme-generator-save"));
    await waitFor(() =>
      expect(updateScheme).toHaveBeenCalledWith("11", { generator: NEXT }),
    );
  });
});
