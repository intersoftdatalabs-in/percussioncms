/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { cleanup, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { roundTripHtmlFragment } from "../../../../main/ts/editor/tiptap/tiptapRoundTrip";
import { TipTapFieldWidget } from "../../../../main/ts/editor/widgets/TipTapFieldWidget";

/**
 * Read-only render parity for HTML fields (#7046, #5622 slice 1/3).
 *
 * <p>Read-only rows (View mode, checked-out-to-other, schema-readonly) render
 * a static schema-normalized view instead of mounting an interactive
 * ProseMirror instance. These tests prove the static markup is exactly what
 * the editing view serializes for the same stored value — parity by
 * construction — over a TinyMCE-era corpus, and that the row contract
 * ({@code editor-field-} testid, data markers, {@code contenteditable},
 * {@code readonly}, aria wiring, label-click guard) is unchanged.</p>
 */
describe("TipTapFieldWidget read-only parity (#7046)", () => {
  afterEach(() => {
    cleanup();
  });

  // Stored-value corpus: TinyMCE-era markup the migration must render
  // identically to the editing view (aliases, bookkeeping attrs, legacy
  // table wrappers, Percussion marker shorthand, empty).
  const corpus: Array<[string, string]> = [
    ["plain paragraph", "<p>Hi</p>"],
    ["inline aliases", "<p><b>Bold</b> and <i>italic</i></p>"],
    ["tiny bookkeeping", '<p data-mce-bogus="1">Kept</p>'],
    ["bullet list", "<ul><li>one</li><li>two</li></ul>"],
    [
      "legacy table wrappers",
      "<table><tbody><tr><td>cell</td></tr></tbody></table>",
    ],
    [
      "more-link marker shorthand",
      '<p>Before</p><span class="perc-blog-more-link"/><p>After</p>',
    ],
    ["empty", ""],
  ];

  it.each(corpus)(
    "static view equals the editor serialization for %s",
    (_label, stored) => {
      const onChange = vi.fn();
      render(
        <TipTapFieldWidget
          name="text"
          value={stored}
          readOnly={true}
          onChange={onChange}
        />,
      );
      const node = screen.getByTestId("editor-field-text");
      expect(node.innerHTML).toBe(roundTripHtmlFragment(stored));
      expect(onChange).not.toHaveBeenCalled();
    },
  );

  it("mounts no interactive editor for read-only rows", () => {
    render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={true}
        onChange={vi.fn()}
      />,
    );
    const host = screen.getByTestId("editor-tiptap-text");
    // No ProseMirror view, no toolbar chrome: nothing to focus or type into.
    expect(host.querySelector(".ProseMirror")).toBeNull();
    expect(
      screen.queryByTestId("editor-tiptap-toolbar-text"),
    ).toBeNull();
    expect(host.textContent).toContain("Hi");
  });

  it("keeps the read-only row contract", () => {
    render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={true}
        invalid
        describedById="text-error"
        onChange={vi.fn()}
      />,
    );
    const node = screen.getByTestId("editor-field-text");
    expect(node.getAttribute("data-editor-kind")).toBe("html");
    expect(node.getAttribute("data-editor-engine")).toBe("tiptap");
    expect(node.getAttribute("contenteditable")).toBe("false");
    expect(node.getAttribute("readonly")).toBe("");
    expect(node.getAttribute("role")).toBe("textbox");
    expect(node.getAttribute("aria-readonly")).toBe("true");
    expect(node.getAttribute("aria-invalid")).toBe("true");
    expect(node.getAttribute("aria-describedby")).toBe("text-error");
    expect(node.getAttribute("aria-label")).toBeTruthy();
  });

  it("drops refused script markup like the editor surface", () => {
    render(
      <TipTapFieldWidget
        name="text"
        value={'<p>Hi</p><script>alert(1)</script>'}
        readOnly={true}
        onChange={vi.fn()}
      />,
    );
    const node = screen.getByTestId("editor-field-text");
    expect(node.innerHTML).not.toContain("<script");
    expect(node.textContent).toContain("Hi");
  });

  it("cancels surface clicks so the row label cannot forward them", () => {
    render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={true}
        onChange={vi.fn()}
      />,
    );
    const host = screen.getByTestId("editor-tiptap-text");
    const click = new MouseEvent("click", { bubbles: true, cancelable: true });
    host.dispatchEvent(click);
    expect(click.defaultPrevented).toBe(true);
  });

  it("swaps between static and editable views when readOnly flips", async () => {
    const { rerender } = render(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={true}
        onChange={vi.fn()}
      />,
    );
    expect(screen.getByTestId("editor-tiptap-text").querySelector(".ProseMirror")).toBeNull();

    // Unlock: the interactive editor mounts with the stored content and the
    // parity toolbar returns.
    rerender(
      <TipTapFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        onChange={vi.fn()}
      />,
    );
    const editable = await screen.findByTestId("editor-field-text");
    expect(editable.getAttribute("contenteditable")).toBe("true");
    await screen.findByTestId("editor-tiptap-toolbar-text");

    // Lock again: back to the static view, no live editor.
    rerender(
      <TipTapFieldWidget
        name="text"
        value="<p>Bye</p>"
        readOnly={true}
        onChange={vi.fn()}
      />,
    );
    const node = await screen.findByTestId("editor-field-text");
    expect(node.innerHTML).toBe(roundTripHtmlFragment("<p>Bye</p>"));
    expect(
      screen.getByTestId("editor-tiptap-text").querySelector(".ProseMirror"),
    ).toBeNull();
    expect(
      screen.queryByTestId("editor-tiptap-toolbar-text"),
    ).toBeNull();
  });
});
