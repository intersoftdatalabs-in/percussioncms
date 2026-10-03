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
import { EditionEditor } from "@/publishing/design/EditionEditor";

const associateContentList = vi.fn();
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
  associateContentList: (...args: unknown[]) => associateContentList(...args),
  disassociateContentList: vi.fn(),
}));

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

async function chooseListAndContext(): Promise<void> {
  await screen.findByRole("option", { name: "Home Pages" });
  await screen.findByRole("option", { name: "Publish" });
  fireEvent.change(screen.getByTestId("edition-assoc-content-list"), {
    target: { value: "5" },
  });
  fireEvent.change(screen.getByTestId("edition-assoc-context"), {
    target: { value: "9" },
  });
}

describe("EditionEditor associate content list", () => {
  beforeEach(() => {
    associateContentList.mockReset();
    listEditionContentLists.mockReset();
    listContentLists.mockReset();
    listContexts.mockReset();
    listEditionContentLists.mockResolvedValue([]);
    listContentLists.mockResolvedValue([
      { contentListId: "5", name: "Home Pages", listType: "modern" },
    ]);
    listContexts.mockResolvedValue([{ contextId: "9", name: "Publish" }]);
  });

  it("does not call the server when neither list nor context is chosen", async () => {
    renderOpen();
    await screen.findByTestId("edition-associate");
    fireEvent.click(screen.getByTestId("edition-associate"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Select content list and delivery context",
    );
    expect(associateContentList).not.toHaveBeenCalled();
    expect(screen.getByTestId("edition-assoc-empty")).toBeInTheDocument();
  });

  it("does not call the server when the content list is missing", async () => {
    renderOpen();
    await screen.findByRole("option", { name: "Publish" });
    fireEvent.change(screen.getByTestId("edition-assoc-context"), {
      target: { value: "9" },
    });
    fireEvent.click(screen.getByTestId("edition-associate"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Select content list and delivery context",
    );
    expect(associateContentList).not.toHaveBeenCalled();
    expect(screen.getByTestId("edition-assoc-empty")).toBeInTheDocument();
  });

  it("does not call the server when the delivery context is missing", async () => {
    renderOpen();
    await screen.findByRole("option", { name: "Home Pages" });
    fireEvent.change(screen.getByTestId("edition-assoc-content-list"), {
      target: { value: "5" },
    });
    fireEvent.click(screen.getByTestId("edition-associate"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Select content list and delivery context",
    );
    expect(associateContentList).not.toHaveBeenCalled();
    expect(screen.getByTestId("edition-assoc-empty")).toBeInTheDocument();
  });

  it("lists the content list only after associate succeeds", async () => {
    let resolveAssoc: (value: {
      contentListId: string;
      name: string;
      listType: string;
    }) => void = () => undefined;
    associateContentList.mockReturnValue(
      new Promise((resolve) => {
        resolveAssoc = resolve;
      }),
    );
    renderOpen();
    await chooseListAndContext();
    fireEvent.click(screen.getByTestId("edition-associate"));
    await waitFor(() =>
      expect(associateContentList).toHaveBeenCalledWith("7", {
        contentListId: "5",
        deliveryContextId: "9",
      }),
    );
    expect(screen.getByTestId("edition-assoc-empty")).toBeInTheDocument();
    expect(screen.queryByTestId("edition-assoc-list")).not.toBeInTheDocument();
    resolveAssoc({
      contentListId: "5",
      name: "Home Pages",
      listType: "modern",
    });
    expect(await screen.findByTestId("edition-assoc-list")).toHaveTextContent(
      "Home Pages",
    );
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.queryByTestId("edition-assoc-empty")).not.toBeInTheDocument();
  });

  it("lists the content list when the server returns a numeric id", async () => {
    associateContentList.mockResolvedValue({
      contentListId: 1013,
      name: "Home Pages",
      listType: "legacy",
    });
    renderOpen();
    await chooseListAndContext();
    fireEvent.click(screen.getByTestId("edition-associate"));
    expect(await screen.findByTestId("edition-assoc-list")).toHaveTextContent(
      "Home Pages",
    );
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("keeps the new row when a stale list reload resolves after success", async () => {
    let resolveLists: (rows: Array<{ contentListId: string; name: string }>) => void =
      () => undefined;
    listEditionContentLists.mockReturnValue(
      new Promise((resolve) => {
        resolveLists = resolve;
      }),
    );
    associateContentList.mockResolvedValue({
      contentListId: "5",
      name: "Home Pages",
      listType: "modern",
    });
    renderOpen();
    await chooseListAndContext();
    fireEvent.click(screen.getByTestId("edition-associate"));
    expect(await screen.findByTestId("edition-assoc-list")).toHaveTextContent(
      "Home Pages",
    );
    resolveLists([]);
    await waitFor(() =>
      expect(screen.getByTestId("edition-assoc-list")).toHaveTextContent("Home Pages"),
    );
    expect(screen.queryByTestId("edition-assoc-empty")).not.toBeInTheDocument();
  });

  it.each([
    [400, "contentListId and deliveryContextId are required"],
    [403, "Admin or Designer role required to save a publish edition"],
    [409, "Content list is already associated with this edition"],
  ])("HTTP %s does not claim the list was associated", async (status, bodyMessage) => {
    associateContentList.mockRejectedValue({
      status,
      statusText: "error",
      body: { message: bodyMessage },
    });
    renderOpen();
    await chooseListAndContext();
    fireEvent.click(screen.getByTestId("edition-associate"));
    expect(await screen.findByRole("alert")).toHaveTextContent(bodyMessage);
    expect(screen.getByTestId("edition-assoc-empty")).toBeInTheDocument();
    expect(screen.queryByTestId("edition-assoc-list")).not.toBeInTheDocument();
  });
});
