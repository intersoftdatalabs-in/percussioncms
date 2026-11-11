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
import { afterEach, describe, expect, it, vi } from "vitest";
import { HtmlFieldWidget } from "../../../../main/ts/editor/widgets/HtmlFieldWidget";

// jsdom Range has no caret geometry; ProseMirror scrollToSelection needs
// these two or the transaction throws. Zero rects keep scrolling a no-op
// without changing editor behavior.
function stubJsdomCaretRects(): void {
  const proto = window.Range.prototype as unknown as Record<string, unknown>;
  if (typeof proto.getClientRects !== "function") {
    proto.getClientRects = () => [];
  }
  if (typeof proto.getBoundingClientRect !== "function") {
    proto.getBoundingClientRect = () => ({
      x: 0,
      y: 0,
      top: 0,
      left: 0,
      bottom: 0,
      right: 0,
      width: 0,
      height: 0,
      toJSON: () => ({}),
    });
  }
}

describe("HtmlFieldWidget", () => {
  afterEach(() => {
    cleanup();
  });

  it("renders the bundled TipTap editor unconditionally (#5902)", async () => {
    stubJsdomCaretRects();
    render(
      <HtmlFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        onChange={vi.fn()}
      />,
    );
    const editable = await screen.findByTestId("editor-field-text");
    expect(editable.getAttribute("data-editor-kind")).toBe("html");
    expect(editable.getAttribute("data-editor-engine")).toBe("tiptap");
    await screen.findByText("Hi");
  });

  it("marks the widget invalid when the HTML is refused", async () => {
    stubJsdomCaretRects();
    render(
      <HtmlFieldWidget
        name="text"
        value={'<script>alert(1)</script>'}
        readOnly={false}
        invalid
        onChange={vi.fn()}
      />,
    );
    const editable = await screen.findByTestId("editor-field-text");
    expect(editable.getAttribute("aria-invalid")).toBe("true");
  });

  it("renders read-only in view mode", async () => {
    stubJsdomCaretRects();
    render(
      <HtmlFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={true}
        onChange={vi.fn()}
      />,
    );
    const editable = await screen.findByTestId("editor-field-text");
    expect(editable.getAttribute("contenteditable")).toBe("false");
    await screen.findByText("Hi");
  });
});
