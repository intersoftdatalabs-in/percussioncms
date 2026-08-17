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
    loadAllowedTypes: vi.fn().mockResolvedValue([
      { id: 1, name: "percRichText", label: "Rich Text" },
    ]),
    loadAllowedTemplates: vi.fn().mockResolvedValue([
      { id: 4, name: "rffSnTitle", label: "Title" },
    ]),
  };
}

describe("AssemblyHost slot create (#3497)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("picker cancel does not create or POST a slot relationship", async () => {
    const addToSlot = vi.fn();
    const createItem = vi.fn();
    const openWindow = vi.fn();
    renderHost({ ...slotHostProps(), addToSlot, createItem, openWindow });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-3")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("assembly-slot-3"));
    fireEvent.click(screen.getByTestId("assembly-slot-create"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-create-dialog")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("assembly-slot-create-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("assembly-slot-create-dialog")).toBeNull();
    });
    expect(createItem).not.toHaveBeenCalled();
    expect(addToSlot).not.toHaveBeenCalled();
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("apply creates via itemmanagement, adds the relationship, and opens the React editor", async () => {
    const addToSlot = vi.fn().mockResolvedValue({
      relationshipId: 9,
      ownerId: 42,
      dependentId: 99,
      slotId: 3,
      templateId: 4,
      sortRank: 0,
    });
    const createItem = vi.fn().mockResolvedValue({
      itemId: "99",
      folderPath: "/Sites/Demo",
      name: "n",
      contentType: "percRichText",
    });
    const openWindow = vi.fn();
    const props = slotHostProps();
    renderHost({ ...props, addToSlot, createItem, openWindow });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-3")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("assembly-slot-3"));
    fireEvent.click(screen.getByTestId("assembly-slot-create"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-create-apply")).toBeTruthy();
      expect(
        (screen.getByTestId("assembly-slot-create-type") as HTMLSelectElement)
          .value,
      ).toBe("percRichText");
    });
    fireEvent.change(screen.getByTestId("assembly-slot-create-folder"), {
      target: { value: "/Sites/Demo" },
    });
    fireEvent.click(screen.getByTestId("assembly-slot-create-apply"));
    await waitFor(() => {
      expect(createItem).toHaveBeenCalledWith({
        contentType: "percRichText",
        folderPath: "/Sites/Demo",
        templateId: undefined,
      });
    });
    expect(addToSlot).toHaveBeenCalledWith({
      ownerId: 42,
      dependentId: 99,
      slotId: 3,
      templateId: 4,
    });
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain("entry=editor");
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain("contentId=99");
    expect(props.loadAllowedTypes).toHaveBeenCalledWith(3);
    expect(props.loadAllowedTemplates).toHaveBeenCalledWith(3);
  });
});
