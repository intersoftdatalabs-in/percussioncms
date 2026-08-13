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

vi.mock("../../../main/ts/contentBrowser/ContentBrowser", () => ({
  ContentBrowser: (props: {
    onConfirm?: (selection: {
      items: Array<{ id: string; name: string; path: string }>;
    }) => void;
    onCancel?: () => void;
  }) => (
    <div data-testid="mock-content-browser">
      <button
        type="button"
        data-testid="mock-cb-confirm-folder"
        onClick={() =>
          props.onConfirm?.({
            items: [
              {
                id: "f1",
                name: "PickedFolder",
                path: "//Sites/Demo/PickedFolder",
              },
            ],
          })
        }
      >
        Confirm folder
      </button>
      <button
        type="button"
        data-testid="mock-cb-confirm-page"
        onClick={() =>
          props.onConfirm?.({
            items: [
              {
                id: "p1",
                name: "index.html",
                path: "//Sites/Demo/PickedFolder/index.html",
              },
            ],
          })
        }
      >
        Confirm page
      </button>
      <button
        type="button"
        data-testid="mock-cb-confirm-page-name-only"
        onClick={() =>
          props.onConfirm?.({
            items: [
              {
                id: "p2",
                name: "stale-risk.html",
                path: "stale-risk.html",
              },
            ],
          })
        }
      >
        Confirm name only
      </button>
      <button
        type="button"
        data-testid="mock-content-browser-cancel"
        onClick={() => props.onCancel?.()}
      >
        Cancel pick
      </button>
    </div>
  ),
}));

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

  it("folder and page ContentBrowser confirm populate fields", () => {
    const onSubmit = vi.fn();
    render(
      <CreateSectionFromFolderDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        useContentBrowser
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-from-folder-browse-folder"));
    expect(screen.getByTestId("mock-content-browser")).toBeTruthy();
    fireEvent.click(screen.getByTestId("mock-cb-confirm-folder"));
    expect(
      (screen.getByTestId("architecture-from-folder-path") as HTMLInputElement)
        .value,
    ).toBe("//Sites/Demo/PickedFolder");

    fireEvent.click(screen.getByTestId("architecture-from-folder-browse-page"));
    fireEvent.click(screen.getByTestId("mock-cb-confirm-page"));
    expect(
      (screen.getByTestId("architecture-from-folder-page") as HTMLInputElement)
        .value,
    ).toBe("index.html");
    fireEvent.click(screen.getByTestId("architecture-from-folder-submit"));
    expect(onSubmit).toHaveBeenCalledWith({
      sourceFolderPath: "//Sites/Demo/PickedFolder",
      pageName: "index.html",
    });
  });

  it("page-name-only confirm clears stale folder path", () => {
    const onSubmit = vi.fn();
    render(
      <CreateSectionFromFolderDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        useContentBrowser
        onCancel={() => undefined}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-from-folder-browse-folder"));
    fireEvent.click(screen.getByTestId("mock-cb-confirm-folder"));
    fireEvent.click(screen.getByTestId("architecture-from-folder-browse-page"));
    fireEvent.click(screen.getByTestId("mock-cb-confirm-page-name-only"));
    expect(
      (screen.getByTestId("architecture-from-folder-path") as HTMLInputElement)
        .value,
    ).toBe("");
    expect(
      (screen.getByTestId("architecture-from-folder-page") as HTMLInputElement)
        .value,
    ).toBe("stale-risk.html");
    fireEvent.click(screen.getByTestId("architecture-from-folder-submit"));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByTestId("architecture-from-folder-error")).toBeTruthy();
  });

  it("ContentBrowser cancel returns to form without submitting", () => {
    const onSubmit = vi.fn();
    const onCancel = vi.fn();
    render(
      <CreateSectionFromFolderDialog
        open
        siteName="Demo"
        parentTitle="Home"
        busy={false}
        useContentBrowser
        onCancel={onCancel}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.click(screen.getByTestId("architecture-from-folder-browse-folder"));
    expect(screen.getByTestId("mock-content-browser")).toBeTruthy();
    fireEvent.click(screen.getByTestId("mock-content-browser-cancel"));
    expect(screen.queryByTestId("mock-content-browser")).toBeNull();
    expect(screen.getByTestId("architecture-from-folder-dialog")).toBeTruthy();
    expect(onSubmit).not.toHaveBeenCalled();
    expect(onCancel).not.toHaveBeenCalled();
  });
});
