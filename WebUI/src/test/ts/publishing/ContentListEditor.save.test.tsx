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
import { ContentListEditor } from "@/publishing/design/ContentListEditor";

const createContentList = vi.fn();
const updateContentList = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createContentList: (...args: unknown[]) => createContentList(...args),
  updateContentList: (...args: unknown[]) => updateContentList(...args),
  deleteContentList: vi.fn(),
}));

vi.mock("@/api/developer/itemFiltersApi", () => ({
  listItemFilters: () => Promise.resolve([]),
}));

const existing = {
  contentListId: "5",
  name: "NightCl",
  description: "old",
  listType: "modern",
  generator: "sys_searchList",
};

describe("ContentListEditor save", () => {
  beforeEach(() => {
    createContentList.mockReset();
    updateContentList.mockReset();
  });

  it("creates a new content list then calls onSaved", async () => {
    createContentList.mockResolvedValue({
      contentListId: "12",
      name: "NightCl",
    });
    const onSaved = vi.fn();
    render(
      <ContentListEditor
        contentList={null}
        onSaved={onSaved}
        onCancel={() => undefined}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "NightCl" },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    await waitFor(() => expect(createContentList).toHaveBeenCalled());
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });

  it("shows 409 conflict on the editor", async () => {
    createContentList.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Content list name already exists" },
    });
    render(
      <ContentListEditor
        contentList={null}
        onSaved={() => undefined}
        onCancel={() => undefined}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "Dup" },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list name already exists",
    );
  });

  it("renames an existing content list without changing its type", async () => {
    updateContentList.mockResolvedValue({
      contentListId: "5",
      name: "Renamed",
      listType: "modern",
    });
    const onSaved = vi.fn();
    render(
      <ContentListEditor
        contentList={existing}
        onSaved={onSaved}
        onCancel={() => undefined}
      />,
    );
    const name = screen.getByLabelText(/Name/i);
    expect(name).not.toBeDisabled();
    expect(screen.getByLabelText(/^Type$/)).toBeDisabled();
    await screen.findByTestId("contentlist-item-filter");
    fireEvent.change(screen.getByLabelText(/^Type$/), {
      target: { value: "legacy" },
    });
    fireEvent.change(name, { target: { value: "  Renamed  " } });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith("5", {
        name: "Renamed",
        description: "old",
        generator: "sys_searchList",
        url: undefined,
        listType: "modern",
        itemFilterId: "",
      }),
    );
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
    expect(createContentList).not.toHaveBeenCalled();
  });

  it("rejects a blank rename in the client", async () => {
    const onSaved = vi.fn();
    render(
      <ContentListEditor
        contentList={existing}
        onSaved={onSaved}
        onCancel={() => undefined}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    expect(updateContentList).not.toHaveBeenCalled();
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("does not PUT when rename is cancelled", () => {
    const onCancel = vi.fn();
    render(
      <ContentListEditor
        contentList={existing}
        onSaved={() => undefined}
        onCancel={onCancel}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "Nope" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(updateContentList).not.toHaveBeenCalled();
    expect(onCancel).toHaveBeenCalled();
  });

  it("does not treat a duplicate rename as saved", async () => {
    updateContentList.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Content list name already exists" },
    });
    const onSaved = vi.fn();
    render(
      <ContentListEditor
        contentList={existing}
        onSaved={onSaved}
        onCancel={() => undefined}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "Taken" },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list name already exists",
    );
    expect(onSaved).not.toHaveBeenCalled();
    expect(screen.getByLabelText(/Name/i)).toHaveValue("Taken");
  });

  it("saves a description without renaming", async () => {
    updateContentList.mockResolvedValue({
      contentListId: "5",
      name: "NightCl",
      description: "notes",
      listType: "modern",
    });
    const onSaved = vi.fn();
    render(
      <ContentListEditor
        contentList={existing}
        onSaved={onSaved}
        onCancel={() => undefined}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Description/i), {
      target: { value: "notes" },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith(
        "5",
        expect.objectContaining({
          name: "NightCl",
          description: "notes",
          listType: "modern",
        }),
      ),
    );
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });
});
