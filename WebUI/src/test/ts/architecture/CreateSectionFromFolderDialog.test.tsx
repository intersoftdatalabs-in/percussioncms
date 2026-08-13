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
import { CreateSectionFromFolderDialog } from "../../../main/ts/architecture/CreateSectionFromFolderDialog";

describe("CreateSectionFromFolderDialog (#3302)", () => {
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

  it("requires folder path and landing page name", () => {
    const onSubmit = vi.fn();
    render(
      <CreateSectionFromFolderDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        useContentBrowser={false}
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-from-folder-submit"));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByTestId("architecture-from-folder-error")).toBeTruthy();
    fireEvent.change(screen.getByTestId("architecture-from-folder-path"), {
      target: { value: "//Sites/Demo/Folder" },
    });
    fireEvent.click(screen.getByTestId("architecture-from-folder-submit"));
    expect(onSubmit).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("architecture-from-folder-page"), {
      target: { value: "index.html" },
    });
    fireEvent.click(screen.getByTestId("architecture-from-folder-submit"));
    expect(onSubmit).toHaveBeenCalledWith({
      sourceFolderPath: "//Sites/Demo/Folder",
      pageName: "index.html",
    });
  });

  it("does not render when closed", () => {
    render(
      <CreateSectionFromFolderDialog
        open={false}
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        useContentBrowser={false}
        onCancel={() => undefined}
        onSubmit={() => undefined}
      />,
    );
    expect(screen.queryByTestId("architecture-from-folder-dialog")).toBeNull();
  });
});
