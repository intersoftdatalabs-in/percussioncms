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

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  EmptyRecycleDialog,
  loadBinContents,
} from "../../../main/ts/contentExplorer/EmptyRecycleDialog";
import { mockFetch } from "./setup";
import { renderA11yGate } from "./a11y";

function item(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    id: "1-101-9",
    path: "/Recycling/Assets/qa6470",
    name: "qa6470",
    type: "percSimpleTextAsset",
    ...overrides,
  };
}

/**
 * Single wall-clock budget for every polling gate in this file (#7147).
 * Each round trip here is purely microtask-driven (mocked {@code loadBin} /
 * {@code runEmpty} promises → setState → render) with no real timers
 * involved, so the gates are deterministic in outcome; the budget is
 * strictly a backstop for loaded-worker scheduling lag. Every
 * {@code findBy} uses it explicitly: the 1s testing-library default is
 * the most-tripped cliff when hundreds of suite files share the worker
 * host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT} in #7122).
 */
const EMPTY_RECYCLE_DIALOG_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7147, same load-contention rationale as
 * {@code CONTEXT_MENU_RETRY_TEST_TIMEOUT} in #7122). The backstop gates
 * above plus the axe gate can jointly exceed the 20s global default when
 * the worker host is saturated. The primary gates stay progress-based
 * (single bounded backstop per distinct state); this ceiling is strictly
 * a last-resort backstop.
 */
const EMPTY_RECYCLE_DIALOG_TEST_TIMEOUT = 30_000;

