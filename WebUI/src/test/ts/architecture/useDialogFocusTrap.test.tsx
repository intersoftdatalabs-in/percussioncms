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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import React, { useRef } from "react";
import { afterEach, describe, expect, it } from "vitest";
import { useDialogFocusTrap } from "../../../main/ts/architecture/useDialogEscape";

function TrapHarness({
  initial = "first",
}: {
  initial?: "first" | "second" | "outside" | "none";
}): React.ReactElement {
  const rootRef = useRef<HTMLDivElement>(null);
  const secondRef = useRef<HTMLButtonElement>(null);
  const outsideRef = useRef<HTMLButtonElement>(null);
  const initialRef =
    initial === "second"
      ? secondRef
      : initial === "outside"
        ? outsideRef
        : undefined;
  useDialogFocusTrap(true, rootRef, initialRef);
  return (
    <>
      <button ref={outsideRef} type="button" data-testid="trap-outside">
        outside
      </button>
      <div ref={rootRef} role="dialog" data-testid="trap-root">
        <button type="button" data-testid="trap-first">
          first
        </button>
        <button ref={secondRef} type="button" data-testid="trap-second">
          second
        </button>
      </div>
    </>
  );
}

function EmptyHarness(): React.ReactElement {
  const rootRef = useRef<HTMLDivElement>(null);
  useDialogFocusTrap(true, rootRef);
  return (
    <div ref={rootRef} role="dialog" data-testid="trap-empty">
      <p>no controls</p>
    </div>
  );
}

describe("useDialogFocusTrap (#7003)", () => {
  afterEach(() => {
    cleanup();
  });

  it("focuses the first control when no initial ref is given", () => {
    render(<TrapHarness initial="none" />);
    expect(screen.getByTestId("trap-first")).toHaveFocus();
  });

  it("focuses the preferred initial control (destructive Cancel pattern)", () => {
    render(<TrapHarness initial="second" />);
    expect(screen.getByTestId("trap-second")).toHaveFocus();
  });

  it("falls back to the first control when the initial ref is outside the root", () => {
    render(<TrapHarness initial="outside" />);
    expect(screen.getByTestId("trap-first")).toHaveFocus();
  });

  it("wraps Tab from the last control to the first", () => {
    render(<TrapHarness initial="none" />);
    const root = screen.getByTestId("trap-root");
    screen.getByTestId("trap-second").focus();
    fireEvent.keyDown(root, { key: "Tab" });
    expect(screen.getByTestId("trap-first")).toHaveFocus();
  });

  it("wraps Shift+Tab from the first control to the last", () => {
    render(<TrapHarness initial="none" />);
    const root = screen.getByTestId("trap-root");
    expect(screen.getByTestId("trap-first")).toHaveFocus();
    fireEvent.keyDown(root, { key: "Tab", shiftKey: true });
    expect(screen.getByTestId("trap-second")).toHaveFocus();
  });

  it("re-anchors Tab onto the first control when focus is outside the container", () => {
    render(<TrapHarness initial="none" />);
    const root = screen.getByTestId("trap-root");
    screen.getByTestId("trap-outside").focus();
    expect(screen.getByTestId("trap-outside")).toHaveFocus();
    const prevented = fireEvent.keyDown(root, { key: "Tab" });
    expect(prevented).toBe(false);
    expect(screen.getByTestId("trap-first")).toHaveFocus();
  });

  it("re-anchors Shift+Tab onto the last control when focus is outside the container", () => {
    render(<TrapHarness initial="none" />);
    const root = screen.getByTestId("trap-root");
    screen.getByTestId("trap-outside").focus();
    fireEvent.keyDown(root, { key: "Tab", shiftKey: true });
    expect(screen.getByTestId("trap-second")).toHaveFocus();
  });

  it("swallows Tab when the dialog has no focusable controls", () => {
    render(<EmptyHarness />);
    const root = screen.getByTestId("trap-empty");
    const prevented = fireEvent.keyDown(root, { key: "Tab" });
    expect(prevented).toBe(false);
    expect(document.activeElement).toBe(document.body);
  });
});
