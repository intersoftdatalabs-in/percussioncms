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
import {
  LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME,
  LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED,
} from "@/publishing/locationSchemeParameterValue";
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

function valueButton(parameterName: string): HTMLElement {
  const row = screen.getAllByTestId("scheme-list-parameter").find((element) =>
    (element.textContent ?? "").includes(`${parameterName} (`),
  );
  if (!row) {
    throw new Error(`missing parameter ${parameterName}`);
  }
  return within(row).getByTestId("location-scheme-parameter-value");
}

async function openValue(parameterName = "suffix"): Promise<void> {
  listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
  listSchemesForContext.mockResolvedValue([source, other]);
  renderPanel();
  await waitFor(() => expect(valueButton(parameterName)).toBeTruthy());
  fireEvent.click(valueButton(parameterName));
  await waitFor(() => expect(screen.getByTestId("scheme-parameter-value")).toBeTruthy());
}

describe("ContextsPanel location scheme parameter value", () => {
  beforeEach(() => {
    updateScheme.mockReset();
    deleteScheme.mockReset();
    getScheme.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    createScheme.mockReset();
    listSchemesForContext.mockResolvedValue([source, other]);
    getScheme.mockResolvedValue(source);
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("shows the new value only after the save reload and keeps type, sequence, and the other parameter", async () => {
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
    await waitFor(() => expect(valueButton("suffix")).toBeTruthy());
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("article");
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(valueButton("suffix"));
    await waitFor(() => expect(screen.getByTestId("scheme-parameter-value")).toBeTruthy());
    expect(getScheme).toHaveBeenCalledWith("11");
    expect(screen.getByTestId("scheme-parameter-value-name")).toHaveTextContent("Article");
    expect(screen.getByTestId("scheme-parameter-value-generator")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-parameter-value-description")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-parameter-value-content-type")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-parameter-value-template")).toHaveTextContent("8");
    expect(screen.getByTestId("scheme-parameter-value-parameter-name")).toHaveTextContent("suffix");
    expect(screen.getByTestId("scheme-parameter-value-type")).toHaveTextContent("BackendColumn");
    expect(screen.getByTestId("scheme-parameter-value-sequence")).toHaveTextContent("1");
    expect(screen.getByTestId("scheme-parameter-value-current")).toHaveTextContent("article");
    expect(screen.getByTestId("scheme-parameter-value-other")).toHaveTextContent("$sys.site.path");
    expect((screen.getByLabelText("* Value") as HTMLInputElement).value).toBe("article");

    fireEvent.change(screen.getByLabelText("* Value"), { target: { value: "  page.html  " } });
    listed = true;
    fireEvent.click(screen.getByTestId("location-scheme-parameter-value-save"));
    await waitFor(() => expect(updateScheme).toHaveBeenCalledTimes(1));
    expect(updateScheme).toHaveBeenCalledWith("11", {
      updateParameterValue: true,
      parameters: [{ name: "suffix", type: "BackendColumn", value: "page.html" }],
    });
    expect(screen.getByTestId("scheme-parameter-value")).toBeTruthy();
    expect(screen.getByTestId("scheme-parameter-value-current")).toHaveTextContent("article");
    expect(screen.queryByTestId("scheme-list-parameters-11")).toBeNull();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(deleteScheme).not.toHaveBeenCalled();

    releaseUpdate({
      ...source,
      parameters: [PATH, { ...SUFFIX, value: "article" }],
    });
    await waitFor(() => expect(screen.queryByTestId("scheme-parameter-value")).toBeNull());
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("article");
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([source, other]);
    await waitFor(() =>
      expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("page.html"),
    );
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("BackendColumn");
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
    expect(screen.getByTestId("scheme-list-parameters-11")).not.toHaveTextContent("article");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-list-content-type-11")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-list-template-11")).toHaveTextContent("8");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(deleteScheme).not.toHaveBeenCalled();
  });

  it("cancel does not write and keeps the previous value", async () => {
    await openValue();
    fireEvent.change(screen.getByLabelText("* Value"), { target: { value: "page.html" } });
    fireEvent.click(screen.getByTestId("location-scheme-parameter-value-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-parameter-value")).toBeNull());
    expect(updateScheme).not.toHaveBeenCalled();
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("article");
    expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
  });

  it("does not write a blank value", async () => {
    await openValue();
    const loads = listSchemesForContext.mock.calls.length;
    fireEvent.change(screen.getByLabelText("* Value"), { target: { value: "   " } });
    fireEvent.click(screen.getByTestId("location-scheme-parameter-value-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED,
    );
    expect(updateScheme).not.toHaveBeenCalled();
    expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
    expect(screen.getByTestId("scheme-parameter-value-current")).toHaveTextContent("article");
    expect(screen.getByTestId("scheme-parameter-value-type")).toHaveTextContent("BackendColumn");
    expect(screen.getByTestId("scheme-parameter-value-sequence")).toHaveTextContent("1");
  });

  it("does not write when the loaded scheme no longer lists that parameter", async () => {
    getScheme.mockResolvedValue({ ...source, parameters: [PATH] });
    await openValue("suffix");
    fireEvent.change(screen.getByLabelText("* Value"), { target: { value: "page.html" } });
    fireEvent.click(screen.getByTestId("location-scheme-parameter-value-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME,
    );
    expect(updateScheme).not.toHaveBeenCalled();
    expect(screen.getByTestId("scheme-parameter-value-other")).toHaveTextContent("path");
  });

  it.each([
    [400, "Change one location scheme parameter value at a time"],
    [403, "Admin or Designer role required"],
    [409, "Parameter is not on this scheme"],
  ])(
    "keeps the previous value when HTTP %s rejects the change",
    async (status, message) => {
      updateScheme.mockRejectedValue({
        status,
        statusText: "error",
        body: { message },
      });
      await openValue();
      const loads = listSchemesForContext.mock.calls.length;
      fireEvent.change(screen.getByLabelText("* Value"), { target: { value: "page.html" } });
      fireEvent.click(screen.getByTestId("location-scheme-parameter-value-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("scheme-parameter-value")).toBeTruthy();
      expect(screen.getByTestId("scheme-parameter-value-name")).toHaveTextContent("Article");
      expect(screen.getByTestId("scheme-parameter-value-current")).toHaveTextContent("article");
      expect(screen.getByTestId("scheme-parameter-value-type")).toHaveTextContent("BackendColumn");
      expect(screen.getByTestId("scheme-parameter-value-other")).toHaveTextContent("$sys.site.path");
      expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
      fireEvent.click(screen.getByTestId("location-scheme-parameter-value-cancel"));
      await waitFor(() => expect(screen.queryByTestId("scheme-parameter-value")).toBeNull());
      expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("article");
      expect(screen.getByTestId("scheme-list-parameters-11")).toHaveTextContent("$sys.site.path");
      expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
      expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
      expect(deleteScheme).not.toHaveBeenCalled();
    },
  );
});
