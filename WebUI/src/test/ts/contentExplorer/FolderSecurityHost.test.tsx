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

import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { FolderSecurityHost } from "../../../main/ts/contentExplorer/FolderSecurityHost";

describe("FolderSecurityHost (#3268 / #2749)", () => {
  it("shows the no-folder placeholder when folderId is missing", () => {
    render(<FolderSecurityHost folderId="" currentUserIdentities={["Admin"]} />);
    expect(screen.getByTestId("perc-folder-security-no-folder")).toBeTruthy();
    expect(screen.queryByTestId("folder-security-loading")).toBeNull();
    expect(screen.queryByTestId("folder-security-panel")).toBeNull();
  });

  it("shows the no-folder placeholder when folderId is only whitespace", () => {
    render(
      <FolderSecurityHost folderId="   " currentUserIdentities={["Admin"]} />,
    );
    expect(screen.getByTestId("perc-folder-security-no-folder")).toBeTruthy();
  });

  it("mounts FolderSecurityPanel loading surface when folderId is set", () => {
    render(
      <FolderSecurityHost
        folderId="16777215-101-703"
        currentUserIdentities={["Admin"]}
        load={() => new Promise(() => undefined)}
      />,
    );
    expect(screen.getByTestId("folder-security-loading")).toBeTruthy();
    expect(screen.queryByTestId("perc-mcol")).toBeNull();
    expect(document.querySelector(".perc-mcol")).toBeNull();
  });

  it("surfaces a structured error when load rejects (no chrome crash)", async () => {
    render(
      <FolderSecurityHost
        folderId="0"
        currentUserIdentities={["Admin"]}
        load={async () => {
          throw new Error("validated object is null");
        }}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("folder-security-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("folder-security-loading")).toBeNull();
  });
});
