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
import { EditorRelatedContentPanel } from "../../../main/ts/editor/EditorRelatedContentPanel";

const canvas = {
  ownerId: 42,
  templateId: 7,
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
    { slotId: 11, name: "sidebar", label: "Sidebar", items: [] },
  ],
};

const local = {
  count: 1,
  links: [{ type: "local", targetId: "88" }],
};

describe("Editor related slot move (#6225)", () => {
  afterEach(() => {
    cleanup();
  });

  it("moves a slot row to another slot and cancel does not post", async () => {
    const change = vi.fn().mockResolvedValue({
      relationshipId: 3,
      ownerId: 42,
      dependentId: 55,
      slotId: 11,
      templateId: 7,
      sortRank: 0,
    });
    render(
      <EditorRelatedContentPanel
        itemId="42"
        loadCanvas={async () => canvas}
        loadLocal={async () => local}
        changeSnippetTemplate={change}
        openRelatedItem={async () => true}
      />,
    );
    await waitFor(() => {
      expect(screen.getAllByTestId("editor-related-move-slot")).toHaveLength(1);
    });
    const rows = screen.getAllByTestId("editor-related-row");
    expect(
      rows[1].querySelector('[data-testid="editor-related-move-slot"]'),
    ).toBeNull();
    fireEvent.click(screen.getByTestId("editor-related-move-slot"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-slot-move-dialog")).toBeTruthy();
    });
    expect(
      screen.getByTestId("editor-related-slot-move-select").textContent,
    ).not.toMatch(/Content/);
    fireEvent.click(screen.getByTestId("editor-related-slot-move-cancel"));
    expect(change).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-related-slot-move-dialog")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-related-move-slot"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-slot-move-select")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-related-slot-move-select"), {
      target: { value: "11" },
    });
    fireEvent.click(screen.getByTestId("editor-related-slot-move-apply"));
    await waitFor(() => {
      expect(change).toHaveBeenCalledWith(3, 11, 7);
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-slot-move-saved")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-related-panel")).toBeTruthy();
  });

  it("keeps the panel when the slot move write is forbidden", async () => {
    const change = vi.fn().mockRejectedValue({ status: 403, statusText: "No", body: {} });
    render(
      <EditorRelatedContentPanel
        itemId="42"
        loadCanvas={async () => canvas}
        loadLocal={async () => ({ count: 0, links: [] })}
        changeSnippetTemplate={change}
        openRelatedItem={async () => true}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-move-slot")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-related-move-slot"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-slot-move-apply")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-related-slot-move-select"), {
      target: { value: "11" },
    });
    fireEvent.click(screen.getByTestId("editor-related-slot-move-apply"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-slot-move-error").textContent).toMatch(
        /not allowed to move/i,
      );
    });
    expect(screen.getByTestId("editor-related-panel")).toBeTruthy();
    expect(screen.getByTestId("editor-related-list")).toBeTruthy();
  });

  it("asks for a destination before moving and hides move in read-only mode", async () => {
    const change = vi.fn();
    const { rerender } = render(
      <EditorRelatedContentPanel
        itemId="42"
        loadCanvas={async () => canvas}
        loadLocal={async () => ({ count: 0, links: [] })}
        changeSnippetTemplate={change}
        openRelatedItem={async () => true}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-move-slot")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-related-move-slot"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-slot-move-apply")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-related-slot-move-apply"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-slot-move-error").textContent).toMatch(
        /choose a destination slot/i,
      );
    });
    expect(change).not.toHaveBeenCalled();
    rerender(
      <EditorRelatedContentPanel
        itemId="42"
        readOnly
        loadCanvas={async () => canvas}
        loadLocal={async () => ({ count: 0, links: [] })}
        openRelatedItem={async () => true}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-list")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-related-move-slot")).toBeNull();
  });
});
