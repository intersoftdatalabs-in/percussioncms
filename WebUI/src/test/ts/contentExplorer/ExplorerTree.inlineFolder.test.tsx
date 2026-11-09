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
import { ExplorerTree } from "../../../main/ts/contentExplorer/ExplorerTree";
import {
  formatInlineNewFolderCommitError,
  formatInlineNewFolderFieldError,
  validateInlineNewFolderName,
} from "../../../main/ts/contentExplorer/ExplorerTreeInlineFolder";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { EXPLORER_SHELL_TEST_TIMEOUT, mockFetch } from "./setup";
import { renderA11yGate } from "./a11y";

const SITES: PSPathItem = {
  id: "sites-1",
  path: "/Sites",
  name: "Sites",
  type: "folder",
  hasFolderChildren: true,
};

const FOO: PSPathItem = {
  id: "foo-1",
  path: "/Sites/Foo",
  name: "Foo",
  type: "folder",
  hasFolderChildren: true,
};

const CREATED = {
  id: "n-5882",
  path: "/Sites/Foo/NewKid",
  name: "NewKid",
  type: "folder",
  accessLevel: "WRITE",
};

/** Wire shape from PSPathItemList (pathApi.findChildren unwraps PathItem). */
function pathItemListResponse(items: PSPathItem[]): Response {
  return new Response(JSON.stringify({ PathItem: items }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

function requestUrl(input: unknown): string {
  return typeof input === "string" ? input : (input as Request).url;
}

function requestMethod(init: unknown): string {
  return String(
    (init as RequestInit | undefined)?.method ?? "GET",
  ).toUpperCase();
}

function submitInlineName(name: string): void {
  const input = screen.getByTestId("tree-inline-new-folder-input");
  fireEvent.change(input, { target: { value: name } });
  const form = input.closest("form");
  expect(form).not.toBeNull();
  fireEvent.submit(form!);
}

describe("ExplorerTree inline new-folder (#5882)", {
  timeout: EXPLORER_SHELL_TEST_TIMEOUT,
}, () => {
  it("commits on Enter, reloads the parent, and closes the row", async () => {
    let created = false;
    let createPosts = 0;
    mockFetch(async (input, init) => {
      const url = requestUrl(input);
      if (url.includes("/folders/create") && requestMethod(init) === "POST") {
        createPosts += 1;
        created = true;
        return new Response(JSON.stringify({ Folder: CREATED }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      // Root children (initialPath "/").
      if (/\/pathmanagement\/path\/folder\/?(\?|$)/.test(url)) {
        return pathItemListResponse([SITES]);
      }
      if (url.includes("/Sites/Foo")) {
        return pathItemListResponse(created ? [{ ...CREATED } as PSPathItem] : []);
      }
      if (url.includes("/Sites")) {
        return pathItemListResponse([FOO]);
      }
      return pathItemListResponse([]);
    });

    const onFolderCreated = vi.fn();
    const { container } = render(
      <ExplorerTree
        initialPath="/"
        selectedPath="/Sites/Foo"
        onSelectFolder={() => undefined}
        onFolderCreated={onFolderCreated}
      />,
    );

    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites")).toBeInTheDocument(),
    );
    // Reveal Foo: the tree does not auto-expand ancestors of the selection.
    fireEvent.click(screen.getByTestId("tree-toggle-/Sites"));
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("tree-new-folder"));
    expect(
      screen.getByTestId("tree-inline-new-folder-input"),
    ).toBeInTheDocument();

    submitInlineName("NewKid");

    await waitFor(() =>
      expect(
        screen.getByTestId("tree-node-/Sites/Foo/NewKid"),
      ).toBeInTheDocument(),
    );
    expect(createPosts).toBe(1);
    expect(
      screen.queryByTestId("tree-inline-new-folder"),
    ).not.toBeInTheDocument();
    expect(onFolderCreated).toHaveBeenCalledTimes(1);
    await renderA11yGate(container);
  });

  it("cancels cleanly on Escape with no POST and no tree change", async () => {
    let createPosts = 0;
    mockFetch(async (input, init) => {
      const url = requestUrl(input);
      if (url.includes("/folders/create") && requestMethod(init) === "POST") {
        createPosts += 1;
        return pathItemListResponse([]);
      }
      if (/\/pathmanagement\/path\/folder\/?(\?|$)/.test(url)) {
        return pathItemListResponse([SITES]);
      }
      return pathItemListResponse([]);
    });

    render(
      <ExplorerTree
        initialPath="/"
        selectedPath="/Sites"
        onSelectFolder={() => undefined}
      />,
    );

    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("tree-new-folder"));
    const input = screen.getByTestId("tree-inline-new-folder-input");
    fireEvent.change(input, { target: { value: "Abandoned" } });
    fireEvent.keyDown(input, { key: "Escape" });

    expect(createPosts).toBe(0);
    expect(
      screen.queryByTestId("tree-inline-new-folder"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("Abandoned")).not.toBeInTheDocument();
  });

  it("refuses a blank name client-side with no POST and keeps the row open", async () => {
    let createPosts = 0;
    mockFetch(async (input, init) => {
      const url = requestUrl(input);
      if (url.includes("/folders/create") && requestMethod(init) === "POST") {
        createPosts += 1;
        return pathItemListResponse([]);
      }
      if (/\/pathmanagement\/path\/folder\/?(\?|$)/.test(url)) {
        return pathItemListResponse([SITES]);
      }
      return pathItemListResponse([]);
    });

    const { container } = render(
      <ExplorerTree
        initialPath="/"
        selectedPath="/Sites"
        onSelectFolder={() => undefined}
      />,
    );

    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("tree-new-folder"));
    submitInlineName("   ");

    await waitFor(() =>
      expect(
        screen.getByTestId("tree-inline-new-folder-error"),
      ).toHaveTextContent(/Enter a folder name/),
    );
    expect(createPosts).toBe(0);
    expect(
      screen.getByTestId("tree-inline-new-folder-input"),
    ).toBeInTheDocument();
    await renderA11yGate(container);
  });

  it("keeps the row open on 409 and names the conflicting folder", async () => {
    let createPosts = 0;
    let fooLoads = 0;
    mockFetch(async (input, init) => {
      const url = requestUrl(input);
      if (url.includes("/folders/create") && requestMethod(init) === "POST") {
        createPosts += 1;
        return new Response("name in use", {
          status: 409,
          headers: { "Content-Type": "text/plain" },
        });
      }
      if (/\/pathmanagement\/path\/folder\/?(\?|$)/.test(url)) {
        return pathItemListResponse([SITES]);
      }
      if (url.includes("/Sites")) {
        fooLoads += url.includes("/Sites/Foo") ? 1 : 0;
        return pathItemListResponse(url.includes("/Sites/Foo") ? [] : [FOO]);
      }
      return pathItemListResponse([]);
    });

    render(
      <ExplorerTree
        initialPath="/"
        selectedPath="/Sites/Foo"
        onSelectFolder={() => undefined}
      />,
    );

    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("tree-toggle-/Sites"));
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("tree-new-folder"));
    await waitFor(() =>
      expect(
        screen.getByTestId("tree-inline-new-folder-input"),
      ).toBeInTheDocument(),
    );
    // Settle the parent load the row opener triggers before committing.
    await waitFor(() => expect(fooLoads).toBeGreaterThan(0));
    const loadsBeforeCommit = fooLoads;
    submitInlineName("Taken");

    await waitFor(() =>
      expect(
        screen.getByTestId("tree-inline-new-folder-error"),
      ).toHaveTextContent(/Taken/),
    );
    expect(createPosts).toBe(1);
    // No tree refresh on failure: the parent is not reloaded after the 409.
    expect(fooLoads).toBe(loadsBeforeCommit);
    expect(
      screen.getByTestId("tree-inline-new-folder-input"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("tree-node-/Sites/Foo/Taken"),
    ).not.toBeInTheDocument();
  });
});

describe("inline new-folder validation helpers (#5882)", () => {
  it("refuses blank, NUL, and illegal names", () => {
    expect(validateInlineNewFolderName("")).toBe("blank");
    expect(validateInlineNewFolderName("   ")).toBe("blank");
    expect(validateInlineNewFolderName(null)).toBe("blank");
    expect(validateInlineNewFolderName("bad\u0000name")).toBe("nul");
    expect(validateInlineNewFolderName("a/b")).toBe("invalid");
    expect(validateInlineNewFolderName("..")).toBe("invalid");
    expect(validateInlineNewFolderName("NewKid")).toBeNull();
  });

  it("maps field refusals to product chrome", () => {
    expect(formatInlineNewFolderFieldError("blank")).toMatch(
      /Enter a folder name/,
    );
    expect(formatInlineNewFolderFieldError("nul")).toMatch(
      /cannot be stored/,
    );
    expect(formatInlineNewFolderFieldError("invalid")).toMatch(
      /without path separators/,
    );
  });

  it("names the folder on a 409 commit failure", () => {
    const err = { status: 409, statusText: "Conflict" };
    expect(formatInlineNewFolderCommitError(err, "Taken")).toMatch(/^Taken: /);
    expect(formatInlineNewFolderCommitError(err, "Taken")).toMatch(
      /name in use/,
    );
    expect(formatInlineNewFolderCommitError({ status: 403 }, "Taken")).toMatch(
      /permission/i,
    );
  });
});
