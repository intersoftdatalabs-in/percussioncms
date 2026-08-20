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

import { cleanup, createEvent, fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { NavTree } from "../../../main/ts/architecture/NavTree";
import {
  FINDER_FOLDER_MIME,
  FINDER_PAGE_MIME,
  serializeFinderItemDrag,
} from "../../../main/ts/architecture/landingPageDrop";
import { ARCH_MSG_KEYS } from "../../../main/ts/architecture/messages";
import type { NavTreeNode } from "../../../main/ts/api/architecture/types";

function mockDataTransfer(map: Record<string, string>) {
  const store = new Map(Object.entries(map));
  return {
    dropEffect: "none",
    effectAllowed: "copy",
    get types() {
      return [...store.keys()];
    },
    setData(type: string, value: string) {
      store.set(type, value);
    },
    getData(type: string) {
      return store.get(type) ?? "";
    },
  };
}

const sampleRoot: NavTreeNode = {
  id: "root",
  title: "Home",
  folderPath: "//Sites/Demo",
  sectionType: "section",
  requiresLogin: false,
  children: [
    {
      id: "link-1",
      title: "Partner",
      folderPath: null,
      sectionType: "sectionlink",
      requiresLogin: false,
      children: [],
    },
    {
      id: "ext-1",
      title: "Docs",
      folderPath: null,
      sectionType: "externallink",
      requiresLogin: true,
      children: [],
    },
    {
      id: "blog-1",
      title: "News blog",
      folderPath: "//Sites/Demo/NewsBlog",
      sectionType: "blog",
      requiresLogin: false,
      children: [],
    },
  ],
};

/** RTL fireEvent returns !defaultPrevented; assert the event flag directly. */
function keyDownDefaultPrevented(el: HTMLElement, key: string): boolean {
  const ev = createEvent.keyDown(el, { key });
  fireEvent(el, ev);
  return ev.defaultPrevented;
}

describe("NavTree (#3095 / #3354)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  afterEach(() => {
    cleanup();
  });

  it("renders role=tree with treeitems, groups, and type badges", () => {
    render(<NavTree root={sampleRoot} />);
    const tree = screen.getByRole("tree");
    expect(tree).toBeTruthy();
    expect(screen.getByTestId("nav-tree-item-root")).toBeTruthy();
    expect(screen.getByTestId("nav-tree-group-root")).toBeTruthy();
    expect(screen.getByTestId("nav-tree-badge-link-1").textContent).toMatch(
      /section link/i,
    );
    expect(screen.getByTestId("nav-tree-badge-ext-1").textContent).toMatch(
      /external/i,
    );
    expect(screen.getByTestId("nav-tree-badge-blog-1").textContent).toMatch(
      /blog/i,
    );
    expect(
      screen.getByTestId("nav-tree-badge-blog-1").getAttribute("data-i18n-key"),
    ).toBe(ARCH_MSG_KEYS.TYPE_BLOG);
    expect(
      screen.getByTestId("nav-tree-item-blog-1").getAttribute("data-section-type"),
    ).toBe("blog");
    const secure = screen.getByTestId("nav-tree-secure-ext-1");
    expect(secure).toBeTruthy();
    expect(secure.getAttribute("title")).toMatch(/requires login/i);
    expect(secure.getAttribute("aria-label")).toMatch(/requires login/i);
    expect(secure.getAttribute("data-i18n-key")).toBe(ARCH_MSG_KEYS.SECURE_TITLE);
    expect(secure.getAttribute("data-i18n-badge-key")).toBe(
      ARCH_MSG_KEYS.SECURE_BADGE,
    );
    expect(secure.textContent).toMatch(/secure/i);
  });

  it("moves focus with arrow keys and toggles expand", () => {
    render(
      <NavTree root={sampleRoot} selectedId="root" onSelect={() => undefined} />,
    );
    const rootItem = screen.getByTestId("nav-tree-item-root");
    rootItem.focus();
    fireEvent.keyDown(rootItem, { key: "ArrowDown" });
    expect(document.activeElement?.getAttribute("data-testid")).toBe(
      "nav-tree-item-link-1",
    );
    fireEvent.keyDown(screen.getByTestId("nav-tree-item-link-1"), {
      key: "ArrowUp",
    });
    expect(document.activeElement?.getAttribute("data-testid")).toBe(
      "nav-tree-item-root",
    );
    fireEvent.keyDown(rootItem, { key: "ArrowLeft" });
    expect(screen.queryByTestId("nav-tree-item-link-1")).toBeNull();
  });

  it("selects with Enter/Space via handleKeyDown and toggles branches", () => {
    const onSelect = vi.fn();
    render(
      <NavTree root={sampleRoot} selectedId="root" onSelect={onSelect} />,
    );
    const leaf = screen.getByTestId("nav-tree-item-link-1");
    leaf.focus();
    expect(keyDownDefaultPrevented(leaf, "Enter")).toBe(true);
    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ id: "link-1" }),
    );
    expect(screen.getByTestId("nav-tree-item-link-1")).toBeTruthy();

    onSelect.mockClear();
    expect(keyDownDefaultPrevented(leaf, " ")).toBe(true);
    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ id: "link-1" }),
    );

    onSelect.mockClear();
    const rootItem = screen.getByTestId("nav-tree-item-root");
    rootItem.focus();
    expect(keyDownDefaultPrevented(rootItem, "Enter")).toBe(true);
    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ id: "root" }),
    );
    expect(screen.queryByTestId("nav-tree-item-link-1")).toBeNull();

    onSelect.mockClear();
    expect(keyDownDefaultPrevented(rootItem, " ")).toBe(true);
    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ id: "root" }),
    );
    expect(screen.getByTestId("nav-tree-item-link-1")).toBeTruthy();
  });

  it("moves focus with Home/End and Arrow Right into a child", () => {
    render(
      <NavTree root={sampleRoot} selectedId="root" onSelect={() => undefined} />,
    );
    const rootItem = screen.getByTestId("nav-tree-item-root");
    rootItem.focus();
    fireEvent.keyDown(rootItem, { key: "End" });
    expect(document.activeElement?.getAttribute("data-testid")).toBe(
      "nav-tree-item-blog-1",
    );
    fireEvent.keyDown(screen.getByTestId("nav-tree-item-blog-1"), {
      key: "Home",
    });
    expect(document.activeElement?.getAttribute("data-testid")).toBe(
      "nav-tree-item-root",
    );
    fireEvent.keyDown(screen.getByTestId("nav-tree-item-root"), {
      key: "ArrowRight",
    });
    expect(document.activeElement?.getAttribute("data-testid")).toBe(
      "nav-tree-item-link-1",
    );
    expect(screen.getByTestId("nav-tree-item-root").getAttribute("tabindex")).toBe(
      "-1",
    );
    expect(screen.getByTestId("nav-tree-item-link-1").getAttribute("tabindex")).toBe(
      "0",
    );
  });

  it("does not trap Tab (default is not prevented)", () => {
    render(
      <NavTree root={sampleRoot} selectedId="root" onSelect={() => undefined} />,
    );
    const rootItem = screen.getByTestId("nav-tree-item-root");
    rootItem.focus();
    expect(keyDownDefaultPrevented(rootItem, "Tab")).toBe(false);
    expect(keyDownDefaultPrevented(rootItem, "ArrowDown")).toBe(true);
  });

  it("reports selection and collapses via toggle", () => {
    const onSelect = vi.fn();
    render(
      <NavTree root={sampleRoot} selectedId="root" onSelect={onSelect} />,
    );
    fireEvent.click(screen.getByTestId("nav-tree-item-link-1"));
    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ id: "link-1" }),
    );
    // Collapse root
    fireEvent.click(screen.getByTestId("nav-tree-toggle-root"));
    expect(screen.queryByTestId("nav-tree-item-link-1")).toBeNull();
  });

  it("renders role=tree with one treeitem for a root-only nav (#3352)", () => {
    const rootOnly: NavTreeNode = {
      id: "root-only",
      title: "Corporate_Investments",
      folderPath: "//Sites/CorporateInvestments",
      sectionType: "section",
      requiresLogin: false,
      children: [],
    };
    render(<NavTree root={rootOnly} />);
    expect(screen.getByRole("tree")).toBeTruthy();
    expect(screen.getByTestId("nav-tree-item-root-only")).toBeTruthy();
    expect(screen.queryByTestId("architecture-nav-tree-empty")).toBeNull();
  });

  it("shows loading, error, and empty states without role=tree", () => {
    const { rerender } = render(<NavTree root={null} loading />);
    expect(screen.getByTestId("architecture-nav-tree-loading")).toBeTruthy();
    expect(screen.queryByRole("tree")).toBeNull();

    rerender(<NavTree root={null} error="Boom" />);
    expect(screen.getByTestId("architecture-nav-tree-error").textContent).toBe(
      "Boom",
    );
    expect(screen.queryByRole("tree")).toBeNull();

    rerender(<NavTree root={null} />);
    expect(screen.getByTestId("architecture-nav-tree-empty")).toBeTruthy();
    expect(
      screen.getByTestId("architecture-nav-tree-empty-title").textContent,
    ).toMatch(/no navigation tree/i);
    expect(screen.getByTestId("architecture-nav-tree-empty-hint")).toBeTruthy();
    expect(screen.queryByRole("tree")).toBeNull();
  });

  it("drops a Finder page onto a regular section and ignores folders (#3660)", () => {
    const onDrop = vi.fn();
    const onSelect = vi.fn();
    render(
      <NavTree
        root={sampleRoot}
        selectedId="root"
        selectedSite="Demo"
        onSelect={onSelect}
        onLandingPageDrop={onDrop}
      />,
    );
    const rootItem = screen.getByTestId("nav-tree-item-root");
    expect(rootItem.getAttribute("data-landing-drop")).toBe("true");
    expect(
      screen.getByTestId("nav-tree-item-link-1").getAttribute("data-landing-drop"),
    ).toBe("false");

    fireEvent.drop(rootItem, {
      dataTransfer: mockDataTransfer({
        [FINDER_PAGE_MIME]: serializeFinderItemDrag({
          id: "page-9",
          name: "Picked",
          path: "//Sites/Demo/Picked",
          type: "page",
        }),
      }),
    });
    expect(onDrop).toHaveBeenCalledWith({
      sectionId: "root",
      pageId: "page-9",
      pageLabel: "Picked",
    });
    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ id: "root" }),
    );

    onDrop.mockClear();
    fireEvent.drop(rootItem, {
      dataTransfer: mockDataTransfer({
        [FINDER_FOLDER_MIME]: serializeFinderItemDrag({
          id: "f1",
          name: "Folder",
          type: "folder",
        }),
      }),
    });
    expect(onDrop).not.toHaveBeenCalled();

    onDrop.mockClear();
    fireEvent.drop(screen.getByTestId("nav-tree-item-blog-1"), {
      dataTransfer: mockDataTransfer({
        [FINDER_PAGE_MIME]: serializeFinderItemDrag({
          id: "page-9",
          type: "page",
        }),
      }),
    });
    expect(onDrop).not.toHaveBeenCalled();
  });

  it("Escape during dragover does not fire a landing drop (#3660)", () => {
    const onDrop = vi.fn();
    render(
      <NavTree
        root={sampleRoot}
        selectedId="root"
        onLandingPageDrop={onDrop}
      />,
    );
    const rootItem = screen.getByTestId("nav-tree-item-root");
    fireEvent.dragOver(rootItem, {
      dataTransfer: mockDataTransfer({
        [FINDER_PAGE_MIME]: "",
      }),
    });
    expect(rootItem.getAttribute("data-drop-active")).toBe("true");
    fireEvent.keyDown(rootItem, { key: "Escape" });
    expect(rootItem.getAttribute("data-drop-active")).not.toBe("true");
    expect(onDrop).not.toHaveBeenCalled();
  });
});
