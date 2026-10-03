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

const deleteEdition = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createEdition: vi.fn(),
  updateEdition: vi.fn(),
  deleteEdition: (...args: unknown[]) => deleteEdition(...args),
  copyEdition: vi.fn(),
  listEditionContentLists: vi.fn().mockResolvedValue([]),
  listContentLists: vi.fn().mockResolvedValue([]),
  listContexts: vi.fn().mockResolvedValue([]),
  associateContentList: vi.fn(),
  disassociateContentList: vi.fn(),
}));

const edition = { editionId: "7", name: "NightEd", siteId: "42" };
const sites = [{ name: "Current", id: "42" }];

function renderEditor(onSaved = vi.fn()) {
  render(
    <EditionEditor
      siteId="42"
      edition={edition}
      sites={sites}
      onSaved={onSaved}
      onCancel={() => undefined}
    />,
  );
  return { onSaved };
}

describe("EditionEditor delete", () => {
  beforeEach(() => {
    deleteEdition.mockReset();
    vi.restoreAllMocks();
  });

  it("deletes after confirm and reports saved", async () => {
    deleteEdition.mockResolvedValue(undefined);
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const { onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("edition-delete"));
    expect(confirm).toHaveBeenCalled();
    await waitFor(() => expect(deleteEdition).toHaveBeenCalledWith("7"));
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });

  it("does not delete when confirm is dismissed", () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    const { onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("edition-delete"));
    expect(deleteEdition).not.toHaveBeenCalled();
    expect(onSaved).not.toHaveBeenCalled();
    expect(screen.getByTestId("edition-editor")).toBeInTheDocument();
  });

  it("shows 409 conflict and stays on the editor", async () => {
    deleteEdition.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Edition is in use" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("edition-delete"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Edition is in use");
    expect(onSaved).not.toHaveBeenCalled();
    expect(screen.getByTestId("edition-editor")).toBeInTheDocument();
  });

  it("shows 403 without treating the delete as success", async () => {
    deleteEdition.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin or Designer role required to save a publish edition" },
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("edition-delete"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/Admin or Designer|403|Forbidden/i);
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("hides delete on a new edition", () => {
    render(
      <EditionEditor
        siteId="42"
        edition={null}
        sites={sites}
        onSaved={() => undefined}
        onCancel={() => undefined}
      />,
    );
    expect(screen.queryByTestId("edition-delete")).not.toBeInTheDocument();
  });
});
