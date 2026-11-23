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
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getSite, updateSite } from "@/api/developer/sitesApi";
import { SiteFolderRootPanel } from "@/publishing/components/SiteFolderRootPanel";

vi.mock("@/api/developer/sitesApi", () => ({
  getSite: vi.fn(),
  updateSite: vi.fn(),
}));

const getSiteMock = getSite as ReturnType<typeof vi.fn>;
const updateSiteMock = updateSite as ReturnType<typeof vi.fn>;

/**
 * Single wall-clock budget for the folder-root round trips (#7152).
 * Every round trip here is purely microtask-driven (resolved `getSite` /
 * `updateSite` promises → state commit → re-render), so the test drains
 * them deterministically with {@code act} and keeps exactly one polling
 * gate as a backstop for loaded-worker scheduling lag — never the 1s
 * testing-library defaults that expire under full-suite CPU contention
 * (same family as #7122). Every {@code waitFor}/{@code findBy} in this
 * file uses it explicitly.
 */
const SITE_FOLDER_ROOT_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7152, same load-contention rationale as
 * {@code CONTEXT_MENU_RETRY_TEST_TIMEOUT} in #7122). The RTL budgets above
 * can jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (act drains + bounded
 * single backstops); this ceiling is strictly a last-resort backstop.
 */
const SITE_FOLDER_ROOT_TEST_TIMEOUT = 30_000;

