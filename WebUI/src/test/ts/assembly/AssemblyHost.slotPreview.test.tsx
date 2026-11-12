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
  ContentBrowser: () => <div data-testid="content-browser" />,
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

function slotHostProps(templateId: number) {
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
          items: [
            {
              relationshipId: 9,
              ownerId: 42,
              dependentId: 55,
              slotId: 3,
              templateId,
              sortRank: 0,
            },
          ],
        },
      ],
    }),
    loadAllowedTemplates: vi.fn().mockResolvedValue([]),
  };
}

async function selectSlotItem(): Promise<void> {
  await waitFor(() => {
    expect(screen.getByTestId("assembly-slot-3")).toBeTruthy();
  });
  fireEvent.click(screen.getByTestId("assembly-slot-3"));
  await waitFor(() => {
    expect(screen.getByTestId("assembly-slot-item-9")).toBeTruthy();
  });
  fireEvent.click(screen.getByTestId("assembly-slot-item-9"));
}

describe("AssemblyHost slot snippet preview (#6224)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("previews the selected snippet through preview-location", async () => {
    const props = slotHostProps(4);
    const openWindow = vi.fn().mockReturnValue(null);
    renderHost({ ...props, openWindow });
    await selectSlotItem();
    const preview = screen.getByTestId("assembly-slot-preview");
    expect((preview as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(preview);
    await waitFor(() => {
      expect(props.fetchPreview).toHaveBeenCalledWith(55, 4);
    });
    expect(openWindow).toHaveBeenCalledWith(
      expect.stringContaining("/assembler/render"),
      "percSnippetPreview_55",
    );
    expect(screen.queryByTestId("assembly-slot-notice")).toBeNull();
  });

  it("a failed preview stays on the canvas as a notice", async () => {
    const props = slotHostProps(4);
    renderHost({
      ...props,
      fetchPreview: vi
        .fn()
        .mockResolvedValueOnce({
          previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
          contentId: 42,
          templateId: 7,
          revision: 1,
        })
        .mockRejectedValueOnce(new Error("nope")),
      openWindow: vi.fn().mockReturnValue(null),
    });
    await selectSlotItem();
    fireEvent.click(screen.getByTestId("assembly-slot-preview"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-notice")).toBeTruthy();
    });
    expect(screen.getByTestId("assembly-slot-notice").textContent).toMatch(
      /snippet preview/i,
    );
  });

  it("disables preview when the selected item has no snippet template", async () => {
    const props = slotHostProps(0);
    const openWindow = vi.fn().mockReturnValue(null);
    renderHost({ ...props, openWindow });
    await selectSlotItem();
    expect(
      (screen.getByTestId("assembly-slot-preview") as HTMLButtonElement)
        .disabled,
    ).toBe(true);
    expect(openWindow).not.toHaveBeenCalled();
  });
});
