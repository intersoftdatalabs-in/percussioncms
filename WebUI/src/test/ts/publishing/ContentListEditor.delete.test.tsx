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

const deleteContentList = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createContentList: vi.fn(),
  updateContentList: vi.fn(),
  deleteContentList: (...args: unknown[]) => deleteContentList(...args),
}));

const contentList = {
  contentListId: "5",
  name: "NightCl",
  listType: "modern",
};

function renderEditor(onSaved = vi.fn()) {
  render(
    <ContentListEditor
      contentList={contentList}
      onSaved={onSaved}
      onCancel={() => undefined}
    />,
  );
  return { onSaved };
}

describe("ContentListEditor delete", () => {
  beforeEach(() => {
    deleteContentList.mockReset();
    vi.restoreAllMocks();
  });

  it("deletes after confirm and reports saved", async () => {
    deleteContentList.mockResolvedValue(undefined);
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const { onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("contentlist-delete"));
    expect(confirm).toHaveBeenCalled();
    await waitFor(() => expect(deleteContentList).toHaveBeenCalledWith("5"));
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });

  it("does not delete when confirm is dismissed", () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    const { onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("contentlist-delete"));
    expect(deleteContentList).not.toHaveBeenCalled();
    expect(onSaved).not.toHaveBeenCalled();
    expect(screen.getByTestId("contentlist-editor")).toBeInTheDocument();
  });

  it("shows 409 conflict and stays on the editor", async () => {
    deleteContentList.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Content list is in use" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("contentlist-delete"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Content list is in use",
    );
    expect(onSaved).not.toHaveBeenCalled();
    expect(screen.getByTestId("contentlist-editor")).toBeInTheDocument();
  });

  it("shows 403 without treating the delete as success", async () => {
    deleteContentList.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {
        message: "Admin or Designer role required to save a publish edition",
      },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("contentlist-delete"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /Admin or Designer|403|Forbidden/i,
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("shows 400 without treating the delete as success", async () => {
    deleteContentList.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "contentListId is required" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("contentlist-delete"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "contentListId is required",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("hides delete on a new content list", () => {
    render(
      <ContentListEditor
        contentList={null}
        onSaved={() => undefined}
        onCancel={() => undefined}
      />,
    );
    expect(screen.queryByTestId("contentlist-delete")).not.toBeInTheDocument();
  });
});
