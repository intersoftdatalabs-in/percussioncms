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
import { EditorRelatedContentPanel } from "../../../main/ts/editor/EditorRelatedContentPanel";

const canvas = {
  ownerId: 42,
  templateId: 7,
  slots: [
    {
      slotId: 9,
      name: "content",
      label: "Content",
      items: [
        {
          relationshipId: 3,
          ownerId: 42,
          dependentId: 55,
          slotId: 9,
          templateId: 7,
          sortRank: 0,
        },
      ],
    },
  ],
};

const local = {
  count: 1,
  links: [{ type: "local", targetId: "88" }],
};

describe("Editor related snippet template", () => {
  afterEach(() => {
    cleanup();
  });

  it("applies a snippet template on a slot row and cancel does not post", async () => {
    const change = vi.fn().mockResolvedValue({
      relationshipId: 3,
      ownerId: 42,
      dependentId: 55,
      slotId: 9,
      templateId: 4,
      sortRank: 0,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      { id: 7, name: "brief", label: "Brief" },
      { id: 4, name: "full", label: "Full" },
    ]);
    render(
      <EditorRelatedContentPanel
        itemId="42"
        loadCanvas={async () => canvas}
        loadLocal={async () => local}
        changeSnippetTemplate={change}
        loadAllowedTemplates={loadTemplates}
        openRelatedItem={async () => true}
      />,
    );
    await waitFor(() => {
      expect(screen.getAllByTestId("editor-related-change-template")).toHaveLength(1);
    });
    const buttons = screen.getAllByTestId("editor-related-row");
    expect(buttons[1].querySelector('[data-testid="editor-related-change-template"]')).toBeNull();
    fireEvent.click(screen.getByTestId("editor-related-change-template"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-template-dialog")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-related-template-cancel"));
    expect(change).not.toHaveBeenCalled();
    expect(screen.queryByTestId("editor-related-template-dialog")).toBeNull();
    fireEvent.click(screen.getByTestId("editor-related-change-template"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-template-select")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("editor-related-template-select"), {
      target: { value: "4" },
    });
    fireEvent.click(screen.getByTestId("editor-related-template-apply"));
    await waitFor(() => {
      expect(change).toHaveBeenCalledWith(3, 9, 4);
    });
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-template-saved")).toBeTruthy();
    });
    expect(screen.getByTestId("editor-related-panel")).toBeTruthy();
  });

  it("keeps the panel when the template write is forbidden", async () => {
    const change = vi.fn().mockRejectedValue({ status: 403, statusText: "No", body: {} });
    render(
      <EditorRelatedContentPanel
        itemId="42"
        loadCanvas={async () => canvas}
        loadLocal={async () => ({ count: 0, links: [] })}
        changeSnippetTemplate={change}
        loadAllowedTemplates={async () => [{ id: 4, name: "full", label: "Full" }]}
        openRelatedItem={async () => true}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-change-template")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-related-change-template"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-template-apply")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("editor-related-template-apply"));
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-template-error").textContent).toMatch(
        /not allowed/i,
      );
    });
    expect(screen.getByTestId("editor-related-panel")).toBeTruthy();
    expect(screen.getByTestId("editor-related-list")).toBeTruthy();
  });

  it("hides template change in read-only mode", async () => {
    render(
      <EditorRelatedContentPanel
        itemId="42"
        readOnly
        loadCanvas={async () => canvas}
        loadLocal={async () => ({ count: 0, links: [] })}
        openRelatedItem={async () => true}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("editor-related-list")).toBeTruthy();
    });
    expect(screen.queryByTestId("editor-related-change-template")).toBeNull();
  });
});
