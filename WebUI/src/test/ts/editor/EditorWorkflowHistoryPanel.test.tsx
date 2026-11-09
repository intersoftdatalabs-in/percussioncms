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

import { cleanup, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it } from "vitest";
import { EditorWorkflowHistoryPanel } from "../../../main/ts/editor/EditorWorkflowHistoryPanel";

describe("EditorWorkflowHistoryPanel (#5829)", () => {
  afterEach(() => {
    cleanup();
  });

  it("lists date, user, transition type, and comment per row", () => {
    render(
      <EditorWorkflowHistoryPanel
        comments={[
          {
            comment: "Looks good",
            commenter: "admin",
            commentType: "Approve",
            commentDate: "2026-04-12",
          },
          {
            comment: "",
            commenter: "editor2",
            commentType: "Submit",
            commentDate: "2026-04-10",
          },
        ]}
      />,
    );
    expect(screen.getByTestId("editor-workflow-history-table")).toBeTruthy();
    expect(
      screen.getByTestId("editor-workflow-history-row-0"),
    ).toBeTruthy();
    expect(
      screen.getByTestId("editor-workflow-history-date-0").textContent,
    ).toBe("2026-04-12");
    expect(
      screen.getByTestId("editor-workflow-history-user-0").textContent,
    ).toBe("admin");
    expect(
      screen.getByTestId("editor-workflow-history-type-0").textContent,
    ).toBe("Approve");
    expect(
      screen.getByTestId("editor-workflow-history-comment-0").textContent,
    ).toBe("Looks good");
    expect(
      screen.getByTestId("editor-workflow-history-user-1").textContent,
    ).toBe("editor2");
    expect(
      screen.queryByTestId("editor-workflow-history-empty"),
    ).toBeNull();
  });

  it("shows an explicit empty state instead of a blank panel", () => {
    render(<EditorWorkflowHistoryPanel comments={[]} />);
    expect(screen.getByTestId("editor-workflow-history-empty")).toBeTruthy();
    expect(
      screen.getByTestId("editor-workflow-history-empty").textContent,
    ).toMatch(/no workflow comments/i);
    expect(
      screen.queryByTestId("editor-workflow-history-table"),
    ).toBeNull();
  });

  it("shows a loading state while the history loads", () => {
    render(<EditorWorkflowHistoryPanel comments={[]} loading />);
    expect(
      screen.getByTestId("editor-workflow-history-loading"),
    ).toBeTruthy();
    expect(
      screen.queryByTestId("editor-workflow-history-empty"),
    ).toBeNull();
  });

  it("shows a load error instead of claiming an empty history", () => {
    render(
      <EditorWorkflowHistoryPanel
        comments={[]}
        loadErrorKey="perc.ui.editor@Could not load the workflow history."
        loadErrorDetail="HTTP 403"
      />,
    );
    expect(
      screen.getByTestId("editor-workflow-history-load-error").textContent,
    ).toMatch(/could not load the workflow history/i);
    expect(
      screen.queryByTestId("editor-workflow-history-table"),
    ).toBeNull();
    expect(
      screen.queryByTestId("editor-workflow-history-empty"),
    ).toBeNull();
  });
});