describe("EmptyRecycleDialog (#6470)", () => {
  it("lists the bin contents, empties, then reports each removed row", { timeout: EMPTY_RECYCLE_DIALOG_TEST_TIMEOUT }, async () => {
    const runEmpty = vi.fn().mockResolvedValue(undefined);
    const onDone = vi.fn();
    const { container } = render(
      <EmptyRecycleDialog
        binPath="/Recycling"
        loadBin={async () => [item(), item({ id: "2", name: "qa6470b", path: "/Recycling/qa6470b" })]}
        runEmpty={runEmpty}
        onDone={onDone}
      />,
    );
    expect(screen.getByTestId("explorer-empty-recycle-loading")).toBeInTheDocument();
    const list = await screen.findByTestId("explorer-empty-recycle-list", undefined, {
      timeout: EMPTY_RECYCLE_DIALOG_ROUND_TRIP_TIMEOUT,
    });
    expect(list).toHaveTextContent(/qa6470/);
    expect(list).toHaveTextContent(/qa6470b/);
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-ok"));
    const report = await screen.findByTestId("explorer-empty-recycle-report", undefined, {
      timeout: EMPTY_RECYCLE_DIALOG_ROUND_TRIP_TIMEOUT,
    });
    expect(report).toHaveTextContent(/Removed 2 item\(s\)/);
    expect(runEmpty).toHaveBeenCalledTimes(1);
    const rows = screen.getAllByTestId("explorer-empty-recycle-report-row");
    expect(rows).toHaveLength(2);
    expect(rows[0]).toHaveAttribute("data-outcome", "removed");
    expect(rows[0]).toHaveTextContent(/qa6470/);
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-close"));
    expect(onDone).toHaveBeenCalledWith({ ran: true, refresh: true });
    await renderA11yGate(container);
  });

  it("disables confirm when the bin is already empty", { timeout: EMPTY_RECYCLE_DIALOG_TEST_TIMEOUT }, async () => {
    const runEmpty = vi.fn();
    const onDone = vi.fn();
    render(
      <EmptyRecycleDialog
        binPath="/Recycling"
        loadBin={async () => []}
        runEmpty={runEmpty}
        onDone={onDone}
      />,
    );
    await screen.findByTestId("explorer-empty-recycle-empty", undefined, {
      timeout: EMPTY_RECYCLE_DIALOG_ROUND_TRIP_TIMEOUT,
    });
    expect(screen.getByTestId("explorer-empty-recycle-ok")).toBeDisabled();
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-cancel"));
    expect(runEmpty).not.toHaveBeenCalled();
    expect(onDone).toHaveBeenCalledWith({ ran: false, refresh: false });
  });

  it("shows the load error with retry and recovers", { timeout: EMPTY_RECYCLE_DIALOG_TEST_TIMEOUT }, async () => {
    let calls = 0;
    const runEmpty = vi.fn();
    const onDone = vi.fn();
    render(
      <EmptyRecycleDialog
        binPath="/Recycling"
        loadBin={async () => {
          calls += 1;
          if (calls === 1) {
            throw new Error("boom");
          }
          return [item()];
        }}
        runEmpty={runEmpty}
        onDone={onDone}
      />,
    );
    await screen.findByTestId("explorer-empty-recycle-load-error", undefined, {
      timeout: EMPTY_RECYCLE_DIALOG_ROUND_TRIP_TIMEOUT,
    });
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-retry"));
    const list = await screen.findByTestId("explorer-empty-recycle-list", undefined, {
      timeout: EMPTY_RECYCLE_DIALOG_ROUND_TRIP_TIMEOUT,
    });
    expect(list).toHaveTextContent(/qa6470/);
    expect(runEmpty).not.toHaveBeenCalled();
  });

  it("names the 409 failure and reports no refresh", { timeout: EMPTY_RECYCLE_DIALOG_TEST_TIMEOUT }, async () => {
    const onDone = vi.fn();
    render(
      <EmptyRecycleDialog
        binPath="/Recycling"
        loadBin={async () => [item()]}
        runEmpty={async () => {
          throw { status: 409, statusText: "Conflict" };
        }}
        onDone={onDone}
      />,
    );
    await screen.findByTestId("explorer-empty-recycle-list", undefined, {
      timeout: EMPTY_RECYCLE_DIALOG_ROUND_TRIP_TIMEOUT,
    });
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-ok"));
    const report = await screen.findByTestId("explorer-empty-recycle-report", undefined, {
      timeout: EMPTY_RECYCLE_DIALOG_ROUND_TRIP_TIMEOUT,
    });
    expect(report).toHaveTextContent(/could not empty/i);
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-close"));
    expect(onDone).toHaveBeenCalledWith({ ran: true, refresh: false });
  });

  it("cancel runs nothing", { timeout: EMPTY_RECYCLE_DIALOG_TEST_TIMEOUT }, async () => {
    const runEmpty = vi.fn();
    const onDone = vi.fn();
    render(
      <EmptyRecycleDialog
        binPath="/Recycling"
        loadBin={async () => [item()]}
        runEmpty={runEmpty}
        onDone={onDone}
      />,
    );
    await screen.findByTestId("explorer-empty-recycle-list", undefined, {
      timeout: EMPTY_RECYCLE_DIALOG_ROUND_TRIP_TIMEOUT,
    });
    fireEvent.click(screen.getByTestId("explorer-empty-recycle-cancel"));
    expect(runEmpty).not.toHaveBeenCalled();
    expect(onDone).toHaveBeenCalledWith({ ran: false, refresh: false });
  });

  it("loadBinContents unions the bin mirrors, skips missing roots, dedupes", async () => {
    const seed = item();
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const body = (kids: PSPathItem[]) =>
        new Response(JSON.stringify({ PathItem: kids }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      // Top-level bin holds nothing directly; the seed sits under the
      // Assets mirror and is (incorrectly) repeated under Sites (#6257).
      if (url.endsWith("/path/folder/Recycling")) {
        return body([]);
      }
      if (url.endsWith("/path/folder/Recycling/Assets")) {
        return body([seed]);
      }
      if (url.endsWith("/path/folder/Recycling/Sites")) {
        return body([seed]);
      }
      return new Response("no such folder", { status: 404 });
    });
    const rows = await loadBinContents("/Recycling");
    expect(rows).toHaveLength(1);
    expect(rows[0]).toMatchObject({ id: seed.id, name: seed.name });
  });
});
