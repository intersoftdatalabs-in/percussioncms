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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MoveSectionDialog } from "../../../main/ts/architecture/MoveSectionDialog";
import type { NavTreeNode } from "../../../main/ts/api/architecture/types";

const tree: NavTreeNode = {
  id: "root",
  title: "Home",
  folderPath: "//Sites/Demo",
  sectionType: "section",
  requiresLogin: false,
  children: [
    {
      id: "a",
      title: "About",
      folderPath: "//Sites/Demo/About",
      sectionType: "section",
      requiresLogin: false,
      children: [],
    },
    {
      id: "b",
      title: "News",
      folderPath: "//Sites/Demo/News",
      sectionType: "section",
      requiresLogin: false,
      children: [
        {
          id: "b1",
          title: "Press",
          folderPath: "//Sites/Demo/News/Press",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
      ],
    },
    {
      id: "c",
      title: "Partner",
      folderPath: "//Sites/Demo/Partner",
      sectionType: "externallink",
      requiresLogin: false,
      children: [],
    },
  ],
};

describe("MoveSectionDialog (#3349)", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  afterEach(() => {
    cleanup();
  });

  it("cancel does not submit", () => {
    const onSubmit = vi.fn();
    const onCancel = vi.fn();
    render(
      <MoveSectionDialog
        open
        sourceId="a"
        sourceTitle="About"
        treeRoot={tree}
        busy={false}
        onCancel={onCancel}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-move-cancel"));
    expect(onCancel).toHaveBeenCalled();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("requires a parent before submit", () => {
    const onSubmit = vi.fn();
    render(
      <MoveSectionDialog
        open
        sourceId="a"
        sourceTitle="About"
        treeRoot={tree}
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-move-submit"));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByTestId("architecture-move-error").textContent).toMatch(
      /Select a parent/i,
    );
  });

  it("picks a parent via tree picker and submits append index", async () => {
    const onSubmit = vi.fn();
    render(
      <MoveSectionDialog
        open
        sourceId="a"
        sourceTitle="About"
        treeRoot={tree}
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-move-browse"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-tree-picker-dialog")).toBeTruthy();
    });
    // Source and its descendants are omitted from the picker.
    expect(screen.queryByTestId("nav-tree-item-a")).toBeNull();
    fireEvent.click(screen.getByTestId("nav-tree-item-b"));
    fireEvent.click(screen.getByTestId("architecture-tree-picker-confirm"));
    await waitFor(() => {
      expect(
        (screen.getByTestId("architecture-move-parent") as HTMLInputElement)
          .value,
      ).toMatch(/News/i);
    });
    fireEvent.click(screen.getByTestId("architecture-move-submit"));
    expect(onSubmit).toHaveBeenCalledWith("b", -1);
  });

  it("shows a clear message for an invalid target (no submit)", async () => {
    const onSubmit = vi.fn();
    render(
      <MoveSectionDialog
        open
        sourceId="a"
        sourceTitle="About"
        treeRoot={tree}
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-move-browse"));
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-c")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-c"));
    fireEvent.click(screen.getByTestId("architecture-tree-picker-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-move-error").textContent).toMatch(
        /cannot be the new parent/i,
      );
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
