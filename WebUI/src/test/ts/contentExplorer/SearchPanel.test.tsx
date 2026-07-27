/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import { SearchPanel } from "../../../main/ts/contentExplorer/SearchPanel";
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

describe("SearchPanel", () => {
  it("renders the search input and submit button", () => {
    render(<SearchPanel />);
    expect(screen.getByTestId("search-panel-input")).toBeTruthy();
    expect(screen.getByTestId("search-panel-submit")).toBeTruthy();
  });

  it("submitting the form invokes the search and renders results", async () => {
    const onOpen = vi.fn();
    const onReveal = vi.fn();
    const search = vi.fn().mockResolvedValue(makeResults(ONE_ROW));
    render(
      <SearchPanel search={search} onOpen={onOpen} onReveal={onReveal} />,
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
    render(<SearchPanel search={search} />);
    fireEvent.change(screen.getByTestId("search-panel-input"), {
      target: { value: "q" },
    });
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    expect(await screen.findByTestId("search-panel-loading")).toBeTruthy();
    resolveSearch(makeResults([]));
  });

  it("renders the empty state when the search returns no rows", async () => {
    const search = vi.fn().mockResolvedValue(makeResults([]));
    render(<SearchPanel search={search} />);
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
    render(<SearchPanel search={search} />);
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
    render(<SearchPanel search={search} />);
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

  it("skip-trim: an empty query returns to idle without calling search", async () => {
    const search = vi.fn().mockResolvedValue(makeResults([]));
    render(<SearchPanel search={search} />);
    fireEvent.click(screen.getByTestId("search-panel-submit"));
    expect(search).not.toHaveBeenCalled();
  });

  it("initialQuery triggers a search on mount", async () => {
    const search = vi.fn().mockResolvedValue(makeResults(ONE_ROW));
    render(
      <SearchPanel initialQuery="deep link" search={search} />,
    );
    await waitFor(() => {
      expect(search).toHaveBeenCalledTimes(1);
    });
    expect(search.mock.calls[0]?.[0]?.query).toBe("deep link");
  });

  it("empty initial query does not auto-fire a search", () => {
    const search = vi.fn();
    render(<SearchPanel search={search} />);
    expect(search).not.toHaveBeenCalled();
  });

  it("passes the zero serious/critical axe-core gate (idle state)", async () => {
    const { container } = render(<SearchPanel />);
    await renderA11yGate(container);
  });

  it("passes the zero serious/critical axe-core gate (results state)", async () => {
    const search = vi.fn().mockResolvedValue(makeResults(ONE_ROW));
    const { container } = render(<SearchPanel search={search} />);
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
    const onRetry = vi.fn();
    const { container } = render(
      <SearchPanel search={search} onRetry={onRetry} />,
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
});
