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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

const fetchSlotCanvas = vi.fn().mockResolvedValue({
  ownerId: 42,
  templateId: null,
  slots: [],
});
const fetchLocal = vi.fn().mockResolvedValue({ count: 0, links: [] });
const addSlotRelationship = vi.fn();

vi.mock("../../../main/ts/api/contentExplorer/slotRelationshipApi", () => ({
  fetchSlotCanvas: (...args: unknown[]) => fetchSlotCanvas(...args),
  addSlotRelationship: (...args: unknown[]) => addSlotRelationship(...args),
}));

vi.mock("../../../main/ts/api/contentExplorer/relationshipsApi", () => ({
  fetchLocal: (...args: unknown[]) => fetchLocal(...args),
}));

import { EditorRelatedContentPanel } from "../../../main/ts/editor/EditorRelatedContentPanel";

describe("EditorRelatedContentPanel", () => {
  afterEach(() => {
    cleanup();
    fetchSlotCanvas.mockClear();
    fetchLocal.mockClear();
  });

  it("does not refetch when parent rerenders with default loaders", async () => {
    const { rerender } = render(<EditorRelatedContentPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-empty")).toBeTruthy();
    });
    expect(fetchSlotCanvas).toHaveBeenCalledTimes(1);
    expect(fetchLocal).toHaveBeenCalledTimes(1);
    rerender(<EditorRelatedContentPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-empty")).toBeTruthy();
    });
    expect(fetchSlotCanvas).toHaveBeenCalledTimes(1);
    expect(fetchLocal).toHaveBeenCalledTimes(1);
  });

  it("inserts an existing item into the selected slot and reloads", async () => {
    const canvas = {
      ownerId: 42,
      templateId: 7,
      slots: [{ slotId: 9, name: "content", label: "Content", items: [] }],
    };
    const loadCanvas = vi
      .fn()
      .mockResolvedValueOnce(canvas)
      .mockResolvedValueOnce({
        ...canvas,
        slots: [
          {
            slotId: 9,
            name: "content",
            label: "Content",
            items: [
              {
                relationshipId: 3,
                ownerId: 42,
                dependentId: 55,
                slotId: 9,
                templateId: 7,
                sortRank: 0,
              },
            ],
          },
        ],
      });
    const insertRelationship = vi.fn().mockResolvedValue({
      relationshipId: 3,
      ownerId: 42,
      dependentId: 55,
      slotId: 9,
      templateId: 7,
      sortRank: 0,
    });
    render(
      <EditorRelatedContentPanel
        itemId="42"
        loadCanvas={loadCanvas}
        loadLocal={async () => ({ count: 0, links: [] })}
        insertRelationship={insertRelationship}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-insert")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-related-item"), {
      target: { value: "55" },
    });
    fireEvent.click(screen.getByTestId("editor-related-insert-submit"));
    await waitFor(() => {
      expect(insertRelationship).toHaveBeenCalledWith({
        ownerId: 42,
        dependentId: 55,
        slotId: 9,
        templateId: 7,
      });
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-item-id").textContent).toBe("55");
    });
  });

  it("maps insert 403 and hides the form when read-only", async () => {
    const canvas = {
      ownerId: 42,
      templateId: 7,
      slots: [{ slotId: 9, name: "content", label: "Content", items: [] }],
    };
    const insertRelationship = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {},
    });
    const { rerender } = render(
      <EditorRelatedContentPanel
        itemId="42"
        loadCanvas={async () => canvas}
        loadLocal={async () => ({ count: 0, links: [] })}
        insertRelationship={insertRelationship}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-insert-submit")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-related-item"), {
      target: { value: "55" },
    });
    fireEvent.click(screen.getByTestId("editor-related-insert-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-insert-error").textContent).toMatch(
        /not allowed to insert/i,
      );
    });
    rerender(
      <EditorRelatedContentPanel
        itemId="42"
        readOnly
        loadCanvas={async () => canvas}
        loadLocal={async () => ({ count: 0, links: [] })}
        insertRelationship={insertRelationship}
      />,
    );
    await waitFor(() => {
      expect(screen.queryByTestId("editor-related-insert")).toBeNull();
    });
  });
});
