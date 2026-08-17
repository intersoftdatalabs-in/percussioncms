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
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AssemblyHost } from "../../../main/ts/assembly/AssemblyHost";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";

vi.mock("../../../main/ts/contentBrowser/ContentBrowser", () => ({
  ContentBrowser: (props: {
    onConfirm?: (selection: {
      items: Array<{ id: string; name: string; path: string }>;
    }) => void;
    onCancel?: () => void;
  }) => (
    <div data-testid="content-browser">
      <button
        type="button"
        data-testid="content-browser-confirm"
        onClick={() =>
          props.onConfirm?.({
            items: [{ id: "7", name: "Snippet", path: "/Sites/A/s" }],
          })
        }
      >
        Confirm
      </button>
      <button
        type="button"
        data-testid="content-browser-cancel"
        onClick={() => props.onCancel?.()}
      >
        Cancel
      </button>
    </div>
  ),
}));

function renderHost(
  props: React.ComponentProps<typeof AssemblyHost> = {},
): void {
  render(
    <MemoryRouter initialEntries={["/assembly?contentId=42&templateId=7"]}>
      <Routes>
        <Route path="/assembly" element={<AssemblyHost {...props} />} />
      </Routes>
    </MemoryRouter>,
  );
}

function slotHostProps() {
  return {
    fetchPreview: vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
      contentId: 42,
      templateId: 7,
      revision: 1,
    }),
    loadTemplates: vi.fn().mockResolvedValue([
      {
        name: "rffPgGeneric",
        label: "Generic Page",
        url: "../assembler/render?sys_template=7",
        sortRank: 0,
        menuType: "MENUITEM",
      } satisfies MenuAction,
    ]),
    loadCanvas: vi.fn().mockResolvedValue({
      ownerId: 42,
      templateId: 7,
      slots: [
        {
          slotId: 3,
          name: "sidebar",
          label: "Sidebar",
          items: [],
        },
      ],
    }),
    loadAllowedTemplates: vi.fn().mockResolvedValue([
      { id: 4, name: "rffSnTitle", label: "Title" },
    ]),
  };
}

describe("AssemblyHost slot add (#3495)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("picker cancel does not POST a slot relationship", async () => {
    const addToSlot = vi.fn();
    renderHost({ ...slotHostProps(), addToSlot });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-3")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("assembly-slot-3"));
    fireEvent.click(screen.getByTestId("assembly-slot-add"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-add-dialog")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("content-browser-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("assembly-slot-add-dialog")).toBeNull();
    });
    expect(addToSlot).not.toHaveBeenCalled();
  });

  it("successful pick calls add with slot owner and template ids", async () => {
    const addToSlot = vi.fn().mockResolvedValue({
      relationshipId: 9,
      ownerId: 42,
      dependentId: 7,
      slotId: 3,
      templateId: 4,
      sortRank: 0,
    });
    const props = slotHostProps();
    renderHost({ ...props, addToSlot });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-3")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("assembly-slot-3"));
    fireEvent.click(screen.getByTestId("assembly-slot-add"));
    await waitFor(() => {
      expect(screen.getByTestId("content-browser-confirm")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("content-browser-confirm"));
    await waitFor(() => {
      expect(addToSlot).toHaveBeenCalledWith({
        ownerId: 42,
        dependentId: 7,
        slotId: 3,
        templateId: 4,
        folderId: undefined,
      });
    });
    expect(props.loadAllowedTemplates).toHaveBeenCalledWith(3, null);
  });
});
