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
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { PSPathItem } from "../../../../main/ts/api/contentExplorer/types";
import {
  ContentPickerDialog,
  type ContentPickerSelection,
} from "../../../../main/ts/editor/tiptap/ContentPickerDialog";

const SITES_CHILDREN: PSPathItem[] = [
  { name: "News", path: "/Sites/News/", type: "folder" },
  { id: "77", name: "About", path: "/Sites/About", type: "page" },
];

const NEWS_CHILDREN: PSPathItem[] = [
  { id: "78", name: "Story", path: "/Sites/News/Story", type: "page" },
];

function listFolder(path: string): Promise<PSPathItem[]> {
  if (path === "/Sites/News/" || path === "/Sites/News") {
    return Promise.resolve(NEWS_CHILDREN);
  }
  return Promise.resolve(SITES_CHILDREN);
}

describe("ContentPickerDialog", () => {
  afterEach(() => {
    cleanup();
  });

  it("browses folders and picks an item through Use selected", async () => {
    const onPick = vi.fn();
    const onClose = vi.fn();
    render(
      <ContentPickerDialog
        name="text"
        mode="link"
        onPick={onPick}
        onClose={onClose}
        listFolder={listFolder}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("tiptap-content-picker-text")).toBeTruthy();
    });
    // Browse rows load from the stubbed folder.
    await waitFor(() => {
      expect(
        screen.getAllByTestId("tiptap-content-picker-row-text"),
      ).toHaveLength(2);
    });
    // Folders navigate; items arm the Use button.
    fireEvent.click(screen.getAllByTestId("tiptap-content-picker-row-text")[0]);
    await waitFor(() => {
      expect(
        screen.getAllByTestId("tiptap-content-picker-row-text"),
      ).toHaveLength(1);
    });
    fireEvent.click(screen.getAllByTestId("tiptap-content-picker-row-text")[0]);
    const use = screen.getByTestId("tiptap-content-picker-use-text");
    await waitFor(() => {
      expect(use.hasAttribute("disabled")).toBe(false);
    });
    fireEvent.click(use);
    expect(onPick).toHaveBeenCalledTimes(1);
    const selection = onPick.mock.calls[0][0] as ContentPickerSelection;
    expect(selection).toEqual({
      contentId: 78,
      path: "/Sites/News/Story",
      name: "Story",
    });
    expect(onClose).not.toHaveBeenCalled();
  });

  it("searches CMS content and picks a hit", async () => {
    const onPick = vi.fn();
    render(
      <ContentPickerDialog
        name="text"
        mode="image"
        onPick={onPick}
        onClose={() => undefined}
        listFolder={() => Promise.resolve([])}
        searchContent={() =>
          Promise.resolve({
            children: [
              {
                id: "88",
                title: "Logo",
                name: "logo.png",
                folderPath: "/Assets/Images",
                type: "asset",
              },
            ],
            totalCount: 1,
            startIndex: 1,
          })
        }
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("tiptap-content-picker-text")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("tiptap-content-picker-search-text"), {
      target: { value: "logo" },
    });
    fireEvent.click(
      screen.getByTestId("tiptap-content-picker-search-button-text"),
    );
    await waitFor(() => {
      expect(
        screen.getAllByTestId("tiptap-content-picker-hit-text"),
      ).toHaveLength(1);
    });
    fireEvent.click(screen.getAllByTestId("tiptap-content-picker-hit-text")[0]);
    fireEvent.click(screen.getByTestId("tiptap-content-picker-use-text"));
    expect(onPick).toHaveBeenCalledTimes(1);
    expect(onPick.mock.calls[0][0]).toEqual({
      contentId: 88,
      path: "/Assets/Images/logo.png",
      name: "Logo",
    });
  });

  it("shows lookup failures without closing", async () => {
    const onPick = vi.fn();
    const onClose = vi.fn();
    render(
      <ContentPickerDialog
        name="text"
        mode="link"
        onPick={onPick}
        onClose={onClose}
        listFolder={() => Promise.reject(new Error("nope"))}
        searchContent={() => Promise.reject(new Error("nope"))}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("tiptap-content-picker-error-text"),
      ).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("tiptap-content-picker-search-text"), {
      target: { value: "q" },
    });
    fireEvent.click(
      screen.getByTestId("tiptap-content-picker-search-button-text"),
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("tiptap-content-picker-search-error-text"),
      ).toBeTruthy();
    });
    expect(onPick).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("tiptap-content-picker-close-text"));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
