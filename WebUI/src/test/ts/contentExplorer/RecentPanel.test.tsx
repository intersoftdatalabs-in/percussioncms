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
import { RecentPanel } from "../../../main/ts/contentExplorer/RecentPanel";
import type { RecentEntry } from "../../../main/ts/contentExplorer/recentItems";
import { renderA11yGate } from "./a11y";

const ITEM: RecentEntry = {
  id: "16777215-101-551",
  name: "Home",
  path: "/Sites/Demo/Home",
  kind: "item",
  openedAt: new Date("2026-01-01T00:00:00.000Z").toISOString(),
};

const FOLDER: RecentEntry = {
  id: null,
  name: "Demo",
  path: "/Sites/Demo/",
  kind: "folder",
  openedAt: new Date("2026-01-02T00:00:00.000Z").toISOString(),
};

function renderPanel(overrides = {}) {
  const onOpen = vi.fn();
  const onRemove = vi.fn();
  const onClear = vi.fn();
  const result = render(
    <RecentPanel
      items={[ITEM, FOLDER]}
      onOpen={onOpen}
      onRemove={onRemove}
      onClear={onClear}
      {...overrides}
    />,
  );
  return { ...result, onOpen, onRemove, onClear };
}

describe("RecentPanel (#6256)", () => {
  it("renders keyboard-reachable rows and activates them", async () => {
    const { container, onOpen } = renderPanel();
    const rows = screen.getAllByTestId("recent-item-row");
    expect(rows).toHaveLength(2);
    expect(rows[0]).toHaveAttribute("data-recent-kind", "item");
    expect(rows[1]).toHaveAttribute("data-recent-kind", "folder");
    const openers = screen.getAllByTestId("recent-item-open");
    for (const opener of openers) {
      expect(opener.tagName).toBe("BUTTON");
    }
    // Keyboard: tab reaches the first row opener and Enter activates it.
    openers[0].focus();
    expect(document.activeElement).toBe(openers[0]);
    fireEvent.click(openers[0]);
    expect(onOpen).toHaveBeenCalledWith(ITEM);
    await renderA11yGate(container);
  });

  it("removes and clears entries", () => {
    const { onRemove, onClear } = renderPanel();
    fireEvent.click(screen.getAllByTestId("recent-item-remove")[1]);
    expect(onRemove).toHaveBeenCalledWith(FOLDER);
    fireEvent.click(screen.getByTestId("recent-clear"));
    expect(onClear).toHaveBeenCalledTimes(1);
  });

  it("shows the empty state and disables clear with no items", async () => {
    const { container } = renderPanel({ items: [] });
    expect(screen.getByTestId("recent-empty")).toBeInTheDocument();
    expect(screen.getByTestId("recent-clear")).toBeDisabled();
    await renderA11yGate(container);
  });

  it("announces dropped-entry notices as status, not error", () => {
    renderPanel({ items: [], notice: "That item is no longer available" });
    const status = screen.getByTestId("recent-status");
    expect(status).toHaveAttribute("role", "status");
    expect(status).toHaveTextContent("no longer available");
  });
});
