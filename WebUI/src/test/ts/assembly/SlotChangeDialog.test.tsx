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
import type { SlotCanvasSlot } from "../../../main/ts/api/contentExplorer/slotRelationshipApi";
import { SlotChangeDialog } from "../../../main/ts/assembly/SlotChangeDialog";

const slots: SlotCanvasSlot[] = [
  { slotId: 1, name: "slot-a", label: "Slot A", items: [] },
  { slotId: 2, name: "slot-b", label: "Slot B", items: [] },
];

describe("SlotChangeDialog", () => {
  it("clears a load-failure notice after a later successful template load", async () => {
    const loadTemplates = vi
      .fn()
      .mockRejectedValueOnce(new Error("boom"))
      .mockResolvedValueOnce([{ id: 7, name: "rffSnTitle", label: "Title" }]);

    render(
      <SlotChangeDialog
        slots={slots}
        initialSlotId={1}
        loadTemplates={loadTemplates}
        onCancel={() => undefined}
        onApply={() => undefined}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-change-notice")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("assembly-slot-change-slot"), {
      target: { value: "2" },
    });

    await waitFor(() => {
      expect(screen.queryByTestId("assembly-slot-change-notice")).toBeNull();
    });
    expect(
      (screen.getByTestId("assembly-slot-change-template") as HTMLSelectElement)
        .value,
    ).toBe("7");
  });
});
