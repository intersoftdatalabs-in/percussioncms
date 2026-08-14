/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
import { SectionPropertiesDialog } from "../../../main/ts/architecture/SectionPropertiesDialog";
import type { SiteSectionPropertiesWire } from "../../../main/ts/api/architecture/types";

const loaded: SiteSectionPropertiesWire = {
  id: "g1",
  title: "About",
  folderName: "About",
  target: "_self",
  cssClassNames: "nav-about",
  requiresLogin: false,
  allowAccessTo: "",
  secureSite: true,
  secureAncestor: false,
  siteRootSection: false,
  folderPermission: { accessLevel: "WRITE" },
};

describe("SectionPropertiesDialog (#3353)", () => {
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

  it("does not render when closed", () => {
    render(
      <SectionPropertiesDialog
        open={false}
        busy={false}
        loading={false}
        loadError={null}
        initial={loaded}
        onCancel={() => undefined}
        onSubmit={() => undefined}
      />,
    );
    expect(screen.queryByTestId("architecture-properties-dialog")).toBeNull();
  });

  it("validates empty title before save", () => {
    const onSubmit = vi.fn();
    render(
      <SectionPropertiesDialog
        open
        busy={false}
        loading={false}
        loadError={null}
        initial={loaded}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.change(screen.getByTestId("architecture-properties-title"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("architecture-properties-submit"));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByTestId("architecture-properties-error")).toBeTruthy();
  });

  it("saves edited fields", () => {
    const onSubmit = vi.fn();
    render(
      <SectionPropertiesDialog
        open
        busy={false}
        loading={false}
        loadError={null}
        initial={loaded}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.change(screen.getByTestId("architecture-properties-title"), {
      target: { value: "About Us" },
    });
    fireEvent.change(screen.getByTestId("architecture-properties-folder"), {
      target: { value: "about-us" },
    });
    fireEvent.change(screen.getByTestId("architecture-properties-target"), {
      target: { value: "_blank" },
    });
    fireEvent.change(screen.getByTestId("architecture-properties-css"), {
      target: { value: "nav-about featured" },
    });
    fireEvent.click(screen.getByTestId("architecture-properties-login"));
    fireEvent.change(screen.getByTestId("architecture-properties-groups"), {
      target: { value: "Editors,Admins" },
    });
    fireEvent.click(screen.getByTestId("architecture-properties-submit"));
    expect(onSubmit).toHaveBeenCalledWith({
      title: "About Us",
      folderName: "about-us",
      target: "_blank",
      cssClassNames: "nav-about featured",
      requiresLogin: true,
      allowAccessTo: "Editors,Admins",
    });
  });

  it("cancel does not submit", () => {
    const onSubmit = vi.fn();
    const onCancel = vi.fn();
    render(
      <SectionPropertiesDialog
        open
        busy={false}
        loading={false}
        loadError={null}
        initial={loaded}
        onCancel={onCancel}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.change(screen.getByTestId("architecture-properties-title"), {
      target: { value: "Changed" },
    });
    fireEvent.click(screen.getByTestId("architecture-properties-cancel"));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(onCancel).toHaveBeenCalled();
  });

  it("locks folder name on site root and login when not secure", () => {
    const onSubmit = vi.fn();
    const root: SiteSectionPropertiesWire = {
      ...loaded,
      siteRootSection: true,
      secureSite: false,
      folderName: "Demo",
    };
    render(
      <SectionPropertiesDialog
        open
        busy={false}
        loading={false}
        loadError={null}
        initial={root}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    expect(
      (screen.getByTestId("architecture-properties-folder") as HTMLInputElement)
        .disabled,
    ).toBe(true);
    expect(
      screen.getByTestId("architecture-properties-folder-locked"),
    ).toBeTruthy();
    expect(
      (screen.getByTestId("architecture-properties-login") as HTMLInputElement)
        .disabled,
    ).toBe(true);
    fireEvent.change(screen.getByTestId("architecture-properties-title"), {
      target: { value: "Home" },
    });
    fireEvent.click(screen.getByTestId("architecture-properties-submit"));
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        title: "Home",
        folderName: "Demo",
      }),
    );
  });
});
