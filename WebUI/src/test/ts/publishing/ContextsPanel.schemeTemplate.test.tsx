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
const NEXT = 12;

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

async function openTemplate(): Promise<void> {
  listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
  listSchemesForContext.mockResolvedValue([source, other]);
  getScheme.mockResolvedValue(source);
  renderPanel();
  await waitFor(() =>
    expect(screen.getAllByTestId("location-scheme-template").length).toBe(2),
  );
  fireEvent.click(screen.getAllByTestId("location-scheme-template")[0]);
  await waitFor(() => expect(screen.getByTestId("scheme-template")).toBeTruthy());
}

describe("ContextsPanel location scheme template", () => {
  beforeEach(() => {
    updateScheme.mockReset();
    getScheme.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    createScheme.mockReset();
    listSchemesForContext.mockResolvedValue([source, other]);
    getScheme.mockResolvedValue(source);
  });

  it("shows the new template only after the save reload and keeps the other fields", async () => {
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
      expect(screen.getAllByTestId("location-scheme-template").length).toBe(2),
    );
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getAllByTestId("location-scheme-template")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-template")).toBeTruthy());
    expect(getScheme).toHaveBeenCalledWith("11");
    expect(screen.getByLabelText("* Template id")).toHaveValue("8");
    expect(screen.getByTestId("scheme-template-name")).toHaveTextContent("Article");
    expect(screen.getByTestId("scheme-template-generator")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-template-description")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-template-content-type")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-template-parameter")).toHaveTextContent("$sys.site.path");

    fireEvent.change(screen.getByLabelText("* Template id"), {
      target: { value: `  ${NEXT}  ` },
    });
    listed = true;
    fireEvent.click(screen.getByTestId("location-scheme-template-save"));
    await waitFor(() => expect(updateScheme).toHaveBeenCalledTimes(1));
    expect(updateScheme).toHaveBeenCalledWith("11", { templateId: NEXT });
    expect(screen.getByTestId("scheme-template")).toBeTruthy();
    expect(screen.queryByTestId("scheme-list-template-11")).toBeNull();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(createScheme).not.toHaveBeenCalled();

    releaseUpdate({ ...source, templateId: NEXT });
    await waitFor(() => expect(screen.queryByTestId("scheme-template")).toBeNull());
    expect(screen.getByTestId("scheme-list-template-11")).toHaveTextContent("8");
    expect(screen.getByTestId("scheme-list-template-12")).toHaveTextContent("9");
    expect(screen.getByTestId("scheme-list-content-type-11")).toHaveTextContent("4");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([{ ...source, templateId: NEXT }, other]);
    await waitFor(() =>
      expect(screen.getByTestId("scheme-list-template-11")).toHaveTextContent(String(NEXT)),
    );
    expect(screen.getByTestId("scheme-list-template-11")).not.toHaveTextContent(/^8$/);
    expect(screen.getByTestId("scheme-list-template-12")).toHaveTextContent("9");
    expect(screen.getByTestId("scheme-list-content-type-11")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("Pages");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("rejects a blank or non-numeric template without updating", async () => {
    await openTemplate();
    const loads = listSchemesForContext.mock.calls.length;
    const reads = getScheme.mock.calls.length;
    fireEvent.change(screen.getByLabelText("* Template id"), { target: { value: " " } });
    fireEvent.click(screen.getByTestId("location-scheme-template-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Location scheme template is required",
    );
    fireEvent.change(screen.getByLabelText("* Template id"), { target: { value: "abc" } });
    fireEvent.click(screen.getByTestId("location-scheme-template-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Location scheme template must be a number",
    );
    fireEvent.change(screen.getByLabelText("* Template id"), { target: { value: "0" } });
    fireEvent.click(screen.getByTestId("location-scheme-template-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Location scheme template must be a number",
    );
    expect(updateScheme).not.toHaveBeenCalled();
    expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
    expect(getScheme).toHaveBeenCalledTimes(reads);
    expect(screen.getByTestId("scheme-template-name")).toHaveTextContent("Article");
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("location-scheme-template-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-template")).toBeNull());
    expect(screen.getByTestId("scheme-list-template-11")).toHaveTextContent("8");
    expect(screen.getByTestId("scheme-list-content-type-11")).toHaveTextContent("4");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
  });

  it("cancel keeps the old template and does not update", async () => {
    await openTemplate();
    fireEvent.change(screen.getByLabelText("* Template id"), { target: { value: "99" } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("location-scheme-template-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-template")).toBeNull());
    expect(updateScheme).not.toHaveBeenCalled();
    expect(screen.getByTestId("scheme-list-template-11")).toHaveTextContent("8");
    expect(screen.queryByText("99")).toBeNull();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it.each([
    [400, "Bad Request", "Location scheme template must be a number"],
    [403, "Forbidden", "Admin or Designer role required"],
    [
      409,
      "Conflict",
      "A location scheme already exists for this context, template, and content type",
    ],
  ])(
    "keeps the previous template when HTTP %s rejects the save",
    async (status, statusText, message) => {
      updateScheme.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openTemplate();
      const loads = listSchemesForContext.mock.calls.length;
      fireEvent.change(screen.getByLabelText("* Template id"), { target: { value: "99" } });
      fireEvent.click(screen.getByTestId("location-scheme-template-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("scheme-template")).toBeTruthy();
      expect(screen.getByTestId("scheme-template-name")).toHaveTextContent("Article");
      expect(screen.getByTestId("scheme-template-generator")).toHaveTextContent(GENERATOR);
      expect(screen.getByTestId("scheme-template-parameter")).toHaveTextContent(
        "$sys.site.path",
      );
      expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("location-scheme-template-cancel"));
      await waitFor(() => expect(screen.queryByTestId("scheme-template")).toBeNull());
      expect(screen.getByTestId("scheme-list-template-11")).toHaveTextContent("8");
      expect(screen.getByTestId("scheme-list-content-type-11")).toHaveTextContent("4");
      expect(screen.queryByText("99")).toBeNull();
      expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    },
  );

  it("keeps the name and the other scheme when the reload after success fails", async () => {
    updateScheme.mockResolvedValue({ ...source, templateId: NEXT });
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
      expect(screen.getAllByTestId("location-scheme-template").length).toBe(2),
    );
    fireEvent.click(screen.getAllByTestId("location-scheme-template")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-template")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("* Template id"), { target: { value: String(NEXT) } });
    failRefresh = true;
    fireEvent.click(screen.getByTestId("location-scheme-template-save"));
    await waitFor(() => expect(screen.queryByTestId("scheme-template")).toBeNull());
    expect(screen.getByTestId("scheme-list-template-11")).toHaveTextContent(String(NEXT));
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(screen.getByTestId("scheme-list-template-12")).toHaveTextContent("9");
    expect(screen.getByTestId("scheme-list-content-type-11")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("still opens the template form from the list row when the scheme read fails", async () => {
    getScheme.mockRejectedValue(new Error("read failed"));
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    listSchemesForContext.mockResolvedValue([
      {
        schemeId: "11",
        name: "Article",
        generator: GENERATOR,
        contentTypeId: 4,
        templateId: 8,
        contextId: "3",
      },
    ]);
    updateScheme.mockResolvedValue({ schemeId: "11", templateId: NEXT });
    renderPanel();
    await waitFor(() => expect(screen.getByTestId("location-scheme-template")).toBeTruthy());
    fireEvent.click(screen.getByTestId("location-scheme-template"));
    await waitFor(() => expect(screen.getByTestId("scheme-template")).toBeTruthy());
    expect(screen.getByTestId("scheme-template-name")).toHaveTextContent("Article");
    expect(screen.getByTestId("scheme-template-generator")).toHaveTextContent(GENERATOR);
    expect(screen.getByLabelText("* Template id")).toHaveValue("8");
    fireEvent.change(screen.getByLabelText("* Template id"), { target: { value: String(NEXT) } });
    fireEvent.click(screen.getByTestId("location-scheme-template-save"));
    await waitFor(() => expect(updateScheme).toHaveBeenCalledWith("11", { templateId: NEXT }));
  });
});
