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
import { describe, expect, it, vi } from "vitest";
import { EditionEditor } from "@/publishing/design/EditionEditor";

const copyEdition = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createEdition: vi.fn(),
  updateEdition: vi.fn(),
  deleteEdition: vi.fn(),
  copyEdition: (...args: unknown[]) => copyEdition(...args),
  listEditionContentLists: vi.fn().mockResolvedValue([]),
  listContentLists: vi.fn().mockResolvedValue([]),
  listContexts: vi.fn().mockResolvedValue([]),
  associateContentList: vi.fn(),
  disassociateContentList: vi.fn(),
}));

const edition = { editionId: "7", name: "SourceEd", siteId: "42" };
const sites = [
  { name: "Current", id: "42" },
  { name: "Other", id: "9" },
];

function renderEditor(onCopied = vi.fn(), onSaved = vi.fn()) {
  render(
    <EditionEditor
      siteId="42"
      edition={edition}
      sites={sites}
      onSaved={onSaved}
      onCopied={onCopied}
      onCancel={() => undefined}
    />,
  );
  return { onCopied, onSaved };
}

describe("EditionEditor copy", () => {
  it("copies onto the selected site and reports the new name", async () => {
    copyEdition.mockResolvedValue({
      editionId: "88",
      name: "NightCopy",
      siteId: "9",
    });
    const { onCopied, onSaved } = renderEditor();
    fireEvent.change(screen.getByLabelText(/Target site/i), {
      target: { value: "9" },
    });
    fireEvent.change(screen.getByLabelText(/New name/i), {
      target: { value: "NightCopy" },
    });
    fireEvent.click(screen.getByTestId("edition-copy"));
    await waitFor(() =>
      expect(copyEdition).toHaveBeenCalledWith({
        sourceEditionId: "7",
        targetSiteId: "9",
        newName: "NightCopy",
        copyContentLists: true,
      }),
    );
    await waitFor(() =>
      expect(onCopied).toHaveBeenCalledWith({
        targetSiteId: "9",
        name: "NightCopy",
      }),
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("shows HTTP 400 on the editor and does not leave the copy", async () => {
    copyEdition.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "sourceEditionId and targetSiteId are required" },
    });
    const { onCopied, onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("edition-copy"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "sourceEditionId and targetSiteId are required",
    );
    expect(onCopied).not.toHaveBeenCalled();
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("shows HTTP 403 on the editor and does not leave the copy", async () => {
    copyEdition.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { message: "Admin or Designer role required" },
    });
    const { onCopied, onSaved } = renderEditor();
    fireEvent.click(screen.getByTestId("edition-copy"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Admin or Designer role required",
    );
    expect(onCopied).not.toHaveBeenCalled();
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("shows a status when a 403 body has no message", async () => {
    copyEdition.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {},
    });
    renderEditor();
    fireEvent.click(screen.getByTestId("edition-copy"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/403|Forbidden/i);
  });
});
