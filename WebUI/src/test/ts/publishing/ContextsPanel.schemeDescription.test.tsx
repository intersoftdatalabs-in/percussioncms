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
import { LOCATION_SCHEME_DESCRIPTION_MAX_LENGTH } from "@/publishing/locationSchemeDescription";
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
const NEXT = "Night notes";

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

async function openDescription(): Promise<void> {
  listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
  listSchemesForContext.mockResolvedValue([source, other]);
  getScheme.mockResolvedValue(source);
  renderPanel();
  await waitFor(() =>
    expect(screen.getAllByTestId("location-scheme-description").length).toBe(2),
  );
  fireEvent.click(screen.getAllByTestId("location-scheme-description")[0]);
  await waitFor(() => expect(screen.getByTestId("scheme-description")).toBeTruthy());
}

describe("ContextsPanel location scheme description", () => {
  beforeEach(() => {
    updateScheme.mockReset();
    getScheme.mockReset();
    listContexts.mockReset();
    listSchemesForContext.mockReset();
    createScheme.mockReset();
    listSchemesForContext.mockResolvedValue([source, other]);
    getScheme.mockResolvedValue(source);
  });

  it("shows the new description only after the save reload and keeps the other fields", async () => {
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
      expect(screen.getAllByTestId("location-scheme-description").length).toBe(2),
    );
    expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    const schemeLoads = listSchemesForContext.mock.calls.length;
    fireEvent.click(screen.getAllByTestId("location-scheme-description")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-description")).toBeTruthy());
    expect(getScheme).toHaveBeenCalledWith("11");
    expect(screen.getByLabelText("Description")).toHaveValue("Pages");
    expect(screen.getByTestId("scheme-description-name")).toHaveTextContent("Article");
    expect(screen.getByTestId("scheme-description-generator")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-description-content-type")).toHaveTextContent("4");
    expect(screen.getByTestId("scheme-description-template")).toHaveTextContent("8");
    expect(screen.getByTestId("scheme-description-parameter")).toHaveTextContent(
      "$sys.site.path",
    );

    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: `  ${NEXT}  ` },
    });
    listed = true;
    fireEvent.click(screen.getByTestId("location-scheme-description-save"));
    await waitFor(() => expect(updateScheme).toHaveBeenCalledTimes(1));
    expect(updateScheme).toHaveBeenCalledWith("11", { description: NEXT });
    expect(screen.getByTestId("scheme-description")).toBeTruthy();
    expect(screen.queryByTestId("scheme-list-description-11")).toBeNull();
    expect(listSchemesForContext).toHaveBeenCalledTimes(schemeLoads);
    expect(createScheme).not.toHaveBeenCalled();

    releaseUpdate({ ...source, description: NEXT });
    await waitFor(() => expect(screen.queryByTestId("scheme-description")).toBeNull());
    expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-list-description-12")).toHaveTextContent("Short");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(releaseList).toEqual(expect.any(Function));
    releaseList?.([{ ...source, description: NEXT }, other]);
    await waitFor(() =>
      expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent(NEXT),
    );
    expect(screen.getByTestId("scheme-list-description-11")).not.toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-list-description-12")).toHaveTextContent("Short");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByTestId("scheme-list-generator-12")).toHaveTextContent("legacy-gen");
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("clears a blank description only after save and keeps the other fields", async () => {
    updateScheme.mockResolvedValue({ ...source, description: "" });
    listSchemesForContext.mockImplementation(async () => [source, other]);
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    renderPanel();
    await waitFor(() =>
      expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("Pages"),
    );
    fireEvent.click(screen.getAllByTestId("location-scheme-description")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-description")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: "   " } });
    listSchemesForContext.mockResolvedValue([
      { ...source, description: "" },
      other,
    ]);
    fireEvent.click(screen.getByTestId("location-scheme-description-save"));
    await waitFor(() => expect(screen.queryByTestId("scheme-description")).toBeNull());
    expect(updateScheme).toHaveBeenCalledWith("11", { description: "" });
    expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("");
    expect(screen.getByTestId("scheme-list-description-12")).toHaveTextContent("Short");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("rejects an overlong description without updating", async () => {
    await openDescription();
    const loads = listSchemesForContext.mock.calls.length;
    const reads = getScheme.mock.calls.length;
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "d".repeat(LOCATION_SCHEME_DESCRIPTION_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("location-scheme-description-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Location scheme description must be 255 characters or fewer",
    );
    expect(updateScheme).not.toHaveBeenCalled();
    expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
    expect(getScheme).toHaveBeenCalledTimes(reads);
    expect(screen.getByTestId("scheme-description-name")).toHaveTextContent("Article");
    expect(screen.getByTestId("scheme-description-generator")).toHaveTextContent(GENERATOR);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("location-scheme-description-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-description")).toBeNull());
    expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("Pages");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
  });

  it("cancel keeps the old description and does not update", async () => {
    await openDescription();
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: "Nope" } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("location-scheme-description-cancel"));
    await waitFor(() => expect(screen.queryByTestId("scheme-description")).toBeNull());
    expect(updateScheme).not.toHaveBeenCalled();
    expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("Pages");
    expect(screen.queryByText("Nope")).toBeNull();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(createScheme).not.toHaveBeenCalled();
  });

  it.each([
    [400, "Bad Request", "Location scheme description must be 255 characters or fewer"],
    [403, "Forbidden", "Admin or Designer role required"],
    [409, "Conflict", "Location scheme name already exists"],
  ])(
    "keeps the previous description when HTTP %s rejects the save",
    async (status, statusText, message) => {
      updateScheme.mockRejectedValue({
        status,
        statusText,
        body: { message },
      });
      await openDescription();
      const loads = listSchemesForContext.mock.calls.length;
      fireEvent.change(screen.getByLabelText("Description"), { target: { value: "Taken" } });
      fireEvent.click(screen.getByTestId("location-scheme-description-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.getByTestId("scheme-description")).toBeTruthy();
      expect(screen.getByTestId("scheme-description-name")).toHaveTextContent("Article");
      expect(screen.getByTestId("scheme-description-generator")).toHaveTextContent(GENERATOR);
      expect(screen.getByTestId("scheme-description-parameter")).toHaveTextContent(
        "$sys.site.path",
      );
      expect(listSchemesForContext).toHaveBeenCalledTimes(loads);
      vi.spyOn(window, "confirm").mockReturnValue(true);
      fireEvent.click(screen.getByTestId("location-scheme-description-cancel"));
      await waitFor(() => expect(screen.queryByTestId("scheme-description")).toBeNull());
      expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent("Pages");
      expect(screen.queryByText("Taken")).toBeNull();
      expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
      expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    },
  );

  it("keeps the name and the other scheme when the reload after success fails", async () => {
    updateScheme.mockResolvedValue({ ...source, description: NEXT });
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
      expect(screen.getAllByTestId("location-scheme-description").length).toBe(2),
    );
    fireEvent.click(screen.getAllByTestId("location-scheme-description")[0]);
    await waitFor(() => expect(screen.getByTestId("scheme-description")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: NEXT } });
    failRefresh = true;
    fireEvent.click(screen.getByTestId("location-scheme-description-save"));
    await waitFor(() => expect(screen.queryByTestId("scheme-description")).toBeNull());
    expect(screen.getByTestId("scheme-list-description-11")).toHaveTextContent(NEXT);
    expect(screen.getByRole("button", { name: "Article" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Brief" })).toBeTruthy();
    expect(screen.getByTestId("scheme-list-description-12")).toHaveTextContent("Short");
    expect(screen.getByTestId("scheme-list-generator-11")).toHaveTextContent(GENERATOR);
    expect(createScheme).not.toHaveBeenCalled();
  });

  it("still opens the description form from the list row when the scheme read fails", async () => {
    getScheme.mockRejectedValue(new Error("read failed"));
    listContexts.mockResolvedValue([{ contextId: "3", name: "Publish" }]);
    listSchemesForContext.mockResolvedValue([
      {
        schemeId: "11",
        name: "Article",
        generator: GENERATOR,
        description: "Pages",
        contextId: "3",
      },
    ]);
    updateScheme.mockResolvedValue({ schemeId: "11", description: NEXT });
    renderPanel();
    await waitFor(() =>
      expect(screen.getByTestId("location-scheme-description")).toBeTruthy(),
    );
    fireEvent.click(screen.getByTestId("location-scheme-description"));
    await waitFor(() => expect(screen.getByTestId("scheme-description")).toBeTruthy());
    expect(screen.getByTestId("scheme-description-name")).toHaveTextContent("Article");
    expect(screen.getByTestId("scheme-description-generator")).toHaveTextContent(GENERATOR);
    expect(screen.getByLabelText("Description")).toHaveValue("Pages");
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: NEXT } });
    fireEvent.click(screen.getByTestId("location-scheme-description-save"));
    await waitFor(() =>
      expect(updateScheme).toHaveBeenCalledWith("11", { description: NEXT }),
    );
  });
});
