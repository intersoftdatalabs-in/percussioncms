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
  PSItemProperties,
  PSSearchResults,
} from "../../../main/ts/api/contentExplorer/types";
import type { SearchDef } from "../../../main/ts/api/developer/types";
import {
  SearchPanel,
  deleteCatalogRow,
  renameCatalogRow,
  toSearchResults,
} from "../../../main/ts/contentExplorer/SearchPanel";
import { renderA11yGate } from "./a11y";

function makeResults(children: PSItemProperties[]): PSSearchResults {
  return { children, totalCount: children.length, startIndex: 0 };
}

const ONE_ROW: PSItemProperties[] = [
  {
    id: "1",
    title: "Welcome",
    folderPath: "/Sites/Foo",
    type: "page",
  },
];

const SAVED: SearchDef[] = [
  {
    name: "View_All",
    label: "All",
    type: "View",
  },
  {
    name: "All Content",
    label: "All Content",
    standardSearch: true,
  },
  {
    name: "My Pages",
    label: "My Pages",
    userSearch: true,
  },
  {
    name: "Custom URL",
    label: "Custom URL",
    customSearch: true,
  },
];

function emptyCatalog() {
  return vi.fn().mockResolvedValue([] as SearchDef[]);
}

function readyCatalog(items: SearchDef[] = SAVED) {
  return vi.fn().mockResolvedValue(items);
}

/** Two-row manageable catalog for rename/delete tests (#6075). */
const MANAGEABLE: SearchDef[] = [
  { name: "My Pages", label: "My Pages", userSearch: true },
  { name: "All Content", label: "All Content", standardSearch: true },
];

describe("toSearchResults", () => {
  it("maps execute wire rows into PSSearchResults", () => {
    const out = toSearchResults({
      children: [{ id: "9", title: "T", folderPath: "/Sites/X", type: "page" }],
      totalCount: 1,
      startIndex: 1,
    });
    expect(out.children).toHaveLength(1);
    expect(out.children[0]?.id).toBe("9");
    expect(out.totalCount).toBe(1);
  });
});

describe("saved-search catalog reducers (#6075)", () => {
  it("renameCatalogRow refreshes the label by key and keeps other rows", () => {
    const out = renameCatalogRow(
      { kind: "ready", items: MANAGEABLE },
      "My Pages",
      "PAGES!",
    );
    expect(out.kind).toBe("ready");
    if (out.kind !== "ready") return;
    expect(out.items).toHaveLength(2);
    expect(out.items.find((d) => d.name === "My Pages")?.label).toBe("PAGES!");
    expect(out.items.find((d) => d.name === "My Pages")?.name).toBe("My Pages");
    expect(out.items.find((d) => d.name === "All Content")?.label).toBe(
      "All Content",
    );
  });

  it("deleteCatalogRow drops the row by key and keeps the rest", () => {
    const out = deleteCatalogRow(
      { kind: "ready", items: MANAGEABLE },
      "My Pages",
    );
    expect(out.kind).toBe("ready");
    if (out.kind !== "ready") return;
    expect(out.items.map((d) => d.name)).toEqual(["All Content"]);
  });

  it("reducers pass through non-ready catalogs", () => {
    expect(renameCatalogRow({ kind: "loading" }, "k", "L")).toEqual({
      kind: "loading",
    });
    expect(deleteCatalogRow({ kind: "empty" }, "k")).toEqual({ kind: "empty" });
  });
});

