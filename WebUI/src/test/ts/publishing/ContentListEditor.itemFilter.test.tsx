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

const updateContentList = vi.fn();
const listItemFilters = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createContentList: vi.fn(),
  updateContentList: (...args: unknown[]) => updateContentList(...args),
  deleteContentList: vi.fn(),
}));

vi.mock("@/api/developer/itemFiltersApi", () => ({
  listItemFilters: () => listItemFilters(),
}));

const existing = {
  contentListId: "5",
  name: "NightCl",
  description: "old",
  listType: "modern",
  generator: "sys_searchList",
  itemFilterId: "1",
  itemFilterName: "public",
};

const catalog = [
  { name: "public", filterId: { uuid: 1 } },
  { name: "preview", filterId: { uuid: 2 } },
];

describe("ContentListEditor item filter", () => {
  beforeEach(() => {
    updateContentList.mockReset();
    listItemFilters.mockReset();
    listItemFilters.mockResolvedValue(catalog);
  });

  async function renderExisting(): Promise<void> {
    render(
      <ContentListEditor
        contentList={existing}
        onSaved={() => undefined}
        onCancel={() => undefined}
      />,
    );
    await screen.findByRole("option", { name: "preview" });
  }

  it("keeps the stored filter while the draft select changes", async () => {
    await renderExisting();
    expect(screen.getByTestId("contentlist-stored-item-filter")).toHaveTextContent(
      "Saved item filter: public",
    );
    fireEvent.change(screen.getByTestId("contentlist-item-filter"), {
      target: { value: "2" },
    });
    expect(screen.getByTestId("contentlist-item-filter")).toHaveValue("2");
    expect(screen.getByTestId("contentlist-stored-item-filter")).toHaveTextContent(
      "Saved item filter: public",
    );
  });

  it("omits the filter until the catalog loads, then sends the chosen id", async () => {
    listItemFilters.mockReturnValue(new Promise(() => undefined));
    updateContentList.mockResolvedValue(existing);
    const { unmount } = render(
      <ContentListEditor
        contentList={existing}
        onSaved={() => undefined}
        onCancel={() => undefined}
      />,
    );
    fireEvent.click(screen.getByTestId("contentlist-save"));
    await waitFor(() => expect(updateContentList).toHaveBeenCalled());
    expect(
      (updateContentList.mock.calls[0][1] as { itemFilterId?: string }).itemFilterId,
    ).toBeUndefined();
    unmount();

    listItemFilters.mockResolvedValue(catalog);
    updateContentList.mockReset();
    updateContentList.mockResolvedValue({
      ...existing,
      itemFilterId: "2",
      itemFilterName: "preview",
    });
    const onSaved = vi.fn();
    render(
      <ContentListEditor
        contentList={existing}
        onSaved={onSaved}
        onCancel={() => undefined}
      />,
    );
    await screen.findByRole("option", { name: "preview" });
    fireEvent.change(screen.getByTestId("contentlist-item-filter"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith(
        "5",
        expect.objectContaining({ name: "NightCl", itemFilterId: "2" }),
      ),
    );
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });

  it("clears the filter with an empty id", async () => {
    updateContentList.mockResolvedValue({ ...existing, itemFilterId: undefined });
    await renderExisting();
    fireEvent.change(screen.getByTestId("contentlist-item-filter"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    await waitFor(() =>
      expect(updateContentList).toHaveBeenCalledWith(
        "5",
        expect.objectContaining({ itemFilterId: "" }),
      ),
    );
  });

  it("does not save when the name is blank", async () => {
    const onSaved = vi.fn();
    render(
      <ContentListEditor
        contentList={existing}
        onSaved={onSaved}
        onCancel={() => undefined}
      />,
    );
    await screen.findByRole("option", { name: "preview" });
    fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: "  " } });
    fireEvent.change(screen.getByTestId("contentlist-item-filter"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByTestId("contentlist-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    expect(updateContentList).not.toHaveBeenCalled();
    expect(onSaved).not.toHaveBeenCalled();
    expect(screen.getByTestId("contentlist-stored-item-filter")).toHaveTextContent(
      "public",
    );
  });

  it("does not treat HTTP 400, 403, or 409 as saved", async () => {
    const onSaved = vi.fn();
    render(
      <ContentListEditor
        contentList={existing}
        onSaved={onSaved}
        onCancel={() => undefined}
      />,
    );
    await screen.findByRole("option", { name: "preview" });
    fireEvent.change(screen.getByTestId("contentlist-item-filter"), {
      target: { value: "2" },
    });

    for (const status of [400, 403, 409]) {
      updateContentList.mockRejectedValueOnce({
        status,
        statusText: "no",
        body: { message: `filter failed ${status}` },
      });
      fireEvent.click(screen.getByTestId("contentlist-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(`filter failed ${status}`);
      expect(onSaved).not.toHaveBeenCalled();
      expect(screen.getByTestId("contentlist-stored-item-filter")).toHaveTextContent(
        "public",
      );
    }
  });

  it("does not PUT when the filter change is cancelled", async () => {
    const onCancel = vi.fn();
    render(
      <ContentListEditor
        contentList={existing}
        onSaved={() => undefined}
        onCancel={onCancel}
      />,
    );
    await screen.findByRole("option", { name: "preview" });
    fireEvent.change(screen.getByTestId("contentlist-item-filter"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Back/i }));
    expect(updateContentList).not.toHaveBeenCalled();
    expect(onCancel).toHaveBeenCalled();
  });

  it("hides the filter on a legacy list and does not send one", async () => {
    updateContentList.mockResolvedValue({
      contentListId: "5",
      name: "Legacy",
      listType: "legacy",
    });
    render(
      <ContentListEditor
        contentList={{ ...existing, listType: "legacy", url: "rx" }}
        onSaved={() => undefined}
        onCancel={() => undefined}
      />,
    );
    expect(screen.queryByTestId("contentlist-item-filter")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("contentlist-save"));
    await waitFor(() => expect(updateContentList).toHaveBeenCalled());
    const body = updateContentList.mock.calls[0][1] as { itemFilterId?: string };
    expect(body.itemFilterId).toBeUndefined();
    expect(listItemFilters).not.toHaveBeenCalled();
  });
});
