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
import { describe, expect, it } from "vitest";
import { DetailList } from "../../../main/ts/contentExplorer/DetailList";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import { message } from "../../../main/ts/i18n/message";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

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
const PAGE_TURN_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const PAGE_TURN_TEST_TIMEOUT = 30_000;

/**
 * Detail-list page-turn loading announcement (#7089, parent #4530).
 *
 * <p>The first-load `detail-list-loading` region only mounts when the
 * list is empty, so page turns kept stale rows mounted with no
 * announcement at all. Page turns now announce through a second
 * `role=status` region (`detail-list-loading-more`) on the same
 * LIST_LOADING i18n key, with `aria-busy` on the list/table/pager and
 * busy (disabled + `aria-disabled`) pager controls while the fetch is
 * in flight.</p>
 */

function item(id: string, name: string): PSPathItem {
  return {
    id,
    path: `/Sites/Demo/${name}`,
    name,
    type: "percPage",
    category: "page",
    accessLevel: "WRITE",
    leaf: true,
  } as PSPathItem;
}

const PAGE0: PSPathItem[] = [item("p0-1", "PageZeroOne")];
const PAGE1: PSPathItem[] = [item("p1-1", "PageOneOne")];

function listingResponse(rows: PSPathItem[], total: number): Response {
  return new Response(
    JSON.stringify({
      PagedItemList: {
        childrenInPage: rows,
        childrenCount: total,
        startIndex: 0,
      },
    }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}

describe("DetailList page-turn announcement (#7089)", () => {
  it("announces the page-turn fetch without duplicating the first-load region", { timeout: PAGE_TURN_TEST_TIMEOUT }, async () => {
    let releasePage2!: (value: Response) => void;
    let calls = 0;
    mockFetch(async () => {
      calls += 1;
      if (calls === 1) {
        return listingResponse(PAGE0, 120);
      }
      return new Promise<Response>((resolve) => {
        releasePage2 = resolve;
      });
    });
    const { container } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p0-1")).toBeInTheDocument(),
      { timeout: PAGE_TURN_ROUND_TRIP_TIMEOUT },
    );

    fireEvent.click(
      screen.getByRole("button", {
        name: message(EXPLORER_MSG.DETAIL_PAGER_NEXT),
      }),
    );

    // The page-turn fetch announces through its own polite live region
    // on the existing LIST_LOADING key while stale rows stay mounted.
    const more = await screen.findByTestId("detail-list-loading-more", undefined, { timeout: PAGE_TURN_ROUND_TRIP_TIMEOUT });
    expect(more).toHaveAttribute("role", "status");
    expect(more).toHaveTextContent(message(EXPLORER_MSG.LIST_LOADING));
    expect(more.textContent ?? "").toMatch(/Loading items/);
    // Stale rows stay mounted during the turn (no blank list flash).
    expect(screen.getByTestId("detail-row-p0-1")).toBeInTheDocument();
    // The first-load region stays unmounted — exactly one live region.
    expect(
      screen.queryByTestId("detail-list-loading"),
    ).not.toBeInTheDocument();
    expect(screen.getAllByRole("status")).toHaveLength(1);

    // The list, table, and pager expose the busy state (the root
    // stays busy-free so the live region above is never inside a busy
    // container, #7089 Codex review).
    expect(screen.getByTestId("detail-list")).not.toHaveAttribute(
      "aria-busy",
    );
    expect(screen.getByTestId("detail-table")).toHaveAttribute(
      "aria-busy",
      "true",
    );
    expect(screen.getByTestId("detail-pagination")).toHaveAttribute(
      "aria-busy",
      "true",
    );
    const prev = screen.getByTestId("detail-pager-prev");
    const next = screen.getByTestId("detail-pager-next");
    expect(prev).toBeDisabled();
    expect(next).toBeDisabled();
    expect(prev).toHaveAttribute("aria-disabled", "true");
    expect(next).toHaveAttribute("aria-disabled", "true");

    await renderA11yGate(container);

    releasePage2(listingResponse(PAGE1, 120));
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p1-1")).toBeInTheDocument(),
      { timeout: PAGE_TURN_ROUND_TRIP_TIMEOUT },
    );
    // The region persists but clears — no stale announcement text.
    expect(screen.getByTestId("detail-list-loading-more")).toBeEmpty();
    expect(screen.getByTestId("detail-pager-next")).not.toBeDisabled();
    await renderA11yGate(container);
  });
});
