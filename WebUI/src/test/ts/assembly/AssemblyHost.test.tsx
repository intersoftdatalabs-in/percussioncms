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
import { AppRoutes } from "../../../main/ts/app/routes";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";

function renderHost(
  search: string,
  props: React.ComponentProps<typeof AssemblyHost> = {},
): void {
  render(
    <MemoryRouter initialEntries={[`/assembly${search}`]}>
      <Routes>
        <Route path="/assembly" element={<AssemblyHost {...props} />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("AssemblyHost", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("asks for an item when contentId is missing", () => {
    renderHost("");
    expect(screen.getByTestId("assembly-overlay")).toBeTruthy();
    expect(screen.getByTestId("assembly-error").textContent).toMatch(
      /content item/i,
    );
    expect(screen.queryByTestId("assembly-preview-frame")).toBeNull();
  });

  it("loads the assembled preview for a page/snippet template", async () => {
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
      contentId: 42,
      templateId: 7,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffPgGeneric",
        label: "Generic Page",
        url: "../assembler/render?sys_template=7",
        sortRank: 0,
        menuType: "MENUITEM",
      } satisfies MenuAction,
    ]);
    renderHost("?contentId=42&templateId=7", { fetchPreview, loadTemplates });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-preview-frame")).toBeTruthy();
    });
    expect(fetchPreview).toHaveBeenCalledWith(42, 7);
    expect(screen.getByTestId("assembly-content-id").textContent).toContain("42");
    const frame = screen.getByTestId("assembly-preview-frame");
    expect(frame.getAttribute("src")).toContain("/assembler/render");
    expect(frame.getAttribute("src")).toContain("sys_template=7");
  });

  it("picks the first AA template when the query omits templateId", async () => {
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=9&sys_template=3",
      contentId: 9,
      templateId: 3,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffSnTitle",
        label: "Title snippet",
        sortRank: 0,
        menuType: "MENUITEM",
        parameters: [{ name: "sys_template", value: "3" }],
      } satisfies MenuAction,
    ]);
    renderHost("?contentId=9", { fetchPreview, loadTemplates });
    await waitFor(() => {
      expect(fetchPreview).toHaveBeenCalledWith(9, 3);
    });
    expect(screen.getByTestId("assembly-template-select")).toBeTruthy();
  });

  it("shows an error when template load fails even with a requested templateId", async () => {
    const fetchPreview = vi.fn();
    const loadTemplates = vi.fn().mockRejectedValue(new Error("catalog down"));
    renderHost("?contentId=42&templateId=7", { fetchPreview, loadTemplates });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-error").textContent).toMatch(
        /no page or snippet template/i,
      );
    });
    expect(fetchPreview).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-preview-frame")).toBeNull();
  });

  it("does not silently fall back when requestedTemplateId is not in options", async () => {
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=3",
      contentId: 42,
      templateId: 3,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffSnTitle",
        label: "Title snippet",
        sortRank: 0,
        menuType: "MENUITEM",
        parameters: [{ name: "sys_template", value: "3" }],
      } satisfies MenuAction,
    ]);
    renderHost("?contentId=42&templateId=99", { fetchPreview, loadTemplates });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-error").textContent).toMatch(
        /not in the available list/i,
      );
    });
    expect(fetchPreview).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-preview-frame")).toBeNull();
    expect(screen.getByTestId("assembly-requested-template").textContent).toContain(
      "99",
    );
  });

  it("AppRoutes mounts assembly outside AppLayout", () => {
    render(
      <MemoryRouter initialEntries={["/assembly"]}>
        <AppRoutes />
      </MemoryRouter>,
    );
    expect(screen.getByTestId("assembly-host")).toBeTruthy();
    expect(screen.queryByTestId("perc-spa-app")).toBeNull();
  });

  it("renders slot add/create/arrange once a slot is selected", async () => {
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
      contentId: 42,
      templateId: 7,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffPgGeneric",
        label: "Generic Page",
        url: "../assembler/render?sys_template=7",
        sortRank: 0,
        menuType: "MENUITEM",
      } satisfies MenuAction,
    ]);
    const loadCanvas = vi.fn().mockResolvedValue({
      ownerId: 42,
      templateId: 7,
      slots: [
        {
          slotId: 3,
          name: "sidebar",
          label: "Sidebar",
          items: [
            {
              relationshipId: 88,
              ownerId: 42,
              dependentId: 7,
              slotId: 3,
              templateId: 4,
              sortRank: 0,
            },
          ],
        },
      ],
    });
    const removeSlotRel = vi.fn().mockResolvedValue(undefined);
    renderHost("?contentId=42&templateId=7", {
      fetchPreview,
      loadTemplates,
      loadCanvas,
      removeSlotRel,
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-3")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("assembly-slot-3"));
    expect(screen.getByTestId("assembly-slot-add")).not.toHaveProperty(
      "disabled",
      true,
    );
    fireEvent.click(screen.getByTestId("assembly-slot-item-88"));
    fireEvent.click(screen.getByTestId("assembly-slot-remove"));
    await waitFor(() => {
      expect(removeSlotRel).toHaveBeenCalledWith(88);
    });
  });
});
