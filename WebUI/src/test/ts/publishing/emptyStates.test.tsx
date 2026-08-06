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
import { EmptyState } from "@/publishing/components/EmptyState";

describe("EmptyState", () => {
  it("shows title and next action", () => {
    render(
      <EmptyState
        title="No sites"
        nextAction="Create a site before publishing."
      />,
    );
    expect(screen.getByTestId("publish-empty-state")).toBeTruthy();
    expect(screen.getByText("No sites")).toBeTruthy();
    expect(screen.getByText(/Create a site/)).toBeTruthy();
  });

  it("invokes CTA", () => {
    const onAction = vi.fn();
    render(
      <EmptyState title="No servers" actionLabel="Add server" onAction={onAction} />,
    );
    fireEvent.click(screen.getByText("Add server"));
    expect(onAction).toHaveBeenCalled();
  });
});
