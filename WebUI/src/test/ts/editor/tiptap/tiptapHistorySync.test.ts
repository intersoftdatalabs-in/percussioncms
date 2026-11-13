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

import { Editor } from "@tiptap/core";
import { afterEach, describe, expect, it, vi } from "vitest";
import { setTipTapContentWithoutHistory } from "../../../../main/ts/editor/tiptap/tiptapHistorySync";
import { createTipTapEditorExtensions } from "../../../../main/ts/editor/tiptap/tiptapSetup";

function makeEditor(html = "<p>One</p>"): Editor {
  return new Editor({
    extensions: createTipTapEditorExtensions(),
    content: html,
  });
}

describe("setTipTapContentWithoutHistory (#6262)", () => {
  let editors: Editor[] = [];
  afterEach(() => {
    for (const editor of editors) {
      editor.destroy();
    }
    editors = [];
  });

  function track(html?: string): Editor {
    const editor = makeEditor(html);
    editors.push(editor);
    return editor;
  }

  it("replaces the document like setContent", () => {
    const editor = track();
    setTipTapContentWithoutHistory(editor, "<p>Two</p>");
    expect(editor.getHTML()).toBe("<p>Two</p>");
  });

  it("stays silent: no onUpdate echo back through onChange", () => {
    const onUpdate = vi.fn();
    const editor = new Editor({
      extensions: createTipTapEditorExtensions(),
      content: "<p>One</p>",
      onUpdate,
    });
    editors.push(editor);
    setTipTapContentWithoutHistory(editor, "<p>Two</p>");
    expect(onUpdate).not.toHaveBeenCalled();
  });

  it("user typing stays undoable and redoable", () => {
    const editor = track();
    editor.commands.setTextSelection(editor.state.doc.content.size - 1);
    editor.commands.insertContent("!");
    expect(editor.getHTML()).toBe("<p>One!</p>");
    expect(editor.commands.undo()).toBe(true);
    expect(editor.getHTML()).toBe("<p>One</p>");
    expect(editor.commands.redo()).toBe(true);
    expect(editor.getHTML()).toBe("<p>One!</p>");
  });

  it("an external sync is not itself an undo step and clears prior history", () => {
    const editor = track();
    // A user edit, then a programmatic sync (revert-all / restore / reload).
    editor.commands.setTextSelection(editor.state.doc.content.size - 1);
    editor.commands.insertContent("!");
    expect(editor.getHTML()).toBe("<p>One!</p>");
    setTipTapContentWithoutHistory(editor, "<p>Two</p>");
    expect(editor.getHTML()).toBe("<p>Two</p>");
    // Undo must not walk back into the pre-sync lineage: the discarded "!"
    // (and the "One" baseline) are gone from the stack, so undo is a no-op
    // and the synced document stands.
    expect(editor.commands.undo()).toBe(false);
    expect(editor.getHTML()).toBe("<p>Two</p>");
    // Redo is empty too: the sync is the new undo baseline.
    expect(editor.commands.redo()).toBe(false);
    expect(editor.getHTML()).toBe("<p>Two</p>");
  });

  it("typing after a sync is undoable back to the synced baseline", () => {
    const editor = track();
    setTipTapContentWithoutHistory(editor, "<p>Two</p>");
    editor.commands.setTextSelection(editor.state.doc.content.size - 1);
    editor.commands.insertContent("?");
    expect(editor.getHTML()).toBe("<p>Two?</p>");
    expect(editor.commands.undo()).toBe(true);
    expect(editor.getHTML()).toBe("<p>Two</p>");
  });

  it("preserves the caret best-effort inside the new document", () => {
    const editor = track("<p>Hello world</p>");
    editor.commands.setTextSelection(6);
    setTipTapContentWithoutHistory(editor, "<p>Hi</p>");
    const { from } = editor.state.selection;
    expect(from).toBeLessThanOrEqual(editor.state.doc.content.size);
  });
});
