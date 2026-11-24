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
import type {
  Clipboard,
  ClipboardItem,
  ClipboardPasteSummary,
} from "../../../main/ts/api/contentExplorer/types";
import { ClipboardPanel } from "../../../main/ts/contentExplorer/clipboard/ClipboardPanel";
import { EMPTY_CLIPBOARD, setClipboard } from "../../../main/ts/contentExplorer/clipboard/model";
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
const CLIPBOARD_PANEL_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const CLIPBOARD_PANEL_TEST_TIMEOUT = 30_000;

function item(id: string): ClipboardItem {
  return { id, path: `/Sites/Foo/${id}`, kind: "page", sourceAccessLevel: "ADMIN" };
}
function makeCb(items: ClipboardItem[]): Clipboard {
  return setClipboard(EMPTY_CLIPBOARD, "copy", items);
}

describe("ClipboardPanel", () => {
  it("still mounts an empty clipboard list (host empty-panel contract #3551)", () => {
    render(
      <ClipboardPanel
        clipboard={EMPTY_CLIPBOARD}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    expect(screen.getByTestId("clipboard-panel")).toBeTruthy();
    expect(screen.getByTestId("clipboard-items")).toBeTruthy();
    expect(screen.queryAllByTestId("clipboard-item-row")).toHaveLength(0);
    expect(screen.getByTestId("clipboard-add")).toBeDisabled();
  });

  it("renders folder-kind Sites rows staged by Add to clipboard (#3551)", () => {
    const sites: ClipboardItem[] = [
      {
        id: "s-1",
        path: "/Sites/CorporateInvestments",
        kind: "folder",
        name: "CorporateInvestments",
      },
      {
        id: "s-2",
        path: "/Sites/EnterpriseInvestments",
        kind: "folder",
        name: "EnterpriseInvestments",
      },
    ];
    render(
      <ClipboardPanel
        clipboard={makeCb(sites)}
        onClipboardChange={() => {}}
        items={sites}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    const rows = screen.getAllByTestId("clipboard-item-row");
    expect(rows).toHaveLength(2);
    expect(rows[0]?.textContent).toContain("/Sites/CorporateInvestments");
    expect(rows[0]?.textContent).toContain("(Folder)");
    expect(rows[0]).toHaveAttribute("data-clipboard-kind", "folder");
  });

  it("renders the clipboard panel with size 0 when empty", () => {
    render(
      <ClipboardPanel
        clipboard={EMPTY_CLIPBOARD}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    expect(screen.getByTestId("clipboard-panel")).toBeTruthy();
    expect(screen.getByTestId("clipboard-size").textContent).toBe("(0)");
  });

  it("Add button pushes the supplied selection into the clipboard", () => {
    const onChange = vi.fn();
    render(
      <ClipboardPanel
        clipboard={EMPTY_CLIPBOARD}
        onClipboardChange={onChange}
        items={[item("a"), item("b")]}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("clipboard-add"));
    expect(onChange).toHaveBeenCalledTimes(1);
    const cb = onChange.mock.calls[0]?.[0] as Clipboard;
    expect(cb.operation).toBe("copy");
    expect(cb.items.map((i) => i.id)).toEqual(["a", "b"]);
  });

  it("Clear button empties the clipboard", () => {
    const onChange = vi.fn();
    const full = makeCb([item("a")]);
    render(
      <ClipboardPanel
        clipboard={full}
        onClipboardChange={onChange}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("clipboard-clear"));
    expect(onChange).toHaveBeenCalledWith(EMPTY_CLIPBOARD);
  });

  it("Mode radio buttons reflect the supplied mode and fire onModeChange", () => {
    const onModeChange = vi.fn();
    render(
      <ClipboardPanel
        clipboard={EMPTY_CLIPBOARD}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={onModeChange}
      />,
    );
    const cutRadio = screen.getByTestId("clipboard-mode-cut") as HTMLInputElement;
    fireEvent.click(cutRadio);
    expect(onModeChange).toHaveBeenCalledWith("cut");
  });

  it("shows the selected destination path for paste (#4638)", () => {
    render(
      <ClipboardPanel
        clipboard={makeCb([item("a")])}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={{ path: "/Assets/dst", accessLevel: "WRITE" }}
      />,
    );
    expect(screen.getByTestId("clipboard-paste-dest").textContent).toBe(
      "/Assets/dst",
    );
  });

  it("Paste is disabled when target is not supplied", () => {
    render(
      <ClipboardPanel
        clipboard={makeCb([item("a")])}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    const paste = screen.getByTestId("clipboard-paste") as HTMLButtonElement;
    expect(paste.disabled).toBe(true);
  });

  it("Paste is disabled when the target is VIEW (FR-016 read-only-without-rights)", () => {
    render(
      <ClipboardPanel
        clipboard={makeCb([item("a")])}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={{ path: "/Sites/Foo", accessLevel: "VIEW" }}
      />,
    );
    const paste = screen.getByTestId("clipboard-paste") as HTMLButtonElement;
    expect(paste.disabled).toBe(true);
  });

  it("Paste triggers the transport and aggregates per-item results", { timeout: CLIPBOARD_PANEL_TEST_TIMEOUT }, async () => {
    const paste = vi.fn().mockRejectedValueOnce(new Error("disk full"));
    const onClipboardChange = vi.fn();
    const onPasteSettled = vi.fn();
    render(
      <ClipboardPanel
        clipboard={makeCb([item("a"), item("b")])}
        onClipboardChange={onClipboardChange}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={{ path: "/Sites/Foo", accessLevel: "ADMIN" }}
        paste={paste}
        onPasteSettled={onPasteSettled}
      />,
    );
    fireEvent.click(screen.getByTestId("clipboard-paste"));
    await waitFor(() => {
      expect(onPasteSettled).toHaveBeenCalledTimes(1);
    },
      { timeout: CLIPBOARD_PANEL_ROUND_TRIP_TIMEOUT },
    );
    const summary: ClipboardPasteSummary = onPasteSettled.mock.calls[0]?.[0];
    expect(summary.results).toHaveLength(2);
    expect(summary.results[0]?.ok).toBe(false);
    expect(summary.results[0]?.message).toBe("disk full");
    // Partial failure: clipboard is not cleared.
    expect(onClipboardChange).not.toHaveBeenCalled();
    // Summary renders failures.
    expect(screen.getByTestId("clipboard-summary-failures")).toBeTruthy();
  });

  it("Paste clears the clipboard on full success (per-item ok)", { timeout: CLIPBOARD_PANEL_TEST_TIMEOUT }, async () => {
    const paste = vi.fn().mockResolvedValue(undefined);
    const onClipboardChange = vi.fn();
    render(
      <ClipboardPanel
        clipboard={makeCb([item("a"), item("b")])}
        onClipboardChange={onClipboardChange}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={{ path: "/Sites/Foo", accessLevel: "ADMIN" }}
        paste={paste}
      />,
    );
    fireEvent.click(screen.getByTestId("clipboard-paste"));
    await waitFor(() => {
      expect(onClipboardChange).toHaveBeenCalledWith(EMPTY_CLIPBOARD);
    },
      { timeout: CLIPBOARD_PANEL_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("clipboard-summary")).toBeTruthy();
  });

  it("renders localized kind badges incl. generic items (#6581)", { timeout: CLIPBOARD_PANEL_TEST_TIMEOUT }, async () => {
    const rows: ClipboardItem[] = [
      { id: "p-1", path: "/Sites/Demo/Home", kind: "page", name: "Home" },
      { id: "a-1", path: "/Assets/hero.png", kind: "asset", name: "hero.png" },
      { id: "g-1", path: "/Sites/Demo/Press", kind: "item", name: "Press release" },
    ];
    const { container } = render(
      <ClipboardPanel
        clipboard={makeCb(rows)}
        onClipboardChange={() => {}}
        items={rows}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    const rendered = screen.getAllByTestId("clipboard-item-row");
    expect(rendered).toHaveLength(3);
    expect(rendered[0]).toHaveAttribute("data-clipboard-kind", "page");
    expect(rendered[1]).toHaveAttribute("data-clipboard-kind", "asset");
    expect(rendered[2]).toHaveAttribute("data-clipboard-kind", "item");
    expect(rendered[0]?.textContent).toContain("(Page)");
    expect(rendered[1]?.textContent).toContain("(Asset)");
    // Generic custom-type items no longer masquerade as pages.
    expect(rendered[2]?.textContent).toContain("(Item)");
    expect(rendered[2]?.textContent).not.toContain("(Page)");
    await renderA11yGate(container);
  });

  it("passes the zero serious/critical axe-core gate (empty state)", { timeout: CLIPBOARD_PANEL_TEST_TIMEOUT }, async () => {
    const { container } = render(
      <ClipboardPanel
        clipboard={EMPTY_CLIPBOARD}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={null}
        paste={async () => ({ count: 0, summary: [] })}
      />,
    );
    await renderA11yGate(container);
  });

  it("passes the zero serious/critical axe-core gate (populated state)", { timeout: CLIPBOARD_PANEL_TEST_TIMEOUT }, async () => {
    const { container } = render(
      <ClipboardPanel
        clipboard={makeCb([item("a"), item("b")])}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={{ path: "/Sites/Foo", accessLevel: "ADMIN" }}
        paste={async () => ({ count: 0, summary: [] })}
      />,
    );
    await renderA11yGate(container);
  });

  it("T092c / Edge Cases #3: marks the failure row with data-conflict when status=409", { timeout: CLIPBOARD_PANEL_TEST_TIMEOUT }, async () => {
    // Simulate the second of two concurrent moves losing the race; the
    // default paste transport would convert this ApiError into a
    // ClipboardPasteResultItem with status=409. The summary view must
    // expose that as data-conflict="true" so Playwright + axe can assert
    // it without scraping the human-readable text.
    const apiError = Object.assign(new Error("409 Conflict"), {
      status: 409,
      statusText: "Conflict",
    });
    const paste = vi.fn().mockRejectedValueOnce(apiError);
    const onPasteSettled = vi.fn();
    render(
      <ClipboardPanel
        clipboard={makeCb([item("shared")])}
        onClipboardChange={() => {}}
        items={[]}
        mode="cut"
        onModeChange={() => {}}
        target={{ path: "/Sites/Foo", accessLevel: "ADMIN" }}
        paste={paste}
        onPasteSettled={onPasteSettled}
      />,
    );
    fireEvent.click(screen.getByTestId("clipboard-paste"));
    await waitFor(() => {
      expect(onPasteSettled).toHaveBeenCalledTimes(1);
    },
      { timeout: CLIPBOARD_PANEL_ROUND_TRIP_TIMEOUT },
    );
    const failureRow = screen.getByTestId("clipboard-summary-failure-0");
    expect(failureRow.getAttribute("data-conflict")).toBe("true");
    expect(failureRow.textContent).toContain("409");
  });
});
