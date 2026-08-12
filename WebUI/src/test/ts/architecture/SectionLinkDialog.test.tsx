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
import { SectionLinkDialog } from "../../../main/ts/architecture/SectionLinkDialog";
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
  ],
};

describe("SectionLinkDialog (#3097)", () => {
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

  it("requires a target before submit", () => {
    const onSubmit = vi.fn();
    render(
      <SectionLinkDialog
        open
        mode="create"
        parentId="root"
        parentTitle="Home"
        treeRoot={tree}
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-section-link-submit"));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByTestId("architecture-section-link-error")).toBeTruthy();
  });

  it("picks nested target via tree picker and submits", async () => {
    const onSubmit = vi.fn();
    render(
      <SectionLinkDialog
        open
        mode="create"
        parentId="root"
        parentTitle="Home"
        treeRoot={tree}
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-section-link-browse"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-tree-picker-dialog")).toBeTruthy();
    });
    // Expand News to reach nested Press (valid: not a direct child of root)
    const toggle = screen.queryByTestId("nav-tree-toggle-b");
    if (toggle) {
      fireEvent.click(toggle);
    }
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-b1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-b1"));
    fireEvent.click(screen.getByTestId("architecture-tree-picker-confirm"));
    await waitFor(() => {
      expect(
        (
          screen.getByTestId(
            "architecture-section-link-target",
          ) as HTMLInputElement
        ).value,
      ).toMatch(/Press/i);
    });
    fireEvent.click(screen.getByTestId("architecture-section-link-submit"));
    expect(onSubmit).toHaveBeenCalledWith("b1");
  });

  it("rejects direct child as target under parent", async () => {
    const onSubmit = vi.fn();
    render(
      <SectionLinkDialog
        open
        mode="create"
        parentId="root"
        parentTitle="Home"
        treeRoot={tree}
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-section-link-browse"));
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-a")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-a"));
    fireEvent.click(screen.getByTestId("architecture-tree-picker-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("architecture-section-link-error"),
      ).toBeTruthy();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("edit mode same target no-ops via onCancel (not onSubmit)", async () => {
    const onSubmit = vi.fn();
    const onCancel = vi.fn();
    render(
      <SectionLinkDialog
        open
        mode="edit"
        parentId="root"
        parentTitle="Home"
        treeRoot={tree}
        // linkSectionId equals chosen target → same-target branch
        linkSectionId="b1"
        busy={false}
        onCancel={onCancel}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-section-link-browse"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-tree-picker-dialog")).toBeTruthy();
    });
    const toggle = screen.queryByTestId("nav-tree-toggle-b");
    if (toggle) {
      fireEvent.click(toggle);
    }
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-b1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-b1"));
    fireEvent.click(screen.getByTestId("architecture-tree-picker-confirm"));
    await waitFor(() => {
      expect(
        (
          screen.getByTestId(
            "architecture-section-link-target",
          ) as HTMLInputElement
        ).value,
      ).toMatch(/Press/i);
    });
    fireEvent.click(screen.getByTestId("architecture-section-link-submit"));
    expect(onCancel).toHaveBeenCalled();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("edit mode can submit a new non-child target", async () => {
    const onSubmit = vi.fn();
    render(
      <SectionLinkDialog
        open
        mode="edit"
        parentId="root"
        parentTitle="Home"
        treeRoot={tree}
        linkSectionId="link-old"
        busy={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-section-link-browse"));
    await waitFor(() => {
      expect(screen.getByTestId("architecture-tree-picker-dialog")).toBeTruthy();
    });
    const toggle = screen.queryByTestId("nav-tree-toggle-b");
    if (toggle) {
      fireEvent.click(toggle);
    }
    await waitFor(() => {
      expect(screen.getByTestId("nav-tree-item-b1")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("nav-tree-item-b1"));
    fireEvent.click(screen.getByTestId("architecture-tree-picker-confirm"));
    await waitFor(() => {
      expect(
        (
          screen.getByTestId(
            "architecture-section-link-target",
          ) as HTMLInputElement
        ).value,
      ).toMatch(/Press/i);
    });
    fireEvent.click(screen.getByTestId("architecture-section-link-submit"));
    expect(onSubmit).toHaveBeenCalledWith("b1");
  });

  it("disables browse/submit while busy", () => {
    render(
      <SectionLinkDialog
        open
        mode="create"
        parentId="root"
        parentTitle="Home"
        treeRoot={tree}
        busy
        onCancel={() => undefined}
        onSubmit={() => undefined}
      />,
    );
    expect(
      (
        screen.getByTestId(
          "architecture-section-link-submit",
        ) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
    expect(
      (
        screen.getByTestId(
          "architecture-section-link-browse",
        ) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
    expect(
      (
        screen.getByTestId(
          "architecture-section-link-cancel",
        ) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
  });
});
