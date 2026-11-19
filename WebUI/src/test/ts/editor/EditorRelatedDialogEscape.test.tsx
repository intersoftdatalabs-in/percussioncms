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
      templateId: 7,
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
  links: [{ type: "local", targetId: "55" }],
};

function panelProps() {
  return {
    itemId: "42",
    loadCanvas: async () => canvas,
    loadLocal: async () => local,
    changeSnippetTemplate: vi.fn(),
    openRelatedItem: async () => true,
    loadAllowedTemplates: vi.fn().mockResolvedValue([
      { id: 7, name: "brief", label: "Brief" },
      { id: 4, name: "full", label: "Full" },
    ]),
    listFolder: vi.fn().mockResolvedValue([]),
    loadContentTypes: vi.fn().mockResolvedValue([{ name: "percPage", label: "Page" }]),
  };
}

describe("Editor related sub-dialogs Escape to cancel (#4532)", () => {
  afterEach(() => {
    cleanup();
  });

  it("Escape closes the change-template dialog and focus returns to its opener", async () => {
    render(<EditorRelatedContentPanel {...panelProps()} />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-change-template")).toBeTruthy();
    });
    const opener = screen.getByTestId("editor-related-change-template") as HTMLElement;
    fireEvent.click(opener);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-template-dialog")).toBeTruthy();
    });
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => {
      expect(screen.queryByTestId("editor-related-template-dialog")).toBeNull();
    });
    expect(document.activeElement).toBe(opener);
  });

  it("Escape closes the slot-move dialog and focus returns to its opener", async () => {
    render(<EditorRelatedContentPanel {...panelProps()} />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-move-slot")).toBeTruthy();
    });
    const opener = screen.getByTestId("editor-related-move-slot") as HTMLElement;
    fireEvent.click(opener);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-slot-move-dialog")).toBeTruthy();
    });
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => {
      expect(screen.queryByTestId("editor-related-slot-move-dialog")).toBeNull();
    });
    expect(document.activeElement).toBe(opener);
  });

  it("Escape closes the folder picker and focus returns to its opener", async () => {
    render(<EditorRelatedContentPanel {...panelProps()} />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-pick")).toBeTruthy();
    });
    const opener = screen.getByTestId("editor-related-pick") as HTMLElement;
    fireEvent.click(opener);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-pick-dialog")).toBeTruthy();
    });
    // Escape is ignored while the picker is busy (peer hook contract);
    // wait for the folder load like a real keypress would.
    await waitFor(() => {
      expect(screen.queryByTestId("editor-related-pick-loading")).toBeNull();
    });
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => {
      expect(screen.queryByTestId("editor-related-pick-dialog")).toBeNull();
    });
    expect(document.activeElement).toBe(opener);
  });

  it("Escape closes the quick-add dialog and focus returns to its opener", async () => {
    render(<EditorRelatedContentPanel {...panelProps()} />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-quick-add")).toBeTruthy();
    });
    const opener = screen.getByTestId("editor-related-quick-add") as HTMLElement;
    fireEvent.click(opener);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-quick-add-dialog")).toBeTruthy();
    });
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => {
      expect(screen.queryByTestId("editor-related-quick-add-dialog")).toBeNull();
    });
    expect(document.activeElement).toBe(opener);
  });
});

describe("Editor related sub-dialogs focus trap + accessible name (#6718)", () => {
  afterEach(() => {
    cleanup();
  });

  async function openTemplateDialog(): Promise<HTMLElement> {
    render(<EditorRelatedContentPanel {...panelProps()} />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-change-template")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-related-change-template"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-template-dialog")).toBeTruthy();
    });
    return screen.getByTestId("editor-related-template-dialog") as HTMLElement;
  }

  async function openSlotMoveDialog(): Promise<HTMLElement> {
    render(<EditorRelatedContentPanel {...panelProps()} />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-move-slot")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-related-move-slot"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-slot-move-dialog")).toBeTruthy();
    });
    return screen.getByTestId("editor-related-slot-move-dialog") as HTMLElement;
  }

  it("each sub-dialog exposes an accessible name", async () => {
    render(<EditorRelatedContentPanel {...panelProps()} />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-pick")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-related-pick"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-pick-dialog")).toBeTruthy();
    });
    expect(
      screen.getByRole("dialog", { name: "Choose a page or asset" }),
    ).toBeTruthy();
    cleanup();

    render(<EditorRelatedContentPanel {...panelProps()} />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-quick-add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-related-quick-add"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-quick-add-dialog")).toBeTruthy();
    });
    expect(
      screen.getByRole("dialog", { name: "Create and attach a related item" }),
    ).toBeTruthy();
    cleanup();

    await openTemplateDialog();
    expect(
      screen.getByRole("dialog", { name: "Change snippet template" }),
    ).toBeTruthy();
    cleanup();

    await openSlotMoveDialog();
    expect(
      screen.getByRole("dialog", { name: "Move to another slot" }),
    ).toBeTruthy();
  });

  it("opening the template dialog moves focus inside it", async () => {
    const dialog = await openTemplateDialog();
    await waitFor(() => {
      expect(
        dialog.contains(document.activeElement as Node | null),
      ).toBe(true);
    });
  });

  it("opening the slot-move dialog moves focus inside it", async () => {
    const dialog = await openSlotMoveDialog();
    await waitFor(() => {
      expect(
        dialog.contains(document.activeElement as Node | null),
      ).toBe(true);
    });
  });

  it("Tab on the last control wraps to the first, Shift+Tab wraps back", async () => {
    const dialog = await openTemplateDialog();
    await waitFor(() => {
      expect(
        dialog.contains(document.activeElement as Node | null),
      ).toBe(true);
    });
    const select = screen.getByTestId(
      "editor-related-template-select",
    ) as HTMLElement;
    const apply = screen.getByTestId(
      "editor-related-template-apply",
    ) as HTMLElement;
    apply.focus();
    fireEvent.keyDown(apply, { key: "Tab" });
    expect(document.activeElement).toBe(select);
    fireEvent.keyDown(select, { key: "Tab", shiftKey: true });
    expect(document.activeElement).toBe(apply);
  });
});
