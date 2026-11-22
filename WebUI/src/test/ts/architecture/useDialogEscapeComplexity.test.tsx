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
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  captureDialogOpener,
  useDialogEscape,
  useDialogFocusTrap,
} from "../../../main/ts/architecture/useDialogEscape";

function Harness({
  open,
  busy,
  onCancel,
}: {
  open: boolean;
  busy: boolean;
  onCancel: () => void;
}): React.ReactElement {
  useDialogEscape(open, busy, onCancel);
  return <div data-testid="escape-complexity">open</div>;
}

describe("useDialogEscape complexity follow-up (#7017)", () => {
  afterEach(() => {
    cleanup();
    captureDialogOpener(null);
  });

  it("ignores non-Escape keys", () => {
    const onCancel = vi.fn();
    render(<Harness open busy={false} onCancel={onCancel} />);
    fireEvent.keyDown(window, { key: "Enter" });
    fireEvent.keyDown(window, { key: "Tab" });
    expect(onCancel).not.toHaveBeenCalled();
  });

  it("fires after busy flips to false without remount", () => {
    const onCancel = vi.fn();
    const { rerender } = render(
      <Harness open busy onCancel={onCancel} />,
    );
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).not.toHaveBeenCalled();
    rerender(<Harness open busy={false} onCancel={onCancel} />);
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("always calls the latest onCancel closure", () => {
    const first = vi.fn();
    const second = vi.fn();
    const { rerender } = render(
      <Harness open busy={false} onCancel={first} />,
    );
    rerender(<Harness open busy={false} onCancel={second} />);
    fireEvent.keyDown(window, { key: "Escape" });
    expect(first).not.toHaveBeenCalled();
    expect(second).toHaveBeenCalledTimes(1);
  });

  it("restores focus to the opener on unmount while open (#6988)", () => {
    const opener = document.createElement("button");
    opener.type = "button";
    document.body.appendChild(opener);
    opener.focus();
    const { unmount } = render(
      <Harness open busy={false} onCancel={() => {}} />,
    );
    const steal = document.createElement("input");
    document.body.appendChild(steal);
    steal.focus();
    unmount();
    expect(document.activeElement).toBe(opener);
    steal.remove();
    opener.remove();
  });

  it("does not steal focus on unmount when never opened", () => {
    const { unmount } = render(
      <Harness open={false} busy={false} onCancel={() => {}} />,
    );
    const before = document.activeElement;
    unmount();
    expect(document.activeElement).toBe(before);
  });
});

function ClosedTrapHarness(): React.ReactElement {
  const rootRef = useRef<HTMLDivElement>(null);
  useDialogFocusTrap(false, rootRef);
  return (
    <div ref={rootRef} role="dialog" data-testid="trap-closed">
      <button type="button" data-testid="trap-closed-btn">
        btn
      </button>
    </div>
  );
}

describe("useDialogFocusTrap complexity follow-up (#7017)", () => {
  afterEach(() => {
    cleanup();
  });

  it("ignores non-Tab keys inside the dialog", () => {
    const rootRef = { current: null as HTMLElement | null };
    function H(): React.ReactElement {
      const inner = useRef<HTMLDivElement>(null);
      React.useEffect(() => {
        rootRef.current = inner.current;
      });
      useDialogFocusTrap(true, inner);
      return (
        <div ref={inner} role="dialog" data-testid="trap-nontab">
          <button type="button" data-testid="trap-nontab-btn">
            btn
          </button>
        </div>
      );
    }
    render(<H />);
    const root = screen.getByTestId("trap-nontab");
    const btn = screen.getByTestId("trap-nontab-btn");
    btn.focus();
    fireEvent.keyDown(root, { key: "Escape" });
    fireEvent.keyDown(root, { key: "Enter" });
    expect(btn).toHaveFocus();
  });

  it("does nothing while closed", () => {
    render(<ClosedTrapHarness />);
    const root = screen.getByTestId("trap-closed");
    // No initial focus steal when closed.
    expect(screen.getByTestId("trap-closed-btn")).not.toHaveFocus();
    fireEvent.keyDown(root, { key: "Tab" });
    expect(screen.getByTestId("trap-closed-btn")).not.toHaveFocus();
  });
});
