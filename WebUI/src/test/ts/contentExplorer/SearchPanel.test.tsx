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
});
