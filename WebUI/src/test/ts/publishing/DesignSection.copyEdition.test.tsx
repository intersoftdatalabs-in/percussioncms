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
import { describe, expect, it, vi } from "vitest";
import type { EditionCopiedInfo } from "@/publishing/design/EditionEditor";
import { DesignSection } from "@/publishing/sections/DesignSection";

const listEditionsBySite = vi.fn();

vi.mock("@/api/home/homeApi", () => ({
  fetchSites: vi.fn().mockResolvedValue([
    { name: "SiteA", siteId: "1" },
    { name: "SiteB", siteId: "2" },
  ]),
}));

vi.mock("@/api/publishing/designApi", () => ({
  listEditionsBySite: (...args: unknown[]) => listEditionsBySite(...args),
  listContentLists: vi.fn().mockResolvedValue([]),
  listContexts: vi.fn().mockResolvedValue([]),
  listDeliveryTypes: vi.fn().mockResolvedValue([]),
  listSchemesForContext: vi.fn().mockResolvedValue([]),
  listDesignSites: vi
    .fn()
    .mockResolvedValue([{ siteId: "1", name: "S1", folderRoot: "//Sites/S1" }]),
  listSiteProperties: vi.fn().mockResolvedValue([]),
  deleteSiteProperty: vi.fn(),
  putSiteProperty: vi.fn(),
}));

vi.mock("@/publishing/design/EditionEditor", () => ({
  EditionEditor: ({
    onCopied,
  }: {
    onCopied?: (info: EditionCopiedInfo) => void;
  }) => (
    <div>
      <button
        type="button"
        data-testid="fake-copy-other"
        onClick={() =>
          onCopied?.({ targetSiteId: "2", name: "Copied Ed" })
        }
      >
        copy other
      </button>
      <button
        type="button"
        data-testid="fake-copy-same"
        onClick={() =>
          onCopied?.({ targetSiteId: "1", name: "Same Site Copy" })
        }
      >
        copy same
      </button>
    </div>
  ),
}));

describe("DesignSection copy edition", () => {
  it("shows the copy on the selected target site", async () => {
    listEditionsBySite.mockImplementation(async (siteId: string) => {
      if (siteId === "2") {
        return [{ editionId: "88", name: "Copied Ed", siteId: "2" }];
      }
      return [{ editionId: "9", name: "Ed1", siteId: "1" }];
    });
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    fireEvent.click(await screen.findByTestId("design-edition-9"));
    fireEvent.click(screen.getByTestId("fake-copy-other"));
    expect(await screen.findByTestId("design-edition-88")).toHaveTextContent(
      "Copied Ed",
    );
    await waitFor(() =>
      expect(screen.getByLabelText("Design site")).toHaveValue("2"),
    );
  });

  it("reloads the current site list after a same-site copy", async () => {
    let copied = false;
    listEditionsBySite.mockImplementation(async (siteId: string) => {
      if (siteId !== "1") {
        return [];
      }
      if (copied) {
        return [
          { editionId: "9", name: "Ed1", siteId: "1" },
          { editionId: "10", name: "Same Site Copy", siteId: "1" },
        ];
      }
      return [{ editionId: "9", name: "Ed1", siteId: "1" }];
    });
    render(<DesignSection />);
    fireEvent.click(screen.getByRole("tab", { name: /Editions/i }));
    fireEvent.click(await screen.findByTestId("design-edition-9"));
    copied = true;
    fireEvent.click(screen.getByTestId("fake-copy-same"));
    expect(await screen.findByTestId("design-edition-10")).toHaveTextContent(
      "Same Site Copy",
    );
    expect(screen.getByLabelText("Design site")).toHaveValue("1");
  });
});
