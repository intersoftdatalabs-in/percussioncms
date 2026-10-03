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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { EditionEditor } from "@/publishing/design/EditionEditor";

const disassociateContentList = vi.fn();
const listEditionContentLists = vi.fn();
const listContentLists = vi.fn();
const listContexts = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createEdition: vi.fn(),
  updateEdition: vi.fn(),
  deleteEdition: vi.fn(),
  copyEdition: vi.fn(),
  listEditionContentLists: (...args: unknown[]) => listEditionContentLists(...args),
  listContentLists: (...args: unknown[]) => listContentLists(...args),
  listContexts: (...args: unknown[]) => listContexts(...args),
  associateContentList: vi.fn(),
  disassociateContentList: (...args: unknown[]) => disassociateContentList(...args),
}));

const CONFIRM =
  "Remove this content list from the edition? The content list itself is not deleted.";

function renderOpen() {
  return render(
    <EditionEditor
      siteId="42"
      edition={{ editionId: "7", name: "Night" }}
      sites={[{ name: "S", id: "42" }]}
      onSaved={() => undefined}
      onCancel={() => undefined}
    />,
  );
}

describe("EditionEditor remove content list", () => {
  const realConfirm = window.confirm;

  beforeEach(() => {
    disassociateContentList.mockReset();
    listEditionContentLists.mockReset();
    listContentLists.mockReset();
    listContexts.mockReset();
    listEditionContentLists.mockResolvedValue([
      { contentListId: "5", name: "Home Pages", listType: "modern" },
      { contentListId: "6", name: "News", listType: "legacy" },
    ]);
    listContentLists.mockResolvedValue([]);
    listContexts.mockResolvedValue([]);
    window.confirm = vi.fn().mockReturnValue(true);
  });

  afterEach(() => {
    window.confirm = realConfirm;
  });

  it("does not call the server when confirm is cancelled", async () => {
    window.confirm = vi.fn().mockReturnValue(false);
    renderOpen();
    fireEvent.click(await screen.findByTestId("edition-disassociate-5"));
    expect(window.confirm).toHaveBeenCalledWith(CONFIRM);
    expect(disassociateContentList).not.toHaveBeenCalled();
    expect(screen.getByTestId("edition-assoc-list")).toHaveTextContent("Home Pages");
    expect(screen.getByTestId("edition-assoc-list")).toHaveTextContent("News");
  });

  it("removes only the confirmed row after delete succeeds", async () => {
    let resolveDelete: () => void = () => undefined;
    disassociateContentList.mockReturnValue(
      new Promise<void>((resolve) => {
        resolveDelete = resolve;
      }),
    );
    renderOpen();
    fireEvent.click(await screen.findByTestId("edition-disassociate-5"));
    await waitFor(() =>
      expect(disassociateContentList).toHaveBeenCalledWith("7", "5"),
    );
    expect(screen.getByTestId("edition-assoc-list")).toHaveTextContent("Home Pages");
    expect(screen.getByTestId("edition-assoc-list")).toHaveTextContent("News");
    resolveDelete();
    await waitFor(() =>
      expect(screen.getByTestId("edition-assoc-list")).not.toHaveTextContent(
        "Home Pages",
      ),
    );
    expect(screen.getByTestId("edition-assoc-list")).toHaveTextContent("News");
    expect(screen.queryByTestId("edition-disassociate-5")).not.toBeInTheDocument();
    expect(screen.getByTestId("edition-disassociate-6")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("keeps the other row when a follow-up list reload fails", async () => {
    const rows = [
      { contentListId: "5", name: "Home Pages", listType: "modern" },
      { contentListId: "6", name: "News", listType: "legacy" },
    ];
    let calls = 0;
    listEditionContentLists.mockImplementation(() => {
      calls += 1;
      if (calls === 1) {
        return Promise.resolve(rows);
      }
      return Promise.reject(new Error("reload failed"));
    });
    disassociateContentList.mockResolvedValue(undefined);
    renderOpen();
    fireEvent.click(await screen.findByTestId("edition-disassociate-5"));
    await waitFor(() =>
      expect(screen.getByTestId("edition-assoc-list")).not.toHaveTextContent(
        "Home Pages",
      ),
    );
    await waitFor(() => expect(calls).toBeGreaterThanOrEqual(1));
    expect(screen.getByTestId("edition-assoc-list")).toHaveTextContent("News");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it.each([
    [400, "contentListId is required"],
    [403, "Admin or Designer role required to save a publish edition"],
    [409, "Edition is in use"],
  ])("HTTP %s does not claim the list was removed", async (status, bodyMessage) => {
    disassociateContentList.mockRejectedValue({
      status,
      statusText: "error",
      body: { message: bodyMessage },
    });
    renderOpen();
    fireEvent.click(await screen.findByTestId("edition-disassociate-5"));
    expect(await screen.findByRole("alert")).toHaveTextContent(bodyMessage);
    expect(screen.getByTestId("edition-assoc-list")).toHaveTextContent("Home Pages");
    expect(screen.getByTestId("edition-assoc-list")).toHaveTextContent("News");
    expect(screen.getByTestId("edition-disassociate-5")).toBeInTheDocument();
  });
});
