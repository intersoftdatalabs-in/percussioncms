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

const updateEdition = vi.fn();
const reorderEditionContentList = vi.fn();
const associateContentList = vi.fn();
const disassociateContentList = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createEdition: vi.fn(),
  updateEdition: (...args: unknown[]) => updateEdition(...args),
  deleteEdition: vi.fn(),
  copyEdition: vi.fn(),
  listEditionContentLists: vi.fn().mockResolvedValue([
    { contentListId: "a", name: "Alpha", listType: "modern" },
    { contentListId: "b", name: "Beta", listType: "modern" },
  ]),
  listContentLists: vi.fn().mockResolvedValue([]),
  listContexts: vi.fn().mockResolvedValue([]),
  associateContentList: (...args: unknown[]) => associateContentList(...args),
  disassociateContentList: (...args: unknown[]) =>
    disassociateContentList(...args),
  reorderEditionContentList: (...args: unknown[]) =>
    reorderEditionContentList(...args),
}));

const edition = {
  editionId: "12",
  name: "NightEd",
  siteId: "1",
  comment: "keep-me",
  priority: 5,
};

function renderEditor(): { onSaved: ReturnType<typeof vi.fn> } {
  const onSaved = vi.fn();
  render(
    <EditionEditor
      siteId="1"
      edition={edition}
      sites={[{ name: "S", id: "1" }]}
      onSaved={onSaved}
      onCancel={() => undefined}
    />,
  );
  return { onSaved };
}

describe("EditionEditor rename", () => {
  beforeEach(() => {
    updateEdition.mockReset();
    reorderEditionContentList.mockReset();
    associateContentList.mockReset();
    disassociateContentList.mockReset();
  });

  it("updates the name only and leaves content-list actions alone", async () => {
    updateEdition.mockResolvedValue({ ...edition, name: "RenamedEd" });
    const { onSaved } = renderEditor();
    expect(await screen.findByTestId("edition-assoc-name-a")).toHaveTextContent(
      "Alpha",
    );
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "RenamedEd" },
    });
    fireEvent.click(screen.getByTestId("edition-save"));
    await waitFor(() =>
      expect(updateEdition).toHaveBeenCalledWith("12", {
        editionId: "12",
        name: "RenamedEd",
        siteId: "1",
      }),
    );
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("comment");
    expect(updateEdition.mock.calls[0][1]).not.toHaveProperty("priority");
    expect(reorderEditionContentList).not.toHaveBeenCalled();
    expect(associateContentList).not.toHaveBeenCalled();
    expect(disassociateContentList).not.toHaveBeenCalled();
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });

  it("still sends comment and priority when those fields were edited", async () => {
    updateEdition.mockResolvedValue({ ...edition, comment: "edited" });
    renderEditor();
    fireEvent.change(screen.getByLabelText(/^Comment$/), {
      target: { value: "edited" },
    });
    fireEvent.change(screen.getByLabelText(/Priority/i), {
      target: { value: "4" },
    });
    fireEvent.click(screen.getByTestId("edition-save"));
    await waitFor(() =>
      expect(updateEdition).toHaveBeenCalledWith("12", {
        name: "NightEd",
        comment: "edited",
        priority: 4,
        siteId: "1",
        editionId: "12",
      }),
    );
  });

  it("rejects a blank or over-long name without calling update", async () => {
    const { onSaved } = renderEditor();
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("edition-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Name is required");
    fireEvent.change(screen.getByLabelText("* Name"), {
      target: { value: "N".repeat(101) },
    });
    fireEvent.click(screen.getByTestId("edition-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Edition name must be 100 characters or fewer",
    );
    expect(updateEdition).not.toHaveBeenCalled();
    expect(onSaved).not.toHaveBeenCalled();
  });

  it.each([
    [400, "Bad Request", "Edition name must be 100 characters or fewer"],
    [403, "Forbidden", "Admin or Designer role required to save a publish edition"],
    [409, "Conflict", "Edition name already exists"],
  ])(
    "shows HTTP %s on the editor and does not close",
    async (status, statusText, message) => {
      updateEdition.mockRejectedValue({ status, statusText, body: { message } });
      const { onSaved } = renderEditor();
      fireEvent.change(screen.getByLabelText("* Name"), {
        target: { value: "RenamedEd" },
      });
      fireEvent.click(screen.getByTestId("edition-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(onSaved).not.toHaveBeenCalled();
      expect(screen.getByLabelText("* Name")).toHaveValue("RenamedEd");
    },
  );
});