describe("SearchPanel", () => {
  it("renders the search input and submit button", async () => {
    render(<SearchPanel listSavedSearches={emptyCatalog()} />);
    expect(screen.getByTestId("search-panel-input")).toBeTruthy();
    expect(screen.getByTestId("search-panel-submit")).toBeTruthy();
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-saved-empty")).toBeTruthy();
    });
  });

  it("submitting the form invokes the search and renders results", async () => {
    const onOpen = vi.fn();
    const onReveal = vi.fn();
    const search = vi.fn().mockResolvedValue(makeResults(ONE_ROW));
    render(
      <SearchPanel
        search={search}
        onOpen={onOpen}
        onReveal={onReveal}
        listSavedSearches={emptyCatalog()}
      />,
    );
    const input = screen.getByTestId("search-panel-input") as HTMLInputElement;
    fireEvent.change(input, { target: { value: "Welcome" } });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    await waitFor(() => {
      expect(search).toHaveBeenCalledTimes(1);
    });
    expect(search.mock.calls[0]?.[0]?.query).toBe("Welcome");
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-results")).toBeTruthy();
    });
    expect(screen.getByTestId("search-panel-result-row")).toHaveAttribute(
      "data-item-type",
      "page",
    );
    fireEvent.click(screen.getByTestId("search-panel-open-1"));
    fireEvent.click(screen.getByTestId("search-panel-reveal-1"));
    expect(onOpen).toHaveBeenCalledTimes(1);
    expect(onOpen.mock.calls[0]?.[0]?.id).toBe("1");
    expect(onReveal).toHaveBeenCalledTimes(1);
  });

  it("renders the loading state while the search is in flight", async () => {
    let resolveSearch!: (results: PSSearchResults) => void;
    const search = vi
      .fn()
      .mockImplementation(
        () =>
          new Promise<PSSearchResults>((res) => {
            resolveSearch = res;
          }),
      );
    render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-saved-empty")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "q" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    expect(await screen.findByTestId("search-panel-loading")).toBeTruthy();
    resolveSearch(makeResults([]));
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-empty")).toBeTruthy();
    });
  });

  it("renders empty-success (not error) when submit returns no rows (#3617)", async () => {
    const search = vi.fn().mockResolvedValue(makeResults([]));
    render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "nothing" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-empty")).toBeTruthy();
    });
  });

  it("renders the error state when the search rejects", async () => {
    const search = vi.fn().mockRejectedValue(new Error("boom"));
    render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "q" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-error")).toBeTruthy();
    });
  });

  it("error state exposes a Retry button that re-issues the failed query — bug fix for kilo-code-bot PR #1398 thread 3614493938", async () => {
    let attempt = 0;
    const search = vi.fn().mockImplementation(() => {
      attempt += 1;
      return attempt === 1
        ? Promise.reject(new Error("first attempt failed"))
        : Promise.resolve(makeResults([]));
    });
    render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "retry-test" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-error")).toBeTruthy();
    });
    expect(attempt).toBe(1);
    // Click Retry — same query is re-issued; this time the search
    // resolves to empty, so the panel transitions to ready + empty.
    fireEvent.click(screen.getByTestId("search-panel-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-empty")).toBeTruthy();
    });
    expect(attempt).toBe(2);
    // The retry used the same query the user submitted earlier
    // (status.query) rather than re-reading the input, so this
    // assertion is a behaviour contract: re-issuing the original
    // failure is more useful than re-issuing whatever the user
    // happens to have typed since.
    expect(search.mock.calls[1]?.[0]?.query).toBe("retry-test");
  });

  it("refuses a NUL query before search and stays open (#5575)", async () => {
    const search = vi.fn().mockResolvedValue(makeResults(ONE_ROW));
    const { container } = render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-saved-empty")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "bad\u0000query" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    expect(search).not.toHaveBeenCalled();
    expect(screen.getByTestId("search-panel")).toBeTruthy();
    expect(screen.getByTestId("search-panel-query-error")).toHaveTextContent(
      /cannot be stored/i,
    );
    expect(screen.queryByTestId("search-panel-results")).toBeNull();
    expect(screen.queryByTestId("search-panel-empty")).toBeNull();
    expect(screen.queryByTestId("search-panel-error")).toBeNull();
    await renderA11yGate(container);
  });

  it("still searches an ordinary query after a refused NUL (#5575)", async () => {
    const search = vi.fn().mockResolvedValue(makeResults(ONE_ROW));
    render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "bad\u0000query" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    expect(search).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "Welcome" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    await waitFor(() => {
      expect(search).toHaveBeenCalledTimes(1);
    });
    expect(search.mock.calls[0]?.[0]?.query).toBe("Welcome");
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-results")).toBeTruthy();
    });
    expect(screen.queryByTestId("search-panel-query-error")).toBeNull();
  });

  it("HTTP 400 on an ordinary query is not a results success (#5575)", async () => {
    const err = Object.assign(new Error("bad request"), { status: 400 });
    const search = vi.fn().mockRejectedValue(err);
    render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "Welcome" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-error")).toBeTruthy();
    });
    expect(search).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId("search-panel-results")).toBeNull();
    expect(screen.queryByTestId("search-panel-empty")).toBeNull();
  });

  it("does not auto-search an initialQuery that contains a NUL (#5575)", async () => {
    const search = vi.fn().mockResolvedValue(makeResults(ONE_ROW));
    render(
      <SearchPanel
        initialQuery={"deep\u0000link"}
        search={search}
        listSavedSearches={emptyCatalog()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-query-error")).toBeTruthy();
    });
    expect(search).not.toHaveBeenCalled();
    expect(screen.queryByTestId("search-panel-results")).toBeNull();
  });

  it("skip-trim: an empty query returns to idle without calling search", async () => {
    const search = vi.fn().mockResolvedValue(makeResults([]));
    render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-saved-empty")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    expect(search).not.toHaveBeenCalled();
  });

  it("normalizes free-text folderPath to repository form (#3438)", async () => {
    const search = vi.fn().mockResolvedValue(makeResults([]));
    render(
      <SearchPanel
        search={search}
        listSavedSearches={emptyCatalog()}
        initialCriteria={{ folderPath: "/Sites" }}
      />,
    );
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "welcome" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    await waitFor(() => {
      expect(search).toHaveBeenCalledTimes(1);
    });
    expect(search.mock.calls[0]?.[0]?.folderPath).toBe("//Sites");
  });

  it("initialQuery triggers a search on mount", async () => {
    const search = vi.fn().mockResolvedValue(makeResults(ONE_ROW));
    render(
      <SearchPanel
        initialQuery="deep link"
        search={search}
        listSavedSearches={emptyCatalog()}
      />,
    );
    await waitFor(() => {
      expect(search).toHaveBeenCalledTimes(1);
    });
    expect(search.mock.calls[0]?.[0]?.query).toBe("deep link");
  });

  it("empty initial query does not auto-fire a search", async () => {
    const search = vi.fn();
    render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-saved-empty")).toBeTruthy();
    });
    expect(search).not.toHaveBeenCalled();
  });

  it("passes the zero serious/critical axe-core gate (idle state)", async () => {
    const { container } = render(
      <SearchPanel listSavedSearches={emptyCatalog()} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-saved-empty")).toBeTruthy();
    });
    await renderA11yGate(container);
  });

  it("passes the zero serious/critical axe-core gate (results state)", async () => {
    const search = vi.fn().mockResolvedValue(makeResults(ONE_ROW));
    const { container } = render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "Welcome" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-results")).toBeTruthy();
    });
    await renderA11yGate(container);
  });

  it("passes the zero serious/critical axe-core gate (error state with retry)", async () => {
    const search = vi.fn().mockRejectedValue(new Error("boom"));
    const { container } = render(
      <SearchPanel search={search} listSavedSearches={emptyCatalog()} />,
    );
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "anything" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("search-panel-retry")).toBeTruthy();
    });
    await renderA11yGate(container);
  });

  describe("saved-search picker (#2506)", () => {
    it("shows loading then catalog select options", async () => {
      let resolveList!: (items: SearchDef[]) => void;
      const listSavedSearches = vi.fn(
        () =>
          new Promise<SearchDef[]>((res) => {
            resolveList = res;
          }),
      );
      render(<SearchPanel listSavedSearches={listSavedSearches} />);
      expect(screen.getByTestId("search-panel-saved-loading")).toBeTruthy();
      resolveList(SAVED);
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-picker")).toBeTruthy();
      });
      const select = screen.getByTestId(
        "search-panel-saved-select",
      ) as HTMLSelectElement;
      expect(select.options.length).toBeGreaterThan(1);
      expect(Array.from(select.options).some((o) => o.value === "All Content")).toBe(
        true,
      );
      expect(Array.from(select.options).some((o) => o.value === "View_All")).toBe(
        true,
      );
    });

    it("shows empty catalog state", async () => {
      render(<SearchPanel listSavedSearches={emptyCatalog()} />);
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-empty")).toBeTruthy();
      });
    });

    it("shows catalog error and retries load", async () => {
      let attempt = 0;
      const listSavedSearches = vi.fn().mockImplementation(() => {
        attempt += 1;
        if (attempt === 1) {
          return Promise.reject(new Error("catalog down"));
        }
        return Promise.resolve(SAVED);
      });
      render(<SearchPanel listSavedSearches={listSavedSearches} />);
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-error")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("search-panel-saved-retry"));
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-picker")).toBeTruthy();
      });
      expect(attempt).toBe(2);
    });

    it("runs the default All view (View_All) via execute", async () => {
      const executeSavedSearch = vi.fn().mockResolvedValue({
        children: ONE_ROW,
        totalCount: 1,
        startIndex: 1,
        searchName: "View_All",
      });
      render(
        <SearchPanel
          listSavedSearches={readyCatalog()}
          executeSavedSearch={executeSavedSearch}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-select")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("search-panel-saved-select"), {
        target: { value: "View_All" },
      });
      fireEvent.click(screen.getByTestId("search-panel-saved-run"));
      await waitFor(() => {
        expect(executeSavedSearch).toHaveBeenCalledTimes(1);
      });
      expect(executeSavedSearch.mock.calls[0]?.[0]).toBe("View_All");
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-results")).toBeTruthy();
      });
    });

    it("runs a selected saved search via execute and shows results", async () => {
      const executeSavedSearch = vi.fn().mockResolvedValue({
        children: ONE_ROW,
        totalCount: 1,
        startIndex: 1,
        searchName: "All Content",
      });
      const onOpen = vi.fn();
      render(
        <SearchPanel
          listSavedSearches={readyCatalog()}
          executeSavedSearch={executeSavedSearch}
          onOpen={onOpen}
          initialCriteria={{ folderPath: "/Sites/Foo", maxResults: 10 }}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-select")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("search-panel-saved-select"), {
        target: { value: "All Content" },
      });
      fireEvent.click(screen.getByTestId("search-panel-saved-run"));
      await waitFor(() => {
        expect(executeSavedSearch).toHaveBeenCalledTimes(1);
      });
      expect(executeSavedSearch.mock.calls[0]?.[0]).toBe("All Content");
      expect(executeSavedSearch.mock.calls[0]?.[1]).toMatchObject({
        folderPath: "//Sites/Foo",
        maxResults: 10,
        startIndex: 1,
      });
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-results")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("search-panel-open-1"));
      expect(onOpen).toHaveBeenCalledTimes(1);
    });

    it("does not scope All / View_All to Explorer root folder (#3517)", async () => {
      const executeSavedSearch = vi.fn().mockResolvedValue({
        children: [],
        totalCount: 0,
        startIndex: 1,
        searchName: "View_All",
      });
      render(
        <SearchPanel
          listSavedSearches={async () => [
            { name: "View_All", label: "All", type: "View" },
          ]}
          executeSavedSearch={executeSavedSearch}
          initialCriteria={{ folderPath: "/" }}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-select")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("search-panel-saved-select"), {
        target: { value: "View_All" },
      });
      fireEvent.click(screen.getByTestId("search-panel-saved-run"));
      await waitFor(() => {
        expect(executeSavedSearch).toHaveBeenCalledTimes(1);
      });
      const req = executeSavedSearch.mock.calls[0]?.[1] as
        | { folderPath?: string }
        | undefined;
      expect(req?.folderPath).toBeUndefined();
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-empty")).toBeTruthy();
      });
    });

    it("runs a custom URL saved search and shows rows or an empty list", async () => {
      const executeSavedSearch = vi.fn().mockResolvedValue({
        children: [],
        totalCount: 0,
        startIndex: 1,
        searchName: "Custom URL",
      });
      render(
        <SearchPanel
          listSavedSearches={readyCatalog()}
          executeSavedSearch={executeSavedSearch}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-select")).toBeTruthy();
      });
      expect(
        (screen.getByTestId("search-panel-saved-run") as HTMLButtonElement).disabled,
      ).toBe(true);
      fireEvent.click(screen.getByTestId("search-panel-saved-run"));
      expect(executeSavedSearch).not.toHaveBeenCalled();
      fireEvent.change(screen.getByTestId("search-panel-saved-select"), {
        target: { value: "Custom URL" },
      });
      expect(
        (screen.getByTestId("search-panel-saved-run") as HTMLButtonElement).disabled,
      ).toBe(false);
      fireEvent.click(screen.getByTestId("search-panel-saved-run"));
      await waitFor(() => {
        expect(executeSavedSearch).toHaveBeenCalledTimes(1);
      });
      expect(executeSavedSearch.mock.calls[0]?.[0]).toBe("Custom URL");
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-empty")).toBeTruthy();
      });
    });

    it("keeps HTTP 403 and 404 on the error panel", async () => {
      const executeSavedSearch = vi
        .fn()
        .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: null })
        .mockRejectedValueOnce({ status: 404, statusText: "Not Found", body: null });
      render(
        <SearchPanel
          listSavedSearches={readyCatalog()}
          executeSavedSearch={executeSavedSearch}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-select")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("search-panel-saved-select"), {
        target: { value: "Custom URL" },
      });
      fireEvent.click(screen.getByTestId("search-panel-saved-run"));
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-error").textContent).toMatch(/HTTP 403/);
      });
      fireEvent.click(screen.getByTestId("search-panel-retry"));
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-error").textContent).toMatch(/HTTP 404/);
      });
      expect(screen.queryByTestId("search-panel-empty")).toBeNull();
      expect(screen.queryByTestId("search-panel-results")).toBeNull();
    });

    it("does not run a custom URL view from the saved-search list", async () => {
      const executeSavedSearch = vi.fn();
      render(
        <SearchPanel
          listSavedSearches={readyCatalog([
            {
              name: "Outbox",
              label: "Outbox",
              type: "View",
              customSearch: true,
            },
          ])}
          executeSavedSearch={executeSavedSearch}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-select")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("search-panel-saved-select"), {
        target: { value: "Outbox" },
      });
      expect(
        (screen.getByTestId("search-panel-saved-run") as HTMLButtonElement).disabled,
      ).toBe(true);
      expect(screen.getByTestId("search-panel-saved-select").textContent).toMatch(
        /view URL/,
      );
      expect(executeSavedSearch).not.toHaveBeenCalled();
    });

    it("retries a failed saved-search execute", async () => {
      let attempt = 0;
      const executeSavedSearch = vi.fn().mockImplementation(() => {
        attempt += 1;
        if (attempt === 1) {
          return Promise.reject(new Error("execute failed"));
        }
        return Promise.resolve(makeResults([]));
      });
      render(
        <SearchPanel
          listSavedSearches={readyCatalog()}
          executeSavedSearch={executeSavedSearch}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-select")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("search-panel-saved-select"), {
        target: { value: "My Pages" },
      });
      fireEvent.click(screen.getByTestId("search-panel-saved-run"));
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-error")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("search-panel-retry"));
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-empty")).toBeTruthy();
      });
      expect(attempt).toBe(2);
      expect(executeSavedSearch.mock.calls[1]?.[0]).toBe("My Pages");
    });

    it("passes axe-core with saved picker visible", async () => {
      const { container } = render(
        <SearchPanel listSavedSearches={readyCatalog()} />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-picker")).toBeTruthy();
      });
      await renderA11yGate(container);
    });
  });

  describe("saved-search rename/delete (#6075)", () => {
    function readyManageable() {
      return vi.fn().mockResolvedValue(MANAGEABLE);
    }

    async function selectManageableRow(value = "My Pages") {
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-select")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("search-panel-saved-select"), {
        target: { value },
      });
    }

    function optionValues(): string[] {
      const select = screen.getByTestId(
        "search-panel-saved-select",
      ) as HTMLSelectElement;
      return Array.from(select.options)
        .map((o) => o.value)
        .filter((v) => v.length > 0);
    }

    it("rename saves the new label and updates the picker row", async () => {
      const renameSavedSearch = vi.fn().mockResolvedValue({
        name: "My Pages",
        label: "PAGES!",
        userSearch: true,
      } as SearchDef);
      render(
        <SearchPanel
          listSavedSearches={readyManageable()}
          renameSavedSearch={renameSavedSearch}
          deleteSavedSearch={vi.fn()}
        />,
      );
      await selectManageableRow();
      fireEvent.click(screen.getByTestId("search-panel-saved-rename"));
      const input = screen.getByTestId(
        "search-panel-saved-rename-input",
      ) as HTMLInputElement;
      expect(input.value).toBe("My Pages");
      fireEvent.change(input, { target: { value: "PAGES!" } });
      fireEvent.click(screen.getByTestId("search-panel-saved-rename-save"));
      await waitFor(() => {
        expect(renameSavedSearch).toHaveBeenCalledTimes(1);
      });
      expect(renameSavedSearch.mock.calls[0]?.[0]).toBe("My Pages");
      expect(renameSavedSearch.mock.calls[0]?.[1]).toBe("PAGES!");
      await waitFor(() => {
        expect(
          screen.getByTestId("search-panel-saved-manage-notice"),
        ).toHaveTextContent(/renamed/i);
      });
      // Dialog closes; the picker still lists both rows with the new label.
      expect(
        screen.queryByTestId("search-panel-saved-manage-dialog"),
      ).toBeNull();
      expect(optionValues()).toEqual(
        expect.arrayContaining(["My Pages", "All Content"]),
      );
      const select = screen.getByTestId(
        "search-panel-saved-select",
      ) as HTMLSelectElement;
      expect(
        Array.from(select.options).some((o) => o.textContent === "PAGES!"),
      ).toBe(true);
    });

    it("rename cancel calls no transport and keeps rows", async () => {
      const renameSavedSearch = vi.fn();
      render(
        <SearchPanel
          listSavedSearches={readyManageable()}
          renameSavedSearch={renameSavedSearch}
          deleteSavedSearch={vi.fn()}
        />,
      );
      await selectManageableRow();
      fireEvent.click(screen.getByTestId("search-panel-saved-rename"));
      fireEvent.change(
        screen.getByTestId("search-panel-saved-rename-input"),
        { target: { value: "Changed!" } },
      );
      fireEvent.click(screen.getByTestId("search-panel-saved-manage-cancel"));
      expect(renameSavedSearch).not.toHaveBeenCalled();
      expect(
        screen.queryByTestId("search-panel-saved-manage-dialog"),
      ).toBeNull();
      expect(optionValues()).toEqual(
        expect.arrayContaining(["My Pages", "All Content"]),
      );
    });

    it("rename refuses a blank label before PUT (#6075)", async () => {
      const renameSavedSearch = vi.fn();
      render(
        <SearchPanel
          listSavedSearches={readyManageable()}
          renameSavedSearch={renameSavedSearch}
          deleteSavedSearch={vi.fn()}
        />,
      );
      await selectManageableRow();
      fireEvent.click(screen.getByTestId("search-panel-saved-rename"));
      fireEvent.change(
        screen.getByTestId("search-panel-saved-rename-input"),
        { target: { value: "   " } },
      );
      expect(
        (screen.getByTestId("search-panel-saved-rename-save") as HTMLButtonElement)
          .disabled,
      ).toBe(true);
      fireEvent.click(screen.getByTestId("search-panel-saved-rename-save"));
      expect(renameSavedSearch).not.toHaveBeenCalled();
      expect(optionValues()).toEqual(
        expect.arrayContaining(["My Pages", "All Content"]),
      );
    });

    it.each([400, 403, 409])(
      "rename HTTP %i keeps rows and stays on the dialog",
      async (status) => {
        const renameSavedSearch = vi
          .fn()
          .mockRejectedValue({ status, statusText: "Refused", body: null });
        render(
          <SearchPanel
            listSavedSearches={readyManageable()}
            renameSavedSearch={renameSavedSearch}
            deleteSavedSearch={vi.fn()}
          />,
        );
        await selectManageableRow();
        fireEvent.click(screen.getByTestId("search-panel-saved-rename"));
        fireEvent.change(
          screen.getByTestId("search-panel-saved-rename-input"),
          { target: { value: "PAGES!" } },
        );
        fireEvent.click(screen.getByTestId("search-panel-saved-rename-save"));
        await waitFor(() => {
          expect(renameSavedSearch).toHaveBeenCalledTimes(1);
        });
        await waitFor(() => {
          expect(
            screen.getByTestId("search-panel-saved-manage-error").textContent,
          ).toMatch(new RegExp(`HTTP ${status}`));
        });
        // Dialog stays open; no row loss.
        expect(
          screen.getByTestId("search-panel-saved-manage-dialog"),
        ).toBeTruthy();
        expect(optionValues()).toEqual(
          expect.arrayContaining(["My Pages", "All Content"]),
        );
      },
    );

    it("delete confirm drops the row and clears the selection", async () => {
      const deleteSavedSearch = vi.fn().mockResolvedValue(undefined);
      render(
        <SearchPanel
          listSavedSearches={readyManageable()}
          renameSavedSearch={vi.fn()}
          deleteSavedSearch={deleteSavedSearch}
        />,
      );
      await selectManageableRow();
      fireEvent.click(screen.getByTestId("search-panel-saved-delete"));
      expect(
        screen.getByTestId("search-panel-saved-manage-dialog"),
      ).toBeTruthy();
      fireEvent.click(screen.getByTestId("search-panel-saved-delete-confirm"));
      await waitFor(() => {
        expect(deleteSavedSearch).toHaveBeenCalledTimes(1);
      });
      expect(deleteSavedSearch.mock.calls[0]?.[0]).toBe("My Pages");
      await waitFor(() => {
        expect(
          screen.getByTestId("search-panel-saved-manage-notice"),
        ).toHaveTextContent(/deleted/i);
      });
      expect(
        screen.queryByTestId("search-panel-saved-manage-dialog"),
      ).toBeNull();
      expect(optionValues()).toEqual(["All Content"]);
      expect(
        (screen.getByTestId("search-panel-saved-select") as HTMLSelectElement)
          .value,
      ).toBe("");
    });

    it("delete cancel calls no transport and keeps rows", async () => {
      const deleteSavedSearch = vi.fn();
      render(
        <SearchPanel
          listSavedSearches={readyManageable()}
          renameSavedSearch={vi.fn()}
          deleteSavedSearch={deleteSavedSearch}
        />,
      );
      await selectManageableRow();
      fireEvent.click(screen.getByTestId("search-panel-saved-delete"));
      fireEvent.click(screen.getByTestId("search-panel-saved-manage-cancel"));
      expect(deleteSavedSearch).not.toHaveBeenCalled();
      expect(
        screen.queryByTestId("search-panel-saved-manage-dialog"),
      ).toBeNull();
      expect(optionValues()).toEqual(
        expect.arrayContaining(["My Pages", "All Content"]),
      );
    });

    it.each([400, 403, 409])(
      "delete HTTP %i keeps rows and stays on the dialog",
      async (status) => {
        const deleteSavedSearch = vi
          .fn()
          .mockRejectedValue({ status, statusText: "Refused", body: null });
        render(
          <SearchPanel
            listSavedSearches={readyManageable()}
            renameSavedSearch={vi.fn()}
            deleteSavedSearch={deleteSavedSearch}
          />,
        );
        await selectManageableRow();
        fireEvent.click(screen.getByTestId("search-panel-saved-delete"));
        fireEvent.click(screen.getByTestId("search-panel-saved-delete-confirm"));
        await waitFor(() => {
          expect(deleteSavedSearch).toHaveBeenCalledTimes(1);
        });
        await waitFor(() => {
          expect(
            screen.getByTestId("search-panel-saved-manage-error").textContent,
          ).toMatch(new RegExp(`HTTP ${status}`));
        });
        expect(
          screen.getByTestId("search-panel-saved-manage-dialog"),
        ).toBeTruthy();
        expect(optionValues()).toEqual(
          expect.arrayContaining(["My Pages", "All Content"]),
        );
      },
    );

    it("rename/delete stay disabled for custom URL views", async () => {
      render(
        <SearchPanel
          listSavedSearches={readyCatalog([
            { name: "Outbox", label: "Outbox", type: "View", customSearch: true },
          ])}
          renameSavedSearch={vi.fn()}
          deleteSavedSearch={vi.fn()}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId("search-panel-saved-select")).toBeTruthy();
      });
      fireEvent.change(screen.getByTestId("search-panel-saved-select"), {
        target: { value: "Outbox" },
      });
      expect(
        (screen.getByTestId("search-panel-saved-rename") as HTMLButtonElement)
          .disabled,
      ).toBe(true);
      expect(
        (screen.getByTestId("search-panel-saved-delete") as HTMLButtonElement)
          .disabled,
      ).toBe(true);
    });

    it("passes axe-core with the rename dialog open", async () => {
      const { container } = render(
        <SearchPanel
          listSavedSearches={readyManageable()}
          renameSavedSearch={vi.fn()}
          deleteSavedSearch={vi.fn()}
        />,
      );
      await selectManageableRow();
      fireEvent.click(screen.getByTestId("search-panel-saved-rename"));
      expect(
        screen.getByTestId("search-panel-saved-manage-dialog"),
      ).toBeTruthy();
      await renderA11yGate(container);
    });
  });
});
