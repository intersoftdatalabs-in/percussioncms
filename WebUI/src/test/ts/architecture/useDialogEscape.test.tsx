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

import { cleanup, fireEvent, render } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useDialogEscape } from "../../../main/ts/architecture/useDialogEscape";

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
  return <div data-testid="escape-harness">open</div>;
}

describe("useDialogEscape (#3098)", () => {
  afterEach(() => {
    cleanup();
  });

  it("calls onCancel on Escape when open and not busy", () => {
    const onCancel = vi.fn();
    render(<Harness open busy={false} onCancel={onCancel} />);
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("ignores Escape when busy or closed", () => {
    const onCancel = vi.fn();
    const { rerender } = render(
      <Harness open busy onCancel={onCancel} />,
    );
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).not.toHaveBeenCalled();
    rerender(<Harness open={false} busy={false} onCancel={onCancel} />);
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).not.toHaveBeenCalled();
  });
});
