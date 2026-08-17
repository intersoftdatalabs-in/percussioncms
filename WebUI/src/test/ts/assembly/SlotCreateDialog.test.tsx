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
import { SlotCreateDialog } from "../../../main/ts/assembly/SlotCreateDialog";

describe("SlotCreateDialog", () => {
  it("apply without a folder does not emit a pick", async () => {
    const onApply = vi.fn();
    render(
      <SlotCreateDialog
        slotId={3}
        initialFolder=""
        loadTypes={async () => [{ id: 1, name: "percRichText", label: "Rich Text" }]}
        loadTemplates={async () => [{ id: 4, name: "rffSnTitle", label: "Title" }]}
        onCancel={() => undefined}
        onApply={onApply}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-create-type")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("assembly-slot-create-apply"));
    expect(onApply).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-slot-create-notice")).toBeTruthy();
  });

  it("apply emits type, folder, and snippet template", async () => {
    const onApply = vi.fn();
    render(
      <SlotCreateDialog
        slotId={3}
        initialFolder="/Sites/A"
        loadTypes={async () => [{ id: 1, name: "percRichText", label: "Rich Text" }]}
        loadTemplates={async () => [{ id: 4, name: "rffSnTitle", label: "Title" }]}
        onCancel={() => undefined}
        onApply={onApply}
      />,
    );
    await waitFor(() => {
      expect(
        (screen.getByTestId("assembly-slot-create-type") as HTMLSelectElement).value,
      ).toBe("percRichText");
    });
    fireEvent.click(screen.getByTestId("assembly-slot-create-apply"));
    expect(onApply).toHaveBeenCalledWith({
      contentType: "percRichText",
      folderPath: "/Sites/A",
      snippetTemplateId: 4,
    });
  });
});
