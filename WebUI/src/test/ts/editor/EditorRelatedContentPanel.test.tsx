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

import { cleanup, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

const fetchSlotCanvas = vi.fn().mockResolvedValue({
  ownerId: 42,
  templateId: null,
  slots: [],
});
const fetchLocal = vi.fn().mockResolvedValue({ count: 0, links: [] });

vi.mock("../../../main/ts/api/contentExplorer/slotRelationshipApi", () => ({
  fetchSlotCanvas: (...args: unknown[]) => fetchSlotCanvas(...args),
}));

vi.mock("../../../main/ts/api/contentExplorer/relationshipsApi", () => ({
  fetchLocal: (...args: unknown[]) => fetchLocal(...args),
}));

import { EditorRelatedContentPanel } from "../../../main/ts/editor/EditorRelatedContentPanel";

describe("EditorRelatedContentPanel", () => {
  afterEach(() => {
    cleanup();
    fetchSlotCanvas.mockClear();
    fetchLocal.mockClear();
  });

  it("does not refetch when parent rerenders with default loaders", async () => {
    const { rerender } = render(<EditorRelatedContentPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-empty")).toBeTruthy();
    });
    expect(fetchSlotCanvas).toHaveBeenCalledTimes(1);
    expect(fetchLocal).toHaveBeenCalledTimes(1);
    rerender(<EditorRelatedContentPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-empty")).toBeTruthy();
    });
    expect(fetchSlotCanvas).toHaveBeenCalledTimes(1);
    expect(fetchLocal).toHaveBeenCalledTimes(1);
  });
});
