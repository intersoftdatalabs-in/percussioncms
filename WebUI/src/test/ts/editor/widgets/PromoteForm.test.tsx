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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { PromoteForm } from "../../../../main/ts/editor/widgets/PromoteForm";

describe("PromoteForm", () => {
  afterEach(() => {
    cleanup();
  });

  it("promotes the selected revision through itemmanagement restore", async () => {
    const promoteRevision = vi.fn().mockResolvedValue(undefined);
    render(
      <PromoteForm
        itemId="42"
        loadRevisions={async () => ({
          restorable: true,
          revisions: [
            { revId: 2, lastModifiedDate: "2026-01-01", lastModifier: "admin", status: "Draft" },
          ],
          comments: [],
        })}
        promoteRevision={promoteRevision}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-promote-select")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-promote"));
    await waitFor(() => {
      expect(promoteRevision).toHaveBeenCalledWith("42", 2);
    });
    expect(screen.getByTestId("editor-promote-ok")).toBeTruthy();
  });
});
