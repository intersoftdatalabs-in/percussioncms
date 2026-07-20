/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import { ContextMenu } from "../../../main/ts/contentExplorer/ContextMenu";

const ACTIONS: MenuAction[] = [
  { name: "open", label: "Open", sortRank: 10, menuType: "MENUITEM" },
  { name: "delete", label: "Delete", sortRank: 20, menuType: "MENUITEM" },
  { name: "edit", label: "Edit", sortRank: 5, menuType: "MENUITEM" },
];

/**
 * Vanilla DOM assertions (no jest-dom matchers) — kept portable so this
 * test runs identically whether vitest.setup.ts auto-loads
 * @testing-library/jest-dom or not (see WebUI/src/test/ts/setup.ts note
 * in the b013222f14 commit).
 */
function isInDocument(el: HTMLElement | null): boolean {
  return el !== null && document.body.contains(el);
}

describe("ContextMenu", () => {
  it("renders one menuitem per action with the action label", () => {
    render(<ContextMenu actions={ACTIONS} />);
    const menu = screen.getByRole("menu");
    expect(menu).toBeTruthy();
    expect(menu.getAttribute("data-testid")).toBe("context-menu");
    expect(screen.getByTestId("context-menu-item-open").textContent).toBe(
      "Open",
    );
    expect(screen.getByTestId("context-menu-item-edit").textContent).toBe(
      "Edit",
    );
    expect(screen.getByTestId("context-menu-item-delete").textContent).toBe(
      "Delete",
    );
  });

  it("uses an empty-state element when there are no actions", () => {
    render(<ContextMenu actions={[]} />);
    expect(screen.getByTestId("context-menu-empty")).toBeTruthy();
  });

  it("clicking a leaf action invokes onInvoke with the action name", () => {
    const onInvoke = vi.fn();
    render(<ContextMenu actions={ACTIONS} onInvoke={onInvoke} />);
    fireEvent.click(screen.getByTestId("context-menu-item-delete"));
    expect(onInvoke).toHaveBeenCalledTimes(1);
    expect(onInvoke.mock.calls[0]?.[0]).toBe("delete");
  });

  it("Escape key fires onClose", () => {
    const onClose = vi.fn();
    render(<ContextMenu actions={ACTIONS} onClose={onClose} />);
    fireEvent.keyDown(screen.getByRole("menu"), { key: "Escape" });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("uses the ariaLabel prop when supplied", () => {
    render(<ContextMenu actions={ACTIONS} ariaLabel="Item actions" />);
    expect(screen.getByRole("menu").getAttribute("aria-label")).toBe(
      "Item actions",
    );
  });

  it("defaults aria-label to 'Context menu' when not supplied", () => {
    render(<ContextMenu actions={ACTIONS} />);
    expect(screen.getByRole("menu").getAttribute("aria-label")).toBe(
      "Context menu",
    );
  });

  it("renders the menu in the document (vanilla DOM check)", () => {
    render(<ContextMenu actions={ACTIONS} />);
    expect(isInDocument(screen.getByRole("menu"))).toBe(true);
  });
});
