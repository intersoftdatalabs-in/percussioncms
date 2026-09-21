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
import { ContentListEditor } from "@/publishing/design/ContentListEditor";

const createContentList = vi.fn();
const updateContentList = vi.fn();

vi.mock("@/api/publishing/designApi", () => ({
  createContentList: (...args: unknown[]) => createContentList(...args),
  updateContentList: (...args: unknown[]) => updateContentList(...args),
  deleteContentList: vi.fn(),
}));

describe("ContentListEditor save", () => {
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
});
