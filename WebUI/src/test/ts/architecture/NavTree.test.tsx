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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { NavTree } from "../../../main/ts/architecture/NavTree";
import type { NavTreeNode } from "../../../main/ts/api/architecture/types";

const sampleRoot: NavTreeNode = {
  id: "root",
  title: "Home",
  folderPath: "//Sites/Demo",
  sectionType: "section",
  requiresLogin: false,
  children: [
    {
      id: "link-1",
      title: "Partner",
      folderPath: null,
      sectionType: "sectionlink",
      requiresLogin: false,
      children: [],
    },
    {
      id: "ext-1",
      title: "Docs",
      folderPath: null,
      sectionType: "externallink",
      requiresLogin: true,
      children: [],
    },
  ],
};

describe("NavTree (#3095)", () => {
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

  it("renders role=tree with treeitems and type badges", () => {
    render(<NavTree root={sampleRoot} />);
    const tree = screen.getByRole("tree");
    expect(tree).toBeTruthy();
    expect(screen.getByTestId("nav-tree-item-root")).toBeTruthy();
    expect(screen.getByTestId("nav-tree-badge-link-1").textContent).toMatch(
      /section link/i,
    );
    expect(screen.getByTestId("nav-tree-badge-ext-1").textContent).toMatch(
      /external/i,
    );
    expect(screen.getByTestId("nav-tree-secure-ext-1")).toBeTruthy();
  });

  it("reports selection and collapses via toggle", () => {
    const onSelect = vi.fn();
    render(
      <NavTree root={sampleRoot} selectedId="root" onSelect={onSelect} />,
    );
    fireEvent.click(screen.getByTestId("nav-tree-item-link-1"));
    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ id: "link-1" }),
    );
    // Collapse root
    fireEvent.click(screen.getByTestId("nav-tree-toggle-root"));
    expect(screen.queryByTestId("nav-tree-item-link-1")).toBeNull();
  });

  it("shows loading, error, and empty states without role=tree", () => {
    const { rerender } = render(<NavTree root={null} loading />);
    expect(screen.getByTestId("architecture-nav-tree-loading")).toBeTruthy();
    expect(screen.queryByRole("tree")).toBeNull();

    rerender(<NavTree root={null} error="Boom" />);
    expect(screen.getByTestId("architecture-nav-tree-error").textContent).toBe(
      "Boom",
    );
    expect(screen.queryByRole("tree")).toBeNull();

    rerender(<NavTree root={null} />);
    expect(screen.getByTestId("architecture-nav-tree-empty")).toBeTruthy();
  });
});
