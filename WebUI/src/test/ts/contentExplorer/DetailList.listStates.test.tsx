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
 * Single wall-clock budget for every polling gate in this file (#7207).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const LIST_STATES_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7207, same load-contention rationale as
 * the #7147 slice). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const LIST_STATES_TEST_TIMEOUT = 30_000;

/**
 * Item-list loading/empty/error states on i18n keys (#7036, parent
 * #4530): every user-visible list-state string resolves through
 * EXPLORER_MSG, loading announces via role=status without duplicate
 * live regions, the error names the failed folder, and Retry re-runs
 * the list fetch.
 */

const ROW: PSPathItem = {
  id: "s-1",
  path: "/Sites/Demo/Snippet",
  name: "Snippet",
  type: "rffSnippet",
  leaf: true,
  accessLevel: "WRITE",
} as PSPathItem;

function listingResponse(rows: PSPathItem[], total?: number): Response {
  return new Response(
    JSON.stringify({
      PagedItemList: {
        childrenInPage: rows,
        childrenCount: total ?? rows.length,
        startIndex: 0,
      },
    }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}

describe("DetailList list states (#7036)", () => {
  it("announces loading through a single role=status region on the i18n key", { timeout: LIST_STATES_TEST_TIMEOUT }, async () => {
    mockFetch(() => new Promise<Response>(() => undefined));
    const { container, rerender } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    const loading = await screen.findByTestId("detail-list-loading", undefined, { timeout: LIST_STATES_ROUND_TRIP_TIMEOUT });
    expect(loading).toHaveAttribute("role", "status");
    expect(loading).toHaveTextContent(message(EXPLORER_MSG.LIST_LOADING));
    expect(loading.textContent ?? "").toMatch(/Loading items/);
    // Re-rendering while still loading keeps the same live region —
    // assistive tech does not get a duplicate announcement.
    rerender(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    expect(screen.getAllByTestId("detail-list-loading")).toHaveLength(1);
    expect(screen.getByRole("status")).toHaveTextContent(
      message(EXPLORER_MSG.LIST_LOADING),
    );
    await renderA11yGate(container);
  });

  it("keeps stable empty testids with accessible names on the i18n keys", { timeout: LIST_STATES_TEST_TIMEOUT }, async () => {
    mockFetch(async () => listingResponse([]));
    const { container } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-list-empty")).toBeInTheDocument(),
      { timeout: LIST_STATES_ROUND_TRIP_TIMEOUT },
    );
    const empty = screen.getByTestId("detail-list-empty");
    expect(empty).toHaveAttribute("role", "status");
    expect(empty).toHaveTextContent(message(EXPLORER_MSG.LIST_EMPTY));
    expect(empty.textContent ?? "").toMatch(/No items in this folder/);
    const hint = screen.getByTestId("detail-list-empty-hint");
    expect(hint).toHaveTextContent(message(EXPLORER_MSG.LIST_EMPTY_HINT));
    expect(
      screen.queryByTestId("detail-list-loading"),
    ).not.toBeInTheDocument();
    await renderA11yGate(container);
  });

  it("names the failed folder and Retry re-runs the list fetch", { timeout: LIST_STATES_TEST_TIMEOUT }, async () => {
    let calls = 0;
    mockFetch(async () => {
      calls += 1;
      if (calls === 1) {
        return new Response("gone", { status: 404, statusText: "Not Found" });
      }
      return listingResponse([ROW]);
    });
    const { container } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-list-error")).toBeInTheDocument(),
      { timeout: LIST_STATES_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.getByTestId("detail-list-error")).toHaveAttribute(
      "role",
      "alert",
    );
    const folder = screen.getByTestId("detail-list-error-folder");
    expect(folder).toHaveTextContent(
      message(EXPLORER_MSG.LIST_LOAD_ERROR_FOLDER).replace(
        "{path}",
        "/Sites/Demo",
      ),
    );
    expect(folder.textContent ?? "").toMatch(/\/Sites\/Demo/);
    const retry = screen.getByTestId("detail-list-retry");
    expect(retry).toHaveAccessibleName(message(EXPLORER_MSG.RETRY));
    fireEvent.click(retry);
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-s-1")).toBeInTheDocument(),
      { timeout: LIST_STATES_ROUND_TRIP_TIMEOUT },
    );
    expect(calls).toBeGreaterThanOrEqual(2);
    expect(screen.queryByTestId("detail-list-error")).not.toBeInTheDocument();
    await renderA11yGate(container);
  });

  it("keys the incremental-approval badge and the pager chrome", { timeout: LIST_STATES_TEST_TIMEOUT }, async () => {
    mockFetch(async () => listingResponse([ROW], 60));
    const { container } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
        approvedIncrementalIds={new Set(["s-1"])}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-s-1")).toBeInTheDocument(),
      { timeout: LIST_STATES_ROUND_TRIP_TIMEOUT },
    );
    expect(
      screen.getByTestId("detail-incremental-approved-s-1"),
    ).toHaveTextContent(message(EXPLORER_MSG.DETAIL_INCREMENTAL_APPROVED));
    expect(
      screen.getByTestId("detail-incremental-approved-s-1").textContent ?? "",
    ).toMatch(/Approved/);
    const pager = screen.getByTestId("detail-pagination");
    expect(pager).toHaveTextContent(
      message(EXPLORER_MSG.DETAIL_PAGER_PAGE_OF_TOTAL)
        .replace("{page}", "1")
        .replace("{total}", "2"),
    );
    expect(pager).toHaveTextContent(
      message(EXPLORER_MSG.DETAIL_PAGER_TOTAL).replace("{count}", "60"),
    );
    expect(
      screen.getByRole("button", {
        name: message(EXPLORER_MSG.DETAIL_PAGER_PREV),
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", {
        name: message(EXPLORER_MSG.DETAIL_PAGER_NEXT),
      }),
    ).toBeInTheDocument();
    await renderA11yGate(container);
  });
});
