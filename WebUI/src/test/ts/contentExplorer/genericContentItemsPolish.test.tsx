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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  DetailList,
  describeListLoadFailure,
  listLoadFailureStatus,
} from "../../../main/ts/contentExplorer/DetailList";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import { message } from "../../../main/ts/i18n/message";
import {
  describeOpenItemUnavailable,
  openItemUnavailable,
} from "../../../main/ts/contentExplorer/openInEditor";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

/**
 * UX polish for the generic content-items list (#6596, parent #4530):
 * an empty-folder state with helpful copy, named failed-summary error
 * copy for list/open failures on Rhythmyx custom-type Item rows, and
 * focus-visible row states for the arrow-key/Enter keyboard path.
 */

function genericItem(
  id: string,
  name: string,
  type: string,
  path = `/Sites/Demo/${name}`,
): PSPathItem {
  return {
    id,
    path,
    name,
    title: `${name} title`,
    type,
    leaf: true,
    accessLevel: "WRITE",
  } as PSPathItem;
}

const ROWS: PSPathItem[] = [
  genericItem("g-1", "PromoSnippet", "rffSnippet"),
  genericItem("g-2", "CustomArticle", "MyCustomArticle"),
];

function listingResponse(rows: PSPathItem[]): Response {
  return new Response(
    JSON.stringify({
      PagedItemList: {
        childrenInPage: rows,
        childrenCount: rows.length,
        startIndex: 0,
      },
    }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}

describe("generic content-items list polish (#6596)", () => {
  it("maps list-load failures to named item-aware summaries", () => {
    expect(describeListLoadFailure(400)).toBe(
      message(EXPLORER_MSG.LIST_LOAD_ERROR_400),
    );
    expect(describeListLoadFailure(403)).toBe(
      message(EXPLORER_MSG.LIST_LOAD_ERROR_403),
    );
    expect(describeListLoadFailure(404)).toBe(
      message(EXPLORER_MSG.LIST_LOAD_ERROR_404),
    );
    expect(describeListLoadFailure(409)).toBe(
      message(EXPLORER_MSG.LIST_LOAD_ERROR_409),
    );
    expect(describeListLoadFailure(500)).toBe(
      message(EXPLORER_MSG.LIST_LOAD_ERROR),
    );
    expect(describeListLoadFailure(null)).toBe(
      message(EXPLORER_MSG.LIST_LOAD_ERROR),
    );
    // Named summaries never scope the list to pages/assets only.
    for (const status of [400, 403, 404, 409]) {
      expect(describeListLoadFailure(status)).not.toMatch(/pages and assets/i);
    }
  });

  it("reads the HTTP status only from status-carrying rejections", () => {
    expect(
      listLoadFailureStatus({ status: 403, statusText: "F", body: null }),
    ).toBe(403);
    expect(listLoadFailureStatus(new Error("boom"))).toBeNull();
    expect(listLoadFailureStatus(null)).toBeNull();
    expect(listLoadFailureStatus("nope")).toBeNull();
  });

  it("empty folder names the empty state and hints at the create paths", async () => {
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
    );
    expect(screen.getByTestId("detail-list-empty")).toHaveTextContent(
      message(EXPLORER_MSG.LIST_EMPTY),
    );
    const hint = screen.getByTestId("detail-list-empty-hint");
    expect(hint).toHaveTextContent(message(EXPLORER_MSG.LIST_EMPTY_HINT));
    expect(hint.textContent ?? "").toMatch(/New Item/i);
    await renderA11yGate(container);
  });

  it("no open folder keeps the bare empty state without the hint", async () => {
    const { container } = render(
      <DetailList
        folderPath={null}
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    expect(screen.getByTestId("detail-list-empty")).toBeInTheDocument();
    expect(
      screen.queryByTestId("detail-list-empty-hint"),
    ).not.toBeInTheDocument();
    await renderA11yGate(container);
  });

  it("403 list failure shows the named summary with a retry affordance", async () => {
    mockFetch(
      async () => new Response("denied", { status: 403, statusText: "Forbidden" }),
    );
    const { container } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-list-error")).toBeInTheDocument(),
    );
    const summary = screen.getByTestId("detail-list-error-summary");
    expect(summary).toHaveTextContent(message(EXPLORER_MSG.LIST_LOAD_ERROR_403));
    expect(summary.textContent ?? "").toMatch(/permission/i);
    expect(summary.textContent ?? "").toMatch(/HTTP 403/);
    // The raw body detail follows the summary instead of replacing it.
    expect(summary.textContent ?? "").toMatch(/denied/);
    expect(screen.getByTestId("detail-list-retry")).toBeInTheDocument();
    expect(screen.getByTestId("detail-list-retry")).toHaveTextContent(
      message(EXPLORER_MSG.RETRY),
    );
    await renderA11yGate(container);
  });

  it("retry reloads the folder list after a failure", async () => {
    let calls = 0;
    mockFetch(async () => {
      calls += 1;
      if (calls === 1) {
        return new Response("denied", { status: 403, statusText: "Forbidden" });
      }
      return listingResponse(ROWS);
    });
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-list-error")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-list-retry"));
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-g-2")).toBeInTheDocument(),
    );
    expect(calls).toBeGreaterThanOrEqual(2);
  });

  it("non-HTTP failures fall back to the generic summary with detail", async () => {
    mockFetch(async () => {
      throw new Error("boom");
    });
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-list-error")).toBeInTheDocument(),
    );
    const summary = screen.getByTestId("detail-list-error-summary");
    expect(summary).toHaveTextContent(message(EXPLORER_MSG.LIST_LOAD_ERROR));
    expect(summary.textContent ?? "").toMatch(/boom/);
  });

  it("generic rows carry the focus-visible ring class for keyboard navigation", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const { container } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-g-1")).toBeInTheDocument(),
    );
    const row = screen.getByTestId("detail-row-g-2");
    expect(row).toHaveClass("perc-explorer-detail-row");
    // The ring lives in an injected stylesheet because inline styles
    // cannot express :focus-visible; plain :focus stays outline-free so
    // mouse clicks do not leave a sticky outline.
    const styles = Array.from(container.querySelectorAll("style"))
      .map((el) => el.textContent ?? "")
      .join("\n");
    expect(styles).toMatch(/\.perc-explorer-detail-row:focus-visible/);
    expect(styles).toMatch(/outline:\s*2px solid/);
    await renderA11yGate(container);
  });

  it("flags only rows that cannot reach the editor as open-unavailable", () => {
    // Folders browse in place — never an open failure.
    expect(
      openItemUnavailable({ id: "1", path: "//Sites/Demo", leaf: false } as PSPathItem),
    ).toBe(false);
    // Numeric ids and path fallbacks both open.
    expect(openItemUnavailable(genericItem("42", "PromoSnippet", "rffSnippet"))).toBe(
      false,
    );
    expect(
      openItemUnavailable({
        path: "//Sites/Demo/CustomArticle",
        leaf: true,
      } as PSPathItem),
    ).toBe(false);
    // Neither an id nor a path: the failed-summary copy applies.
    expect(
      openItemUnavailable({ name: "Ghost", leaf: true } as PSPathItem),
    ).toBe(true);
    expect(describeOpenItemUnavailable()).toBe(
      message(EXPLORER_MSG.OPEN_ITEM_UNAVAILABLE),
    );
  });
});
