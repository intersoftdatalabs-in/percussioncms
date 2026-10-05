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

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CheckinCommentDialog } from "../../../main/ts/contentExplorer/CheckinCommentDialog";
import { renderA11yGate } from "./a11y";

describe("CheckinCommentDialog (#5199)", () => {
  it("cancel does not confirm and the dialog is keyboard-labelled", async () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <CheckinCommentDialog onConfirm={onConfirm} onCancel={onCancel} />,
    );
    expect(screen.getByTestId("explorer-checkin-comment")).toBeTruthy();
    fireEvent.click(screen.getByTestId("explorer-checkin-cancel"));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("Escape cancels without sending the comment", () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    render(<CheckinCommentDialog onConfirm={onConfirm} onCancel={onCancel} />);
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it("confirm sends the entered comment", () => {
    const onConfirm = vi.fn();
    render(<CheckinCommentDialog onConfirm={onConfirm} onCancel={vi.fn()} />);
    fireEvent.change(screen.getByTestId("explorer-checkin-comment-input"), {
      target: { value: "shipped copy" },
    });
    fireEvent.click(screen.getByTestId("explorer-checkin-confirm"));
    expect(onConfirm).toHaveBeenCalledWith("shipped copy");
  });

  it("blank confirm is an empty comment, not a cancel", () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    render(<CheckinCommentDialog onConfirm={onConfirm} onCancel={onCancel} />);
    fireEvent.click(screen.getByTestId("explorer-checkin-confirm"));
    expect(onConfirm).toHaveBeenCalledWith("");
    expect(onCancel).not.toHaveBeenCalled();
  });
});
