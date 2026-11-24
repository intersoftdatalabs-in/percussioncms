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
import { describe, expect, it, vi } from "vitest";
import {
  FolderSecurityHost,
  FolderSecurityMountBoundary,
} from "../../../main/ts/contentExplorer/FolderSecurityHost";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import { fallbackLabelFromKey } from "../../../main/ts/i18n/message";

/**
 * Single wall-clock budget for every polling gate in this file (#7226).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const FOLDER_SECURITY_HOST_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const FOLDER_SECURITY_HOST_TEST_TIMEOUT = 30_000;

describe("FolderSecurityHost (#3268 / #2749)", () => {
  it("shows the no-folder placeholder when folderId is missing", () => {
    render(<FolderSecurityHost folderId="" currentUserIdentities={["Admin"]} />);
    const hint = screen.getByTestId("perc-folder-security-no-folder");
    expect(hint).toBeTruthy();
    expect(hint.textContent).toBe(
      fallbackLabelFromKey(EXPLORER_MSG.SECURITY_HOST_NO_FOLDER),
    );
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

  it("surfaces a structured error when load rejects (no chrome crash)", { timeout: FOLDER_SECURITY_HOST_TEST_TIMEOUT }, async () => {
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
    },
      { timeout: FOLDER_SECURITY_HOST_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("folder-security-loading")).toBeNull();
  });

  it("FolderSecurityMountBoundary surfaces render-time child errors", () => {
    const boom = vi.spyOn(console, "error").mockImplementation(() => undefined);
    try {
      function Boom(): never {
        throw new Error("panel render failed");
      }
      render(
        <FolderSecurityMountBoundary>
          <Boom />
        </FolderSecurityMountBoundary>,
      );
      expect(screen.getByTestId("folder-security-error")).toBeTruthy();
      expect(screen.getByText("panel render failed")).toBeTruthy();
    } finally {
      boom.mockRestore();
    }
  });

  it("mounts FolderSecurityPanel when load resolves", { timeout: FOLDER_SECURITY_HOST_TEST_TIMEOUT }, async () => {
    render(
      <FolderSecurityHost
        folderId="16777215-101-703"
        currentUserIdentities={["Admin"]}
        load={async () => ({
          id: "16777215-101-703",
          name: "Sites",
          permission: {
            accessLevel: "ADMIN",
            adminPrincipals: [{ type: "USER", name: "Admin" }],
            writePrincipals: [],
            readPrincipals: [],
            viewPrincipals: [],
          },
        })}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("folder-security-panel")).toBeTruthy();
    },
      { timeout: FOLDER_SECURITY_HOST_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("folder-security-loading")).toBeNull();
    expect(screen.queryByTestId("folder-security-error")).toBeNull();
    expect(screen.getByText(/Sites/)).toBeTruthy();
  });
});