describe("SiteFolderRootPanel", () => {
  beforeEach(() => {
    getSiteMock.mockReset();
    updateSiteMock.mockReset();
    getSiteMock.mockResolvedValue({
      name: "NightlySite",
      folderRoot: "//Sites/Old",
      description: "keep",
      baseUrl: "https://keep.example",
    });
  });

  it(
    "shows the saved path after PUT and a remount refresh",
    { timeout: SITE_FOLDER_ROOT_TEST_TIMEOUT },
    async () => {
      updateSiteMock.mockResolvedValue({
        name: "NightlySite",
        folderRoot: "//Sites/Nightly",
      });
      const { unmount } = render(
        <SiteFolderRootPanel siteName="NightlySite" />,
      );
      // Single backstop gate for the initial load terminal state (#7152):
      // the panel renders immediately with a placeholder, so a bare
      // findByTestId resolves before the getSite promise settles and the
      // text assertion races the re-render. Polling on the text itself is
      // deterministic regardless of worker scheduling.
      await waitFor(
        () => {
          expect(
            screen.getByTestId("publish-site-folder-root"),
          ).toHaveTextContent("//Sites/Old");
        },
        { timeout: SITE_FOLDER_ROOT_TIMEOUT },
      );
      fireEvent.click(screen.getByTestId("publish-site-folder-root-edit"));
      fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
        target: { value: "  \\\\Sites\\Nightly  " },
      });
      fireEvent.click(screen.getByTestId("publish-site-folder-root-save"));
      await waitFor(
        () => {
          expect(updateSiteMock).toHaveBeenCalledWith("NightlySite", {
            name: "NightlySite",
            folderRoot: "//Sites/Nightly",
          });
        },
        { timeout: SITE_FOLDER_ROOT_TIMEOUT },
      );
      // Deterministic drain (#7152): the PUT round trip is purely
      // microtask-driven — the resolved updateSite promise settles and the
      // saved state commits — with no real timers involved. Flushing with
      // act instead of asserting synchronously removes the starvation cliff.
      await act(async () => {});
      // Single backstop gate for the post-save terminal state: saved text,
      // success notice, and the closed form.
      await waitFor(
        () => {
          expect(
            screen.getByTestId("publish-site-folder-root"),
          ).toHaveTextContent("//Sites/Nightly");
          expect(
            screen.getByTestId("publish-site-folder-root-saved"),
          ).toBeTruthy();
          expect(
            screen.queryByTestId("publish-site-folder-root-form"),
          ).toBeNull();
        },
        { timeout: SITE_FOLDER_ROOT_TIMEOUT },
      );

      unmount();
      getSiteMock.mockResolvedValue({
        name: "NightlySite",
        folderRoot: "//Sites/Nightly",
      });
      render(<SiteFolderRootPanel siteName="NightlySite" />);
      // Deterministic drain for the remount fetch, then a single backstop
      // gate on the refreshed text — the exact gate that flaked under
      // full-suite load with the 1s findBy default (#7152).
      await act(async () => {});
      await waitFor(
        () => {
          expect(
            screen.getByTestId("publish-site-folder-root"),
          ).toHaveTextContent("//Sites/Nightly");
        },
        { timeout: SITE_FOLDER_ROOT_TIMEOUT },
      );
    },
  );

  it(
    "does not PUT on cancel, unchanged text, empty, or an unsafe path",
    { timeout: SITE_FOLDER_ROOT_TEST_TIMEOUT },
    async () => {
      render(<SiteFolderRootPanel siteName="NightlySite" />);
      await screen.findByText("//Sites/Old", undefined, {
        timeout: SITE_FOLDER_ROOT_TIMEOUT,
      });
      fireEvent.click(screen.getByTestId("publish-site-folder-root-edit"));
      fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
        target: { value: "//Sites/Other" },
      });
      fireEvent.click(screen.getByTestId("publish-site-folder-root-cancel"));
      expect(updateSiteMock).not.toHaveBeenCalled();
      expect(screen.getByTestId("publish-site-folder-root")).toHaveTextContent(
        "//Sites/Old",
      );
      expect(screen.queryByTestId("publish-site-folder-root-form")).toBeNull();

      fireEvent.click(screen.getByTestId("publish-site-folder-root-edit"));
      fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
        target: { value: "  \\\\Sites\\Old  " },
      });
      fireEvent.click(screen.getByTestId("publish-site-folder-root-save"));
      expect(updateSiteMock).not.toHaveBeenCalled();
      expect(screen.queryByTestId("publish-site-folder-root-saved")).toBeNull();

      fireEvent.click(screen.getByTestId("publish-site-folder-root-edit"));
      fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
        target: { value: "   " },
      });
      fireEvent.click(screen.getByTestId("publish-site-folder-root-save"));
      expect(updateSiteMock).not.toHaveBeenCalled();
      expect(
        screen
          .getByTestId("publish-site-folder-root-error")
          .textContent?.toLowerCase(),
      ).toContain("empty");
      expect(screen.queryByTestId("publish-site-folder-root-saved")).toBeNull();
      expect(screen.getByTestId("publish-site-folder-root-form")).toBeTruthy();

      fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
        target: { value: "//Sites/../etc" },
      });
      fireEvent.click(screen.getByTestId("publish-site-folder-root-save"));
      expect(updateSiteMock).not.toHaveBeenCalled();
      expect(
        screen
          .getByTestId("publish-site-folder-root-error")
          .textContent?.toLowerCase(),
      ).toContain("invalid");
      expect(screen.queryByTestId("publish-site-folder-root-saved")).toBeNull();
      expect(screen.getByTestId("publish-site-folder-root")).toHaveTextContent(
        "//Sites/Old",
      );
    },
  );

  it.each([
    [400, "was not accepted"],
    [403, "not allowed"],
    [409, "conflicts"],
  ])(
    "keeps HTTP %s on the form without a success notice",
    { timeout: SITE_FOLDER_ROOT_TEST_TIMEOUT },
    async (status, snippet) => {
      updateSiteMock.mockRejectedValue({ status, statusText: "no", body: {} });
      render(<SiteFolderRootPanel siteName="NightlySite" />);
      await screen.findByText("//Sites/Old", undefined, {
        timeout: SITE_FOLDER_ROOT_TIMEOUT,
      });
      fireEvent.click(screen.getByTestId("publish-site-folder-root-edit"));
      fireEvent.change(screen.getByTestId("publish-site-folder-root-input"), {
        target: { value: "//Sites/Blocked" },
      });
      fireEvent.click(screen.getByTestId("publish-site-folder-root-save"));
      // Explicit budget (never the 1s findBy default): the rejection round
      // trip plus the error render can exceed a second when hundreds of
      // suite files share the worker host (#7152).
      const alert = await screen.findByTestId(
        "publish-site-folder-root-error",
        undefined,
        {
          timeout: SITE_FOLDER_ROOT_TIMEOUT,
        },
      );
      expect(alert.textContent?.toLowerCase()).toContain(snippet);
      expect(screen.getByTestId("publish-site-folder-root-form")).toBeTruthy();
      expect(screen.getByTestId("publish-site-folder-root")).toHaveTextContent(
        "//Sites/Old",
      );
      expect(screen.queryByTestId("publish-site-folder-root-saved")).toBeNull();
    },
  );
});
