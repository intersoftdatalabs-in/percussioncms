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

/**
 * Summary-preview branch of the metadata field row (#6959): for
 * {@code percPage} items the summary editor hides behind the sanitized
 * read-only preview while auto-summary is on, and stays editable while
 * auto-summary is off. Behavioral lock for the {@code renderFieldRow}
 * helper extraction — rendered output must not change.
 */

import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { EditorHost } from "../../../main/ts/editor/EditorHost";

function renderSummaryHost(autoGenerateSummary: string) {
  render(
    <MemoryRouter initialEntries={["/editor?contentId=42&mode=edit"]}>
      <Routes>
        <Route
          path="/editor"
          element={
            <EditorHost
              checkout={vi.fn().mockResolvedValue(undefined)}
              loadFields={async () => ({
                contentId: "42",
                contentType: "percPage",
                name: "Home",
                checkoutUser: "admin",
                fields: [
                  { name: "sys_title", value: "Home" },
                  { name: "page_summary", value: "<p>Summary text</p>" },
                  {
                    name: "auto_generate_summary",
                    value: autoGenerateSummary,
                  },
                ],
              })}
              saveFields={vi.fn()}
              loadType={async () => ({ fields: [] })}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("EditorHost summary preview branch", () => {
  it("shows the read-only preview instead of the summary editor when auto-summary is on", async () => {
    renderSummaryHost("yes");
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-row-page_summary")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-metadata-summary-preview")).toBeTruthy();
    expect(screen.queryByTestId("editor-field-page_summary")).toBeNull();
  });

  it("keeps the editable summary control when auto-summary is off", async () => {
    renderSummaryHost("no");
    await waitFor(() => {
      expect(screen.getByTestId("editor-field-row-page_summary")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-metadata-summary-preview")).toBeNull();
    expect(screen.getByTestId("editor-field-page_summary")).toBeTruthy();
  });
});
