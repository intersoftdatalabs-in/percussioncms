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

import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import type { ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

const adminBootstrap: SpaBootstrap = {
  userName: "Admin",
  locale: "en-us",
  entry: "explorer",
  isAdmin: true,
  isDesigner: false,
  isWidgetBuilderActive: false,
  allowExternalAvatarFetch: true,
};

function renderShell(
  ui: ReactElement,
  bootstrap: SpaBootstrap = adminBootstrap,
) {
  return render(<BootstrapProvider value={bootstrap}>{ui}</BootstrapProvider>);
}

const TREE_FOLDER = {
  id: "folder-1",
  path: "/Sites/Foo",
  folderPath: "/Sites/Foo",
  name: "Foo",
  type: "folder",
  hasFolderChildren: false,
  accessLevel: "WRITE",
};

function stubTreeAndListFetch() {
  mockFetch(async (input) => {
    const url = typeof input === "string" ? input : (input as Request).url;
    if (url.includes("paginatedFolder") || url.includes("/folder/")) {
      return new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: [],
            childrenCount: 0,
            startIndex: 0,
          },
          PathItem: [TREE_FOLDER],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }
    return new Response("{}", {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  });
}

function treeRow(): HTMLElement {
  const host = screen.getByTestId("tree-node-/Sites/Foo");
  const row = host.querySelector('[role="treeitem"]');
  if (!row) {
    throw new Error(
      "tree row [role=treeitem] missing under tree-node-/Sites/Foo",
    );
  }
  return row as HTMLElement;
}

const PROBE_ACTION = {
  name: "tree-probe-action",
  label: "Tree probe action",
  sortRank: 1,
  menuType: "MENUITEM" as const,
};

/**
 * Single wall-clock budget for the retry-failure round trip (#7122).
 * The round trip is purely microtask-driven (rejected catalog promise →
 * error render → focus effect), so the test drains it deterministically
 * with {@code act} and keeps exactly one polling gate as a backstop for
 * loaded-worker scheduling lag — never the three chained 10s gates that
 * expired one after another under full-suite CPU contention. Every
 * {@code waitFor}/{@code findBy} in this file uses it explicitly: the 1s
 * testing-library defaults are the most-tripped cliff when hundreds of
 * suite files share the worker host.
 */
const RETRY_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7122, same load-contention rationale as
 * {@code EXPLORER_SHELL_TEST_TIMEOUT} in #4558). The RTL budgets above
 * plus the axe gate in the error-state test can jointly exceed the 20s
 * global default when the worker host is saturated. The primary gates
 * stay progress-based (act drains + bounded single backstops); this
 * ceiling is strictly a last-resort backstop.
 */
const CONTEXT_MENU_RETRY_TEST_TIMEOUT = 30_000;

describe("ContentExplorerShell context-menu catalog Retry (#6874)", () => {
  it(
    "failed catalog load shows the error with Retry, not 'No actions'",
    { timeout: CONTEXT_MENU_RETRY_TEST_TIMEOUT },
    async () => {
      stubTreeAndListFetch();
      const loadMenuActions = vi.fn(async () => {
        throw new Error("catalog down");
      });

      const { container } = renderShell(
        <ContentExplorerShell
          initialPath="/Sites"
          loadDisplayFormats={async () => []}
          loadMenuActions={loadMenuActions}
          loadWorkflowMenuActions={async () => null}
        />,
      );
      await waitFor(() => expect(treeRow()).toBeInTheDocument(), {
        timeout: RETRY_ROUND_TRIP_TIMEOUT,
      });

      fireEvent.contextMenu(treeRow(), { clientX: 100, clientY: 200 });

      // Loading answers first; the failure swaps in the error state.
      expect(screen.getByTestId("context-menu-loading")).toBeInTheDocument();
      await waitFor(
        () =>
          expect(screen.getByTestId("context-menu-error")).toBeInTheDocument(),
        { timeout: RETRY_ROUND_TRIP_TIMEOUT },
      );
      expect(screen.getByTestId("context-menu-error").textContent).toMatch(
        /could not load/i,
      );
      expect(screen.getByTestId("context-menu-retry")).toBeTruthy();
      // A failure is not an empty catalog.
      expect(screen.queryByTestId("context-menu-empty")).toBeNull();
      expect(
        screen.queryByTestId("context-menu-item-tree-probe-action"),
      ).toBeNull();

      await renderA11yGate(container);
    },
  );

  it(
    "Retry re-runs the catalog load for the same row and swaps in the actions",
    { timeout: CONTEXT_MENU_RETRY_TEST_TIMEOUT },
    async () => {
      stubTreeAndListFetch();
      // The toolbar shares loadMenuActions, so gate on a flag instead of
      // mockRejectedValueOnce: every catalog load fails until Retry flips it.
      let catalogUp = false;
      const loadMenuActions = vi.fn(async () => {
        if (!catalogUp) {
          throw new Error("catalog down");
        }
        return [PROBE_ACTION];
      });

      renderShell(
        <ContentExplorerShell
          initialPath="/Sites"
          loadDisplayFormats={async () => []}
          loadMenuActions={loadMenuActions}
          loadWorkflowMenuActions={async () => null}
        />,
      );
      await waitFor(() => expect(treeRow()).toBeInTheDocument(), {
        timeout: RETRY_ROUND_TRIP_TIMEOUT,
      });

      fireEvent.contextMenu(treeRow(), { clientX: 100, clientY: 200 });
      const retry = await screen.findByTestId("context-menu-retry", undefined, {
        timeout: RETRY_ROUND_TRIP_TIMEOUT,
      });
      expect(loadMenuActions).toHaveBeenCalled();

      catalogUp = true;
      fireEvent.click(retry);
      await waitFor(
        () =>
          expect(
            screen.getByTestId("context-menu-item-tree-probe-action"),
          ).toBeInTheDocument(),
        { timeout: RETRY_ROUND_TRIP_TIMEOUT },
      );
      expect(loadMenuActions).toHaveBeenLastCalledWith(
        expect.objectContaining({ path: "/Sites/Foo" }),
      );
      expect(screen.queryByTestId("context-menu-error")).toBeNull();
      expect(screen.queryByTestId("context-menu-empty")).toBeNull();
    },
  );

  it(
    "keeps the error and returns focus to Retry when the retry fails",
    { timeout: CONTEXT_MENU_RETRY_TEST_TIMEOUT },
    async () => {
      stubTreeAndListFetch();
      const loadMenuActions = vi.fn(async () => {
        throw new Error("still down");
      });

      renderShell(
        <ContentExplorerShell
          initialPath="/Sites"
          loadDisplayFormats={async () => []}
          loadMenuActions={loadMenuActions}
          loadWorkflowMenuActions={async () => null}
        />,
      );
      await waitFor(() => expect(treeRow()).toBeInTheDocument(), {
        timeout: RETRY_ROUND_TRIP_TIMEOUT,
      });

      fireEvent.contextMenu(treeRow(), { clientX: 100, clientY: 200 });
      // Explicit budget (never the 1s findBy default): the initial tree
      // load plus the first catalog round trip can exceed a second when
      // hundreds of suite files share the worker host (#7122).
      const retry = await screen.findByTestId("context-menu-retry", undefined, {
        timeout: RETRY_ROUND_TRIP_TIMEOUT,
      });
      retry.focus();
      expect(retry).toHaveFocus();
      fireEvent.click(retry);

      // Deterministic drain (#7122): the retry round trip is purely
      // microtask-driven — the rejected catalog promise settles, the error
      // state re-renders, and the focus effect runs — with no real timers
      // involved. Flushing with act instead of polling wall-clock time
      // removes the starvation cliff: a loaded worker is merely slow, and
      // slowness alone no longer fails the test.
      await act(async () => {});
      // The retry click synchronously re-invokes the catalog load, so the
      // call count is already settled after the deterministic drain.
      expect(loadMenuActions.mock.calls.length).toBeGreaterThanOrEqual(2);

      // Single backstop gate for the full terminal state: error kept, Retry
      // re-rendered, focus returned. One budget absorbs loaded-worker
      // scheduling lag between the drain and the passive-effect commit.
      await waitFor(
        () => {
          expect(screen.getByTestId("context-menu-error")).toBeTruthy();
          expect(screen.getByTestId("context-menu-retry")).toHaveFocus();
        },
        { timeout: RETRY_ROUND_TRIP_TIMEOUT },
      );
    },
  );
});
