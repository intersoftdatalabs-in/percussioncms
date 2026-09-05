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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as api from "../../../main/ts/api/developer/fileExplorerApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { FileExplorerPanel } from "../../../main/ts/developer/FileExplorerPanel";

vi.mock("../../../main/ts/api/developer/fileExplorerApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/fileExplorerApi")>();
  return {
    ...actual,
    listFileExplorerRoots: vi.fn(),
    listFileExplorerChildren: vi.fn(),
  };
});

const listFileExplorerRoots = api.listFileExplorerRoots as ReturnType<typeof vi.fn>;
const listFileExplorerChildren = api.listFileExplorerChildren as ReturnType<typeof vi.fn>;

describe("FileExplorerPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listFileExplorerRoots.mockReset();
    listFileExplorerChildren.mockReset();
  });

  it("lists roots and drills into a directory then back", async () => {
    listFileExplorerRoots.mockResolvedValue([
      { id: "rx_resources", displayName: "rx_resources", exists: true },
    ]);
    listFileExplorerChildren.mockImplementation(async (_id: string, path?: string | null) => {
      if (!path) {
        return [
          { name: "css", relativePath: "css", directory: true },
          { name: "readme.txt", relativePath: "readme.txt", directory: false, size: 40 },
        ];
      }
      if (path === "css") {
        return [{ name: "theme.css", relativePath: "css/theme.css", directory: false, size: 8 }];
      }
      return [];
    });
    render(<FileExplorerPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-roots-table")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-fe-open-root"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-children-table")).toBeTruthy();
    });
    expect(listFileExplorerChildren).toHaveBeenCalledWith("rx_resources", "");
    expect(screen.getByTestId("developer-fe-file-name").textContent).toBe("readme.txt");
    fireEvent.click(screen.getByTestId("developer-fe-open-dir"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-file-name").textContent).toBe("theme.css");
    });
    expect(listFileExplorerChildren).toHaveBeenCalledWith("rx_resources", "css");
    fireEvent.click(screen.getByTestId("developer-fe-up"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-open-dir")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-fe-back-roots"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-roots-table")).toBeTruthy();
    });
  });

  it("shows empty state when no roots are configured", async () => {
    listFileExplorerRoots.mockResolvedValue([]);
    render(<FileExplorerPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-fe-empty").textContent).toBe(DEV_MSG.FE_EMPTY);
  });

  it("shows empty children when a root has no entries", async () => {
    listFileExplorerRoots.mockResolvedValue([
      { id: "drop", displayName: "Drop", exists: true },
    ]);
    listFileExplorerChildren.mockResolvedValue([]);
    render(<FileExplorerPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-open-root")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-fe-open-root"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-children-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listFileExplorerRoots.mockRejectedValue(new SessionRedirectError());
    render(<FileExplorerPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-fe-error").textContent).toBe(DEV_MSG.SESSION_REDIRECT);
  });

  it("shows children error without leaving browse chrome", async () => {
    listFileExplorerRoots.mockResolvedValue([
      { id: "drop", displayName: "Drop", exists: false },
    ]);
    listFileExplorerChildren.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "Path not found" },
    });
    render(<FileExplorerPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-open-root")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-fe-open-root"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-fe-children-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-fe-browse")).toBeTruthy();
    expect(screen.getByTestId("developer-fe-children-error").textContent).toContain(
      DEV_MSG.FE_CHILDREN_ERROR,
    );
  });
});
