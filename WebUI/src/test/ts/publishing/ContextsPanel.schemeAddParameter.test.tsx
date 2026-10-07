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
import {
  LOCATION_SCHEME_PARAMETER_EXISTS,
  LOCATION_SCHEME_PARAMETER_NAME_MAX_LENGTH,
  LOCATION_SCHEME_PARAMETER_NAME_REQUIRED,
  LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED,
} from "@/publishing/locationSchemeAddParameter";
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
const ADDED = {
  name: "suffix",
  type: "BackendColumn",
  value: "Contentstatus.contentid",
};

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

async function openAdd(): Promise<void> {
  listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
  listSchemesForContext.mockResolvedValue([source, other]);
  getScheme.mockResolvedValue(source);
  renderPanel();
  await waitFor(() =>
    expect(screen.getAllByTestId("location-scheme-add-parameter").length).toBe(2),
  );
  fireEvent.click(screen.getAllByTestId("location-scheme-add-parameter")[0]);
  await waitFor(() => expect(screen.getByTestId("scheme-add-parameter")).toBeTruthy());
}

function fillParameter(name: string, value: string, type = "BackendColumn"): void {
  fireEvent.change(screen.getByLabelText("* Name"), { target: { value: name } });
  fireEvent.change(screen.getByLabelText("* Type"), { target: { value: type } });
  fireEvent.change(screen.getByLabelText("* Value"), { target: { value } });
}

describe("ContextsPanel location scheme add parameter", () => {
  beforeEach(() => {
    updateScheme.mockReset();
    getScheme.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    createScheme.mockReset();
    listSchemesForContext.mockResolvedValue([source, other]);
    getScheme.mockResolvedValue(source);
  });

  it("lists the new parameter only after the save reload and keeps the other fields", async () => {
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
      expect(screen.getAllByTestId("location-scheme-add-parameter").length).toBe(2),
    );
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("path");
    expect(screen.getByTestId("scheme-list-parameters-11")).not.toHaveTextContent("suffix");
    expect(screen.getAllByTestId("location-scheme-rename").length).toBe(2);
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getAllByTestId("location-scheme-add-parameter")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-add-parameter")).toBeTruthy());
    expect(getScheme).toHaveBeenCalledWith("11");
    expect(screen.getByTestId("scheme-add-parameter-name")).toHaveTextContent("Article");
    expect(screen.getByTestId("scheme-add-parameter-generator")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-add-parameter-description")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-add-parameter-content-type")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-add-parameter-template")).toHaveTextContent("8");
    expect(screen.getByTestId("scheme-add-parameter-existing-row")).toHaveTextContent(
      "$sys.site.path",
    );
    expect(screen.getByTestId("scheme-add-parameter-existing")).not.toHaveTextContent("suffix");

    fillParameter(`  ${ADDED.name}  `, `  ${ADDED.value}  `);
    listed = true;
    fireEvent.click(screen.getByTestId("location-scheme-add-parameter-save"));
    await waitFor(() => expect(updateScheme).toHaveBeenCalledTimes(1));
    expect(updateScheme).toHaveBeenCalledWith("11", {
      addParameter: true,
      parameters: [ADDED],
    });
    expect(screen.getByTestId("scheme-add-parameter")).toBeTruthy();
    expect(screen.queryByTestId("scheme-list-parameters-11")).toBeNull();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(createScheme).not.toHaveBeenCalled();

    releaseUpdate({ ...source, parameters: [...source.parameters, { ...ADDED, sequence: 1 }] });
    await waitFor(() => expect(screen.queryByTestId("scheme-add-parameter")).toBeNull());
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("path");
    expect(screen.getByTestId("scheme-list-parameters-11")).not.toHaveTextContent("suffix");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([
      { ...source, parameters: [...source.parameters, { ...ADDED, sequence: 1 }] },
      other,
    ]);
    await waitFor(() =>
      expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("suffix"),
    );
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("BackendColumn");
    expect(screen.queryByTestId("scheme-list-parameters-12")).toBeNull();
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("Pages");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getAllByTestId("location-scheme-rename").length).toBe(2);
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("cancel does not write and keeps the previous parameter list", async () => {
    await openAdd();
    fillParameter("suffix", "article");
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("location-scheme-add-parameter-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-add-parameter")).toBeNull());
    expect(updateScheme).not.toHaveBeenCalled();
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("path");
    expect(screen.getByTestId("scheme-list-parameters-11")).not.toHaveTextContent("suffix");
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it.each([
    ["   ", "article", LOCATION_SCHEME_PARAMETER_NAME_REQUIRED],
    ["suffix", "   ", LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED],
    ["path", "other", LOCATION_SCHEME_PARAMETER_EXISTS],
    ["n".repeat(LOCATION_SCHEME_PARAMETER_NAME_MAX_LENGTH + 1), "article", "50 characters or fewer"],
  ])("does not write when name=%s value=%s", async (name, value, message) => {
    await openAdd();
    const loads = listSchemesForContext.mock.calls.length;
    fillParameter(name, value);
    fireEvent.click(screen.getByTestId("location-scheme-add-parameter-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(updateScheme).not.toHaveBeenCalled();
    expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
    expect(screen.getByTestId("scheme-add-parameter-existing")).toHaveTextContent("path");
    expect(screen.getByTestId("scheme-add-parameter-existing")).not.toHaveTextContent("suffix");
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("location-scheme-add-parameter-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-add-parameter")).toBeNull());
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
    expect(screen.getByTestId("scheme-list-parameters-11").textContent ?? "").not.toContain(
      "suffix (",
    );
  });

  it.each([
    [400, "Bad Request", "Parameter name is required"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Parameter name already exists on this scheme"],
  ])(
    "keeps the previous parameter list when HTTP %s rejects the add",
    async (status, statusText, message) => {
      updateScheme.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openAdd();
      const loads = listSchemesForContext.mock.calls.length;
      fillParameter("suffix", "article");
      fireEvent.click(screen.getByTestId("location-scheme-add-parameter-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("scheme-add-parameter")).toBeTruthy();
      expect(screen.getByTestId("scheme-add-parameter-name")).toHaveTextContent("Article");
      expect(screen.getByTestId("scheme-add-parameter-generator")).toHaveTextContent(GENERATOR);
      expect(screen.getByTestId("scheme-add-parameter-existing-row")).toHaveTextContent(
        "$sys.site.path",
      );
      expect(screen.getAllByTestId("scheme-add-parameter-existing-row")).toHaveLength(1);
      expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("location-scheme-add-parameter-cancel"));
      await waitFor(() => expect(screen.queryByTestId("scheme-add-parameter")).toBeNull());
      expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
      expect(screen.getByTestId("scheme-list-parameters-11")).not.toHaveTextContent("suffix");
      expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
      expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    },
  );

  it("still lists the new parameter when the reload after success fails", async () => {
    updateScheme.mockResolvedValue({
      ...source,
      parameters: [...source.parameters, { ...ADDED, sequence: 1 }],
    });
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
      expect(screen.getAllByTestId("location-scheme-add-parameter").length).toBe(2),
    );
    fireEvent.click(screen.getAllByTestId("location-scheme-add-parameter")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-add-parameter")).toBeTruthy());
    fillParameter(ADDED.name, ADDED.value);
    failRefresh = true;
    fireEvent.click(screen.getByTestId("location-scheme-add-parameter-save"));
    await waitFor(() => expect(screen.queryByTestId("scheme-add-parameter")).toBeNull());
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("suffix");
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-list-description-12")).toHaveTextContent("Short");
    expect(createScheme).not.toHaveBeenCalled();
  });
});
