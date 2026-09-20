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

const createEdition = vi.fn();
const updateEdition = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createEdition: (...args: unknown[]) => createEdition(...args),
  updateEdition: (...args: unknown[]) => updateEdition(...args),
  deleteEdition: vi.fn(),
  copyEdition: vi.fn(),
  listEditionContentLists: vi.fn().mockResolvedValue([]),
  listContentLists: vi.fn().mockResolvedValue([]),
  listContexts: vi.fn().mockResolvedValue([]),
  associateContentList: vi.fn(),
  disassociateContentList: vi.fn(),
}));

describe("EditionEditor save", () => {
  it("creates a new edition then calls onSaved", async () => {
    createEdition.mockResolvedValue({ editionId: "12", name: "NightEd" });
    const onSaved = vi.fn();
    render(
      <EditionEditor
        siteId="42"
        edition={null}
        sites={[{ name: "S", id: "42" }]}
        onSaved={onSaved}
        onCancel={() => undefined}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "NightEd" },
    });
    fireEvent.click(screen.getByTestId("edition-save"));
    await waitFor(() => expect(createEdition).toHaveBeenCalled());
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });

  it("shows 409 conflict on the editor", async () => {
    createEdition.mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Edition name already exists" },
    });
    render(
      <EditionEditor
        siteId="42"
        edition={null}
        sites={[{ name: "S", id: "42" }]}
        onSaved={() => undefined}
        onCancel={() => undefined}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Name/i), {
      target: { value: "Dup" },
    });
    fireEvent.click(screen.getByTestId("edition-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Edition name already exists",
    );
  });
});
