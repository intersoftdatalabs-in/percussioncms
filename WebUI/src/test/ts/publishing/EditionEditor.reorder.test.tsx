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

const reorderEditionContentList = vi.fn();
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
  disassociateContentList: vi.fn(),
  reorderEditionContentList: (...args: unknown[]) =>
    reorderEditionContentList(...args),
}));

const CONFIRM_UP = "Move this content list up in the edition?";
const CONFIRM_DOWN = "Move this content list down in the edition?";

const TWO = [
  { contentListId: "5", name: "Home Pages", listType: "modern" },
  { contentListId: "6", name: "News", listType: "legacy" },
];

function renderOpen(
  rows: Array<{ contentListId: string; name: string; listType: string }> = TWO,
) {
  listEditionContentLists.mockResolvedValue(rows);
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

function orderOf(name: string): number {
  return (screen.getByTestId("edition-assoc-list").textContent ?? "").indexOf(name);
}

describe("EditionEditor reorder content list", () => {
  const realConfirm = window.confirm;

  beforeEach(() => {
    reorderEditionContentList.mockReset();
    listEditionContentLists.mockReset();
    listContentLists.mockReset();
    listContexts.mockReset();
    listContentLists.mockResolvedValue([]);
    listContexts.mockResolvedValue([]);
    window.confirm = vi.fn().mockReturnValue(true);
  });

  afterEach(() => {
    window.confirm = realConfirm;
  });

  it("disables move up on the first row and move down on the last", async () => {
    renderOpen();
    expect(await screen.findByTestId("edition-move-up-5")).toBeDisabled();
    expect(screen.getByTestId("edition-move-down-6")).toBeDisabled();
    expect(screen.getByTestId("edition-move-down-5")).toBeEnabled();
    expect(screen.getByTestId("edition-move-up-6")).toBeEnabled();
    expect(reorderEditionContentList).not.toHaveBeenCalled();
  });

  it("disables both moves when the edition has one content list", async () => {
    renderOpen([{ contentListId: "5", name: "Home Pages", listType: "modern" }]);
    expect(await screen.findByTestId("edition-move-up-5")).toBeDisabled();
    expect(screen.getByTestId("edition-move-down-5")).toBeDisabled();
  });

  it("does not call the server when confirm is cancelled", async () => {
    window.confirm = vi.fn().mockReturnValue(false);
    renderOpen();
    fireEvent.click(await screen.findByTestId("edition-move-up-6"));
    expect(window.confirm).toHaveBeenCalledWith(CONFIRM_UP);
    expect(reorderEditionContentList).not.toHaveBeenCalled();
    expect(orderOf("Home Pages")).toBeLessThan(orderOf("News"));
  });

  it("swaps the row only after move up succeeds", async () => {
    let resolveMove: () => void = () => undefined;
    reorderEditionContentList.mockReturnValue(
      new Promise<void>((resolve) => {
        resolveMove = resolve;
      }),
    );
    renderOpen();
    fireEvent.click(await screen.findByTestId("edition-move-up-6"));
    await waitFor(() =>
      expect(reorderEditionContentList).toHaveBeenCalledWith("7", "6", 0),
    );
    expect(window.confirm).toHaveBeenCalledWith(CONFIRM_UP);
    expect(orderOf("Home Pages")).toBeLessThan(orderOf("News"));
    resolveMove();
    await waitFor(() => expect(orderOf("News")).toBeLessThan(orderOf("Home Pages")));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("moves the first row down only after success", async () => {
    reorderEditionContentList.mockResolvedValue(undefined);
    renderOpen();
    fireEvent.click(await screen.findByTestId("edition-move-down-5"));
    expect(window.confirm).toHaveBeenCalledWith(CONFIRM_DOWN);
    await waitFor(() =>
      expect(reorderEditionContentList).toHaveBeenCalledWith("7", "5", 1),
    );
    await waitFor(() => expect(orderOf("News")).toBeLessThan(orderOf("Home Pages")));
  });

  it("keeps the new order when a follow-up list reload fails", async () => {
    const rows = TWO.map((row) => ({ ...row }));
    let calls = 0;
    listEditionContentLists.mockImplementation(() => {
      calls += 1;
      if (calls === 1) {
        return Promise.resolve(rows);
      }
      return Promise.reject(new Error("reload failed"));
    });
    reorderEditionContentList.mockResolvedValue(undefined);
    render(
      <EditionEditor
        siteId="42"
        edition={{ editionId: "7", name: "Night" }}
        sites={[{ name: "S", id: "42" }]}
        onSaved={() => undefined}
        onCancel={() => undefined}
      />,
    );
    fireEvent.click(await screen.findByTestId("edition-move-up-6"));
    await waitFor(() => expect(orderOf("News")).toBeLessThan(orderOf("Home Pages")));
    await waitFor(() => expect(calls).toBeGreaterThanOrEqual(1));
    expect(orderOf("News")).toBeLessThan(orderOf("Home Pages"));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it.each([
    [400, "sequence must be the adjacent position"],
    [403, "Admin or Designer role required to save a publish edition"],
    [409, "Edition is in use"],
  ])("HTTP %s does not change the rendered order", async (status, bodyMessage) => {
    reorderEditionContentList.mockRejectedValue({
      status,
      statusText: "error",
      body: { message: bodyMessage },
    });
    renderOpen();
    fireEvent.click(await screen.findByTestId("edition-move-up-6"));
    expect(await screen.findByRole("alert")).toHaveTextContent(bodyMessage);
    expect(orderOf("Home Pages")).toBeLessThan(orderOf("News"));
    expect(screen.getByTestId("edition-move-up-6")).toBeEnabled();
  });
});
