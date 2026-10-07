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

import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ContextsPanel } from "@/publishing/design/ContextsPanel";
import { LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME } from "@/publishing/locationSchemeRemoveParameter";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

const updateScheme = vi.fn();
const deleteScheme = vi.fn();
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
  deleteScheme: (...args: unknown[]) => deleteScheme(...args),
  getScheme: (...args: unknown[]) => getScheme(...args),
}));

const GENERATOR =
  "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation";
const PATH = { name: "path", type: "String", value: "$sys.site.path", sequence: 0 };
const SUFFIX = { name: "suffix", type: "BackendColumn", value: "article", sequence: 1 };

const source = {
  schemeId: "11",
  name: "Article",
  generator: GENERATOR,
  description: "Pages",
  contentTypeId: 4,
  templateId: 8,
  contextId: "3",
  parameters: [PATH, SUFFIX],
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

function removeButton(parameterName: string): HTMLElement {
  const row = screen.getAllByTestId("scheme-list-parameter").find((element) =>
    (element.textContent ?? "").includes(`${parameterName} (`),
  );
  if (!row) {
    throw new Error(`missing parameter ${parameterName}`);
  }
  return within(row).getByTestId("location-scheme-remove-parameter");
}

async function openRemove(parameterName = "suffix"): Promise<void> {
  listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
  listSchemesForContext.mockResolvedValue([source, other]);
  renderPanel();
  await waitFor(() => expect(removeButton(parameterName)).toBeTruthy());
  fireEvent.click(removeButton(parameterName));
  await waitFor(() => expect(screen.getByTestId("scheme-remove-parameter")).toBeTruthy());
}

describe("ContextsPanel location scheme remove parameter", () => {
  beforeEach(() => {
    updateScheme.mockReset();
    deleteScheme.mockReset();
    getScheme.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    createScheme.mockReset();
    listSchemesForContext.mockResolvedValue([source, other]);
    getScheme.mockResolvedValue(source);
  });

  it("drops one parameter only after the save reload and keeps the other fields", async () => {
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
    await waitFor(() => expect(removeButton("suffix")).toBeTruthy());
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("path");
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("suffix");
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(removeButton("suffix"));
    await waitFor(() => expect(screen.getByTestId("scheme-remove-parameter")).toBeTruthy());
    expect(getScheme).toHaveBeenCalledWith("11");
    expect(screen.getByTestId("scheme-remove-parameter-name")).toHaveTextContent("Article");
    expect(screen.getByTestId("scheme-remove-parameter-generator")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-remove-parameter-description")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-remove-parameter-content-type")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-remove-parameter-template")).toHaveTextContent("8");
    expect(screen.getByTestId("scheme-remove-parameter-target")).toHaveTextContent(
      "suffix (BackendColumn): article",
    );
    expect(screen.getByTestId("scheme-remove-parameter-other")).toHaveTextContent(
      "$sys.site.path",
    );
    expect(screen.getByTestId("scheme-remove-parameter-others")).not.toHaveTextContent("suffix");

    listed = true;
    fireEvent.click(screen.getByTestId("location-scheme-remove-parameter-confirm"));
    await waitFor(() => expect(updateScheme).toHaveBeenCalledTimes(1));
    expect(updateScheme).toHaveBeenCalledWith("11", {
      removeParameter: true,
      parameters: [{ name: "suffix", type: "BackendColumn", value: "article" }],
    });
    expect(screen.getByTestId("scheme-remove-parameter")).toBeTruthy();
    expect(screen.queryByTestId("scheme-list-parameters-11")).toBeNull();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(deleteScheme).not.toHaveBeenCalled();
    expect(createScheme).not.toHaveBeenCalled();

    releaseUpdate({ ...source, parameters: [PATH] });
    await waitFor(() => expect(screen.queryByTestId("scheme-remove-parameter")).toBeNull());
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("suffix");
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("path");
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([{ ...source, parameters: [PATH] }, other]);
    await waitFor(() =>
      expect(screen.getByTestId("scheme-list-parameters-11").textContent ?? "").not.toContain(
        "suffix (",
      ),
    );
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-list-content-type-11")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-list-template-11")).toHaveTextContent("8");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(deleteScheme).not.toHaveBeenCalled();
  });

  it("cancel does not write and keeps both parameters", async () => {
    await openRemove();
    fireEvent.click(screen.getByTestId("location-scheme-remove-parameter-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-remove-parameter")).toBeNull());
    expect(updateScheme).not.toHaveBeenCalled();
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("path");
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("suffix");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(deleteScheme).not.toHaveBeenCalled();
  });

  it("does not write when the loaded scheme no longer lists that parameter", async () => {
    getScheme.mockResolvedValue({ ...source, parameters: [PATH] });
    await openRemove("suffix");
    const loads = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getByTestId("location-scheme-remove-parameter-confirm"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME,
    );
    expect(updateScheme).not.toHaveBeenCalled();
    expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
    expect(screen.getByTestId("scheme-remove-parameter-other")).toHaveTextContent("path");
    fireEvent.click(screen.getByTestId("location-scheme-remove-parameter-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-remove-parameter")).toBeNull());
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("suffix");
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
  });

  it.each([
    [400, "Bad Request", "Remove one location scheme parameter at a time"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Parameter is not on this scheme"],
  ])(
    "keeps the previous parameter list when HTTP %s rejects the remove",
    async (status, statusText, message) => {
      updateScheme.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openRemove();
      const loads = listSchemesForContext.mock.calls.length;
      fireEvent.click(screen.getByTestId("location-scheme-remove-parameter-confirm"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("scheme-remove-parameter")).toBeTruthy();
      expect(screen.getByTestId("scheme-remove-parameter-name")).toHaveTextContent("Article");
      expect(screen.getByTestId("scheme-remove-parameter-generator")).toHaveTextContent(GENERATOR);
      expect(screen.getByTestId("scheme-remove-parameter-target")).toHaveTextContent("suffix");
      expect(screen.getByTestId("scheme-remove-parameter-other")).toHaveTextContent("path");
      expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
      fireEvent.click(screen.getByTestId("location-scheme-remove-parameter-cancel"));
      await waitFor(() => expect(screen.queryByTestId("scheme-remove-parameter")).toBeNull());
      expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("suffix");
      expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
      expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
      expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
      expect(deleteScheme).not.toHaveBeenCalled();
    },
  );

  it("leaves an empty parameter list and keeps the scheme when the last parameter is removed", async () => {
    const onlyPath = { ...source, parameters: [PATH] };
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    listSchemesForContext.mockResolvedValue([onlyPath, other]);
    getScheme.mockResolvedValue(onlyPath);
    updateScheme.mockResolvedValue({ ...onlyPath, parameters: [] });
    renderPanel();
    await waitFor(() => expect(removeButton("path")).toBeTruthy());
    fireEvent.click(removeButton("path"));
    await waitFor(() => expect(screen.getByTestId("scheme-remove-parameter")).toBeTruthy());
    expect(screen.queryByTestId("scheme-remove-parameter-other")).toBeNull();
    listSchemesForContext.mockResolvedValueOnce([{ ...onlyPath, parameters: [] }, other]);
    fireEvent.click(screen.getByTestId("location-scheme-remove-parameter-confirm"));
    await waitFor(() => expect(screen.queryByTestId("scheme-remove-parameter")).toBeNull());
    expect(updateScheme).toHaveBeenCalledWith("11", {
      removeParameter: true,
      parameters: [{ name: "path", type: "String", value: "$sys.site.path" }],
    });
    expect(screen.queryByTestId("scheme-list-parameters-11")).toBeNull();
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(deleteScheme).not.toHaveBeenCalled();
  });

  it("still drops the parameter when the reload after success fails", async () => {
    updateScheme.mockResolvedValue({ ...source, parameters: [PATH] });
    let failRefresh = false;
    listSchemesForContext.mockImplementation(async () => {
      if (failRefresh) {
        throw new Error("list failed");
      }
      return [source, other];
    });
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    renderPanel();
    await waitFor(() => expect(removeButton("suffix")).toBeTruthy());
    fireEvent.click(removeButton("suffix"));
    await waitFor(() => expect(screen.getByTestId("scheme-remove-parameter")).toBeTruthy());
    failRefresh = true;
    fireEvent.click(screen.getByTestId("location-scheme-remove-parameter-confirm"));
    await waitFor(() => expect(screen.queryByTestId("scheme-remove-parameter")).toBeNull());
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
    expect(screen.getByTestId("scheme-list-parameters-11").textContent ?? "").not.toContain(
      "suffix (",
    );
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-list-description-12")).toHaveTextContent("Short");
    expect(deleteScheme).not.toHaveBeenCalled();
  });
});
