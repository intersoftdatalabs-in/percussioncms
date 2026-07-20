/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { TransitionDialog } from "../../../main/ts/workflowActions/TransitionDialog";

describe("TransitionDialog", () => {
  it("blocks submit if comment is required and empty", async () => {
    const handleSubmit = vi.fn();
    const handleCancel = vi.fn();

    render(
      <TransitionDialog
        trigger="Approve"
        requiresComment={true}
        supportsAdhocAssignees={false}
        onSubmit={handleSubmit}
        onCancel={handleCancel}
      />
    );

    const submitBtn = screen.getByTestId("transition-submit-button");
    fireEvent.click(submitBtn);

    expect(handleSubmit).not.toHaveBeenCalled();
    expect(screen.getByTestId("transition-error")).toBeTruthy();
  });

  it("submits comment if filled", async () => {
    const handleSubmit = vi.fn();
    const handleCancel = vi.fn();

    render(
      <TransitionDialog
        trigger="Approve"
        requiresComment={true}
        supportsAdhocAssignees={false}
        onSubmit={handleSubmit}
        onCancel={handleCancel}
      />
    );

    const input = screen.getByTestId("transition-comment-input");
    fireEvent.change(input, { target: { value: "Looks good" } });

    const submitBtn = screen.getByTestId("transition-submit-button");
    fireEvent.click(submitBtn);

    expect(handleSubmit).toHaveBeenCalledWith("Looks good", []);
  });
});
