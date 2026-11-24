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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { ViewDef } from "../../../main/ts/api/developer/types";
import {
  collectVisibleViewsCatalogKeys,
  ViewsCatalogTree,
  viewsCatalogGroupKey,
  viewsCatalogLeafKey,
  VIEWS_CATALOG_ROOT_KEY,
} from "../../../main/ts/contentExplorer/ViewsCatalogTree";
import {
  emptyViewCatalogGroups,
  type ViewCatalogGroups,
} from "../../../main/ts/contentExplorer/viewCatalog";
import { renderA11yGate } from "./a11y";

/**
 * Single wall-clock budget for every polling gate in this file (#7226).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const KEYBOARD_NAV_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const KEYBOARD_NAV_TEST_TIMEOUT = 30_000;

const CATALOG: ViewDef[] = [
  { name: "MyPages", label: "My Pages", parentCategory: 1, standardView: true },
  { name: "Inbox", label: "Inbox", parentCategory: 1, customView: true },
  { name: "CommunityNews", label: "Community News", parentCategory: 2 },
  { name: "View_All", label: "All Content", parentCategory: 3, standardView: true },
];

function groupsFrom(catalog: ViewDef[]): ViewCatalogGroups {
  const groups = emptyViewCatalogGroups();
  for (const def of catalog) {
    const cat = (def.parentCategory ?? 4) as 1 | 2 | 3 | 4;
    groups[cat].push(def);
  }
  return groups;
}

const OPEN_ONE = { 1: true, 2: false, 3: false, 4: false } as const;
const OPEN_ALL = { 1: true, 2: true, 3: true, 4: true } as const;

async function renderCatalog(
  props: {
    onSelectView?: (view: ViewDef) => void;
    selectedViewKey?: string | null;
  } = {},
): Promise<{ container: HTMLElement }> {
  const rendered = render(
    <ViewsCatalogTree
      listViews={async () => CATALOG}
      onSelectView={props.onSelectView}
      selectedViewKey={props.selectedViewKey ?? null}
    />,
  );
  await waitFor(() =>
    expect(screen.getByTestId("explorer-views-group-1")).toBeInTheDocument(),
    { timeout: KEYBOARD_NAV_ROUND_TRIP_TIMEOUT },
  );
  return rendered;
}

function catalogRow(testId: string): HTMLElement {
  return screen.getByTestId(testId);
}

function focusCatalogRow(testId: string): HTMLElement {
  const row = catalogRow(testId);
  row.focus();
  return row;
}

function tabStops(): HTMLElement[] {
  return Array.from(
    document.querySelectorAll(
      '[data-testid="explorer-views-tree"] [role="treeitem"]',
    ),
  ).filter((el) => (el as HTMLElement).tabIndex === 0) as HTMLElement[];
}

describe("ViewsCatalogTree visible-row keys (#7043)", () => {
  it("walks root, groups, and open-group leaves in display order", () => {
    expect(
      collectVisibleViewsCatalogKeys(true, { ...OPEN_ALL }, groupsFrom(CATALOG)),
    ).toEqual([
      VIEWS_CATALOG_ROOT_KEY,
      viewsCatalogGroupKey(1),
      viewsCatalogLeafKey(1, "MyPages"),
      viewsCatalogLeafKey(1, "Inbox"),
      viewsCatalogGroupKey(2),
      viewsCatalogLeafKey(2, "CommunityNews"),
      viewsCatalogGroupKey(3),
      viewsCatalogLeafKey(3, "View_All"),
      viewsCatalogGroupKey(4),
    ]);
  });

  it("skips collapsed subtrees and a closed root", () => {
    expect(
      collectVisibleViewsCatalogKeys(true, { ...OPEN_ONE }, groupsFrom(CATALOG)),
    ).toEqual([
      VIEWS_CATALOG_ROOT_KEY,
      viewsCatalogGroupKey(1),
      viewsCatalogLeafKey(1, "MyPages"),
      viewsCatalogLeafKey(1, "Inbox"),
      viewsCatalogGroupKey(2),
      viewsCatalogGroupKey(3),
      viewsCatalogGroupKey(4),
    ]);
    expect(
      collectVisibleViewsCatalogKeys(false, { ...OPEN_ALL }, groupsFrom(CATALOG)),
    ).toEqual([VIEWS_CATALOG_ROOT_KEY]);
  });
});

describe("ViewsCatalogTree arrow-key navigation (#7043)", () => {
  it("starts with a single tab stop on the first visible row", { timeout: KEYBOARD_NAV_TEST_TIMEOUT }, async () => {
    const { container } = await renderCatalog();
    expect(tabStops()).toHaveLength(1);
    expect(tabStops()[0]).toBe(catalogRow("explorer-views-root-row"));
    await renderA11yGate(container);
  });

  it("ArrowDown moves focus without selecting on group rows, selects on leaves", { timeout: KEYBOARD_NAV_TEST_TIMEOUT }, async () => {
    const onSelectView = vi.fn();
    await renderCatalog({ onSelectView });
    // Root -> group 1: focus moves, nothing is selected (groups expand
    // on click/Enter, never on arrow moves).
    focusCatalogRow("explorer-views-root-row");
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    expect(document.activeElement).toBe(catalogRow("explorer-views-group-1-row"));
    expect(onSelectView).not.toHaveBeenCalled();
    // Group 1 -> first leaf: focus and selection move together.
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    const focused = document.activeElement as HTMLElement;
    expect(focused.getAttribute("role")).toBe("treeitem");
    expect(
      focused.getAttribute("data-testid")?.startsWith("explorer-views-leaf-"),
    ).toBe(true);
    expect(onSelectView).toHaveBeenCalledTimes(1);
    expect(tabStops()).toHaveLength(1);
    expect(tabStops()[0]).toBe(focused);
  });

  it("ArrowUp/ArrowDown clamp at the ends instead of wrapping", { timeout: KEYBOARD_NAV_TEST_TIMEOUT }, async () => {
    const onSelectView = vi.fn();
    await renderCatalog({ onSelectView });
    // ArrowUp on the first row stays on the first row.
    focusCatalogRow("explorer-views-root-row");
    fireEvent.keyDown(document.activeElement!, { key: "ArrowUp" });
    expect(document.activeElement).toBe(catalogRow("explorer-views-root-row"));
    expect(onSelectView).not.toHaveBeenCalled();
    // ArrowDown on the last visible row (group 4, closed) stays put.
    focusCatalogRow("explorer-views-group-4-row");
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    expect(document.activeElement).toBe(catalogRow("explorer-views-group-4-row"));
    expect(onSelectView).not.toHaveBeenCalled();
  });

  it("Home/End jump to the first/last visible row", { timeout: KEYBOARD_NAV_TEST_TIMEOUT }, async () => {
    const onSelectView = vi.fn();
    await renderCatalog({ onSelectView });
    focusCatalogRow("explorer-views-group-2-row");
    fireEvent.keyDown(document.activeElement!, { key: "End" });
    expect(document.activeElement).toBe(catalogRow("explorer-views-group-4-row"));
    expect(onSelectView).not.toHaveBeenCalled();
    fireEvent.keyDown(document.activeElement!, { key: "Home" });
    expect(document.activeElement).toBe(catalogRow("explorer-views-root-row"));
    expect(onSelectView).not.toHaveBeenCalled();
  });

  it("arrow moves skip collapsed subtrees", { timeout: KEYBOARD_NAV_TEST_TIMEOUT }, async () => {
    await renderCatalog();
    // Group 2 is closed: ArrowDown from the group 2 row lands on the
    // group 3 row, not on the CommunityNews leaf.
    focusCatalogRow("explorer-views-group-2-row");
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    expect(document.activeElement).toBe(catalogRow("explorer-views-group-3-row"));
    // Expanding group 3 reveals its leaf to the same move.
    fireEvent.click(catalogRow("explorer-views-group-3-row"));
    await waitFor(() =>
      expect(screen.getByTestId("explorer-views-leaf-View_All")).toBeInTheDocument(),
      { timeout: KEYBOARD_NAV_ROUND_TRIP_TIMEOUT },
    );
    focusCatalogRow("explorer-views-group-2-row");
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    expect(document.activeElement).toBe(catalogRow("explorer-views-group-3-row"));
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    expect(document.activeElement).toBe(catalogRow("explorer-views-leaf-View_All"));
  });

  it("moves the tab stop to the selected leaf after an external selection change", { timeout: KEYBOARD_NAV_TEST_TIMEOUT }, async () => {
    const onSelectView = vi.fn();
    const rendered = render(
      <ViewsCatalogTree
        listViews={async () => CATALOG}
        onSelectView={onSelectView}
        selectedViewKey={null}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-views-group-1")).toBeInTheDocument(),
      { timeout: KEYBOARD_NAV_ROUND_TRIP_TIMEOUT },
    );
    expect(tabStops()[0]).toBe(catalogRow("explorer-views-root-row"));
    // Host selection arrives without catalog interaction: the tab stop
    // resolves against the newly selected leaf.
    rendered.rerender(
      <ViewsCatalogTree
        listViews={async () => CATALOG}
        onSelectView={onSelectView}
        selectedViewKey="MyPages"
      />,
    );
    await waitFor(() => expect(tabStops()).toHaveLength(1), { timeout: KEYBOARD_NAV_ROUND_TRIP_TIMEOUT });
    expect(tabStops()[0]).toBe(catalogRow("explorer-views-leaf-MyPages"));
    await renderA11yGate(rendered.container);
  });

  it("collapsing an ancestor of the focused leaf keeps one tab stop on a visible row", { timeout: KEYBOARD_NAV_TEST_TIMEOUT }, async () => {
    const { container } = await renderCatalog();
    focusCatalogRow("explorer-views-root-row");
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    const leaf = document.activeElement as HTMLElement;
    expect(
      leaf.getAttribute("data-testid")?.startsWith("explorer-views-leaf-"),
    ).toBe(true);
    expect(tabStops()[0]).toBe(leaf);
    // Collapse group 1 with the mouse toggle: the leaf unmounts while it
    // is still the last-interacted key. The catalog must keep exactly one
    // tab stop, and it must be a row that is still rendered.
    fireEvent.click(catalogRow("explorer-views-group-1-row"));
    await waitFor(() =>
      expect(screen.queryByTestId("explorer-views-leaf-MyPages")).toBeNull(),
      { timeout: KEYBOARD_NAV_ROUND_TRIP_TIMEOUT },
    );
    expect(tabStops()).toHaveLength(1);
    expect(tabStops()[0]).toBe(catalogRow("explorer-views-group-1-row"));
    await renderA11yGate(container);
  });

  it("labels rows with tree levels and carries the focus-ring class", { timeout: KEYBOARD_NAV_TEST_TIMEOUT }, async () => {
    const { container } = await renderCatalog();
    expect(catalogRow("explorer-views-root-row").getAttribute("aria-level")).toBe("1");
    expect(catalogRow("explorer-views-group-1-row").getAttribute("aria-level")).toBe("2");
    expect(catalogRow("explorer-views-leaf-MyPages").getAttribute("aria-level")).toBe("3");
    for (const testId of [
      "explorer-views-root-row",
      "explorer-views-group-1-row",
      "explorer-views-leaf-MyPages",
    ]) {
      expect(catalogRow(testId).classList.contains("perc-explorer-views-catalog-row")).toBe(
        true,
      );
    }
    const styles = Array.from(document.querySelectorAll("style")).map(
      (el) => el.textContent ?? "",
    );
    expect(
      styles.some((css) =>
        css.includes(".perc-explorer-views-catalog-row:focus-visible"),
      ),
    ).toBe(true);
    await renderA11yGate(container);
  });
});
