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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import {
  ActionToolbar,
  slotActionDisabledReason,
} from "../../../main/ts/contentExplorer/ActionToolbar";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import {
  ActionIcon,
  normalizeActionKey,
  resolveActionIcon,
} from "../../../main/ts/contentExplorer/actionIcons";
import { renderA11yGate } from "./a11y";

const ACTIONS: MenuAction[] = [
  { name: "open", label: "Open", sortRank: 0, menuType: "MENUITEM" },
  { name: "edit", label: "Edit", sortRank: 1, menuType: "MENUITEM" },
];

describe("ActionToolbar", () => {
  it("renders a button per action with aria-label", () => {
    render(<ActionToolbar actions={ACTIONS} />);
    const toolbar = screen.getByRole("toolbar");
    expect(toolbar).toBeTruthy();
    expect(screen.getByLabelText("Open")).toBeTruthy();
    expect(screen.getByLabelText("Edit")).toBeTruthy();
  });

  it("clicking a button invokes onInvoke with the action name", () => {
    const onInvoke = vi.fn();
    render(<ActionToolbar actions={ACTIONS} onInvoke={onInvoke} />);
    fireEvent.click(screen.getByLabelText("Edit"));
    expect(onInvoke).toHaveBeenCalledTimes(1);
    expect(onInvoke.mock.calls[0]?.[0]).toBe("edit");
  });

  it("uses an empty-state placeholder when there are no actions", () => {
    render(<ActionToolbar actions={[]} />);
    expect(screen.getByTestId("action-toolbar-empty")).toBeTruthy();
  });

  it("uses ariaLabel on the toolbar role when supplied", () => {
    render(<ActionToolbar actions={ACTIONS} ariaLabel="Item actions" />);
    expect(screen.getByRole("toolbar").getAttribute("aria-label")).toBe(
      "Item actions",
    );
  });

  it("passes the zero serious/critical axe-core gate (populated state)", async () => {
    const { container } = render(<ActionToolbar actions={ACTIONS} ariaLabel="Item actions" />);
    await renderA11yGate(container);
  });

  it("passes the zero serious/critical axe-core gate (empty state)", async () => {
    const { container } = render(<ActionToolbar actions={[]} ariaLabel="Item actions" />);
    await renderA11yGate(container);
  });

  it("renders MENU parents with children as nested dropdowns (#2730/#2731)", () => {
    const nested: MenuAction[] = [
      {
        name: "new",
        label: "New",
        sortRank: 0,
        menuType: "MENU",
        children: [
          {
            name: "new-folder",
            label: "Folder",
            sortRank: 0,
            menuType: "MENUITEM",
          },
          {
            name: "new-page",
            label: "Page",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ],
      },
      { name: "open", label: "Open", sortRank: 1, menuType: "MENUITEM" },
    ];
    const onInvoke = vi.fn();
    render(<ActionToolbar actions={nested} onInvoke={onInvoke} />);

    // Parent is a single toolbar control — not three flat buttons.
    expect(screen.getByTestId("action-toolbar-item-new")).toBeTruthy();
    expect(
      screen.getByTestId("action-toolbar-item-new").getAttribute("aria-haspopup"),
    ).toBe("menu");
    expect(screen.queryByTestId("action-toolbar-item-new-folder")).toBeNull();
    expect(screen.getByTestId("action-toolbar-item-open")).toBeTruthy();
    // Only two top-level controls (New dropdown + Open), not three flat buttons.
    expect(screen.getAllByRole("button").length).toBe(2);

    fireEvent.click(screen.getByTestId("action-toolbar-item-new"));
    expect(screen.getByTestId("action-toolbar-menu-new")).toBeTruthy();
    fireEvent.click(screen.getByTestId("action-toolbar-item-new-folder"));
    expect(onInvoke).toHaveBeenCalledWith(
      "new-folder",
      expect.objectContaining({ name: "new-folder", parentName: "new" }),
    );
  });

  it("keeps an open menu when a refresh still includes that parent (#5006)", () => {
    const menu = (label: string): MenuAction[] => [
      {
        name: "Create",
        label: "Create",
        sortRank: 0,
        menuType: "MENU",
        children: [
          {
            name: "Workflow_NewVersion",
            label,
            sortRank: 0,
            menuType: "MENUITEM",
          },
        ],
      },
    ];
    const onInvoke = vi.fn();
    const view = render(
      <ActionToolbar actions={menu("New Copy")} onInvoke={onInvoke} />,
    );
    fireEvent.click(screen.getByTestId("action-toolbar-item-Create"));
    expect(screen.getByTestId("action-toolbar-item-Workflow_NewVersion")).toBeTruthy();
    view.rerender(<ActionToolbar actions={menu("New Copy")} onInvoke={onInvoke} />);
    expect(screen.getByTestId("action-toolbar-menu-Create")).toBeTruthy();
    fireEvent.click(screen.getByTestId("action-toolbar-item-Workflow_NewVersion"));
    expect(onInvoke).toHaveBeenCalledWith(
      "Workflow_NewVersion",
      expect.objectContaining({ name: "Workflow_NewVersion", parentName: "Create" }),
    );
  });

  it("returns focus to the parent button on Escape (#6824)", () => {
    const nested: MenuAction[] = [
      {
        name: "new",
        label: "New",
        sortRank: 0,
        menuType: "MENU",
        children: [
          {
            name: "new-folder",
            label: "Folder",
            sortRank: 0,
            menuType: "MENUITEM",
          },
        ],
      },
    ];
    render(<ActionToolbar actions={nested} />);
    const parent = screen.getByTestId("action-toolbar-item-new");
    fireEvent.click(parent);
    expect(screen.getByTestId("action-toolbar-menu-new")).toBeTruthy();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.queryByTestId("action-toolbar-menu-new")).toBeNull();
    expect(document.activeElement).toBe(parent);
  });

  it("does not dump multi-level MENU grandchildren as top-level toolbar buttons (#2730)", () => {
    const nested: MenuAction[] = [
      {
        name: "content",
        label: "Content",
        sortRank: 0,
        menuType: "MENU",
        children: [
          {
            name: "new",
            label: "New",
            sortRank: 0,
            menuType: "MENU",
            children: [
              {
                name: "new-folder",
                label: "Folder",
                sortRank: 0,
                menuType: "MENUITEM",
              },
            ],
          },
        ],
      },
    ];
    render(<ActionToolbar actions={nested} />);
    expect(screen.getByTestId("action-toolbar-item-content")).toBeTruthy();
    expect(screen.queryByTestId("action-toolbar-item-new")).toBeNull();
    expect(screen.queryByTestId("action-toolbar-item-new-folder")).toBeNull();
  });

  it("collapses dumped MENU children so they are not top-level buttons (#3560)", () => {
    const dumped: MenuAction[] = [
      {
        name: "Paste",
        label: "Paste",
        sortRank: 0,
        menuType: "MENU",
        children: [
          { name: "Move", label: "Move", sortRank: 0, menuType: "MENUITEM" },
          {
            name: "Paste_As_Link",
            label: "Paste as Link",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ],
      },
      { name: "Move", label: "Move", sortRank: 1, menuType: "MENUITEM" },
      {
        name: "Paste_As_Link",
        label: "Paste as Link",
        sortRank: 2,
        menuType: "MENUITEM",
      },
      { name: "Open", label: "Open", sortRank: 3, menuType: "MENUITEM" },
    ];
    render(<ActionToolbar actions={dumped} />);
    expect(
      screen.getByTestId("action-toolbar-item-Paste").getAttribute(
        "aria-haspopup",
      ),
    ).toBe("menu");
    expect(screen.queryByTestId("action-toolbar-item-Move")).toBeNull();
    expect(screen.queryByTestId("action-toolbar-item-Paste_As_Link")).toBeNull();
    expect(screen.getByTestId("action-toolbar-item-Open")).toBeTruthy();
    expect(screen.getAllByRole("button").length).toBe(2);
  });

  it("unwraps envelope children so static MENU parents stay dropdowns (#3560)", () => {
    const envelope = {
      name: "Arrange",
      label: "Arrange",
      sortRank: 0,
      menuType: "MENU",
      children: {
        ActionMenuList: [
          {
            name: "Arrange_MoveUpLeft",
            label: "Move up",
            sortRank: 0,
            menuType: "MENUITEM",
          },
        ],
      },
    } as unknown as MenuAction;
    render(<ActionToolbar actions={[envelope]} />);
    expect(
      screen.getByTestId("action-toolbar-item-Arrange").getAttribute(
        "aria-haspopup",
      ),
    ).toBe("menu");
    expect(
      screen.queryByTestId("action-toolbar-item-Arrange_MoveUpLeft"),
    ).toBeNull();
    fireEvent.click(screen.getByTestId("action-toolbar-item-Arrange"));
    expect(screen.getByTestId("action-toolbar-menu-Arrange")).toBeTruthy();
    expect(
      screen.getByTestId("action-toolbar-item-Arrange_MoveUpLeft"),
    ).toBeTruthy();
  });

  it("passes the zero serious/critical axe-core gate (icon+label populated state)", async () => {
    const { container } = render(
      <ActionToolbar
        actions={[
          { name: "Open", label: "Open", sortRank: 0, menuType: "MENUITEM" },
          { name: "Checkout", label: "Check out", sortRank: 1, menuType: "MENUITEM" },
          { name: "Publish_Now", label: "Publish now", sortRank: 2, menuType: "MENUITEM" },
        ]}
        ariaLabel="Item actions"
      />,
    );
    await renderA11yGate(container);
  });

  it("renders cascading Workflow children as a labeled group (#2732)", () => {
    const onInvoke = vi.fn();
    const actions: MenuAction[] = [
      {
        name: "workflow",
        label: "Workflow",
        sortRank: 9000,
        menuType: "MENU",
        children: [
          {
            name: "workflow-transition:Submit",
            label: "Submit",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ],
      },
    ];
    render(<ActionToolbar actions={actions} onInvoke={onInvoke} />);
    expect(screen.getByTestId("action-toolbar-group-workflow")).toBeTruthy();
    fireEvent.click(screen.getByTestId("action-toolbar-item-workflow-transition:Submit"));
    expect(onInvoke).toHaveBeenCalledWith(
      "workflow-transition:Submit",
      expect.objectContaining({ label: "Submit" }),
    );
  });

  it("renders icon+label affordances for mapped actions (#6515)", () => {
    const { container } = render(<ActionToolbar actions={ACTIONS} />);
    // Labels stay (i18n/a11y unchanged); each mapped button gains one
    // decorative, aria-hidden icon next to its label.
    for (const action of ACTIONS) {
      const button = screen.getByTestId(`action-toolbar-item-${action.name}`);
      expect(button.textContent).toContain(action.label);
      const icon = button.querySelector('[data-testid="action-toolbar-icon"]');
      expect(icon, `${action.name} renders an icon`).toBeTruthy();
      expect(icon?.querySelector("svg")).toBeTruthy();
    }
    // No icon escapes the button chrome.
    expect(
      container.querySelectorAll('[data-testid="action-toolbar-icon"]').length,
    ).toBe(ACTIONS.length);
  });

  it("keeps labels when the action has no icon mapping (#6515)", () => {
    const unmapped: MenuAction[] = [
      {
        name: "lifecycle_analysis",
        label: "Lifecycle analysis",
        sortRank: 0,
        menuType: "MENUITEM",
      },
    ];
    render(<ActionToolbar actions={unmapped} />);
    const button = screen.getByTestId("action-toolbar-item-lifecycle_analysis");
    expect(button.textContent).toContain("Lifecycle analysis");
    expect(
      button.querySelector('[data-testid="action-toolbar-icon"]'),
    ).toBeNull();
  });

  it("renders icons inside MENU dropdown children and the Workflow group (#6515)", () => {
    const nested: MenuAction[] = [
      {
        name: "Paste",
        label: "Paste",
        sortRank: 0,
        menuType: "MENU",
        children: [
          {
            name: "Paste_As_Link",
            label: "Paste as Link",
            sortRank: 0,
            menuType: "MENUITEM",
          },
        ],
      },
      {
        name: "workflow",
        label: "Workflow",
        sortRank: 9000,
        menuType: "MENU",
        children: [
          {
            name: "workflow-transition:Approve",
            label: "Approve",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ],
      },
    ];
    render(<ActionToolbar actions={nested} />);
    // MENU parent carries its own icon next to the label + caret.
    const parent = screen.getByTestId("action-toolbar-item-Paste");
    expect(parent.querySelector('[data-testid="action-toolbar-icon"]')).toBeTruthy();
    fireEvent.click(parent);
    const child = screen.getByTestId("action-toolbar-item-Paste_As_Link");
    expect(child.querySelector('[data-testid="action-toolbar-icon"]')).toBeTruthy();
    const transition = screen.getByTestId(
      "action-toolbar-item-workflow-transition:Approve",
    );
    expect(
      transition.querySelector('[data-testid="action-toolbar-icon"]'),
    ).toBeTruthy();
  });

  it("shows a keyboard focus ring on icon buttons (#6515)", () => {
    render(<ActionToolbar actions={ACTIONS} />);
    const button = screen.getByTestId(
      "action-toolbar-item-open",
    ) as HTMLElement;
    fireEvent.focus(button);
    expect(button.style.outline).toBe("2px solid #2563eb");
    fireEvent.blur(button);
    expect(button.style.outline).toBe("");
  });

  describe("dropdown arrow-key navigation (#6827)", () => {
    const MENU: MenuAction[] = [
      {
        name: "new",
        label: "New",
        sortRank: 0,
        menuType: "MENU",
        children: [
          { name: "new-folder", label: "Folder", sortRank: 0, menuType: "MENUITEM" },
          { name: "new-page", label: "Page", sortRank: 1, menuType: "MENUITEM" },
          { name: "new-site", label: "Site", sortRank: 2, menuType: "MENUITEM" },
        ],
      },
    ];

    function item(name: string): HTMLElement {
      return screen.getByTestId(`action-toolbar-item-${name}`) as HTMLElement;
    }

    it("ArrowDown on the parent opens the menu on the first item", () => {
      render(<ActionToolbar actions={MENU} />);
      fireEvent.keyDown(item("new"), { key: "ArrowDown" });
      expect(screen.getByTestId("action-toolbar-menu-new")).toBeTruthy();
      expect(document.activeElement).toBe(item("new-folder"));
      expect(item("new-folder").getAttribute("tabindex")).toBe("0");
      expect(item("new-page").getAttribute("tabindex")).toBe("-1");
      expect(item("new-site").getAttribute("tabindex")).toBe("-1");
    });

    it("ArrowUp on the parent opens the menu on the last item", () => {
      render(<ActionToolbar actions={MENU} />);
      fireEvent.keyDown(item("new"), { key: "ArrowUp" });
      expect(screen.getByTestId("action-toolbar-menu-new")).toBeTruthy();
      expect(document.activeElement).toBe(item("new-site"));
    });

    it("ArrowDown/ArrowUp cycle focus across items with wrap", () => {
      render(<ActionToolbar actions={MENU} />);
      fireEvent.click(item("new"));
      // Mouse open lands focus on the first item so arrows work right away.
      expect(document.activeElement).toBe(item("new-folder"));
      fireEvent.keyDown(item("new-folder"), { key: "ArrowDown" });
      expect(document.activeElement).toBe(item("new-page"));
      fireEvent.keyDown(item("new-page"), { key: "ArrowDown" });
      expect(document.activeElement).toBe(item("new-site"));
      // Wraps past the last item back to the first.
      fireEvent.keyDown(item("new-site"), { key: "ArrowDown" });
      expect(document.activeElement).toBe(item("new-folder"));
      // ArrowUp from the first item wraps to the last.
      fireEvent.keyDown(item("new-folder"), { key: "ArrowUp" });
      expect(document.activeElement).toBe(item("new-site"));
    });

    it("Home/End jump to the first/last item", () => {
      render(<ActionToolbar actions={MENU} />);
      fireEvent.click(item("new"));
      fireEvent.keyDown(item("new-folder"), { key: "End" });
      expect(document.activeElement).toBe(item("new-site"));
      expect(item("new-site").getAttribute("tabindex")).toBe("0");
      expect(item("new-folder").getAttribute("tabindex")).toBe("-1");
      fireEvent.keyDown(item("new-site"), { key: "Home" });
      expect(document.activeElement).toBe(item("new-folder"));
    });

    it("keeps one tab stop per open menu (roving tabindex)", () => {
      render(<ActionToolbar actions={MENU} />);
      fireEvent.click(item("new"));
      const tabbable = screen
        .getByTestId("action-toolbar-menu-new")
        .querySelectorAll('[tabindex="0"]');
      expect(tabbable.length).toBe(1);
      expect(tabbable[0]).toBe(item("new-folder"));
    });

    it("Escape from inside the menu returns focus to the parent", () => {
      render(<ActionToolbar actions={MENU} />);
      fireEvent.keyDown(item("new"), { key: "ArrowDown" });
      expect(document.activeElement).toBe(item("new-folder"));
      fireEvent.keyDown(document, { key: "Escape" });
      expect(screen.queryByTestId("action-toolbar-menu-new")).toBeNull();
      expect(document.activeElement).toBe(item("new"));
    });

    it("Tab closes the menu so no detached dropdown lingers", () => {
      render(<ActionToolbar actions={MENU} />);
      fireEvent.click(item("new"));
      expect(screen.getByTestId("action-toolbar-menu-new")).toBeTruthy();
      fireEvent.keyDown(document, { key: "Tab" });
      expect(screen.queryByTestId("action-toolbar-menu-new")).toBeNull();
    });

    it("passes the zero serious/critical axe-core gate with a menu open", async () => {
      const { container } = render(
        <ActionToolbar actions={MENU} ariaLabel="Item actions" />,
      );
      fireEvent.click(item("new"));
      expect(screen.getByTestId("action-toolbar-menu-new")).toBeTruthy();
      await renderA11yGate(container);
    });

    it("arrow keys move focus across the one-click Workflow group", () => {
      const actions: MenuAction[] = [
        {
          name: "workflow",
          label: "Workflow",
          sortRank: 9000,
          menuType: "MENU",
          children: [
            { name: "workflow-transition:Submit", label: "Submit", sortRank: 1, menuType: "MENUITEM" },
            { name: "workflow-transition:Approve", label: "Approve", sortRank: 2, menuType: "MENUITEM" },
          ],
        },
      ];
      render(<ActionToolbar actions={actions} />);
      const submit = item("workflow-transition:Submit");
      const approve = item("workflow-transition:Approve");
      submit.focus();
      fireEvent.keyDown(submit, { key: "ArrowDown" });
      expect(document.activeElement).toBe(approve);
      fireEvent.keyDown(approve, { key: "ArrowUp" });
      expect(document.activeElement).toBe(submit);
      fireEvent.keyDown(submit, { key: "End" });
      expect(document.activeElement).toBe(approve);
      fireEvent.keyDown(approve, { key: "Home" });
      expect(document.activeElement).toBe(submit);
    });
  });

  describe("slot-less Arrange disabled state (#6915)", () => {
    const ARRANGE_MENU: MenuAction[] = [
      {
        name: "Arrange",
        label: "Arrange",
        sortRank: 0,
        menuType: "MENU",
        children: [
          {
            name: "Arrange_MoveUpLeft",
            label: "Move Up",
            sortRank: 0,
            menuType: "MENUITEM",
          },
          {
            name: "Arrange_MoveDownRight",
            label: "Move Down",
            sortRank: 1,
            menuType: "MENUITEM",
          },
          {
            name: "Arrange_ChangeTemplateSlot",
            label: "Change Template",
            sortRank: 2,
            menuType: "MENUITEM",
          },
          {
            name: "Arrange_Remove",
            label: "Remove from Slot",
            sortRank: 3,
            menuType: "MENUITEM",
          },
        ],
      },
      { name: "Slot_Add", label: "Slot Add", sortRank: 1, menuType: "MENUITEM" },
    ];

    const ARRANGE_CHILDREN = [
      "Arrange_MoveUpLeft",
      "Arrange_MoveDownRight",
      "Arrange_ChangeTemplateSlot",
      "Arrange_Remove",
    ];

    function openArrangeMenu(): void {
      fireEvent.click(screen.getByTestId("action-toolbar-item-Arrange"));
      expect(screen.getByTestId("action-toolbar-menu-Arrange")).toBeTruthy();
    }

    it("renders Arrange children aria-disabled with a slot tooltip when no slot is selected", () => {
      render(<ActionToolbar actions={ARRANGE_MENU} slot={null} />);
      openArrangeMenu();
      for (const child of ARRANGE_CHILDREN) {
        const el = screen.getByTestId(`action-toolbar-item-${child}`);
        expect(el.getAttribute("aria-disabled")).toBe("true");
        expect(el.getAttribute("title")).toMatch(/slot/i);
        expect(el.getAttribute("aria-label")).toMatch(/slot/i);
      }
      // The Arrange parent stays enabled so the menu stays discoverable.
      expect(
        screen.getByTestId("action-toolbar-item-Arrange").getAttribute(
          "aria-disabled",
        ),
      ).toBeNull();
      // Flat slot actions answer the same contract.
      expect(
        screen.getByTestId("action-toolbar-item-Slot_Add").getAttribute(
          "aria-disabled",
        ),
      ).toBe("true");
    });

    it("disabled activation is a no-op and keeps the menu open", () => {
      const onInvoke = vi.fn();
      render(
        <ActionToolbar actions={ARRANGE_MENU} slot={null} onInvoke={onInvoke} />,
      );
      openArrangeMenu();
      const move = screen.getByTestId("action-toolbar-item-Arrange_MoveUpLeft");
      fireEvent.click(move);
      fireEvent.keyDown(move, { key: "Enter" });
      fireEvent.keyDown(move, { key: " " });
      expect(onInvoke).not.toHaveBeenCalled();
      expect(screen.getByTestId("action-toolbar-menu-Arrange")).toBeTruthy();
    });

    it("keeps disabled items in the roving-tabindex order", () => {
      render(<ActionToolbar actions={ARRANGE_MENU} slot={null} />);
      openArrangeMenu();
      const menu = screen.getByTestId("action-toolbar-menu-Arrange");
      expect(menu.querySelectorAll('[tabindex="0"]').length).toBe(1);
      // Arrow keys land on (not skip) disabled items so SR users discover them.
      const moveUp = screen.getByTestId("action-toolbar-item-Arrange_MoveUpLeft");
      fireEvent.keyDown(moveUp, { key: "ArrowDown" });
      expect(document.activeElement).toBe(
        screen.getByTestId("action-toolbar-item-Arrange_MoveDownRight"),
      );
    });

    it("slot without a snippet names the missing item but enables Slot_Add", () => {
      render(
        <ActionToolbar actions={ARRANGE_MENU} slot={{ ownerId: 42, slotId: 3 }} />,
      );
      openArrangeMenu();
      const remove = screen.getByTestId("action-toolbar-item-Arrange_Remove");
      expect(remove.getAttribute("aria-disabled")).toBe("true");
      expect(remove.getAttribute("title")).toMatch(/item in the slot/i);
      expect(
        screen.getByTestId("action-toolbar-item-Slot_Add").getAttribute(
          "aria-disabled",
        ),
      ).toBeNull();
    });

    it("snippet selection enables every Arrange child", () => {
      const onInvoke = vi.fn();
      render(
        <ActionToolbar
          actions={ARRANGE_MENU}
          slot={{ ownerId: 42, slotId: 3, relationshipId: 88 }}
          onInvoke={onInvoke}
        />,
      );
      openArrangeMenu();
      for (const child of ARRANGE_CHILDREN) {
        expect(
          screen.getByTestId(`action-toolbar-item-${child}`).getAttribute(
            "aria-disabled",
          ),
        ).toBeNull();
      }
      expect(
        screen.getByTestId("action-toolbar-item-Slot_Add").getAttribute(
          "aria-disabled",
        ),
      ).toBeNull();
      fireEvent.click(screen.getByTestId("action-toolbar-item-Arrange_Remove"));
      expect(onInvoke).toHaveBeenCalledWith(
        "Arrange_Remove",
        expect.objectContaining({ name: "Arrange_Remove", parentName: "Arrange" }),
      );
    });

    it("passes the zero serious/critical axe-core gate with slot-less disabled children", async () => {
      const { container } = render(
        <ActionToolbar actions={ARRANGE_MENU} slot={null} ariaLabel="Item actions" />,
      );
      openArrangeMenu();
      await renderA11yGate(container);
    });

    it("slotActionDisabledReason matches the dispatcher hint contract", () => {
      expect(slotActionDisabledReason("Arrange", null)).toBeNull();
      expect(slotActionDisabledReason("Open", null)).toBeNull();
      expect(slotActionDisabledReason("Arrange_MoveUpLeft", null)).toBe(
        EXPLORER_MSG.ACTION_NEEDS_SLOT,
      );
      expect(slotActionDisabledReason("Slot_Add", null)).toBe(
        EXPLORER_MSG.ACTION_NEEDS_SLOT,
      );
      expect(
        slotActionDisabledReason("Arrange_Remove", { ownerId: 1, slotId: 2 }),
      ).toBe(EXPLORER_MSG.ACTION_NEEDS_RELATIONSHIP);
      expect(
        slotActionDisabledReason("Slot_Add", { ownerId: 1, slotId: 2 }),
      ).toBeNull();
      expect(
        slotActionDisabledReason("Arrange_MoveUpLeft", {
          ownerId: 1,
          slotId: 2,
          relationshipId: 9,
        }),
      ).toBeNull();
    });
  });
});

describe("actionIcons", () => {
  it("normalizes server names with separators and case", () => {
    expect(normalizeActionKey("workflow-transition:Submit")).toBe(
      "workflowtransitionsubmit",
    );
    expect(normalizeActionKey("Paste_As_Link")).toBe("pasteaslink");
    expect(normalizeActionKey(" Arrange_MoveUpLeft ")).toBe(
      "arrangemoveupleft",
    );
  });

  it("maps the highest-traffic toolbar actions", () => {
    expect(resolveActionIcon("Open")).toBe("open");
    expect(resolveActionIcon("Preview")).toBe("preview");
    expect(resolveActionIcon("Edit")).toBe("edit");
    expect(resolveActionIcon("New_Folder")).toBe("folder");
    expect(resolveActionIcon("New_Page")).toBe("page");
    expect(resolveActionIcon("New_Asset")).toBe("asset");
    expect(resolveActionIcon("Checkout")).toBe("checkout");
    expect(resolveActionIcon("Checkin")).toBe("checkin");
    expect(resolveActionIcon("Force_Checkin")).toBe("checkin");
    expect(resolveActionIcon("Publish_Now")).toBe("publish");
    expect(resolveActionIcon("Take_Down")).toBe("takedown");
    expect(resolveActionIcon("Stage")).toBe("stage");
    expect(resolveActionIcon("Remove_From_Staging")).toBe("unstage");
    expect(resolveActionIcon("Schedule")).toBe("schedule");
    expect(resolveActionIcon("Rename")).toBe("rename");
    expect(resolveActionIcon("Delete")).toBe("delete");
    expect(resolveActionIcon("Restore")).toBe("restore");
    expect(resolveActionIcon("Purge")).toBe("purge");
    expect(resolveActionIcon("Empty_Recycle_Bin")).toBe("empty");
    expect(resolveActionIcon("Refresh")).toBe("refresh");
    expect(resolveActionIcon("Search")).toBe("search");
    expect(resolveActionIcon("Revisions")).toBe("revisions");
    expect(resolveActionIcon("Promotable_Version")).toBe("promote");
    expect(resolveActionIcon("New_Copy")).toBe("newcopy");
    expect(resolveActionIcon("Folder_Security")).toBe("security");
    expect(resolveActionIcon("Translations")).toBe("lang");
    expect(resolveActionIcon("Relationships")).toBe("relationships");
  });

  it("maps MENU parents and workflow triggers by prefix", () => {
    expect(resolveActionIcon("Paste")).toBe("clipboard");
    expect(resolveActionIcon("Arrange")).toBe("move");
    expect(resolveActionIcon("Create")).toBe("new");
    expect(resolveActionIcon("workflow-transition:Submit")).toBe("transition");
    expect(resolveActionIcon("Workflow_NewVersion")).toBe("workflow");
  });

  it("returns null for long-tail actions so buttons stay label-only", () => {
    expect(resolveActionIcon("lifecycle_analysis")).toBeNull();
    expect(resolveActionIcon("")).toBeNull();
    expect(resolveActionIcon("!!!")).toBeNull();
  });

  it("ActionIcon renders an aria-hidden svg for mapped names only", () => {
    const { container, rerender } = render(<ActionIcon name="Open" />);
    const svg = container.querySelector("svg");
    expect(svg).toBeTruthy();
    expect(svg?.getAttribute("aria-hidden")).toBe("true");
    rerender(<ActionIcon name="lifecycle_analysis" />);
    expect(container.querySelector("svg")).toBeNull();
  });
});
